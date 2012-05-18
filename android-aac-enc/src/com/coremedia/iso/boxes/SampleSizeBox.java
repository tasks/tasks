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
 * This box containes the sample count and a table giving the size in bytes of each sample.
 * Defined in ISO/IEC 14496-12.
 */
public class SampleSizeBox extends AbstractFullBox {
    private long sampleSize;
    private long[] sampleSizes = new long[0];
    public static final String TYPE = "stsz";
    int sampleCount;

    public SampleSizeBox() {
        super(TYPE);
    }

    /**
     * Returns the field sample size.
     * If sampleSize > 0 every sample has the same size.
     * If sampleSize == 0 the samples have different size as stated in the sampleSizes field.
     *
     * @return the sampleSize field
     */
    public long getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(long sampleSize) {
        this.sampleSize = sampleSize;
    }


    public long getSampleSizeAtIndex(int index) {
        if (sampleSize > 0) {
            return sampleSize;
        } else {
            return sampleSizes[index];
        }
    }

    public long getSampleCount() {
        if (sampleSize > 0) {
            return sampleCount;
        } else {
            return sampleSizes.length;
        }

    }

    public long[] getSampleSizes() {
        return sampleSizes;
    }

    public void setSampleSizes(long[] sampleSizes) {
        this.sampleSizes = sampleSizes;
    }

    protected long getContentSize() {
        return 12 + (sampleSize == 0 ? sampleSizes.length * 4 : 0);
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        sampleSize = IsoTypeReader.readUInt32(content);
        sampleCount = l2i(IsoTypeReader.readUInt32(content));

        if (sampleSize == 0) {
            sampleSizes = new long[(int) sampleCount];

            for (int i = 0; i < sampleCount; i++) {
                sampleSizes[i] = IsoTypeReader.readUInt32(content);
            }
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, sampleSize);

        if (sampleSize == 0) {
            IsoTypeWriter.writeUInt32(byteBuffer, sampleSizes.length);
            for (long sampleSize1 : sampleSizes) {
                IsoTypeWriter.writeUInt32(byteBuffer, sampleSize1);
            }
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, sampleCount);
        }

    }

    public String toString() {
        return "SampleSizeBox[sampleSize=" + getSampleSize() + ";sampleCount=" + getSampleCount() + "]";
    }
}
