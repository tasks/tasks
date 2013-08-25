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

import static java.lang.Math.round;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.coremedia.iso.boxes.AbstractMediaHeaderBox;
import com.coremedia.iso.boxes.CompositionTimeToSample;
import com.coremedia.iso.boxes.SampleDependencyTypeBox;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.SubSampleInformationBox;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.TrackMetaData;

/**
 * Changes the timescale of a track by wrapping the track.
 */
public class ChangeTimeScaleTrack implements Track {
    Track source;
    List<CompositionTimeToSample.Entry> ctts;
    List<TimeToSampleBox.Entry> tts;
    long timeScale;

    /**
     * Changes the time scale of the source track to the target time scale and makes sure
     * that any rounding errors that may have summed are corrected exactly before the syncSamples.
     *
     * @param source          the source track
     * @param targetTimeScale the resulting time scale of this track.
     * @param syncSamples     at these sync points where rounding error are corrected.
     */
    public ChangeTimeScaleTrack(Track source, long targetTimeScale, long[] syncSamples) {
        this.source = source;
        this.timeScale = targetTimeScale;
        double timeScaleFactor = (double) targetTimeScale / source.getTrackMetaData().getTimescale();
        ctts = adjustCtts(source.getCompositionTimeEntries(), timeScaleFactor);
        tts = adjustTts(source.getDecodingTimeEntries(), timeScaleFactor, syncSamples);
    }

    public SampleDescriptionBox getSampleDescriptionBox() {
        return source.getSampleDescriptionBox();
    }

    public List<TimeToSampleBox.Entry> getDecodingTimeEntries() {
        return tts;
    }

    public List<CompositionTimeToSample.Entry> getCompositionTimeEntries() {
        return ctts;
    }

    public long[] getSyncSamples() {
        return source.getSyncSamples();
    }

    public List<SampleDependencyTypeBox.Entry> getSampleDependencies() {
        return source.getSampleDependencies();
    }

    public TrackMetaData getTrackMetaData() {
        TrackMetaData trackMetaData = (TrackMetaData) source.getTrackMetaData().clone();
        trackMetaData.setTimescale(timeScale);
        return trackMetaData;
    }

    public String getHandler() {
        return source.getHandler();
    }

    public boolean isEnabled() {
        return source.isEnabled();
    }

    public boolean isInMovie() {
        return source.isInMovie();
    }

    public boolean isInPreview() {
        return source.isInPreview();
    }

    public boolean isInPoster() {
        return source.isInPoster();
    }

    public List<ByteBuffer> getSamples() {
        return source.getSamples();
    }


    /**
     * Adjusting the composition times is easy. Just scale it by the factor - that's it. There is no rounding
     * error summing up.
     *
     * @param source
     * @param timeScaleFactor
     * @return
     */
    static List<CompositionTimeToSample.Entry> adjustCtts(List<CompositionTimeToSample.Entry> source, double timeScaleFactor) {
        if (source != null) {
            List<CompositionTimeToSample.Entry> entries2 = new ArrayList<CompositionTimeToSample.Entry>(source.size());
            for (CompositionTimeToSample.Entry entry : source) {
                entries2.add(new CompositionTimeToSample.Entry(entry.getCount(), (int) Math.round(timeScaleFactor * entry.getOffset())));
            }
            return entries2;
        } else {
            return null;
        }
    }

    static List<TimeToSampleBox.Entry> adjustTts(List<TimeToSampleBox.Entry> source, double timeScaleFactor, long[] syncSample) {
        double deviation = 0;
        long[] sourceArray = TimeToSampleBox.blowupTimeToSamples(source);
        LinkedList<TimeToSampleBox.Entry> entries2 = new LinkedList<TimeToSampleBox.Entry>();
        for (int i = 0; i < sourceArray.length; i++) {
            long duration = sourceArray[i];
            double d = timeScaleFactor * duration;
            long x = round(d);
            deviation += d - x;
            TimeToSampleBox.Entry last = entries2.getLast();
            if (Arrays.binarySearch(syncSample, i + 1) >= 0) {
                // apply correction here!
                if (Math.abs(deviation) >= 1) {
                    //System.err.println("Sample " + i + " corrected by adding + " + Math.round(deviation) );
                    x += Math.round(deviation);
                    deviation = deviation - Math.round(deviation); // there is a rest!

                }
            }
            if (last == null) {
                entries2.add(new TimeToSampleBox.Entry(1, x));
            } else if (last.getDelta() != x) {
                entries2.add(new TimeToSampleBox.Entry(1, x));
            } else {
                last.setCount(last.getCount() + 1);
            }

        }
        return entries2;
    }

    public AbstractMediaHeaderBox getMediaHeaderBox() {
        return source.getMediaHeaderBox();
    }

    public SubSampleInformationBox getSubsampleInformationBox() {
        return source.getSubsampleInformationBox();
    }

    @Override
    public String toString() {
        return "ChangeTimeScaleTrack{" +
                "source=" + source +
                '}';
    }
}