/*
 * Copyright 2011 castLabs, Berlin
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
package com.googlecode.mp4parser.boxes.mp4.objectdescriptors;

import com.coremedia.iso.IsoTypeWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;


//
//GetAudioObjectType()
//{
//audioObjectType; 5 uimsbf
//if (audioObjectType == 31) {
//audioObjectType = 32 + audioObjectTypeExt; 6 uimsbf
//}
//return audioObjectType;
//}
//AudioSpecificConfig ()
//{
//audioObjectType = GetAudioObjectType();
//samplingFrequencyIndex; 4 bslbf
//if ( samplingFrequencyIndex == 0xf ) {
//samplingFrequency; 24 uimsbf
//}
//channelConfiguration; 4 bslbf
//sbrPresentFlag = -1;
//psPresentFlag = -1;
//if ( audioObjectType == 5 ||
//audioObjectType == 29 ) {
//extensionAudioObjectType = 5;
//sbrPresentFlag = 1;
//if ( audioObjectType == 29 ) {
//psPresentFlag = 1;
//}
//extensionSamplingFrequencyIndex; 4 uimsbf
//if ( extensionSamplingFrequencyIndex == 0xf )
//extensionSamplingFrequency; 24 uimsbf
//audioObjectType = GetAudioObjectType();
//if ( audioObjectType == 22 )
//extensionChannelConfiguration; 4 uimsbf
//}
//else {
//extensionAudioObjectType = 0;
//}
//switch (audioObjectType) {
//case 1:
//case 2:
//case 3:
//case 4:
//case 6:
//case 7:
//case 17:
//case 19:
//case 20:
//case 21:
//case 22:
//case 23:
//GASpecificConfig();
//break:
//case 8:
//CelpSpecificConfig();
//break;
//case 9:
//HvxcSpecificConfig();
//break:
//case 12:
//TTSSpecificConfig();
//break;
//case 13:
//case 14:
//case 15:
//case 16:
//StructuredAudioSpecificConfig();
//break;
//case 24:
//ErrorResilientCelpSpecificConfig();
//break;
//case 25:
//ErrorResilientHvxcSpecificConfig();
//break;
//case 26:
//case 27:
//ParametricSpecificConfig();
//break;
// case 28:
//SSCSpecificConfig();
//break;
//case 30:
//sacPayloadEmbedding; 1 uimsbf
//SpatialSpecificConfig();
//break;
//case 32:
//case 33:
//case 34:
//MPEG_1_2_SpecificConfig();
//break;
//case 35:
//DSTSpecificConfig();
//break;
//case 36:
//fillBits; 5 bslbf
//ALSSpecificConfig();
//break;
//case 37:
//case 38:
//SLSSpecificConfig();
//break;
//case 39:
//ELDSpecificConfig(channelConfiguration);
//break:
//case 40:
//case 41:
//SymbolicMusicSpecificConfig();
//break;
//default:
///* reserved */
//}
//switch (audioObjectType) {
//case 17:
//case 19:
//case 20:
//case 21:
//case 22:
//case 23:
//case 24:
//case 25:
//case 26:
//case 27:
//case 39:
//epConfig; 2 bslbf
//if ( epConfig == 2 || epConfig == 3 ) {
//ErrorProtectionSpecificConfig();
//}
//if ( epConfig == 3 ) {
//directMapping; 1 bslbf
//if ( ! directMapping ) {
///* tbd */
//}
//}
//}
//if ( extensionAudioObjectType != 5 && bits_to_decode() >= 16 ) {
//syncExtensionType; 11 bslbf
//if (syncExtensionType == 0x2b7) {
//        extensionAudioObjectType = GetAudioObjectType();
//if ( extensionAudioObjectType == 5 ) {
//sbrPresentFlag; 1 uimsbf
//if (sbrPresentFlag == 1) {
//extensionSamplingFrequencyIndex; 4 uimsbf
//if ( extensionSamplingFrequencyIndex == 0xf ) {
//extensionSamplingFrequency; 24 uimsbf
//}
//if ( bits_to_decode() >= 12 ) {
//syncExtensionType; 11 bslbf
//if (syncExtesionType == 0x548) {
//psPresentFlag; 1 uimsbf
//}
//}
//}
//}
//if ( extensionAudioObjectType == 22 ) {
//sbrPresentFlag; 1 uimsbf
//if (sbrPresentFlag == 1) {
//extensionSamplingFrequencyIndex; 4 uimsbf
//if ( extensionSamplingFrequencyIndex == 0xf ) {
//extensionSamplingFrequency; 24 uimsbf
//}
//}
//extensionChannelConfiguration; 4 uimsbf
//}
//}
//}
//}
//        }
//
// TFCodingType
//0x0 AAC scaleable
//0x1 BSAC
//0x2 TwinVQ
//0x3 AAC non scaleable (i.e. multichannel)
//
// class TFSpecificConfig( uint(4) samplingFrequencyIndex, uint(4) channelConfiguration ) {
//uint(2) TFCodingType;
//uint(1) frameLength;
//uint(1) dependsOnCoreCoder;
//if (dependsOnCoreCoder == 1){
//uint(14)coreCoderDelay
//}
//if (TFCodingType==BSAC) {
//uint(11) lslayer_length
//}
//uint (1) extensionFlag;
//if (channelConfiguration == 0 ){
//program_config_element();
//}
//if (extensionFlag==1){
//<to be defined in mpeg4 phase 2>
//}
//}
//
//program_config_element()
//{
//element_instance_tag 4 uimsbf
//profile 2 uimsbf
//sampling_frequency_index 4 uimsbf
//num_front_channel_elements 4 uimsbf
//num_side_channel_elements 4 uimsbf
//num_back_channel_elements 4 uimsbf
// num_lfe_channel_elements 2 uimsbf
//num_assoc_data_elements 3 uimsbf
//num_valid_cc_elements 4 uimsbf
//mono_mixdown_present 1 uimsbf
//if ( mono_mixdown_present == 1 )
//mono_mixdown_element_number 4 uimsbf
//stereo_mixdown_present 1 uimsbf
//if ( stereo_mixdown_present == 1 )
//stereo_mixdown_element_number 4 uimsbf
//matrix_mixdown_idx_present 1 uimsbf
//if ( matrix_mixdown_idx_present == 1 ) {
//matrix_mixdown_idx 2 uimsbf
//pseudo_surround_enable 1 uimsbf
//}
//for ( i = 0; i < num_front_channel_elements; i++) {
//front_element_is_cpe[i]; 1 bslbf
//front_element_tag_select[i]; 4 uimsbf
//}
//for ( i = 0; i < num_side_channel_elements; i++) {
//side_element_is_cpe[i]; 1 bslbf
//side_element_tag_select[i]; 4 uimsbf
//}
//for ( i = 0; i < num_back_channel_elements; i++) {
//back_element_is_cpe[i]; 1 bslbf
//back_element_tag_select[i]; 4 uimsbf
//}
//for ( i = 0; i < num_lfe_channel_elements; i++)
//lfe_element_tag_select[i]; 4 uimsbf
//for ( i = 0; i < num_assoc_data_elements; i++)
//assoc_data_element_tag_select[i]; 4 uimsbf
//for ( i = 0; i < num_valid_cc_elements; i++) {
//cc_element_is_ind_sw[i]; 1 uimsbf
//valid_cc_element_tag_select[i]; 4 uimsbf
//}
//byte_alignment()
//comment_field_bytes 8 uimsbf
//for ( i = 0; i < comment_field_bytes; i++)
//comment_field_data[i]; 8 uimsbf
//}

