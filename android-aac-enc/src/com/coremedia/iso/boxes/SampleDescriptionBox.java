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

import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;
import com.googlecode.mp4parser.FullContainerBox;

import java.nio.ByteBuffer;

/**
 * The sample description table gives detailed information about the coding type used, and any initialization
 * information needed for that coding. <br>
 * The information stored in the sample description box after the entry-count is both track-type specific as
 * documented here, and can also have variants within a track type (e.g. different codings may use different
 * specific information after some common fields, even within a video track).<br>
 * For video tracks, a VisualSampleEntry is used; for audio tracks, an AudioSampleEntry. Hint tracks use an
 * entry format specific to their protocol, with an appropriate name. Timed Text tracks use a TextSampleEntry
 * For hint tracks, the sample description contains appropriate declarative data for the streaming protocol being
 * used, and the format of the hint track. The definition of the sample description is specific to the protocol.
 * Multiple descriptions may be used within a track.<br>
 * The 'protocol' and 'codingname' fields are registered identifiers that uniquely identify the streaming protocol or
 * compression format decoder to be used. A given protocol or codingname may have optional or required
 * extensions to the sample description (e.g. codec initialization parameters). All such extensions shall be within
 * boxes; these boxes occur after the required fields. Unrecognized boxes shall be ignored.
 * <br>
 * Defined in ISO/IEC 14496-12
 *
 * @see com.coremedia.iso.boxes.sampleentry.VisualSampleEntry
 * @see com.coremedia.iso.boxes.sampleentry.TextSampleEntry
 * @see com.coremedia.iso.boxes.sampleentry.AudioSampleEntry
 */
public class SampleDescriptionBox extends FullContainerBox {
    public static final String TYPE = "stsd";

    public SampleDescriptionBox() {
        super(TYPE);
    }

    @Override
    protected long getContentSize() {
        return super.getContentSize() + 4;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        content.get(new byte[4]);
        parseChildBoxes(content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, boxes.size());
        writeChildBoxes(byteBuffer);
    }

    public SampleEntry getSampleEntry() {
        for (Box box : boxes) {
            if (box instanceof SampleEntry) {
                return (SampleEntry) box;
            }
        }
        return null;
    }
}
