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
 * Contained a the <code>UserDataBox</code> and containing information about the media's rating. E.g.
 * PG13or FSK16.
 */
public class RatingBox extends AbstractFullBox {
    public static final String TYPE = "rtng";

    private String ratingEntity;
    private String ratingCriteria;
    private String language;
    private String ratingInfo;

    public RatingBox() {
        super(TYPE);
    }


    public void setRatingEntity(String ratingEntity) {
        this.ratingEntity = ratingEntity;
    }

    public void setRatingCriteria(String ratingCriteria) {
        this.ratingCriteria = ratingCriteria;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setRatingInfo(String ratingInfo) {
        this.ratingInfo = ratingInfo;
    }

    public String getLanguage() {
        return language;
    }

    /**
     * Gets a four-character code that indicates the rating entity grading the asset, e.g., 'BBFC'. The values of this
     * field should follow common names of worldwide movie rating systems, such as those mentioned in
     * [http://www.movie-ratings.net/, October 2002].
     *
     * @return the rating organization
     */
    public String getRatingEntity() {
        return ratingEntity;
    }

    /**
     * Gets the four-character code that indicates which rating criteria are being used for the corresponding rating
     * entity, e.g., 'PG13'.
     *
     * @return the actual rating
     */
    public String getRatingCriteria() {
        return ratingCriteria;
    }

    public String getRatingInfo() {
        return ratingInfo;
    }

    protected long getContentSize() {
        return 15 + Utf8.utf8StringLengthInBytes(ratingInfo);
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        ratingEntity = IsoTypeReader.read4cc(content);
        ratingCriteria = IsoTypeReader.read4cc(content);
        language = IsoTypeReader.readIso639(content);
        ratingInfo = IsoTypeReader.readString(content);

    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        byteBuffer.put(IsoFile.fourCCtoBytes(ratingEntity));
        byteBuffer.put(IsoFile.fourCCtoBytes(ratingCriteria));
        IsoTypeWriter.writeIso639(byteBuffer, language);
        byteBuffer.put(Utf8.convert(ratingInfo));
        byteBuffer.put((byte) 0);
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("RatingBox[language=").append(getLanguage());
        buffer.append("ratingEntity=").append(getRatingEntity());
        buffer.append(";ratingCriteria=").append(getRatingCriteria());
        buffer.append(";language=").append(getLanguage());
        buffer.append(";ratingInfo=").append(getRatingInfo());
        buffer.append("]");
        return buffer.toString();
    }
}
