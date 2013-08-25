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

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * This box provides a compact marking of the random access points withinthe stream. The table is arranged in
 * strictly decreasinf order of sample number. Defined in ISO/IEC 14496-12.
 */
public class SyncSampleBox extends AbstractFullBox {
    public static final String TYPE = "stss";

    private long[] sampleNumber;

    public SyncSampleBox() {
        super(TYPE);
    }

    /**
     * Gives the numbers of the samples that are random access points in the stream.
     *
     * @return random access sample numbers.
     */
    public long[] getSampleNumber() {
        return sampleNumber;
    }

    protected long getContentSize() {
        return sampleNumber.length * 4 + 8;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        int entryCount = l2i(IsoTypeReader.readUInt32(content));

        sampleNumber = new long[entryCount];
        for (int i = 0; i < entryCount; i++) {
            sampleNumber[i] = IsoTypeReader.readUInt32(content);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);

        IsoTypeWriter.writeUInt32(byteBuffer, sampleNumber.length);

        for (long aSampleNumber : sampleNumber) {
            IsoTypeWriter.writeUInt32(byteBuffer, aSampleNumber);
        }

    }

    public String toString() {
        return "SyncSampleBox[entryCount=" + sampleNumber.length + "]";
    }

    public void setSampleNumber(long[] sampleNumber) {
        this.sampleNumber = sampleNumber;
    }
}
