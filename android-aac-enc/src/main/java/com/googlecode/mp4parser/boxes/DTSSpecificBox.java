package com.googlecode.mp4parser.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BitReaderBuffer;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BitWriterBuffer;

import java.nio.ByteBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: magnus
 * Date: 2012-03-09
 * Time: 16:11
 * To change this template use File | Settings | File Templates.
 */
public class DTSSpecificBox extends AbstractBox {
    
    long DTSSamplingFrequency;
    long maxBitRate;
    long avgBitRate;
    int pcmSampleDepth;
    int frameDuration;
    int streamConstruction;
    int coreLFEPresent;
    int coreLayout;
    int coreSize;
    int stereoDownmix;
    int representationType;
    int channelLayout;
    int multiAssetFlag;
    int LBRDurationMod;
    int reservedBoxPresent;
    int reserved;

    public DTSSpecificBox() {
        super("ddts");
    }

    @Override
    protected long getContentSize() {
        return 20;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        DTSSamplingFrequency = IsoTypeReader.readUInt32(content);
        maxBitRate = IsoTypeReader.readUInt32(content);
        avgBitRate = IsoTypeReader.readUInt32(content);
        pcmSampleDepth = IsoTypeReader.readUInt8(content);
        BitReaderBuffer brb = new BitReaderBuffer(content);
        frameDuration = brb.readBits(2);
        streamConstruction = brb.readBits(5);
        coreLFEPresent = brb.readBits(1);
        coreLayout = brb.readBits(6);
        coreSize = brb.readBits(14);
        stereoDownmix = brb.readBits(1);
        representationType = brb.readBits(3);
        channelLayout = brb.readBits(16);
        multiAssetFlag = brb.readBits(1);
        LBRDurationMod = brb.readBits(1);
        reservedBoxPresent = brb.readBits(1);
        reserved = brb.readBits(5);

    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        IsoTypeWriter.writeUInt32(byteBuffer, DTSSamplingFrequency);
        IsoTypeWriter.writeUInt32(byteBuffer, maxBitRate);
        IsoTypeWriter.writeUInt32(byteBuffer, avgBitRate);
        IsoTypeWriter.writeUInt8(byteBuffer, pcmSampleDepth);
        BitWriterBuffer bwb = new BitWriterBuffer(byteBuffer);
        bwb.writeBits(frameDuration, 2);
        bwb.writeBits(streamConstruction, 5);
        bwb.writeBits(coreLFEPresent, 1);
        bwb.writeBits(coreLayout, 6);
        bwb.writeBits(coreSize, 14);
        bwb.writeBits(stereoDownmix, 1);
        bwb.writeBits(representationType, 3);
        bwb.writeBits(channelLayout, 16);
        bwb.writeBits(multiAssetFlag, 1);
        bwb.writeBits(LBRDurationMod, 1);
        bwb.writeBits(reservedBoxPresent, 1);
        bwb.writeBits(reserved, 5);

    }

    public long getAvgBitRate() {
        return avgBitRate;
    }

    public void setAvgBitRate(long avgBitRate) {
        this.avgBitRate = avgBitRate;
    }

    public long getDTSSamplingFrequency() {
        return DTSSamplingFrequency;
    }

    public void setDTSSamplingFrequency(long DTSSamplingFrequency) {
        this.DTSSamplingFrequency = DTSSamplingFrequency;
    }

    public long getMaxBitRate() {
        return maxBitRate;
    }

    public void setMaxBitRate(long maxBitRate) {
        this.maxBitRate = maxBitRate;
    }

    public int getPcmSampleDepth() {
        return pcmSampleDepth;
    }

    public void setPcmSampleDepth(int pcmSampleDepth) {
        this.pcmSampleDepth = pcmSampleDepth;
    }

    public int getFrameDuration() {
        return frameDuration;
    }

    public void setFrameDuration(int frameDuration) {
        this.frameDuration = frameDuration;
    }

    public int getStreamConstruction() {
        return streamConstruction;
    }

    public void setStreamConstruction(int streamConstruction) {
        this.streamConstruction = streamConstruction;
    }

    public int getCoreLFEPresent() {
        return coreLFEPresent;
    }

    public void setCoreLFEPresent(int coreLFEPresent) {
        this.coreLFEPresent = coreLFEPresent;
    }

    public int getCoreLayout() {
        return coreLayout;
    }

    public void setCoreLayout(int coreLayout) {
        this.coreLayout = coreLayout;
    }

    public int getCoreSize() {
        return coreSize;
    }

    public void setCoreSize(int coreSize) {
        this.coreSize = coreSize;
    }

    public int getStereoDownmix() {
        return stereoDownmix;
    }

    public void setStereoDownmix(int stereoDownmix) {
        this.stereoDownmix = stereoDownmix;
    }

    public int getRepresentationType() {
        return representationType;
    }

    public void setRepresentationType(int representationType) {
        this.representationType = representationType;
    }

    public int getChannelLayout() {
        return channelLayout;
    }

    public void setChannelLayout(int channelLayout) {
        this.channelLayout = channelLayout;
    }

    public int getMultiAssetFlag() {
        return multiAssetFlag;
    }

    public void setMultiAssetFlag(int multiAssetFlag) {
        this.multiAssetFlag = multiAssetFlag;
    }

    public int getLBRDurationMod() {
        return LBRDurationMod;
    }

    public void setLBRDurationMod(int LBRDurationMod) {
        this.LBRDurationMod = LBRDurationMod;
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    public int getReservedBoxPresent() {
        return reservedBoxPresent;
    }

    public void setReservedBoxPresent(int reservedBoxPresent) {
        this.reservedBoxPresent = reservedBoxPresent;
    }
}
