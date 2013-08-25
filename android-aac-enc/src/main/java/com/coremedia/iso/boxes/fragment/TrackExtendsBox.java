/*
 * Copyright 2009 castLabs GmbH, Berlin
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

package com.coremedia.iso.boxes.fragment;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;

/**
 * aligned(8) class TrackExtendsBox extends FullBox('trex', 0, 0){
 * unsigned int(32) track_ID;
 * unsigned int(32) default_sample_description_index;
 * unsigned int(32) default_sample_duration;
 * unsigned int(32) default_sample_size;
 * unsigned int(32) default_sample_flags
 * }
 */
public class TrackExtendsBox extends AbstractFullBox {
    public static final String TYPE = "trex";
    private long trackId;
    private long defaultSampleDescriptionIndex;
    private long defaultSampleDuration;
    private long defaultSampleSize;
    private SampleFlags defaultSampleFlags;

    public TrackExtendsBox() {
        super(TYPE);
    }

    @Override
    protected long getContentSize() {
        return 5 * 4 + 4;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, trackId);
        IsoTypeWriter.writeUInt32(byteBuffer, defaultSampleDescriptionIndex);
        IsoTypeWriter.writeUInt32(byteBuffer, defaultSampleDuration);
        IsoTypeWriter.writeUInt32(byteBuffer, defaultSampleSize);
        defaultSampleFlags.getContent(byteBuffer);
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        trackId = IsoTypeReader.readUInt32(content);
        defaultSampleDescriptionIndex = IsoTypeReader.readUInt32(content);
        defaultSampleDuration = IsoTypeReader.readUInt32(content);
        defaultSampleSize = IsoTypeReader.readUInt32(content);
        defaultSampleFlags = new SampleFlags(content);
    }

    public long getTrackId() {
        return trackId;
    }

    public long getDefaultSampleDescriptionIndex() {
        return defaultSampleDescriptionIndex;
    }

    public long getDefaultSampleDuration() {
        return defaultSampleDuration;
    }

    public long getDefaultSampleSize() {
        return defaultSampleSize;
    }

    public SampleFlags getDefaultSampleFlags() {
        return defaultSampleFlags;
    }

    public String getDefaultSampleFlagsStr() {
        return defaultSampleFlags.toString();
    }

    public void setTrackId(long trackId) {
        this.trackId = trackId;
    }

    public void setDefaultSampleDescriptionIndex(long defaultSampleDescriptionIndex) {
        this.defaultSampleDescriptionIndex = defaultSampleDescriptionIndex;
    }

    public void setDefaultSampleDuration(long defaultSampleDuration) {
        this.defaultSampleDuration = defaultSampleDuration;
    }

    public void setDefaultSampleSize(long defaultSampleSize) {
        this.defaultSampleSize = defaultSampleSize;
    }

    public void setDefaultSampleFlags(SampleFlags defaultSampleFlags) {
        this.defaultSampleFlags = defaultSampleFlags;

    }
}
