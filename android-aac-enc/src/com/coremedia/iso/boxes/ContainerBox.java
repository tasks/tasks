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

import com.coremedia.iso.IsoFile;

import java.util.List;

/**
 * Interface for all ISO boxes that may contain other boxes.
 */
public interface ContainerBox extends Box {

    /**
     * Gets all child boxes. May not return <code>null</code>.
     *
     * @return an array of boxes, empty array in case of no children.
     */
    List<Box> getBoxes();

    /**
     * Sets all boxes and removes all previous child boxes.
     * @param boxes the new list of children
     */
    void setBoxes(List<Box> boxes);

    /**
     * Gets all child boxes of the given type. May not return <code>null</code>.
     *
     * @param clazz child box's type
     * @return an array of boxes, empty array in case of no children.
     */
    <T extends Box> List<T> getBoxes(Class<T> clazz);

    /**
     * Gets all child boxes of the given type. May not return <code>null</code>.
     *
     * @param clazz     child box's type
     * @param recursive step down the tree
     * @return an array of boxes, empty array in case of no children.
     */
    <T extends Box> List<T> getBoxes(Class<T> clazz, boolean recursive);

    /**
     * Gets the parent box. May be <code>null</code> in case of the
     * {@link com.coremedia.iso.IsoFile} itself.
     *
     * @return a <code>ContainerBox</code> that contains <code>this</code>
     */
    ContainerBox getParent();

    /**
     * Returns the number of bytes from the start of the box to start of the first child.
     *
     * @return offset of first child from box start
     */
    long getNumOfBytesToFirstChild();

    IsoFile getIsoFile();
}
