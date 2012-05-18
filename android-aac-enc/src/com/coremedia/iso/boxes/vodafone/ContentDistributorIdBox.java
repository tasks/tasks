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

package com.coremedia.iso.boxes.vodafone;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;

/**
 * Vodafone specific box. Usage unclear.
 */
public class ContentDistributorIdBox extends AbstractFullBox {
    public static final String TYPE = "cdis";

    private String language;
    private String contentDistributorId;

    public ContentDistributorIdBox() {
        super(TYPE);
    }

    public String getLanguage() {
        return language;
    }

    public String getContentDistributorId() {
        return contentDistributorId;
    }

    protected long getContentSize() {
        return 2 + Utf8.utf8StringLengthInBytes(contentDistributorId) + 5;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        language = IsoTypeReader.readIso639(content);
        contentDistributorId = IsoTypeReader.readString(content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeIso639(byteBuffer, language);
        byteBuffer.put(Utf8.convert(contentDistributorId));
        byteBuffer.put((byte) 0);

    }

    public String toString() {
        return "ContentDistributorIdBox[language=" + getLanguage() + ";contentDistributorId=" + getContentDistributorId() + "]";
    }
}
