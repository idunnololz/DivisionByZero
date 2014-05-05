LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := fix-GLES20
LOCAL_CFLAGS    := -Werror
LOCAL_SRC_FILES := fix-GLES20.c
LOCAL_LDLIBS    := -lGLESv2

include $(BUILD_SHARED_LIBRARY)
