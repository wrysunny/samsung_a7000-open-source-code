#
# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH:= $(call my-dir)


include $(CLEAR_VARS)
LOCAL_PACKAGE_NAME := HybridRadio2016_M
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, src)
#LOCAL_SRC_FILES += src/com/sec/android/app/fm/listplayer/IFMListPlayerService.aidl
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

ifneq ($(SEC_DEVICE_RESOLUTION_OVERLAYS),)
overlaydirectory := $(wildcard $(LOCAL_PATH)/res_$(SEC_DEVICE_RESOLUTION_OVERLAYS)/res)
ifneq ($(overlaydirectory),)
LOCAL_RESOURCE_OVERLAY_DIR += $(LOCAL_PATH)/res_$(SEC_DEVICE_RESOLUTION_OVERLAYS)/res
endif
endif

# widget resize file overay for Over 5.5inch Delta widget support
ifneq ($(filter a8% a7x% a9x%, $(TARGET_PRODUCT)),)
LOCAL_RESOURCE_OVERLAY_DIR += $(LOCAL_PATH)/WIDGET_OVER_55INCH_DELTA_SUPPORT/res
ifneq ($(filter a8% a9x%, $(TARGET_PRODUCT)),)
LOCAL_RESOURCE_OVERLAY_DIR += $(LOCAL_PATH)/WIDGET_OVER_55INCH_DELTA_SUPPORT_EXTRA_CELL_HEIGHT/res
endif
endif

#Additional resources files for dark or light AMOLED
ifeq (true,$(call spf_check,SEC_PRODUCT_FEATURE_LCD_SUPPORT_AMOLED_DISPLAY,FALSE))
    LOCAL_RESOURCE_OVERLAY_DIR += $(LOCAL_PATH)/res_overlay/res_tft/res
endif

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 \
        dnsjava
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-appcompat
LOCAL_STATIC_JAVA_LIBRARIES += android-support-design

LOCAL_RESOURCE_DIR += frameworks/support/v7/appcompat/res
LOCAL_RESOURCE_DIR += frameworks/support/design/res

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_JAVA_LIBRARIES := secmediarecorder sec_feature SecProductFeature_FMRADIO
LOCAL_JAVA_LIBRARIES += SecProductFeature_RIL SecProductFeature_COMMON
LOCAL_AAPT_FLAGS += $(SEC_DEV_APP_LOCAL_AAPT_FLAGS)
LOCAL_AAPT_FLAGS += --auto-add-overlay --extra-packages android.support.v7.appcompat --extra-packages android.support.design
include $(BUILD_TW_PACKAGE)

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := dnsjava2016M:libs/dnsjava-2.1.3.jar
include $(BUILD_MULTI_PREBUILT)
