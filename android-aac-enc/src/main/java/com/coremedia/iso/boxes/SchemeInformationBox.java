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
 * The Scheme Information Box is a container box that is only interpreted by the scheme beeing used.
 * Any information the encryption system needs is stored here. The content of this box is a series of
 * boxexes whose type annd format are defined by the scheme declared in the {@link com.coremedia.iso.boxes.SchemeTypeBox}.
 */
public class SchemeInformationBox extends AbstractContainerBox {
    public static final String TYPE = "schi";

    public SchemeInformationBox() {
        super(TYPE);
    }

}
