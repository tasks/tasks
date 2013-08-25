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
 * <code>
 * Box Type: 'tref'<br>
 * Container: {@link TrackBox} ('trak')<br>
 * Mandatory: No<br>
 * Quantity: Zero or one<br><br>
 * </code>
 * This box provides a reference from the containing track to another track in the presentation. These references
 * are typed. A 'hint' reference links from the containing hint track to the media data that it hints. A content
 * description reference 'cdsc' links a descriptive or metadata track to the content which it describes.
 * Exactly one Track Reference Box can be contained within the Track Box.
 * If this box is not present, the track is not referencing any other track in any way. The reference array is sized
 * to fill the reference type box.
 */
public class TrackReferenceBox extends AbstractContainerBox {
    public static final String TYPE = "tref";

    public TrackReferenceBox() {
        super(TYPE);
    }
}
