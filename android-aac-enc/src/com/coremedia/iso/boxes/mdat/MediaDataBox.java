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

package com.coremedia.iso.boxes.mdat;

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.ChannelHelper;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * This box contains the media data. In video tracks, this box would contain video frames. A presentation may
 * contain zero or more Media Data Boxes. The actual media data follows the type field; its structure is described
 * by the metadata (see {@link com.coremedia.iso.boxes.SampleTableBox}).<br>
 * In large presentations, it may be desirable to have more data in this box than a 32-bit size would permit. In this
 * case, the large variant of the size field is used.<br>
 * There may be any number of these boxes in the file (including zero, if all the media data is in other files). The
 * metadata refers to media data by its absolute offset within the file (see {@link com.coremedia.iso.boxes.StaticChunkOffsetBox});
 * so Media Data Box headers and free space may easily be skipped, and files without any box structure may
 * also be referenced and used.
 */
public final class MediaDataBox implements Box {
    public static final String TYPE = "mdat";
    ContainerBox parent;

    ByteBuffer header;
    ByteBuffer content;


    public ContainerBox getParent() {
        return parent;
    }

    public void setParent(ContainerBox parent) {
        this.parent = parent;
    }

    public String getType() {
        return TYPE;
    }

    public void getBox(WritableByteChannel writableByteChannel) throws IOException {
        header.rewind();
        content.rewind();
        writableByteChannel.write(header);
        writableByteChannel.write(content);
    }

    public long getSize() {
        return header.limit() + content.limit();
    }

    public void parse(ReadableByteChannel readableByteChannel, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {
        this.header = header;
        if (readableByteChannel instanceof FileChannel && contentSize > 1024 * 1024) {
            // It's quite expensive to map a file into the memory. Just do it when the box is larger than a MB.
            content = ((FileChannel) readableByteChannel).map(FileChannel.MapMode.READ_ONLY, ((FileChannel) readableByteChannel).position(), contentSize);
            ((FileChannel) readableByteChannel).position(((FileChannel) readableByteChannel).position() + contentSize);
        } else {
            content = ChannelHelper.readFully(readableByteChannel, l2i(contentSize));
        }


    }

    public ByteBuffer getContent() {
        return content;
    }

    public ByteBuffer getHeader() {
        return header;
    }
}
