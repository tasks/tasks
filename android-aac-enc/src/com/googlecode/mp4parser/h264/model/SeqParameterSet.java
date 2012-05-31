/*
Copyright (c) 2011 Stanislav Vitvitskiy

Permission is hereby granted, free of charge, to any person obtaining a copy of this
software and associated documentation files (the "Software"), to deal in the Software
without restriction, including without limitation the rights to use, copy, modify,
merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
OR OTHER DEALINGS IN THE SOFTWARE.
*/
package com.googlecode.mp4parser.h264.model;

import com.googlecode.mp4parser.h264.read.CAVLCReader;
import com.googlecode.mp4parser.h264.write.CAVLCWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Sequence Parameter Set structure of h264 bitstream
 * <p/>
 * capable to serialize and deserialize with CAVLC bitstream
 *
 * @author Stanislav Vitvitskiy
 */
public class SeqParameterSet extends BitstreamElement {
    public int pic_order_cnt_type;
    public boolean field_pic_flag;
    public boolean delta_pic_order_always_zero_flag;
    public boolean weighted_pred_flag;
    public int weighted_bipred_idc;
    public boolean entropy_coding_mode_flag;
    public boolean mb_adaptive_frame_field_flag;
    public boolean direct_8x8_inference_flag;
    public ChromaFormat chroma_format_idc;
    public int log2_max_frame_num_minus4;
    public int log2_max_pic_order_cnt_lsb_minus4;
    public int pic_height_in_map_units_minus1;
    public int pic_width_in_mbs_minus1;
    public int bit_depth_luma_minus8;
    public int bit_depth_chroma_minus8;
    public boolean qpprime_y_zero_transform_bypass_flag;
    public int profile_idc;
    public boolean constraint_set_0_flag;
    public boolean constraint_set_1_flag;
    public boolean constraint_set_2_flag;
    public boolean constraint_set_3_flag;
    public int level_idc;
    public int seq_parameter_set_id;
    public boolean residual_color_transform_flag;
    public int offset_for_non_ref_pic;
    public int offset_for_top_to_bottom_field;
    public int num_ref_frames;
    public boolean gaps_in_frame_num_value_allowed_flag;
    public boolean frame_mbs_only_flag;
    public boolean frame_cropping_flag;
    public int frame_crop_left_offset;
    public int frame_crop_right_offset;
    public int frame_crop_top_offset;
    public int frame_crop_bottom_offset;
    public int[] offsetForRefFrame;
    public VUIParameters vuiParams;
    public ScalingMatrix scalingMatrix;
    public int num_ref_frames_in_pic_order_cnt_cycle;

