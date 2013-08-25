package com.coremedia.iso.boxes.dece;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * aligned(8) class TrickPlayBox
 * extends FullBox(‘trik’, version=0, flags=0)
 * {
 * for (i=0; I < sample_count; i++) {
 * unsigned int(2) pic_type;
 * unsigned int(6) dependency_level;
 * }
 * }
 */
public class TrickPlayBox extends AbstractFullBox {
    public static final String TYPE = "trik";

    private List<Entry> entries = new ArrayList<Entry>();

    public TrickPlayBox() {
        super(TYPE);
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public static class Entry {

        public Entry() {
        }

        public Entry(int value) {
            this.value = value;
        }


        private int value;

        public int getPicType() {
            return (value >> 6) & 0x03;
        }

        public void setPicType(int picType) {
            value = value & (0xff >> 3);
            value = (picType & 0x03) << 6 | value;
        }

        public int getDependencyLevel() {
            return value & 0x3f;
        }

        public void setDependencyLevel(int dependencyLevel) {
            value = (dependencyLevel & 0x3f) | value;
        }


        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Entry");
            sb.append("{picType=").append(getPicType());
            sb.append(",dependencyLevel=").append(getDependencyLevel());
            sb.append('}');
            return sb.toString();
        }
    }

    @Override
    protected long getContentSize() {
        return 4 + entries.size();
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        while (content.remaining() > 0) {
            entries.add(new Entry(IsoTypeReader.readUInt8(content)));
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        for (Entry entry : entries) {
            IsoTypeWriter.writeUInt8(byteBuffer, entry.value);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TrickPlayBox");
        sb.append("{entries=").append(entries);
        sb.append('}');
        return sb.toString();
    }
}
