package com.googlecode.mp4parser.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.UUID;

/**
 *
 */
public abstract class AbstractTrackEncryptionBox extends AbstractFullBox {
    int defaultAlgorithmId;
    int defaultIvSize;
    byte[] default_KID;

    protected AbstractTrackEncryptionBox(String type) {
        super(type);
    }

    public int getDefaultAlgorithmId() {
        return defaultAlgorithmId;
    }

    public void setDefaultAlgorithmId(int defaultAlgorithmId) {
        this.defaultAlgorithmId = defaultAlgorithmId;
    }

    public int getDefaultIvSize() {
        return defaultIvSize;
    }

    public void setDefaultIvSize(int defaultIvSize) {
        this.defaultIvSize = defaultIvSize;
    }

    public String getDefault_KID() {
        ByteBuffer b = ByteBuffer.wrap(default_KID);
        b.order(ByteOrder.BIG_ENDIAN);
        return new UUID(b.getLong(), b.getLong()).toString();
    }

    public void setDefault_KID(byte[] default_KID) {
        this.default_KID = default_KID;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        defaultAlgorithmId = IsoTypeReader.readUInt24(content);
        defaultIvSize = IsoTypeReader.readUInt8(content);
        default_KID = new byte[16];
        content.get(default_KID);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt24(byteBuffer, defaultAlgorithmId);
        IsoTypeWriter.writeUInt8(byteBuffer, defaultIvSize);
        byteBuffer.put(default_KID);
    }

    @Override
    protected long getContentSize() {
        return 24;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractTrackEncryptionBox that = (AbstractTrackEncryptionBox) o;

        if (defaultAlgorithmId != that.defaultAlgorithmId) return false;
        if (defaultIvSize != that.defaultIvSize) return false;
        if (!Arrays.equals(default_KID, that.default_KID)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = defaultAlgorithmId;
        result = 31 * result + defaultIvSize;
        result = 31 * result + (default_KID != null ? Arrays.hashCode(default_KID) : 0);
        return result;
    }
}
