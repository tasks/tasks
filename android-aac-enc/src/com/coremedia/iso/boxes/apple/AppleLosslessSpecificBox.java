package com.coremedia.iso.boxes.apple;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;

/**
 *
 */
public final class AppleLosslessSpecificBox extends AbstractFullBox {

    public static final String TYPE = "alac";
    /*
   Extradata: 32bit size 32bit tag (=alac) 32bit zero?
   32bit max sample per frame 8bit ?? (zero?) 8bit sample
   size 8bit history mult 8bit initial history 8bit kmodifier
   8bit channels? 16bit ?? 32bit max coded frame size 32bit
   bitrate? 32bit samplerate
    */
    private long maxSamplePerFrame; // 32bi
    private int unknown1; // 8bit
    private int sampleSize; // 8bit
    private int historyMult; // 8bit
    private int initialHistory; // 8bit
    private int kModifier; // 8bit
    private int channels; // 8bit
    private int unknown2; // 16bit
    private long maxCodedFrameSize; // 32bit
    private long bitRate; // 32bit
    private long sampleRate; // 32bit

    public long getMaxSamplePerFrame() {
        return maxSamplePerFrame;
    }

    public void setMaxSamplePerFrame(int maxSamplePerFrame) {
        this.maxSamplePerFrame = maxSamplePerFrame;
    }

    public int getUnknown1() {
        return unknown1;
    }

    public void setUnknown1(int unknown1) {
        this.unknown1 = unknown1;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public int getHistoryMult() {
        return historyMult;
    }

    public void setHistoryMult(int historyMult) {
        this.historyMult = historyMult;
    }

    public int getInitialHistory() {
        return initialHistory;
    }

    public void setInitialHistory(int initialHistory) {
        this.initialHistory = initialHistory;
    }

    public int getKModifier() {
        return kModifier;
    }

    public void setKModifier(int kModifier) {
        this.kModifier = kModifier;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    public int getUnknown2() {
        return unknown2;
    }

    public void setUnknown2(int unknown2) {
        this.unknown2 = unknown2;
    }

    public long getMaxCodedFrameSize() {
        return maxCodedFrameSize;
    }

    public void setMaxCodedFrameSize(int maxCodedFrameSize) {
        this.maxCodedFrameSize = maxCodedFrameSize;
    }

    public long getBitRate() {
        return bitRate;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public long getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }


    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        maxSamplePerFrame = IsoTypeReader.readUInt32(content);
        unknown1 = IsoTypeReader.readUInt8(content);
        sampleSize = IsoTypeReader.readUInt8(content);
        historyMult = IsoTypeReader.readUInt8(content);
        initialHistory = IsoTypeReader.readUInt8(content);
        kModifier = IsoTypeReader.readUInt8(content);
        channels = IsoTypeReader.readUInt8(content);
        unknown2 = IsoTypeReader.readUInt16(content);
        maxCodedFrameSize = IsoTypeReader.readUInt32(content);
        bitRate = IsoTypeReader.readUInt32(content);
        sampleRate = IsoTypeReader.readUInt32(content);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, maxSamplePerFrame);
        IsoTypeWriter.writeUInt8(byteBuffer, unknown1);
        IsoTypeWriter.writeUInt8(byteBuffer, sampleSize);
        IsoTypeWriter.writeUInt8(byteBuffer, historyMult);
        IsoTypeWriter.writeUInt8(byteBuffer, initialHistory);
        IsoTypeWriter.writeUInt8(byteBuffer, kModifier);
        IsoTypeWriter.writeUInt8(byteBuffer, channels);
        IsoTypeWriter.writeUInt16(byteBuffer, unknown2);
        IsoTypeWriter.writeUInt32(byteBuffer, maxCodedFrameSize);
        IsoTypeWriter.writeUInt32(byteBuffer, bitRate);
        IsoTypeWriter.writeUInt32(byteBuffer, sampleRate);
    }

    public AppleLosslessSpecificBox() {
        super("alac");
    }

    protected long getContentSize() {
        return 28;
    }

}
