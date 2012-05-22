package com.googlecode.mp4parser.authoring.tracks;

import com.coremedia.iso.boxes.*;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.googlecode.mp4parser.authoring.AbstractTrack;
import com.googlecode.mp4parser.authoring.TrackMetaData;
import com.googlecode.mp4parser.boxes.AC3SpecificBox;
import com.googlecode.mp4parser.boxes.EC3SpecificBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BitReaderBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: magnus
 * Date: 2012-03-14
 * Time: 10:39
 * To change this template use File | Settings | File Templates.
 */
public class EC3TrackImpl extends AbstractTrack {
    TrackMetaData trackMetaData = new TrackMetaData();
    SampleDescriptionBox sampleDescriptionBox;

    int samplerate;
    int bitrate;
    int frameSize;

    List<BitStreamInfo> entries = new LinkedList<BitStreamInfo>();

    private InputStream inputStream;
    private List<ByteBuffer> samples;
    List<TimeToSampleBox.Entry> stts = new LinkedList<TimeToSampleBox.Entry>();

    public EC3TrackImpl(InputStream fin) throws IOException {
        inputStream = fin;

        boolean done = false;
        while (!done) {
            BitStreamInfo bsi = readVariables();
            if (bsi == null) {
                throw new IOException();
            }
            for (int i = 0; i < entries.size(); i++) {
                if (bsi.strmtyp != 1 && entries.get(i).substreamid == bsi.substreamid) {
                    done = true;
                }
            }
            if (!done) {
                entries.add(bsi);
                inputStream.skip(bsi.frameSize);
            }
        }
        for (BitStreamInfo bsi : entries) {
            inputStream.skip(-1 * bsi.frameSize);
        }

        if (entries.size() == 0) {
            throw new IOException();
        }
        samplerate = entries.get(0).samplerate;

        sampleDescriptionBox = new SampleDescriptionBox();
        AudioSampleEntry audioSampleEntry = new AudioSampleEntry("ec-3");
        audioSampleEntry.setChannelCount(2);
        audioSampleEntry.setSampleRate(samplerate);
        audioSampleEntry.setDataReferenceIndex(1);
        audioSampleEntry.setSampleSize(16);

        EC3SpecificBox ec3 = new EC3SpecificBox();
        int[] deps = new int[entries.size()];
        int[] chan_locs = new int[entries.size()];
        for (BitStreamInfo bsi : entries) {
            if (bsi.strmtyp == 1) {
                deps[bsi.substreamid]++;
                chan_locs[bsi.substreamid] = ((bsi.chanmap >> 6) & 0x100) | ((bsi.chanmap >> 5) & 0xff);
            }
        }
        for (BitStreamInfo bsi : entries) {
            if (bsi.strmtyp != 1) {
                EC3SpecificBox.Entry e = new EC3SpecificBox.Entry();
                e.fscod = bsi.fscod;
                e.bsid = bsi.bsid;
                e.bsmod = bsi.bsmod;
                e.acmod = bsi.acmod;
                e.lfeon = bsi.lfeon;
                e.reserved = 0;
                e.num_dep_sub = deps[bsi.substreamid];
                e.chan_loc = chan_locs[bsi.substreamid];
                e.reserved2 = 0;
                ec3.addEntry(e);
            }
            bitrate += bsi.bitrate;
            frameSize += bsi.frameSize;
        }

        ec3.setDataRate(bitrate / 1000);
        audioSampleEntry.addBox(ec3);
        sampleDescriptionBox.addBox(audioSampleEntry);

        trackMetaData.setCreationTime(new Date());
        trackMetaData.setModificationTime(new Date());
        trackMetaData.setLanguage("eng");
        trackMetaData.setTimescale(samplerate); // Audio tracks always use samplerate as timescale

        samples = new LinkedList<ByteBuffer>();
        if (!readSamples()) {
            throw new IOException();
        }
    }


    public List<ByteBuffer> getSamples() {

        return samples;
    }

    public SampleDescriptionBox getSampleDescriptionBox() {
        return sampleDescriptionBox;
    }

    public List<TimeToSampleBox.Entry> getDecodingTimeEntries() {
        return stts;
    }

    public List<CompositionTimeToSample.Entry> getCompositionTimeEntries() {
        return null;
    }

    public long[] getSyncSamples() {
        return null;
    }

    public List<SampleDependencyTypeBox.Entry> getSampleDependencies() {
        return null;
    }

    public TrackMetaData getTrackMetaData() {
        return trackMetaData;
    }

    public String getHandler() {
        return "soun";
    }

    public AbstractMediaHeaderBox getMediaHeaderBox() {
        return new SoundMediaHeaderBox();
    }

    public SubSampleInformationBox getSubsampleInformationBox() {
        return null;
    }

