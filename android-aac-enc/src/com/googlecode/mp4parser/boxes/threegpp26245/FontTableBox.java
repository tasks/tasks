package com.googlecode.mp4parser.boxes.threegpp26245;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractBox;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class FontTableBox extends AbstractBox {
    List<FontRecord> entries = new LinkedList<FontRecord>();

    public FontTableBox() {
        super("ftab");
    }

    @Override
    protected long getContentSize() {
        int size = 2;
        for (FontRecord fontRecord : entries) {
            size += fontRecord.getSize();
        }
        return size;
    }


    @Override
    public void _parseDetails(ByteBuffer content) {
        int numberOfRecords = IsoTypeReader.readUInt16(content);
        for (int i = 0; i < numberOfRecords; i++) {
            FontRecord fr = new FontRecord();
            fr.parse(content);
            entries.add(fr);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        IsoTypeWriter.writeUInt16(byteBuffer, entries.size());
        for (FontRecord record : entries) {
            record.getContent(byteBuffer);
        }
    }

    public List<FontRecord> getEntries() {
        return entries;
    }

    public void setEntries(List<FontRecord> entries) {
        this.entries = entries;
    }

    public static class FontRecord {
        int fontId;
        String fontname;

        public FontRecord() {
        }

        public FontRecord(int fontId, String fontname) {
            this.fontId = fontId;
            this.fontname = fontname;
        }

        public void parse(ByteBuffer bb) {
            fontId = IsoTypeReader.readUInt16(bb);
            int length = IsoTypeReader.readUInt8(bb);
            fontname = IsoTypeReader.readString(bb, length);
        }

        public void getContent(ByteBuffer bb) {
            IsoTypeWriter.writeUInt16(bb, fontId);
            IsoTypeWriter.writeUInt8(bb, fontname.length());
            bb.put(Utf8.convert(fontname));
        }

        public int getSize() {
            return Utf8.utf8StringLengthInBytes(fontname) + 3;
        }

        @Override
        public String toString() {
            return "FontRecord{" +
                    "fontId=" + fontId +
                    ", fontname='" + fontname + '\'' +
                    '}';
        }
    }
}
