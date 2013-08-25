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
 * A user specifc box. See ISO/IEC 14496-12 for details.
 */
public class UserBox extends AbstractBox {
    byte[] data;
    public static final String TYPE = "uuid";

    public UserBox(byte[] userType) {
        super(TYPE, userType);
    }


    protected long getContentSize() {
        return data.length;
    }

    public String toString() {
        return "UserBox[type=" + (getType()) +
                ";userType=" + new String(getUserType()) +
                ";contentLength=" + data.length + "]";
    }


    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        data = new byte[content.remaining()];
        content.get(data);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        byteBuffer.put(data);
    }
}
