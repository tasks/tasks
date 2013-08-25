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

/**
 * class SLConfigDescriptor extends BaseDescriptor : bit(8) tag=SLConfigDescrTag {
 * bit(8) predefined;
 * if (predefined==0) {
 * bit(1) useAccessUnitStartFlag;
 * bit(1) useAccessUnitEndFlag;
 * bit(1) useRandomAccessPointFlag;
 * bit(1) hasRandomAccessUnitsOnlyFlag;
 * bit(1) usePaddingFlag;
 * bit(1) useTimeStampsFlag;
 * bit(1) useIdleFlag;
 * bit(1) durationFlag;
 * bit(32) timeStampResolution;
 * bit(32) OCRResolution;
 * bit(8) timeStampLength; // must be ≤ 64
 * bit(8) OCRLength; // must be ≤ 64
 * bit(8) AU_Length; // must be ≤ 32
 * bit(8) instantBitrateLength;
 * bit(4) degradationPriorityLength;
 * bit(5) AU_seqNumLength; // must be ≤ 16
 * bit(5) packetSeqNumLength; // must be ≤ 16
 * bit(2) reserved=0b11;
 * }
 * if (durationFlag) {
 * bit(32) timeScale;
 * bit(16) accessUnitDuration;
 * bit(16) compositionUnitDuration;
 * }
 * if (!useTimeStampsFlag) {
 * bit(timeStampLength) startDecodingTimeStamp;
 * bit(timeStampLength) startCompositionTimeStamp;
 * }
 * }
 */
@Descriptor(tags = {0x06})
public class SLConfigDescriptor extends BaseDescriptor {
    int predefined;

    public int getPredefined() {
        return predefined;
    }

    public void setPredefined(int predefined) {
        this.predefined = predefined;
    }

    @Override
    public void parseDetail(ByteBuffer bb) throws IOException {
        predefined =  IsoTypeReader.readUInt8(bb);
    }

    public int serializedSize() {
        return 3;
    }

    public ByteBuffer serialize() {
        ByteBuffer out = ByteBuffer.allocate(3);
        IsoTypeWriter.writeUInt8(out, 6);
        IsoTypeWriter.writeUInt8(out, 1);
        IsoTypeWriter.writeUInt8(out, predefined);
        return out;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SLConfigDescriptor");
        sb.append("{predefined=").append(predefined);
        sb.append('}');
        return sb.toString();
    }
}
