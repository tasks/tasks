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
import com.googlecode.mp4parser.authoring.TrackMetaData;
import com.googlecode.mp4parser.boxes.adobe.ActionMessageFormat0SampleEntryBox;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Amf0Track extends AbstractTrack {
    SortedMap<Long, byte[]> rawSamples = new TreeMap<Long, byte[]>() {
    };
    private TrackMetaData trackMetaData = new TrackMetaData();


    /**
     * Creates a new AMF0 track from
     *
     * @param rawSamples
     */
    public Amf0Track(Map<Long, byte[]> rawSamples) {
        this.rawSamples = new TreeMap<Long, byte[]>(rawSamples);
        trackMetaData.setCreationTime(new Date());
        trackMetaData.setModificationTime(new Date());
        trackMetaData.setTimescale(1000); // Text tracks use millieseconds
        trackMetaData.setLanguage("eng");
    }

    public List<ByteBuffer> getSamples() {
        LinkedList<ByteBuffer> samples = new LinkedList<ByteBuffer>();
        for (byte[] bytes : rawSamples.values()) {
            samples.add(ByteBuffer.wrap(bytes));
        }
        return samples;
    }

    public SampleDescriptionBox getSampleDescriptionBox() {
        SampleDescriptionBox stsd = new SampleDescriptionBox();
        ActionMessageFormat0SampleEntryBox amf0 = new ActionMessageFormat0SampleEntryBox();
        amf0.setDataReferenceIndex(1);
        stsd.addBox(amf0);
        return stsd;
    }

    public List<TimeToSampleBox.Entry> getDecodingTimeEntries() {
        LinkedList<TimeToSampleBox.Entry> timesToSample = new LinkedList<TimeToSampleBox.Entry>();
        LinkedList<Long> keys = new LinkedList<Long>(rawSamples.keySet());
        Collections.sort(keys);
        long lastTimeStamp = 0;
        for (Long key : keys) {
            long delta = key - lastTimeStamp;
            if (timesToSample.size() > 0 && timesToSample.peek().getDelta() == delta) {
                timesToSample.peek().setCount(timesToSample.peek().getCount() + 1);
            } else {
                timesToSample.add(new TimeToSampleBox.Entry(1, delta));
            }
            lastTimeStamp = key;
        }
        return timesToSample;
    }

    public List<CompositionTimeToSample.Entry> getCompositionTimeEntries() {
        // AMF0 tracks do not have Composition Time
        return null;
    }

    public long[] getSyncSamples() {
        // AMF0 tracks do not have Sync Samples
        return null;
    }

    public List<SampleDependencyTypeBox.Entry> getSampleDependencies() {
        // AMF0 tracks do not have Sample Dependencies
        return null;
    }

    public TrackMetaData getTrackMetaData() {
        return trackMetaData;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getHandler() {
        return "data";
    }

    public AbstractMediaHeaderBox getMediaHeaderBox() {
        return new NullMediaHeaderBox();
    }

    public SubSampleInformationBox getSubsampleInformationBox() {
        return null;
    }

}
