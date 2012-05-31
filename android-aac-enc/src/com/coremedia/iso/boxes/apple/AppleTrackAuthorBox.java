package com.coremedia.iso.boxes.apple;

/**
 *
 */
public final class AppleTrackAuthorBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "\u00a9wrt";


    public AppleTrackAuthorBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getStringAppleDataBox();
    }


}