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

package com.coremedia.iso.boxes.sampleentry;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.Box;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Entry type for timed text samples defined in the timed text specification (ISO/IEC 14496-17).
 */
public class TextSampleEntry extends SampleEntry {

    public static final String TYPE1 = "tx3g";

    public static final String TYPE_ENCRYPTED = "enct";

/*  class TextSampleEntry() extends SampleEntry ('tx3g') {
    unsigned int(32)  displayFlags;
    signed int(8)     horizontal-justification;
    signed int(8)     vertical-justification;
    unsigned int(8)   background-color-rgba[4];
    BoxRecord         default-text-box;
    StyleRecord       default-style;
    FontTableBox      font-table;
  }
  */

    private long displayFlags; // 32 bits
    private int horizontalJustification; // 8 bit
    private int verticalJustification;  // 8 bit
    private int[] backgroundColorRgba = new int[4]; // 4 bytes
    private BoxRecord boxRecord = new BoxRecord();
    private StyleRecord styleRecord = new StyleRecord();

    public TextSampleEntry(String type) {
        super(type);
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        _parseReservedAndDataReferenceIndex(content);
        displayFlags = IsoTypeReader.readUInt32(content);
        horizontalJustification = IsoTypeReader.readUInt8(content);
        verticalJustification = IsoTypeReader.readUInt8(content);
        backgroundColorRgba = new int[4];
        backgroundColorRgba[0] = IsoTypeReader.readUInt8(content);
        backgroundColorRgba[1] = IsoTypeReader.readUInt8(content);
        backgroundColorRgba[2] = IsoTypeReader.readUInt8(content);
        backgroundColorRgba[3] = IsoTypeReader.readUInt8(content);
        boxRecord = new BoxRecord();
        boxRecord.parse(content);

        styleRecord = new StyleRecord();
        styleRecord.parse(content);
        _parseChildBoxes(content);
    }


    protected long getContentSize() {
        long contentSize = 18;
        contentSize += boxRecord.getSize();
        contentSize += styleRecord.getSize();
        for (Box boxe : boxes) {
            contentSize += boxe.getSize();
        }
        return contentSize;
    }

    public String toString() {
        return "TextSampleEntry";
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        _writeReservedAndDataReferenceIndex(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, displayFlags);
        IsoTypeWriter.writeUInt8(byteBuffer, horizontalJustification);
        IsoTypeWriter.writeUInt8(byteBuffer, verticalJustification);
        IsoTypeWriter.writeUInt8(byteBuffer, backgroundColorRgba[0]);
        IsoTypeWriter.writeUInt8(byteBuffer, backgroundColorRgba[1]);
        IsoTypeWriter.writeUInt8(byteBuffer, backgroundColorRgba[2]);
        IsoTypeWriter.writeUInt8(byteBuffer, backgroundColorRgba[3]);
        boxRecord.getContent(byteBuffer);
        styleRecord.getContent(byteBuffer);

        _writeChildBoxes(byteBuffer);
    }

    public BoxRecord getBoxRecord() {
        return boxRecord;
    }

    public void setBoxRecord(BoxRecord boxRecord) {
        this.boxRecord = boxRecord;
    }

    public StyleRecord getStyleRecord() {
        return styleRecord;
    }

    public void setStyleRecord(StyleRecord styleRecord) {
        this.styleRecord = styleRecord;
    }

    public boolean isScrollIn() {
        return (displayFlags & 0x00000020) == 0x00000020;
    }

    public void setScrollIn(boolean scrollIn) {
        if (scrollIn) {
            displayFlags |= 0x00000020;
        } else {
            displayFlags &= ~0x00000020;
        }
    }

    public boolean isScrollOut() {
        return (displayFlags & 0x00000040) == 0x00000040;
    }

    public void setScrollOut(boolean scrollOutIn) {
        if (scrollOutIn) {
            displayFlags |= 0x00000040;
        } else {
            displayFlags &= ~0x00000040;
        }
    }

    public boolean isScrollDirection() {
        return (displayFlags & 0x00000180) == 0x00000180;
    }

    public void setScrollDirection(boolean scrollOutIn) {
        if (scrollOutIn) {
            displayFlags |= 0x00000180;
        } else {
            displayFlags &= ~0x00000180;
        }
    }

    public boolean isContinuousKaraoke() {
        return (displayFlags & 0x00000800) == 0x00000800;
    }

