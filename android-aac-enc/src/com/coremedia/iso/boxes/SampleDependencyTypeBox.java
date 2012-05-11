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

package com.coremedia.iso.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * aligned(8) class SampleDependencyTypeBox
 * extends FullBox('sdtp', version = 0, 0) {
 * for (i=0; i < sample_count; i++){
 * unsigned int(2) reserved = 0;
 * unsigned int(2) sample_depends_on;
 * unsigned int(2) sample_is_depended_on;
 * unsigned int(2) sample_has_redundancy;
 * }
 * }
 */
public class SampleDependencyTypeBox extends AbstractFullBox {
    public static final String TYPE = "sdtp";

    private List<Entry> entries = new ArrayList<Entry>();

    public static class Entry {

        public Entry(int value) {
            this.value = value;
        }

        private int value;


        public int getReserved() {
            return (value >> 6) & 0x03;
        }

        public void setReserved(int res) {
            value = (res & 0x03) << 6 | value & 0x3f;
        }

        public int getSampleDependsOn() {
            return (value >> 4) & 0x03;
        }

        public void setSampleDependsOn(int sdo) {
            value = (sdo & 0x03) << 4 | value & 0xcf;
        }

        public int getSampleIsDependentOn() {
            return (value >> 2) & 0x03;
        }

        public void setSampleIsDependentOn(int sido) {
            value = (sido & 0x03) << 2 | value & 0xf3;
        }

        public int getSampleHasRedundancy() {
            return value & 0x03;
        }

        public void setSampleHasRedundancy(int shr) {
            value = shr & 0x03 | value & 0xfc;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "reserved=" + getReserved() +
                    ", sampleDependsOn=" + getSampleDependsOn() +
                    ", sampleIsDependentOn=" + getSampleIsDependentOn() +
                    ", sampleHasRedundancy=" + getSampleHasRedundancy() +
                    '}';
        }
    }

    public SampleDependencyTypeBox() {
        super(TYPE);
    }

    @Override
    protected long getContentSize() {
        return 4 + entries.size();
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        for (Entry entry : entries) {
            IsoTypeWriter.writeUInt8(byteBuffer, entry.value);
        }
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        while (content.remaining() > 0) {
            entries.add(new Entry(IsoTypeReader.readUInt8(content)));
        }
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SampleDependencyTypeBox");
        sb.append("{entries=").append(entries);
        sb.append('}');
        return sb.toString();
    }
}