    public static SeqParameterSet read(InputStream is) throws IOException {
        CAVLCReader reader = new CAVLCReader(is);
        SeqParameterSet sps = new SeqParameterSet();

        sps.profile_idc = (int) reader.readNBit(8, "SPS: profile_idc");
        sps.constraint_set_0_flag = reader
                .readBool("SPS: constraint_set_0_flag");
        sps.constraint_set_1_flag = reader
                .readBool("SPS: constraint_set_1_flag");
        sps.constraint_set_2_flag = reader
                .readBool("SPS: constraint_set_2_flag");
        sps.constraint_set_3_flag = reader
                .readBool("SPS: constraint_set_3_flag");
        reader.readNBit(4, "SPS: reserved_zero_4bits");
        sps.level_idc = (int) reader.readNBit(8, "SPS: level_idc");
        sps.seq_parameter_set_id = reader.readUE("SPS: seq_parameter_set_id");

        if (sps.profile_idc == 100 || sps.profile_idc == 110
                || sps.profile_idc == 122 || sps.profile_idc == 144) {
            sps.chroma_format_idc = ChromaFormat.fromId(reader
                    .readUE("SPS: chroma_format_idc"));
            if (sps.chroma_format_idc == ChromaFormat.YUV_444) {
                sps.residual_color_transform_flag = reader
                        .readBool("SPS: residual_color_transform_flag");
            }
            sps.bit_depth_luma_minus8 = reader
                    .readUE("SPS: bit_depth_luma_minus8");
            sps.bit_depth_chroma_minus8 = reader
                    .readUE("SPS: bit_depth_chroma_minus8");
            sps.qpprime_y_zero_transform_bypass_flag = reader
                    .readBool("SPS: qpprime_y_zero_transform_bypass_flag");
            boolean seqScalingMatrixPresent = reader
                    .readBool("SPS: seq_scaling_matrix_present_lag");
            if (seqScalingMatrixPresent) {
                readScalingListMatrix(reader, sps);
            }
        } else {
            sps.chroma_format_idc = ChromaFormat.YUV_420;
        }
        sps.log2_max_frame_num_minus4 = reader
                .readUE("SPS: log2_max_frame_num_minus4");
        sps.pic_order_cnt_type = reader.readUE("SPS: pic_order_cnt_type");
        if (sps.pic_order_cnt_type == 0) {
            sps.log2_max_pic_order_cnt_lsb_minus4 = reader
                    .readUE("SPS: log2_max_pic_order_cnt_lsb_minus4");
        } else if (sps.pic_order_cnt_type == 1) {
            sps.delta_pic_order_always_zero_flag = reader
                    .readBool("SPS: delta_pic_order_always_zero_flag");
            sps.offset_for_non_ref_pic = reader
                    .readSE("SPS: offset_for_non_ref_pic");
            sps.offset_for_top_to_bottom_field = reader
                    .readSE("SPS: offset_for_top_to_bottom_field");
            sps.num_ref_frames_in_pic_order_cnt_cycle = reader
                    .readUE("SPS: num_ref_frames_in_pic_order_cnt_cycle");
            sps.offsetForRefFrame = new int[sps.num_ref_frames_in_pic_order_cnt_cycle];
            for (int i = 0; i < sps.num_ref_frames_in_pic_order_cnt_cycle; i++) {
                sps.offsetForRefFrame[i] = reader
                        .readSE("SPS: offsetForRefFrame [" + i + "]");
            }
        }
        sps.num_ref_frames = reader.readUE("SPS: num_ref_frames");
        sps.gaps_in_frame_num_value_allowed_flag = reader
                .readBool("SPS: gaps_in_frame_num_value_allowed_flag");
        sps.pic_width_in_mbs_minus1 = reader
                .readUE("SPS: pic_width_in_mbs_minus1");
        sps.pic_height_in_map_units_minus1 = reader
                .readUE("SPS: pic_height_in_map_units_minus1");
        sps.frame_mbs_only_flag = reader.readBool("SPS: frame_mbs_only_flag");
        if (!sps.frame_mbs_only_flag) {
            sps.mb_adaptive_frame_field_flag = reader
                    .readBool("SPS: mb_adaptive_frame_field_flag");
        }
        sps.direct_8x8_inference_flag = reader
                .readBool("SPS: direct_8x8_inference_flag");
        sps.frame_cropping_flag = reader.readBool("SPS: frame_cropping_flag");
        if (sps.frame_cropping_flag) {
            sps.frame_crop_left_offset = reader
                    .readUE("SPS: frame_crop_left_offset");
            sps.frame_crop_right_offset = reader
                    .readUE("SPS: frame_crop_right_offset");
            sps.frame_crop_top_offset = reader
                    .readUE("SPS: frame_crop_top_offset");
            sps.frame_crop_bottom_offset = reader
                    .readUE("SPS: frame_crop_bottom_offset");
        }
        boolean vui_parameters_present_flag = reader
                .readBool("SPS: vui_parameters_present_flag");
        if (vui_parameters_present_flag)
            sps.vuiParams = ReadVUIParameters(reader);

        reader.readTrailingBits();

        return sps;
    }

    private static void readScalingListMatrix(CAVLCReader reader,
                                              SeqParameterSet sps) throws IOException {
        sps.scalingMatrix = new ScalingMatrix();
        for (int i = 0; i < 8; i++) {
            boolean seqScalingListPresentFlag = reader
                    .readBool("SPS: seqScalingListPresentFlag");
            if (seqScalingListPresentFlag) {
                sps.scalingMatrix.ScalingList4x4 = new ScalingList[8];
                sps.scalingMatrix.ScalingList8x8 = new ScalingList[8];
                if (i < 6) {
                    sps.scalingMatrix.ScalingList4x4[i] = ScalingList.read(
                            reader, 16);
                } else {
                    sps.scalingMatrix.ScalingList8x8[i - 6] = ScalingList.read(
                            reader, 64);
                }
            }
        }
    }

