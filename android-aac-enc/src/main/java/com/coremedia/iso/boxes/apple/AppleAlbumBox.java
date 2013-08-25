package com.coremedia.iso.boxes.apple;

/**
 *
 */
public final class AppleAlbumBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "\u00a9alb";


    public AppleAlbumBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getStringAppleDataBox();
    }

}