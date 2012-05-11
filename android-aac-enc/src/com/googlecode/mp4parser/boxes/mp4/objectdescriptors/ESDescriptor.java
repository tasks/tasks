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
import com.coremedia.iso.IsoTypeWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/*
class ES_Descriptor extends BaseDescriptor : bit(8) tag=ES_DescrTag {
bit(16) ES_ID;
bit(1) streamDependenceFlag;
bit(1) URL_Flag;
bit(1) OCRstreamFlag;
bit(5) streamPriority;
if (streamDependenceFlag)
bit(16) dependsOn_ES_ID;
if (URL_Flag) {
bit(8) URLlength;
bit(8) URLstring[URLlength];
}
if (OCRstreamFlag)
bit(16) OCR_ES_Id;
DecoderConfigDescriptor decConfigDescr;
if (ODProfileLevelIndication==0x01) //no SL extension.
{
SLConfigDescriptor slConfigDescr;
}
else // SL extension is possible.
{
SLConfigDescriptor slConfigDescr;
}
IPI_DescrPointer ipiPtr[0 .. 1];
IP_IdentificationDataSet ipIDS[0 .. 255];
IPMP_DescriptorPointer ipmpDescrPtr[0 .. 255];
LanguageDescriptor langDescr[0 .. 255];
QoS_Descriptor qosDescr[0 .. 1];
RegistrationDescriptor regDescr[0 .. 1];
ExtensionDescriptor extDescr[0 .. 255];
}
 */
@Descriptor(tags = {0x03})
public class ESDescriptor extends BaseDescriptor {
    private static Logger log = Logger.getLogger(ESDescriptor.class.getName());

    int esId;
    int streamDependenceFlag;
    int URLFlag;
    int oCRstreamFlag;
    int streamPriority;


    int URLLength = 0;
    String URLString;
    int remoteODFlag;

    int dependsOnEsId;
    int oCREsId;

    DecoderConfigDescriptor decoderConfigDescriptor;
    SLConfigDescriptor slConfigDescriptor;
    List<BaseDescriptor> otherDescriptors = new ArrayList<BaseDescriptor>();

    @Override
    public void parseDetail(ByteBuffer bb) throws IOException {
        esId = IsoTypeReader.readUInt16(bb);

        int data = IsoTypeReader.readUInt8(bb);
        streamDependenceFlag = data >>> 7;
        URLFlag = (data >>> 6) & 0x1;
        oCRstreamFlag = (data >>> 5) & 0x1;
        streamPriority = data & 0x1f;

        if (streamDependenceFlag == 1) {
            dependsOnEsId = IsoTypeReader.readUInt16(bb);
        }
        if (URLFlag == 1) {
            URLLength = IsoTypeReader.readUInt8(bb);
            URLString = IsoTypeReader.readString(bb, URLLength);
        }
        if (oCRstreamFlag == 1) {
            oCREsId = IsoTypeReader.readUInt16(bb);
        }

        int baseSize = 1 /*tag*/ + getSizeBytes() + 2 + 1 + (streamDependenceFlag == 1 ? 2 : 0) + (URLFlag == 1 ? 1 + URLLength : 0) + (oCRstreamFlag == 1 ? 2 : 0);

        int begin = bb.position();
        if (getSize() > baseSize + 2) {
            BaseDescriptor descriptor = ObjectDescriptorFactory.createFrom(-1, bb);
            final long read = bb.position() - begin;
            log.finer(descriptor + " - ESDescriptor1 read: " + read + ", size: " + (descriptor != null ? descriptor.getSize() : null));
            if (descriptor != null) {
                final int size = descriptor.getSize();
                bb.position(begin + size);
                baseSize += size;
            } else {
                baseSize += read;
            }
            if (descriptor instanceof DecoderConfigDescriptor) {
                decoderConfigDescriptor = (DecoderConfigDescriptor) descriptor;
            }
        }

        begin = bb.position();
        if (getSize() > baseSize + 2) {
            BaseDescriptor descriptor = ObjectDescriptorFactory.createFrom(-1, bb);
            final long read = bb.position() - begin;
            log.finer(descriptor + " - ESDescriptor2 read: " + read + ", size: " + (descriptor != null ? descriptor.getSize() : null));
            if (descriptor != null) {
                final int size = descriptor.getSize();
                bb.position(begin + size);
                baseSize += size;
            } else {
                baseSize += read;
            }
            if (descriptor instanceof SLConfigDescriptor) {
                slConfigDescriptor = (SLConfigDescriptor) descriptor;
            }
        } else {
            log.warning("SLConfigDescriptor is missing!");
        }

        while (getSize() - baseSize > 2) {
            begin = bb.position();
            BaseDescriptor descriptor = ObjectDescriptorFactory.createFrom(-1, bb);
            final long read = bb.position() - begin;
            log.finer(descriptor + " - ESDescriptor3 read: " + read + ", size: " + (descriptor != null ? descriptor.getSize() : null));
            if (descriptor != null) {
                final int size = descriptor.getSize();
                bb.position(begin + size);
                baseSize += size;
            } else {
                baseSize += read;
            }
            otherDescriptors.add(descriptor);
        }
    }
    public int serializedSize() {
        int out = 5;
        if (streamDependenceFlag > 0) {
            out += 2;
        }
        if (URLFlag > 0) {
            out += 1 + URLLength;
        }
        if (oCRstreamFlag > 0) {
            out += 2;
        }

        out += decoderConfigDescriptor.serializedSize();
        out += slConfigDescriptor.serializedSize();

        // Doesn't handle other descriptors yet

        return out;
    }

