package com.ggstudios.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.GLES20;

public class BufferUtils {
	private static final int TYPE_SIZE = 4;
	private static final int SHORT_SIZE = Short.SIZE / 8;

	public static int createRectangleBuffer(float w, float h){
		float arr[] = {
				0, 0,
				w, 0,
				0, h,
				w, h
		};

		return copyToBuffer(arr);
	}

	public static int copyToBuffer(float[] arr){
		return copyToBuffer(arr, arr.length);
	}

	public static int copyToBuffer(float[] arr, int len) {
		// allocate a buffer of the right size...

		// size of float = 4 bytes so size of buffer = 4 * length of float array
		FloatBuffer floatBuf = ByteBuffer.allocateDirect(len * TYPE_SIZE)
				.order(ByteOrder.nativeOrder())		// use the device hardware's native byte order
				.asFloatBuffer(); 					// create a floating point buffer from the ByteBuffer

		floatBuf.put(arr, 0, len)				// copy the array into the buffer
		.position(0);							// set the buffer to read the first coordinate

		int err;
		if((err = GLES20.glGetError()) != 0) {
			DebugLog.e("buff", "err " + err);
		}
		
		int[] buffer = new int [1];
		GLES20.glGenBuffers(1, buffer, 0);
		int handle = buffer[0];
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, handle);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, len * TYPE_SIZE, floatBuf, GLES20.GL_STATIC_DRAW);

		if((err = GLES20.glGetError()) != 0) {
			DebugLog.e("buff", "err " + err);
		}
		
		return handle;
	}
	
	public static int copyToBuffer(int target, short[] arr, int len) {
		// allocate a buffer of the right size...

		// size of float = 4 bytes so size of buffer = 4 * length of float array
		ShortBuffer buf = ByteBuffer.allocateDirect(len * SHORT_SIZE)
				.order(ByteOrder.nativeOrder())		// use the device hardware's native byte order
				.asShortBuffer(); 					// create a floating point buffer from the ByteBuffer

		buf.put(arr, 0, len)								// copy the array into the buffer
		.position(0);							// set the buffer to read the first coordinate

		int[] buffer = new int [1];
		GLES20.glGenBuffers(1, buffer, 0);
		int handle = buffer[0];
		GLES20.glBindBuffer(target, handle);
		GLES20.glBufferData(target, len * SHORT_SIZE, buf, GLES20.GL_STATIC_DRAW);
		
		return handle;
	}
}
