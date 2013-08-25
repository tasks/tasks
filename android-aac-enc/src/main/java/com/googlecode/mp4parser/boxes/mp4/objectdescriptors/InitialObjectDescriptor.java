/*
 * Copyright 2011 castLabs, Berlin
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

package com.googlecode.mp4parser.boxes.mp4.objectdescriptors;


import com.coremedia.iso.IsoTypeReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/*
class InitialObjectDescriptor extends ObjectDescriptorBase : bit(8)
tag=InitialObjectDescrTag {
bit(10) ObjectDescriptorID;
bit(1) URL_Flag;
bit(1) includeInlineProfileLevelFlag;
const bit(4) reserved=0b1111;
if (URL_Flag) {
bit(8) URLlength;
bit(8) URLstring[URLlength];
} else {
bit(8) ODProfileLevelIndication;
bit(8) sceneProfileLevelIndication;
bit(8) audioProfileLevelIndication;
bit(8) visualProfileLevelIndication;
bit(8) graphicsProfileLevelIndication;
ES_Descriptor esDescr[1 .. 255];
OCI_Descriptor ociDescr[0 .. 255];
IPMP_DescriptorPointer ipmpDescrPtr[0 .. 255];
IPMP_Descriptor ipmpDescr [0 .. 255];
IPMP_ToolListDescriptor toolListDescr[0 .. 1];
}
ExtensionDescriptor extDescr[0 .. 255];
}
*/
//@Descriptor(tags = {0x02, 0x10})
public class InitialObjectDescriptor extends ObjectDescriptorBase {
    private int objectDescriptorId;
    int urlFlag;
    int includeInlineProfileLevelFlag;

    int urlLength;
    String urlString;

    int oDProfileLevelIndication;
    int sceneProfileLevelIndication;
    int audioProfileLevelIndication;
    int visualProfileLevelIndication;
    int graphicsProfileLevelIndication;

    List<ESDescriptor> esDescriptors = new ArrayList<ESDescriptor>();

    List<ExtensionDescriptor> extensionDescriptors = new ArrayList<ExtensionDescriptor>();

    List<BaseDescriptor> unknownDescriptors = new ArrayList<BaseDescriptor>();

    @Override
    public void parseDetail(ByteBuffer bb) throws IOException {
        int data = IsoTypeReader.readUInt16(bb);
        objectDescriptorId = (data & 0xFFC0) >> 6;

        urlFlag = (data & 0x3F) >> 5;
        includeInlineProfileLevelFlag = (data & 0x1F) >> 4;

        int sizeLeft = getSize() - 2;
        if (urlFlag == 1) {
            urlLength = IsoTypeReader.readUInt8(bb);
            urlString = IsoTypeReader.readString(bb, urlLength);
            sizeLeft = sizeLeft - (1 + urlLength);
        } else {
            oDProfileLevelIndication = IsoTypeReader.readUInt8(bb);
            sceneProfileLevelIndication = IsoTypeReader.readUInt8(bb);
            audioProfileLevelIndication = IsoTypeReader.readUInt8(bb);
            visualProfileLevelIndication = IsoTypeReader.readUInt8(bb);
            graphicsProfileLevelIndication = IsoTypeReader.readUInt8(bb);

            sizeLeft = sizeLeft - 5;

            if (sizeLeft > 2) {
                final BaseDescriptor descriptor = ObjectDescriptorFactory.createFrom(-1, bb);
                sizeLeft = sizeLeft - descriptor.getSize();
                if (descriptor instanceof ESDescriptor) {
                    esDescriptors.add((ESDescriptor) descriptor);
                } else {
                    unknownDescriptors.add(descriptor);
                }
            }
        }

        if (sizeLeft > 2) {
            final BaseDescriptor descriptor = ObjectDescriptorFactory.createFrom(-1, bb);
            if (descriptor instanceof ExtensionDescriptor) {
                extensionDescriptors.add((ExtensionDescriptor) descriptor);
            } else {
                unknownDescriptors.add(descriptor);
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("InitialObjectDescriptor");
        sb.append("{objectDescriptorId=").append(objectDescriptorId);
        sb.append(", urlFlag=").append(urlFlag);
        sb.append(", includeInlineProfileLevelFlag=").append(includeInlineProfileLevelFlag);
        sb.append(", urlLength=").append(urlLength);
        sb.append(", urlString='").append(urlString).append('\'');
        sb.append(", oDProfileLevelIndication=").append(oDProfileLevelIndication);
        sb.append(", sceneProfileLevelIndication=").append(sceneProfileLevelIndication);
        sb.append(", audioProfileLevelIndication=").append(audioProfileLevelIndication);
        sb.append(", visualProfileLevelIndication=").append(visualProfileLevelIndication);
        sb.append(", graphicsProfileLevelIndication=").append(graphicsProfileLevelIndication);
        sb.append(", esDescriptors=").append(esDescriptors);
        sb.append(", extensionDescriptors=").append(extensionDescriptors);
        sb.append(", unknownDescriptors=").append(unknownDescriptors);
        sb.append('}');
        return sb.toString();
    }
}
