LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
include $(LOCAL_PATH)/Config.mk

LOCAL_MODULE := aac-encoder

ENC_SRC := src

LOCAL_C_INCLUDES := $(LOCAL_PATH)/inc

LOCAL_SRC_FILES = \
    aac-enc.c \
    $(ENC_SRC)/cmnMemory.c \
    basic_op/basicop2.c \
    basic_op/oper_32b.c \
    $(ENC_SRC)/aac_rom.c \
    $(ENC_SRC)/aacenc.c \
    $(ENC_SRC)/aacenc_core.c \
    $(ENC_SRC)/adj_thr.c \
    $(ENC_SRC)/band_nrg.c \
    $(ENC_SRC)/bit_cnt.c \
    $(ENC_SRC)/bitbuffer.c \
    $(ENC_SRC)/bitenc.c \
    $(ENC_SRC)/block_switch.c \
    $(ENC_SRC)/channel_map.c \
    $(ENC_SRC)/dyn_bits.c \
    $(ENC_SRC)/grp_data.c \
    $(ENC_SRC)/interface.c \
    $(ENC_SRC)/line_pe.c \
    $(ENC_SRC)/memalign.c \
    $(ENC_SRC)/ms_stereo.c \
    $(ENC_SRC)/pre_echo_control.c \
    $(ENC_SRC)/psy_configuration.c \
    $(ENC_SRC)/psy_main.c \
    $(ENC_SRC)/qc_main.c \
    $(ENC_SRC)/quantize.c \
    $(ENC_SRC)/sf_estim.c \
    $(ENC_SRC)/spreading.c \
    $(ENC_SRC)/stat_bits.c \
    $(ENC_SRC)/tns.c \
    $(ENC_SRC)/transform.c

ifeq ($(VOTT), v5)
LOCAL_SRC_FILES += \
	$(ENC_SRC)/asm/ARMV5E/AutoCorrelation_v5.s \
	$(ENC_SRC)/asm/ARMV5E/band_nrg_v5.s \
	$(ENC_SRC)/asm/ARMV5E/CalcWindowEnergy_v5.s \
	$(ENC_SRC)/asm/ARMV5E/PrePostMDCT_v5.s \
	$(ENC_SRC)/asm/ARMV5E/R4R8First_v5.s \
	$(ENC_SRC)/asm/ARMV5E/Radix4FFT_v5.s
endif

ifeq ($(VOTT), v7)
LOCAL_SRC_FILES += \
	$(ENC_SRC)/asm/ARMV5E/AutoCorrelation_v5.s \
	$(ENC_SRC)/asm/ARMV5E/band_nrg_v5.s \
	$(ENC_SRC)/asm/ARMV5E/CalcWindowEnergy_v5.s \
	$(ENC_SRC)/asm/ARMV7/PrePostMDCT_v7.s \
	$(ENC_SRC)/asm/ARMV7/R4R8First_v7.s \
	$(ENC_SRC)/asm/ARMV7/Radix4FFT_v7.s
endif

LOCAL_ARM_MODE := arm

LOCAL_LDLIBS := -llog

LOCAL_STATIC_LIBRARIES := 
LOCAL_SHARED_LIBRARIES :=

LOCAL_CFLAGS := $(VO_CFLAGS)

ifeq ($(VOTT), v5)
LOCAL_CFLAGS += -DARMV5E -DARM_INASM -DARMV5_INASM
LOCAL_C_INCLUDES += $(ENC_SRC)/asm/ARMV5E
endif

ifeq ($(VOTT), v7)
LOCAL_CFLAGS += -DARMV5E -DARMV7Neon -DARM_INASM -DARMV5_INASM -DARMV6_INASM
LOCAL_C_INCLUDES += $(ENC_SRC)/asm/ARMV5E
LOCAL_C_INCLUDES += $(ENC_SRC)/asm/ARMV7
endif

include $(BUILD_SHARED_LIBRARY)