@Descriptor(tags = 0x5, objectTypeIndication = 0x40)
public class AudioSpecificConfig extends BaseDescriptor {
    ByteBuffer configBytes;
    long endPos;

    public static Map<Integer, Integer> samplingFrequencyIndexMap = new HashMap<Integer, Integer>();
    public static Map<Integer, String> audioObjectTypeMap = new HashMap<Integer, String>();
    int audioObjectType;
    int samplingFrequencyIndex;
    int samplingFrequency;
    int channelConfiguration;
    int extensionAudioObjectType;
    int sbrPresentFlag;
    int psPresentFlag;
    int extensionSamplingFrequencyIndex;
    int extensionSamplingFrequency;
    int extensionChannelConfiguration;
    int sacPayloadEmbedding;
    int fillBits;
    int epConfig;
    int directMapping;
    int syncExtensionType;

    //GASpecificConfig
    int frameLengthFlag;
    int dependsOnCoreCoder;
    int coreCoderDelay;
    int extensionFlag;
    int layerNr;
    int numOfSubFrame;
    int layer_length;
    int aacSectionDataResilienceFlag;
    int aacScalefactorDataResilienceFlag;
    int aacSpectralDataResilienceFlag;
    int extensionFlag3;
    boolean gaSpecificConfig;

    //ParametricSpecificConfig
    int isBaseLayer;
    int paraMode;
    int paraExtensionFlag;
    int hvxcVarMode;
    int hvxcRateMode;
    int erHvxcExtensionFlag;
    int var_ScalableFlag;
    int hilnQuantMode;
    int hilnMaxNumLine;
    int hilnSampleRateCode;
    int hilnFrameLength;
    int hilnContMode;
    int hilnEnhaLayer;
    int hilnEnhaQuantMode;
    boolean parametricSpecificConfig;

