package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * aligned(8) class SubSampleInformationBox
 * extends FullBox('subs', version, 0) {
 * unsigned int(32) entry_count;
 * int i,j;
 * for (i=0; i < entry_count; i++) {
 * unsigned int(32) sample_delta;
 * unsigned int(16) subsample_count;
 * if (subsample_count > 0) {
 * for (j=0; j < subsample_count; j++) {
 * if(version == 1)
 * {
 * unsigned int(32) subsample_size;
 * }
 * else
 * {
 * unsigned int(16) subsample_size;
 * }
 * unsigned int(8) subsample_priority;
 * unsigned int(8) discardable;
 * unsigned int(32) reserved = 0;
 * }
 * }
 * }
 * }
 */
public class SubSampleInformationBox extends AbstractFullBox {
    public static final String TYPE = "subs";

    private long entryCount;
    private List<SampleEntry> entries = new ArrayList<SampleEntry>();

    public SubSampleInformationBox() {
        super(TYPE);
    }

    public List<SampleEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<SampleEntry> entries) {
        this.entries = entries;
        entryCount = entries.size();
    }

    @Override
    protected long getContentSize() {
        long entries = 8 + ((4 + 2) * entryCount);
        int subsampleEntries = 0;
        for (SampleEntry sampleEntry : this.entries) {
            subsampleEntries += sampleEntry.getSubsampleCount() * (((getVersion() == 1) ? 4 : 2) + 1 + 1 + 4);
        }
        return entries + subsampleEntries;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);

        entryCount = IsoTypeReader.readUInt32(content);

        for (int i = 0; i < entryCount; i++) {
            SampleEntry sampleEntry = new SampleEntry();
            sampleEntry.setSampleDelta(IsoTypeReader.readUInt32(content));
            int subsampleCount = IsoTypeReader.readUInt16(content);
            for (int j = 0; j < subsampleCount; j++) {
                SampleEntry.SubsampleEntry subsampleEntry = new SampleEntry.SubsampleEntry();
                subsampleEntry.setSubsampleSize(getVersion() == 1 ? IsoTypeReader.readUInt32(content) : IsoTypeReader.readUInt16(content));
                subsampleEntry.setSubsamplePriority(IsoTypeReader.readUInt8(content));
                subsampleEntry.setDiscardable(IsoTypeReader.readUInt8(content));
                subsampleEntry.setReserved(IsoTypeReader.readUInt32(content));
                sampleEntry.addSubsampleEntry(subsampleEntry);
            }
            entries.add(sampleEntry);
        }

    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, entries.size());
        for (SampleEntry sampleEntry : entries) {
            IsoTypeWriter.writeUInt32(byteBuffer, sampleEntry.getSampleDelta());
            IsoTypeWriter.writeUInt16(byteBuffer, sampleEntry.getSubsampleCount());
            List<SampleEntry.SubsampleEntry> subsampleEntries = sampleEntry.getSubsampleEntries();
            for (SampleEntry.SubsampleEntry subsampleEntry : subsampleEntries) {
                if (getVersion() == 1) {
                    IsoTypeWriter.writeUInt32(byteBuffer, subsampleEntry.getSubsampleSize());
                } else {
                    IsoTypeWriter.writeUInt16(byteBuffer, l2i(subsampleEntry.getSubsampleSize()));
                }
                IsoTypeWriter.writeUInt8(byteBuffer, subsampleEntry.getSubsamplePriority());
                IsoTypeWriter.writeUInt8(byteBuffer, subsampleEntry.getDiscardable());
                IsoTypeWriter.writeUInt32(byteBuffer, subsampleEntry.getReserved());
            }
        }
    }

    @Override
    public String toString() {
        return "SubSampleInformationBox{" +
                "entryCount=" + entryCount +
                ", entries=" + entries +
                '}';
    }

    public static class SampleEntry {
        private long sampleDelta;
        private int subsampleCount;
        private List<SubsampleEntry> subsampleEntries = new ArrayList<SubsampleEntry>();

        public long getSampleDelta() {
            return sampleDelta;
        }

        public void setSampleDelta(long sampleDelta) {
            this.sampleDelta = sampleDelta;
        }

        public int getSubsampleCount() {
            return subsampleCount;
        }

        public void setSubsampleCount(int subsampleCount) {
            this.subsampleCount = subsampleCount;
        }

        public List<SubsampleEntry> getSubsampleEntries() {
            return subsampleEntries;
        }

        public void addSubsampleEntry(SubsampleEntry subsampleEntry) {
            subsampleEntries.add(subsampleEntry);
            subsampleCount++;
        }

        public static class SubsampleEntry {
            private long subsampleSize;
            private int subsamplePriority;
            private int discardable;
            private long reserved;

            public long getSubsampleSize() {
                return subsampleSize;
            }

            public void setSubsampleSize(long subsampleSize) {
                this.subsampleSize = subsampleSize;
            }

            public int getSubsamplePriority() {
                return subsamplePriority;
            }

            public void setSubsamplePriority(int subsamplePriority) {
                this.subsamplePriority = subsamplePriority;
            }

            public int getDiscardable() {
                return discardable;
            }

            public void setDiscardable(int discardable) {
                this.discardable = discardable;
            }

            public long getReserved() {
                return reserved;
            }

            public void setReserved(long reserved) {
                this.reserved = reserved;
            }

            @Override
            public String toString() {
                return "SubsampleEntry{" +
                        "subsampleSize=" + subsampleSize +
                        ", subsamplePriority=" + subsamplePriority +
                        ", discardable=" + discardable +
                        ", reserved=" + reserved +
                        '}';
            }
        }

        @Override
        public String toString() {
            return "SampleEntry{" +
                    "sampleDelta=" + sampleDelta +
                    ", subsampleCount=" + subsampleCount +
                    ", subsampleEntries=" + subsampleEntries +
                    '}';
        }
    }
}
