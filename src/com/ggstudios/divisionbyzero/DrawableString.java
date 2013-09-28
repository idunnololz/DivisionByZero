package com.ggstudios.divisionbyzero;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.ggstudios.divisionbyzero.FontManager.TextureRegion;
import com.ggstudios.utils.BufferUtils;
import com.ggstudios.utils.DebugLog;

import android.opengl.GLES20;

import static fix.android.opengl.GLES20.glVertexAttribPointer;
import static fix.android.opengl.GLES20.glDrawElements;;

public class DrawableString extends Drawable{
	private static final String TAG = "DrawableString";

	private static final int MAX_STRING_LEN = 15;

	public static final int 
	ALIGN_RIGHT = 1, ALIGN_LEFT = 2, ALIGN_CENTER = 3;

	private float[] spacing = new float[MAX_STRING_LEN];
	private float[] vertexBuffer = new float[MAX_STRING_LEN * 16];

	private int alignment = ALIGN_LEFT;

	int width, height;

	float x, y;

	private FontManager fm;

	private String text;

	private int bufferHandle;

	private FloatBuffer floatBuf;
	
	private float scale = 1f;

	private Runnable rebuild = new Runnable() {

		@Override
		public void run() {
			build();
		}

	};

	/**
	 * Creates a drawable string using dynamic string technology
	 * @param input	The string to draw
	 * @param x		X Position
	 * @param y		Y Position
	 * @param fm	Font to use
	 */

	public DrawableString(float x, float y, FontManager fm, String input){
		this.text = input;
		this.fm = fm;
		width = fm.getWidth(text);

		this.x = x;
		this.y = y;

		floatBuf = ByteBuffer.allocateDirect(vertexBuffer.length * 4)
				.order(ByteOrder.nativeOrder())		// use the device hardware's native byte order
				.asFloatBuffer(); 					// create a floating point buffer from the ByteBuffer

		build();
	}

	public DrawableString(float x, float y, FontManager fm, String input, int alignment){
		this.text = input;
		this.fm = fm;
		this.alignment = alignment;
		width = fm.getWidth(text);

		this.x = x;
		this.y = y;

		floatBuf = ByteBuffer.allocateDirect(vertexBuffer.length * 4)
				.order(ByteOrder.nativeOrder())		// use the device hardware's native byte order
				.asFloatBuffer(); 					// create a floating point buffer from the ByteBuffer

		build();
	}

	private void build(){
		bufferIndex = 0;

		float x = 0, y = 0;

		for(int i = 0; i < text.length(); i++){
			char c = text.charAt(i);
			if(c == '\n'){
				x = 0;
				y += fm.cellH;
				spacing[i] = 0;
				continue;
			}

			spacing[i] = fm.charWidth[c];

			if(x + spacing[i] > width && c != ' ') {
				x = 0;
				y += fm.cellH;
				spacing[i] = 0;
				i--;
				continue;
			}

			drawSprite(x, y, fm.cellW, fm.cellH, fm.charRegion[c]);

			x += spacing[i];
		}

		floatBuf.put(vertexBuffer, 0, bufferIndex)				// copy the array into the buffer
		.position(0);							// set the buffer to read the first coordinate

		int[] buffer = new int [1];
		GLES20.glGenBuffers(1, buffer, 0);
		bufferHandle = buffer[0];
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferHandle);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, bufferIndex * 4, floatBuf, GLES20.GL_STATIC_DRAW);
	}

	private int bufferIndex = 0;
	private void drawSprite(float x, float y, float width, float height, TextureRegion region)  {
		float x1 = x;
		float y1 = y;
		float x2 = x + width;
		float y2 = y + height;

		vertexBuffer[bufferIndex++] = x1;               // Add X for Vertex 0
		vertexBuffer[bufferIndex++] = y1;               // Add Y for Vertex 0
		vertexBuffer[bufferIndex++] = region.u1;        // Add U for Vertex 0
		vertexBuffer[bufferIndex++] = region.v1;        // Add V for Vertex 0

		vertexBuffer[bufferIndex++] = x2;               // Add X for Vertex 1
		vertexBuffer[bufferIndex++] = y1;               // Add Y for Vertex 1
		vertexBuffer[bufferIndex++] = region.u2;        // Add U for Vertex 1
		vertexBuffer[bufferIndex++] = region.v1;        // Add V for Vertex 1

		vertexBuffer[bufferIndex++] = x2;               // Add X for Vertex 2
		vertexBuffer[bufferIndex++] = y2;               // Add Y for Vertex 2
		vertexBuffer[bufferIndex++] = region.u2;        // Add U for Vertex 2
		vertexBuffer[bufferIndex++] = region.v2;        // Add V for Vertex 2

		vertexBuffer[bufferIndex++] = x1;               // Add X for Vertex 3
		vertexBuffer[bufferIndex++] = y2;               // Add Y for Vertex 3
		vertexBuffer[bufferIndex++] = region.u1;        // Add U for Vertex 3
		vertexBuffer[bufferIndex++] = region.v2;        // Add V for Vertex 3
	}

	@Override
	public void draw(float offX, float offY){
		final float fx = x + offX;
		final float fy = y + offY;

		Utils.resetMatrix();
		switch(alignment) {
		case ALIGN_RIGHT:
			Utils.translate(fx - width, fy);
			break;
		case ALIGN_LEFT:
			Utils.translate(fx, fy);
			break;
		case ALIGN_CENTER:
			Utils.translate(fx - (width / 2.0f), fy);
			break;
		}
		Utils.scale(scale);
		GLES20.glUniformMatrix4fv(Core.U_TRANSLATION_MATRIX_HANDLE, 1, false, Core.matrix, 0);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fm.textureHandle);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferHandle);
		glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, 4 * 4, 0);
		glVertexAttribPointer(Core.A_TEX_COORD_HANDLE, 2, GLES20.GL_FLOAT, false, 4 * 4, 2 * 4);

		glDrawElements(GLES20.GL_TRIANGLES, text.length() * 6, GLES20.GL_UNSIGNED_SHORT, 0);
		Core.gr.restoreTextureHandle();
	}

	@Override
	public void refresh(){
		build();
	}

	public void setText(String string) {		
		setText(string, fm.getWidth(string));
	}

	public void setText(String string, int maxWidth) {
		if(string.length() > MAX_STRING_LEN) {
			DebugLog.e(TAG, "Error. Text set is longer than max string length.");
		}

		this.text = string;

		this.width = maxWidth;

		Core.glView.queueEvent(rebuild);
	}
	
	public void setTextSize(float size) {
		scale = size / fm.getFontSize();
	}
}
