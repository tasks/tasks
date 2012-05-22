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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Picture Parameter Set entity of H264 bitstream
 * <p/>
 * capable to serialize / deserialize with CAVLC bitstream
 *
 * @author Stanislav Vitvitskiy
 */
public class PictureParameterSet extends BitstreamElement {

    public static class PPSExt {
        public boolean transform_8x8_mode_flag;
        public ScalingMatrix scalindMatrix = new ScalingMatrix();
        public int second_chroma_qp_index_offset;
        public boolean[] pic_scaling_list_present_flag;

        @Override
        public String toString() {
            return "PPSExt{" +
                    "transform_8x8_mode_flag=" + transform_8x8_mode_flag +
                    ", scalindMatrix=" + scalindMatrix +
                    ", second_chroma_qp_index_offset=" + second_chroma_qp_index_offset +
                    ", pic_scaling_list_present_flag=" + pic_scaling_list_present_flag +
                    '}';
        }
    }

    public boolean entropy_coding_mode_flag;
    public int num_ref_idx_l0_active_minus1;
    public int num_ref_idx_l1_active_minus1;
    public int slice_group_change_rate_minus1;
    public int pic_parameter_set_id;
    public int seq_parameter_set_id;
    public boolean pic_order_present_flag;
    public int num_slice_groups_minus1;
    public int slice_group_map_type;
    public boolean weighted_pred_flag;
    public int weighted_bipred_idc;
    public int pic_init_qp_minus26;
    public int pic_init_qs_minus26;
    public int chroma_qp_index_offset;
    public boolean deblocking_filter_control_present_flag;
    public boolean constrained_intra_pred_flag;
    public boolean redundant_pic_cnt_present_flag;
    public int[] top_left;
    public int[] bottom_right;
    public int[] run_length_minus1;
    public boolean slice_group_change_direction_flag;
    public int[] slice_group_id;
    public PPSExt extended;

    public static PictureParameterSet read(byte[] b) throws IOException {
        return read(new ByteArrayInputStream(b));
    }

