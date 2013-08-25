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
 * aligned(8) class TrackFragmentHeaderBox
 * extends FullBox('tfhd', 0, tf_flags){
 * unsigned int(32) track_ID;
 * // all the following are optional fields
 * unsigned int(64) base_data_offset;
 * unsigned int(32) sample_description_index;
 * unsigned int(32) default_sample_duration;
 * unsigned int(32) default_sample_size;
 * unsigned int(32) default_sample_flags
 * }
 */
public class TrackFragmentHeaderBox extends AbstractFullBox {
    public static final String TYPE = "tfhd";

    private long trackId;
    private long baseDataOffset = -1;
    private long sampleDescriptionIndex;
    private long defaultSampleDuration = -1;
    private long defaultSampleSize = -1;
    private SampleFlags defaultSampleFlags;
    private boolean durationIsEmpty;

    public TrackFragmentHeaderBox() {
        super(TYPE);
    }

    protected long getContentSize() {
        long size = 8;
        int flags = getFlags();
        if ((flags & 0x1) == 1) { //baseDataOffsetPresent
            size += 8;
        }
        if ((flags & 0x2) == 0x2) { //sampleDescriptionIndexPresent
            size += 4;
        }
        if ((flags & 0x8) == 0x8) { //defaultSampleDurationPresent
            size += 4;
        }
        if ((flags & 0x10) == 0x10) { //defaultSampleSizePresent
            size += 4;
        }
        if ((flags & 0x20) == 0x20) { //defaultSampleFlagsPresent
            size += 4;
        }
        return size;
    }


    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, trackId);

        if ((getFlags() & 0x1) == 1) { //baseDataOffsetPresent
            IsoTypeWriter.writeUInt64(byteBuffer, getBaseDataOffset());
        }
        if ((getFlags() & 0x2) == 0x2) { //sampleDescriptionIndexPresent
            IsoTypeWriter.writeUInt32(byteBuffer, getSampleDescriptionIndex());
        }
        if ((getFlags() & 0x8) == 0x8) { //defaultSampleDurationPresent
            IsoTypeWriter.writeUInt32(byteBuffer, getDefaultSampleDuration());
        }
        if ((getFlags() & 0x10) == 0x10) { //defaultSampleSizePresent
            IsoTypeWriter.writeUInt32(byteBuffer, getDefaultSampleSize());
        }
        if ((getFlags() & 0x20) == 0x20) { //defaultSampleFlagsPresent
            defaultSampleFlags.getContent(byteBuffer);
        }
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        trackId = IsoTypeReader.readUInt32(content);
        if ((getFlags() & 0x1) == 1) { //baseDataOffsetPresent
            baseDataOffset = IsoTypeReader.readUInt64(content);
        }
        if ((getFlags() & 0x2) == 0x2) { //sampleDescriptionIndexPresent
            sampleDescriptionIndex = IsoTypeReader.readUInt32(content);
        }
        if ((getFlags() & 0x8) == 0x8) { //defaultSampleDurationPresent
            defaultSampleDuration = IsoTypeReader.readUInt32(content);
        }
        if ((getFlags() & 0x10) == 0x10) { //defaultSampleSizePresent
            defaultSampleSize = IsoTypeReader.readUInt32(content);
        }
        if ((getFlags() & 0x20) == 0x20) { //defaultSampleFlagsPresent
            defaultSampleFlags = new SampleFlags(content);
        }
        if ((getFlags() & 0x10000) == 0x10000) { //durationIsEmpty
            durationIsEmpty = true;
        }
    }

    public boolean hasBaseDataOffset() {
        return (getFlags() & 0x1) != 0;
    }

    public long getTrackId() {
        return trackId;
    }

    public long getBaseDataOffset() {
        return baseDataOffset;
    }

    public long getSampleDescriptionIndex() {
        return sampleDescriptionIndex;
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

    public boolean isDurationIsEmpty() {
        return durationIsEmpty;
    }

    public void setTrackId(long trackId) {
        this.trackId = trackId;
    }

    public void setBaseDataOffset(long baseDataOffset) {
        if (baseDataOffset == -1) {
            setFlags(getFlags() & (Integer.MAX_VALUE ^ 0x1));
        } else {
            setFlags(getFlags() | 0x1); // activate the field
        }
        this.baseDataOffset = baseDataOffset;
    }

    public void setSampleDescriptionIndex(long sampleDescriptionIndex) {
        if (sampleDescriptionIndex == -1) {
            setFlags(getFlags() & (Integer.MAX_VALUE ^ 0x2));
        } else {
            setFlags(getFlags() | 0x2); // activate the field
        }
        this.sampleDescriptionIndex = sampleDescriptionIndex;
    }

    public void setDefaultSampleDuration(long defaultSampleDuration) {
        setFlags(getFlags() | 0x8); // activate the field
        this.defaultSampleDuration = defaultSampleDuration;
    }

    public void setDefaultSampleSize(long defaultSampleSize) {
        setFlags(getFlags() | 0x10); // activate the field
        this.defaultSampleSize = defaultSampleSize;
    }

    public void setDefaultSampleFlags(SampleFlags defaultSampleFlags) {
        setFlags(getFlags() | 0x20); // activate the field
        this.defaultSampleFlags = defaultSampleFlags;
    }

    public void setDurationIsEmpty(boolean durationIsEmpty) {
        setFlags(getFlags() | 0x10000); // activate the field
        this.durationIsEmpty = durationIsEmpty;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TrackFragmentHeaderBox");
        sb.append("{trackId=").append(trackId);
        sb.append(", baseDataOffset=").append(baseDataOffset);
        sb.append(", sampleDescriptionIndex=").append(sampleDescriptionIndex);
        sb.append(", defaultSampleDuration=").append(defaultSampleDuration);
        sb.append(", defaultSampleSize=").append(defaultSampleSize);
        sb.append(", defaultSampleFlags=").append(defaultSampleFlags);
        sb.append(", durationIsEmpty=").append(durationIsEmpty);
        sb.append('}');
        return sb.toString();
    }

}
