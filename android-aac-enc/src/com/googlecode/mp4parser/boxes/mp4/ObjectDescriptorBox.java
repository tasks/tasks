/*
 * Copyright 2011 Sebastian Annies, Hamburg
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

package com.googlecode.mp4parser.boxes.mp4;

/**
 * This object contains an Object Descriptor or an Initial Object Descriptor.
 * There are a number of possible file types based on usage, depending on the descriptor:
 * <ul>
 * <li>Presentation, contains IOD which contains a BIFS stream (MP4 file);
 * <li>Sub-part of a presentation, contains an IOD without a BIFS stream (MP4 file);</li>
 * <li>Sub-part of a presentation, contains an OD (MP4 file);</li>
 * <li>Free-form file, referenced by MP4 data references (free-format);</li>
 * <li>Sub-part of a presentation, referenced by an ES URL.</li>
 * </ul>
 * NOTE: <br/>
 * The first three are MP4 files, a file referenced by a data reference is not necessarily an MP4 file, as it is
 * free-format. Files referenced by ES URLs, by data references, or intended as input to an editing process, need not have
 * an Object Descriptor Box. <br/>
 * An OD URL may point to an MP4 file. Implicitly, the target of such a URL is the OD/IOD located in the 'iods'
 * atom in that file.</br/>
 * If an MP4 file contains several object descriptors, only the OD/IOD in the 'iods' atom can be addressed using
 * an OD URL from a remote MPEG-4 presentation.
 */
public class ObjectDescriptorBox extends AbstractDescriptorBox {
    public static final String TYPE = "iods";

    public ObjectDescriptorBox() {
        super(TYPE);
    }


}
