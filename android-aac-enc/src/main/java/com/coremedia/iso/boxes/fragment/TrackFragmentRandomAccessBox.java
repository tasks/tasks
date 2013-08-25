/*
 * Copyright 2009 castLabs GmbH, Berlin
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

package com.coremedia.iso.boxes.fragment;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeReaderVariable;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.IsoTypeWriterVariable;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * aligned(8) class TrackFragmentRandomAccessBox
 * extends FullBox('tfra', version, 0) {
 * unsigned int(32) track_ID;
 * const unsigned int(26) reserved = 0;
 * unsigned int(2) length_size_of_traf_num;
 * unsigned int(2) length_size_of_trun_num;
 * unsigned int(2) length_size_of_sample_num;
 * unsigned int(32) number_of_entry;
 * for(i=1; i <= number_of_entry; i++){
 * if(version==1){
 * unsigned int(64) time;
 * unsigned int(64) moof_offset;
 * }else{
 * unsigned int(32) time;
 * unsigned int(32) moof_offset;
 * }
 * unsigned int((length_size_of_traf_num+1) * 8) traf_number;
 * unsigned int((length_size_of_trun_num+1) * 8) trun_number;
 * unsigned int((length_size_of_sample_num+1) * 8) sample_number;
 * }
 * }
 */
public class TrackFragmentRandomAccessBox extends AbstractFullBox {
    public static final String TYPE = "tfra";

    private long trackId;
    private int reserved;
    private int lengthSizeOfTrafNum = 2;
    private int lengthSizeOfTrunNum = 2;
    private int lengthSizeOfSampleNum = 2;
    private List<Entry> entries = Collections.emptyList();

    public TrackFragmentRandomAccessBox() {
        super(TYPE);
    }


    protected long getContentSize() {
        long contentSize = 4;
        contentSize += 4 + 4 /*26 + 2 + 2 + 2 */ + 4;
        if (getVersion() == 1) {
            contentSize += (8 + 8) * entries.size();
        } else {
            contentSize += (4 + 4) * entries.size();
        }
        contentSize += lengthSizeOfTrafNum * entries.size();
        contentSize += lengthSizeOfTrunNum * entries.size();
        contentSize += lengthSizeOfSampleNum * entries.size();
        return contentSize;
    }


    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        trackId = IsoTypeReader.readUInt32(content);
        long temp = IsoTypeReader.readUInt32(content);
        reserved = (int) (temp >> 6);
        lengthSizeOfTrafNum = ((int) (temp & 0x3F) >> 4) + 1;
        lengthSizeOfTrunNum = ((int) (temp & 0xC) >> 2) + 1;
        lengthSizeOfSampleNum = ((int) (temp & 0x3)) + 1;
        long numberOfEntries = IsoTypeReader.readUInt32(content);

        entries = new ArrayList<Entry>();

