package com.coremedia.iso.boxes.apple;

/**
 * itunes MetaData comment box.
 */
public final class AppleEncoderBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "\u00a9too";


    public AppleEncoderBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getStringAppleDataBox();
    }

}