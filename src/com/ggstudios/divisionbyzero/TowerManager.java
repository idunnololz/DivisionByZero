package com.ggstudios.divisionbyzero;

import static fix.android.opengl.GLES20.glDrawElements;
import static fix.android.opengl.GLES20.glVertexAttribPointer;

import com.ggstudios.utils.BufferUtils;
import com.ggstudios.utils.DebugLog;

import android.opengl.GLES20;

public class TowerManager extends DrawableCollection<Tower> {
	private static final String TAG = "TowerManager";

	private int bufferHandle;
	private int textureHandle;

	private static final int TEXTURE_DATA_SIZE = 4 * 4;
	private static final int MAX_TEXTURES = 300;

	private float[] vertexBuffer = new float[MAX_TEXTURES * TEXTURE_DATA_SIZE];

	private boolean dirty = false;

	public void loadGlData() {
		DebugLog.d(TAG, "loadGlData()");
		refresh();
	}

	@Override
	public void addDrawable(final Tower t) {
		super.addDrawable(t);

		dirty = true;
	}

	@Override
	public void removeDrawable(int index) {
		super.removeDrawable(index);

		dirty = true;
	}

	@Override
	public void draw(float offX, float offY) {
		if(dirty) {
			cleanBuffer();
		}

		Utils.resetMatrix();
		Utils.translateAndCommit(offX, offY);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferHandle);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, Core.indiceHandle);

		glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, TEXTURE_DATA_SIZE, 0);
		glVertexAttribPointer(Core.A_TEX_COORD_HANDLE, 2, GLES20.GL_FLOAT, false, TEXTURE_DATA_SIZE, 2 * 4);
		glDrawElements(GLES20.GL_TRIANGLES, len * 6, GLES20.GL_UNSIGNED_SHORT, 0);

		Core.gr.restoreTextureHandle();

		for(int i = len - 1; i >= 0; i--) {
			drawables.get(i).drawSpecial(offX, offY);
		}
	}

	private void cleanBuffer() {
		dirty = false;

		int offset = 0;
		final float mapH = Core.MAP_SDP / 2f;
		for(int i = len - 1; i >= 0; i--) {
			drawables.get(i).drawSprite(vertexBuffer, offset, -mapH, -mapH);
			offset += TEXTURE_DATA_SIZE;
		}

		bufferHandle = BufferUtils.copyToBuffer(vertexBuffer, offset);
	}

	@Override
	public void refresh() {		
		textureHandle= Core.tm.get(R.drawable.tower_atlas);
		if(textureHandle == -1) return;
		
		bufferHandle = Core.GeneralBuffers.map_tile.handle;

		super.refresh();

		if(this.len != 0) {
			int offset = 0;
			final float mapH = Core.MAP_SDP / 2f;
			for(int i = this.len - 1; i >= 0; i--) {
				drawables.get(i).drawSprite(vertexBuffer, offset, -mapH, -mapH);
				offset += TEXTURE_DATA_SIZE;
			}

			bufferHandle = BufferUtils.copyToBuffer(vertexBuffer, offset);		
		}
	}

	public void invalidate() {
		dirty = true;
	}
}
