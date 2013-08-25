/*
 * Copyright 2011 castLabs, Berlin
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

package com.googlecode.mp4parser.boxes.mp4.objectdescriptors;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class UnknownDescriptor extends BaseDescriptor {
    private ByteBuffer data;
    private static Logger log = Logger.getLogger(UnknownDescriptor.class.getName());

    @Override
    public void parseDetail(ByteBuffer bb) throws IOException {
        data = (ByteBuffer) bb.slice().limit(this.getSizeOfInstance());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("UnknownDescriptor");
        sb.append("{tag=").append(tag);
        sb.append(", sizeOfInstance=").append(sizeOfInstance);
        sb.append(", data=").append(data);
        sb.append('}');
        return sb.toString();
    }
}
