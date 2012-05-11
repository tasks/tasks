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


public class CastUtils {
    /**
     * Casts a long to an int. In many cases I use a long for a UInt32 but this cannot be used to allocate
     * ByteBuffers or arrays since they restricted to <code>Integer.MAX_VALUE</code> this cast-method will throw
     * a RuntimeException if the cast would cause a loss of information.
     *
     * @param l the long value
     * @return the long value as int
     */
    public static int l2i(long l) {
        if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
            throw new RuntimeException("A cast to int has gone wrong. Please contact the mp4parser discussion group (" + l + ")");
        }
        return (int) l;
    }
}
