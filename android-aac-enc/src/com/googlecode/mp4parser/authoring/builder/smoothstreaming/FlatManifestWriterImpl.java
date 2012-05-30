/*
 * Copyright 2012 Sebastian Annies, Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.mp4parser.authoring.builder.smoothstreaming;

import com.coremedia.iso.Hex;
import com.coremedia.iso.boxes.OriginalFormatBox;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.SoundMediaHeaderBox;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.coremedia.iso.boxes.VideoMediaHeaderBox;
import com.coremedia.iso.boxes.h264.AvcConfigurationBox;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;
import com.coremedia.iso.boxes.sampleentry.VisualSampleEntry;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.FragmentIntersectionFinder;
import com.googlecode.mp4parser.authoring.builder.SyncSampleIntersectFinderImpl;
import com.googlecode.mp4parser.boxes.mp4.ESDescriptorBox;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

public class FlatManifestWriterImpl implements ManifestWriter {


    private FragmentIntersectionFinder intersectionFinder = new SyncSampleIntersectFinderImpl();
    private long[] audioFragmentsDurations;
    private long[] videoFragmentsDurations;


    public void setIntersectionFinder(FragmentIntersectionFinder intersectionFinder) {
        this.intersectionFinder = intersectionFinder;
    }

    /**
     * Overwrite this method in subclasses to add your specialities.
     *
     * @param manifest the original manifest
     * @return your customized version of the manifest
     */
    protected Document customizeManifest(Document manifest) {
        return manifest;
    }

    public String getManifest(Movie movie) throws IOException {

        LinkedList<VideoQuality> videoQualities = new LinkedList<VideoQuality>();
        long videoTimescale = -1;

        LinkedList<AudioQuality> audioQualities = new LinkedList<AudioQuality>();
        long audioTimescale = -1;



        for (Track track : movie.getTracks()) {
            if (track.getMediaHeaderBox() instanceof VideoMediaHeaderBox) {
                videoFragmentsDurations = checkFragmentsAlign(videoFragmentsDurations, calculateFragmentDurations(track, movie));
                SampleDescriptionBox stsd = track.getSampleDescriptionBox();
                videoQualities.add(getVideoQuality(track, (VisualSampleEntry) stsd.getSampleEntry()));
                if (videoTimescale == -1) {
                    videoTimescale = track.getTrackMetaData().getTimescale();
                } else {
                    assert videoTimescale == track.getTrackMetaData().getTimescale();
                }
            }
            if (track.getMediaHeaderBox() instanceof SoundMediaHeaderBox) {
                audioFragmentsDurations = checkFragmentsAlign(audioFragmentsDurations, calculateFragmentDurations(track, movie));
                SampleDescriptionBox stsd = track.getSampleDescriptionBox();
                audioQualities.add(getAudioQuality(track, (AudioSampleEntry) stsd.getSampleEntry()));
                if (audioTimescale == -1) {
                    audioTimescale = track.getTrackMetaData().getTimescale();
                } else {
                    assert audioTimescale == track.getTrackMetaData().getTimescale();
                }

            }
        }

        Element smoothStreamingMedia = new Element("SmoothStreamingMedia");
        smoothStreamingMedia.addAttribute(new Attribute("MajorVersion", "2"));
        smoothStreamingMedia.addAttribute(new Attribute("MinorVersion", "1"));
// silverlight ignores the timescale attr        smoothStreamingMedia.addAttribute(new Attribute("TimeScale", Long.toString(movieTimeScale)));
        smoothStreamingMedia.addAttribute(new Attribute("Duration", "0"));

        Element videoStreamIndex = new Element("StreamIndex");
        videoStreamIndex.addAttribute(new Attribute("Type", "video"));
        videoStreamIndex.addAttribute(new Attribute("TimeScale", Long.toString(videoTimescale))); // silverlight ignores the timescale attr
        videoStreamIndex.addAttribute(new Attribute("Chunks", Integer.toString(videoFragmentsDurations.length)));
        videoStreamIndex.addAttribute(new Attribute("Url", "video/{bitrate}/{start time}"));
        videoStreamIndex.addAttribute(new Attribute("QualityLevels", Integer.toString(videoQualities.size())));
        smoothStreamingMedia.appendChild(videoStreamIndex);

        for (int i = 0; i < videoQualities.size(); i++) {
            VideoQuality vq = videoQualities.get(i);
            Element qualityLevel = new Element("QualityLevel");
            qualityLevel.addAttribute(new Attribute("Index", Integer.toString(i)));
            qualityLevel.addAttribute(new Attribute("Bitrate", Long.toString(vq.bitrate)));
            qualityLevel.addAttribute(new Attribute("FourCC", vq.fourCC));
            qualityLevel.addAttribute(new Attribute("MaxWidth", Long.toString(vq.width)));
            qualityLevel.addAttribute(new Attribute("MaxHeight", Long.toString(vq.height)));
            qualityLevel.addAttribute(new Attribute("CodecPrivateData", vq.codecPrivateData));
            qualityLevel.addAttribute(new Attribute("NALUnitLengthField", Integer.toString(vq.nalLength)));
            videoStreamIndex.appendChild(qualityLevel);
        }

        for (int i = 0; i < videoFragmentsDurations.length; i++) {
            Element c = new Element("c");
            c.addAttribute(new Attribute("n", Integer.toString(i)));
            c.addAttribute(new Attribute("d", Long.toString((long) (videoFragmentsDurations[i] ))));
            videoStreamIndex.appendChild(c);
        }

        if (audioFragmentsDurations != null) {
            Element audioStreamIndex = new Element("StreamIndex");
            audioStreamIndex.addAttribute(new Attribute("Type", "audio"));
            audioStreamIndex.addAttribute(new Attribute("TimeScale", Long.toString(audioTimescale))); // silverlight ignores the timescale attr
            audioStreamIndex.addAttribute(new Attribute("Chunks", Integer.toString(audioFragmentsDurations.length)));
            audioStreamIndex.addAttribute(new Attribute("Url", "audio/{bitrate}/{start time}"));
            audioStreamIndex.addAttribute(new Attribute("QualityLevels", Integer.toString(audioQualities.size())));
            smoothStreamingMedia.appendChild(audioStreamIndex);

            for (int i = 0; i < audioQualities.size(); i++) {
                AudioQuality aq = audioQualities.get(i);
                Element qualityLevel = new Element("QualityLevel");
                qualityLevel.addAttribute(new Attribute("Index", Integer.toString(i)));
                qualityLevel.addAttribute(new Attribute("Bitrate", Long.toString(aq.bitrate)));
                qualityLevel.addAttribute(new Attribute("AudioTag", Integer.toString(aq.audioTag)));
                qualityLevel.addAttribute(new Attribute("SamplingRate", Long.toString(aq.samplingRate)));
                qualityLevel.addAttribute(new Attribute("Channels", Integer.toString(aq.channels)));
                qualityLevel.addAttribute(new Attribute("BitsPerSample", Integer.toString(aq.bitPerSample)));
                qualityLevel.addAttribute(new Attribute("PacketSize", Integer.toString(aq.packetSize)));
                qualityLevel.addAttribute(new Attribute("CodecPrivateData", aq.codecPrivateData));
                audioStreamIndex.appendChild(qualityLevel);
            }
            for (int i = 0; i < audioFragmentsDurations.length; i++) {
                Element c = new Element("c");
                c.addAttribute(new Attribute("n", Integer.toString(i)));
                c.addAttribute(new Attribute("d", Long.toString((long) (audioFragmentsDurations[i] ))));
                audioStreamIndex.appendChild(c);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Serializer serializer = new Serializer(baos);
        serializer.setIndent(4);
        serializer.write(customizeManifest(new Document(smoothStreamingMedia)));

        return baos.toString("UTF-8");

    }

    private AudioQuality getAudioQuality(Track track, AudioSampleEntry ase) {
        if (getFormat(ase).equals("mp4a")) {
            AudioQuality l = new AudioQuality();
            l.bitrate = getBitrate(track);
            l.audioTag = 255;
            l.samplingRate = ase.getSampleRate();
            l.channels = ase.getChannelCount();
            l.bitPerSample = ase.getSampleSize();
            l.packetSize = 4;
            l.codecPrivateData = getAudioCodecPrivateData(ase.getBoxes(ESDescriptorBox.class).get(0));
            //Index="0" Bitrate="103000" AudioTag="255" SamplingRate="44100" Channels="2" BitsPerSample="16" packetSize="4" CodecPrivateData=""
            return l;
        } else {
            throw new InternalError("I don't know what to do with audio of type " + getFormat(ase));
        }

    }

    public long getBitrate(Track track) {
        long bitrate = 0;
        for (ByteBuffer sample : track.getSamples()) {
            bitrate += sample.limit();
        }
        bitrate *= 8; // from bytes to bits
        bitrate /= ((double) getDuration(track)) / track.getTrackMetaData().getTimescale(); // per second
        return bitrate;
    }


    private String getAudioCodecPrivateData(ESDescriptorBox esDescriptorBox) {

        ByteBuffer configBytes = esDescriptorBox.getEsDescriptor().getDecoderConfigDescriptor().getAudioSpecificInfo().getConfigBytes();
        byte[] configByteArray = new byte[configBytes.limit()];
        configBytes.rewind();
        configBytes.get(configByteArray);
        return Hex.encodeHex(configByteArray);
    }


    private VideoQuality getVideoQuality(Track track, VisualSampleEntry vse) {
        VideoQuality l;
        if ("avc1".equals(getFormat(vse))) {
            AvcConfigurationBox avcConfigurationBox = vse.getBoxes(AvcConfigurationBox.class).get(0);
            l = new VideoQuality();
            l.bitrate = getBitrate(track);
            l.codecPrivateData = Hex.encodeHex(getAvcCodecPrivateData(avcConfigurationBox));
            l.fourCC = "AVC1";
            l.width = vse.getWidth();
            l.height = vse.getHeight();
            l.nalLength = avcConfigurationBox.getLengthSizeMinusOne() + 1;

        } else {
            throw new InternalError("I don't know how to handle video of type " + getFormat(vse));
        }
        return l;
    }

    private long[] checkFragmentsAlign(long[] referenceTimes, long[] checkTimes) throws IOException {

        if (referenceTimes == null || referenceTimes.length == 0) {
            return checkTimes;
        }
        long[] referenceTimesMinusLast = new long[referenceTimes.length - 1];
        System.arraycopy(referenceTimes, 0, referenceTimesMinusLast, 0, referenceTimes.length - 1);
        long[] checkTimesMinusLast = new long[checkTimes.length - 1];
        System.arraycopy(checkTimes, 0, checkTimesMinusLast, 0, checkTimes.length - 1);

        if (!Arrays.equals(checkTimesMinusLast, referenceTimesMinusLast)) {
            System.err.print("Reference     :  [");
            for (long l : checkTimes) {
                System.err.print(l + ",");
            }
            System.err.println("]");


            System.err.print("Current       :  [");
            for (long l : referenceTimes) {
                System.err.print(l + ",");
            }
            System.err.println("]");
            throw new IOException("Track does not have the same fragment borders as its predecessor.");


        } else {
            return checkTimes;
        }
    }

    private byte[] getAvcCodecPrivateData(AvcConfigurationBox avcConfigurationBox) {
        List<byte[]> sps = avcConfigurationBox.getSequenceParameterSets();
        List<byte[]> pps = avcConfigurationBox.getPictureParameterSets();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(new byte[]{0, 0, 0, 1});

            for (byte[] sp : sps) {
                baos.write(sp);
            }
            baos.write(new byte[]{0, 0, 0, 1});
            for (byte[] pp : pps) {
                baos.write(pp);
            }
        } catch (IOException ex) {
            throw new InternalError("ByteArrayOutputStream do not throw IOException ?!?!?");
        }
        return baos.toByteArray();
    }

    private String getFormat(SampleEntry se) {
        String type = se.getType();
        if (type.equals("encv") || type.equals("enca") || type.equals("encv")) {
            OriginalFormatBox frma = se.getBoxes(OriginalFormatBox.class, true).get(0);
            type = frma.getDataFormat();
        }
        return type;
    }

    /**
     * Calculates the length of each fragment in the given <code>track</code> (as part of <code>movie</code>).
     *
     * @param track target of calculation
     * @param movie the <code>track</code> must be part of this <code>movie</code>
     * @return the duration of each fragment in track timescale
     */
    public long[] calculateFragmentDurations(Track track, Movie movie) {
        long[] startSamples = intersectionFinder.sampleNumbers(track, movie);
        long[] durations = new long[startSamples.length];
        int currentFragment = -1;
        int currentSample = 1; // sync samples start with 1 !

        for (TimeToSampleBox.Entry entry : track.getDecodingTimeEntries()) {
            for (int max = currentSample + l2i(entry.getCount()); currentSample <= max; currentSample++) {
                // in this loop we go through the entry.getCount() samples starting from current sample.
                // the next entry.getCount() samples have the same decoding time.
                if (currentFragment != startSamples.length - 1 && currentSample == startSamples[currentFragment + 1]) {
                    // we are not in the last fragment && the current sample is the start sample of the next fragment
                    currentFragment++;
                }
                durations[currentFragment] += entry.getDelta();
            }
        }
        return durations;

    }


    protected static long getDuration(Track track) {
        long duration = 0;
        for (TimeToSampleBox.Entry entry : track.getDecodingTimeEntries()) {
            duration += entry.getCount() * entry.getDelta();
        }
        return duration;
    }


}