    private static VUIParameters ReadVUIParameters(CAVLCReader reader)
            throws IOException {
        VUIParameters vuip = new VUIParameters();
        vuip.aspect_ratio_info_present_flag = reader
                .readBool("VUI: aspect_ratio_info_present_flag");
        if (vuip.aspect_ratio_info_present_flag) {
            vuip.aspect_ratio = AspectRatio.fromValue((int) reader.readNBit(8,
                    "VUI: aspect_ratio"));
            if (vuip.aspect_ratio == AspectRatio.Extended_SAR) {
                vuip.sar_width = (int) reader.readNBit(16, "VUI: sar_width");
                vuip.sar_height = (int) reader.readNBit(16, "VUI: sar_height");
            }
        }
        vuip.overscan_info_present_flag = reader
                .readBool("VUI: overscan_info_present_flag");
        if (vuip.overscan_info_present_flag) {
            vuip.overscan_appropriate_flag = reader
                    .readBool("VUI: overscan_appropriate_flag");
        }
        vuip.video_signal_type_present_flag = reader
                .readBool("VUI: video_signal_type_present_flag");
        if (vuip.video_signal_type_present_flag) {
            vuip.video_format = (int) reader.readNBit(3, "VUI: video_format");
            vuip.video_full_range_flag = reader
                    .readBool("VUI: video_full_range_flag");
            vuip.colour_description_present_flag = reader
                    .readBool("VUI: colour_description_present_flag");
            if (vuip.colour_description_present_flag) {
                vuip.colour_primaries = (int) reader.readNBit(8,
                        "VUI: colour_primaries");
                vuip.transfer_characteristics = (int) reader.readNBit(8,
                        "VUI: transfer_characteristics");
                vuip.matrix_coefficients = (int) reader.readNBit(8,
                        "VUI: matrix_coefficients");
            }
        }
        vuip.chroma_loc_info_present_flag = reader
                .readBool("VUI: chroma_loc_info_present_flag");
        if (vuip.chroma_loc_info_present_flag) {
            vuip.chroma_sample_loc_type_top_field = reader
                    .readUE("VUI chroma_sample_loc_type_top_field");
            vuip.chroma_sample_loc_type_bottom_field = reader
                    .readUE("VUI chroma_sample_loc_type_bottom_field");
        }
        vuip.timing_info_present_flag = reader
                .readBool("VUI: timing_info_present_flag");
        if (vuip.timing_info_present_flag) {
            vuip.num_units_in_tick = (int) reader.readNBit(32,
                    "VUI: num_units_in_tick");
            vuip.time_scale = (int) reader.readNBit(32, "VUI: time_scale");
            vuip.fixed_frame_rate_flag = reader
                    .readBool("VUI: fixed_frame_rate_flag");
        }
        boolean nal_hrd_parameters_present_flag = reader
                .readBool("VUI: nal_hrd_parameters_present_flag");
        if (nal_hrd_parameters_present_flag)
            vuip.nalHRDParams = readHRDParameters(reader);
        boolean vcl_hrd_parameters_present_flag = reader
                .readBool("VUI: vcl_hrd_parameters_present_flag");
        if (vcl_hrd_parameters_present_flag)
            vuip.vclHRDParams = readHRDParameters(reader);
        if (nal_hrd_parameters_present_flag || vcl_hrd_parameters_present_flag) {
            vuip.low_delay_hrd_flag = reader
                    .readBool("VUI: low_delay_hrd_flag");
        }
        vuip.pic_struct_present_flag = reader
                .readBool("VUI: pic_struct_present_flag");
        boolean bitstream_restriction_flag = reader
                .readBool("VUI: bitstream_restriction_flag");
        if (bitstream_restriction_flag) {
            vuip.bitstreamRestriction = new VUIParameters.BitstreamRestriction();
            vuip.bitstreamRestriction.motion_vectors_over_pic_boundaries_flag = reader
                    .readBool("VUI: motion_vectors_over_pic_boundaries_flag");
            vuip.bitstreamRestriction.max_bytes_per_pic_denom = reader
                    .readUE("VUI max_bytes_per_pic_denom");
            vuip.bitstreamRestriction.max_bits_per_mb_denom = reader
                    .readUE("VUI max_bits_per_mb_denom");
            vuip.bitstreamRestriction.log2_max_mv_length_horizontal = reader
                    .readUE("VUI log2_max_mv_length_horizontal");
            vuip.bitstreamRestriction.log2_max_mv_length_vertical = reader
                    .readUE("VUI log2_max_mv_length_vertical");
            vuip.bitstreamRestriction.num_reorder_frames = reader
                    .readUE("VUI num_reorder_frames");
            vuip.bitstreamRestriction.max_dec_frame_buffering = reader
                    .readUE("VUI max_dec_frame_buffering");
        }

        return vuip;
    }

