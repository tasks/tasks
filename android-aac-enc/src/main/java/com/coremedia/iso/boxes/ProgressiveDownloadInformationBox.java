package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ProgressiveDownloadInformationBox extends AbstractFullBox {


    List<Entry> entries = Collections.emptyList();

    public ProgressiveDownloadInformationBox() {
        super("pdin");
    }

    @Override
    protected long getContentSize() {
        return 4 + entries.size() * 8;
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        for (Entry entry : entries) {
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getRate());
            IsoTypeWriter.writeUInt32(byteBuffer, entry.getInitialDelay());
        }
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        entries = new LinkedList<Entry>();
        while (content.remaining() >= 8) {
            Entry entry = new Entry(IsoTypeReader.readUInt32(content), IsoTypeReader.readUInt32(content));
            entries.add(entry);
        }
    }


    public static class Entry {
        long rate;
        long initialDelay;

        public Entry(long rate, long initialDelay) {
            this.rate = rate;
            this.initialDelay = initialDelay;
        }

        public long getRate() {
            return rate;
        }

        public void setRate(long rate) {
            this.rate = rate;
        }

        public long getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(long initialDelay) {
            this.initialDelay = initialDelay;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "rate=" + rate +
                    ", initialDelay=" + initialDelay +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ProgressiveDownloadInfoBox{" +
                "entries=" + entries +
                '}';
    }

}