package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;

import java.nio.ByteBuffer;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * Abstract Chunk Offset Box
 */
public class ChunkOffset64BitBox extends ChunkOffsetBox {
    public static final String TYPE = "co64";
    private long[] chunkOffsets;

    public ChunkOffset64BitBox() {
        super(TYPE);
    }

    @Override
    public long[] getChunkOffsets() {
        return chunkOffsets;
    }

    @Override
    protected long getContentSize() {
        return 8 + 8 * chunkOffsets.length;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        int entryCount = l2i(IsoTypeReader.readUInt32(content));
        chunkOffsets = new long[entryCount];
        for (int i = 0; i < entryCount; i++) {
            chunkOffsets[i] = IsoTypeReader.readUInt64(content);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, chunkOffsets.length);
        for (long chunkOffset : chunkOffsets) {
            IsoTypeWriter.writeUInt64(byteBuffer, chunkOffset);
        }
    }


}
