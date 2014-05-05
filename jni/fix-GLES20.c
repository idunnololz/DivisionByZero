#include <jni.h>
#include <GLES2/gl2.h>
//#include <GLES2/gl2ext.h>

void Java_fix_android_opengl_GLES20_glVertexAttribPointer
  (JNIEnv *env, jclass c, jint index, jint size, jint type, jboolean normalized, jint stride, jint offset)
{
	glVertexAttribPointer(index, size, type, normalized, stride, (void*) offset);
}

void Java_fix_android_opengl_GLES20_glDrawElements
  (JNIEnv *env, jclass c, jint mode, jint count, jint type, jint offset)
{
	glDrawElements(mode, count, type, (void*) offset);
}