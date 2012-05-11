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
import com.googlecode.mp4parser.AbstractContainerBox;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * This box contains objects that declare user information about the containing box and its data (presentation or
 * track).<br>
 * The User Data Box is a container box for informative user-data. This user data is formatted as a set of boxes
 * with more specific box types, which declare more precisely their content
 */
public class UserDataBox extends AbstractContainerBox {
    public static final String TYPE = "udta";

    @Override
    protected long getContentSize() {
        return super.getContentSize();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void parse(ReadableByteChannel readableByteChannel, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {
        super.parse(readableByteChannel, header, contentSize, boxParser);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        super._parseDetails(content);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        super.getContent(byteBuffer);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public UserDataBox() {
        super(TYPE);
    }

}
