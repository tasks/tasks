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

import java.io.UnsupportedEncodingException;

/**
 * Converts <code>byte[]</code> -> <code>String</code> and vice versa.
 */
public final class Ascii {
  public static byte[] convert(String s) {
    try {
      if (s != null) {
        return s.getBytes("us-ascii");
      } else {
        return null;
      }
    } catch (UnsupportedEncodingException e) {
      throw new Error(e);
    }
  }

  public static String convert(byte[] b) {
    try {
      if (b != null) {
        return new String(b, "us-ascii");
      } else {
        return null;
      }
    } catch (UnsupportedEncodingException e) {
      throw new Error(e);
    }
  }
}