    private static HRDParameters readHRDParameters(CAVLCReader reader)
            throws IOException {
        HRDParameters hrd = new HRDParameters();
        hrd.cpb_cnt_minus1 = reader.readUE("SPS: cpb_cnt_minus1");
        hrd.bit_rate_scale = (int) reader.readNBit(4, "HRD: bit_rate_scale");
        hrd.cpb_size_scale = (int) reader.readNBit(4, "HRD: cpb_size_scale");
        hrd.bit_rate_value_minus1 = new int[hrd.cpb_cnt_minus1 + 1];
        hrd.cpb_size_value_minus1 = new int[hrd.cpb_cnt_minus1 + 1];
        hrd.cbr_flag = new boolean[hrd.cpb_cnt_minus1 + 1];

        for (int SchedSelIdx = 0; SchedSelIdx <= hrd.cpb_cnt_minus1; SchedSelIdx++) {
            hrd.bit_rate_value_minus1[SchedSelIdx] = reader
                    .readUE("HRD: bit_rate_value_minus1");
            hrd.cpb_size_value_minus1[SchedSelIdx] = reader
                    .readUE("HRD: cpb_size_value_minus1");
            hrd.cbr_flag[SchedSelIdx] = reader.readBool("HRD: cbr_flag");
        }
        hrd.initial_cpb_removal_delay_length_minus1 = (int) reader.readNBit(5,
                "HRD: initial_cpb_removal_delay_length_minus1");
        hrd.cpb_removal_delay_length_minus1 = (int) reader.readNBit(5,
                "HRD: cpb_removal_delay_length_minus1");
        hrd.dpb_output_delay_length_minus1 = (int) reader.readNBit(5,
                "HRD: dpb_output_delay_length_minus1");
        hrd.time_offset_length = (int) reader.readNBit(5,
                "HRD: time_offset_length");
        return hrd;
    }

