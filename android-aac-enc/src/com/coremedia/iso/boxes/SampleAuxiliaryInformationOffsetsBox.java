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

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/*
aligned(8) class SampleAuxiliaryInformationOffsetsBox
            extends FullBox(‘saio’, version, flags)
{
            if (flags & 1) {
                        unsigned int(32) aux_info_type;
                        unsigned int(32) aux_info_type_parameter;
            }
            unsigned int(32) entry_count;
            if ( version == 0 )
            {
                        unsigned int(32) offset[ entry_count ];
            }
            else
            {
                        unsigned int(64) offset[ entry_count ];
            }
}
 */
public class SampleAuxiliaryInformationOffsetsBox extends AbstractFullBox {
    public static final String TYPE = "saio";

    private List<Long> offsets = new LinkedList<Long>();
    private long auxInfoType;
    private long auxInfoTypeParameter;

    public SampleAuxiliaryInformationOffsetsBox() {
        super(TYPE);
    }

    @Override
    protected long getContentSize() {
        return 8 + (getVersion() == 0 ? 4 * offsets.size() : 8 * offsets.size()) + ((getFlags() & 1) == 1 ? 8 : 0);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if ((getFlags() & 1) == 1) {
            IsoTypeWriter.writeUInt32(byteBuffer, auxInfoType);
            IsoTypeWriter.writeUInt32(byteBuffer, auxInfoTypeParameter);
        }

        IsoTypeWriter.writeUInt32(byteBuffer, offsets.size());
        for (Long offset : offsets) {
            if (getVersion() == 0) {
                IsoTypeWriter.writeUInt32(byteBuffer, offset);
            } else {
                IsoTypeWriter.writeUInt64(byteBuffer, offset);
            }
        }
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);

        if ((getFlags() & 1) == 1) {
            auxInfoType = IsoTypeReader.readUInt32(content);
            auxInfoTypeParameter = IsoTypeReader.readUInt32(content);
        }

        int entryCount = l2i(IsoTypeReader.readUInt32(content));
        offsets.clear();

        for (int i = 0; i < entryCount; i++) {
            if (getVersion() == 0) {
                offsets.add(IsoTypeReader.readUInt32(content));
            } else {
                offsets.add(IsoTypeReader.readUInt64(content));
            }
        }
    }


    public long getAuxInfoType() {
        return auxInfoType;
    }

    public void setAuxInfoType(long auxInfoType) {
        this.auxInfoType = auxInfoType;
    }

    public long getAuxInfoTypeParameter() {
        return auxInfoTypeParameter;
    }

    public void setAuxInfoTypeParameter(long auxInfoTypeParameter) {
        this.auxInfoTypeParameter = auxInfoTypeParameter;
    }

    public List<Long> getOffsets() {
        return offsets;
    }

    public void setOffsets(List<Long> offsets) {
        this.offsets = offsets;
    }
}
