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

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;

/**
 * The Scheme Type Box identifies the protection scheme. Resides in  a Protection Scheme Information Box or
 * an SRTP Process Box.
 *
 * @see com.coremedia.iso.boxes.SchemeInformationBox
 */
public class SchemeTypeBox extends AbstractFullBox {
    public static final String TYPE = "schm";
    String schemeType = "    ";
    long schemeVersion;
    String schemeUri = null;

    public SchemeTypeBox() {
        super(TYPE);
    }

    public String getSchemeType() {
        return schemeType;
    }

    public long getSchemeVersion() {
        return schemeVersion;
    }

    public String getSchemeUri() {
        return schemeUri;
    }

    public void setSchemeType(String schemeType) {
        assert schemeType != null && schemeType.length() == 4 : "SchemeType may not be null or not 4 bytes long";
        this.schemeType = schemeType;
    }

    public void setSchemeVersion(int schemeVersion) {
        this.schemeVersion = schemeVersion;
    }

    public void setSchemeUri(String schemeUri) {
        this.schemeUri = schemeUri;
    }

    protected long getContentSize() {
        return 12 + (((getFlags() & 1) == 1) ? Utf8.utf8StringLengthInBytes(schemeUri) + 1 : 0);
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        schemeType = IsoTypeReader.read4cc(content);
        schemeVersion = IsoTypeReader.readUInt32(content);
        if ((getFlags() & 1) == 1) {
            schemeUri = IsoTypeReader.readString(content);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        byteBuffer.put(IsoFile.fourCCtoBytes(schemeType));
        IsoTypeWriter.writeUInt32(byteBuffer, schemeVersion);
        if ((getFlags() & 1) == 1) {
            byteBuffer.put(Utf8.convert(schemeUri));
        }
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Schema Type Box[");
        buffer.append("schemeUri=").append(schemeUri).append("; ");
        buffer.append("schemeType=").append(schemeType).append("; ");
        buffer.append("schemeVersion=").append(schemeUri).append("; ");
        buffer.append("]");
        return buffer.toString();
    }
}