    public ByteBuffer serialize() {
        ByteBuffer out = ByteBuffer.allocate(serializedSize()); // Usually is around 30 bytes, so 200 should be enough...
        IsoTypeWriter.writeUInt8(out, 3);
        IsoTypeWriter.writeUInt8(out, serializedSize() - 2); // Not OK for longer sizes!
        IsoTypeWriter.writeUInt16(out, esId);
        int flags = (streamDependenceFlag << 7) | (URLFlag << 6) | (oCRstreamFlag << 5) | (streamPriority & 0x1f);
        IsoTypeWriter.writeUInt8(out, flags);
        if (streamDependenceFlag > 0) {
            IsoTypeWriter.writeUInt16(out, dependsOnEsId);
        }
        if (URLFlag > 0) {
            IsoTypeWriter.writeUInt8(out, URLLength);
            IsoTypeWriter.writeUtf8String(out, URLString);
        }
        if (oCRstreamFlag > 0) {
            IsoTypeWriter.writeUInt16(out, oCREsId);
        }

        ByteBuffer dec = decoderConfigDescriptor.serialize();
        ByteBuffer sl = slConfigDescriptor.serialize();
        out.put(dec.array());
        out.put(sl.array());

        // Doesn't handle other descriptors yet

        return out;
    }

//  @Override
//  public int getSize() {
//    return 3 + (streamDependenceFlag == 1 ? 2 : 0) +
//            (URLFlag == 1 ? 1 + 8 * URLLength : 0) +
//            (oCRstreamFlag == 1 ? 2 : 0);
//  }

    public DecoderConfigDescriptor getDecoderConfigDescriptor() {
        return decoderConfigDescriptor;
    }

    public SLConfigDescriptor getSlConfigDescriptor() {
        return slConfigDescriptor;
    }

    public void setDecoderConfigDescriptor(DecoderConfigDescriptor decoderConfigDescriptor) {
        this.decoderConfigDescriptor = decoderConfigDescriptor;
    }

    public void setSlConfigDescriptor(SLConfigDescriptor slConfigDescriptor) {
        this.slConfigDescriptor = slConfigDescriptor;
    }

    public List<BaseDescriptor> getOtherDescriptors() {
        return otherDescriptors;
    }

    public int getoCREsId() {
        return oCREsId;
    }

    public void setoCREsId(int oCREsId) {
        this.oCREsId = oCREsId;
    }

    public int getEsId() {
        return esId;
    }

    public void setEsId(int esId) {
        this.esId = esId;
    }

    public int getStreamDependenceFlag() {
        return streamDependenceFlag;
    }

    public void setStreamDependenceFlag(int streamDependenceFlag) {
        this.streamDependenceFlag = streamDependenceFlag;
    }

    public int getURLFlag() {
        return URLFlag;
    }

    public void setURLFlag(int URLFlag) {
        this.URLFlag = URLFlag;
    }

    public int getoCRstreamFlag() {
        return oCRstreamFlag;
    }

    public void setoCRstreamFlag(int oCRstreamFlag) {
        this.oCRstreamFlag = oCRstreamFlag;
    }

    public int getStreamPriority() {
        return streamPriority;
    }

    public void setStreamPriority(int streamPriority) {
        this.streamPriority = streamPriority;
    }

    public int getURLLength() {
        return URLLength;
    }

    public void setURLLength(int URLLength) {
        this.URLLength = URLLength;
    }

    public String getURLString() {
        return URLString;
    }

    public void setURLString(String URLString) {
        this.URLString = URLString;
    }

    public int getRemoteODFlag() {
        return remoteODFlag;
    }

    public void setRemoteODFlag(int remoteODFlag) {
        this.remoteODFlag = remoteODFlag;
    }

    public int getDependsOnEsId() {
        return dependsOnEsId;
    }

    public void setDependsOnEsId(int dependsOnEsId) {
        this.dependsOnEsId = dependsOnEsId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ESDescriptor");
        sb.append("{esId=").append(esId);
        sb.append(", streamDependenceFlag=").append(streamDependenceFlag);
        sb.append(", URLFlag=").append(URLFlag);
        sb.append(", oCRstreamFlag=").append(oCRstreamFlag);
        sb.append(", streamPriority=").append(streamPriority);
        sb.append(", URLLength=").append(URLLength);
        sb.append(", URLString='").append(URLString).append('\'');
        sb.append(", remoteODFlag=").append(remoteODFlag);
        sb.append(", dependsOnEsId=").append(dependsOnEsId);
        sb.append(", oCREsId=").append(oCREsId);
        sb.append(", decoderConfigDescriptor=").append(decoderConfigDescriptor);
        sb.append(", slConfigDescriptor=").append(slConfigDescriptor);
        sb.append('}');
        return sb.toString();
    }


}
