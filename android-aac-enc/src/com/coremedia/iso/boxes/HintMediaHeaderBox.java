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

import java.nio.ByteBuffer;

/**
 * The hint media header contains general information, independent of the protocaol, for hint tracks. Resides
 * in Media Information Box.
 *
 * @see com.coremedia.iso.boxes.MediaInformationBox
 */
public class HintMediaHeaderBox extends AbstractMediaHeaderBox {
    private int maxPduSize;
    private int avgPduSize;
    private long maxBitrate;
    private long avgBitrate;
    public static final String TYPE = "hmhd";

    public HintMediaHeaderBox() {
        super(TYPE);
    }

    public int getMaxPduSize() {
        return maxPduSize;
    }

    public int getAvgPduSize() {
        return avgPduSize;
    }

    public long getMaxBitrate() {
        return maxBitrate;
    }

    public long getAvgBitrate() {
        return avgBitrate;
    }

    protected long getContentSize() {
        return 20;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        maxPduSize = IsoTypeReader.readUInt16(content);
        avgPduSize = IsoTypeReader.readUInt16(content);
        maxBitrate = IsoTypeReader.readUInt32(content);
        avgBitrate = IsoTypeReader.readUInt32(content);
        IsoTypeReader.readUInt32(content);    // reserved!

    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt16(byteBuffer, maxPduSize);
        IsoTypeWriter.writeUInt16(byteBuffer, avgPduSize);
        IsoTypeWriter.writeUInt32(byteBuffer, maxBitrate);
        IsoTypeWriter.writeUInt32(byteBuffer, avgBitrate);
        IsoTypeWriter.writeUInt32(byteBuffer, 0);
    }

    @Override
    public String toString() {
        return "HintMediaHeaderBox{" +
                "maxPduSize=" + maxPduSize +
                ", avgPduSize=" + avgPduSize +
                ", maxBitrate=" + maxBitrate +
                ", avgBitrate=" + avgBitrate +
                '}';
    }
}
