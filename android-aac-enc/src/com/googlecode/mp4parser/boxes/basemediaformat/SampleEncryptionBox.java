package com.googlecode.mp4parser.boxes.basemediaformat;

import com.googlecode.mp4parser.boxes.AbstractSampleEncryptionBox;

/**
 * aligned(8) class AbstractSampleEncryptionBox extends FullBox(‘uuid’, extended_type= 0xA2394F52-5A9B-4f14-A244-6C427C648DF4, version=0, flags=0)
 * {
 * <p/>
 * unsigned int (32) sample_count;
 * {
 * unsigned int(16) InitializationVector;
 * }[ sample_count ]
 * }
 */
public class SampleEncryptionBox extends AbstractSampleEncryptionBox {

    /**
     * Creates a SampleEncryptionBox for non-h264 tracks.
     */
    public SampleEncryptionBox() {
        super("senc");

    }
}
