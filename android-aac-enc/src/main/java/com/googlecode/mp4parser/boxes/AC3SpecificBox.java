package com.googlecode.mp4parser.boxes;

import com.googlecode.mp4parser.AbstractBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BitReaderBuffer;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BitWriterBuffer;

import java.nio.ByteBuffer;

public class AC3SpecificBox extends AbstractBox {
    int fscod;
    int bsid;
    int bsmod;
    int acmod;
    int lfeon;
    int bitRateCode;
    int reserved;

    public AC3SpecificBox() {
        super("dac3");
    }

    @Override
    protected long getContentSize() {
        return 3;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        BitReaderBuffer brb = new BitReaderBuffer(content);
        fscod = brb.readBits(2);
        bsid = brb.readBits(5);
        bsmod = brb.readBits(3);
        acmod = brb.readBits(3);
        lfeon = brb.readBits(1);
        bitRateCode = brb.readBits(5);
        reserved = brb.readBits(5);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        BitWriterBuffer bwb = new BitWriterBuffer(byteBuffer);
        bwb.writeBits(fscod, 2);
        bwb.writeBits(bsid, 5);
        bwb.writeBits(bsmod, 3);
        bwb.writeBits(acmod, 3);
        bwb.writeBits(lfeon, 1);
        bwb.writeBits(bitRateCode, 5);
        bwb.writeBits(reserved, 5);
    }

    public int getFscod() {
        return fscod;
    }

    public void setFscod(int fscod) {
        this.fscod = fscod;
    }

    public int getBsid() {
        return bsid;
    }

    public void setBsid(int bsid) {
        this.bsid = bsid;
    }

    public int getBsmod() {
        return bsmod;
    }

    public void setBsmod(int bsmod) {
        this.bsmod = bsmod;
    }

    public int getAcmod() {
        return acmod;
    }

    public void setAcmod(int acmod) {
        this.acmod = acmod;
    }

    public int getLfeon() {
        return lfeon;
    }

    public void setLfeon(int lfeon) {
        this.lfeon = lfeon;
    }

    public int getBitRateCode() {
        return bitRateCode;
    }

    public void setBitRateCode(int bitRateCode) {
        this.bitRateCode = bitRateCode;
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    @Override
    public String toString() {
        return "AC3SpecificBox{" +
                "fscod=" + fscod +
                ", bsid=" + bsid +
                ", bsmod=" + bsmod +
                ", acmod=" + acmod +
                ", lfeon=" + lfeon +
                ", bitRateCode=" + bitRateCode +
                ", reserved=" + reserved +
                '}';
    }
}
