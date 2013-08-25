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

import com.coremedia.iso.IsoTypeReader;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/* class tag values of 14496-1
0x00 Forbidden
0x01 ObjectDescrTag
0x02 InitialObjectDescrTag
0x03 ES_DescrTag
0x04 DecoderConfigDescrTag
0x05 DecSpecificInfoTag
0x06 SLConfigDescrTag
0x07 ContentIdentDescrTag
0x08 SupplContentIdentDescrTag
0x09 IPI_DescrPointerTag
0x0A IPMP_DescrPointerTag
0x0B IPMP_DescrTag
0x0C QoS_DescrTag
0x0D RegistrationDescrTag
0x0E ES_ID_IncTag
0x0F ES_ID_RefTag
0x10 MP4_IOD_Tag
0x11 MP4_OD_Tag
0x12 IPL_DescrPointerRefTag
0x13 ExtensionProfileLevelDescrTag
0x14 profileLevelIndicationIndexDescrTag
0x15-0x3F Reserved for ISO use
0x40 ContentClassificationDescrTag
0x41 KeyWordDescrTag
0x42 RatingDescrTag
0x43 LanguageDescrTag
0x44 ShortTextualDescrTag
0x45 ExpandedTextualDescrTag
0x46 ContentCreatorNameDescrTag
0x47 ContentCreationDateDescrTag
0x48 OCICreatorNameDescrTag
0x49 OCICreationDateDescrTag
0x4A SmpteCameraPositionDescrTag
0x4B SegmentDescrTag
0x4C MediaTimeDescrTag
0x4D-0x5F Reserved for ISO use (OCI extensions)
0x60 IPMP_ToolsListDescrTag
0x61 IPMP_ToolTag
0x62 M4MuxTimingDescrTag
0x63 M4MuxCodeTableDescrTag
0x64 ExtSLConfigDescrTag
0x65 M4MuxBufferSizeDescrTag
0x66 M4MuxIdentDescrTag
0x67 DependencyPointerTag
0x68 DependencyMarkerTag
0x69 M4MuxChannelDescrTag
0x6A-0xBF Reserved for ISO use
0xC0-0xFE User private
0xFF Forbidden
 */

/* objectTypeIndication as of 14496-1
0x00 Forbidden
0x01 Systems ISO/IEC 14496-1 a
0x02 Systems ISO/IEC 14496-1 b
0x03 Interaction Stream
0x04 Systems ISO/IEC 14496-1 Extended BIFS Configuration c
0x05 Systems ISO/IEC 14496-1 AFX d
0x06 Font Data Stream
0x07 Synthesized Texture Stream
0x08 Streaming Text Stream
0x09-0x1F reserved for ISO use
0x20 Visual ISO/IEC 14496-2 e
0x21 Visual ITU-T Recommendation H.264 | ISO/IEC 14496-10 f
0x22 Parameter Sets for ITU-T Recommendation H.264 | ISO/IEC 14496-10 f
0x23-0x3F reserved for ISO use
0x40 Audio ISO/IEC 14496-3 g
0x41-0x5F reserved for ISO use
0x60 Visual ISO/IEC 13818-2 Simple Profile
0x61 Visual ISO/IEC 13818-2 Main Profile
0x62 Visual ISO/IEC 13818-2 SNR Profile
0x63 Visual ISO/IEC 13818-2 Spatial Profile
0x64 Visual ISO/IEC 13818-2 High Profile
0x65 Visual ISO/IEC 13818-2 422 Profile
0x66 Audio ISO/IEC 13818-7 Main Profile
0x67 Audio ISO/IEC 13818-7 LowComplexity Profile
0x68 Audio ISO/IEC 13818-7 Scaleable Sampling Rate Profile
0x69 Audio ISO/IEC 13818-3
0x6A Visual ISO/IEC 11172-2
0x6B Audio ISO/IEC 11172-3
0x6C Visual ISO/IEC 10918-1
0x6D reserved for registration authority
0x6E Visual ISO/IEC 15444-1
0x6F - 0x9F reserved for ISO use
0xA0 - 0xBF reserved for registration authority i
0xC0 - 0xE0 user private
0xE1 reserved for registration authority i
0xE2 - 0xFE user private
0xFF no object type specified h
 */
public class ObjectDescriptorFactory {
    protected static Logger log = Logger.getLogger(ObjectDescriptorFactory.class.getName());

    protected static Map<Integer, Map<Integer, Class<? extends BaseDescriptor>>> descriptorRegistry = new HashMap<Integer, Map<Integer, Class<? extends BaseDescriptor>>>();

    static {
        Set<Class<? extends BaseDescriptor>> annotated = new HashSet<Class<? extends BaseDescriptor>>();

        annotated.add(DecoderSpecificInfo.class);
        annotated.add(SLConfigDescriptor.class);
        annotated.add(BaseDescriptor.class);
        annotated.add(ExtensionDescriptor.class);
        annotated.add(ObjectDescriptorBase.class);
        annotated.add(ProfileLevelIndicationDescriptor.class);
        annotated.add(AudioSpecificConfig.class);
        annotated.add(ExtensionProfileLevelDescriptor.class);
        annotated.add(ESDescriptor.class);
        annotated.add(DecoderConfigDescriptor.class);
        //annotated.add(ObjectDescriptor.class);

        for (Class<? extends BaseDescriptor> clazz : annotated) {
            final Descriptor descriptor = clazz.getAnnotation(Descriptor.class);
            final int[] tags = descriptor.tags();
            final int objectTypeInd = descriptor.objectTypeIndication();

            Map<Integer, Class<? extends BaseDescriptor>> tagMap = descriptorRegistry.get(objectTypeInd);
            if (tagMap == null) {
                tagMap = new HashMap<Integer, Class<? extends BaseDescriptor>>();
            }
            for (int tag : tags) {
                tagMap.put(tag, clazz);
            }
            descriptorRegistry.put(objectTypeInd, tagMap);
        }
    }

    public static BaseDescriptor createFrom(int objectTypeIndication, ByteBuffer bb) throws IOException {
        int tag = IsoTypeReader.readUInt8(bb);

        Map<Integer, Class<? extends BaseDescriptor>> tagMap = descriptorRegistry.get(objectTypeIndication);
        if (tagMap == null) {
            tagMap = descriptorRegistry.get(-1);
        }
        Class<? extends BaseDescriptor> aClass = tagMap.get(tag);

//    if (tag == 0x00) {
//      log.warning("Found illegal tag 0x00! objectTypeIndication " + Integer.toHexString(objectTypeIndication) +
//              " and tag " + Integer.toHexString(tag) + " using: " + aClass);
//      aClass = BaseDescriptor.class;
//    }

        BaseDescriptor baseDescriptor;
        if (aClass == null || aClass.isInterface() || Modifier.isAbstract(aClass.getModifiers())) {
            log.warning("No ObjectDescriptor found for objectTypeIndication " + Integer.toHexString(objectTypeIndication) +
                    " and tag " + Integer.toHexString(tag) + " found: " + aClass);
            baseDescriptor = new UnknownDescriptor();
        } else {
            try {
                baseDescriptor = aClass.newInstance();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Couldn't instantiate BaseDescriptor class " + aClass + " for objectTypeIndication " + objectTypeIndication + " and tag " + tag, e);
                throw new RuntimeException(e);
            }
        }
        baseDescriptor.parse(tag, bb);
        return baseDescriptor;
    }
}
