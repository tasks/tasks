package com.coremedia.iso.boxes.apple;

/**
 * Compilation.
 */
public final class AppleCompilationBox extends AbstractAppleMetaDataBox {
    public static final String TYPE = "cpil";


    public AppleCompilationBox() {
        super(TYPE);
        appleDataBox = AppleDataBox.getUint8AppleDataBox();
    }

}