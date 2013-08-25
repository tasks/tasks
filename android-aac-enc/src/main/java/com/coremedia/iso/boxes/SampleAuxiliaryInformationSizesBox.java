/*
 * Copyright 2009 castLabs GmbH, Berlin
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

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

public class SampleAuxiliaryInformationSizesBox extends AbstractFullBox {
    public static final String TYPE = "saiz";

    private int defaultSampleInfoSize;
    private List<Short> sampleInfoSizes = new LinkedList<Short>();
    private int sampleCount;
    private String auxInfoType;
    private String auxInfoTypeParameter;

    public SampleAuxiliaryInformationSizesBox() {
        super(TYPE);
    }

    @Override
    protected long getContentSize() {
        int size = 4;
        if ((getFlags() & 1) == 1) {
            size += 8;
        }

        size += 5;
        size += sampleInfoSizes.size();
        return size;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if ((getFlags() & 1) == 1) {
            byteBuffer.put(IsoFile.fourCCtoBytes(auxInfoType));
            byteBuffer.put(IsoFile.fourCCtoBytes(auxInfoTypeParameter));
        }

        IsoTypeWriter.writeUInt8(byteBuffer, defaultSampleInfoSize);
        if (defaultSampleInfoSize == 0) {
            IsoTypeWriter.writeUInt32(byteBuffer, sampleInfoSizes.size());
            for (short sampleInfoSize : sampleInfoSizes) {
                IsoTypeWriter.writeUInt8(byteBuffer, sampleInfoSize);
            }
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, sampleCount);
        }
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        if ((getFlags() & 1) == 1) {
            auxInfoType = IsoTypeReader.read4cc(content);
            auxInfoTypeParameter = IsoTypeReader.read4cc(content);
        }

        defaultSampleInfoSize = (short) IsoTypeReader.readUInt8(content);
        int sampleCount = l2i(IsoTypeReader.readUInt32(content));

        sampleInfoSizes.clear();

        for (int i = 0; i < sampleCount; i++) {
            sampleInfoSizes.add((short) IsoTypeReader.readUInt8(content));
        }
    }

    public String getAuxInfoType() {
        return auxInfoType;
    }

    public void setAuxInfoType(String auxInfoType) {
        this.auxInfoType = auxInfoType;
    }

    public String getAuxInfoTypeParameter() {
        return auxInfoTypeParameter;
    }

    public void setAuxInfoTypeParameter(String auxInfoTypeParameter) {
        this.auxInfoTypeParameter = auxInfoTypeParameter;
    }

    public int getDefaultSampleInfoSize() {
        return defaultSampleInfoSize;
    }

    public void setDefaultSampleInfoSize(int defaultSampleInfoSize) {
        assert defaultSampleInfoSize <= 255;
        assert defaultSampleInfoSize > 0;
        this.defaultSampleInfoSize = defaultSampleInfoSize;
    }


    public List<Short> getSampleInfoSizes() {
        return sampleInfoSizes;
    }

    public void setSampleInfoSizes(List<Short> sampleInfoSizes) {
        this.sampleInfoSizes = sampleInfoSizes;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }
}
