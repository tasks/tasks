/*  
 * Copyright 2008 CoreMedia AG, Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the License); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an AS IS BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */

package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;

/**
 * Describes the format of media access units in PDCF files.
 */
public final class OmaDrmAccessUnitFormatBox extends AbstractFullBox {
    public static final String TYPE = "odaf";

    private boolean selectiveEncryption;
    private byte allBits;

    private int keyIndicatorLength;
    private int initVectorLength;

    protected long getContentSize() {
        return 7;
    }

    public OmaDrmAccessUnitFormatBox() {
        super("odaf");
    }

    public boolean isSelectiveEncryption() {
        return selectiveEncryption;
    }

    public int getKeyIndicatorLength() {
        return keyIndicatorLength;
    }

    public int getInitVectorLength() {
        return initVectorLength;
    }

    public void setInitVectorLength(int initVectorLength) {
        this.initVectorLength = initVectorLength;
    }

    public void setKeyIndicatorLength(int keyIndicatorLength) {
        this.keyIndicatorLength = keyIndicatorLength;
    }

    public void setAllBits(byte allBits) {
        this.allBits = allBits;
        selectiveEncryption = (allBits & 0x80) == 0x80;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        allBits = (byte) IsoTypeReader.readUInt8(content);
        selectiveEncryption = (allBits & 0x80) == 0x80;
        keyIndicatorLength = IsoTypeReader.readUInt8(content);
        initVectorLength = IsoTypeReader.readUInt8(content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt8(byteBuffer, allBits);
        IsoTypeWriter.writeUInt8(byteBuffer, keyIndicatorLength);
        IsoTypeWriter.writeUInt8(byteBuffer, initVectorLength);
    }

}
