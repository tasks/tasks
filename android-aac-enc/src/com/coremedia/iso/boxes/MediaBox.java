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
 * The media declaration container contains all the objects that declare information about the media data within a
 * track.
 */
public class MediaBox extends AbstractContainerBox {
    public static final String TYPE = "mdia";

    public MediaBox() {
        super(TYPE);
    }

    public MediaInformationBox getMediaInformationBox() {
        for (Box box : boxes) {
            if (box instanceof MediaInformationBox) {
                return (MediaInformationBox) box;
            }
        }
        return null;
    }

    public MediaHeaderBox getMediaHeaderBox() {
        for (Box box : boxes) {
            if (box instanceof MediaHeaderBox) {
                return (MediaHeaderBox) box;
            }
        }
        return null;
    }

    public HandlerBox getHandlerBox() {
        for (Box box : boxes) {
            if (box instanceof HandlerBox) {
                return (HandlerBox) box;
            }
        }
        return null;
    }


}
