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
package com.googlecode.mp4parser.authoring;

import java.util.Date;

/**
 * Converts ISO Dates (seconds since 1/1/1904) to Date and vice versa.
 */
public class DateHelper {
    /**
     * Converts a long value with seconds since 1/1/1904 to Date.
     *
     * @param secondsSince seconds since 1/1/1904
     * @return date the corresponding <code>Date</code>
     */
    static public Date convert(long secondsSince) {
        return new Date((secondsSince - 2082844800L) * 1000L);
    }


    /**
     * Converts a date as long to a mac date as long
     *
     * @param date date to convert
     * @return date in mac format
     */
    static public long convert(Date date) {
        return (date.getTime() / 1000L) + 2082844800L;
    }
}
