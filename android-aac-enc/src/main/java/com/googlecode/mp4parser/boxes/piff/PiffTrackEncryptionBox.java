package com.googlecode.mp4parser.boxes.piff;

import com.googlecode.mp4parser.boxes.AbstractTrackEncryptionBox;

/**
 * aligned(8) class TrackEncryptionBox extends FullBox(‘uuid’,
 * extended_type=0x8974dbce-7be7-4c51-84f9-7148f9882554, version=0,
 * flags=0)
 * {
 * unsigned int(24) default_AlgorithmID;
 * unsigned int(8) default_IV_size;
 * unsigned int(8)[16] default_KID;
 * }
 */
public class PiffTrackEncryptionBox extends AbstractTrackEncryptionBox {


    public PiffTrackEncryptionBox() {
        super("uuid");
    }

    @Override
    public byte[] getUserType() {
        return new byte[]{(byte) 0x89, 0x74, (byte) 0xdb, (byte) 0xce, 0x7b, (byte) 0xe7, 0x4c, 0x51,
                (byte) 0x84, (byte) 0xf9, 0x71, 0x48, (byte) 0xf9, (byte) 0x88, 0x25, 0x54};
    }

    @Override
    public int getFlags() {
        return 0;
    }


}
