package com.googlecode.mp4parser.boxes.piff;

import com.googlecode.mp4parser.boxes.AbstractSampleEncryptionBox;

/**
 * <pre>
 * aligned(8) class SampleEncryptionBox extends FullBox(‘uuid’, extended_type= 0xA2394F52-5A9B-4f14-A244-6C427C648DF4, version=0, flags=0)
 * {
 *  if (flags & 0x000001)
 *  {
 *   unsigned int(24) AlgorithmID;
 *   unsigned int(8) IV_size;
 *   unsigned int(8)[16] KID;
 *  }
 *  unsigned int (32) sample_count;
 *  {
 *   unsigned int(IV_size) InitializationVector;
 *   if (flags & 0x000002)
 *   {
 *    unsigned int(16) NumberOfEntries;
 *    {
 *     unsigned int(16) BytesOfClearData;
 *     unsigned int(32) BytesOfEncryptedData;
 *    } [ NumberOfEntries]
 *   }
 *  }[ sample_count ]
 * }
 * </pre>
 */
public class PiffSampleEncryptionBox extends AbstractSampleEncryptionBox {

    /**
     * Creates a AbstractSampleEncryptionBox for non-h264 tracks.
     */
    public PiffSampleEncryptionBox() {
        super("uuid");

    }

    @Override
    public byte[] getUserType() {
        return new byte[]{(byte) 0xA2, 0x39, 0x4F, 0x52, 0x5A, (byte) 0x9B, 0x4f, 0x14, (byte) 0xA2, 0x44, 0x6C, 0x42, 0x7C, 0x64, (byte) 0x8D, (byte) 0xF4};
    }

}
