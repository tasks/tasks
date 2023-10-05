package org.tasks.filters

import com.todoroo.astrid.api.Filter
import org.tasks.data.TagData
import java.util.*

/*
 * The Alphanum Algorithm is an improved sorting algorithm for strings
 * containing numbers.  Instead of sorting numbers in ASCII order like
 * a standard sort, this algorithm sorts numbers in numeric order.
 *
 * The Alphanum Algorithm is discussed at http://www.DaveKoelle.com
 *
 * Released under the MIT License - https://opensource.org/licenses/MIT
 *
 * Copyright 2007-2017 David Koelle
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 */ /**
 * This is an updated version with enhancements made by Daniel Migowski, Andre Bogus, and David
 * Koelle. Updated by David Koelle in 2017.
 *
 *
 * To use this class: Use the static "sort" method from the java.util.Collections class:
 * Collections.sort(your list, new AlphanumComparator());
 */
class AlphanumComparator<T>(private val getTitle: (T) -> String?) : Comparator<T> {
    private fun isDigit(ch: Char): Boolean = ch.code in 48..57

    /** Length of string is passed in for improved efficiency (only need to calculate it once) *  */
    private fun getChunk(s: String, slength: Int, marker: Int): String {
        var marker = marker
        val chunk = StringBuilder()
        var c = s[marker]
        chunk.append(c)
        marker++
        if (isDigit(c)) {
            while (marker < slength) {
                c = s[marker]
                if (!isDigit(c)) break
                chunk.append(c)
                marker++
            }
        } else {
            while (marker < slength) {
                c = s[marker]
                if (isDigit(c)) break
                chunk.append(c)
                marker++
            }
        }
        return chunk.toString()
    }

    override fun compare(t1: T, t2: T): Int {
        val s1 = getTitle(t1)
        val s2 = getTitle(t2)
        if (s1 == null || s2 == null) {
            return 0
        }
        var thisMarker = 0
        var thatMarker = 0
        val s1Length = s1.length
        val s2Length = s2.length
        while (thisMarker < s1Length && thatMarker < s2Length) {
            val thisChunk = getChunk(s1, s1Length, thisMarker)
            thisMarker += thisChunk.length
            val thatChunk = getChunk(s2, s2Length, thatMarker)
            thatMarker += thatChunk.length

            // If both chunks contain numeric characters, sort them numerically
            var result: Int
            if (isDigit(thisChunk[0]) && isDigit(thatChunk[0])) {
                // Simple chunk comparison by length.
                val thisChunkLength = thisChunk.length
                result = thisChunkLength - thatChunk.length
                // If equal, the first different number counts
                if (result == 0) {
                    for (i in 0 until thisChunkLength) {
                        result = thisChunk[i] - thatChunk[i]
                        if (result != 0) {
                            return result
                        }
                    }
                }
            } else {
                result = java.lang.String.CASE_INSENSITIVE_ORDER.compare(thisChunk, thatChunk)
            }
            if (result != 0) return result
        }
        return s1Length - s2Length
    }

    companion object {
        @JvmField
        val FILTER = AlphanumComparator(Filter::title)
        val TAGDATA = AlphanumComparator(TagData::name)
    }
}