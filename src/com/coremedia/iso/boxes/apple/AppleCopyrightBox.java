package com.coremedia.iso.boxes.apple;

/**
 * itunes MetaData comment box.
 */
public final class AppleCopyrightBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "cprt";


    public AppleCopyrightBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getStringAppleDataBox();
    }

}