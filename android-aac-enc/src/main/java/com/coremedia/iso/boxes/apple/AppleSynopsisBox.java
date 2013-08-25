package com.coremedia.iso.boxes.apple;

/**
 *
 */
public final class AppleSynopsisBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "ldes";


    public AppleSynopsisBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getStringAppleDataBox();
    }


}