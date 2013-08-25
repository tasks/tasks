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
package com.googlecode.mp4parser.authoring.tracks;

import com.coremedia.iso.boxes.*;
import com.googlecode.mp4parser.authoring.AbstractTrack;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.TrackMetaData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Appends two or more <code>Tracks</code> of the same type. No only that the type must be equal
 * also the decoder settings must be the same.
 */
public class AppendTrack extends AbstractTrack {
    Track[] tracks;

    public AppendTrack(Track... tracks) throws IOException {
        this.tracks = tracks;
        byte[] referenceSampleDescriptionBox = null;
        for (Track track : tracks) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            track.getSampleDescriptionBox().getBox(Channels.newChannel(baos));
            if (referenceSampleDescriptionBox == null) {
                referenceSampleDescriptionBox = baos.toByteArray();
            } else if (!Arrays.equals(referenceSampleDescriptionBox, baos.toByteArray())) {
                throw new IOException("Cannot append " + track + " to " + tracks[0] + " since their Sample Description Boxes differ");
            }
        }
    }

    public List<ByteBuffer> getSamples() {
        ArrayList<ByteBuffer> lists = new ArrayList<ByteBuffer>();

        for (Track track : tracks) {
            lists.addAll(track.getSamples());
        }

        return lists;
    }

    public SampleDescriptionBox getSampleDescriptionBox() {
        return tracks[0].getSampleDescriptionBox();
    }

    public List<TimeToSampleBox.Entry> getDecodingTimeEntries() {
        if (tracks[0].getDecodingTimeEntries() != null && !tracks[0].getDecodingTimeEntries().isEmpty()) {
            List<long[]> lists = new LinkedList<long[]>();
            for (Track track : tracks) {
                lists.add(TimeToSampleBox.blowupTimeToSamples(track.getDecodingTimeEntries()));
            }

            LinkedList<TimeToSampleBox.Entry> returnDecodingEntries = new LinkedList<TimeToSampleBox.Entry>();
            for (long[] list : lists) {
                for (long nuDecodingTime : list) {
                    if (returnDecodingEntries.isEmpty() || returnDecodingEntries.getLast().getDelta() != nuDecodingTime) {
                        TimeToSampleBox.Entry e = new TimeToSampleBox.Entry(1, nuDecodingTime);
                        returnDecodingEntries.add(e);
                    } else {
                        TimeToSampleBox.Entry e = returnDecodingEntries.getLast();
                        e.setCount(e.getCount() + 1);
                    }
                }
            }
            return returnDecodingEntries;
        } else {
            return null;
        }
    }

    public List<CompositionTimeToSample.Entry> getCompositionTimeEntries() {
        if (tracks[0].getCompositionTimeEntries() != null && !tracks[0].getCompositionTimeEntries().isEmpty()) {
            List<int[]> lists = new LinkedList<int[]>();
            for (Track track : tracks) {
                lists.add(CompositionTimeToSample.blowupCompositionTimes(track.getCompositionTimeEntries()));
            }
            LinkedList<CompositionTimeToSample.Entry> compositionTimeEntries = new LinkedList<CompositionTimeToSample.Entry>();
            for (int[] list : lists) {
                for (int compositionTime : list) {
                    if (compositionTimeEntries.isEmpty() || compositionTimeEntries.getLast().getOffset() != compositionTime) {
                        CompositionTimeToSample.Entry e = new CompositionTimeToSample.Entry(1, compositionTime);
                        compositionTimeEntries.add(e);
                    } else {
                        CompositionTimeToSample.Entry e = compositionTimeEntries.getLast();
                        e.setCount(e.getCount() + 1);
                    }
                }
            }
            return compositionTimeEntries;
        } else {
            return null;
        }
    }

    public long[] getSyncSamples() {
        if (tracks[0].getSyncSamples() != null && tracks[0].getSyncSamples().length > 0) {
            int numSyncSamples = 0;
            for (Track track : tracks) {
                numSyncSamples += track.getSyncSamples().length;
            }
            long[] returnSyncSamples = new long[numSyncSamples];

            int pos = 0;
            long samplesBefore = 0;
            for (Track track : tracks) {
                for (long l : track.getSyncSamples()) {
                    returnSyncSamples[pos++] = samplesBefore + l;
                }
                samplesBefore += track.getSamples().size();
            }
            return returnSyncSamples;
        } else {
            return null;
        }
    }

    public List<SampleDependencyTypeBox.Entry> getSampleDependencies() {
        if (tracks[0].getSampleDependencies() != null && !tracks[0].getSampleDependencies().isEmpty()) {
            List<SampleDependencyTypeBox.Entry> list = new LinkedList<SampleDependencyTypeBox.Entry>();
            for (Track track : tracks) {
                list.addAll(track.getSampleDependencies());
            }
            return list;
        } else {
            return null;
        }
    }

    public TrackMetaData getTrackMetaData() {
        return tracks[0].getTrackMetaData();
    }

    public String getHandler() {
        return tracks[0].getHandler();
    }

    public AbstractMediaHeaderBox getMediaHeaderBox() {
        return tracks[0].getMediaHeaderBox();
    }

    public SubSampleInformationBox getSubsampleInformationBox() {
        return tracks[0].getSubsampleInformationBox();
    }

}
