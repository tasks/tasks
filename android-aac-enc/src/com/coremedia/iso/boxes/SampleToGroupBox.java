package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * aligned(8) class SampleToGroupBox
 * extends FullBox('sbgp', version = 0, 0)
 * {
 * unsigned int(32) grouping_type;
 * unsigned int(32) entry_count;
 * for (i=1; i <= entry_count; i++)
 * {
 * unsigned int(32) sample_count;
 * unsigned int(32) group_description_index;
 * }
 * }
 */
public class SampleToGroupBox extends AbstractFullBox {
    public static final String TYPE = "sbgp";
    private long groupingType;
    private long entryCount;
    private long groupingTypeParameter;
    private List<Entry> entries = new ArrayList<Entry>();

    public SampleToGroupBox() {
        super(TYPE);
    }

    @Override
    protected long getContentSize() {
        return 12 + entryCount * 8;
    }

    public long getGroupingTypeParameter() {
        return groupingTypeParameter;
    }

    /**
     * Usage of this parameter requires version == 1. The version must be set manually.
     */
    public void setGroupingTypeParameter(long groupingTypeParameter) {
        this.groupingTypeParameter = groupingTypeParameter;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public long getGroupingType() {
        return groupingType;
    }


    public void setGroupingType(long groupingType) {
        this.groupingType = groupingType;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        groupingType = IsoTypeReader.readUInt32(content);
        if (getVersion() == 1) {
            groupingTypeParameter = IsoTypeReader.readUInt32(content);
        } else {
            groupingTypeParameter = -1;
        }
        entryCount = IsoTypeReader.readUInt32(content);

        for (int i = 0; i < entryCount; i++) {
            Entry entry = new Entry();
            entry.setSampleCount(IsoTypeReader.readUInt32(content));
            entry.setGroupDescriptionIndex(IsoTypeReader.readUInt32(content));
            entries.add(entry);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);

        IsoTypeWriter.writeUInt32(byteBuffer, groupingType);
        if (getVersion() == 1) {
            IsoTypeWriter.writeUInt32(byteBuffer, groupingTypeParameter);
        }
        IsoTypeWriter.writeUInt32(byteBuffer, entryCount);
        for (Entry entry : entries) {
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getSampleCount());
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getGroupDescriptionIndex());
        }
    }

    public static class Entry {
        private long sampleCount;
        private long groupDescriptionIndex;

        public long getSampleCount() {
            return sampleCount;
        }

        public void setSampleCount(long sampleCount) {
            this.sampleCount = sampleCount;
        }

        public long getGroupDescriptionIndex() {
            return groupDescriptionIndex;
        }

        public void setGroupDescriptionIndex(long groupDescriptionIndex) {
            this.groupDescriptionIndex = groupDescriptionIndex;
        }
    }
}
