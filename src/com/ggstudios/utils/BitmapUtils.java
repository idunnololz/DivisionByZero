package com.ggstudios.utils;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class BitmapUtils {
	public static int loadBitmap(Bitmap bitmap) {
		return loadBitmap(bitmap, -1, true);
	}
	
	public static int loadBitmap(Bitmap bitmap, int oldHandle) {
		return loadBitmap(bitmap, oldHandle, true);
	}
	
	/**
	 * Loads a bitmap into memory then discards the bitmap.
	 * @param bitmap
	 * @param oldHandle	Handle to use.
	 * @return
	 */
	public static int loadBitmap(Bitmap bitmap, int oldHandle, boolean recycle) {
		int h;
		
		int handle[] = new int[1];
		GLES20.glGenTextures(1, handle, 0);
		h = handle[0];
//		
//		if(oldHandle <= 0) {
//			int handle[] = new int[1];
//			GLES20.glGenTextures(1, handle, 0);
//			h = handle[0];
//		} else {
//			h = oldHandle;
//		}
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, h);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

		if(recycle)
			bitmap.recycle();
		
		return h;
	}
}
