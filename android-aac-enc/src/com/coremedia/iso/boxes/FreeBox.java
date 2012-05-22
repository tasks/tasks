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


import com.googlecode.mp4parser.AbstractBox;

import java.nio.ByteBuffer;

/**
 * A free box. Just a placeholder to enable editing without rewriting the whole file.
 */
public class FreeBox extends AbstractBox {
    public static final String TYPE = "free";
    ByteBuffer data;

    public FreeBox() {
        super(TYPE);
    }

    public FreeBox(int size) {
        super(TYPE);
        this.data = ByteBuffer.allocate(size);
    }

    @Override
    protected long getContentSize() {
        return data.limit();
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        data = content;
        data.position(data.position() + data.remaining());
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        data.rewind();
        byteBuffer.put(data);
    }

    public ByteBuffer getData() {
        return data;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }
}