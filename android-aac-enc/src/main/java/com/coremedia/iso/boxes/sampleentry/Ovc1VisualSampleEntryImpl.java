package com.coremedia.iso.boxes.sampleentry;

import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.Box;

import java.nio.ByteBuffer;


public class Ovc1VisualSampleEntryImpl extends SampleEntry {
    private byte[] vc1Content;
    public static final String TYPE = "ovc1";


    @Override
    protected long getContentSize() {
        long size = 8;

        for (Box box : boxes) {
            size += box.getSize();
        }
        size += vc1Content.length;
        return size;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        _parseReservedAndDataReferenceIndex(content);
        vc1Content = new byte[content.remaining()];
        content.get(vc1Content);

    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        byteBuffer.put(new byte[6]);
        IsoTypeWriter.writeUInt16(byteBuffer, getDataReferenceIndex());
        byteBuffer.put(vc1Content);
    }


    protected Ovc1VisualSampleEntryImpl() {
        super(TYPE);
    }

}
