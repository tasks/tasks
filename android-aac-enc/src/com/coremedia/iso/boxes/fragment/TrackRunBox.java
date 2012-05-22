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
import java.util.ArrayList;
import java.util.List;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * aligned(8) class TrackRunBox
 * extends FullBox('trun', 0, tr_flags) {
 * unsigned int(32) sample_count;
 * // the following are optional fields
 * signed int(32) data_offset;
 * unsigned int(32) first_sample_flags;
 * // all fields in the following array are optional
 * {
 * unsigned int(32) sample_duration;
 * unsigned int(32) sample_size;
 * unsigned int(32) sample_flags
 * unsigned int(32) sample_composition_time_offset;
 * }[ sample_count ]
 * }
 */

public class TrackRunBox extends AbstractFullBox {
    public static final String TYPE = "trun";
    private int dataOffset;
    private SampleFlags firstSampleFlags;
    private List<Entry> entries = new ArrayList<Entry>();


    public List<Entry> getEntries() {
        return entries;
    }

    public static class Entry {
        private long sampleDuration;
        private long sampleSize;
        private SampleFlags sampleFlags;
        private int sampleCompositionTimeOffset;

        public Entry() {
        }

        public Entry(long sampleDuration, long sampleSize, SampleFlags sampleFlags, int sampleCompositionTimeOffset) {
            this.sampleDuration = sampleDuration;
            this.sampleSize = sampleSize;
            this.sampleFlags = sampleFlags;
            this.sampleCompositionTimeOffset = sampleCompositionTimeOffset;
        }

        public long getSampleDuration() {
            return sampleDuration;
        }

        public long getSampleSize() {
            return sampleSize;
        }

        public SampleFlags getSampleFlags() {
            return sampleFlags;
        }

        public int getSampleCompositionTimeOffset() {
            return sampleCompositionTimeOffset;
        }

        public void setSampleDuration(long sampleDuration) {
            this.sampleDuration = sampleDuration;
        }

        public void setSampleSize(long sampleSize) {
            this.sampleSize = sampleSize;
        }

        public void setSampleFlags(SampleFlags sampleFlags) {
            this.sampleFlags = sampleFlags;
        }

        public void setSampleCompositionTimeOffset(int sampleCompositionTimeOffset) {
            this.sampleCompositionTimeOffset = sampleCompositionTimeOffset;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "sampleDuration=" + sampleDuration +
                    ", sampleSize=" + sampleSize +
                    ", sampleFlags=" + sampleFlags +
                    ", sampleCompositionTimeOffset=" + sampleCompositionTimeOffset +
                    '}';
        }
    }

    public void setDataOffset(int dataOffset) {
        if (dataOffset == -1) {
            setFlags(getFlags() & (0xFFFFFF ^ 1));
        } else {
            setFlags(getFlags() | 0x1); // turn on dataoffset
        }
        this.dataOffset = dataOffset;
    }

    public long[] getSampleOffsets() {
        long[] result = new long[entries.size()];

        long offset = 0;
        for (int i = 0; i < result.length; i++) {
            result[i] = offset;
            if (isSampleSizePresent()) {
                offset += entries.get(i).getSampleSize();
            } else {
                offset += ((TrackFragmentBox) getParent()).getTrackFragmentHeaderBox().getDefaultSampleSize();
            }
        }

        return result;
    }

    public long[] getSampleSizes() {
        long[] result = new long[entries.size()];

        for (int i = 0; i < result.length; i++) {
            if (isSampleSizePresent()) {
                result[i] = entries.get(i).getSampleSize();
            } else {
                result[i] = ((TrackFragmentBox) getParent()).getTrackFragmentHeaderBox().getDefaultSampleSize();
            }
        }

        return result;
    }

    public long[] getSampleCompositionTimeOffsets() {
        if (isSampleCompositionTimeOffsetPresent()) {
            long[] result = new long[entries.size()];

            for (int i = 0; i < result.length; i++) {
                result[i] = entries.get(i).getSampleCompositionTimeOffset();
            }
            return result;
        }
        return null;
    }

    public long[] getSampleDurations() {
        long[] result = new long[entries.size()];

        for (int i = 0; i < result.length; i++) {
            if (isSampleDurationPresent()) {
                result[i] = entries.get(i).getSampleDuration();
            } else {
                result[i] = ((TrackFragmentBox) getParent()).getTrackFragmentHeaderBox().getDefaultSampleDuration();
            }
        }

        return result;
    }

    public TrackRunBox() {
        super(TYPE);
    }

