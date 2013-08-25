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
import com.googlecode.mp4parser.AbstractBox;

import java.nio.ByteBuffer;

/**
 * Contains a reference to a track. The type of the box gives the kind of reference.
 */
public class TrackReferenceTypeBox extends AbstractBox {

    public static final String TYPE1 = "hint";
    public static final String TYPE2 = "cdsc";

    private long[] trackIds;

    public TrackReferenceTypeBox(String type) {
        super(type);
    }

    public long[] getTrackIds() {
        return trackIds;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        int count = (int) (content.remaining() / 4);
        trackIds = new long[count];
        for (int i = 0; i < count; i++) {
            trackIds[i] = IsoTypeReader.readUInt32(content);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        for (long trackId : trackIds) {
            IsoTypeWriter.writeUInt32(byteBuffer, trackId);
        }
    }


    protected long getContentSize() {
        return trackIds.length * 4;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("TrackReferenceTypeBox[type=").append(getType());
        for (int i = 0; i < trackIds.length; i++) {
            buffer.append(";trackId");
            buffer.append(i);
            buffer.append("=");
            buffer.append(trackIds[i]);
        }
        buffer.append("]");
        return buffer.toString();
    }
}
