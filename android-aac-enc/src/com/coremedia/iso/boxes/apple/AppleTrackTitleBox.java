package com.coremedia.iso.boxes.apple;

/**
 *
 */
public final class AppleTrackTitleBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "\u00a9nam";


    public AppleTrackTitleBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getStringAppleDataBox();
    }

}
