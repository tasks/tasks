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

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 * Generates a Track that starts at fromSample and ends at toSample (exclusive). The user of this class
 * has to make sure that the fromSample is a random access sample.
 * <ul>
 * <li>In AAC this is every single sample</li>
 * <li>In H264 this is every sample that is marked in the SyncSampleBox</li>
 * </ul>
 */
public class CroppedTrack extends AbstractTrack {
    Track origTrack;
    private int fromSample;
    private int toSample;
    private long[] syncSampleArray;

    public CroppedTrack(Track origTrack, long fromSample, long toSample) {
        this.origTrack = origTrack;
        assert fromSample <= Integer.MAX_VALUE;
        assert toSample <= Integer.MAX_VALUE;
        this.fromSample = (int) fromSample;
        this.toSample = (int) toSample;
    }

    public List<ByteBuffer> getSamples() {
        return origTrack.getSamples().subList(fromSample, toSample);
    }

    public SampleDescriptionBox getSampleDescriptionBox() {
        return origTrack.getSampleDescriptionBox();
    }

    public List<TimeToSampleBox.Entry> getDecodingTimeEntries() {
        if (origTrack.getDecodingTimeEntries() != null && !origTrack.getDecodingTimeEntries().isEmpty()) {
            long[] decodingTimes = TimeToSampleBox.blowupTimeToSamples(origTrack.getDecodingTimeEntries());
            long[] nuDecodingTimes = new long[toSample - fromSample];
            System.arraycopy(decodingTimes, fromSample, nuDecodingTimes, 0, toSample - fromSample);

            LinkedList<TimeToSampleBox.Entry> returnDecodingEntries = new LinkedList<TimeToSampleBox.Entry>();

            for (long nuDecodingTime : nuDecodingTimes) {
                if (returnDecodingEntries.isEmpty() || returnDecodingEntries.getLast().getDelta() != nuDecodingTime) {
                    TimeToSampleBox.Entry e = new TimeToSampleBox.Entry(1, nuDecodingTime);
                    returnDecodingEntries.add(e);
                } else {
                    TimeToSampleBox.Entry e = returnDecodingEntries.getLast();
                    e.setCount(e.getCount() + 1);
                }
            }
            return returnDecodingEntries;
        } else {
            return null;
        }
    }

    public List<CompositionTimeToSample.Entry> getCompositionTimeEntries() {
        if (origTrack.getCompositionTimeEntries() != null && !origTrack.getCompositionTimeEntries().isEmpty()) {
            int[] compositionTime = CompositionTimeToSample.blowupCompositionTimes(origTrack.getCompositionTimeEntries());
            int[] nuCompositionTimes = new int[toSample - fromSample];
            System.arraycopy(compositionTime, fromSample, nuCompositionTimes, 0, toSample - fromSample);

            LinkedList<CompositionTimeToSample.Entry> returnDecodingEntries = new LinkedList<CompositionTimeToSample.Entry>();

            for (int nuDecodingTime : nuCompositionTimes) {
                if (returnDecodingEntries.isEmpty() || returnDecodingEntries.getLast().getOffset() != nuDecodingTime) {
                    CompositionTimeToSample.Entry e = new CompositionTimeToSample.Entry(1, nuDecodingTime);
                    returnDecodingEntries.add(e);
                } else {
                    CompositionTimeToSample.Entry e = returnDecodingEntries.getLast();
                    e.setCount(e.getCount() + 1);
                }
            }
            return returnDecodingEntries;
        } else {
            return null;
        }
    }

    synchronized public long[] getSyncSamples() {
        if (this.syncSampleArray == null) {
            if (origTrack.getSyncSamples() != null && origTrack.getSyncSamples().length > 0) {
                List<Long> syncSamples = new LinkedList<Long>();
                for (long l : origTrack.getSyncSamples()) {
                    if (l >= fromSample && l < toSample) {
                        syncSamples.add(l - fromSample);
                    }
                }
                syncSampleArray = new long[syncSamples.size()];
                for (int i = 0; i < syncSampleArray.length; i++) {
                    syncSampleArray[i] = syncSamples.get(i);

                }
                return syncSampleArray;
            } else {
                return null;
            }
        } else {
            return this.syncSampleArray;
        }
    }

    public List<SampleDependencyTypeBox.Entry> getSampleDependencies() {
        if (origTrack.getSampleDependencies() != null && !origTrack.getSampleDependencies().isEmpty()) {
            return origTrack.getSampleDependencies().subList(fromSample, toSample);
        } else {
            return null;
        }
    }

    public TrackMetaData getTrackMetaData() {
        return origTrack.getTrackMetaData();
    }

    public String getHandler() {
        return origTrack.getHandler();
    }

    public AbstractMediaHeaderBox getMediaHeaderBox() {
        return origTrack.getMediaHeaderBox();
    }

    public SubSampleInformationBox getSubsampleInformationBox() {
        return origTrack.getSubsampleInformationBox();
    }

}