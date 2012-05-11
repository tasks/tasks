package com.googlecode.mp4parser.authoring.tracks;

import com.coremedia.iso.boxes.AbstractMediaHeaderBox;
import com.coremedia.iso.boxes.CompositionTimeToSample;
import com.coremedia.iso.boxes.SampleDependencyTypeBox;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.SubSampleInformationBox;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.coremedia.iso.boxes.VideoMediaHeaderBox;
import com.coremedia.iso.boxes.h264.AvcConfigurationBox;
import com.coremedia.iso.boxes.sampleentry.VisualSampleEntry;
import com.googlecode.mp4parser.authoring.AbstractTrack;
import com.googlecode.mp4parser.authoring.TrackMetaData;
import com.googlecode.mp4parser.h264.model.PictureParameterSet;
import com.googlecode.mp4parser.h264.model.SeqParameterSet;
import com.googlecode.mp4parser.h264.read.CAVLCReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * The <code>H264TrackImpl</code> creates a <code>Track</code> from an H.264
 * Annex B file.
 */
public class H264TrackImpl extends AbstractTrack {
    TrackMetaData trackMetaData = new TrackMetaData();
    SampleDescriptionBox sampleDescriptionBox;

    private ReaderWrapper reader;
    private List<ByteBuffer> samples;
    boolean readSamples = false;

    List<TimeToSampleBox.Entry> stts;
    List<CompositionTimeToSample.Entry> ctts;
    List<SampleDependencyTypeBox.Entry> sdtp;
    List<Integer> stss;

    SeqParameterSet seqParameterSet = null;
    PictureParameterSet pictureParameterSet = null;
    LinkedList<byte[]> seqParameterSetList = new LinkedList<byte[]>();
    LinkedList<byte[]> pictureParameterSetList = new LinkedList<byte[]>();

    private int width;
    private int height;
    private int timescale;
    private int frametick;

    private SEIMessage seiMessage;
    int frameNrInGop = 0;

    public H264TrackImpl(InputStream inputStream) throws IOException {
        this.reader = new ReaderWrapper(inputStream);
        stts = new LinkedList<TimeToSampleBox.Entry>();
        ctts = new LinkedList<CompositionTimeToSample.Entry>();
        sdtp = new LinkedList<SampleDependencyTypeBox.Entry>();
        stss = new LinkedList<Integer>();

        samples = new LinkedList<ByteBuffer>();
        if (!readSamples()) {
            throw new IOException();
        }

        if (!readVariables()) {
            throw new IOException();
        }

        sampleDescriptionBox = new SampleDescriptionBox();
        VisualSampleEntry visualSampleEntry = new VisualSampleEntry("avc1");
        visualSampleEntry.setDataReferenceIndex(1);
        visualSampleEntry.setDepth(24);
        visualSampleEntry.setFrameCount(1);
        visualSampleEntry.setHorizresolution(72);
        visualSampleEntry.setVertresolution(72);
        visualSampleEntry.setWidth(width);
        visualSampleEntry.setHeight(height);
        visualSampleEntry.setCompressorname("AVC Coding");

        AvcConfigurationBox avcConfigurationBox = new AvcConfigurationBox();

        avcConfigurationBox.setSequenceParameterSets(seqParameterSetList);
        avcConfigurationBox.setPictureParameterSets(pictureParameterSetList);
        avcConfigurationBox.setAvcLevelIndication(seqParameterSet.level_idc);
        avcConfigurationBox.setAvcProfileIndicaation(seqParameterSet.profile_idc);
        avcConfigurationBox.setBitDepthLumaMinus8(seqParameterSet.bit_depth_luma_minus8);
        avcConfigurationBox.setBitDepthChromaMinus8(seqParameterSet.bit_depth_chroma_minus8);
        avcConfigurationBox.setChromaFormat(seqParameterSet.chroma_format_idc.getId());
        avcConfigurationBox.setConfigurationVersion(1);
        avcConfigurationBox.setLengthSizeMinusOne(3);
        avcConfigurationBox.setProfileCompatibility(seqParameterSetList.get(0)[1]);

        visualSampleEntry.addBox(avcConfigurationBox);
        sampleDescriptionBox.addBox(visualSampleEntry);

        trackMetaData.setCreationTime(new Date());
        trackMetaData.setModificationTime(new Date());
        trackMetaData.setLanguage("eng");
        trackMetaData.setTimescale(timescale); // Audio tracks always use samplerate as timescale
        trackMetaData.setWidth(width);
        trackMetaData.setHeight(height);
    }

