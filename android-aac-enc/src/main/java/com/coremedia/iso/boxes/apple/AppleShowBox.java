package com.coremedia.iso.boxes.apple;

/**
 *
 */
public final class AppleShowBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "tvsh";


    public AppleShowBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getStringAppleDataBox();
    }

}