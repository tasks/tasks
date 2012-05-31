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

import com.coremedia.iso.BoxParser;
import com.coremedia.iso.IsoFile;
import com.coremedia.iso.IsoTypeWriter;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.CompositionTimeToSample;
import com.coremedia.iso.boxes.ContainerBox;
import com.coremedia.iso.boxes.DataEntryUrlBox;
import com.coremedia.iso.boxes.DataInformationBox;
import com.coremedia.iso.boxes.DataReferenceBox;
import com.coremedia.iso.boxes.FileTypeBox;
import com.coremedia.iso.boxes.HandlerBox;
import com.coremedia.iso.boxes.MediaBox;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.MediaInformationBox;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.MovieHeaderBox;
import com.coremedia.iso.boxes.SampleDependencyTypeBox;
import com.coremedia.iso.boxes.SampleTableBox;
import com.coremedia.iso.boxes.StaticChunkOffsetBox;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.coremedia.iso.boxes.fragment.MovieExtendsBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentHeaderBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentRandomAccessBox;
import com.coremedia.iso.boxes.fragment.MovieFragmentRandomAccessOffsetBox;
import com.coremedia.iso.boxes.fragment.SampleFlags;
import com.coremedia.iso.boxes.fragment.TrackExtendsBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentHeaderBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentRandomAccessBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.googlecode.mp4parser.authoring.DateHelper;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

import static com.googlecode.mp4parser.util.CastUtils.l2i;

/**
 * Creates a fragmented MP4 file.
 */
public class FragmentedMp4Builder implements Mp4Builder {
    FragmentIntersectionFinder intersectionFinder = new SyncSampleIntersectFinderImpl();

    private static final Logger LOG = Logger.getLogger(FragmentedMp4Builder.class.getName());

    public List<String> getAllowedHandlers() {
        return Arrays.asList("soun", "vide");
    }

    public Box createFtyp(Movie movie) {
        List<String> minorBrands = new LinkedList<String>();
        minorBrands.add("isom");
        minorBrands.add("iso2");
        minorBrands.add("avc1");
        return new FileTypeBox("isom", 0, minorBrands);
    }

    protected List<Box> createMoofMdat(final Movie movie) {
        List<Box> boxes = new LinkedList<Box>();
        int maxNumberOfFragments = 0;
        for (Track track : movie.getTracks()) {
            int currentLength = intersectionFinder.sampleNumbers(track, movie).length;
            maxNumberOfFragments = currentLength > maxNumberOfFragments ? currentLength : maxNumberOfFragments;
        }
        int sequence = 1;
        for (int i = 0; i < maxNumberOfFragments; i++) {

            final List<Track> sizeSortedTracks = new LinkedList<Track>(movie.getTracks());
            final int j = i;
            Collections.sort(sizeSortedTracks, new Comparator<Track>() {
                public int compare(Track o1, Track o2) {
                    long[] startSamples1 = intersectionFinder.sampleNumbers(o1, movie);
                    long startSample1 = startSamples1[j];
                    // one based sample numbers - the first sample is 1
                    long endSample1 = j + 1 < startSamples1.length ? startSamples1[j + 1] : o1.getSamples().size() + 1;
                    long[] startSamples2 = intersectionFinder.sampleNumbers(o2, movie);
                    long startSample2 = startSamples2[j];
                    // one based sample numbers - the first sample is 1
                    long endSample2 = j + 1 < startSamples2.length ? startSamples2[j + 1] : o2.getSamples().size() + 1;
                    List<ByteBuffer> samples1 = o1.getSamples().subList(l2i(startSample1) - 1, l2i(endSample1) - 1);
                    List<ByteBuffer> samples2 = o2.getSamples().subList(l2i(startSample2) - 1, l2i(endSample2) - 1);
                    int size1 = 0;
                    for (ByteBuffer byteBuffer : samples1) {
                        size1 += byteBuffer.limit();
                    }
                    int size2 = 0;
                    for (ByteBuffer byteBuffer : samples2) {
                        size2 += byteBuffer.limit();
                    }
                    return size1 - size2;
                }
            });

            for (Track track : sizeSortedTracks) {
                if (getAllowedHandlers().isEmpty() || getAllowedHandlers().contains(track.getHandler())) {
                    long[] startSamples = intersectionFinder.sampleNumbers(track, movie);

                    if (i < startSamples.length) {
                        long startSample = startSamples[i];
                        // one based sample numbers - the first sample is 1
                        long endSample = i + 1 < startSamples.length ? startSamples[i + 1] : track.getSamples().size() + 1;

                        if (startSample == endSample) {
                            // empty fragment
                            // just don't add any boxes.
                        } else {
                            boxes.add(createMoof(startSample, endSample, track, sequence));
                            boxes.add(createMdat(startSample, endSample, track, sequence++));
                        }

                    } else {
                        //obvious this track has not that many fragments
                    }
                }
            }


        }
        return boxes;
    }

