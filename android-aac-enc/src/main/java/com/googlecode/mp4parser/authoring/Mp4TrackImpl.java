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
package com.googlecode.mp4parser.authoring;

import com.coremedia.iso.boxes.*;
import com.coremedia.iso.boxes.fragment.MovieExtendsBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.coremedia.iso.boxes.mdat.SampleList;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * Represents a single track of an MP4 file.
 */
public class Mp4TrackImpl extends AbstractTrack {
    private List<ByteBuffer> samples;
    private SampleDescriptionBox sampleDescriptionBox;
    private List<TimeToSampleBox.Entry> decodingTimeEntries;
    private List<CompositionTimeToSample.Entry> compositionTimeEntries;
    private long[] syncSamples;
    private List<SampleDependencyTypeBox.Entry> sampleDependencies;
    private TrackMetaData trackMetaData = new TrackMetaData();
    private String handler;
    private AbstractMediaHeaderBox mihd;

    public Mp4TrackImpl(TrackBox trackBox) {
        samples = new SampleList(trackBox);
        SampleTableBox stbl = trackBox.getMediaBox().getMediaInformationBox().getSampleTableBox();
        handler = trackBox.getMediaBox().getHandlerBox().getHandlerType();


        mihd = trackBox.getMediaBox().getMediaInformationBox().getMediaHeaderBox();


        sampleDescriptionBox = stbl.getSampleDescriptionBox();
        if (trackBox.getParent().getBoxes(MovieExtendsBox.class).size() > 0) {

            decodingTimeEntries = new LinkedList<TimeToSampleBox.Entry>();
            compositionTimeEntries = new LinkedList<CompositionTimeToSample.Entry>();
            sampleDependencies = new LinkedList<SampleDependencyTypeBox.Entry>();

            for (MovieFragmentBox movieFragmentBox : trackBox.getIsoFile().getBoxes(MovieFragmentBox.class)) {
                List<TrackFragmentBox> trafs = movieFragmentBox.getBoxes(TrackFragmentBox.class);
                for (TrackFragmentBox traf : trafs) {
                    if (traf.getTrackFragmentHeaderBox().getTrackId() == trackBox.getTrackHeaderBox().getTrackId()) {
                        List<TrackRunBox> truns = traf.getBoxes(TrackRunBox.class);
                        for (TrackRunBox trun : truns) {
                            for (TrackRunBox.Entry entry : trun.getEntries()) {
                                if (trun.isSampleDurationPresent()) {
                                    if (decodingTimeEntries.size() == 0 ||
                                            decodingTimeEntries.get(decodingTimeEntries.size() - 1).getDelta() != entry.getSampleDuration()) {
                                        decodingTimeEntries.add(new TimeToSampleBox.Entry(1, entry.getSampleDuration()));
                                    } else {
                                        TimeToSampleBox.Entry e = decodingTimeEntries.get(decodingTimeEntries.size() - 1);
                                        e.setCount(e.getCount() + 1);
                                    }
                                }
                                if (trun.isSampleCompositionTimeOffsetPresent()) {
                                    if (compositionTimeEntries.size() == 0 ||
                                            compositionTimeEntries.get(compositionTimeEntries.size() - 1).getOffset() != entry.getSampleCompositionTimeOffset()) {
                                        compositionTimeEntries.add(new CompositionTimeToSample.Entry(1, l2i(entry.getSampleCompositionTimeOffset())));
                                    } else {
                                        CompositionTimeToSample.Entry e = compositionTimeEntries.get(compositionTimeEntries.size() - 1);
                                        e.setCount(e.getCount() + 1);
                                    }
                                }

                            }


                        }


                    }
                }
            }
        } else {
            decodingTimeEntries = stbl.getTimeToSampleBox().getEntries();
            if (stbl.getCompositionTimeToSample() != null) {
                compositionTimeEntries = stbl.getCompositionTimeToSample().getEntries();
            }
            if (stbl.getSyncSampleBox() != null) {
                syncSamples = stbl.getSyncSampleBox().getSampleNumber();
            }
            if (stbl.getSampleDependencyTypeBox() != null) {
                sampleDependencies = stbl.getSampleDependencyTypeBox().getEntries();
            }
        }
        MediaHeaderBox mdhd = trackBox.getMediaBox().getMediaHeaderBox();
        TrackHeaderBox tkhd = trackBox.getTrackHeaderBox();

        setEnabled(tkhd.isEnabled());
        setInMovie(tkhd.isInMovie());
        setInPoster(tkhd.isInPoster());
        setInPreview(tkhd.isInPreview());

        trackMetaData.setTrackId(tkhd.getTrackId());
        trackMetaData.setCreationTime(DateHelper.convert(mdhd.getCreationTime()));
        trackMetaData.setLanguage(mdhd.getLanguage());
/*        System.err.println(mdhd.getModificationTime());
        System.err.println(DateHelper.convert(mdhd.getModificationTime()));
        System.err.println(DateHelper.convert(DateHelper.convert(mdhd.getModificationTime())));
        System.err.println(DateHelper.convert(DateHelper.convert(DateHelper.convert(mdhd.getModificationTime()))));*/

        trackMetaData.setModificationTime(DateHelper.convert(mdhd.getModificationTime()));
        trackMetaData.setTimescale(mdhd.getTimescale());
        trackMetaData.setHeight(tkhd.getHeight());
        trackMetaData.setWidth(tkhd.getWidth());
        trackMetaData.setLayer(tkhd.getLayer());
    }

    public List<ByteBuffer> getSamples() {
        return samples;
    }


    public SampleDescriptionBox getSampleDescriptionBox() {
        return sampleDescriptionBox;
    }

    public List<TimeToSampleBox.Entry> getDecodingTimeEntries() {
        return decodingTimeEntries;
    }

    public List<CompositionTimeToSample.Entry> getCompositionTimeEntries() {
        return compositionTimeEntries;
    }

    public long[] getSyncSamples() {
        return syncSamples;
    }

    public List<SampleDependencyTypeBox.Entry> getSampleDependencies() {
        return sampleDependencies;
    }

    public TrackMetaData getTrackMetaData() {
        return trackMetaData;
    }

    public String getHandler() {
        return handler;
    }

    public AbstractMediaHeaderBox getMediaHeaderBox() {
        return mihd;
    }

    public SubSampleInformationBox getSubsampleInformationBox() {
        return null;
    }

    @Override
    public String toString() {
        return "Mp4TrackImpl{" +
                "handler='" + handler + '\'' +
                '}';
    }
}