    protected long getContentSize() {
        long size = 8;
        int flags = getFlags();

        if ((flags & 0x1) == 0x1) { //dataOffsetPresent
            size += 4;
        }
        if ((flags & 0x4) == 0x4) { //firstSampleFlagsPresent
            size += 4;
        }

        long entrySize = 0;
        if ((flags & 0x100) == 0x100) { //sampleDurationPresent
            entrySize += 4;
        }
        if ((flags & 0x200) == 0x200) { //sampleSizePresent
            entrySize += 4;
        }
        if ((flags & 0x400) == 0x400) { //sampleFlagsPresent
            entrySize += 4;
        }
        if ((flags & 0x800) == 0x800) { //sampleCompositionTimeOffsetPresent
            entrySize += 4;
        }
        size += entrySize * entries.size();
        return size;
    }

    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, entries.size());
        int flags = getFlags();

        if ((flags & 0x1) == 1) { //dataOffsetPresent
            IsoTypeWriter.writeUInt32(byteBuffer, dataOffset);
        }
        if ((flags & 0x4) == 0x4) { //firstSampleFlagsPresent
            firstSampleFlags.getContent(byteBuffer);
        }

        for (Entry entry : entries) {
            if ((flags & 0x100) == 0x100) { //sampleDurationPresent
                IsoTypeWriter.writeUInt32(byteBuffer, entry.sampleDuration);
            }
            if ((flags & 0x200) == 0x200) { //sampleSizePresent
                IsoTypeWriter.writeUInt32(byteBuffer, entry.sampleSize);
            }
            if ((flags & 0x400) == 0x400) { //sampleFlagsPresent
                entry.sampleFlags.getContent(byteBuffer);
            }
            if ((flags & 0x800) == 0x800) { //sampleCompositionTimeOffsetPresent
                byteBuffer.putInt(entry.sampleCompositionTimeOffset);
            }
        }
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        long sampleCount = IsoTypeReader.readUInt32(content);

        if ((getFlags() & 0x1) == 1) { //dataOffsetPresent
            dataOffset = l2i(IsoTypeReader.readUInt32(content));
        } else {
            dataOffset = -1;
        }
        if ((getFlags() & 0x4) == 0x4) { //firstSampleFlagsPresent
            firstSampleFlags = new SampleFlags(content);
        }

        for (int i = 0; i < sampleCount; i++) {
            Entry entry = new Entry();
            if ((getFlags() & 0x100) == 0x100) { //sampleDurationPresent
                entry.sampleDuration = IsoTypeReader.readUInt32(content);
            }
            if ((getFlags() & 0x200) == 0x200) { //sampleSizePresent
                entry.sampleSize = IsoTypeReader.readUInt32(content);
            }
            if ((getFlags() & 0x400) == 0x400) { //sampleFlagsPresent
                entry.sampleFlags = new SampleFlags(content);
            }
            if ((getFlags() & 0x800) == 0x800) { //sampleCompositionTimeOffsetPresent
                entry.sampleCompositionTimeOffset = content.getInt();
            }
            entries.add(entry);
        }

    }

    public long getSampleCount() {
        return entries.size();
    }

    public boolean isDataOffsetPresent() {
        return (getFlags() & 0x1) == 1;
    }

    public boolean isFirstSampleFlagsPresent() {
        return (getFlags() & 0x4) == 0x4;
    }


    public boolean isSampleSizePresent() {
        return (getFlags() & 0x200) == 0x200;
    }

    public boolean isSampleDurationPresent() {
        return (getFlags() & 0x100) == 0x100;
    }

    public boolean isSampleFlagsPresent() {
        return (getFlags() & 0x400) == 0x400;
    }

    public boolean isSampleCompositionTimeOffsetPresent() {
        return (getFlags() & 0x800) == 0x800;
    }

    public void setDataOffsetPresent(boolean v) {
        if (v) {
            setFlags(getFlags() | 0x01);
        } else {
            setFlags(getFlags() & (0xFFFFFF ^ 0x1));
        }
    }

    public void setSampleSizePresent(boolean v) {
        if (v) {
            setFlags(getFlags() | 0x200);
        } else {
            setFlags(getFlags() & (0xFFFFFF ^ 0x200));
        }
    }

    public void setSampleDurationPresent(boolean v) {

        if (v) {
            setFlags(getFlags() | 0x100);
        } else {
            setFlags(getFlags() & (0xFFFFFF ^ 0x100));
        }
    }

    public void setSampleFlagsPresent(boolean v) {
        if (v) {
            setFlags(getFlags() | 0x400);
        } else {
            setFlags(getFlags() & (0xFFFFFF ^ 0x400));
        }
    }

    public void setSampleCompositionTimeOffsetPresent(boolean v) {
        if (v) {
            setFlags(getFlags() | 0x800);
        } else {
            setFlags(getFlags() & (0xFFFFFF ^ 0x800));
        }

    }

    public int getDataOffset() {
        return dataOffset;
    }

    public SampleFlags getFirstSampleFlags() {
        return firstSampleFlags;
    }

    public void setFirstSampleFlags(SampleFlags firstSampleFlags) {
        if (firstSampleFlags == null) {
            setFlags(getFlags() & (0xFFFFFF ^ 0x4));
        } else {
            setFlags(getFlags() | 0x4);
        }
        this.firstSampleFlags = firstSampleFlags;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TrackRunBox");
        sb.append("{sampleCount=").append(entries.size());
        sb.append(", dataOffset=").append(dataOffset);
        sb.append(", dataOffsetPresent=").append(isDataOffsetPresent());
        sb.append(", sampleSizePresent=").append(isSampleSizePresent());
        sb.append(", sampleDurationPresent=").append(isSampleDurationPresent());
        sb.append(", sampleFlagsPresentPresent=").append(isSampleFlagsPresent());
        sb.append(", sampleCompositionTimeOffsetPresent=").append(isSampleCompositionTimeOffsetPresent());
        sb.append(", firstSampleFlags=").append(firstSampleFlags);
        sb.append('}');
        return sb.toString();
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }
}
