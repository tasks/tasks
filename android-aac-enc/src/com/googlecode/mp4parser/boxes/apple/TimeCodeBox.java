package com.googlecode.mp4parser.boxes.apple;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;

import java.nio.ByteBuffer;

public class TimeCodeBox extends SampleEntry {
    byte[] data;


    public TimeCodeBox() {
        super("tmcd");
    }

    @Override
    protected long getContentSize() {
        long size = 26;
        for (Box box : boxes) {
            size += box.getSize();
        }
        return size;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        _parseReservedAndDataReferenceIndex(content);
        data = new byte[18];
        content.get(data);
        _parseChildBoxes(content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        _writeReservedAndDataReferenceIndex(byteBuffer);
        byteBuffer.put(data);
        _writeChildBoxes(byteBuffer);
    }
}
