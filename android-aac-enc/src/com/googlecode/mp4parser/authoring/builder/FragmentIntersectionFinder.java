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
package com.googlecode.mp4parser.authoring.builder;

import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;

/**
 *
 */
public interface FragmentIntersectionFinder {
    /**
     * Gets the ordinal number of the samples which will be the first sample
     * in each fragment.
     *
     * @param track concerned track
     * @param movie the context of the track
     * @return an array containing the ordinal of each fragment's first sample
     */
    public long[] sampleNumbers(Track track, Movie movie);
}
