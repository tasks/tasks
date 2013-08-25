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

import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BitReaderBuffer;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BitWriterBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * bit(6) reserved=0;
 * unsigned int(2) sample_depends_on;
 * unsigned int(2) sample_is_depended_on;
 * unsigned int(2) sample_has_redundancy;
 * bit(3) sample_padding_value;
 * bit(1) sample_is_difference_sample;
 * // i.e. when 1 signals a non-key or non-sync sample
 * unsigned int(16) sample_degradation_priority;
 */
public class SampleFlags {
    private int reserved;
    private int sampleDependsOn;
    private int sampleIsDependedOn;
    private int sampleHasRedundancy;
    private int samplePaddingValue;
    private boolean sampleIsDifferenceSample;
    private int sampleDegradationPriority;

    public SampleFlags() {

    }

    public SampleFlags(ByteBuffer bb) {
        BitReaderBuffer brb = new BitReaderBuffer(bb);
        reserved = brb.readBits(6);
        sampleDependsOn = brb.readBits(2);
        sampleIsDependedOn = brb.readBits(2);
        sampleHasRedundancy = brb.readBits(2);
        samplePaddingValue = brb.readBits(3);
        sampleIsDifferenceSample = brb.readBits(1) == 1;
        sampleDegradationPriority = brb.readBits(16);
    }


    public void getContent(ByteBuffer os) {
        BitWriterBuffer bitWriterBuffer = new BitWriterBuffer(os);
        bitWriterBuffer.writeBits(reserved, 6);
        bitWriterBuffer.writeBits(sampleDependsOn, 2);
        bitWriterBuffer.writeBits(sampleIsDependedOn, 2);
        bitWriterBuffer.writeBits(sampleHasRedundancy, 2);
        bitWriterBuffer.writeBits(samplePaddingValue, 3);
        bitWriterBuffer.writeBits(this.sampleIsDifferenceSample ? 1 : 0, 1);
        bitWriterBuffer.writeBits(sampleDegradationPriority, 16);
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    /**
     * @see #setSampleDependsOn(int)
     */
    public int getSampleDependsOn() {
        return sampleDependsOn;
    }

    /**
     * sample_depends_on takes one of the following four values:
     * <pre>
     * 0: the dependency of this sample is unknown;
     * 1: this sample does depend on others (not an I picture);
     * 2: this sample does not depend on others (I picture);
     * 3: reserved
     * </pre>
     *
     */
    public void setSampleDependsOn(int sampleDependsOn) {
        this.sampleDependsOn = sampleDependsOn;
    }

    /**
     * @see #setSampleIsDependedOn(int)
     */
    public int getSampleIsDependedOn() {
        return sampleIsDependedOn;
    }

    /**
     * sample_is_depended_on takes one of the following four values:
     * <pre>
     * 0: the dependency of other samples on this sample is unknown;
     * 1: other samples may depend on this one (not disposable);
     * 2: no other sample depends on this one (disposable);
     * 3: reserved
     * </pre>
     *
     */
    public void setSampleIsDependedOn(int sampleIsDependedOn) {
        this.sampleIsDependedOn = sampleIsDependedOn;
    }

    /**
     * @see #setSampleHasRedundancy(int)
     */
    public int getSampleHasRedundancy() {
        return sampleHasRedundancy;
    }

    /**
     * sample_has_redundancy takes one of the following four values:
     * <pre>
     * 0: it is unknown whether there is redundant coding in this sample;
     * 1: there is redundant coding in this sample;
     * 2: there is no redundant coding in this sample;
     * 3: reserved
     * </pre>
     */
    public void setSampleHasRedundancy(int sampleHasRedundancy) {
        this.sampleHasRedundancy = sampleHasRedundancy;
    }

    public int getSamplePaddingValue() {
        return samplePaddingValue;
    }

    public void setSamplePaddingValue(int samplePaddingValue) {
        this.samplePaddingValue = samplePaddingValue;
    }

    public boolean isSampleIsDifferenceSample() {
        return sampleIsDifferenceSample;
    }


    public void setSampleIsDifferenceSample(boolean sampleIsDifferenceSample) {
        this.sampleIsDifferenceSample = sampleIsDifferenceSample;
    }

    public int getSampleDegradationPriority() {
        return sampleDegradationPriority;
    }

    public void setSampleDegradationPriority(int sampleDegradationPriority) {
        this.sampleDegradationPriority = sampleDegradationPriority;
    }

    @Override
    public String toString() {
        return "SampleFlags{" +
                "reserved=" + reserved +
                ", sampleDependsOn=" + sampleDependsOn +
                ", sampleHasRedundancy=" + sampleHasRedundancy +
                ", samplePaddingValue=" + samplePaddingValue +
                ", sampleIsDifferenceSample=" + sampleIsDifferenceSample +
                ", sampleDegradationPriority=" + sampleDegradationPriority +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SampleFlags that = (SampleFlags) o;

        if (reserved != that.reserved) return false;
        if (sampleDegradationPriority != that.sampleDegradationPriority) return false;
        if (sampleDependsOn != that.sampleDependsOn) return false;
        if (sampleHasRedundancy != that.sampleHasRedundancy) return false;
        if (sampleIsDependedOn != that.sampleIsDependedOn) return false;
        if (sampleIsDifferenceSample != that.sampleIsDifferenceSample) return false;
        if (samplePaddingValue != that.samplePaddingValue) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = reserved;
        result = 31 * result + sampleDependsOn;
        result = 31 * result + sampleIsDependedOn;
        result = 31 * result + sampleHasRedundancy;
        result = 31 * result + samplePaddingValue;
        result = 31 * result + (sampleIsDifferenceSample ? 1 : 0);
        result = 31 * result + sampleDegradationPriority;
        return result;
    }
}
