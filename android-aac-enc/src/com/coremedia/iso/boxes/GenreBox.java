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
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.Utf8;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;

/**
 * Containing genre information and contained in the <code>UserDataBox</code>.
 *
 * @see com.coremedia.iso.boxes.UserDataBox
 */
public class GenreBox extends AbstractFullBox {
    public static final String TYPE = "gnre";

    private String language;
    private String genre;

    public GenreBox() {
        super(TYPE);
    }

    public String getLanguage() {
        return language;
    }

    public String getGenre() {
        return genre;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    protected long getContentSize() {
        return 7 + Utf8.utf8StringLengthInBytes(genre);
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        language = IsoTypeReader.readIso639(content);
        genre = IsoTypeReader.readString(content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeIso639(byteBuffer, language);
        byteBuffer.put(Utf8.convert(genre));
        byteBuffer.put((byte) 0);
    }

    public String toString() {
        return "GenreBox[language=" + getLanguage() + ";genre=" + getGenre() + "]";
    }

}
