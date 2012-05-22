package com.googlecode.mp4parser.boxes.piff;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;

/**
 * The syntax of the fields defined in this section, specified in ABNF [RFC5234], is as follows:
 * TfxdBox = TfxdBoxLength TfxdBoxType [TfxdBoxLongLength] TfxdBoxUUID TfxdBoxFields
 * TfxdBoxChildren
 * TfxdBoxType = "u" "u" "i" "d"
 * TfxdBoxLength = BoxLength
 * TfxdBoxLongLength = LongBoxLength
 * TfxdBoxUUID = %x6D %x1D %x9B %x05 %x42 %xD5 %x44 %xE6
 * %x80 %xE2 %x14 %x1D %xAF %xF7 %x57 %xB2
 * TfxdBoxFields = TfxdBoxVersion
 * TfxdBoxFlags
 * TfxdBoxDataFields32 / TfxdBoxDataFields64
 * TfxdBoxVersion = %x00 / %x01
 * TfxdBoxFlags = 24*24 RESERVED_BIT
 * TfxdBoxDataFields32 = FragmentAbsoluteTime32
 * FragmentDuration32
 * TfxdBoxDataFields64 = FragmentAbsoluteTime64
 * FragmentDuration64
 * FragmentAbsoluteTime64 = UNSIGNED_INT32
 * FragmentDuration64 = UNSIGNED_INT32
 * FragmentAbsoluteTime64 = UNSIGNED_INT64
 * FragmentDuration64 = UNSIGNED_INT64
 * TfxdBoxChildren = *( VendorExtensionUUIDBox )
 */
//@ExtendedUserType(uuid = "6d1d9b05-42d5-44e6-80e2-141daff757b2")
public class TfxdBox extends AbstractFullBox {
    public long fragmentAbsoluteTime;
    public long fragmentAbsoluteDuration;

    public TfxdBox() {
        super("uuid");
    }

    @Override
    public byte[] getUserType() {
        return new byte[]{(byte) 0x6d, (byte) 0x1d, (byte) 0x9b, (byte) 0x05, (byte) 0x42, (byte) 0xd5, (byte) 0x44,
                (byte) 0xe6, (byte) 0x80, (byte) 0xe2, 0x14, (byte) 0x1d, (byte) 0xaf, (byte) 0xf7, (byte) 0x57, (byte) 0xb2};
    }

    @Override
    protected long getContentSize() {
        return getVersion() == 0x01 ? 20 : 12;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);

        if (getVersion() == 0x01) {
            fragmentAbsoluteTime = IsoTypeReader.readUInt64(content);
            fragmentAbsoluteDuration = IsoTypeReader.readUInt64(content);
        } else {
            fragmentAbsoluteTime = IsoTypeReader.readUInt32(content);
            fragmentAbsoluteDuration = IsoTypeReader.readUInt32(content);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if (getVersion() == 0x01) {
            IsoTypeWriter.writeUInt64(byteBuffer, fragmentAbsoluteTime);
            IsoTypeWriter.writeUInt64(byteBuffer, fragmentAbsoluteDuration);
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, fragmentAbsoluteTime);
            IsoTypeWriter.writeUInt32(byteBuffer, fragmentAbsoluteDuration);
        }
    }

    public long getFragmentAbsoluteTime() {
        return fragmentAbsoluteTime;
    }

    public long getFragmentAbsoluteDuration() {
        return fragmentAbsoluteDuration;
    }
}