    public static PictureParameterSet read(InputStream is) throws IOException {
        CAVLCReader reader = new CAVLCReader(is);
        PictureParameterSet pps = new PictureParameterSet();

        pps.pic_parameter_set_id = reader.readUE("PPS: pic_parameter_set_id");
        pps.seq_parameter_set_id = reader.readUE("PPS: seq_parameter_set_id");
        pps.entropy_coding_mode_flag = reader
                .readBool("PPS: entropy_coding_mode_flag");
        pps.pic_order_present_flag = reader
                .readBool("PPS: pic_order_present_flag");
        pps.num_slice_groups_minus1 = reader
                .readUE("PPS: num_slice_groups_minus1");
        if (pps.num_slice_groups_minus1 > 0) {
            pps.slice_group_map_type = reader
                    .readUE("PPS: slice_group_map_type");
            pps.top_left = new int[pps.num_slice_groups_minus1 + 1];
            pps.bottom_right = new int[pps.num_slice_groups_minus1 + 1];
            pps.run_length_minus1 = new int[pps.num_slice_groups_minus1 + 1];
            if (pps.slice_group_map_type == 0)
                for (int iGroup = 0; iGroup <= pps.num_slice_groups_minus1; iGroup++)
                    pps.run_length_minus1[iGroup] = reader
                            .readUE("PPS: run_length_minus1");
            else if (pps.slice_group_map_type == 2)
                for (int iGroup = 0; iGroup < pps.num_slice_groups_minus1; iGroup++) {
                    pps.top_left[iGroup] = reader.readUE("PPS: top_left");
                    pps.bottom_right[iGroup] = reader
                            .readUE("PPS: bottom_right");
                }
            else if (pps.slice_group_map_type == 3
                    || pps.slice_group_map_type == 4
                    || pps.slice_group_map_type == 5) {
                pps.slice_group_change_direction_flag = reader
                        .readBool("PPS: slice_group_change_direction_flag");
                pps.slice_group_change_rate_minus1 = reader
                        .readUE("PPS: slice_group_change_rate_minus1");
            } else if (pps.slice_group_map_type == 6) {
                int NumberBitsPerSliceGroupId;
                if (pps.num_slice_groups_minus1 + 1 > 4)
                    NumberBitsPerSliceGroupId = 3;
                else if (pps.num_slice_groups_minus1 + 1 > 2)
                    NumberBitsPerSliceGroupId = 2;
                else
                    NumberBitsPerSliceGroupId = 1;
                int pic_size_in_map_units_minus1 = reader
                        .readUE("PPS: pic_size_in_map_units_minus1");
                pps.slice_group_id = new int[pic_size_in_map_units_minus1 + 1];
                for (int i = 0; i <= pic_size_in_map_units_minus1; i++) {
                    pps.slice_group_id[i] = reader.readU(
                            NumberBitsPerSliceGroupId, "PPS: slice_group_id ["
                            + i + "]f");
                }
            }
        }
        pps.num_ref_idx_l0_active_minus1 = reader
                .readUE("PPS: num_ref_idx_l0_active_minus1");
        pps.num_ref_idx_l1_active_minus1 = reader
                .readUE("PPS: num_ref_idx_l1_active_minus1");
        pps.weighted_pred_flag = reader.readBool("PPS: weighted_pred_flag");
        pps.weighted_bipred_idc = (int) reader.readNBit(2,
                "PPS: weighted_bipred_idc");
        pps.pic_init_qp_minus26 = reader.readSE("PPS: pic_init_qp_minus26");
        pps.pic_init_qs_minus26 = reader.readSE("PPS: pic_init_qs_minus26");
        pps.chroma_qp_index_offset = reader
                .readSE("PPS: chroma_qp_index_offset");
        pps.deblocking_filter_control_present_flag = reader
                .readBool("PPS: deblocking_filter_control_present_flag");
        pps.constrained_intra_pred_flag = reader
                .readBool("PPS: constrained_intra_pred_flag");
        pps.redundant_pic_cnt_present_flag = reader
                .readBool("PPS: redundant_pic_cnt_present_flag");
        if (reader.moreRBSPData()) {
            pps.extended = new PictureParameterSet.PPSExt();
            pps.extended.transform_8x8_mode_flag = reader
                    .readBool("PPS: transform_8x8_mode_flag");
            boolean pic_scaling_matrix_present_flag = reader
                    .readBool("PPS: pic_scaling_matrix_present_flag");
            if (pic_scaling_matrix_present_flag) {
                for (int i = 0; i < 6 + 2 * (pps.extended.transform_8x8_mode_flag ? 1
                        : 0); i++) {
                    boolean pic_scaling_list_present_flag = reader
                            .readBool("PPS: pic_scaling_list_present_flag");
                    if (pic_scaling_list_present_flag) {
                        pps.extended.scalindMatrix.ScalingList4x4 = new ScalingList[8];
                        pps.extended.scalindMatrix.ScalingList8x8 = new ScalingList[8];
                        if (i < 6) {
                            pps.extended.scalindMatrix.ScalingList4x4[i] = ScalingList
                                    .read(reader, 16);
                        } else {
                            pps.extended.scalindMatrix.ScalingList8x8[i - 6] = ScalingList
                                    .read(reader, 64);
                        }
                    }
                }
            }
            pps.extended.second_chroma_qp_index_offset = reader
                    .readSE("PPS: second_chroma_qp_index_offset");
        }

        reader.readTrailingBits();

        return pps;
    }

