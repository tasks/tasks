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
 * Special box used by Vodafone in their DCF containing information about the artist. Mainly used for OMA DCF files
 * containing music. Resides in the {@link com.coremedia.iso.boxes.UserDataBox}.
 */
public class AlbumArtistBox extends AbstractFullBox {
    public static final String TYPE = "albr";

    private String language;
    private String albumArtist;

    public AlbumArtistBox() {
        super(TYPE);
    }

    public String getLanguage() {
        return language;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }

    protected long getContentSize() {
        return 6 + Utf8.utf8StringLengthInBytes(albumArtist) + 1;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        language = IsoTypeReader.readIso639(content);
        albumArtist = IsoTypeReader.readString(content);
    }

    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeIso639(byteBuffer, language);
        byteBuffer.put(Utf8.convert(albumArtist));
        byteBuffer.put((byte) 0);
    }

    public String toString() {
        return "AlbumArtistBox[language=" + getLanguage() + ";albumArtist=" + getAlbumArtist() + "]";
    }
}
