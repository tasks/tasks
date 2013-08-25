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
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;

/**
 * Only used within the DataReferenceBox. Find more information there.
 *
 * @see com.coremedia.iso.boxes.DataReferenceBox
 */
public class DataEntryUrnBox extends AbstractFullBox {
    private String name;
    private String location;
    public static final String TYPE = "urn ";

    public DataEntryUrnBox() {
        super(TYPE);
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    protected long getContentSize() {
        return Utf8.utf8StringLengthInBytes(name) + 1 + Utf8.utf8StringLengthInBytes(location) + 1;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        name = IsoTypeReader.readString(content);
        location = IsoTypeReader.readString(content);

    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        byteBuffer.put(Utf8.convert(name));
        byteBuffer.put((byte) 0);
        byteBuffer.put(Utf8.convert(location));
        byteBuffer.put((byte) 0);
    }

    public String toString() {
        return "DataEntryUrlBox[name=" + getName() + ";location=" + getLocation() + "]";
    }
}
