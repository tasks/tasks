package com.coremedia.iso.boxes.apple;

/**
 *
 */
public final class ApplePurchaseDateBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "purd";


    public ApplePurchaseDateBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getStringAppleDataBox();
    }

}