    @Override
    public void parseDetail(ByteBuffer bb) throws IOException {
        configBytes = bb.slice();
        configBytes.limit(sizeOfInstance);
        bb.position(bb.position() + sizeOfInstance);


        BitReaderBuffer bitReaderBuffer = new BitReaderBuffer(configBytes);
        audioObjectType = getAudioObjectType(bitReaderBuffer);
        samplingFrequencyIndex = bitReaderBuffer.readBits(4);

        if (samplingFrequencyIndex == 0xf) {
            samplingFrequency = bitReaderBuffer.readBits(24);
        }

        channelConfiguration = bitReaderBuffer.readBits(4);

        if (audioObjectType == 5 ||
                audioObjectType == 29) {
            extensionAudioObjectType = 5;
            sbrPresentFlag = 1;
            if (audioObjectType == 29) {
                psPresentFlag = 1;
            }
            extensionSamplingFrequencyIndex = bitReaderBuffer.readBits(4);
            if (extensionSamplingFrequencyIndex == 0xf)
                extensionSamplingFrequency = bitReaderBuffer.readBits(24);
            audioObjectType = getAudioObjectType(bitReaderBuffer);
            if (audioObjectType == 22)
                extensionChannelConfiguration = bitReaderBuffer.readBits(4);
        } else {
            extensionAudioObjectType = 0;
        }

        switch (audioObjectType) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 6:
            case 7:
            case 17:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                parseGaSpecificConfig(samplingFrequencyIndex, channelConfiguration, audioObjectType, bitReaderBuffer);
                //GASpecificConfig();
                break;
            case 8:
                throw new UnsupportedOperationException("can't parse CelpSpecificConfig yet");
                //CelpSpecificConfig();
                //break;
            case 9:
                throw new UnsupportedOperationException("can't parse HvxcSpecificConfig yet");
                //HvxcSpecificConfig();
                //break;
            case 12:
                throw new UnsupportedOperationException("can't parse TTSSpecificConfig yet");
                //TTSSpecificConfig();
                //break;
            case 13:
            case 14:
            case 15:
            case 16:
                throw new UnsupportedOperationException("can't parse StructuredAudioSpecificConfig yet");
                //StructuredAudioSpecificConfig();
                //break;
            case 24:
                throw new UnsupportedOperationException("can't parse ErrorResilientCelpSpecificConfig yet");
                //ErrorResilientCelpSpecificConfig();
                //break;
            case 25:
                throw new UnsupportedOperationException("can't parse ErrorResilientHvxcSpecificConfig yet");
                //ErrorResilientHvxcSpecificConfig();
                //break;
            case 26:
            case 27:
                parseParametricSpecificConfig(samplingFrequencyIndex, channelConfiguration, audioObjectType, bitReaderBuffer);
                //ParametricSpecificConfig();
                break;
            case 28:
                throw new UnsupportedOperationException("can't parse SSCSpecificConfig yet");
                //SSCSpecificConfig();
                //break;
            case 30:
                sacPayloadEmbedding = bitReaderBuffer.readBits(1);
                throw new UnsupportedOperationException("can't parse SpatialSpecificConfig yet");
                //SpatialSpecificConfig();
                //break;
            case 32:
            case 33:
            case 34:
                throw new UnsupportedOperationException("can't parse MPEG_1_2_SpecificConfig yet");
                //MPEG_1_2_SpecificConfig();
                //break;
            case 35:
                throw new UnsupportedOperationException("can't parse DSTSpecificConfig yet");
                //DSTSpecificConfig();
                //break;
            case 36:
                fillBits = bitReaderBuffer.readBits(5);
                throw new UnsupportedOperationException("can't parse ALSSpecificConfig yet");
                //ALSSpecificConfig();
                //break;
            case 37:
            case 38:
                throw new UnsupportedOperationException("can't parse SLSSpecificConfig yet");
                //SLSSpecificConfig();
                //break;
            case 39:
                throw new UnsupportedOperationException("can't parse ELDSpecificConfig yet");
                //ELDSpecificConfig(channelConfiguration);
                //break;
            case 40:
            case 41:
                throw new UnsupportedOperationException("can't parse SymbolicMusicSpecificConfig yet");
                //SymbolicMusicSpecificConfig();
                //break;
            default:
                /* reserved */
        }

