/*
Copyright (c) 2011 Stanislav Vitvitskiy

Permission is hereby granted, free of charge, to any person obtaining a copy of this
software and associated documentation files (the "Software"), to deal in the Software
without restriction, including without limitation the rights to use, copy, modify,
merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.googlecode.mp4parser.h264.read;


import com.googlecode.mp4parser.h264.BTree;

import java.io.IOException;
import java.io.InputStream;

import static com.googlecode.mp4parser.h264.Debug.println;


public class CAVLCReader extends BitstreamReader {

    public CAVLCReader(InputStream is) throws IOException {
        super(is);
    }

    public long readNBit(int n, String message) throws IOException {
        long val = readNBit(n);

        trace(message, String.valueOf(val));

        return val;
    }

    /**
     * Read unsigned exp-golomb code
     *
     * @return
     * @throws java.io.IOException
     * @throws java.io.IOException
     */
    private int readUE() throws IOException {
        int cnt = 0;
        while (read1Bit() == 0)
            cnt++;

        int res = 0;
        if (cnt > 0) {
            long val = readNBit(cnt);

            res = (int) ((1 << cnt) - 1 + val);
        }

        return res;
    }

    /*
      * (non-Javadoc)
      *
      * @see
      * ua.org.jplayer.javcodec.h264.H264BitInputStream#readUE(java.lang.String)
      */
    public int readUE(String message) throws IOException {
        int res = readUE();

        trace(message, String.valueOf(res));

        return res;
    }

    public int readSE(String message) throws IOException {
        int val = readUE();

        int sign = ((val & 0x1) << 1) - 1;
        val = ((val >> 1) + (val & 0x1)) * sign;

        trace(message, String.valueOf(val));

        return val;
    }

    public boolean readBool(String message) throws IOException {

        boolean res = read1Bit() == 0 ? false : true;

        trace(message, res ? "1" : "0");

        return res;
    }

    public int readU(int i, String string) throws IOException {
        return (int) readNBit(i, string);
    }

    public byte[] read(int payloadSize) throws IOException {
        byte[] result = new byte[payloadSize];
        for (int i = 0; i < payloadSize; i++) {
            result[i] = (byte) readByte();
        }
        return result;
    }

    public boolean readAE() {
        // TODO: do it!!
        throw new UnsupportedOperationException("Stan");
    }

    public int readTE(int max) throws IOException {
        if (max > 1)
            return readUE();
        return ~read1Bit() & 0x1;
    }

    public int readAEI() {
        // TODO: do it!!
        throw new UnsupportedOperationException("Stan");
    }

    public int readME(String string) throws IOException {
        return readUE(string);
    }

    public Object readCE(BTree bt, String message) throws IOException {
        while (true) {
            int bit = read1Bit();
            bt = bt.down(bit);
            if (bt == null) {
                throw new RuntimeException("Illegal code");
            }
            Object i = bt.getValue();
            if (i != null) {
                trace(message, i.toString());
                return i;
            }
        }
    }

    public int readZeroBitCount(String message) throws IOException {
        int count = 0;
        while (read1Bit() == 0)
            count++;

        trace(message, String.valueOf(count));

        return count;
    }

    public void readTrailingBits() throws IOException {
        read1Bit();
        readRemainingByte();
    }

    private void trace(String message, String val) {
        StringBuilder traceBuilder = new StringBuilder();
        int spaces;
        String pos = String.valueOf(bitsRead - debugBits.length());
        spaces = 8 - pos.length();

        traceBuilder.append("@" + pos);

        for (int i = 0; i < spaces; i++)
            traceBuilder.append(' ');

        traceBuilder.append(message);
        spaces = 100 - traceBuilder.length() - debugBits.length();
        for (int i = 0; i < spaces; i++)
            traceBuilder.append(' ');
        traceBuilder.append(debugBits);
        traceBuilder.append(" (" + val + ")");
        debugBits.clear();

        println(traceBuilder.toString());
    }
}