    public SampleDescriptionBox getSampleDescriptionBox() {
        return sampleDescriptionBox;
    }

    public List<TimeToSampleBox.Entry> getDecodingTimeEntries() {
        return stts;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<CompositionTimeToSample.Entry> getCompositionTimeEntries() {
        return ctts;
    }

    public long[] getSyncSamples() {
        long[] returns = new long[stss.size()];
        for (int i = 0; i < stss.size(); i++) {
            returns[i] = stss.get(i);
        }
        return returns;
    }

    public List<SampleDependencyTypeBox.Entry> getSampleDependencies() {
        return sdtp;
    }

    public TrackMetaData getTrackMetaData() {
        return trackMetaData;
    }

    public String getHandler() {
        return "vide";
    }

    public List<ByteBuffer> getSamples() {
        return samples;
    }

    public AbstractMediaHeaderBox getMediaHeaderBox() {
        return new VideoMediaHeaderBox();
    }

    public SubSampleInformationBox getSubsampleInformationBox() {
        return null;
    }

    private boolean readVariables() {
        width = (seqParameterSet.pic_width_in_mbs_minus1 + 1) * 16;
        int mult = 2;
        if (seqParameterSet.frame_mbs_only_flag) {
            mult = 1;
        }
        height = 16 * (seqParameterSet.pic_height_in_map_units_minus1 + 1) * mult;
        if (seqParameterSet.frame_cropping_flag) {
            int chromaArrayType = 0;
            if (seqParameterSet.residual_color_transform_flag == false) {
                chromaArrayType = seqParameterSet.chroma_format_idc.getId();
            }
            int cropUnitX = 1;
            int cropUnitY = mult;
            if (chromaArrayType != 0) {
                cropUnitX = seqParameterSet.chroma_format_idc.getSubWidth();
                cropUnitY = seqParameterSet.chroma_format_idc.getSubHeight() * mult;
            }

            width -= cropUnitX * (seqParameterSet.frame_crop_left_offset + seqParameterSet.frame_crop_right_offset);
            height -= cropUnitY * (seqParameterSet.frame_crop_top_offset + seqParameterSet.frame_crop_bottom_offset);
        }
        return true;
    }

    private boolean findNextStartcode() throws IOException {
        byte[] test = new byte[4];
        while (4 == reader.read(test)) {
            if (test[0] == 0 && test[1] == 0 && test[2] == 0 && test[3] == 1) {
                return true;
            }
            reader.rewind(3);
        }
        return false;
    }

    private enum NALActions {
        IGNORE, BUFFER, STORE, END
    }

    private boolean readSamples() throws IOException {
        if (readSamples) {
            return true;
        }

        readSamples = true;

        long pos = reader.getPos();
        findNextStartcode();

        ArrayList<byte[]> buffered = new ArrayList<byte[]>();

        int frameNr = 0;

        while (findNextStartcode()) {
            long newpos = reader.getPos() - 4;
            int size = (int) (newpos - pos);
            reader.rewind(size);
            byte[] data = new byte[size - 4];
            reader.read(data);
            int type = data[0];
            int nal_ref_idc = (type >> 5) & 3;
            int nal_unit_type = type & 0x1f;
            System.out.println("Found startcode at " + pos + " Type: " + nal_unit_type + " ref idc: " + nal_ref_idc + " (size " + size + ")");
            NALActions action = handleNALUnit(nal_ref_idc, nal_unit_type, data);
            switch (action) {
                case IGNORE:
                    break;

                case BUFFER:
                    buffered.add(data);
                    break;

                case STORE:
                    int stdpValue = 22;
                    frameNr++;
                    buffered.add(data);
                    ByteBuffer bb = createSample(buffered);
                    boolean IdrPicFlag = false;
                    if (nal_unit_type == 5) {
                        stdpValue += 16;
                        IdrPicFlag = true;
                    }
                    ByteArrayInputStream bs = cleanBuffer(buffered.get(buffered.size() - 1));
                    SliceHeader sh = new SliceHeader(bs, seqParameterSet, pictureParameterSet, IdrPicFlag);
                    if (sh.slice_type == SliceHeader.SliceType.B) {
                        stdpValue += 4;
                    }
                    System.out.println("Adding sample with size " + bb.capacity() + " and header " + sh);
                    buffered.clear();
                    samples.add(bb);
                    stts.add(new TimeToSampleBox.Entry(1, frametick));
                    if (nal_unit_type == 5) { // IDR Picture
                        stss.add(frameNr);
                    }
                    if (seiMessage.n_frames == 0) {
                        frameNrInGop = 0;
                    }
                    int offset = 0;
                    if (seiMessage.clock_timestamp_flag) {
                        offset = seiMessage.n_frames - frameNrInGop;
                    } else if (seiMessage.removal_delay_flag) {
                        offset = seiMessage.dpb_removal_delay / 2;
                    }
                    ctts.add(new CompositionTimeToSample.Entry(1, offset * frametick));
                    sdtp.add(new SampleDependencyTypeBox.Entry(stdpValue));
                    frameNrInGop++;
                    break;

                case END:
                    return true;


            }
            pos = newpos;
            reader.seek(4);
        }
        return true;
    }

    private ByteBuffer createSample(List<byte[]> buffers) {
        int outsize = 0;
        for (int i = 0; i < buffers.size(); i++) {
            outsize += buffers.get(i).length + 4;
        }
        byte[] output = new byte[outsize];

        ByteBuffer bb = ByteBuffer.wrap(output);
        for (int i = 0; i < buffers.size(); i++) {
            bb.putInt(buffers.get(i).length);
            bb.put(buffers.get(i));
        }
        bb.rewind();
        return bb;
    }

    private ByteArrayInputStream cleanBuffer(byte[] data) {
        byte[] output = new byte[data.length];
        int inPos = 0;
        int outPos = 0;
        while (inPos < data.length) {
            if (data[inPos] == 0 && data[inPos + 1] == 0 && data[inPos + 2] == 3) {
                output[outPos] = 0;
                output[outPos + 1] = 0;
                inPos += 3;
                outPos += 2;
            } else {
                output[outPos] = data[inPos];
                inPos++;
                outPos++;
            }
        }
        return new ByteArrayInputStream(output, 0, outPos);
    }

    private NALActions handleNALUnit(int nal_ref_idc, int nal_unit_type, byte[] data) throws IOException {
        NALActions action;
        switch (nal_unit_type) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                action = NALActions.STORE; // Will only work in single slice per frame mode!
                break;

            case 6:
                seiMessage = new SEIMessage(cleanBuffer(data), seqParameterSet);
                action = NALActions.BUFFER;
                break;

            case 9:
//                printAccessUnitDelimiter(data);
                action = NALActions.BUFFER;
                break;


            case 7:
                if (seqParameterSet == null) {
                    ByteArrayInputStream is = new ByteArrayInputStream(data);
                    is.read();
                    seqParameterSet = SeqParameterSet.read(is);
                    seqParameterSetList.add(data);
                    if (seqParameterSet.vuiParams != null) {
                        timescale = seqParameterSet.vuiParams.time_scale >> 1; // Not sure why, but I found this in several places, and it works...
                        frametick = seqParameterSet.vuiParams.num_units_in_tick;
                    } else {
                        System.err.println("Warning: Can't determine frame rate. Guessing 25 fps");
                        timescale = 90000;
                        frametick = 3600;
                    }
                }
                action = NALActions.IGNORE;
                break;

            case 8:
                if (pictureParameterSet == null) {
                    ByteArrayInputStream is = new ByteArrayInputStream(data);
                    is.read();
                    pictureParameterSet = PictureParameterSet.read(is);
                    pictureParameterSetList.add(data);
                }
                action = NALActions.IGNORE;
                break;

            case 10:
            case 11:
                action = NALActions.END;
                break;

            default:
                System.err.println("Unknown NAL unit type: " + nal_unit_type);
                action = NALActions.IGNORE;

        }

        return action;
    }

