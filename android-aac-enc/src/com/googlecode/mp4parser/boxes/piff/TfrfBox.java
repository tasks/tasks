package com.googlecode.mp4parser.boxes.piff;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * The syntax of the fields defined in this section, specified in ABNF [RFC5234], is as follows:
 * TfrfBox = TfrfBoxLength TfrfBoxType [TfrfBoxLongLength] TfrfBoxUUID TfrfBoxFields
 * TfrfBoxChildren
 * TfrfBoxType = "u" "u" "i" "d"
 * TfrfBoxLength = BoxLength
 * TfrfBoxLongLength = LongBoxLength
 * TfrfBoxUUID = %xD4 %x80 %x7E %xF2 %xCA %x39 %x46 %x95
 * %x8E %x54 %x26 %xCB %x9E %x46 %xA7 %x9F
 * TfrfBoxFields = TfrfBoxVersion
 * TfrfBoxFlags
 * FragmentCount
 * (1* TfrfBoxDataFields32) / (1* TfrfBoxDataFields64)
 * TfrfBoxVersion = %x00 / %x01
 * TfrfBoxFlags = 24*24 RESERVED_BIT
 * FragmentCount = UINT8
 * TfrfBoxDataFields32 = FragmentAbsoluteTime32
 * FragmentDuration32
 * TfrfBoxDataFields64 = FragmentAbsoluteTime64
 * FragmentDuration64
 * FragmentAbsoluteTime64 = UNSIGNED_INT32
 * FragmentDuration64 = UNSIGNED_INT32
 * FragmentAbsoluteTime64 = UNSIGNED_INT64
 * FragmentDuration64 = UNSIGNED_INT64
 * TfrfBoxChildren = *( VendorExtensionUUIDBox )
 */
public class TfrfBox extends AbstractFullBox {
    public List<Entry> entries = new ArrayList<Entry>();

    public TfrfBox() {
        super("uuid");
    }

    @Override
    public byte[] getUserType() {
        return new byte[]{(byte) 0xd4, (byte) 0x80, (byte) 0x7e, (byte) 0xf2, (byte) 0xca, (byte) 0x39, (byte) 0x46,
                (byte) 0x95, (byte) 0x8e, (byte) 0x54, 0x26, (byte) 0xcb, (byte) 0x9e, (byte) 0x46, (byte) 0xa7, (byte) 0x9f};
    }

    @Override
    protected long getContentSize() {
        return 5 + entries.size() * (getVersion() == 0x01 ? 16 : 8);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt8(byteBuffer, entries.size());

        for (Entry entry : entries) {
            if (getVersion() == 0x01) {
                IsoTypeWriter.writeUInt64(byteBuffer, entry.fragmentAbsoluteTime);
                IsoTypeWriter.writeUInt64(byteBuffer, entry.fragmentAbsoluteDuration);
            } else {
                IsoTypeWriter.writeUInt32(byteBuffer, entry.fragmentAbsoluteTime);
                IsoTypeWriter.writeUInt32(byteBuffer, entry.fragmentAbsoluteDuration);
            }
        }
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        int fragmentCount = IsoTypeReader.readUInt8(content);

        for (int i = 0; i < fragmentCount; i++) {
            Entry entry = new Entry();
            if (getVersion() == 0x01) {
                entry.fragmentAbsoluteTime = IsoTypeReader.readUInt64(content);
                entry.fragmentAbsoluteDuration = IsoTypeReader.readUInt64(content);
            } else {
                entry.fragmentAbsoluteTime = IsoTypeReader.readUInt32(content);
                entry.fragmentAbsoluteDuration = IsoTypeReader.readUInt32(content);
            }
            entries.add(entry);
        }
    }


    public long getFragmentCount() {
        return entries.size();
    }

    public List<Entry> getEntries() {
        return entries;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("TfrfBox");
        sb.append("{entries=").append(entries);
        sb.append('}');
        return sb.toString();
    }

    public class Entry {
        long fragmentAbsoluteTime;
        long fragmentAbsoluteDuration;

        public long getFragmentAbsoluteTime() {
            return fragmentAbsoluteTime;
        }

        public long getFragmentAbsoluteDuration() {
            return fragmentAbsoluteDuration;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Entry");
            sb.append("{fragmentAbsoluteTime=").append(fragmentAbsoluteTime);
            sb.append(", fragmentAbsoluteDuration=").append(fragmentAbsoluteDuration);
            sb.append('}');
            return sb.toString();
        }
    }
}
