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

package com.coremedia.iso.boxes.h264;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractBox;
import com.googlecode.mp4parser.h264.model.PictureParameterSet;
import com.googlecode.mp4parser.h264.model.SeqParameterSet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Defined in ISO/IEC 14496-15:2004.
 */
public final class AvcConfigurationBox extends AbstractBox {
    public static final String TYPE = "avcC";

    private int configurationVersion;
    private int avcProfileIndicaation;
    private int profileCompatibility;
    private int avcLevelIndication;
    private int lengthSizeMinusOne;
    List<byte[]> sequenceParameterSets = new ArrayList<byte[]>();
    List<byte[]> pictureParameterSets = new ArrayList<byte[]>();
    
    boolean hasExts = true;
    private int chromaFormat = 1;
    private int bitDepthLumaMinus8  = 0;
    private int bitDepthChromaMinus8 = 0;
    List<byte[]> sequenceParameterSetExts = new ArrayList<byte[]>();

    public AvcConfigurationBox() {
        super(TYPE);
    }

    public int getConfigurationVersion() {
        return configurationVersion;
    }

    public int getAvcProfileIndicaation() {
        return avcProfileIndicaation;
    }

    public int getProfileCompatibility() {
        return profileCompatibility;
    }

    public int getAvcLevelIndication() {
        return avcLevelIndication;
    }

    public int getLengthSizeMinusOne() {
        return lengthSizeMinusOne;
    }

    public List<byte[]> getSequenceParameterSets() {
        return Collections.unmodifiableList(sequenceParameterSets);
    }

    public List<byte[]> getPictureParameterSets() {
        return Collections.unmodifiableList(pictureParameterSets);
    }

    public void setConfigurationVersion(int configurationVersion) {
        this.configurationVersion = configurationVersion;
    }

    public void setAvcProfileIndicaation(int avcProfileIndicaation) {
        this.avcProfileIndicaation = avcProfileIndicaation;
    }

    public void setProfileCompatibility(int profileCompatibility) {
        this.profileCompatibility = profileCompatibility;
    }

    public void setAvcLevelIndication(int avcLevelIndication) {
        this.avcLevelIndication = avcLevelIndication;
    }

    public void setLengthSizeMinusOne(int lengthSizeMinusOne) {
        this.lengthSizeMinusOne = lengthSizeMinusOne;
    }

    public void setSequenceParameterSets(List<byte[]> sequenceParameterSets) {
        this.sequenceParameterSets = sequenceParameterSets;
    }

    public void setPictureParameterSets(List<byte[]> pictureParameterSets) {
        this.pictureParameterSets = pictureParameterSets;
    }

    public int getChromaFormat() {
        return chromaFormat;
    }

    public void setChromaFormat(int chromaFormat) {
        this.chromaFormat = chromaFormat;
    }

    public int getBitDepthLumaMinus8() {
        return bitDepthLumaMinus8;
    }

    public void setBitDepthLumaMinus8(int bitDepthLumaMinus8) {
        this.bitDepthLumaMinus8 = bitDepthLumaMinus8;
    }

    public int getBitDepthChromaMinus8() {
        return bitDepthChromaMinus8;
    }

    public void setBitDepthChromaMinus8(int bitDepthChromaMinus8) {
        this.bitDepthChromaMinus8 = bitDepthChromaMinus8;
    }

    public List<byte[]> getSequenceParameterSetExts() {
        return sequenceParameterSetExts;
    }

    public void setSequenceParameterSetExts(List<byte[]> sequenceParameterSetExts) {
        this.sequenceParameterSetExts = sequenceParameterSetExts;
    }

    public boolean hasExts() {
        return hasExts;
    }

    public void setHasExts(boolean hasExts) {
        this.hasExts = hasExts;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        configurationVersion = IsoTypeReader.readUInt8(content);
        avcProfileIndicaation = IsoTypeReader.readUInt8(content);
        profileCompatibility = IsoTypeReader.readUInt8(content);
        avcLevelIndication = IsoTypeReader.readUInt8(content);
        int temp = IsoTypeReader.readUInt8(content);
        lengthSizeMinusOne = temp & 3;
        long numberOfSeuqenceParameterSets = IsoTypeReader.readUInt8(content) & 31;
        for (int i = 0; i < numberOfSeuqenceParameterSets; i++) {
            int sequenceParameterSetLength = IsoTypeReader.readUInt16(content);

            byte[] sequenceParameterSetNALUnit = new byte[sequenceParameterSetLength];
            content.get(sequenceParameterSetNALUnit);
            sequenceParameterSets.add(sequenceParameterSetNALUnit);
        }
        long numberOfPictureParameterSets = IsoTypeReader.readUInt8(content);
        for (int i = 0; i < numberOfPictureParameterSets; i++) {
            int pictureParameterSetLength = IsoTypeReader.readUInt16(content);
            byte[] pictureParameterSetNALUnit = new byte[pictureParameterSetLength];
            content.get(pictureParameterSetNALUnit);
            pictureParameterSets.add(pictureParameterSetNALUnit);
        }
        if (content.remaining() < 4) {
            hasExts = false;
        }
        if (hasExts && (avcProfileIndicaation == 100 || avcProfileIndicaation == 110 || avcProfileIndicaation == 122 || avcProfileIndicaation == 144)) {
            chromaFormat = IsoTypeReader.readUInt8(content) & 3;
            bitDepthLumaMinus8 = IsoTypeReader.readUInt8(content) & 7;
            bitDepthChromaMinus8 = IsoTypeReader.readUInt8(content) & 7;
            long numOfSequenceParameterSetExt = IsoTypeReader.readUInt8(content);
            for (int i = 0; i < numOfSequenceParameterSetExt; i++) {
                int sequenceParameterSetExtLength = IsoTypeReader.readUInt16(content);
                byte[] sequenceParameterSetExtNALUnit = new byte[sequenceParameterSetExtLength];
                content.get(sequenceParameterSetExtNALUnit);
                sequenceParameterSetExts.add(sequenceParameterSetExtNALUnit);
            }
        } else {
            chromaFormat = -1;
            bitDepthLumaMinus8 = -1;
            bitDepthChromaMinus8 = -1;
        }

    }


