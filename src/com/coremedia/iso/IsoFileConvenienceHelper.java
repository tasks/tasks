/*
 * Copyright 2012 Sebastian Annies, Hamburg
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
package com.coremedia.iso;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ContainerBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A fine selection of useful methods.
 *
 * @author Andre John Mas
 * @author Sebastian Annies
 * @deprecated please use {@link com.googlecode.mp4parser.util.Path}. I will remove that class before 1.0.
 */
public class IsoFileConvenienceHelper {


    public static Box get(ContainerBox containerBox, String path) {

        String[] parts = path.split("/");
        if (parts.length == 0) {
            return null;
        }

        List<String> partList = new ArrayList<String>(Arrays.asList(parts));

        if ("".equals(partList.get(0))) {
            partList.remove(0);
        }

        if (partList.size() > 0) {
            return get((List<Box>) containerBox.getBoxes(), partList);
        }
        return null;
    }

    private static Box get(List<Box> boxes, List<String> path) {


        String typeInPath = path.remove(0);

        for (Box box : boxes) {
            if (box instanceof ContainerBox) {
                ContainerBox boxContainer = (ContainerBox) box;
                String type = boxContainer.getType();

                if (typeInPath.equals(type)) {
                    List<Box> children = boxContainer.getBoxes();
                    if (path.size() > 0) {
                        if (children.size() > 0) {
                            return get(children, path);
                        }
                    } else {
                        return box;
                    }
                }

            } else {
                String type = box.getType();

                if (path.size() == 0 && typeInPath.equals(type)) {
                    return box;
                }

            }

        }

        return null;
    }
}

