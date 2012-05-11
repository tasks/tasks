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

package com.coremedia.iso;

import com.googlecode.mp4parser.AbstractContainerBox;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MovieBox;
import com.googlecode.mp4parser.annotations.DoNotParseDetail;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * The most upper container for ISO Boxes. It is a container box that is a file.
 * Uses IsoBufferWrapper  to access the underlying file.
 */
@DoNotParseDetail
public class IsoFile extends AbstractContainerBox {
    protected BoxParser boxParser = new PropertyBoxParserImpl();
    ReadableByteChannel byteChannel;

    public IsoFile() {
        super("");
    }

    public IsoFile(ReadableByteChannel byteChannel) throws IOException {
        super("");
        this.byteChannel = byteChannel;
        boxParser = createBoxParser();
        parse();
    }

    public IsoFile(ReadableByteChannel byteChannel, BoxParser boxParser) throws IOException {
        super("");
        this.byteChannel = byteChannel;
        this.boxParser = boxParser;
        parse();


    }

    protected BoxParser createBoxParser() {
        return new PropertyBoxParserImpl();
    }


    @Override
    public void _parseDetails(ByteBuffer content) {
        // there are no details to parse we should be just file
    }

    public void parse(ReadableByteChannel inFC, ByteBuffer header, long contentSize, AbstractBoxParser abstractBoxParser) throws IOException {
        throw new IOException("This method is not meant to be called. Use #parse() directly.");
    }

    private void parse() throws IOException {

        boolean done = false;
        while (!done) {
            try {
                Box box = boxParser.parseBox(byteChannel, this);
                if (box != null) {
                    //  System.err.println(box.getType());
                    boxes.add(box);
                } else {
                    done = true;
                }
            } catch (EOFException e) {
                done = true;
            }
        }
    }

    @DoNotParseDetail
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("IsoFile[");
        if (boxes == null) {
            buffer.append("unparsed");
        } else {
            for (int i = 0; i < boxes.size(); i++) {
                if (i > 0) {
                    buffer.append(";");
                }
                buffer.append(boxes.get(i).toString());
            }
        }
        buffer.append("]");
        return buffer.toString();
    }

    @DoNotParseDetail
    public static byte[] fourCCtoBytes(String fourCC) {
        byte[] result = new byte[4];
        if (fourCC != null) {
            for (int i = 0; i < Math.min(4, fourCC.length()); i++) {
                result[i] = (byte) fourCC.charAt(i);
            }
        }
        return result;
    }

    @DoNotParseDetail
    public static String bytesToFourCC(byte[] type) {
        byte[] result = new byte[]{0, 0, 0, 0};
        if (type != null) {
            System.arraycopy(type, 0, result, 0, Math.min(type.length, 4));
        }
        try {
            return new String(result, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new Error("Required character encoding is missing", e);
        }
    }


    @Override
    public long getNumOfBytesToFirstChild() {
        return 0;
    }

    @Override
    public long getSize() {
        long size = 0;
        for (Box box : boxes) {
            size += box.getSize();
        }
        return size;
    }

    @Override
    public IsoFile getIsoFile() {
        return this;
    }


    /**
     * Shortcut to get the MovieBox since it is often needed and present in
     * nearly all ISO 14496 files (at least if they are derived from MP4 ).
     *
     * @return the MovieBox or <code>null</code>
     */
    @DoNotParseDetail
    public MovieBox getMovieBox() {
        for (Box box : boxes) {
            if (box instanceof MovieBox) {
                return (MovieBox) box;
            }
        }
        return null;
    }

    public void getBox(WritableByteChannel os) throws IOException {
        for (Box box : boxes) {

            if (os instanceof FileChannel) {
                long startPos = ((FileChannel) os).position();
                box.getBox(os);
                long size = ((FileChannel) os).position() - startPos;
                assert size == box.getSize();
            } else {
                box.getBox(os);
            }

        }
    }
}