    public long getContentSize() {
        long size = 5;
        size += 1; // sequenceParamsetLength
        for (byte[] sequenceParameterSetNALUnit : sequenceParameterSets) {
            size += 2; //lengthSizeMinusOne field
            size += sequenceParameterSetNALUnit.length;
        }
        size += 1; // pictureParamsetLength
        for (byte[] pictureParameterSetNALUnit : pictureParameterSets) {
            size += 2; //lengthSizeMinusOne field
            size += pictureParameterSetNALUnit.length;
        }
        if (hasExts && (avcProfileIndicaation == 100 || avcProfileIndicaation == 110 || avcProfileIndicaation == 122 || avcProfileIndicaation == 144)) {
            size += 4;
            for (byte[] sequenceParameterSetExtNALUnit : sequenceParameterSetExts) {
                size += 2;
                size += sequenceParameterSetExtNALUnit.length;
            }
        }

        return size;
    }


    @Override
    public void getContent(ByteBuffer byteBuffer) {
        IsoTypeWriter.writeUInt8(byteBuffer, configurationVersion);
        IsoTypeWriter.writeUInt8(byteBuffer, avcProfileIndicaation);
        IsoTypeWriter.writeUInt8(byteBuffer, profileCompatibility);
        IsoTypeWriter.writeUInt8(byteBuffer, avcLevelIndication);
        IsoTypeWriter.writeUInt8(byteBuffer, lengthSizeMinusOne | (63 << 2));
        IsoTypeWriter.writeUInt8(byteBuffer, (pictureParameterSets.size() & 31) | (7 << 5));
        for (byte[] sequenceParameterSetNALUnit : sequenceParameterSets) {
            IsoTypeWriter.writeUInt16(byteBuffer, sequenceParameterSetNALUnit.length);
            byteBuffer.put(sequenceParameterSetNALUnit);
        }
        IsoTypeWriter.writeUInt8(byteBuffer, pictureParameterSets.size());
        for (byte[] pictureParameterSetNALUnit : pictureParameterSets) {
            IsoTypeWriter.writeUInt16(byteBuffer, pictureParameterSetNALUnit.length);
            byteBuffer.put(pictureParameterSetNALUnit);
        }
        if (hasExts && (avcProfileIndicaation == 100 || avcProfileIndicaation == 110 || avcProfileIndicaation == 122 || avcProfileIndicaation == 144)) {
            IsoTypeWriter.writeUInt8(byteBuffer, chromaFormat | (63 << 2));
            IsoTypeWriter.writeUInt8(byteBuffer, bitDepthLumaMinus8 | (31 << 3));
            IsoTypeWriter.writeUInt8(byteBuffer, bitDepthChromaMinus8 | (31 << 3));
            IsoTypeWriter.writeUInt8(byteBuffer, sequenceParameterSetExts.size());
            for (byte[] sequenceParameterSetExtNALUnit : sequenceParameterSetExts) {
                IsoTypeWriter.writeUInt16(byteBuffer, sequenceParameterSetExtNALUnit.length);
                byteBuffer.put(sequenceParameterSetExtNALUnit);
            }
        }
    }


    // just to display sps in isoviewer no practical use
    public String[] getPPS() {
        ArrayList<String> l = new ArrayList<String>();
        for (byte[] pictureParameterSet : pictureParameterSets) {
            String details = "not parsable";
            try {
                details = PictureParameterSet.read(pictureParameterSet).toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            l.add(details);
        }
        return l.toArray(new String[l.size()]);
    }

    // just to display sps in isoviewer no practical use
    public String[] getSPS() {
        ArrayList<String> l = new ArrayList<String>();
        for (byte[] sequenceParameterSet : sequenceParameterSets) {
            String detail = "not parsable";
            try {
                detail = SeqParameterSet.read(new ByteArrayInputStream(sequenceParameterSet)).toString();
            } catch (IOException e) {

            }
            l.add(detail);
        }
        return l.toArray(new String[l.size()]);
    }


}

