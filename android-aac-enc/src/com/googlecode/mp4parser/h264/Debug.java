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
package com.googlecode.mp4parser.h264;

import java.nio.ShortBuffer;

public class Debug {
    public final static void print8x8(int[] output) {
        int i = 0;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                System.out.printf("%3d, ", output[i]);
                i++;
            }
            System.out.println();
        }
    }

    public final static void print8x8(short[] output) {
        int i = 0;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                System.out.printf("%3d, ", output[i]);
                i++;
            }
            System.out.println();
        }
    }

    public final static void print8x8(ShortBuffer output) {
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                System.out.printf("%3d, ", output.get());
            }
            System.out.println();
        }
    }

    public static void print(short[] table) {
        int i = 0;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                System.out.printf("%3d, ", table[i]);
                i++;
            }
            System.out.println();
        }
    }

    public static void trace(String format, Object... args) {
        // System.out.printf("> " + format + "\n", args);
    }

    public final static boolean debug = false;

    public static void print(int i) {
        if (debug)
            System.out.print(i);
    }

    public static void print(String string) {
        if (debug)
            System.out.print(string);
    }

    public static void println(String string) {
        if (debug)
            System.out.println(string);
    }
}
