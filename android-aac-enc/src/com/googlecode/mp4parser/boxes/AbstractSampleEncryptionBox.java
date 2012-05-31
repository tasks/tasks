package com.googlecode.mp4parser.boxes;

import com.coremedia.iso.Hex;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public abstract class AbstractSampleEncryptionBox extends AbstractFullBox {
    int algorithmId = -1;
    int ivSize = -1;
    byte[] kid = new byte[16];
    List<Entry> entries = new LinkedList<Entry>();

    protected AbstractSampleEncryptionBox(String type) {
        super(type);
    }

    public int getOffsetToFirstIV() {
        int offset = (getSize() > (1l << 32) ? 16 : 8);
        offset += isOverrideTrackEncryptionBoxParameters() ? 20 : 0;
        offset += 4; //num entries
        return offset;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        if ((getFlags() & 0x1) > 0) {
            algorithmId = IsoTypeReader.readUInt24(content);
            ivSize = IsoTypeReader.readUInt8(content);
            kid = new byte[16];
            content.get(kid);
        }
        long numOfEntries = IsoTypeReader.readUInt32(content);
        while (numOfEntries-- > 0) {
            Entry e = new Entry();
            e.iv = new byte[((getFlags() & 0x1) > 0) ? ivSize : 8];
            content.get(e.iv);
            if ((getFlags() & 0x2) > 0) {
                int numOfPairs = IsoTypeReader.readUInt16(content);
                e.pairs = new LinkedList<Entry.Pair>();
                while (numOfPairs-- > 0) {
                    e.pairs.add(e.createPair(IsoTypeReader.readUInt16(content), IsoTypeReader.readUInt32(content)));
                }
            }
            entries.add(e);

        }
    }


    public int getSampleCount() {
        return entries.size();
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public int getAlgorithmId() {
        return algorithmId;
    }

    public void setAlgorithmId(int algorithmId) {
        this.algorithmId = algorithmId;
    }

    public int getIvSize() {
        return ivSize;
    }

    public void setIvSize(int ivSize) {
        this.ivSize = ivSize;
    }

    public byte[] getKid() {
        return kid;
    }

    public void setKid(byte[] kid) {
        this.kid = kid;
    }


    public boolean isSubSampleEncryption() {
        return (getFlags() & 0x2) > 0;
    }

    public boolean isOverrideTrackEncryptionBoxParameters() {
        return (getFlags() & 0x1) > 0;
    }

    public void setSubSampleEncryption(boolean b) {
        if (b) {
            setFlags(getFlags() | 0x2);
        } else {
            setFlags(getFlags() & (0xffffff ^ 0x2));
        }
    }

    public void setOverrideTrackEncryptionBoxParameters(boolean b) {
        if (b) {
            setFlags(getFlags() | 0x1);
        } else {
            setFlags(getFlags() & (0xffffff ^ 0x1));
        }
    }


    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if (isOverrideTrackEncryptionBoxParameters()) {
            IsoTypeWriter.writeUInt24(byteBuffer, algorithmId);
            IsoTypeWriter.writeUInt8(byteBuffer, ivSize);
            byteBuffer.put(kid);
        }
        IsoTypeWriter.writeUInt32(byteBuffer, entries.size());
        for (Entry entry : entries) {
            if (isOverrideTrackEncryptionBoxParameters()) {
                byte[] ivFull = new byte[ivSize];
                System.arraycopy(entry.iv, 0, ivFull, ivSize - entry.iv.length, entry.iv.length);
                byteBuffer.put(ivFull);
            } else {
                // just put the iv - i don't know any better
                byteBuffer.put(entry.iv);
            }
            if (isSubSampleEncryption()) {
                IsoTypeWriter.writeUInt16(byteBuffer, entry.pairs.size());
                for (Entry.Pair pair : entry.pairs) {
                    IsoTypeWriter.writeUInt16(byteBuffer, pair.clear);
                    IsoTypeWriter.writeUInt32(byteBuffer, pair.encrypted);
                }
            }
        }
    }

    @Override
    protected long getContentSize() {
        long contentSize = 4;
        if (isOverrideTrackEncryptionBoxParameters()) {
            contentSize += 4;
            contentSize += kid.length;
        }
        contentSize += 4;
        for (Entry entry : entries) {
            contentSize += entry.getSize();
        }
        return contentSize;
    }

    @Override
    public void getBox(WritableByteChannel os) throws IOException {
        super.getBox(os);
    }

    public Entry createEntry() {
        return new Entry();
    }

    public class Entry {
        public byte[] iv;
        public List<Pair> pairs = new LinkedList<Pair>();

        public int getSize() {
            int size = 0;
            if (isOverrideTrackEncryptionBoxParameters()) {
                size = ivSize;
            } else {
                size = iv.length;
            }


            if (isSubSampleEncryption()) {
                size += 2;
                for (Entry.Pair pair : pairs) {
                    size += 6;
                }
            }
            return size;
        }

        public Pair createPair(int clear, long encrypted) {
            return new Pair(clear, encrypted);
        }


        public class Pair {
            public int clear;
            public long encrypted;

            public Pair(int clear, long encrypted) {
                this.clear = clear;
                this.encrypted = encrypted;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Pair pair = (Pair) o;

                if (clear != pair.clear) return false;
                if (encrypted != pair.encrypted) return false;

                return true;
            }

            @Override
            public int hashCode() {
                int result = clear;
                result = 31 * result + (int) (encrypted ^ (encrypted >>> 32));
                return result;
            }

            @Override
            public String toString() {
                return "clr:" + clear + " enc:" + encrypted;
            }
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            if (!new BigInteger(iv).equals(new BigInteger(entry.iv))) return false;
            if (pairs != null ? !pairs.equals(entry.pairs) : entry.pairs != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = iv != null ? Arrays.hashCode(iv) : 0;
            result = 31 * result + (pairs != null ? pairs.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "iv=" + Hex.encodeHex(iv) +
                    ", pairs=" + pairs +
                    '}';
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractSampleEncryptionBox that = (AbstractSampleEncryptionBox) o;

        if (algorithmId != that.algorithmId) return false;
        if (ivSize != that.ivSize) return false;
        if (entries != null ? !entries.equals(that.entries) : that.entries != null) return false;
        if (!Arrays.equals(kid, that.kid)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = algorithmId;
        result = 31 * result + ivSize;
        result = 31 * result + (kid != null ? Arrays.hashCode(kid) : 0);
        result = 31 * result + (entries != null ? entries.hashCode() : 0);
        return result;
    }

    public List<Short> getEntrySizes() {
        List<Short> entrySizes = new ArrayList<Short>(entries.size());
        for (Entry entry : entries) {
            short size = (short) entry.iv.length;
            if (isSubSampleEncryption()) {
                size += 2; //numPairs
                size += entry.pairs.size() * 6;
            }
            entrySizes.add(size);
        }
        return entrySizes;
    }
}
