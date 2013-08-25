package com.coremedia.iso.boxes.apple;

/**
 * itunes MetaData comment box.
 */
public class AppleAlbumArtistBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "aART";


    public AppleAlbumArtistBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getStringAppleDataBox();
    }


}