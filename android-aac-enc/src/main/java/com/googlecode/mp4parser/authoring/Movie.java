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

import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class Movie {
    List<Track> tracks = new LinkedList<Track>();

    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }

    public void addTrack(Track nuTrack) {
        // do some checking
        // perhaps the movie needs to get longer!
        if (getTrackByTrackId(nuTrack.getTrackMetaData().getTrackId()) != null) {
            // We already have a track with that trackId. Create a new one
            nuTrack.getTrackMetaData().setTrackId(getNextTrackId());
        }
        tracks.add(nuTrack);
    }


    @Override
    public String toString() {
        String s = "Movie{ ";
        for (Track track : tracks) {
            s += "track_" + track.getTrackMetaData().getTrackId() + " (" + track.getHandler() + ") ";
        }

        s += '}';
        return s;
    }

    public long getNextTrackId() {
        long nextTrackId = 0;
        for (Track track : tracks) {
            nextTrackId = nextTrackId < track.getTrackMetaData().getTrackId() ? track.getTrackMetaData().getTrackId() : nextTrackId;
        }
        return ++nextTrackId;
    }


    public Track getTrackByTrackId(long trackId) {
        for (Track track : tracks) {
            if (track.getTrackMetaData().getTrackId() == trackId) {
                return track;
            }
        }
        return null;
    }


    public long getTimescale() {
        long timescale = this.getTracks().iterator().next().getTrackMetaData().getTimescale();
        for (Track track : this.getTracks()) {
            timescale = gcd(track.getTrackMetaData().getTimescale(), timescale);
        }
        return timescale;
    }

    public static long gcd(long a, long b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b);
    }

}
