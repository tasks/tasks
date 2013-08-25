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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * This box contains a compact version of a table that allows indexing from decoding time to sample number.
 * Other tables give sample sizes and pointers, from the sample number. Each entry in the table gives the
 * number of consecutive samples with the same time delta, and the delta of those samples. By adding the
 * deltas a complete time-to-sample map may be built.<br>
 * The Decoding Time to Sample Box contains decode time delta's: <code>DT(n+1) = DT(n) + STTS(n)</code> where STTS(n)
 * is the (uncompressed) table entry for sample n.<br>
 * The sample entries are ordered by decoding time stamps; therefore the deltas are all non-negative. <br>
 * The DT axis has a zero origin; <code>DT(i) = SUM(for j=0 to i-1 of delta(j))</code>, and the sum of all
 * deltas gives the length of the media in the track (not mapped to the overall timescale, and not considering
 * any edit list).    <br>
 * The Edit List Box provides the initial CT value if it is non-empty (non-zero).
 */
public class TimeToSampleBox extends AbstractFullBox {
    public static final String TYPE = "stts";

    List<Entry> entries = Collections.emptyList();


    public TimeToSampleBox() {
        super(TYPE);
    }

    protected long getContentSize() {
        return 8 + entries.size() * 8;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        int entryCount = l2i(IsoTypeReader.readUInt32(content));
        entries = new ArrayList<Entry>(entryCount);

        for (int i = 0; i < entryCount; i++) {
            entries.add(new Entry(IsoTypeReader.readUInt32(content), IsoTypeReader.readUInt32(content)));
        }

    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, entries.size());
        for (Entry entry : entries) {
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getCount());
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getDelta());
        }
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public String toString() {
        return "TimeToSampleBox[entryCount=" + entries.size() + "]";
    }

    public static class Entry {
        long count;
        long delta;

        public Entry(long count, long delta) {
            this.count = count;
            this.delta = delta;
        }

        public long getCount() {
            return count;
        }

        public long getDelta() {
            return delta;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public void setDelta(long delta) {
            this.delta = delta;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "count=" + count +
                    ", delta=" + delta +
                    '}';
        }
    }

    /**
     * Decompresses the list of entries and returns the list of decoding times.
     *
     * @return decoding time per sample
     */
    public static long[] blowupTimeToSamples(List<TimeToSampleBox.Entry> entries) {
        long numOfSamples = 0;
        for (TimeToSampleBox.Entry entry : entries) {
            numOfSamples += entry.getCount();
        }
        assert numOfSamples <= Integer.MAX_VALUE;
        long[] decodingTime = new long[(int) numOfSamples];

        int current = 0;


        for (TimeToSampleBox.Entry entry : entries) {
            for (int i = 0; i < entry.getCount(); i++) {
                decodingTime[current++] = entry.getDelta();
            }
        }

        return decodingTime;
    }


}
