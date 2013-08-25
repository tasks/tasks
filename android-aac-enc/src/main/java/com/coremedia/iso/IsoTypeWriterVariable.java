/*
 * Copyright 2012 Sebastian Annies, Hamburg
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
package com.coremedia.iso;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class IsoTypeWriterVariable {

    public static void write(long v, ByteBuffer bb, int bytes) {
        switch (bytes) {
            case 1:
                IsoTypeWriter.writeUInt8(bb, (int) (v & 0xff));
                break;
            case 2:
                IsoTypeWriter.writeUInt16(bb, (int) (v & 0xffff));
                break;
            case 3:
                IsoTypeWriter.writeUInt24(bb, (int) (v & 0xffffff));
                break;
            case 4:
                IsoTypeWriter.writeUInt32(bb, v);
                break;
            case 8:
                IsoTypeWriter.writeUInt64(bb, v);
                break;
            default:
                throw new RuntimeException("I don't know how to read " + bytes + " bytes");
        }

    }
}
