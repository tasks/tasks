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

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Represents a Track. A track is a timed sequence of related samples.
 * <p/>
 * <b>NOTE: </b><br/
 * For media data, a track corresponds to a sequence of images or sampled audio; for hint tracks, a track
 * corresponds to a streaming channel.
 */
public interface Track {

    SampleDescriptionBox getSampleDescriptionBox();

    List<TimeToSampleBox.Entry> getDecodingTimeEntries();

    List<CompositionTimeToSample.Entry> getCompositionTimeEntries();

    long[] getSyncSamples();

    List<SampleDependencyTypeBox.Entry> getSampleDependencies();

    TrackMetaData getTrackMetaData();

    String getHandler();

    boolean isEnabled();

    boolean isInMovie();

    boolean isInPreview();

    boolean isInPoster();

    List<ByteBuffer> getSamples();

    public AbstractMediaHeaderBox getMediaHeaderBox();

    public SubSampleInformationBox getSubsampleInformationBox();

}
