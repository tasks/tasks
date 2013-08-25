package com.googlecode.mp4parser.boxes;

import com.googlecode.mp4parser.AbstractBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BitReaderBuffer;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BitWriterBuffer;

import java.nio.ByteBuffer;


public class MLPSpecificBox extends AbstractBox {

    int format_info;
    int peak_data_rate;
    int reserved;
    int reserved2;

    public MLPSpecificBox() {
        super("dmlp");
    }

    @Override
    protected long getContentSize() {
        return 10;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        BitReaderBuffer brb = new BitReaderBuffer(content);
        format_info = brb.readBits(32);
        peak_data_rate = brb.readBits(15);
        reserved = brb.readBits(1);
        reserved2 = brb.readBits(32);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        BitWriterBuffer bwb = new BitWriterBuffer(byteBuffer);
        bwb.writeBits(format_info, 32);
        bwb.writeBits(peak_data_rate, 15);
        bwb.writeBits(reserved, 1);
        bwb.writeBits(reserved2, 32);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getFormat_info() {
        return format_info;
    }

    public void setFormat_info(int format_info) {
        this.format_info = format_info;
    }

    public int getPeak_data_rate() {
        return peak_data_rate;
    }

    public void setPeak_data_rate(int peak_data_rate) {
        this.peak_data_rate = peak_data_rate;
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    public int getReserved2() {
        return reserved2;
    }

    public void setReserved2(int reserved2) {
        this.reserved2 = reserved2;
    }
}
