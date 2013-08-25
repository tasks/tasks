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

public class CharCache {
    private char[] cache;
    private int pos;

    public CharCache(int capacity) {
        cache = new char[capacity];
    }

    public void append(String str) {
        char[] chars = str.toCharArray();
        int available = cache.length - pos;
        int toWrite = chars.length < available ? chars.length : available;
        System.arraycopy(chars, 0, cache, pos, toWrite);
        pos += toWrite;
    }

    public String toString() {
        return new String(cache, 0, pos);
    }

    public void clear() {
        pos = 0;
    }

    public void append(char c) {
        if (pos < cache.length - 1) {
            cache[pos] = c;
            pos++;
        }
    }

    public int length() {
        return pos;
    }
}
