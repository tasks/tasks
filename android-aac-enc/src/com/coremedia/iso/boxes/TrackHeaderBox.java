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

package com.coremedia.iso.boxes;


import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;

/**
 * This box specifies the characteristics of a single track. Exactly one Track Header Box is contained in a track.<br>
 * In the absence of an edit list, the presentation of a track starts at the beginning of the overall presentation. An
 * empty edit is used to offset the start time of a track. <br>
 * The default value of the track header flags for media tracks is 7 (track_enabled, track_in_movie,
 * track_in_preview). If in a presentation all tracks have neither track_in_movie nor track_in_preview set, then all
 * tracks shall be treated as if both flags were set on all tracks. Hint tracks should have the track header flags set
 * to 0, so that they are ignored for local playback and preview.
 */
public class TrackHeaderBox extends AbstractFullBox {

    public static final String TYPE = "tkhd";

    private long creationTime;
    private long modificationTime;
    private long trackId;
    private long duration;
    private int layer;
    private int alternateGroup;
    private float volume;
    private long[] matrix = new long[]{0x00010000, 0, 0, 0, 0x00010000, 0, 0, 0, 0x40000000};
    private double width;
    private double height;


    public TrackHeaderBox() {
        super(TYPE);

    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getModificationTime() {
        return modificationTime;
    }

    public long getTrackId() {
        return trackId;
    }

    public long getDuration() {
        return duration;
    }

    public int getLayer() {
        return layer;
    }

    public int getAlternateGroup() {
        return alternateGroup;
    }

    public float getVolume() {
        return volume;
    }

    public long[] getMatrix() {
        return matrix;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    protected long getContentSize() {
        long contentSize = 4;
        if (getVersion() == 1) {
            contentSize += 32;
        } else {
            contentSize += 20;
        }
        contentSize += 60;
        return contentSize;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        if (getVersion() == 1) {
            creationTime = IsoTypeReader.readUInt64(content);
            modificationTime = IsoTypeReader.readUInt64(content);
            trackId = IsoTypeReader.readUInt32(content);
            IsoTypeReader.readUInt32(content);
            duration = IsoTypeReader.readUInt64(content);
        } else {
            creationTime = IsoTypeReader.readUInt32(content);
            modificationTime = IsoTypeReader.readUInt32(content);
            trackId = IsoTypeReader.readUInt32(content);
            IsoTypeReader.readUInt32(content);
            duration = IsoTypeReader.readUInt32(content);
        } // 196
        IsoTypeReader.readUInt32(content);
        IsoTypeReader.readUInt32(content);
        layer = IsoTypeReader.readUInt16(content);    // 204
        alternateGroup = IsoTypeReader.readUInt16(content);
        volume = IsoTypeReader.readFixedPoint88(content);
        IsoTypeReader.readUInt16(content);     // 212
        matrix = new long[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = IsoTypeReader.readUInt32(content);
        }
        width = IsoTypeReader.readFixedPoint1616(content);    // 248
        height = IsoTypeReader.readFixedPoint1616(content);
    }

    public void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if (getVersion() == 1) {
            IsoTypeWriter.writeUInt64(byteBuffer, creationTime);
            IsoTypeWriter.writeUInt64(byteBuffer, modificationTime);
            IsoTypeWriter.writeUInt32(byteBuffer, trackId);
            IsoTypeWriter.writeUInt32(byteBuffer, 0);
            IsoTypeWriter.writeUInt64(byteBuffer, duration);
        } else {
            IsoTypeWriter.writeUInt32(byteBuffer, creationTime);
            IsoTypeWriter.writeUInt32(byteBuffer, modificationTime);
            IsoTypeWriter.writeUInt32(byteBuffer, trackId);
            IsoTypeWriter.writeUInt32(byteBuffer, 0);
            IsoTypeWriter.writeUInt32(byteBuffer, duration);
        } // 196
        IsoTypeWriter.writeUInt32(byteBuffer, 0);
        IsoTypeWriter.writeUInt32(byteBuffer, 0);
        IsoTypeWriter.writeUInt16(byteBuffer, layer);
        IsoTypeWriter.writeUInt16(byteBuffer, alternateGroup);
        IsoTypeWriter.writeFixedPont88(byteBuffer, volume);
        IsoTypeWriter.writeUInt16(byteBuffer, 0);
        for (int i = 0; i < 9; i++) {
            IsoTypeWriter.writeUInt32(byteBuffer, matrix[i]);
        }
        IsoTypeWriter.writeFixedPont1616(byteBuffer, width);
        IsoTypeWriter.writeFixedPont1616(byteBuffer, height);
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("TrackHeaderBox[");
        result.append("creationTime=").append(getCreationTime());
        result.append(";");
        result.append("modificationTime=").append(getModificationTime());
        result.append(";");
        result.append("trackId=").append(getTrackId());
        result.append(";");
        result.append("duration=").append(getDuration());
        result.append(";");
        result.append("layer=").append(getLayer());
        result.append(";");
        result.append("alternateGroup=").append(getAlternateGroup());
        result.append(";");
        result.append("volume=").append(getVolume());
        for (int i = 0; i < matrix.length; i++) {
            result.append(";");
            result.append("matrix").append(i).append("=").append(matrix[i]);
        }
        result.append(";");
        result.append("width=").append(getWidth());
        result.append(";");
        result.append("height=").append(getHeight());
        result.append("]");
        return result.toString();
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public void setModificationTime(long modificationTime) {
        this.modificationTime = modificationTime;
    }

    public void setTrackId(long trackId) {
        this.trackId = trackId;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public void setAlternateGroup(int alternateGroup) {
        this.alternateGroup = alternateGroup;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public void setMatrix(long[] matrix) {
        this.matrix = matrix;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public void setHeight(double height) {
        this.height = height;
    }


    public boolean isEnabled() {
        return (getFlags() & 1) > 0;
    }

    public boolean isInMovie() {
        return (getFlags() & 2) > 0;
    }

    public boolean isInPreview() {
        return (getFlags() & 4) > 0;
    }

    public boolean isInPoster() {
        return (getFlags() & 8) > 0;
    }
}