    private BitStreamInfo readVariables() throws IOException {
        byte[] data = new byte[200];
        if (200 != inputStream.read(data, 0, 200)) {
            return null;
        }
        inputStream.skip(-200); // Rewind
        ByteBuffer bb = ByteBuffer.wrap(data);
        BitReaderBuffer brb = new BitReaderBuffer(bb);
        int syncword = brb.readBits(16);
        if (syncword != 0xb77) {
            return null;
        }

        BitStreamInfo entry = new BitStreamInfo();

        entry.strmtyp = brb.readBits(2);
        entry.substreamid = brb.readBits(3);
        int frmsiz = brb.readBits(11);
        entry.frameSize = 2 * (frmsiz + 1);

        entry.fscod = brb.readBits(2);
        int fscod2 = -1;
        int numblkscod;
        if (entry.fscod == 3) {
            fscod2 = brb.readBits(2);
            numblkscod = 3;
        } else {
            numblkscod = brb.readBits(2);
        }
        int numberOfBlocksPerSyncFrame = 0;
        switch (numblkscod) {
            case 0:
                numberOfBlocksPerSyncFrame = 1;
                break;

            case 1:
                numberOfBlocksPerSyncFrame = 2;
                break;

            case 2:
                numberOfBlocksPerSyncFrame = 3;
                break;

            case 3:
                numberOfBlocksPerSyncFrame = 6;
                break;

        }
        entry.frameSize *= (6 / numberOfBlocksPerSyncFrame); // Not sure if this is the correct algorithm?

        entry.acmod = brb.readBits(3);
        entry.lfeon = brb.readBits(1);
        entry.bsid = brb.readBits(5);
        brb.readBits(5);
        if (1 == brb.readBits(1)) {
            brb.readBits(8); // compr
        }
        if (0 == entry.acmod) {
            brb.readBits(5);
            if (1 == brb.readBits(1)) {
                brb.readBits(8);
            }
        }
        if (1 == entry.strmtyp) {
            if (1 == brb.readBits(1)) {
                entry.chanmap = brb.readBits(16);
            }
        }
        if (1 == brb.readBits(1)) {     // mixing metadata
            if (entry.acmod > 2) {
                brb.readBits(2);
            }
            if (1 == (entry.acmod & 1) && entry.acmod > 2) {
                brb.readBits(3);
                brb.readBits(3);
            }
            if (0 < (entry.acmod & 4)) {
                brb.readBits(3);
                brb.readBits(3);
            }
            if (1 == entry.lfeon) {
                if (1 == brb.readBits(1)) {
                    brb.readBits(5);
                }
            }
            if (0 == entry.strmtyp) {
                if (1 == brb.readBits(1)) {
                    brb.readBits(6);
                }
                if (0 == entry.acmod) {
                    if (1 == brb.readBits(1)) {
                        brb.readBits(6);
                    }
                }
                if (1 == brb.readBits(1)) {
                    brb.readBits(6);
                }
                int mixdef = brb.readBits(2);
                if (1 == mixdef) {
                    brb.readBits(5);
                } else if (2 == mixdef) {
                    brb.readBits(12);
                } else if (3 == mixdef) {
                    int mixdeflen = brb.readBits(5);
                    if (1 == brb.readBits(1)) {
                        brb.readBits(5);
                        if (1 == brb.readBits(1)) {
                            brb.readBits(4);
                        }
                        if (1 == brb.readBits(1)) {
                            brb.readBits(4);
                        }
                        if (1 == brb.readBits(1)) {
                            brb.readBits(4);
                        }
                        if (1 == brb.readBits(1)) {
                            brb.readBits(4);
                        }
                        if (1 == brb.readBits(1)) {
                            brb.readBits(4);
                        }
                        if (1 == brb.readBits(1)) {
                            brb.readBits(4);
                        }
                        if (1 == brb.readBits(1)) {
                            brb.readBits(4);
                        }
                        if (1 == brb.readBits(1)) {
                            if (1 == brb.readBits(1)) {
                                brb.readBits(4);
                            }
                            if (1 == brb.readBits(1)) {
                                brb.readBits(4);
                            }
                        }
                    }
                    if (1 == brb.readBits(1)) {
                        brb.readBits(5);
                        if (1 == brb.readBits(1)) {
                            brb.readBits(7);
                            if (1 == brb.readBits(1)) {
                                brb.readBits(8);
                            }
                        }
                    }
                    for (int i = 0; i < (mixdeflen + 2); i++) {
                        brb.readBits(8);
                    }
                    brb.byteSync();
                }
                if (entry.acmod < 2) {
                    if (1 == brb.readBits(1)) {
                        brb.readBits(14);
                    }
                    if (0 == entry.acmod) {
                        if (1 == brb.readBits(1)) {
                            brb.readBits(14);
                        }
                    }
                    if (1 == brb.readBits(1)) {
                        if (numblkscod == 0) {
                            brb.readBits(5);
                        } else {
                            for (int i = 0; i < numberOfBlocksPerSyncFrame; i++) {
                                if (1 == brb.readBits(1)) {
                                    brb.readBits(5);
                                }
                            }
                        }

                    }
                }
            }
        }
        if (1 == brb.readBits(1)) { // infomdate
            entry.bsmod = brb.readBits(3);
        }

        switch (entry.fscod) {
            case 0:
                entry.samplerate = 48000;
                break;

            case 1:
                entry.samplerate = 44100;
                break;

            case 2:
                entry.samplerate = 32000;
                break;

            case 3: {
                switch (fscod2) {
                    case 0:
                        entry.samplerate = 24000;
                        break;

                    case 1:
                        entry.samplerate = 22050;
                        break;

                    case 2:
                        entry.samplerate = 16000;
                        break;

                    case 3:
                        entry.samplerate = 0;
                        break;
                }
                break;
            }

        }
        if (entry.samplerate == 0) {
            return null;
        }

        entry.bitrate = (int) (((double) entry.samplerate) / 1536.0 * entry.frameSize * 8);

        return entry;
    }

    private boolean readSamples() throws IOException {
        int read = frameSize;
        boolean ret = false;
        while (frameSize == read) {
            ret = true;
            byte[] data = new byte[frameSize];
            read = inputStream.read(data);
            if (read == frameSize) {
                samples.add(ByteBuffer.wrap(data));
                stts.add(new TimeToSampleBox.Entry(1, 1536));
            }
        }
        return ret;
    }

    public static class BitStreamInfo extends EC3SpecificBox.Entry {
        public int frameSize;
        public int substreamid;
        public int bitrate;
        public int samplerate;
        public int strmtyp;
        public int chanmap;
    }
}