    /**
     * {@inheritDoc}
     */
    public IsoFile build(Movie movie) {
        LOG.fine("Creating movie " + movie);
        IsoFile isoFile = new IsoFile();


        isoFile.addBox(createFtyp(movie));
        isoFile.addBox(createMoov(movie));

        for (Box box : createMoofMdat(movie)) {
            isoFile.addBox(box);
        }
        isoFile.addBox(createMfra(movie, isoFile));

        return isoFile;
    }

    protected Box createMdat(final long startSample, final long endSample, final Track track, final int i) {

        class Mdat implements Box {
            ContainerBox parent;

            public ContainerBox getParent() {
                return parent;
            }

            public void setParent(ContainerBox parent) {
                this.parent = parent;
            }

            public long getSize() {
                long size = 8; // I don't expect 2gig fragments
                for (ByteBuffer sample : getSamples(startSample, endSample, track, i)) {
                    size += sample.limit();
                }
                return size;
            }

            public String getType() {
                return "mdat";
            }

            public void getBox(WritableByteChannel writableByteChannel) throws IOException {
                List<ByteBuffer> bbs = getSamples(startSample, endSample, track, i);
                final List<ByteBuffer> samples = ByteBufferHelper.mergeAdjacentBuffers(bbs);
                ByteBuffer header = ByteBuffer.allocate(8);
                IsoTypeWriter.writeUInt32(header, l2i(getSize()));
                header.put(IsoFile.fourCCtoBytes(getType()));
                header.rewind();
                writableByteChannel.write(header);
                if (writableByteChannel instanceof GatheringByteChannel) {

                    int STEPSIZE = 1024;
                    for (int i = 0; i < Math.ceil((double) samples.size() / STEPSIZE); i++) {
                        List<ByteBuffer> sublist = samples.subList(
                                i * STEPSIZE, // start
                                (i + 1) * STEPSIZE < samples.size() ? (i + 1) * STEPSIZE : samples.size()); // end
                        ByteBuffer sampleArray[] = sublist.toArray(new ByteBuffer[sublist.size()]);
                        do {
                            ((GatheringByteChannel) writableByteChannel).write(sampleArray);
                        } while (sampleArray[sampleArray.length - 1].remaining() > 0);
                    }
                    //System.err.println(bytesWritten);
                } else {
                    for (ByteBuffer sample : samples) {
                        sample.rewind();
                        writableByteChannel.write(sample);
                    }
                }

            }

            public void parse(ReadableByteChannel readableByteChannel, ByteBuffer header, long contentSize, BoxParser boxParser) throws IOException {

            }
        }

        return new Mdat();
    }

    protected Box createTfhd(long startSample, long endSample, Track track, int sequenceNumber) {
        TrackFragmentHeaderBox tfhd = new TrackFragmentHeaderBox();
        SampleFlags sf = new SampleFlags();

        tfhd.setDefaultSampleFlags(sf);
        tfhd.setBaseDataOffset(-1);
        tfhd.setTrackId(track.getTrackMetaData().getTrackId());
        return tfhd;
    }