        for (int i = 0; i < numberOfEntries; i++) {
            Entry entry = new Entry();
            if (getVersion() == 1) {
                entry.time = IsoTypeReader.readUInt64(content);
                entry.moofOffset = IsoTypeReader.readUInt64(content);
            } else {
                entry.time = IsoTypeReader.readUInt32(content);
                entry.moofOffset = IsoTypeReader.readUInt32(content);
            }
            entry.trafNumber = IsoTypeReaderVariable.read(content, lengthSizeOfTrafNum);
            entry.trunNumber = IsoTypeReaderVariable.read(content, lengthSizeOfTrunNum);
            entry.sampleNumber = IsoTypeReaderVariable.read(content, lengthSizeOfSampleNum);

            entries.add(entry);
        }

    }


    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        IsoTypeWriter.writeUInt32(byteBuffer, trackId);
        long temp;
        temp = reserved << 6;
        temp = temp | (((lengthSizeOfTrafNum - 1) & 0x3) << 4);
        temp = temp | (((lengthSizeOfTrunNum - 1) & 0x3) << 2);
        temp = temp | ((lengthSizeOfSampleNum - 1) & 0x3);
        IsoTypeWriter.writeUInt32(byteBuffer, temp);
        IsoTypeWriter.writeUInt32(byteBuffer, entries.size());

        for (Entry entry : entries) {
            if (getVersion() == 1) {
                IsoTypeWriter.writeUInt64(byteBuffer, entry.time);
                IsoTypeWriter.writeUInt64(byteBuffer, entry.moofOffset);
            } else {
                IsoTypeWriter.writeUInt32(byteBuffer, entry.time);
                IsoTypeWriter.writeUInt32(byteBuffer, entry.moofOffset);
            }
            IsoTypeWriterVariable.write(entry.trafNumber, byteBuffer, lengthSizeOfTrafNum);
            IsoTypeWriterVariable.write(entry.trunNumber, byteBuffer, lengthSizeOfTrunNum);
            IsoTypeWriterVariable.write(entry.sampleNumber, byteBuffer, lengthSizeOfSampleNum);

        }
    }


    public void setTrackId(long trackId) {
        this.trackId = trackId;
    }

    public void setLengthSizeOfTrafNum(int lengthSizeOfTrafNum) {
        this.lengthSizeOfTrafNum = lengthSizeOfTrafNum;
    }

    public void setLengthSizeOfTrunNum(int lengthSizeOfTrunNum) {
        this.lengthSizeOfTrunNum = lengthSizeOfTrunNum;
    }

    public void setLengthSizeOfSampleNum(int lengthSizeOfSampleNum) {
        this.lengthSizeOfSampleNum = lengthSizeOfSampleNum;
    }

    public long getTrackId() {
        return trackId;
    }

    public int getReserved() {
        return reserved;
    }

    public int getLengthSizeOfTrafNum() {
        return lengthSizeOfTrafNum;
    }

    public int getLengthSizeOfTrunNum() {
        return lengthSizeOfTrunNum;
    }

    public int getLengthSizeOfSampleNum() {
        return lengthSizeOfSampleNum;
    }

    public long getNumberOfEntries() {
        return entries.size();
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public static class Entry {
        private long time;
        private long moofOffset;
        private long trafNumber;
        private long trunNumber;
        private long sampleNumber;

        public Entry() {
        }

        public Entry(long time, long moofOffset, long trafNumber, long trunNumber, long sampleNumber) {
            this.moofOffset = moofOffset;
            this.sampleNumber = sampleNumber;
            this.time = time;
            this.trafNumber = trafNumber;
            this.trunNumber = trunNumber;
        }

        public long getTime() {
            return time;
        }

        public long getMoofOffset() {
            return moofOffset;
        }

        public long getTrafNumber() {
            return trafNumber;
        }

        public long getTrunNumber() {
            return trunNumber;
        }

        public long getSampleNumber() {
            return sampleNumber;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public void setMoofOffset(long moofOffset) {
            this.moofOffset = moofOffset;
        }

        public void setTrafNumber(long trafNumber) {
            this.trafNumber = trafNumber;
        }

        public void setTrunNumber(long trunNumber) {
            this.trunNumber = trunNumber;
        }

        public void setSampleNumber(long sampleNumber) {
            this.sampleNumber = sampleNumber;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "time=" + time +
                    ", moofOffset=" + moofOffset +
                    ", trafNumber=" + trafNumber +
                    ", trunNumber=" + trunNumber +
                    ", sampleNumber=" + sampleNumber +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            if (moofOffset != entry.moofOffset) return false;
            if (sampleNumber != entry.sampleNumber) return false;
            if (time != entry.time) return false;
            if (trafNumber != entry.trafNumber) return false;
            if (trunNumber != entry.trunNumber) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (time ^ (time >>> 32));
            result = 31 * result + (int) (moofOffset ^ (moofOffset >>> 32));
            result = 31 * result + (int) (trafNumber ^ (trafNumber >>> 32));
            result = 31 * result + (int) (trunNumber ^ (trunNumber >>> 32));
            result = 31 * result + (int) (sampleNumber ^ (sampleNumber >>> 32));
            return result;
        }
    }

}
