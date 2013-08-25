package com.coremedia.iso.boxes.apple;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;

/**
 * Apple Meaning box. Allowed as subbox of "----" box.
 *
 * @see com.coremedia.iso.boxes.apple.AppleGenericBox
 */
public final class AppleMeanBox extends AbstractFullBox {
    public static final String TYPE = "mean";
    private String meaning;

    public AppleMeanBox() {
        super(TYPE);
    }

    protected long getContentSize() {
        return 4 + Utf8.utf8StringLengthInBytes(meaning);
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        meaning = IsoTypeReader.readString(content, content.remaining());
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        byteBuffer.put(Utf8.convert(meaning));
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }


}
