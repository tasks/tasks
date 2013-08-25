/*
 * Copyright 2009 castLabs GmbH, Berlin
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

package com.coremedia.iso.boxes.fragment;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;

public class TrackFragmentBaseMediaDecodeTimeBox extends AbstractFullBox {
    public static final String TYPE = "tfdt";

    private long baseMediaDecodeTime;

    public TrackFragmentBaseMediaDecodeTimeBox() {
        super(TYPE);
    }

    @Override
    protected long getContentSize() {
        return getVersion() == 0 ? 8 : 12;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if (getVersion() == 1) {
            IsoTypeWriter.writeUInt64(byteBuffer, baseMediaDecodeTime);
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, baseMediaDecodeTime);
        }
    }


    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        if (getVersion() == 1) {
            baseMediaDecodeTime = IsoTypeReader.readUInt64(content);
        } else {
            baseMediaDecodeTime = IsoTypeReader.readUInt32(content);
        }

    }


    public long getBaseMediaDecodeTime() {
        return baseMediaDecodeTime;
    }

    public void setBaseMediaDecodeTime(long baseMediaDecodeTime) {
        this.baseMediaDecodeTime = baseMediaDecodeTime;
    }

    @Override
    public String toString() {
        return "TrackFragmentBaseMediaDecodeTimeBox{" +
                "baseMediaDecodeTime=" + baseMediaDecodeTime +
                '}';
    }
}
