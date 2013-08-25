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

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * The chunk offset table gives the index of each chunk into the containing file. Defined in ISO/IEC 14496-12.
 */
public class StaticChunkOffsetBox extends ChunkOffsetBox {
    public static final String TYPE = "stco";

    private long[] chunkOffsets = new long[0];

    public StaticChunkOffsetBox() {
        super(TYPE);
    }

    public long[] getChunkOffsets() {
        return chunkOffsets;
    }

    protected long getContentSize() {
        return 8 + chunkOffsets.length * 4;
    }

    public void setChunkOffsets(long[] chunkOffsets) {
        this.chunkOffsets = chunkOffsets;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        int entryCount = l2i(IsoTypeReader.readUInt32(content));
        chunkOffsets = new long[entryCount];
        for (int i = 0; i < entryCount; i++) {
            chunkOffsets[i] = IsoTypeReader.readUInt32(content);
        }

    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, chunkOffsets.length);
        for (long chunkOffset : chunkOffsets) {
            IsoTypeWriter.writeUInt32(byteBuffer, chunkOffset);
        }
    }


}
