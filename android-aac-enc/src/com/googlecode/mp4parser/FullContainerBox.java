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

package com.googlecode.mp4parser;

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;
import com.googlecode.mp4parser.util.ByteBufferByteChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Abstract base class for a full iso box only containing ither boxes.
 */
public abstract class FullContainerBox extends AbstractFullBox implements ContainerBox {
    protected List<Box> boxes = new LinkedList<Box>();
    private static Logger LOG = Logger.getLogger(FullContainerBox.class.getName());
    BoxParser boxParser;

    public void setBoxes(List<Box> boxes) {
        this.boxes = new LinkedList<Box>(boxes);
    }

    @SuppressWarnings("unchecked")
    public <T extends Box> List<T> getBoxes(Class<T> clazz) {
        return getBoxes(clazz, false);
    }

    @SuppressWarnings("unchecked")
    public <T extends Box> List<T> getBoxes(Class<T> clazz, boolean recursive) {
        List<T> boxesToBeReturned = new ArrayList<T>(2);
        for (Box boxe : boxes) { //clazz.isInstance(boxe) / clazz == boxe.getClass()?
            if (clazz == boxe.getClass()) {
                boxesToBeReturned.add((T) boxe);
            }

            if (recursive && boxe instanceof ContainerBox) {
                boxesToBeReturned.addAll((((ContainerBox) boxe).getBoxes(clazz, recursive)));
            }
        }
        // Optimize here! Spare object creation work on arrays directly! System.arrayCopy
        return boxesToBeReturned;
        //return (T[]) boxesToBeReturned.toArray();
    }

    protected long getContentSize() {
        long contentSize = 4; // flags and version
        for (Box boxe : boxes) {
            contentSize += boxe.getSize();
        }
        return contentSize;
    }

    public void addBox(Box b) {
        boxes.add(b);
    }

    public void removeBox(Box b) {
        boxes.remove(b);
    }

    public FullContainerBox(String type) {
        super(type);
    }

    public List<Box> getBoxes() {
        return boxes;
    }

    @Override
    public void parse(ReadableByteChannel readableByteChannel, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {
        super.parse(readableByteChannel, header, contentSize, boxParser);
        this.boxParser = boxParser;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        parseChildBoxes(content);
    }

    protected final void parseChildBoxes(ByteBuffer content) {
        try {
            while (content.remaining() >= 8) { //  8 is the minimal size for a sane box
                boxes.add(boxParser.parseBox(new ByteBufferByteChannel(content), this));
            }

            if (content.remaining() != 0) {
                setDeadBytes(content.slice());
                LOG.severe("Some sizes are wrong");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(this.getClass().getSimpleName()).append("[");
        for (int i = 0; i < boxes.size(); i++) {
            if (i > 0) {
                buffer.append(";");
            }
            buffer.append(boxes.get(i).toString());
        }
        buffer.append("]");
        return buffer.toString();
    }


    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        writeChildBoxes(byteBuffer);
    }

    protected final void writeChildBoxes(ByteBuffer bb) {
        WritableByteChannel wbc = new ByteBufferByteChannel(bb);
        for (Box box : boxes) {
            try {
                box.getBox(wbc);
            } catch (IOException e) {
                // cannot happen since my WritableByteChannel won't throw any excpetion
                throw new RuntimeException("Cannot happen.", e);
            }

        }
    }

    public long getNumOfBytesToFirstChild() {
        long sizeOfChildren = 0;
        for (Box box : boxes) {
            sizeOfChildren += box.getSize();
        }
        return getSize() - sizeOfChildren;
    }
}