    protected Box createMfhd(long startSample, long endSample, Track track, int sequenceNumber) {
        MovieFragmentHeaderBox mfhd = new MovieFragmentHeaderBox();
        mfhd.setSequenceNumber(sequenceNumber);
        return mfhd;
    }

    protected Box createTraf(long startSample, long endSample, Track track, int sequenceNumber) {
        TrackFragmentBox traf = new TrackFragmentBox();
        traf.addBox(createTfhd(startSample, endSample, track, sequenceNumber));
        for (Box trun : createTruns(startSample, endSample, track, sequenceNumber)) {
            traf.addBox(trun);
        }

        return traf;
    }


    /**
     * @param startSample    first sample in list starting with 1. 1 is the first sample.
     * @param endSample
     * @param track
     * @param sequenceNumber
     * @return
     */
    protected List<ByteBuffer> getSamples(long startSample, long endSample, Track track, int sequenceNumber) {
        // since startSample and endSample are one-based substract 1 before addressing list elements
        return track.getSamples().subList(l2i(startSample) - 1, l2i(endSample) - 1);
    }


    protected List<? extends Box> createTruns(long startSample, long endSample, Track track, int sequenceNumber) {
        List<ByteBuffer> samples = getSamples(startSample, endSample, track, sequenceNumber);

        long[] sampleSizes = new long[samples.size()];
        for (int i = 0; i < sampleSizes.length; i++) {
            sampleSizes[i] = samples.get(i).limit();
        }
        TrackRunBox trun = new TrackRunBox();


        trun.setSampleDurationPresent(true);
        trun.setSampleSizePresent(true);
        List<TrackRunBox.Entry> entries = new ArrayList<TrackRunBox.Entry>(l2i(endSample - startSample));


        Queue<TimeToSampleBox.Entry> timeQueue = new LinkedList<TimeToSampleBox.Entry>(track.getDecodingTimeEntries());
        long left = startSample;
        long curEntryLeft = timeQueue.peek().getCount();
        while (left >= curEntryLeft) {
            left -= curEntryLeft;
            timeQueue.remove();
            curEntryLeft = timeQueue.peek().getCount();
        }
        curEntryLeft -= left;


        Queue<CompositionTimeToSample.Entry> compositionTimeQueue =
                track.getCompositionTimeEntries() != null && track.getCompositionTimeEntries().size() > 0 ?
                        new LinkedList<CompositionTimeToSample.Entry>(track.getCompositionTimeEntries()) : null;
        long compositionTimeEntriesLeft = compositionTimeQueue != null ? compositionTimeQueue.peek().getCount() : -1;


        trun.setSampleCompositionTimeOffsetPresent(compositionTimeEntriesLeft > 0);

        // fast forward composition stuff
        for (long i = 1; i < startSample; i++) {
            if (compositionTimeQueue != null) {
                trun.setSampleCompositionTimeOffsetPresent(true);
                if (--compositionTimeEntriesLeft == 0 && compositionTimeQueue.size() > 1) {
                    compositionTimeQueue.remove();
                    compositionTimeEntriesLeft = compositionTimeQueue.element().getCount();
                }
            }
        }

        boolean sampleFlagsRequired = (track.getSampleDependencies() != null && !track.getSampleDependencies().isEmpty() ||
                track.getSyncSamples() != null && track.getSyncSamples().length != 0);

        trun.setSampleFlagsPresent(sampleFlagsRequired);

        for (int i = 0; i < sampleSizes.length; i++) {
            TrackRunBox.Entry entry = new TrackRunBox.Entry();
            entry.setSampleSize(sampleSizes[i]);
            if (sampleFlagsRequired) {
                //if (false) {
                SampleFlags sflags = new SampleFlags();

                if (track.getSampleDependencies() != null && !track.getSampleDependencies().isEmpty()) {
                    SampleDependencyTypeBox.Entry e = track.getSampleDependencies().get(i);
                    sflags.setSampleDependsOn(e.getSampleDependsOn());
                    sflags.setSampleIsDependedOn(e.getSampleIsDependentOn());
                    sflags.setSampleHasRedundancy(e.getSampleHasRedundancy());
                }
                if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                    // we have to mark non-sync samples!
                    if (Arrays.binarySearch(track.getSyncSamples(), startSample + i) >= 0) {
                        sflags.setSampleIsDifferenceSample(false);
                        sflags.setSampleDependsOn(2);
                    } else {
                        sflags.setSampleIsDifferenceSample(true);
                        sflags.setSampleDependsOn(1);
                    }
                }
                // i don't have sample degradation
                entry.setSampleFlags(sflags);

            }

            entry.setSampleDuration(timeQueue.peek().getDelta());
            if (--curEntryLeft == 0 && timeQueue.size() > 1) {
                timeQueue.remove();
                curEntryLeft = timeQueue.peek().getCount();
            }

            if (compositionTimeQueue != null) {
                trun.setSampleCompositionTimeOffsetPresent(true);
                entry.setSampleCompositionTimeOffset(compositionTimeQueue.peek().getOffset());
                if (--compositionTimeEntriesLeft == 0 && compositionTimeQueue.size() > 1) {
                    compositionTimeQueue.remove();
                    compositionTimeEntriesLeft = compositionTimeQueue.element().getCount();
                }
            }
            entries.add(entry);
        }