    public void write(OutputStream out) throws IOException {
        CAVLCWriter writer = new CAVLCWriter(out);

        writer.writeNBit(profile_idc, 8, "SPS: profile_idc");
        writer.writeBool(constraint_set_0_flag, "SPS: constraint_set_0_flag");
        writer.writeBool(constraint_set_1_flag, "SPS: constraint_set_1_flag");
        writer.writeBool(constraint_set_2_flag, "SPS: constraint_set_2_flag");
        writer.writeBool(constraint_set_3_flag, "SPS: constraint_set_3_flag");
        writer.writeNBit(0, 4, "SPS: reserved");
        writer.writeNBit(level_idc, 8, "SPS: level_idc");
        writer.writeUE(seq_parameter_set_id, "SPS: seq_parameter_set_id");

        if (profile_idc == 100 || profile_idc == 110 || profile_idc == 122
                || profile_idc == 144) {
            writer.writeUE(chroma_format_idc.getId(), "SPS: chroma_format_idc");
            if (chroma_format_idc == ChromaFormat.YUV_444) {
                writer.writeBool(residual_color_transform_flag,
                        "SPS: residual_color_transform_flag");
            }
            writer.writeUE(bit_depth_luma_minus8, "SPS: ");
            writer.writeUE(bit_depth_chroma_minus8, "SPS: ");
            writer.writeBool(qpprime_y_zero_transform_bypass_flag,
                    "SPS: qpprime_y_zero_transform_bypass_flag");
            writer.writeBool(scalingMatrix != null, "SPS: ");
            if (scalingMatrix != null) {
                for (int i = 0; i < 8; i++) {
                    if (i < 6) {
                        writer.writeBool(
                                scalingMatrix.ScalingList4x4[i] != null,
                                "SPS: ");
                        if (scalingMatrix.ScalingList4x4[i] != null) {
                            scalingMatrix.ScalingList4x4[i].write(writer);
                        }
                    } else {
                        writer.writeBool(
                                scalingMatrix.ScalingList8x8[i - 6] != null,
                                "SPS: ");
                        if (scalingMatrix.ScalingList8x8[i - 6] != null) {
                            scalingMatrix.ScalingList8x8[i - 6].write(writer);
                        }
                    }
                }
            }
        }
        writer.writeUE(log2_max_frame_num_minus4,
                "SPS: log2_max_frame_num_minus4");
        writer.writeUE(pic_order_cnt_type, "SPS: pic_order_cnt_type");
        if (pic_order_cnt_type == 0) {
            writer.writeUE(log2_max_pic_order_cnt_lsb_minus4,
                    "SPS: log2_max_pic_order_cnt_lsb_minus4");
        } else if (pic_order_cnt_type == 1) {
            writer.writeBool(delta_pic_order_always_zero_flag,
                    "SPS: delta_pic_order_always_zero_flag");
            writer.writeSE(offset_for_non_ref_pic,
                    "SPS: offset_for_non_ref_pic");
            writer.writeSE(offset_for_top_to_bottom_field,
                    "SPS: offset_for_top_to_bottom_field");
            writer.writeUE(offsetForRefFrame.length, "SPS: ");
            for (int i = 0; i < offsetForRefFrame.length; i++)
                writer.writeSE(offsetForRefFrame[i], "SPS: ");
        }
        writer.writeUE(num_ref_frames, "SPS: num_ref_frames");
        writer.writeBool(gaps_in_frame_num_value_allowed_flag,
                "SPS: gaps_in_frame_num_value_allowed_flag");
        writer.writeUE(pic_width_in_mbs_minus1, "SPS: pic_width_in_mbs_minus1");
        writer.writeUE(pic_height_in_map_units_minus1,
                "SPS: pic_height_in_map_units_minus1");
        writer.writeBool(frame_mbs_only_flag, "SPS: frame_mbs_only_flag");
        if (!frame_mbs_only_flag) {
            writer.writeBool(mb_adaptive_frame_field_flag,
                    "SPS: mb_adaptive_frame_field_flag");
        }
        writer.writeBool(direct_8x8_inference_flag,
                "SPS: direct_8x8_inference_flag");
        writer.writeBool(frame_cropping_flag, "SPS: frame_cropping_flag");
        if (frame_cropping_flag) {
            writer.writeUE(frame_crop_left_offset,
                    "SPS: frame_crop_left_offset");
            writer.writeUE(frame_crop_right_offset,
                    "SPS: frame_crop_right_offset");
            writer.writeUE(frame_crop_top_offset, "SPS: frame_crop_top_offset");
            writer.writeUE(frame_crop_bottom_offset,
                    "SPS: frame_crop_bottom_offset");
        }
        writer.writeBool(vuiParams != null, "SPS: ");
        if (vuiParams != null)
            writeVUIParameters(vuiParams, writer);

        writer.writeTrailingBits();
    }

