package com.coremedia.iso.boxes.apple;

/**
 *
 */
public final class AppleNetworkBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "tvnn";


    public AppleNetworkBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getStringAppleDataBox();
    }


}