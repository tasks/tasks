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
import com.coremedia.iso.Utf8;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;

import java.nio.ByteBuffer;

/**
 * Contains information common to all visual tracks.
 * <code>
 * <pre>
 * class VisualSampleEntry(codingname) extends SampleEntry (codingname){
 * unsigned int(16) pre_defined = 0;
 * const unsigned int(16) reserved = 0;
 * unsigned int(32)[3] pre_defined = 0;
 * unsigned int(16) width;
 * unsigned int(16) height;
 * template unsigned int(32) horizresolution = 0x00480000; // 72 dpi
 * template unsigned int(32) vertresolution = 0x00480000; // 72 dpi
 * const unsigned int(32) reserved = 0;
 * template unsigned int(16) frame_count = 1;
 * string[32] compressorname;
 * template unsigned int(16) depth = 0x0018;
 * int(16) pre_defined = -1;
 * }<br>
 * </pre>
 * </code>
 * <p/>
 * Format-specific informationis appened as boxes after the data described in ISO/IEC 14496-12 chapter 8.16.2.
 */
public class VisualSampleEntry extends SampleEntry implements ContainerBox {
    public static final String TYPE1 = "mp4v";
    public static final String TYPE2 = "s263";
    public static final String TYPE3 = "avc1";


    /**
     * Identifier for an encrypted video track.
     *
     * @see com.coremedia.iso.boxes.ProtectionSchemeInformationBox
     */
    public static final String TYPE_ENCRYPTED = "encv";


    private int width;
    private int height;
    private double horizresolution;
    private double vertresolution;
    private int frameCount;
    private String compressorname;
    private int depth;

    private long[] predefined = new long[3];

    public VisualSampleEntry(String type) {
        super(type);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double getHorizresolution() {
        return horizresolution;
    }

    public double getVertresolution() {
        return vertresolution;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public String getCompressorname() {
        return compressorname;
    }

    public int getDepth() {
        return depth;
    }

    public void setCompressorname(String compressorname) {
        this.compressorname = compressorname;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setHorizresolution(double horizresolution) {
        this.horizresolution = horizresolution;
    }

    public void setVertresolution(double vertresolution) {
        this.vertresolution = vertresolution;
    }

    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        _parseReservedAndDataReferenceIndex(content);
        long tmp = IsoTypeReader.readUInt16(content);
        assert 0 == tmp : "reserved byte not 0";
        tmp = IsoTypeReader.readUInt16(content);
        assert 0 == tmp : "reserved byte not 0";
        predefined[0] = IsoTypeReader.readUInt32(content);     // should be zero
        predefined[1] = IsoTypeReader.readUInt32(content);     // should be zero
        predefined[2] = IsoTypeReader.readUInt32(content);     // should be zero
        width = IsoTypeReader.readUInt16(content);
        height = IsoTypeReader.readUInt16(content);
        horizresolution = IsoTypeReader.readFixedPoint1616(content);
        vertresolution = IsoTypeReader.readFixedPoint1616(content);
        tmp = IsoTypeReader.readUInt32(content);
        assert 0 == tmp : "reserved byte not 0";
        frameCount = IsoTypeReader.readUInt16(content);
        int compressornameDisplayAbleData = IsoTypeReader.readUInt8(content);
        if (compressornameDisplayAbleData > 31) {
            System.out.println("invalid compressor name displayable data: " + compressornameDisplayAbleData);
            compressornameDisplayAbleData = 31;
        }
        byte[] bytes = new byte[compressornameDisplayAbleData];
        content.get(bytes);
        compressorname = Utf8.convert(bytes);
        if (compressornameDisplayAbleData < 31) {
            byte[] zeros = new byte[31 - compressornameDisplayAbleData];
            content.get(zeros);
            //assert Arrays.equals(zeros, new byte[zeros.length]) : "The compressor name length was not filled up with zeros";
        }
        depth = IsoTypeReader.readUInt16(content);
        tmp = IsoTypeReader.readUInt16(content);
        assert 0xFFFF == tmp;

        _parseChildBoxes(content);

    }


    protected long getContentSize() {
        long contentSize = 78;
        for (Box boxe : boxes) {
            contentSize += boxe.getSize();
        }
        return contentSize;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        _writeReservedAndDataReferenceIndex(byteBuffer);
        IsoTypeWriter.writeUInt16(byteBuffer, 0);
        IsoTypeWriter.writeUInt16(byteBuffer, 0);
        IsoTypeWriter.writeUInt32(byteBuffer, predefined[0]);
        IsoTypeWriter.writeUInt32(byteBuffer, predefined[1]);
        IsoTypeWriter.writeUInt32(byteBuffer, predefined[2]);

        IsoTypeWriter.writeUInt16(byteBuffer, getWidth());
        IsoTypeWriter.writeUInt16(byteBuffer, getHeight());

        IsoTypeWriter.writeFixedPont1616(byteBuffer, getHorizresolution());
        IsoTypeWriter.writeFixedPont1616(byteBuffer, getVertresolution());


        IsoTypeWriter.writeUInt32(byteBuffer, 0);
        IsoTypeWriter.writeUInt16(byteBuffer, getFrameCount());
        IsoTypeWriter.writeUInt8(byteBuffer, Utf8.utf8StringLengthInBytes(getCompressorname()));
        byteBuffer.put(Utf8.convert(getCompressorname()));
        int a = Utf8.utf8StringLengthInBytes(getCompressorname());
        while (a < 31) {
            a++;
            byteBuffer.put((byte) 0);
        }
        IsoTypeWriter.writeUInt16(byteBuffer, getDepth());
        IsoTypeWriter.writeUInt16(byteBuffer, 0xFFFF);

        _writeChildBoxes(byteBuffer);

    }

}
