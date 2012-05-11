package com.googlecode.mp4parser.authoring.tracks;

import com.coremedia.iso.boxes.*;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.googlecode.mp4parser.authoring.AbstractTrack;
import com.googlecode.mp4parser.authoring.TrackMetaData;
import com.googlecode.mp4parser.boxes.AC3SpecificBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BitReaderBuffer;

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class AC3TrackImpl extends AbstractTrack {
    TrackMetaData trackMetaData = new TrackMetaData();
    SampleDescriptionBox sampleDescriptionBox;

    int samplerate;
    int bitrate;
    int channelCount;

    int fscod;
    int bsid;
    int bsmod;
    int acmod;
    int lfeon;
    int frmsizecod;

    int frameSize;
    int[][][][] bitRateAndFrameSizeTable;

    private InputStream inputStream;
    private List<ByteBuffer> samples;
    boolean readSamples = false;
    List<TimeToSampleBox.Entry> stts;

    public AC3TrackImpl(InputStream fin) throws IOException {
        inputStream = fin;
        bitRateAndFrameSizeTable = new int[19][2][3][2];
        stts = new LinkedList<TimeToSampleBox.Entry>();
        initBitRateAndFrameSizeTable();
        if (!readVariables()) {
            throw new IOException();
        }

        sampleDescriptionBox = new SampleDescriptionBox();
        AudioSampleEntry audioSampleEntry = new AudioSampleEntry("ac-3");
        audioSampleEntry.setChannelCount(2);
        audioSampleEntry.setSampleRate(samplerate);
        audioSampleEntry.setDataReferenceIndex(1);
        audioSampleEntry.setSampleSize(16);

        AC3SpecificBox ac3 = new AC3SpecificBox();
        ac3.setAcmod(acmod);
        ac3.setBitRateCode(frmsizecod >> 1);
        ac3.setBsid(bsid);
        ac3.setBsmod(bsmod);
        ac3.setFscod(fscod);
        ac3.setLfeon(lfeon);
        ac3.setReserved(0);

        audioSampleEntry.addBox(ac3);
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

    private boolean readVariables() throws IOException {
        byte[] data = new byte[100];
        if (100 != inputStream.read(data, 0, 100)) {
            return false;
        }
        inputStream.skip(-100); // Rewind
        ByteBuffer bb = ByteBuffer.wrap(data);
        BitReaderBuffer brb = new BitReaderBuffer(bb);
        int syncword = brb.readBits(16);
        if (syncword != 0xb77) {
            return false;
        }
        brb.readBits(16); // CRC-1
        fscod = brb.readBits(2);

        switch (fscod) {
            case 0:
                samplerate = 48000;
                break;

            case 1:
                samplerate = 44100;
                break;

            case 2:
                samplerate = 32000;
                break;

            case 3:
                samplerate = 0;
                break;

        }
        if (samplerate == 0) {
            return false;
        }

        frmsizecod = brb.readBits(6);

        if (!calcBitrateAndFrameSize(frmsizecod)) {
            return false;
        }

        if (frameSize == 0) {
            return false;
        }
        bsid = brb.readBits(5);
        bsmod = brb.readBits(3);
        acmod = brb.readBits(3);

        if (bsid == 9) {
            samplerate /= 2;
        } else if (bsid != 8 && bsid != 6) {
            return false;
        }

        if ((acmod != 1) && ((acmod & 1) == 1)) {
            brb.readBits(2);
        }

        if (0 != (acmod & 4)) {
            brb.readBits(2);
        }

        if (acmod == 2) {
            brb.readBits(2);
        }

        switch (acmod) {
            case 0:
                channelCount = 2;
                break;

            case 1:
                channelCount = 1;
                break;

            case 2:
                channelCount = 2;
                break;

            case 3:
                channelCount = 3;
                break;

            case 4:
                channelCount = 3;
                break;

            case 5:
                channelCount = 4;
                break;

            case 6:
                channelCount = 4;
                break;

            case 7:
                channelCount = 5;
                break;

        }

        lfeon = brb.readBits(1);

        if (lfeon == 1) {
            channelCount++;
        }
        return true;
    }

    private boolean calcBitrateAndFrameSize(int code) {
        int frmsizecode = code >>> 1;
        int flag = code & 1;
        if (frmsizecode > 18 || flag > 1 || fscod > 2) {
            return false;
        }
        bitrate = bitRateAndFrameSizeTable[frmsizecode][flag][fscod][0];
        frameSize = 2 * bitRateAndFrameSizeTable[frmsizecode][flag][fscod][1];
        return true;
    }

    private boolean readSamples() throws IOException {
        if (readSamples) {
            return true;
        }
        readSamples = true;
        byte[] header = new byte[5];
        boolean ret = false;
        while (-1 != inputStream.read(header)) {
            ret = true;
            int frmsizecode = header[4] & 63;
            calcBitrateAndFrameSize(frmsizecode);
            inputStream.skip(-5);
            byte[] data = new byte[frameSize];
            inputStream.read(data);
            samples.add(ByteBuffer.wrap(data));
            stts.add(new TimeToSampleBox.Entry(1, 1536));

        }
        return ret;
    }

    private void initBitRateAndFrameSizeTable() {
        // ETSI 102 366 Table 4.13, in frmsizecod, flag, fscod, bitrate/size order. Note that all sizes are in words, and all bitrates in kbps

        // 48kHz
        bitRateAndFrameSizeTable[0][0][0][0] = 32;
        bitRateAndFrameSizeTable[0][1][0][0] = 32;
        bitRateAndFrameSizeTable[0][0][0][1] = 64;
        bitRateAndFrameSizeTable[0][1][0][1] = 64;
        bitRateAndFrameSizeTable[1][0][0][0] = 40;
        bitRateAndFrameSizeTable[1][1][0][0] = 40;
        bitRateAndFrameSizeTable[1][0][0][1] = 80;
        bitRateAndFrameSizeTable[1][1][0][1] = 80;
        bitRateAndFrameSizeTable[2][0][0][0] = 48;
        bitRateAndFrameSizeTable[2][1][0][0] = 48;
        bitRateAndFrameSizeTable[2][0][0][1] = 96;
        bitRateAndFrameSizeTable[2][1][0][1] = 96;
        bitRateAndFrameSizeTable[3][0][0][0] = 56;
        bitRateAndFrameSizeTable[3][1][0][0] = 56;
        bitRateAndFrameSizeTable[3][0][0][1] = 112;
        bitRateAndFrameSizeTable[3][1][0][1] = 112;
        bitRateAndFrameSizeTable[4][0][0][0] = 64;
        bitRateAndFrameSizeTable[4][1][0][0] = 64;
        bitRateAndFrameSizeTable[4][0][0][1] = 128;
        bitRateAndFrameSizeTable[4][1][0][1] = 128;
        bitRateAndFrameSizeTable[5][0][0][0] = 80;
        bitRateAndFrameSizeTable[5][1][0][0] = 80;
        bitRateAndFrameSizeTable[5][0][0][1] = 160;
        bitRateAndFrameSizeTable[5][1][0][1] = 160;
        bitRateAndFrameSizeTable[6][0][0][0] = 96;
        bitRateAndFrameSizeTable[6][1][0][0] = 96;
        bitRateAndFrameSizeTable[6][0][0][1] = 192;
        bitRateAndFrameSizeTable[6][1][0][1] = 192;
        bitRateAndFrameSizeTable[7][0][0][0] = 112;
        bitRateAndFrameSizeTable[7][1][0][0] = 112;
        bitRateAndFrameSizeTable[7][0][0][1] = 224;
        bitRateAndFrameSizeTable[7][1][0][1] = 224;
        bitRateAndFrameSizeTable[8][0][0][0] = 128;
        bitRateAndFrameSizeTable[8][1][0][0] = 128;
        bitRateAndFrameSizeTable[8][0][0][1] = 256;
        bitRateAndFrameSizeTable[8][1][0][1] = 256;
        bitRateAndFrameSizeTable[9][0][0][0] = 160;
        bitRateAndFrameSizeTable[9][1][0][0] = 160;
        bitRateAndFrameSizeTable[9][0][0][1] = 320;
        bitRateAndFrameSizeTable[9][1][0][1] = 320;
        bitRateAndFrameSizeTable[10][0][0][0] = 192;
        bitRateAndFrameSizeTable[10][1][0][0] = 192;
        bitRateAndFrameSizeTable[10][0][0][1] = 384;
        bitRateAndFrameSizeTable[10][1][0][1] = 384;
        bitRateAndFrameSizeTable[11][0][0][0] = 224;
        bitRateAndFrameSizeTable[11][1][0][0] = 224;
        bitRateAndFrameSizeTable[11][0][0][1] = 448;
        bitRateAndFrameSizeTable[11][1][0][1] = 448;
        bitRateAndFrameSizeTable[12][0][0][0] = 256;
        bitRateAndFrameSizeTable[12][1][0][0] = 256;
        bitRateAndFrameSizeTable[12][0][0][1] = 512;
        bitRateAndFrameSizeTable[12][1][0][1] = 512;
        bitRateAndFrameSizeTable[13][0][0][0] = 320;
        bitRateAndFrameSizeTable[13][1][0][0] = 320;
        bitRateAndFrameSizeTable[13][0][0][1] = 640;
        bitRateAndFrameSizeTable[13][1][0][1] = 640;
        bitRateAndFrameSizeTable[14][0][0][0] = 384;
        bitRateAndFrameSizeTable[14][1][0][0] = 384;
        bitRateAndFrameSizeTable[14][0][0][1] = 768;
        bitRateAndFrameSizeTable[14][1][0][1] = 768;
        bitRateAndFrameSizeTable[15][0][0][0] = 448;
        bitRateAndFrameSizeTable[15][1][0][0] = 448;
        bitRateAndFrameSizeTable[15][0][0][1] = 896;
        bitRateAndFrameSizeTable[15][1][0][1] = 896;
        bitRateAndFrameSizeTable[16][0][0][0] = 512;
        bitRateAndFrameSizeTable[16][1][0][0] = 512;
        bitRateAndFrameSizeTable[16][0][0][1] = 1024;
        bitRateAndFrameSizeTable[16][1][0][1] = 1024;
        bitRateAndFrameSizeTable[17][0][0][0] = 576;
        bitRateAndFrameSizeTable[17][1][0][0] = 576;
        bitRateAndFrameSizeTable[17][0][0][1] = 1152;
        bitRateAndFrameSizeTable[17][1][0][1] = 1152;
        bitRateAndFrameSizeTable[18][0][0][0] = 640;
        bitRateAndFrameSizeTable[18][1][0][0] = 640;
        bitRateAndFrameSizeTable[18][0][0][1] = 1280;
        bitRateAndFrameSizeTable[18][1][0][1] = 1280;

        // 44.1 kHz
        bitRateAndFrameSizeTable[0][0][1][0] = 32;
        bitRateAndFrameSizeTable[0][1][1][0] = 32;
        bitRateAndFrameSizeTable[0][0][1][1] = 69;
        bitRateAndFrameSizeTable[0][1][1][1] = 70;
        bitRateAndFrameSizeTable[1][0][1][0] = 40;
        bitRateAndFrameSizeTable[1][1][1][0] = 40;
        bitRateAndFrameSizeTable[1][0][1][1] = 87;
        bitRateAndFrameSizeTable[1][1][1][1] = 88;
        bitRateAndFrameSizeTable[2][0][1][0] = 48;
        bitRateAndFrameSizeTable[2][1][1][0] = 48;
        bitRateAndFrameSizeTable[2][0][1][1] = 104;
        bitRateAndFrameSizeTable[2][1][1][1] = 105;
        bitRateAndFrameSizeTable[3][0][1][0] = 56;
        bitRateAndFrameSizeTable[3][1][1][0] = 56;
        bitRateAndFrameSizeTable[3][0][1][1] = 121;
        bitRateAndFrameSizeTable[3][1][1][1] = 122;
        bitRateAndFrameSizeTable[4][0][1][0] = 64;
        bitRateAndFrameSizeTable[4][1][1][0] = 64;
        bitRateAndFrameSizeTable[4][0][1][1] = 139;
        bitRateAndFrameSizeTable[4][1][1][1] = 140;
        bitRateAndFrameSizeTable[5][0][1][0] = 80;
        bitRateAndFrameSizeTable[5][1][1][0] = 80;
        bitRateAndFrameSizeTable[5][0][1][1] = 174;
        bitRateAndFrameSizeTable[5][1][1][1] = 175;
        bitRateAndFrameSizeTable[6][0][1][0] = 96;
        bitRateAndFrameSizeTable[6][1][1][0] = 96;
        bitRateAndFrameSizeTable[6][0][1][1] = 208;
        bitRateAndFrameSizeTable[6][1][1][1] = 209;
        bitRateAndFrameSizeTable[7][0][1][0] = 112;
        bitRateAndFrameSizeTable[7][1][1][0] = 112;
        bitRateAndFrameSizeTable[7][0][1][1] = 243;
        bitRateAndFrameSizeTable[7][1][1][1] = 244;
        bitRateAndFrameSizeTable[8][0][1][0] = 128;
        bitRateAndFrameSizeTable[8][1][1][0] = 128;
        bitRateAndFrameSizeTable[8][0][1][1] = 278;
        bitRateAndFrameSizeTable[8][1][1][1] = 279;
        bitRateAndFrameSizeTable[9][0][1][0] = 160;
        bitRateAndFrameSizeTable[9][1][1][0] = 160;
        bitRateAndFrameSizeTable[9][0][1][1] = 348;
        bitRateAndFrameSizeTable[9][1][1][1] = 349;
        bitRateAndFrameSizeTable[10][0][1][0] = 192;
        bitRateAndFrameSizeTable[10][1][1][0] = 192;
        bitRateAndFrameSizeTable[10][0][1][1] = 417;
        bitRateAndFrameSizeTable[10][1][1][1] = 418;
        bitRateAndFrameSizeTable[11][0][1][0] = 224;
        bitRateAndFrameSizeTable[11][1][1][0] = 224;
        bitRateAndFrameSizeTable[11][0][1][1] = 487;
        bitRateAndFrameSizeTable[11][1][1][1] = 488;
        bitRateAndFrameSizeTable[12][0][1][0] = 256;
        bitRateAndFrameSizeTable[12][1][1][0] = 256;
        bitRateAndFrameSizeTable[12][0][1][1] = 557;
        bitRateAndFrameSizeTable[12][1][1][1] = 558;
        bitRateAndFrameSizeTable[13][0][1][0] = 320;
        bitRateAndFrameSizeTable[13][1][1][0] = 320;
        bitRateAndFrameSizeTable[13][0][1][1] = 696;
        bitRateAndFrameSizeTable[13][1][1][1] = 697;
        bitRateAndFrameSizeTable[14][0][1][0] = 384;
        bitRateAndFrameSizeTable[14][1][1][0] = 384;
        bitRateAndFrameSizeTable[14][0][1][1] = 835;
        bitRateAndFrameSizeTable[14][1][1][1] = 836;
        bitRateAndFrameSizeTable[15][0][1][0] = 448;
        bitRateAndFrameSizeTable[15][1][1][0] = 448;
        bitRateAndFrameSizeTable[15][0][1][1] = 975;
        bitRateAndFrameSizeTable[15][1][1][1] = 975;
        bitRateAndFrameSizeTable[16][0][1][0] = 512;
        bitRateAndFrameSizeTable[16][1][1][0] = 512;
        bitRateAndFrameSizeTable[16][0][1][1] = 1114;
        bitRateAndFrameSizeTable[16][1][1][1] = 1115;
        bitRateAndFrameSizeTable[17][0][1][0] = 576;
        bitRateAndFrameSizeTable[17][1][1][0] = 576;
        bitRateAndFrameSizeTable[17][0][1][1] = 1253;
        bitRateAndFrameSizeTable[17][1][1][1] = 1254;
        bitRateAndFrameSizeTable[18][0][1][0] = 640;
        bitRateAndFrameSizeTable[18][1][1][0] = 640;
        bitRateAndFrameSizeTable[18][0][1][1] = 1393;
        bitRateAndFrameSizeTable[18][1][1][1] = 1394;

        // 32kHz
        bitRateAndFrameSizeTable[0][0][2][0] = 32;
        bitRateAndFrameSizeTable[0][1][2][0] = 32;
        bitRateAndFrameSizeTable[0][0][2][1] = 96;
        bitRateAndFrameSizeTable[0][1][2][1] = 96;
        bitRateAndFrameSizeTable[1][0][2][0] = 40;
        bitRateAndFrameSizeTable[1][1][2][0] = 40;
        bitRateAndFrameSizeTable[1][0][2][1] = 120;
        bitRateAndFrameSizeTable[1][1][2][1] = 120;
        bitRateAndFrameSizeTable[2][0][2][0] = 48;
        bitRateAndFrameSizeTable[2][1][2][0] = 48;
        bitRateAndFrameSizeTable[2][0][2][1] = 144;
        bitRateAndFrameSizeTable[2][1][2][1] = 144;
        bitRateAndFrameSizeTable[3][0][2][0] = 56;
        bitRateAndFrameSizeTable[3][1][2][0] = 56;
        bitRateAndFrameSizeTable[3][0][2][1] = 168;
        bitRateAndFrameSizeTable[3][1][2][1] = 168;
        bitRateAndFrameSizeTable[4][0][2][0] = 64;
        bitRateAndFrameSizeTable[4][1][2][0] = 64;
        bitRateAndFrameSizeTable[4][0][2][1] = 192;
        bitRateAndFrameSizeTable[4][1][2][1] = 192;
        bitRateAndFrameSizeTable[5][0][2][0] = 80;
        bitRateAndFrameSizeTable[5][1][2][0] = 80;
        bitRateAndFrameSizeTable[5][0][2][1] = 240;
        bitRateAndFrameSizeTable[5][1][2][1] = 240;
        bitRateAndFrameSizeTable[6][0][2][0] = 96;
        bitRateAndFrameSizeTable[6][1][2][0] = 96;
        bitRateAndFrameSizeTable[6][0][2][1] = 288;
        bitRateAndFrameSizeTable[6][1][2][1] = 288;
        bitRateAndFrameSizeTable[7][0][2][0] = 112;
        bitRateAndFrameSizeTable[7][1][2][0] = 112;
        bitRateAndFrameSizeTable[7][0][2][1] = 336;
        bitRateAndFrameSizeTable[7][1][2][1] = 336;
        bitRateAndFrameSizeTable[8][0][2][0] = 128;
        bitRateAndFrameSizeTable[8][1][2][0] = 128;
        bitRateAndFrameSizeTable[8][0][2][1] = 384;
        bitRateAndFrameSizeTable[8][1][2][1] = 384;
        bitRateAndFrameSizeTable[9][0][2][0] = 160;
        bitRateAndFrameSizeTable[9][1][2][0] = 160;
        bitRateAndFrameSizeTable[9][0][2][1] = 480;
        bitRateAndFrameSizeTable[9][1][2][1] = 480;
        bitRateAndFrameSizeTable[10][0][2][0] = 192;
        bitRateAndFrameSizeTable[10][1][2][0] = 192;
        bitRateAndFrameSizeTable[10][0][2][1] = 576;
        bitRateAndFrameSizeTable[10][1][2][1] = 576;
        bitRateAndFrameSizeTable[11][0][2][0] = 224;
        bitRateAndFrameSizeTable[11][1][2][0] = 224;
        bitRateAndFrameSizeTable[11][0][2][1] = 672;
        bitRateAndFrameSizeTable[11][1][2][1] = 672;
        bitRateAndFrameSizeTable[12][0][2][0] = 256;
        bitRateAndFrameSizeTable[12][1][2][0] = 256;
        bitRateAndFrameSizeTable[12][0][2][1] = 768;
        bitRateAndFrameSizeTable[12][1][2][1] = 768;
        bitRateAndFrameSizeTable[13][0][2][0] = 320;
        bitRateAndFrameSizeTable[13][1][2][0] = 320;
        bitRateAndFrameSizeTable[13][0][2][1] = 960;
        bitRateAndFrameSizeTable[13][1][2][1] = 960;
        bitRateAndFrameSizeTable[14][0][2][0] = 384;
        bitRateAndFrameSizeTable[14][1][2][0] = 384;
        bitRateAndFrameSizeTable[14][0][2][1] = 1152;
        bitRateAndFrameSizeTable[14][1][2][1] = 1152;
        bitRateAndFrameSizeTable[15][0][2][0] = 448;
        bitRateAndFrameSizeTable[15][1][2][0] = 448;
        bitRateAndFrameSizeTable[15][0][2][1] = 1344;
        bitRateAndFrameSizeTable[15][1][2][1] = 1344;
        bitRateAndFrameSizeTable[16][0][2][0] = 512;
        bitRateAndFrameSizeTable[16][1][2][0] = 512;
        bitRateAndFrameSizeTable[16][0][2][1] = 1536;
        bitRateAndFrameSizeTable[16][1][2][1] = 1536;
        bitRateAndFrameSizeTable[17][0][2][0] = 576;
        bitRateAndFrameSizeTable[17][1][2][0] = 576;
        bitRateAndFrameSizeTable[17][0][2][1] = 1728;
        bitRateAndFrameSizeTable[17][1][2][1] = 1728;
        bitRateAndFrameSizeTable[18][0][2][0] = 640;
        bitRateAndFrameSizeTable[18][1][2][0] = 640;
        bitRateAndFrameSizeTable[18][0][2][1] = 1920;
        bitRateAndFrameSizeTable[18][1][2][1] = 1920;
    }
}