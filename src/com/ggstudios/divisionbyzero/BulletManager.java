package com.ggstudios.divisionbyzero;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.opengl.GLES20;

import com.ggstudios.utils.BufferUtils;
import com.ggstudios.utils.DebugLog;

public class BulletManager extends DrawableCollection<Bullet> {
	private static final String TAG = "BulletManager";
	
	private VBO bulletVbo;
	private int capacity;
	
	public BulletManager() {
		bulletVbo = new VBO();
	}
	
	public void loadGlData() {
		final float w = Core.SDP * 0.1f;
		final float[] vertices = {
			-w, -w,
			w, -w,
			-w, w,
			w, w
		};
		
		bulletVbo.setVBO(w * 2.0f, w * 2.0f, 
				BufferUtils.copyToBuffer(vertices), VBO.Alignment.CENTER);
	}
	
	public void growPool(int size) {
		capacity = capacity + size;
		
		for(int i = len; i < capacity; i++) {
			Bullet b = new Bullet(0, 0);
			drawables.add(b);
		}
	}
	
	/**
	 * Attempts to grab a unused instance of a bullet and entire it into
	 * the drawing pool. The PictureBox returned will be initially invisible.
	 * The caller should set the properties of the object first, then
	 * reinstate the object by making it visible.
	 * 
	 * If there are non left, the bullet pool size will be increased.
	 * @return A bullet drawable object.
	 */
	public Bullet obtain() {
		Bullet b = drawables.get(len++);
		b.isVisible = false;
		b.setVBO(bulletVbo);
		
		return b;
	}
	
	@Override
	public void addDrawable(Bullet d) {
		DebugLog.e(TAG, "Error. Do not call add drawable on this object." +
				" Call obtain() instead.");
	}

	public void setBulletBounds(float l, float t, float r, float b) {
		Bullet.setBounds(l, t, r, b);
	}
	
	@Override
	public void draw(float offX, float offY) {
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
		super.draw(offX, offY);
		Core.gr.resetBlendFunc();
	}
	
	@Override
	public void refresh(){
		loadGlData();
		super.refresh();
	}
	
	public void writeToStream(DataOutputStream stream) throws IOException {
		final int len = size();
		stream.writeInt(len);
		for(int i = 0; i < len; i++) {
			Bullet b = get(i);
			b.writeToStream(stream);
		}
	}
	
	public void readFromStream(DataInputStream stream) throws IOException {
		final int len = stream.readInt();
		for(int i = 0; i < len; i++) {
			Bullet b = obtain();
			b.loadFromStream(stream);
			b.isVisible = true;
			
			Core.gu.addGameUpdatable(b);
		}
	}
}