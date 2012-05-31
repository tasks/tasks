package com.coremedia.iso.boxes;

import java.nio.ByteBuffer;

public class SubtitleMediaHeaderBox extends AbstractMediaHeaderBox {

    public static final String TYPE = "sthd";

    public SubtitleMediaHeaderBox() {
        super(TYPE);
    }

    protected long getContentSize() {
        return 4;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
    }

    public String toString() {
        return "SubtitleMediaHeaderBox";
    }
}
