package com.ggstudios.divisionbyzero;

import com.ggstudios.utils.BufferUtils;
import com.ggstudios.utils.DebugLog;

import android.opengl.GLES20;

import static fix.android.opengl.GLES20.glVertexAttribPointer;

public class Circle extends Drawable{
	private static final String TAG = "Circle";
	
	private static final float DEFAULT_RADIUS = 5.0f;
	
	private int handle = 0;
	
	float x, y;
	int points;
	
	private float radius;
	float scale;
	float[] vertices;
	
	float a, r, g, b;

	public boolean visible = true;
	
	private int textureHandle;
	
	/**
	 * Creates a new circle object
	 * 
	 * @param x The x coordinate
	 * @param y The y coordinate
	 * @param radius The radius of the circle
	 * @param points The number of points... The more the smoother the circle but also the slower
	 */
	
	public Circle(int x, int y, float r, int points){
		vertices = new float[(points+2)*2];
		this.points = points;
		this.x = x;
		this.y = y;
		
		if(r < 0) {
			DebugLog.e(TAG, "Error. Negative radius set: " + r);
		}
		
		// CENTER OF CIRCLE
		vertices[0] = 0;
		vertices[1] = 0;

		radius = DEFAULT_RADIUS * Core.SDP;
		
		float rad;
		for (int i = 2; i<(points+2)*2; i+=2){
		    rad = i*(2*Utils.PI)/(points*2);
		    vertices[i] = (short) (Math.cos(rad)*radius);
		    vertices[i+1] = (short) (Math.sin(rad)*radius);
		}
	
		handle = BufferUtils.copyToBuffer(vertices);
		
		scale = r / radius;
		
		this.a = 0.3f;
		this.r = 1.0f;
		this.g = 0.0f;
		this.b = 0.0f;
		
		textureHandle = Core.tm.get(R.drawable.white);
	}
	
	public void update(float x, float y, float r){
		this.x = x;
		this.y = y;
		scale = r / radius;
	}
	
	public void setPos(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	@Override
	public void draw(float offX, float offY){
		if(!visible) return;
		
		final float finalX = offX + x;
		final float finalY = offY + y;
		
		Utils.resetMatrix();
		Utils.scale(scale);
		Utils.translateAndCommit(finalX, finalY);
		
		GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, r, g, b, a);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, handle);
		glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, 0, 0);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, points+2);
		
		GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1, 1, 1, 1);

	}

	@Override
	public void refresh() {
		handle = BufferUtils.copyToBuffer(vertices);
		
		textureHandle = Core.tm.get(R.drawable.white);
	}
}
