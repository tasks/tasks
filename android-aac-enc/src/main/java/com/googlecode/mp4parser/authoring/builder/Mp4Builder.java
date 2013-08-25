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

import com.coremedia.iso.IsoFile;
import com.googlecode.mp4parser.authoring.Movie;

import java.io.IOException;

/**
 * Transforms a <code>Movie</code> object to an IsoFile. Implementations can
 * determine the specific format: Fragmented MP4, MP4, MP4 with Apple Metadata,
 * MP4 with 3GPP Metadata, MOV.
 */
public interface Mp4Builder {
    /**
     * Builds the actual IsoFile from the Movie.
     *
     * @param movie data source
     * @return the freshly built IsoFile
     */
    public IsoFile build(Movie movie);

}
