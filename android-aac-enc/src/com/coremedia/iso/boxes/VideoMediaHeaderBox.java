/*  
 * Copyright 2008 CoreMedia AG, Hamburg
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

package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;

import java.nio.ByteBuffer;

/**
 * The video media header contains general presentation information, independent of the coding, for video
 * media. Note that the flags field has the value 1.
 */
public class VideoMediaHeaderBox extends AbstractMediaHeaderBox {
    private int graphicsmode = 0;
    private int[] opcolor = new int[]{0, 0, 0};
    public static final String TYPE = "vmhd";

    public VideoMediaHeaderBox() {
        super(TYPE);
        setFlags(1); // 1 is default.
    }

    public int getGraphicsmode() {
        return graphicsmode;
    }

    public int[] getOpcolor() {
        return opcolor;
    }

    protected long getContentSize() {
        return 12;
    }


    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        graphicsmode = IsoTypeReader.readUInt16(content);
        opcolor = new int[3];
        for (int i = 0; i < 3; i++) {
            opcolor[i] = IsoTypeReader.readUInt16(content);
        }
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt16(byteBuffer, graphicsmode);
        for (int anOpcolor : opcolor) {
            IsoTypeWriter.writeUInt16(byteBuffer, anOpcolor);
        }
    }

    public String toString() {
        return "VideoMediaHeaderBox[graphicsmode=" + getGraphicsmode() + ";opcolor0=" + getOpcolor()[0] + ";opcolor1=" + getOpcolor()[1] + ";opcolor2=" + getOpcolor()[2] + "]";
    }

    public void setOpcolor(int[] opcolor) {
        this.opcolor = opcolor;
    }

    public void setGraphicsmode(int graphicsmode) {
        this.graphicsmode = graphicsmode;
    }
}