        switch (audioObjectType) {
            case 17:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 39:
                epConfig = bitReaderBuffer.readBits(2);
                if (epConfig == 2 || epConfig == 3) {
                    throw new UnsupportedOperationException("can't parse ErrorProtectionSpecificConfig yet");
                    //ErrorProtectionSpecificConfig();
                }
                if (epConfig == 3) {
                    directMapping = bitReaderBuffer.readBits(1);
                    if (directMapping == 0) {
                        /* tbd */
                        throw new RuntimeException("not implemented");
                    }
                }
        }

        if (extensionAudioObjectType != 5 && bitReaderBuffer.remainingBits() >= 16) {
            syncExtensionType = bitReaderBuffer.readBits(11);
            if (syncExtensionType == 0x2b7) {
                extensionAudioObjectType = getAudioObjectType(bitReaderBuffer);
                if (extensionAudioObjectType == 5) {
                    sbrPresentFlag = bitReaderBuffer.readBits(1);
                    if (sbrPresentFlag == 1) {
                        extensionSamplingFrequencyIndex = bitReaderBuffer.readBits(4);
                        if (extensionSamplingFrequencyIndex == 0xf) {
                            extensionSamplingFrequency = bitReaderBuffer.readBits(24);
                        }
                        if (bitReaderBuffer.remainingBits() >= 12) {
                            syncExtensionType = bitReaderBuffer.readBits(11); //10101001000
                            if (syncExtensionType == 0x548) {
                                psPresentFlag = bitReaderBuffer.readBits(1);
                            }
                        }
                    }
                }
                if (extensionAudioObjectType == 22) {
                    sbrPresentFlag = bitReaderBuffer.readBits(1);
                    if (sbrPresentFlag == 1) {
                        extensionSamplingFrequencyIndex = bitReaderBuffer.readBits(4);
                        if (extensionSamplingFrequencyIndex == 0xf) {
                            extensionSamplingFrequency = bitReaderBuffer.readBits(24);
                        }
                    }
                    extensionChannelConfiguration = bitReaderBuffer.readBits(4);
                }
            }
        }
    }

    private int gaSpecificConfigSize() {
        return 0;
    }

    public int serializedSize() {
        int out = 4;
        if (audioObjectType == 2) {
            out += gaSpecificConfigSize();
        } else {
            throw new UnsupportedOperationException("can't serialize that yet");
        }
        return out;
    }

    public ByteBuffer serialize() {
        ByteBuffer out = ByteBuffer.allocate(serializedSize());
        IsoTypeWriter.writeUInt8(out, 5);
        IsoTypeWriter.writeUInt8(out, serializedSize() - 2);
        BitWriterBuffer bwb = new BitWriterBuffer(out);
        bwb.writeBits(audioObjectType, 5);
        bwb.writeBits(samplingFrequencyIndex, 4);
        if (samplingFrequencyIndex == 0xf) {
            throw new UnsupportedOperationException("can't serialize that yet");
        }
        bwb.writeBits(channelConfiguration, 4);

        // Don't support any extensions, unusual GASpecificConfig other than the default or anything...

        return out;
    }

    private int getAudioObjectType(BitReaderBuffer in) throws IOException {
        int audioObjectType = in.readBits(5);
        if (audioObjectType == 31) {
            audioObjectType = 32 + in.readBits(6);
        }
        return audioObjectType;
    }

    private void parseGaSpecificConfig(int samplingFrequencyIndex, int channelConfiguration, int audioObjectType, BitReaderBuffer in) throws IOException {
//    GASpecificConfig (samplingFrequencyIndex,
//            channelConfiguration,
//            audioObjectType)
//    {
        frameLengthFlag = in.readBits(1);
        dependsOnCoreCoder = in.readBits(1);
        if (dependsOnCoreCoder == 1) {
            coreCoderDelay = in.readBits(14);
        }
        extensionFlag = in.readBits(1);
        if (channelConfiguration == 0) {
            throw new UnsupportedOperationException("can't parse program_config_element yet");
            //program_config_element ();
        }
        if ((audioObjectType == 6) || (audioObjectType == 20)) {
            layerNr = in.readBits(3);
        }
        if (extensionFlag == 1) {
            if (audioObjectType == 22) {
                numOfSubFrame = in.readBits(5);
                layer_length = in.readBits(11);
            }
            if (audioObjectType == 17 || audioObjectType == 19 ||
                    audioObjectType == 20 || audioObjectType == 23) {
                aacSectionDataResilienceFlag = in.readBits(1);
                aacScalefactorDataResilienceFlag = in.readBits(1);
                aacSpectralDataResilienceFlag = in.readBits(1);
            }
            extensionFlag3 = in.readBits(1);
            if (extensionFlag3 == 1) {
                /* tbd in version 3 */
            }
        }
//    }
        gaSpecificConfig = true;
    }

    private void parseParametricSpecificConfig(int samplingFrequencyIndex, int channelConfiguration, int audioObjectType, BitReaderBuffer in) throws IOException {
        /*
        ParametricSpecificConfig() {
            isBaseLayer; 1 uimsbf
            if (isBaseLayer) {
                PARAconfig();
            } else {
                HILNenexConfig();
            }
        }
        */
        isBaseLayer = in.readBits(1);
        if (isBaseLayer == 1) {
            parseParaConfig(samplingFrequencyIndex, channelConfiguration, audioObjectType, in);
        } else {
            parseHilnEnexConfig(samplingFrequencyIndex, channelConfiguration, audioObjectType, in);
        }
    }

    private void parseParaConfig(int samplingFrequencyIndex, int channelConfiguration, int audioObjectType, BitReaderBuffer in) throws IOException {
        /*
        PARAconfig()
        {
            PARAmode; 2 uimsbf
            if (PARAmode != 1) {
                ErHVXCconfig();
            }
            if (PARAmode != 0) {
                HILNconfig();
            }
            PARAextensionFlag; 1 uimsbf
            if (PARAextensionFlag) {
                // to be defined in MPEG-4 Phase 3
            }
        }
        */
        paraMode = in.readBits(2);

        if (paraMode != 1) {
            parseErHvxcConfig(samplingFrequencyIndex, channelConfiguration, audioObjectType, in);
        }
        if (paraMode != 0) {
            parseHilnConfig(samplingFrequencyIndex, channelConfiguration, audioObjectType, in);
        }

        paraExtensionFlag = in.readBits(1);
        parametricSpecificConfig = true;
    }

    private void parseErHvxcConfig(int samplingFrequencyIndex, int channelConfiguration, int audioObjectType, BitReaderBuffer in) throws IOException {
        /*
        ErHVXCconfig()
        {
            HVXCvarMode; 1 uimsbf
                HVXCrateMode; 2 uimsbf
                extensionFlag; 1 uimsbf
            if (extensionFlag) {
                var_ScalableFlag; 1 uimsbf
            }
        }
        */
        hvxcVarMode = in.readBits(1);
        hvxcRateMode = in.readBits(2);
        erHvxcExtensionFlag = in.readBits(1);

        if (erHvxcExtensionFlag == 1) {
            var_ScalableFlag = in.readBits(1);
        }
    }

    private void parseHilnConfig(int samplingFrequencyIndex, int channelConfiguration, int audioObjectType, BitReaderBuffer in) throws IOException {
        /*
        HILNconfig()
        {
            HILNquantMode; 1 uimsbf
            HILNmaxNumLine; 8 uimsbf
            HILNsampleRateCode; 4 uimsbf
            HILNframeLength; 12 uimsbf
            HILNcontMode; 2 uimsbf
        }
        */
        hilnQuantMode = in.readBits(1);
        hilnMaxNumLine = in.readBits(8);
        hilnSampleRateCode = in.readBits(4);
        hilnFrameLength = in.readBits(12);
        hilnContMode = in.readBits(2);
    }

    private void parseHilnEnexConfig(int samplingFrequencyIndex, int channelConfiguration, int audioObjectType, BitReaderBuffer in) throws IOException {
        /*
        HILNenexConfig()
        {
            HILNenhaLayer; 1 uimsbf
            if (HILNenhaLayer) {
                HILNenhaQuantMode; 2 uimsbf
            }
        }
        */
        hilnEnhaLayer = in.readBits(1);
        if (hilnEnhaLayer == 1) {
            hilnEnhaQuantMode = in.readBits(2);
        }
    }

    public ByteBuffer getConfigBytes() {
        return configBytes;
    }

    public int getAudioObjectType() {
        return audioObjectType;
    }

    public int getExtensionAudioObjectType() {
        return extensionAudioObjectType;
    }

    public int getSbrPresentFlag() {
        return sbrPresentFlag;
    }

    public int getPsPresentFlag() {
        return psPresentFlag;
    }

    public void setAudioObjectType(int audioObjectType) {
        this.audioObjectType = audioObjectType;
    }

    public void setSamplingFrequencyIndex(int samplingFrequencyIndex) {
        this.samplingFrequencyIndex = samplingFrequencyIndex;
    }

    public void setSamplingFrequency(int samplingFrequency) {
        this.samplingFrequency = samplingFrequency;
    }

    public void setChannelConfiguration(int channelConfiguration) {
        this.channelConfiguration = channelConfiguration;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AudioSpecificConfig");
        sb.append("{configBytes=").append(configBytes);
        sb.append(", audioObjectType=").append(audioObjectType).append(" (").append(audioObjectTypeMap.get(audioObjectType)).append(")");
        sb.append(", samplingFrequencyIndex=").append(samplingFrequencyIndex).append(" (").append(samplingFrequencyIndexMap.get(samplingFrequencyIndex)).append(")");
        sb.append(", samplingFrequency=").append(samplingFrequency);
        sb.append(", channelConfiguration=").append(channelConfiguration);
        if (extensionAudioObjectType > 0) {
            sb.append(", extensionAudioObjectType=").append(extensionAudioObjectType).append(" (").append(audioObjectTypeMap.get(extensionAudioObjectType)).append(")");
            sb.append(", sbrPresentFlag=").append(sbrPresentFlag);
            sb.append(", psPresentFlag=").append(psPresentFlag);
            sb.append(", extensionSamplingFrequencyIndex=").append(extensionSamplingFrequencyIndex).append(" (").append(samplingFrequencyIndexMap.get(extensionSamplingFrequencyIndex)).append(")");
            sb.append(", extensionSamplingFrequency=").append(extensionSamplingFrequency);
            sb.append(", extensionChannelConfiguration=").append(extensionChannelConfiguration);
        }
//    sb.append(", sacPayloadEmbedding=").append(sacPayloadEmbedding);
//    sb.append(", fillBits=").append(fillBits);
//    sb.append(", epConfig=").append(epConfig);
//    sb.append(", directMapping=").append(directMapping);
        sb.append(", syncExtensionType=").append(syncExtensionType);
        if (gaSpecificConfig) {
            sb.append(", frameLengthFlag=").append(frameLengthFlag);
            sb.append(", dependsOnCoreCoder=").append(dependsOnCoreCoder);
            sb.append(", coreCoderDelay=").append(coreCoderDelay);
            sb.append(", extensionFlag=").append(extensionFlag);
            sb.append(", layerNr=").append(layerNr);
            sb.append(", numOfSubFrame=").append(numOfSubFrame);
            sb.append(", layer_length=").append(layer_length);
            sb.append(", aacSectionDataResilienceFlag=").append(aacSectionDataResilienceFlag);
            sb.append(", aacScalefactorDataResilienceFlag=").append(aacScalefactorDataResilienceFlag);
            sb.append(", aacSpectralDataResilienceFlag=").append(aacSpectralDataResilienceFlag);
            sb.append(", extensionFlag3=").append(extensionFlag3);
        }
        if (parametricSpecificConfig) {
            sb.append(", isBaseLayer=").append(isBaseLayer);
            sb.append(", paraMode=").append(paraMode);
            sb.append(", paraExtensionFlag=").append(paraExtensionFlag);
            sb.append(", hvxcVarMode=").append(hvxcVarMode);
            sb.append(", hvxcRateMode=").append(hvxcRateMode);
            sb.append(", erHvxcExtensionFlag=").append(erHvxcExtensionFlag);
            sb.append(", var_ScalableFlag=").append(var_ScalableFlag);
            sb.append(", hilnQuantMode=").append(hilnQuantMode);
            sb.append(", hilnMaxNumLine=").append(hilnMaxNumLine);
            sb.append(", hilnSampleRateCode=").append(hilnSampleRateCode);
            sb.append(", hilnFrameLength=").append(hilnFrameLength);
            sb.append(", hilnContMode=").append(hilnContMode);
            sb.append(", hilnEnhaLayer=").append(hilnEnhaLayer);
            sb.append(", hilnEnhaQuantMode=").append(hilnEnhaQuantMode);
        }
        sb.append('}');
        return sb.toString();
    }

    static {
        // sampling_frequency_index sampling frequeny
//0x0 96000
//0x1 88200
//0x2 64000
//0x3 48000
//0x4 44100
//0x5 32000
//0x6 24000
//0x7 22050
//0x8 16000
//0x9 12000
//0xa 11025
//0xb 8000
//0xc reserved
//0xd reserved
//0xe reserved
//0xf reserved
        samplingFrequencyIndexMap.put(0x0, 96000);
        samplingFrequencyIndexMap.put(0x1, 88200);
        samplingFrequencyIndexMap.put(0x2, 64000);
        samplingFrequencyIndexMap.put(0x3, 48000);
        samplingFrequencyIndexMap.put(0x4, 44100);
        samplingFrequencyIndexMap.put(0x5, 32000);
        samplingFrequencyIndexMap.put(0x6, 24000);
        samplingFrequencyIndexMap.put(0x7, 22050);
        samplingFrequencyIndexMap.put(0x8, 16000);
        samplingFrequencyIndexMap.put(0x9, 12000);
        samplingFrequencyIndexMap.put(0xa, 11025);
        samplingFrequencyIndexMap.put(0xb, 8000);

        /* audioObjectType IDs
          0 Null
        1 AAC main X X
        2 AAC LC X X X X X X X
        3 AAC SSR X X
        4 AAC LTP X X X X
        5 SBR X X
        6 AAC Scalable X X X X
        7 TwinVQ X X X
        8 CELP X X X X X X
        9 HVXC X X X X X
        10 (reserved)
        11 (reserved)
        12 TTSI X X X X X X
        13 Main synthetic X X
        14 Wavetable synthesis X* X*
        15 General MIDI X* X*
        16 Algorithmic Synthesis and Audio FX X* X*
        17 ER AAC LC X X X
        18 (reserved)
        19 ER AAC LTP X X
        20 ER AAC Scalable X X X
        21 ER TwinVQ X X
        22 ER BSAC X X
        23 ER AAC LD X X X X
        24 ER CELP X X X
        25 ER HVXC X X
        26 ER HILN X
        27 ER Parametric X
        28 SSC
        29 PS X
        30 MPEG Surround
        31 (escape)
        32 Layer-1
        33 Layer-2
        34 Layer-3
        35 DST
        36 ALS
        37 SLS
        38 SLS non-core
        39 ER AAC ELD
        40 SMR Simple
        41 SMR Main
        */
        audioObjectTypeMap.put(1, "AAC main");
        audioObjectTypeMap.put(2, "AAC LC");
        audioObjectTypeMap.put(3, "AAC SSR");
        audioObjectTypeMap.put(4, "AAC LTP");
        audioObjectTypeMap.put(5, "SBR");
        audioObjectTypeMap.put(6, "AAC Scalable");
        audioObjectTypeMap.put(7, "TwinVQ");
        audioObjectTypeMap.put(8, "CELP");
        audioObjectTypeMap.put(9, "HVXC");
        audioObjectTypeMap.put(10, "(reserved)");
        audioObjectTypeMap.put(11, "(reserved)");
        audioObjectTypeMap.put(12, "TTSI");
        audioObjectTypeMap.put(13, "Main synthetic");
        audioObjectTypeMap.put(14, "Wavetable synthesis");
        audioObjectTypeMap.put(15, "General MIDI");
        audioObjectTypeMap.put(16, "Algorithmic Synthesis and Audio FX");
        audioObjectTypeMap.put(17, "ER AAC LC");
        audioObjectTypeMap.put(18, "(reserved)");
        audioObjectTypeMap.put(19, "ER AAC LTP");
        audioObjectTypeMap.put(20, "ER AAC Scalable");
        audioObjectTypeMap.put(21, "ER TwinVQ");
        audioObjectTypeMap.put(22, "ER BSAC");
        audioObjectTypeMap.put(23, "ER AAC LD");
        audioObjectTypeMap.put(24, "ER CELP");
        audioObjectTypeMap.put(25, "ER HVXC");
        audioObjectTypeMap.put(26, "ER HILN");
        audioObjectTypeMap.put(27, "ER Parametric");
        audioObjectTypeMap.put(28, "SSC");
        audioObjectTypeMap.put(29, "PS");
        audioObjectTypeMap.put(30, "MPEG Surround");
        audioObjectTypeMap.put(31, "(escape)");
        audioObjectTypeMap.put(32, "Layer-1");
        audioObjectTypeMap.put(33, "Layer-2");
        audioObjectTypeMap.put(34, "Layer-3");
        audioObjectTypeMap.put(35, "DST");
        audioObjectTypeMap.put(36, "ALS");
        audioObjectTypeMap.put(37, "SLS");
        audioObjectTypeMap.put(38, "SLS non-core");
        audioObjectTypeMap.put(39, "ER AAC ELD");
        audioObjectTypeMap.put(40, "SMR Simple");
        audioObjectTypeMap.put(41, "SMR Main");

        /* profileLevelIds
       0x00 Reserved for ISO use -
     0x01 Main Audio Profile L1
     0x02 Main Audio Profile L2
     0x03 Main Audio Profile L3
     0x04 Main Audio Profile L4
     0x05 Scalable Audio Profile L1
     0x06 Scalable Audio Profile L2
     0x07 Scalable Audio Profile L3
     0x08 Scalable Audio Profile L4
     0x09 Speech Audio Profile L1
     0x0A Speech Audio Profile L2
     0x0B Synthetic Audio Profile L1
     0x0C Synthetic Audio Profile L2
     0x0D Synthetic Audio Profile L3
     0x0E High Quality Audio Profile L1
     0x0F High Quality Audio Profile L2
     0x10 High Quality Audio Profile L3
     0x11 High Quality Audio Profile L4
     0x12 High Quality Audio Profile L5
     0x13 High Quality Audio Profile L6
     0x14 High Quality Audio Profile L7
     0x15 High Quality Audio Profile L8
     0x16 Low Delay Audio Profile L1
     0x17 Low Delay Audio Profile L2
     0x18 Low Delay Audio Profile L3
     0x19 Low Delay Audio Profile L4
     0x1A Low Delay Audio Profile L5
     0x1B Low Delay Audio Profile L6
     0x1C Low Delay Audio Profile L7
     0x1D Low Delay Audio Profile L8
     0x1E Natural Audio Profile L1
     0x1F Natural Audio Profile L2
     0x20 Natural Audio Profile L3
     0x21 Natural Audio Profile L4
     0x22 Mobile Audio Internetworking Profile L1
     0x23 Mobile Audio Internetworking Profile L2
     0x24 Mobile Audio Internetworking Profile L3
     0x25 Mobile Audio Internetworking Profile L4
     0x26 Mobile Audio Internetworking Profile L5
     0x27 Mobile Audio Internetworking Profile L6
     0x28 AAC Profile L1
     0x29 AAC Profile L2
     0x2A AAC Profile L4
     0x2B AAC Profile L5
     0x2C High Efficiency AAC Profile L2
     0x2D High Efficiency AAC Profile L3
     0x2E High Efficiency AAC Profile L4
     0x2F High Efficiency AAC Profile L5
     0x30 High Efficiency AAC v2 Profile L2
     0x31 High Efficiency AAC v2 Profile L3
     0x32 High Efficiency AAC v2 Profile L4
     0x33 High Efficiency AAC v2 Profile L5
     0x34 Low Delay AAC Profile L1
     0x35 Baseline MPEG Surround Profile (see ISO/IEC
     23003-1)
     L1
     0x36 Baseline MPEG Surround Profile (see ISO/IEC
     23003-1)
     L2
     0x37 Baseline MPEG Surround Profile (see ISO/IEC
     23003-1)
     L3
     0x38 Baseline MPEG Surround Profile (see ISO/IEC
     23003-1)
     L4
     0c39 Baseline MPEG Surround Profile (see ISO/IEC
     23003-1)
     L5
     0x3A Baseline MPEG Surround Profile (see ISO/IEC
     23003-1)
     L6
     0x3B - 0x7F reserved for ISO use -
     0x80 - 0xFD user private -
     0xFE no audio profile specified -
     0xFF no audio capability required -

        */
    }


    public int getSamplingFrequency() {
        return samplingFrequencyIndex == 0xf ? samplingFrequency : samplingFrequencyIndexMap.get(samplingFrequencyIndex);
    }

    public int getChannelConfiguration() {
        return channelConfiguration;
    }
}
