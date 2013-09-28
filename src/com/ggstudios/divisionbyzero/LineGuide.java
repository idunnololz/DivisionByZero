package com.ggstudios.divisionbyzero;

import com.ggstudios.utils.BufferUtils;

import android.opengl.GLES20;

import static fix.android.opengl.GLES20.glVertexAttribPointer;

public class LineGuide extends Drawable{
	
	private float x, y;
	private int horizontalHandle, verticalHandle;
	private int textureHandle;
	
	private boolean isVisible = true;
	
	public LineGuide() {
		setupBuffers();
	}
	
	private void setupBuffers() {
		final float lineWidth = Core.SDP * 0.05f;
		final float horizontalVertices[] = {
				0, 	0, 	//Vertex 0
				lineWidth, 0,
				0, 	Core.canvasHeight, 	//v1
				lineWidth, Core.canvasHeight
		};
		final float verticalVertices[] = {
				0, 	0, 	//Vertex 0
				Core.canvasWidth, 	0, 	//v1
				0, 	lineWidth,
				Core.canvasWidth, 	lineWidth,
		};

		horizontalHandle = BufferUtils.copyToBuffer(horizontalVertices);
		verticalHandle = BufferUtils.copyToBuffer(verticalVertices);
		
		textureHandle = Core.tm.get(R.drawable.white);
	}

	public void setXY(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public void hide() {
		isVisible = false;
	}
	
	public void show() {
		isVisible = true;
	}

	@Override
	public void draw(float offX, float offY) {
		if (!isVisible) return;
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

		GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1f, 0f, 0f, 1f);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, horizontalHandle);
		glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, 0, 0); 
		
		Utils.resetMatrix();
		Utils.translateAndCommit(x, 0);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticalHandle);
		glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, 0, 0); 
		
		Utils.translateAndCommit(-x, y);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1, 1, 1, 1);
	}

	@Override
	public void refresh() {
		setupBuffers();
	}

}
