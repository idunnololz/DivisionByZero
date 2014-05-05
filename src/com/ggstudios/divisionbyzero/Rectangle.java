package com.ggstudios.divisionbyzero;

import static fix.android.opengl.GLES20.glVertexAttribPointer;
import android.opengl.GLES20;

import com.ggstudios.utils.BufferUtils;

class Rectangle extends Drawable {
	private int handle;

	float x, y, w, h;

	private float r, g, b;
	private int textureHandle = 0;
	private float scaleW = 1f;
	private float scaleH = 1f;

	public float transparency = 1f;

	private boolean dirty = false;

	/**
	 * Creates a drawable square object
	 * 
	 * @param x	X
	 * @param y Y
	 * @param w Width
	 * @param h Height
	 * @param a Alpha
	 * @param r Red
	 * @param g Green
	 * @param b Blue
	 */

	public Rectangle(float x, float y, float w, float h, int color){
		this.x = x;
		this.y = y;

		this.w = w;
		this.h = h;

		r = ((color>>16)&0xFF) / 255f;
		g = ((color>>8)&0xFF) / 255f;
		b = ((color)&0xFF) / 255f;

		refresh();
	}

	public void setSize(float newW, float newH){
		w = newW;
		h = newH;

		dirty = true;
	}

	public void setScaleWidth(float scaleW) {
		this.scaleW = scaleW;
	}

	private void rebuild() {
		handle = BufferUtils.createRectangleBuffer(w, h);
		textureHandle = Core.tm.get(R.drawable.white);
	}

	@Override
	public void draw(float offX, float offY) {
		if(dirty) {
			rebuild();
			dirty = false;
		}

		final float finalX = offX + x;
		final float finalY = offY + y;

		Utils.resetMatrix();
		if(scaleW != 1f)
			Utils.scaleW(scaleW);
		if(scaleH != 1f)
			Utils.scaleH(scaleH);
		Utils.translateAndCommit(finalX, finalY);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, handle);
		glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, 0, 0);

		GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, r, g, b, transparency);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1, 1, 1, 1);
	}

	@Override
	public void refresh() {
		rebuild();
	}

	public void setScaleHeight(float scaleH) {
		this.scaleH = scaleH;
	}
}
