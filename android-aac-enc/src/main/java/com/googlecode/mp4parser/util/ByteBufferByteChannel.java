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
package com.googlecode.mp4parser.util;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * Creates a <code>ReadableByteChannel</code> that is backed by a <code>ByteBuffer</code>.
 */
public class ByteBufferByteChannel implements ByteChannel {
    ByteBuffer byteBuffer;

    public ByteBufferByteChannel(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public int read(ByteBuffer dst) throws IOException {
        byte[] b = dst.array();
        int r = dst.remaining();
        if (byteBuffer.remaining() >= r) {
            byteBuffer.get(b, dst.position(), r);
            return r;
        } else {
            throw new EOFException("Reading beyond end of stream");
        }
    }

    public boolean isOpen() {
        return true;
    }

    public void close() throws IOException {
    }

    public int write(ByteBuffer src) throws IOException {
        int r = src.remaining();
        byteBuffer.put(src);
        return r;
    }
}