    private void writeVUIParameters(VUIParameters vuip, CAVLCWriter writer)
            throws IOException {
        writer.writeBool(vuip.aspect_ratio_info_present_flag,
                "VUI: aspect_ratio_info_present_flag");
        if (vuip.aspect_ratio_info_present_flag) {
            writer.writeNBit(vuip.aspect_ratio.getValue(), 8,
                    "VUI: aspect_ratio");
            if (vuip.aspect_ratio == AspectRatio.Extended_SAR) {
                writer.writeNBit(vuip.sar_width, 16, "VUI: sar_width");
                writer.writeNBit(vuip.sar_height, 16, "VUI: sar_height");
            }
        }
        writer.writeBool(vuip.overscan_info_present_flag,
                "VUI: overscan_info_present_flag");
        if (vuip.overscan_info_present_flag) {
            writer.writeBool(vuip.overscan_appropriate_flag,
                    "VUI: overscan_appropriate_flag");
        }
        writer.writeBool(vuip.video_signal_type_present_flag,
                "VUI: video_signal_type_present_flag");
        if (vuip.video_signal_type_present_flag) {
            writer.writeNBit(vuip.video_format, 3, "VUI: video_format");
            writer.writeBool(vuip.video_full_range_flag,
                    "VUI: video_full_range_flag");
            writer.writeBool(vuip.colour_description_present_flag,
                    "VUI: colour_description_present_flag");
            if (vuip.colour_description_present_flag) {
                writer.writeNBit(vuip.colour_primaries, 8,
                        "VUI: colour_primaries");
                writer.writeNBit(vuip.transfer_characteristics, 8,
                        "VUI: transfer_characteristics");
                writer.writeNBit(vuip.matrix_coefficients, 8,
                        "VUI: matrix_coefficients");
            }
        }
        writer.writeBool(vuip.chroma_loc_info_present_flag,
                "VUI: chroma_loc_info_present_flag");
        if (vuip.chroma_loc_info_present_flag) {
            writer.writeUE(vuip.chroma_sample_loc_type_top_field,
                    "VUI: chroma_sample_loc_type_top_field");
            writer.writeUE(vuip.chroma_sample_loc_type_bottom_field,
                    "VUI: chroma_sample_loc_type_bottom_field");
        }
        writer.writeBool(vuip.timing_info_present_flag,
                "VUI: timing_info_present_flag");
        if (vuip.timing_info_present_flag) {
            writer.writeNBit(vuip.num_units_in_tick, 32,
                    "VUI: num_units_in_tick");
            writer.writeNBit(vuip.time_scale, 32, "VUI: time_scale");
            writer.writeBool(vuip.fixed_frame_rate_flag,
                    "VUI: fixed_frame_rate_flag");
        }
        writer.writeBool(vuip.nalHRDParams != null, "VUI: ");
        if (vuip.nalHRDParams != null) {
            writeHRDParameters(vuip.nalHRDParams, writer);
        }
        writer.writeBool(vuip.vclHRDParams != null, "VUI: ");
        if (vuip.vclHRDParams != null) {
            writeHRDParameters(vuip.vclHRDParams, writer);
        }

        if (vuip.nalHRDParams != null || vuip.vclHRDParams != null) {
            writer
                    .writeBool(vuip.low_delay_hrd_flag,
                            "VUI: low_delay_hrd_flag");
        }
        writer.writeBool(vuip.pic_struct_present_flag,
                "VUI: pic_struct_present_flag");
        writer.writeBool(vuip.bitstreamRestriction != null, "VUI: ");
        if (vuip.bitstreamRestriction != null) {
            writer
                    .writeBool(
                            vuip.bitstreamRestriction.motion_vectors_over_pic_boundaries_flag,
                            "VUI: motion_vectors_over_pic_boundaries_flag");
            writer.writeUE(vuip.bitstreamRestriction.max_bytes_per_pic_denom,
                    "VUI: max_bytes_per_pic_denom");
            writer.writeUE(vuip.bitstreamRestriction.max_bits_per_mb_denom,
                    "VUI: max_bits_per_mb_denom");
            writer.writeUE(
                    vuip.bitstreamRestriction.log2_max_mv_length_horizontal,
                    "VUI: log2_max_mv_length_horizontal");
            writer.writeUE(
                    vuip.bitstreamRestriction.log2_max_mv_length_vertical,
                    "VUI: log2_max_mv_length_vertical");
            writer.writeUE(vuip.bitstreamRestriction.num_reorder_frames,
                    "VUI: num_reorder_frames");
            writer.writeUE(vuip.bitstreamRestriction.max_dec_frame_buffering,
                    "VUI: max_dec_frame_buffering");
        }

    }

