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

import com.googlecode.mp4parser.AbstractContainerBox;

/**
 * The sample table contains all the time and data indexing of the media samples in a track. Using the tables
 * here, it is possible to locate samples in time, determine their type (e.g. I-frame or not), and determine their
 * size, container, and offset into that container.  <br>
 * If the track that contains the Sample Table Box references no data, then the Sample Table Box does not need
 * to contain any sub-boxes (this is not a very useful media track).                                          <br>
 * If the track that the Sample Table Box is contained in does reference data, then the following sub-boxes are
 * required: Sample Description, Sample Size, Sample To Chunk, and Chunk Offset. Further, the Sample
 * Description Box shall contain at least one entry. A Sample Description Box is required because it contains the
 * data reference index field which indicates which Data Reference Box to use to retrieve the media samples.
 * Without the Sample Description, it is not possible to determine where the media samples are stored. The Sync
 * Sample Box is optional. If the Sync Sample Box is not present, all samples are sync samples.<br>
 * Annex A provides a narrative description of random access using the structures defined in the Sample Table
 * Box.
 */
public class SampleTableBox extends AbstractContainerBox {
    public static final String TYPE = "stbl";

    public SampleTableBox() {
        super(TYPE);
    }

    public SampleDescriptionBox getSampleDescriptionBox() {
        for (Box box : boxes) {
            if (box instanceof SampleDescriptionBox) {
                return (SampleDescriptionBox) box;
            }
        }
        return null;
    }

    public SampleSizeBox getSampleSizeBox() {
        for (Box box : boxes) {
            if (box instanceof SampleSizeBox) {
                return (SampleSizeBox) box;
            }
        }
        return null;
    }

    public SampleToChunkBox getSampleToChunkBox() {
        for (Box box : boxes) {
            if (box instanceof SampleToChunkBox) {
                return (SampleToChunkBox) box;
            }
        }
        return null;
    }

    public ChunkOffsetBox getChunkOffsetBox() {
        for (Box box : boxes) {
            if (box instanceof ChunkOffsetBox) {
                return (ChunkOffsetBox) box;
            }
        }
        return null;
    }

    public void setChunkOffsetBox(ChunkOffsetBox b) {
        for (int i = 0; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            if (box instanceof ChunkOffsetBox) {
                boxes.set(i, b);
            }
        }
    }

    public TimeToSampleBox getTimeToSampleBox() {
        for (Box box : boxes) {
            if (box instanceof TimeToSampleBox) {
                return (TimeToSampleBox) box;
            }
        }
        return null;
    }

    public SyncSampleBox getSyncSampleBox() {
        for (Box box : boxes) {
            if (box instanceof SyncSampleBox) {
                return (SyncSampleBox) box;
            }
        }
        return null;
    }

    public CompositionTimeToSample getCompositionTimeToSample() {
        for (Box box : boxes) {
            if (box instanceof CompositionTimeToSample) {
                return (CompositionTimeToSample) box;
            }
        }
        return null;
    }

    public SampleDependencyTypeBox getSampleDependencyTypeBox() {
        for (Box box : boxes) {
            if (box instanceof SampleDependencyTypeBox) {
                return (SampleDependencyTypeBox) box;
            }
        }
        return null;
    }

}
