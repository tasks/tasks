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
 * <pre>
 * aligned(8) class CompositionOffsetBox
 * extends FullBox(‘ctts’, version = 0, 0) {
 *  unsigned int(32) entry_count;
 *  int i;
 *  if (version==0) {
 *   for (i=0; i < entry_count; i++) {
 *    unsigned int(32) sample_count;
 *    unsigned int(32) sample_offset;
 *   }
 *  }
 *  else if (version == 1) {
 *   for (i=0; i < entry_count; i++) {
 *    unsigned int(32) sample_count;
 *    signed int(32) sample_offset;
 *   }
 *  }
 * }
 * </pre>
 * <p/>
 * This box provides the offset between decoding time and composition time.
 * In version 0 of this box the decoding time must be less than the composition time, and
 * the offsets are expressed as unsigned numbers such that
 * CT(n) = DT(n) + CTTS(n) where CTTS(n) is the (uncompressed) table entry for sample n.
 * <p/>
 * In version 1 of this box, the composition timeline and the decoding timeline are
 * still derived from each other, but the offsets are signed.
 * It is recommended that for the computed composition timestamps, there is
 * exactly one with the value 0 (zero).
 */
public class CompositionTimeToSample extends AbstractFullBox {
    public static final String TYPE = "ctts";

    List<Entry> entries = Collections.emptyList();

    public CompositionTimeToSample() {
        super(TYPE);
    }

    protected long getContentSize() {
        return 8 + 8 * entries.size();
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        int numberOfEntries = l2i(IsoTypeReader.readUInt32(content));
        entries = new ArrayList<Entry>(numberOfEntries);
        for (int i = 0; i < numberOfEntries; i++) {
            Entry e = new Entry(l2i(IsoTypeReader.readUInt32(content)), content.getInt());
            entries.add(e);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, entries.size());

        for (Entry entry : entries) {
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getCount());
            byteBuffer.putInt(entry.getOffset());
        }

    }


    public static class Entry {
        int count;
        int offset;

        public Entry(int count, int offset) {
            this.count = count;
            this.offset = offset;
        }

        public int getCount() {
            return count;
        }

        public int getOffset() {
            return offset;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "count=" + count +
                    ", offset=" + offset +
                    '}';
        }
    }


    /**
     * Decompresses the list of entries and returns the list of composition times.
     *
     * @return decoding time per sample
     */
    public static int[] blowupCompositionTimes(List<CompositionTimeToSample.Entry> entries) {
        long numOfSamples = 0;
        for (CompositionTimeToSample.Entry entry : entries) {
            numOfSamples += entry.getCount();
        }
        assert numOfSamples <= Integer.MAX_VALUE;
        int[] decodingTime = new int[(int) numOfSamples];

        int current = 0;


        for (CompositionTimeToSample.Entry entry : entries) {
            for (int i = 0; i < entry.getCount(); i++) {
                decodingTime[current++] = entry.getOffset();
            }
        }

        return decodingTime;
    }

}