    public void printAccessUnitDelimiter(byte[] data) {
        System.out.println("Access unit delimiter: " + (data[0] >> 5));
    }

    public static class SliceHeader {

        public enum SliceType {
            P, B, I, SP, SI
        }

        public int first_mb_in_slice;
        public SliceType slice_type;
        public int pic_parameter_set_id;
        public int colour_plane_id;
        public int frame_num;
        public boolean field_pic_flag = false;
        public boolean bottom_field_flag = false;
        public int idr_pic_id;
        public int pic_order_cnt_lsb;
        public int delta_pic_order_cnt_bottom;

        public SliceHeader(InputStream is, SeqParameterSet sps, PictureParameterSet pps, boolean IdrPicFlag) throws IOException {
            is.read();
            CAVLCReader reader = new CAVLCReader(is);
            first_mb_in_slice = reader.readUE("SliceHeader: first_mb_in_slice");
            switch (reader.readUE("SliceHeader: slice_type")) {
                case 0:
                case 5:
                    slice_type = SliceType.P;
                    break;

                case 1:
                case 6:
                    slice_type = SliceType.B;
                    break;

                case 2:
                case 7:
                    slice_type = SliceType.I;
                    break;

                case 3:
                case 8:
                    slice_type = SliceType.SP;
                    break;

                case 4:
                case 9:
                    slice_type = SliceType.SI;
                    break;

            }
            pic_parameter_set_id = reader.readUE("SliceHeader: pic_parameter_set_id");
            if (sps.residual_color_transform_flag) {
                colour_plane_id = reader.readU(2, "SliceHeader: colour_plane_id");
            }
            frame_num = reader.readU(sps.log2_max_frame_num_minus4 + 4, "SliceHeader: frame_num");

            if (!sps.frame_mbs_only_flag) {
                field_pic_flag = reader.readBool("SliceHeader: field_pic_flag");
                if (field_pic_flag) {
                    bottom_field_flag = reader.readBool("SliceHeader: bottom_field_flag");
                }
            }
            if (IdrPicFlag) {
                idr_pic_id = reader.readUE("SliceHeader: idr_pic_id");
                if (sps.pic_order_cnt_type == 0) {
                    pic_order_cnt_lsb = reader.readU(sps.log2_max_pic_order_cnt_lsb_minus4 + 4, "SliceHeader: pic_order_cnt_lsb");
                    if (pps.pic_order_present_flag && !field_pic_flag) {
                        delta_pic_order_cnt_bottom = reader.readSE("SliceHeader: delta_pic_order_cnt_bottom");
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "SliceHeader{" +
                    "first_mb_in_slice=" + first_mb_in_slice +
                    ", slice_type=" + slice_type +
                    ", pic_parameter_set_id=" + pic_parameter_set_id +
                    ", colour_plane_id=" + colour_plane_id +
                    ", frame_num=" + frame_num +
                    ", field_pic_flag=" + field_pic_flag +
                    ", bottom_field_flag=" + bottom_field_flag +
                    ", idr_pic_id=" + idr_pic_id +
                    ", pic_order_cnt_lsb=" + pic_order_cnt_lsb +
                    ", delta_pic_order_cnt_bottom=" + delta_pic_order_cnt_bottom +
                    '}';
        }
    }

    private class ReaderWrapper {
        private InputStream inputStream;
        private long pos = 0;

        private ReaderWrapper(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        int read() throws IOException {
            pos++;
            return inputStream.read();
        }

        long read(byte[] data) throws IOException {
            long read = inputStream.read(data);
            pos += read;
            return read;
        }

        long rewind(int dist) throws IOException {
            long skipped = inputStream.skip(-dist);
            pos += skipped;
            return -skipped;
        }

        long seek(int dist) throws IOException {
            long seeked = inputStream.skip(dist);
            pos += seeked;
            return seeked;
        }

        public long getPos() {
            return pos;
        }
    }

    public class SEIMessage {

        int payloadType = 0;
        int payloadSize = 0;

        boolean removal_delay_flag;
        int cpb_removal_delay;
        int dpb_removal_delay;

        boolean clock_timestamp_flag;
        int pic_struct;
        int ct_type;
        int nuit_field_based_flag;
        int counting_type;
        int full_timestamp_flag;
        int discontinuity_flag;
        int cnt_dropped_flag;
        int n_frames;
        int seconds_value;
        int minutes_value;
        int hours_value;
        int time_offset_length;
        int time_offset;

        SeqParameterSet sps;

        public SEIMessage(InputStream is, SeqParameterSet sps) throws IOException {
            this.sps = sps;
            is.read();
            int datasize = is.available();
            int read = 0;
            while (read < datasize) {
                payloadType = 0;
                payloadSize = 0;
                int last_payload_type_bytes = is.read();
                read++;
                while (last_payload_type_bytes == 0xff) {
                    payloadType += last_payload_type_bytes;
                    last_payload_type_bytes = is.read();
                    read++;
                }
                payloadType += last_payload_type_bytes;
                int last_payload_size_bytes = is.read();
                read++;

                while (last_payload_size_bytes == 0xff) {
                    payloadSize += last_payload_size_bytes;
                    last_payload_size_bytes = is.read();
                    read++;
                }
                payloadSize += last_payload_size_bytes;
                if (datasize - read >= payloadSize) {
                    if (payloadType == 1) { // pic_timing is what we are interested in!
                        if (sps.vuiParams != null && (sps.vuiParams.nalHRDParams != null || sps.vuiParams.vclHRDParams != null || sps.vuiParams.pic_struct_present_flag)) {
                            byte[] data = new byte[payloadSize];
                            is.read(data);
                            read += payloadSize;
                            CAVLCReader reader = new CAVLCReader(new ByteArrayInputStream(data));
                            if (sps.vuiParams.nalHRDParams != null || sps.vuiParams.vclHRDParams != null) {
                                removal_delay_flag = true;
                                cpb_removal_delay = reader.readU(sps.vuiParams.nalHRDParams.cpb_removal_delay_length_minus1 + 1, "SEI: cpb_removal_delay");
                                dpb_removal_delay = reader.readU(sps.vuiParams.nalHRDParams.dpb_output_delay_length_minus1 + 1, "SEI: dpb_removal_delay");
                            } else {
                                removal_delay_flag = false;
                            }
                            if (sps.vuiParams.pic_struct_present_flag) {
                                pic_struct = reader.readU(4, "SEI: pic_struct");
                                int numClockTS;
                                switch (pic_struct) {
                                    case 0:
                                    case 1:
                                    case 2:
                                    default:
                                        numClockTS = 1;
                                        break;

                                    case 3:
                                    case 4:
                                    case 7:
                                        numClockTS = 2;
                                        break;

                                    case 5:
                                    case 6:
                                    case 8:
                                        numClockTS = 3;
                                        break;
                                }
                                for (int i = 0; i < numClockTS; i++) {
                                    clock_timestamp_flag = reader.readBool("pic_timing SEI: clock_timestamp_flag[" + i + "]");
                                    if (clock_timestamp_flag) {
                                        ct_type = reader.readU(2, "pic_timing SEI: ct_type");
                                        nuit_field_based_flag = reader.readU(1, "pic_timing SEI: nuit_field_based_flag");
                                        counting_type = reader.readU(5, "pic_timing SEI: counting_type");
                                        full_timestamp_flag = reader.readU(1, "pic_timing SEI: full_timestamp_flag");
                                        discontinuity_flag = reader.readU(1, "pic_timing SEI: discontinuity_flag");
                                        cnt_dropped_flag = reader.readU(1, "pic_timing SEI: cnt_dropped_flag");
                                        n_frames = reader.readU(8, "pic_timing SEI: n_frames");
                                        if (full_timestamp_flag == 1) {
                                            seconds_value = reader.readU(6, "pic_timing SEI: seconds_value");
                                            minutes_value = reader.readU(6, "pic_timing SEI: minutes_value");
                                            hours_value = reader.readU(5, "pic_timing SEI: hours_value");
                                        } else {
                                            if (reader.readBool("pic_timing SEI: seconds_flag")) {
                                                seconds_value = reader.readU(6, "pic_timing SEI: seconds_value");
                                                if (reader.readBool("pic_timing SEI: minutes_flag")) {
                                                    minutes_value = reader.readU(6, "pic_timing SEI: minutes_value");
                                                    if (reader.readBool("pic_timing SEI: hours_flag")) {
                                                        hours_value = reader.readU(5, "pic_timing SEI: hours_value");
                                                    }
                                                }
                                            }
                                        }
                                        if (true) {
                                            if (sps.vuiParams.nalHRDParams != null) {
                                                time_offset_length = sps.vuiParams.nalHRDParams.time_offset_length;
                                            } else if (sps.vuiParams.vclHRDParams != null) {
                                                time_offset_length = sps.vuiParams.vclHRDParams.time_offset_length;
                                            } else {
                                                time_offset_length = 24;
                                            }
                                            time_offset = reader.readU(24, "pic_timing SEI: time_offset");
                                        }
                                    }
                                }
                            }

                        } else {
                            for (int i = 0; i < payloadSize; i++) {
                                is.read();
                                read++;
                            }
                        }
                    } else {
                        for (int i = 0; i < payloadSize; i++) {
                            is.read();
                            read++;
                        }
                    }
                } else {
                    read = datasize;
                }
                System.out.println(this);
            }
        }

        @Override
        public String toString() {
            String out = "SEIMessage{" +
                    "payloadType=" + payloadType +
                    ", payloadSize=" + payloadSize;
            if (payloadType == 1) {
                if (sps.vuiParams.nalHRDParams != null || sps.vuiParams.vclHRDParams != null) {

                    out += ", cpb_removal_delay=" + cpb_removal_delay +
                            ", dpb_removal_delay=" + dpb_removal_delay;
                }
                if (sps.vuiParams.pic_struct_present_flag) {
                    out += ", pic_struct=" + pic_struct;
                    if (clock_timestamp_flag) {
                        out += ", ct_type=" + ct_type +
                                ", nuit_field_based_flag=" + nuit_field_based_flag +
                                ", counting_type=" + counting_type +
                                ", full_timestamp_flag=" + full_timestamp_flag +
                                ", discontinuity_flag=" + discontinuity_flag +
                                ", cnt_dropped_flag=" + cnt_dropped_flag +
                                ", n_frames=" + n_frames +
                                ", seconds_value=" + seconds_value +
                                ", minutes_value=" + minutes_value +
                                ", hours_value=" + hours_value +
                                ", time_offset_length=" + time_offset_length +
                                ", time_offset=" + time_offset;
                    }
                }
            }
            out += '}';
            return out;
        }
    }
}
