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

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.boxes.ContainerBox;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Defines basic interaction possibilities for any ISO box. Each box has a parent box and a type.
 */
public interface Box {
    ContainerBox getParent();

    void setParent(ContainerBox parent);

    long getSize();

    /**
     * The box's 4-cc type.
     * @return the 4 character type of the box
     */
    String getType();

    /**
     * Writes the complete box - size | 4-cc | content - to the given <code>writableByteChannel</code>.
     * @param writableByteChannel the box's sink
     * @throws IOException in case of problems with the <code>Channel</code>
     */
    void getBox(WritableByteChannel writableByteChannel) throws IOException;

    void parse(ReadableByteChannel readableByteChannel, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException;
}
