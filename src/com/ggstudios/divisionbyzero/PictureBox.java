package com.ggstudios.divisionbyzero;

import com.ggstudios.utils.BufferUtils;

import android.opengl.GLES20;

import static fix.android.opengl.GLES20.glVertexAttribPointer;

public class PictureBox extends Drawable {
	protected float x, y;
	protected float w, h;
	protected float drawingW, drawingH;
	protected boolean center = false;

	protected int textureHandle = -1, textureId = -1;

	protected int handle = 0;

	protected float transparency = 1.0f;

	protected boolean isVisible = true;
	protected boolean cull = false;

	protected VBO vbo;

	private float scale = 1f;
	private float angle = 0f;

	/**
	 * Constructor used to produce a empty PictureBox. Additional calls 
	 * are required before this object becomes usable.
	 * @param x
	 * @param y
	 */
	protected PictureBox(float x, float y){
		this.x = x;
		this.y = y;
	}

	public PictureBox(float x, float y, float w, float h, int resId, boolean isCentered){
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.textureId = resId;
		center = isCentered;

		refresh();
	}
	
	public PictureBox(float x, float y, float w, float h, int resId){
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.textureId = resId;

		refresh();
	}

	public PictureBox(float x, float y, VBO vbo, int resId){
		this.x = x;
		this.y = y;
		this.vbo = vbo;
		this.textureId = resId;

		refresh();
	}

	public void setVBO(VBO vbo) {
		this.vbo = vbo;
		w = vbo.width;
		h = vbo.height;

		handle = vbo.handle;		
	}

	public void setTexture(int resId){
		this.textureId = resId;
		textureHandle = Core.tm.get(resId);
	}

	public void setTextureHandle(int textureHandle) {
		textureId = -1;
		this.textureHandle = textureHandle;
	}

	public void generateBuffer(){
		vbo = null;

		float w, h;
		if(drawingW <= 0) {
			w = this.w;
			h = this.h;
		} else {
			w = drawingW;
			h = drawingH;
		}
		
		if(center) {
			float hw = w/2f;
			float hh = h/2f;
			float vertices[] = {
					//Vertices according to faces
					-hw, 	-hh, 	//Vertex 0
					hw, 	-hh, 	//v1
					-hw, 	hh, 	//v2
					hw, 	hh, 	//v3
			};

			handle = BufferUtils.copyToBuffer(vertices);
		} else {
			float vertices[] = {
					//Vertices according to faces
					0, 	0, 	//Vertex 0
					w, 	0, 	//v1
					0, 	h, 	//v2
					w, 	h, 	//v3
			};

			handle = BufferUtils.copyToBuffer(vertices);
		}
	}

	public void refresh(){
		if(this.vbo != null) {
			this.w = vbo.width;
			this.h = vbo.height;

			handle = vbo.handle;
		} else {
			generateBuffer();
		}

		if(textureId >= 0) {
			textureHandle = Core.tm.get(textureId);
		}
	}

	public void reset(){
		transparency = 1.0f;
	}

	public void draw(float offX, float offY) {
		final float finalX = x + offX;
		final float finalY = y + offY;

		if ( (finalX + w < 0
				|| finalX > Core.cullR 
				|| finalY + h < 0
				|| finalY > Core.cullB
				|| textureHandle == -1 
				|| !isVisible) 
				&& !Core.forceVisible) {
			// cull this drawable...
			cull = true;
		} else {
			cull = false;
		}

		if(!cull) {
			Utils.resetMatrix();
			Utils.scale(scale);

			if(angle != 0f)
				Utils.rotate(angle);

			Utils.translateAndCommit(finalX, finalY);

			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, handle);
			glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, 0, 0);

			if(transparency < 1.0f){
				GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1f, 1f, 1f, transparency);
				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
				GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1f, 1f, 1f, 1f);
			} else
				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		}
	}

	public void setSize(float w, float h) {
		scale = w/this.w;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public void setAngle(float angle) {
		this.angle = angle;
	}
}