    public void setContinuousKaraoke(boolean continuousKaraoke) {
        if (continuousKaraoke) {
            displayFlags |= 0x00000800;
        } else {
            displayFlags &= ~0x00000800;
        }
    }

    public boolean isWriteTextVertically() {
        return (displayFlags & 0x00020000) == 0x00020000;
    }

    public void setWriteTextVertically(boolean writeTextVertically) {
        if (writeTextVertically) {
            displayFlags |= 0x00020000;
        } else {
            displayFlags &= ~0x00020000;
        }
    }


    public boolean isFillTextRegion() {
        return (displayFlags & 0x00040000) == 0x00040000;
    }

    public void setFillTextRegion(boolean fillTextRegion) {
        if (fillTextRegion) {
            displayFlags |= 0x00040000;
        } else {
            displayFlags &= ~0x00040000;
        }
    }


    public int getHorizontalJustification() {
        return horizontalJustification;
    }

    public void setHorizontalJustification(int horizontalJustification) {
        this.horizontalJustification = horizontalJustification;
    }

    public int getVerticalJustification() {
        return verticalJustification;
    }

    public void setVerticalJustification(int verticalJustification) {
        this.verticalJustification = verticalJustification;
    }

    public int[] getBackgroundColorRgba() {
        return backgroundColorRgba;
    }

    public void setBackgroundColorRgba(int[] backgroundColorRgba) {
        this.backgroundColorRgba = backgroundColorRgba;
    }

    public static class BoxRecord {
        int top;
        int left;
        int bottom;
        int right;

        public void parse(ByteBuffer in) {
            top = IsoTypeReader.readUInt16(in);
            left = IsoTypeReader.readUInt16(in);
            bottom = IsoTypeReader.readUInt16(in);
            right = IsoTypeReader.readUInt16(in);
        }

        public void getContent(ByteBuffer bb)  {
            IsoTypeWriter.writeUInt16(bb, top);
            IsoTypeWriter.writeUInt16(bb, left);
            IsoTypeWriter.writeUInt16(bb, bottom);
            IsoTypeWriter.writeUInt16(bb, right);
        }

        public int getSize() {
            return 8;
        }
    }

    /*
    class FontRecord {
	unsigned int(16) 	font-ID;
	unsigned int(8)	font-name-length;
	unsigned int(8)	font[font-name-length];
}
     */


    /*
   aligned(8) class StyleRecord {
   unsigned int(16) 	startChar;
   unsigned int(16)	endChar;
   unsigned int(16)	font-ID;
   unsigned int(8)	face-style-flags;
   unsigned int(8)	font-size;
   unsigned int(8)	text-color-rgba[4];
}
    */
    public static class StyleRecord {
        int startChar;
        int endChar;
        int fontId;
        int faceStyleFlags;
        int fontSize;
        int[] textColor = new int[]{0xff, 0xff, 0xff, 0xff};

        public void parse(ByteBuffer in) {
            startChar = IsoTypeReader.readUInt16(in);
            endChar = IsoTypeReader.readUInt16(in);
            fontId = IsoTypeReader.readUInt16(in);
            faceStyleFlags = IsoTypeReader.readUInt8(in);
            fontSize = IsoTypeReader.readUInt8(in);
            textColor = new int[4];
            textColor[0] = IsoTypeReader.readUInt8(in);
            textColor[1] = IsoTypeReader.readUInt8(in);
            textColor[2] = IsoTypeReader.readUInt8(in);
            textColor[3] = IsoTypeReader.readUInt8(in);
        }


        public void getContent(ByteBuffer bb) {
            IsoTypeWriter.writeUInt16(bb, startChar);
            IsoTypeWriter.writeUInt16(bb, endChar);
            IsoTypeWriter.writeUInt16(bb, fontId);
            IsoTypeWriter.writeUInt8(bb, faceStyleFlags);
            IsoTypeWriter.writeUInt8(bb, fontSize);
            IsoTypeWriter.writeUInt8(bb, textColor[0]);
            IsoTypeWriter.writeUInt8(bb, textColor[1]);
            IsoTypeWriter.writeUInt8(bb, textColor[2]);
            IsoTypeWriter.writeUInt8(bb, textColor[3]);
        }

        public int getSize() {
            return 12;
        }
    }


}