        trun.setEntries(entries);

        return Collections.singletonList(trun);
    }

    protected Box createMoof(long startSample, long endSample, Track track, int sequenceNumber) {


        MovieFragmentBox moof = new MovieFragmentBox();
        moof.addBox(createMfhd(startSample, endSample, track, sequenceNumber));
        moof.addBox(createTraf(startSample, endSample, track, sequenceNumber));

        TrackRunBox firstTrun = moof.getTrackRunBoxes().get(0);
        firstTrun.setDataOffset(1); // dummy to make size correct
        firstTrun.setDataOffset((int) (8 + moof.getSize())); // mdat header + moof size

        return moof;
    }

    protected Box createMvhd(Movie movie) {
        MovieHeaderBox mvhd = new MovieHeaderBox();
        mvhd.setVersion(1);
        mvhd.setCreationTime(DateHelper.convert(new Date()));
        mvhd.setModificationTime(DateHelper.convert(new Date()));
        long movieTimeScale = movie.getTimescale();
        long duration = 0;

        for (Track track : movie.getTracks()) {
            long tracksDuration = getDuration(track) * movieTimeScale / track.getTrackMetaData().getTimescale();
            if (tracksDuration > duration) {
                duration = tracksDuration;
            }


        }

        mvhd.setDuration(duration);
        mvhd.setTimescale(movieTimeScale);
        // find the next available trackId
        long nextTrackId = 0;
        for (Track track : movie.getTracks()) {
            nextTrackId = nextTrackId < track.getTrackMetaData().getTrackId() ? track.getTrackMetaData().getTrackId() : nextTrackId;
        }
        mvhd.setNextTrackId(++nextTrackId);
        return mvhd;
    }

    protected Box createMoov(Movie movie) {
        MovieBox movieBox = new MovieBox();

        movieBox.addBox(createMvhd(movie));
        movieBox.addBox(createMvex(movie));

        for (Track track : movie.getTracks()) {
            movieBox.addBox(createTrak(track, movie));
        }
        // metadata here
        return movieBox;

    }

    protected Box createTfra(Track track, IsoFile isoFile) {
        TrackFragmentRandomAccessBox tfra = new TrackFragmentRandomAccessBox();
        tfra.setVersion(1); // use long offsets and times
        List<TrackFragmentRandomAccessBox.Entry> offset2timeEntries = new LinkedList<TrackFragmentRandomAccessBox.Entry>();
        List<Box> boxes = isoFile.getBoxes();
        long offset = 0;
        long duration = 0;
        for (Box box : boxes) {
            if (box instanceof MovieFragmentBox) {
                List<TrackFragmentBox> trafs = ((MovieFragmentBox) box).getBoxes(TrackFragmentBox.class);
                for (int i = 0; i < trafs.size(); i++) {
                    TrackFragmentBox traf = trafs.get(i);
                    if (traf.getTrackFragmentHeaderBox().getTrackId() == track.getTrackMetaData().getTrackId()) {
                        // here we are at the offset required for the current entry.
                        List<TrackRunBox> truns = traf.getBoxes(TrackRunBox.class);
                        for (int j = 0; j < truns.size(); j++) {
                            List<TrackFragmentRandomAccessBox.Entry> offset2timeEntriesThisTrun = new LinkedList<TrackFragmentRandomAccessBox.Entry>();
                            TrackRunBox trun = truns.get(j);
                            for (int k = 0; k < trun.getEntries().size(); k++) {
                                TrackRunBox.Entry trunEntry = trun.getEntries().get(k);
                                SampleFlags sf = null;
                                if (k == 0 && trun.isFirstSampleFlagsPresent()) {
                                    sf = trun.getFirstSampleFlags();
                                } else if (trun.isSampleFlagsPresent()) {
                                    sf = trunEntry.getSampleFlags();
                                } else {
                                    List<MovieExtendsBox> mvexs = isoFile.getMovieBox().getBoxes(MovieExtendsBox.class);
                                    for (MovieExtendsBox mvex : mvexs) {
                                        List<TrackExtendsBox> trexs = mvex.getBoxes(TrackExtendsBox.class);
                                        for (TrackExtendsBox trex : trexs) {
                                            if (trex.getTrackId() == track.getTrackMetaData().getTrackId()) {
                                                sf = trex.getDefaultSampleFlags();
                                            }
                                        }
                                    }

                                }
                                if (sf == null) {
                                    throw new RuntimeException("Could not find any SampleFlags to indicate random access or not");
                                }
                                if (sf.getSampleDependsOn() == 2) {
                                    offset2timeEntriesThisTrun.add(new TrackFragmentRandomAccessBox.Entry(
                                            duration,
                                            offset,
                                            i + 1, j + 1, k + 1));
                                }
                                duration += trunEntry.getSampleDuration();
                            }
                            if (offset2timeEntriesThisTrun.size() == trun.getEntries().size() && trun.getEntries().size() > 0) {
                                // Oooops every sample seems to be random access sample
                                // is this an audio track? I don't care.
                                // I just use the first for trun sample for tfra random access
                                offset2timeEntries.add(offset2timeEntriesThisTrun.get(0));
                            } else {
                                offset2timeEntries.addAll(offset2timeEntriesThisTrun);
                            }
                        }
                    }
                }
            }


            offset += box.getSize();
        }
        tfra.setEntries(offset2timeEntries);
        tfra.setTrackId(track.getTrackMetaData().getTrackId());
        return tfra;
    }

    protected Box createMfra(Movie movie, IsoFile isoFile) {
        MovieFragmentRandomAccessBox mfra = new MovieFragmentRandomAccessBox();
        for (Track track : movie.getTracks()) {
            mfra.addBox(createTfra(track, isoFile));
        }

        MovieFragmentRandomAccessOffsetBox mfro = new MovieFragmentRandomAccessOffsetBox();
        mfra.addBox(mfro);
        mfro.setMfraSize(mfra.getSize());
        return mfra;
    }

    protected Box createTrex(Movie movie, Track track) {
        TrackExtendsBox trex = new TrackExtendsBox();
        trex.setTrackId(track.getTrackMetaData().getTrackId());
        trex.setDefaultSampleDescriptionIndex(1);
        trex.setDefaultSampleDuration(0);
        trex.setDefaultSampleSize(0);
        SampleFlags sf = new SampleFlags();
        if ("soun".equals(track.getHandler())) {
            // as far as I know there is no audio encoding
            // where the sample are not self contained.
            sf.setSampleDependsOn(2);
            sf.setSampleIsDependedOn(2);
        }
        trex.setDefaultSampleFlags(sf);
        return trex;
    }


    protected Box createMvex(Movie movie) {
        MovieExtendsBox mvex = new MovieExtendsBox();

        for (Track track : movie.getTracks()) {
            mvex.addBox(createTrex(movie, track));
        }
        return mvex;
    }

    protected Box createTkhd(Movie movie, Track track) {
        TrackHeaderBox tkhd = new TrackHeaderBox();
        tkhd.setVersion(1);
        int flags = 0;
        if (track.isEnabled()) {
            flags += 1;
        }

        if (track.isInMovie()) {
            flags += 2;
        }

        if (track.isInPreview()) {
            flags += 4;
        }

        if (track.isInPoster()) {
            flags += 8;
        }
        tkhd.setFlags(flags);

        tkhd.setAlternateGroup(track.getTrackMetaData().getGroup());
        tkhd.setCreationTime(DateHelper.convert(track.getTrackMetaData().getCreationTime()));
        // We need to take edit list box into account in trackheader duration
        // but as long as I don't support edit list boxes it is sufficient to
        // just translate media duration to movie timescale
        tkhd.setDuration(getDuration(track) * movie.getTimescale() / track.getTrackMetaData().getTimescale());
        tkhd.setHeight(track.getTrackMetaData().getHeight());
        tkhd.setWidth(track.getTrackMetaData().getWidth());
        tkhd.setLayer(track.getTrackMetaData().getLayer());
        tkhd.setModificationTime(DateHelper.convert(new Date()));
        tkhd.setTrackId(track.getTrackMetaData().getTrackId());
        tkhd.setVolume(track.getTrackMetaData().getVolume());
        return tkhd;
    }

    protected Box createMdhd(Movie movie, Track track) {
        MediaHeaderBox mdhd = new MediaHeaderBox();
        mdhd.setCreationTime(DateHelper.convert(track.getTrackMetaData().getCreationTime()));
        mdhd.setDuration(getDuration(track));
        mdhd.setTimescale(track.getTrackMetaData().getTimescale());
        mdhd.setLanguage(track.getTrackMetaData().getLanguage());
        return mdhd;
    }

    protected Box createStbl(Movie movie, Track track) {
        SampleTableBox stbl = new SampleTableBox();

        stbl.addBox(track.getSampleDescriptionBox());
        stbl.addBox(new TimeToSampleBox());
        //stbl.addBox(new SampleToChunkBox());
        stbl.addBox(new StaticChunkOffsetBox());
        return stbl;
    }

    protected Box createMinf(Track track, Movie movie) {
        MediaInformationBox minf = new MediaInformationBox();
        minf.addBox(track.getMediaHeaderBox());
        minf.addBox(createDinf(movie, track));
        minf.addBox(createStbl(movie, track));
        return minf;
    }

    protected Box createMdiaHdlr(Track track, Movie movie) {
        HandlerBox hdlr = new HandlerBox();
        hdlr.setHandlerType(track.getHandler());
        return hdlr;
    }

    protected Box createMdia(Track track, Movie movie) {
        MediaBox mdia = new MediaBox();
        mdia.addBox(createMdhd(movie, track));


        mdia.addBox(createMdiaHdlr(track, movie));


        mdia.addBox(createMinf(track, movie));
        return mdia;
    }

    protected Box createTrak(Track track, Movie movie) {
        LOG.fine("Creating Track " + track);
        TrackBox trackBox = new TrackBox();
        trackBox.addBox(createTkhd(movie, track));
        trackBox.addBox(createMdia(track, movie));
        return trackBox;
    }

    protected DataInformationBox createDinf(Movie movie, Track track) {
        DataInformationBox dinf = new DataInformationBox();
        DataReferenceBox dref = new DataReferenceBox();
        dinf.addBox(dref);
        DataEntryUrlBox url = new DataEntryUrlBox();
        url.setFlags(1);
        dref.addBox(url);
        return dinf;
    }

    public void setIntersectionFinder(FragmentIntersectionFinder intersectionFinder) {
        this.intersectionFinder = intersectionFinder;
    }

    protected long getDuration(Track track) {
        long duration = 0;
        for (TimeToSampleBox.Entry entry : track.getDecodingTimeEntries()) {
            duration += entry.getCount() * entry.getDelta();
        }
        return duration;
    }


}
