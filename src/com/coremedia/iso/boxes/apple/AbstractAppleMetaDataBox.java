package com.coremedia.iso.boxes.apple;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractBox;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import com.googlecode.mp4parser.util.ByteBufferByteChannel;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public abstract class AbstractAppleMetaDataBox extends AbstractBox implements ContainerBox {
    private static Logger LOG = Logger.getLogger(AbstractAppleMetaDataBox.class.getName());
    AppleDataBox appleDataBox = new AppleDataBox();

    public List<Box> getBoxes() {
        return Collections.singletonList((Box) appleDataBox);
    }

    public void setBoxes(List<Box> boxes) {
        if (boxes.size() == 1 && boxes.get(0) instanceof AppleDataBox) {
            appleDataBox = (AppleDataBox) boxes.get(0);
        } else {
            throw new IllegalArgumentException("This box only accepts one AppleDataBox child");
        }
    }

    public <T extends Box> List<T> getBoxes(Class<T> clazz) {
        return getBoxes(clazz, false);
    }

    public <T extends Box> List<T> getBoxes(Class<T> clazz, boolean recursive) {
        //todo recursive?
        if (clazz.isAssignableFrom(appleDataBox.getClass())) {
            return (List<T>) Collections.singletonList(appleDataBox);
        }
        return null;
    }

    public AbstractAppleMetaDataBox(String type) {
        super(type);
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        long dataBoxSize = IsoTypeReader.readUInt32(content);
        String thisShouldBeData = IsoTypeReader.read4cc(content);
        assert "data".equals(thisShouldBeData);
        appleDataBox = new AppleDataBox();
        try {
            appleDataBox.parse(new ByteBufferByteChannel(content), null, content.remaining(), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        appleDataBox.setParent(this);
    }


    protected long getContentSize() {
        return appleDataBox.getSize();
    }

    protected void getContent(ByteBuffer byteBuffer) {
        try {
            appleDataBox.getBox(new ByteBufferByteChannel(byteBuffer));
        } catch (IOException e) {
            throw new RuntimeException("The Channel is based on a ByteBuffer and therefore it shouldn't throw any exception");
        }
    }

    public long getNumOfBytesToFirstChild() {
        return getSize() - appleDataBox.getSize();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "appleDataBox=" + getValue() +
                '}';
    }

    static long toLong(byte b) {
        return b < 0 ? b + 256 : b;
    }

    public void setValue(String value) {
        if (appleDataBox.getFlags() == 1) {
            appleDataBox = new AppleDataBox();
            appleDataBox.setVersion(0);
            appleDataBox.setFlags(1);
            appleDataBox.setFourBytes(new byte[4]);
            appleDataBox.setData(Utf8.convert(value));
        } else if (appleDataBox.getFlags() == 21) {
            byte[] content = appleDataBox.getData();
            appleDataBox = new AppleDataBox();
            appleDataBox.setVersion(0);
            appleDataBox.setFlags(21);
            appleDataBox.setFourBytes(new byte[4]);

            ByteBuffer bb = ByteBuffer.allocate(content.length);
            if (content.length == 1) {
                IsoTypeWriter.writeUInt8(bb, (Byte.parseByte(value) & 0xFF));
            } else if (content.length == 2) {
                IsoTypeWriter.writeUInt16(bb, Integer.parseInt(value));
            } else if (content.length == 4) {
                IsoTypeWriter.writeUInt32(bb, Long.parseLong(value));
            } else if (content.length == 8) {
                IsoTypeWriter.writeUInt64(bb, Long.parseLong(value));
            } else {
                throw new Error("The content length within the appleDataBox is neither 1, 2, 4 or 8. I can't handle that!");
            }
            appleDataBox.setData(bb.array());
        } else if (appleDataBox.getFlags() == 0) {
            appleDataBox = new AppleDataBox();
            appleDataBox.setVersion(0);
            appleDataBox.setFlags(0);
            appleDataBox.setFourBytes(new byte[4]);
            appleDataBox.setData(hexStringToByteArray(value));

        } else {
            LOG.warning("Don't know how to handle appleDataBox with flag=" + appleDataBox.getFlags());
        }
    }

    public String getValue() {
        if (appleDataBox.getFlags() == 1) {
            return Utf8.convert(appleDataBox.getData());
        } else if (appleDataBox.getFlags() == 21) {
            byte[] content = appleDataBox.getData();
            long l = 0;
            int current = 1;
            int length = content.length;
            for (byte b : content) {
                l += toLong(b) << (8 * (length - current++));
            }
            return "" + l;
        } else if (appleDataBox.getFlags() == 0) {
            return String.format("%x", new BigInteger(appleDataBox.getData()));
        } else {
            return "unknown";
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }


}
