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
package com.googlecode.mp4parser.h264.model;

/**
 * Chroma format enum
 *
 * @author Stanislav Vitvitskiy
 */
public class ChromaFormat {
    public static ChromaFormat MONOCHROME = new ChromaFormat(0, 0, 0);
    public static ChromaFormat YUV_420 = new ChromaFormat(1, 2, 2);
    public static ChromaFormat YUV_422 = new ChromaFormat(2, 2, 1);
    public static ChromaFormat YUV_444 = new ChromaFormat(3, 1, 1);

    private int id;
    private int subWidth;
    private int subHeight;

    public ChromaFormat(int id, int subWidth, int subHeight) {
        this.id = id;
        this.subWidth = subWidth;
        this.subHeight = subHeight;
    }

    public static ChromaFormat fromId(int id) {
        if (id == MONOCHROME.id) {
            return MONOCHROME;
        } else if (id == YUV_420.id) {
            return YUV_420;
        } else if (id == YUV_422.id) {
            return YUV_422;
        } else if (id == YUV_444.id) {
            return YUV_444;
        }
        return null;
    }

    public int getId() {
        return id;
    }

    public int getSubWidth() {
        return subWidth;
    }

    public int getSubHeight() {
        return subHeight;
    }

    @Override
    public String toString() {
        return "ChromaFormat{" + "\n" +
                "id=" + id + ",\n" +
                " subWidth=" + subWidth + ",\n" +
                " subHeight=" + subHeight +
                '}';
    }
}
