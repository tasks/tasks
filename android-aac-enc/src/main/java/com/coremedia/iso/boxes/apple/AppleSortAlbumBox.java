package com.coremedia.iso.boxes.apple;

/**
 *
 */
public final class AppleSortAlbumBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "soal";


    public AppleSortAlbumBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getStringAppleDataBox();
    }
}