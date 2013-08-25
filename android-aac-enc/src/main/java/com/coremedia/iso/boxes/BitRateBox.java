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
import com.googlecode.mp4parser.AbstractBox;

import java.nio.ByteBuffer;

/**
 * <code>class BitRateBox extends Box('btrt') {<br/>
 * unsigned int(32) bufferSizeDB;<br/>
 * // gives the size of the decoding buffer for<br/>
 * // the elementary stream in bytes.<br/>
 * unsigned int(32) maxBitrate;<br/>
 * // gives the maximum rate in bits/second <br/>
 * // over any window of one second.<br/>
 * unsigned int(32) avgBitrate;<br/>
 * // avgBitrate gives the average rate in <br/>
 * // bits/second over the entire presentation.<br/>
 * }</code>
 */

public final class BitRateBox extends AbstractBox {
    public static final String TYPE = "btrt";

    private long bufferSizeDb;
    private long maxBitrate;
    private long avgBitrate;

    public BitRateBox() {
        super(TYPE);
    }

    protected long getContentSize() {
        return 12;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        bufferSizeDb = IsoTypeReader.readUInt32(content);
        maxBitrate = IsoTypeReader.readUInt32(content);
        avgBitrate = IsoTypeReader.readUInt32(content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        IsoTypeWriter.writeUInt32(byteBuffer, bufferSizeDb);
        IsoTypeWriter.writeUInt32(byteBuffer, maxBitrate);
        IsoTypeWriter.writeUInt32(byteBuffer, avgBitrate);
    }

    public long getBufferSizeDb() {
        return bufferSizeDb;
    }

    public void setBufferSizeDb(long bufferSizeDb) {
        this.bufferSizeDb = bufferSizeDb;
    }

    public long getMaxBitrate() {
        return maxBitrate;
    }

    public void setMaxBitrate(long maxBitrate) {
        this.maxBitrate = maxBitrate;
    }

    public long getAvgBitrate() {
        return avgBitrate;
    }

    public void setAvgBitrate(long avgBitrate) {
        this.avgBitrate = avgBitrate;
    }
}