    public void write(OutputStream out) throws IOException {
        CAVLCWriter writer = new CAVLCWriter(out);

        writer.writeUE(pic_parameter_set_id, "PPS: pic_parameter_set_id");
        writer.writeUE(seq_parameter_set_id, "PPS: seq_parameter_set_id");
        writer.writeBool(entropy_coding_mode_flag,
                "PPS: entropy_coding_mode_flag");
        writer.writeBool(pic_order_present_flag, "PPS: pic_order_present_flag");
        writer.writeUE(num_slice_groups_minus1, "PPS: num_slice_groups_minus1");
        if (num_slice_groups_minus1 > 0) {
            writer.writeUE(slice_group_map_type, "PPS: slice_group_map_type");
            int[] top_left = new int[1];
            int[] bottom_right = new int[1];
            int[] run_length_minus1 = new int[1];
            if (slice_group_map_type == 0) {
                for (int iGroup = 0; iGroup <= num_slice_groups_minus1; iGroup++) {
                    writer.writeUE(run_length_minus1[iGroup], "PPS: ");
                }
            } else if (slice_group_map_type == 2) {
                for (int iGroup = 0; iGroup < num_slice_groups_minus1; iGroup++) {
                    writer.writeUE(top_left[iGroup], "PPS: ");
                    writer.writeUE(bottom_right[iGroup], "PPS: ");
                }
            } else if (slice_group_map_type == 3 || slice_group_map_type == 4
                    || slice_group_map_type == 5) {
                writer.writeBool(slice_group_change_direction_flag,
                        "PPS: slice_group_change_direction_flag");
                writer.writeUE(slice_group_change_rate_minus1,
                        "PPS: slice_group_change_rate_minus1");
            } else if (slice_group_map_type == 6) {
                int NumberBitsPerSliceGroupId;
                if (num_slice_groups_minus1 + 1 > 4)
                    NumberBitsPerSliceGroupId = 3;
                else if (num_slice_groups_minus1 + 1 > 2)
                    NumberBitsPerSliceGroupId = 2;
                else
                    NumberBitsPerSliceGroupId = 1;
                writer.writeUE(slice_group_id.length, "PPS: ");
                for (int i = 0; i <= slice_group_id.length; i++) {
                    writer.writeU(slice_group_id[i], NumberBitsPerSliceGroupId);
                }
            }
        }
        writer.writeUE(num_ref_idx_l0_active_minus1,
                "PPS: num_ref_idx_l0_active_minus1");
        writer.writeUE(num_ref_idx_l1_active_minus1,
                "PPS: num_ref_idx_l1_active_minus1");
        writer.writeBool(weighted_pred_flag, "PPS: weighted_pred_flag");
        writer.writeNBit(weighted_bipred_idc, 2, "PPS: weighted_bipred_idc");
        writer.writeSE(pic_init_qp_minus26, "PPS: pic_init_qp_minus26");
        writer.writeSE(pic_init_qs_minus26, "PPS: pic_init_qs_minus26");
        writer.writeSE(chroma_qp_index_offset, "PPS: chroma_qp_index_offset");
        writer.writeBool(deblocking_filter_control_present_flag,
                "PPS: deblocking_filter_control_present_flag");
        writer.writeBool(constrained_intra_pred_flag,
                "PPS: constrained_intra_pred_flag");
        writer.writeBool(redundant_pic_cnt_present_flag,
                "PPS: redundant_pic_cnt_present_flag");
        if (extended != null) {
            writer.writeBool(extended.transform_8x8_mode_flag,
                    "PPS: transform_8x8_mode_flag");
            writer.writeBool(extended.scalindMatrix != null,
                    "PPS: scalindMatrix");
            if (extended.scalindMatrix != null) {
                for (int i = 0; i < 6 + 2 * (extended.transform_8x8_mode_flag ? 1
                        : 0); i++) {
                    if (i < 6) {
                        writer
                                .writeBool(
                                        extended.scalindMatrix.ScalingList4x4[i] != null,
                                        "PPS: ");
                        if (extended.scalindMatrix.ScalingList4x4[i] != null) {
                            extended.scalindMatrix.ScalingList4x4[i]
                                    .write(writer);
                        }

                    } else {
                        writer
                                .writeBool(
                                        extended.scalindMatrix.ScalingList8x8[i - 6] != null,
                                        "PPS: ");
                        if (extended.scalindMatrix.ScalingList8x8[i - 6] != null) {
                            extended.scalindMatrix.ScalingList8x8[i - 6]
                                    .write(writer);
                        }
                    }
                }
            }
            writer.writeSE(extended.second_chroma_qp_index_offset, "PPS: ");
        }

        writer.writeTrailingBits();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(bottom_right);
        result = prime * result + chroma_qp_index_offset;
        result = prime * result + (constrained_intra_pred_flag ? 1231 : 1237);
        result = prime * result
                + (deblocking_filter_control_present_flag ? 1231 : 1237);
        result = prime * result + (entropy_coding_mode_flag ? 1231 : 1237);
        result = prime * result
                + ((extended == null) ? 0 : extended.hashCode());
        result = prime * result + num_ref_idx_l0_active_minus1;
        result = prime * result + num_ref_idx_l1_active_minus1;
        result = prime * result + num_slice_groups_minus1;
        result = prime * result + pic_init_qp_minus26;
        result = prime * result + pic_init_qs_minus26;
        result = prime * result + (pic_order_present_flag ? 1231 : 1237);
        result = prime * result + pic_parameter_set_id;
        result = prime * result
                + (redundant_pic_cnt_present_flag ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(run_length_minus1);
        result = prime * result + seq_parameter_set_id;
        result = prime * result
                + (slice_group_change_direction_flag ? 1231 : 1237);
        result = prime * result + slice_group_change_rate_minus1;
        result = prime * result + Arrays.hashCode(slice_group_id);
        result = prime * result + slice_group_map_type;
        result = prime * result + Arrays.hashCode(top_left);
        result = prime * result + weighted_bipred_idc;
        result = prime * result + (weighted_pred_flag ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PictureParameterSet other = (PictureParameterSet) obj;
        if (!Arrays.equals(bottom_right, other.bottom_right))
            return false;
        if (chroma_qp_index_offset != other.chroma_qp_index_offset)
            return false;
        if (constrained_intra_pred_flag != other.constrained_intra_pred_flag)
            return false;
        if (deblocking_filter_control_present_flag != other.deblocking_filter_control_present_flag)
            return false;
        if (entropy_coding_mode_flag != other.entropy_coding_mode_flag)
            return false;
        if (extended == null) {
            if (other.extended != null)
                return false;
        } else if (!extended.equals(other.extended))
            return false;
        if (num_ref_idx_l0_active_minus1 != other.num_ref_idx_l0_active_minus1)
            return false;
        if (num_ref_idx_l1_active_minus1 != other.num_ref_idx_l1_active_minus1)
            return false;
        if (num_slice_groups_minus1 != other.num_slice_groups_minus1)
            return false;
        if (pic_init_qp_minus26 != other.pic_init_qp_minus26)
            return false;
        if (pic_init_qs_minus26 != other.pic_init_qs_minus26)
            return false;
        if (pic_order_present_flag != other.pic_order_present_flag)
            return false;
        if (pic_parameter_set_id != other.pic_parameter_set_id)
            return false;
        if (redundant_pic_cnt_present_flag != other.redundant_pic_cnt_present_flag)
            return false;
        if (!Arrays.equals(run_length_minus1, other.run_length_minus1))
            return false;
        if (seq_parameter_set_id != other.seq_parameter_set_id)
            return false;
        if (slice_group_change_direction_flag != other.slice_group_change_direction_flag)
            return false;
        if (slice_group_change_rate_minus1 != other.slice_group_change_rate_minus1)
            return false;
        if (!Arrays.equals(slice_group_id, other.slice_group_id))
            return false;
        if (slice_group_map_type != other.slice_group_map_type)
            return false;
        if (!Arrays.equals(top_left, other.top_left))
            return false;
        if (weighted_bipred_idc != other.weighted_bipred_idc)
            return false;
        if (weighted_pred_flag != other.weighted_pred_flag)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PictureParameterSet{" +
                "\n       entropy_coding_mode_flag=" + entropy_coding_mode_flag +
                ",\n       num_ref_idx_l0_active_minus1=" + num_ref_idx_l0_active_minus1 +
                ",\n       num_ref_idx_l1_active_minus1=" + num_ref_idx_l1_active_minus1 +
                ",\n       slice_group_change_rate_minus1=" + slice_group_change_rate_minus1 +
                ",\n       pic_parameter_set_id=" + pic_parameter_set_id +
                ",\n       seq_parameter_set_id=" + seq_parameter_set_id +
                ",\n       pic_order_present_flag=" + pic_order_present_flag +
                ",\n       num_slice_groups_minus1=" + num_slice_groups_minus1 +
                ",\n       slice_group_map_type=" + slice_group_map_type +
                ",\n       weighted_pred_flag=" + weighted_pred_flag +
                ",\n       weighted_bipred_idc=" + weighted_bipred_idc +
                ",\n       pic_init_qp_minus26=" + pic_init_qp_minus26 +
                ",\n       pic_init_qs_minus26=" + pic_init_qs_minus26 +
                ",\n       chroma_qp_index_offset=" + chroma_qp_index_offset +
                ",\n       deblocking_filter_control_present_flag=" + deblocking_filter_control_present_flag +
                ",\n       constrained_intra_pred_flag=" + constrained_intra_pred_flag +
                ",\n       redundant_pic_cnt_present_flag=" + redundant_pic_cnt_present_flag +
                ",\n       top_left=" + top_left +
                ",\n       bottom_right=" + bottom_right +
                ",\n       run_length_minus1=" + run_length_minus1 +
                ",\n       slice_group_change_direction_flag=" + slice_group_change_direction_flag +
                ",\n       slice_group_id=" + slice_group_id +
                ",\n       extended=" + extended +
                '}';
    }
}