    private void writeHRDParameters(HRDParameters hrd, CAVLCWriter writer)
            throws IOException {
        writer.writeUE(hrd.cpb_cnt_minus1, "HRD: cpb_cnt_minus1");
        writer.writeNBit(hrd.bit_rate_scale, 4, "HRD: bit_rate_scale");
        writer.writeNBit(hrd.cpb_size_scale, 4, "HRD: cpb_size_scale");

        for (int SchedSelIdx = 0; SchedSelIdx <= hrd.cpb_cnt_minus1; SchedSelIdx++) {
            writer.writeUE(hrd.bit_rate_value_minus1[SchedSelIdx], "HRD: ");
            writer.writeUE(hrd.cpb_size_value_minus1[SchedSelIdx], "HRD: ");
            writer.writeBool(hrd.cbr_flag[SchedSelIdx], "HRD: ");
        }
        writer.writeNBit(hrd.initial_cpb_removal_delay_length_minus1, 5,
                "HRD: initial_cpb_removal_delay_length_minus1");
        writer.writeNBit(hrd.cpb_removal_delay_length_minus1, 5,
                "HRD: cpb_removal_delay_length_minus1");
        writer.writeNBit(hrd.dpb_output_delay_length_minus1, 5,
                "HRD: dpb_output_delay_length_minus1");
        writer.writeNBit(hrd.time_offset_length, 5, "HRD: time_offset_length");
    }

    @Override
    public String toString() {
        return "SeqParameterSet{ " +
                "\n        pic_order_cnt_type=" + pic_order_cnt_type +
                ", \n        field_pic_flag=" + field_pic_flag +
                ", \n        delta_pic_order_always_zero_flag=" + delta_pic_order_always_zero_flag +
                ", \n        weighted_pred_flag=" + weighted_pred_flag +
                ", \n        weighted_bipred_idc=" + weighted_bipred_idc +
                ", \n        entropy_coding_mode_flag=" + entropy_coding_mode_flag +
                ", \n        mb_adaptive_frame_field_flag=" + mb_adaptive_frame_field_flag +
                ", \n        direct_8x8_inference_flag=" + direct_8x8_inference_flag +
                ", \n        chroma_format_idc=" + chroma_format_idc +
                ", \n        log2_max_frame_num_minus4=" + log2_max_frame_num_minus4 +
                ", \n        log2_max_pic_order_cnt_lsb_minus4=" + log2_max_pic_order_cnt_lsb_minus4 +
                ", \n        pic_height_in_map_units_minus1=" + pic_height_in_map_units_minus1 +
                ", \n        pic_width_in_mbs_minus1=" + pic_width_in_mbs_minus1 +
                ", \n        bit_depth_luma_minus8=" + bit_depth_luma_minus8 +
                ", \n        bit_depth_chroma_minus8=" + bit_depth_chroma_minus8 +
                ", \n        qpprime_y_zero_transform_bypass_flag=" + qpprime_y_zero_transform_bypass_flag +
                ", \n        profile_idc=" + profile_idc +
                ", \n        constraint_set_0_flag=" + constraint_set_0_flag +
                ", \n        constraint_set_1_flag=" + constraint_set_1_flag +
                ", \n        constraint_set_2_flag=" + constraint_set_2_flag +
                ", \n        constraint_set_3_flag=" + constraint_set_3_flag +
                ", \n        level_idc=" + level_idc +
                ", \n        seq_parameter_set_id=" + seq_parameter_set_id +
                ", \n        residual_color_transform_flag=" + residual_color_transform_flag +
                ", \n        offset_for_non_ref_pic=" + offset_for_non_ref_pic +
                ", \n        offset_for_top_to_bottom_field=" + offset_for_top_to_bottom_field +
                ", \n        num_ref_frames=" + num_ref_frames +
                ", \n        gaps_in_frame_num_value_allowed_flag=" + gaps_in_frame_num_value_allowed_flag +
                ", \n        frame_mbs_only_flag=" + frame_mbs_only_flag +
                ", \n        frame_cropping_flag=" + frame_cropping_flag +
                ", \n        frame_crop_left_offset=" + frame_crop_left_offset +
                ", \n        frame_crop_right_offset=" + frame_crop_right_offset +
                ", \n        frame_crop_top_offset=" + frame_crop_top_offset +
                ", \n        frame_crop_bottom_offset=" + frame_crop_bottom_offset +
                ", \n        offsetForRefFrame=" + offsetForRefFrame +
                ", \n        vuiParams=" + vuiParams +
                ", \n        scalingMatrix=" + scalingMatrix +
                ", \n        num_ref_frames_in_pic_order_cnt_cycle=" + num_ref_frames_in_pic_order_cnt_cycle +
                '}';
    }
}