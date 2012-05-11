/*  
 * Copyright 2008 CoreMedia AG, Hamburg
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

package com.coremedia.iso.boxes;


import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * <code>
 * Box Type  : 'elst'<br>
 * Container: {@link EditBox}('edts')<br>
 * Mandatory: No<br>
 * Quantity  : Zero or one</code><br><br>
 * This box contains an explicit timeline map. Each entry defines part of the track time-line: by mapping part of
 * the media time-line, or by indicating 'empty' time, or by defining a 'dwell', where a single time-point in the
 * media is held for a period.<br>
 * Note that edits are not restricted to fall on sample times. This means that when entering an edit, it can be
 * necessary to (a) back up to a sync point, and pre-roll from there and then (b) be careful about the duration of
 * the first sample - it might have been truncated if the edit enters it during its normal duration. If this is audio,
 * that frame might need to be decoded, and then the final slicing done. Likewise, the duration of the last sample
 * in an edit might need slicing. <br>
 * Starting offsets for tracks (streams) are represented by an initial empty edit. For example, to play a track from
 * its start for 30 seconds, but at 10 seconds into the presentation, we have the following edit list:<br>
 * <p/>
 * <li>Entry-count = 2</li>
 * <li>Segment-duration = 10 seconds</li>
 * <li>Media-Time = -1</li>
 * <li>Media-Rate = 1</li>
 * <li>Segment-duration = 30 seconds (could be the length of the whole track)</li>
 * <li>Media-Time = 0 seconds</li>
 * <li>Media-Rate = 1</li>
 */
public class EditListBox extends AbstractFullBox {
    private List<Entry> entries = new LinkedList<Entry>();
    public static final String TYPE = "elst";

    public EditListBox() {
        super(TYPE);
    }


    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    protected long getContentSize() {
        long contentSize = 8;
        if (getVersion() == 1) {
            contentSize += entries.size() * 20;
        } else {
            contentSize += entries.size() * 12;
        }

        return contentSize;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        int entryCount = l2i(IsoTypeReader.readUInt32(content));
        entries = new LinkedList<Entry>();
        for (int i = 0; i < entryCount; i++) {
            entries.add(new Entry(this, content));

        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, entries.size());
        for (Entry entry : entries) {
            entry.getContent(byteBuffer);
        }
    }

    @Override
    public String toString() {
        return "EditListBox{" +
                "entries=" + entries +
                '}';
    }

    public static class Entry {
        private long segmentDuration;
        private long mediaTime;
        private double mediaRate;
        EditListBox editListBox;

        /**
         * Creates a new <code>Entry</code> with all values set.
         *
         * @param segmentDuration duration in movie timescale
         * @param mediaTime       starting time
         * @param mediaRate       relative play rate
         */
        public Entry(EditListBox editListBox, long segmentDuration, long mediaTime, double mediaRate) {
            this.segmentDuration = segmentDuration;
            this.mediaTime = mediaTime;
            this.mediaRate = mediaRate;
            this.editListBox = editListBox;
        }

        public Entry(EditListBox editListBox, ByteBuffer bb) {
            if (editListBox.getVersion() == 1) {
                segmentDuration = IsoTypeReader.readUInt64(bb);
                mediaTime = IsoTypeReader.readUInt64(bb);
                mediaRate = IsoTypeReader.readFixedPoint1616(bb);
            } else {
                segmentDuration = IsoTypeReader.readUInt32(bb);
                mediaTime = IsoTypeReader.readUInt32(bb);
                mediaRate = IsoTypeReader.readFixedPoint1616(bb);
            }
            this.editListBox = editListBox;
        }

        /**
         * The segment duration is an integer that specifies the duration
         * of this edit segment in units of the timescale in the Movie
         * Header Box
         *
         * @return segment duration in movie timescale
         */
        public long getSegmentDuration() {
            return segmentDuration;
        }

        /**
         * The segment duration is an integer that specifies the duration
         * of this edit segment in units of the timescale in the Movie
         * Header Box
         *
         * @param segmentDuration new segment duration in movie timescale
         */
        public void setSegmentDuration(long segmentDuration) {
            this.segmentDuration = segmentDuration;
        }

        /**
         * The media time is an integer containing the starting time
         * within the media of a specific edit segment(in media time
         * scale units, in composition time)
         *
         * @return starting time
         */
        public long getMediaTime() {
            return mediaTime;
        }

        /**
         * The media time is an integer containing the starting time
         * within the media of a specific edit segment(in media time
         * scale units, in composition time)
         *
         * @param mediaTime starting time
         */
        public void setMediaTime(long mediaTime) {
            this.mediaTime = mediaTime;
        }

        /**
         * The media rate specifies the relative rate at which to play the
         * media corresponding to a specific edit segment.
         *
         * @return relative play rate
         */
        public double getMediaRate() {
            return mediaRate;
        }

        /**
         * The media rate specifies the relative rate at which to play the
         * media corresponding to a specific edit segment.
         *
         * @param mediaRate new relative play rate
         */
        public void setMediaRate(double mediaRate) {
            this.mediaRate = mediaRate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            if (mediaTime != entry.mediaTime) return false;
            if (segmentDuration != entry.segmentDuration) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (segmentDuration ^ (segmentDuration >>> 32));
            result = 31 * result + (int) (mediaTime ^ (mediaTime >>> 32));
            return result;
        }

        public void getContent(ByteBuffer bb)  {
            if (editListBox.getVersion() == 1) {
                IsoTypeWriter.writeUInt64(bb, segmentDuration);
                IsoTypeWriter.writeUInt64(bb, mediaTime);
            } else {
                IsoTypeWriter.writeUInt32(bb, l2i(segmentDuration));
                bb.putInt(l2i(mediaTime));
            }
            IsoTypeWriter.writeFixedPont1616(bb, mediaRate);
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "segmentDuration=" + segmentDuration +
                    ", mediaTime=" + mediaTime +
                    ", mediaRate=" + mediaRate +
                    '}';
        }
    }
}
