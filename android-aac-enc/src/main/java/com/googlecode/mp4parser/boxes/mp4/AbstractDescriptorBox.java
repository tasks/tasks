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

package com.googlecode.mp4parser.boxes.mp4;

import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.BaseDescriptor;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.ObjectDescriptorFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ES Descriptor Box.
 */
public class AbstractDescriptorBox extends AbstractFullBox {
    private static Logger log = Logger.getLogger(AbstractDescriptorBox.class.getName());


    public BaseDescriptor descriptor;
    public ByteBuffer data;

    public AbstractDescriptorBox(String type) {
        super(type);
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        data.rewind(); // has been fforwarded by parsing
        byteBuffer.put(data);
    }

    @Override
    protected long getContentSize() {
        return 4 + data.limit();
    }

    public BaseDescriptor getDescriptor() {
        return descriptor;
    }

    public String getDescriptorAsString() {
        return descriptor.toString();
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        data = content.slice();
        content.position(content.position() + content.remaining());
        try {
            data.rewind();
            descriptor = ObjectDescriptorFactory.createFrom(-1, data);
        } catch (IOException e) {
            log.log(Level.WARNING, "Error parsing ObjectDescriptor", e);
            //that's why we copied it ;)
        } catch (IndexOutOfBoundsException e) {
            log.log(Level.WARNING, "Error parsing ObjectDescriptor", e);
            //that's why we copied it ;)
        }

    }

}
