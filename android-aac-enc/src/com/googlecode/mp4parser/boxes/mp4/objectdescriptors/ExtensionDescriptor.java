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

import com.coremedia.iso.Hex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * abstract class ExtensionDescriptor extends BaseDescriptor
 * : bit(8) tag = ExtensionProfileLevelDescrTag, ExtDescrTagStartRange ..
 * ExtDescrTagEndRange {
 * // empty. To be filled by classes extending this class.
 * }
 */
@Descriptor(tags = {0x13, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253})
public class ExtensionDescriptor extends BaseDescriptor {
    private static Logger log = Logger.getLogger(ExtensionDescriptor.class.getName());

    byte[] bytes;


    //todo: add this better to the tags list?
    //14496-1:2010 p.20:
    //0x6A-0xBF Reserved for ISO use
    //0xC0-0xFE User private
    //
    //ExtDescrTagStartRange = 0x6A
    //ExtDescrTagEndRange = 0xFE
    static int[] allTags() {
        int[] ints = new int[0xFE - 0x6A];

        for (int i = 0x6A; i < 0xFE; i++) {
            final int pos = i - 0x6A;
            log.finest("pos:" + pos);
            ints[pos] = i;
        }
        return ints;
    }

    @Override
    public void parseDetail(ByteBuffer bb) throws IOException {
        if (getSize() > 0) {
            bytes = new byte[sizeOfInstance];
            bb.get(bytes);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ExtensionDescriptor");
        sb.append("{bytes=").append(bytes == null ? "null" : Hex.encodeHex(bytes));
        sb.append('}');
        return sb.toString();
    }
}
