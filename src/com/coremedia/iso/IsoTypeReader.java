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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public final class IsoTypeReader {


    public static long readUInt32BE(ByteBuffer bb) {
        long ch1 = readUInt8(bb);
        long ch2 = readUInt8(bb);
        long ch3 = readUInt8(bb);
        long ch4 = readUInt8(bb);
        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));

    }


    public static long readUInt32(ByteBuffer bb) {
        long ch1 = readUInt8(bb);
        long ch2 = readUInt8(bb);
        long ch3 = readUInt8(bb);
        long ch4 = readUInt8(bb);
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));

    }

    public static int readUInt24(ByteBuffer bb) {
        int result = 0;
        result += readUInt16(bb) << 8;
        result += byte2int(bb.get());
        return result;
    }


    public static int readUInt16(ByteBuffer bb) {
        int result = 0;
        result += byte2int(bb.get()) << 8;
        result += byte2int(bb.get());
        return result;
    }

    public static int readUInt16BE(ByteBuffer bb) {
        int result = 0;
        result += byte2int(bb.get());
        result += byte2int(bb.get()) << 8;
        return result;
    }

    public static int readUInt8(ByteBuffer bb) {
        return byte2int(bb.get());
    }

    public static int byte2int(byte b) {
        return b < 0 ? b + 256 : b;
    }


    /**
     * Reads a zero terminated UTF-8 string.
     *
     * @param byteBuffer the data source
     * @return the string readByte
     * @throws Error in case of an error in the underlying stream
     */
    public static String readString(ByteBuffer byteBuffer) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read;
        while ((read = byteBuffer.get()) != 0) {
            out.write(read);
        }
        return Utf8.convert(out.toByteArray());
    }

    public static String readString(ByteBuffer byteBuffer, int length) {
        byte[] buffer = new byte[length];
        byteBuffer.get(buffer);
        return Utf8.convert(buffer);

    }

    public static long readUInt64(ByteBuffer byteBuffer) {
        long result = 0;
        // thanks to Erik Nicolas for finding a bug! Cast to long is definitivly needed
        result += readUInt32(byteBuffer) << 32;
        if (result < 0) {
            throw new RuntimeException("I don't know how to deal with UInt64! long is not sufficient and I don't want to use BigInt");
        }
        result += readUInt32(byteBuffer);

        return result;
    }

    public static double readFixedPoint1616(ByteBuffer bb) {
        byte[] bytes = new byte[4];
        bb.get(bytes);

        int result = 0;
        result |= ((bytes[0] << 24) & 0xFF000000);
        result |= ((bytes[1] << 16) & 0xFF0000);
        result |= ((bytes[2] << 8) & 0xFF00);
        result |= ((bytes[3]) & 0xFF);
        return ((double) result) / 65536;

    }

    public static float readFixedPoint88(ByteBuffer bb) {
        byte[] bytes = new byte[2];
        bb.get(bytes);
        short result = 0;
        result |= ((bytes[0] << 8) & 0xFF00);
        result |= ((bytes[1]) & 0xFF);
        return ((float) result) / 256;
    }

    public static String readIso639(ByteBuffer bb) {
        int bits = readUInt16(bb);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            int c = (bits >> (2 - i) * 5) & 0x1f;
            result.append((char) (c + 0x60));
        }
        return result.toString();
    }

    public static String read4cc(ByteBuffer bb) {
        byte[] b = new byte[4];
        bb.get(b);
        return IsoFile.bytesToFourCC(b);
    }

}
