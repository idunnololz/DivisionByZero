package com.ggstudios.divisionbyzero;

import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;

import com.ggstudios.utils.DebugLog;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.ETC1Util;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;
import android.util.SparseIntArray;

public class TextureManager {
	private static final String TAG = "TextureManager";
	private static final int DEFAULT_MAX_CAPACITY = 100;

	private SparseIntArray resIdToHandle;

	// this class manages binding textures etc.
	public TextureManager() {
		resIdToHandle = new SparseIntArray(DEFAULT_MAX_CAPACITY);
	}

	public int get(int resId) {
		if(resId < 0){
			DebugLog.e(TAG, "Invalid resource id");

			try {
				throw new Exception();
			} catch (Exception e) {
				DebugLog.e(TAG, e);
			}
			return -1;
		}

		int handle = resIdToHandle.get(resId, -1);
		if(handle == -1){
			String res = Core.context.getResources().getResourceEntryName(resId);
			DebugLog.e(TAG, "Trying to load a texture that hasn't been preloaded. Res. name: " + res);
			return -1;
		}
		return handle;
	}

	public void loadTexture(int resId) {
		int handle = loadTexture(resId, false);
		resIdToHandle.put(resId, handle);
	}

	public synchronized void loadGameTextures() {
		DebugLog.d(TAG, "Loading textures...");
		// delete all older textures...
		clearTextures();
		
		loadTexture(R.drawable.bg_element_direction_mark);
		loadTexture(R.drawable.faction_bronze_icn);
		loadTexture(R.drawable.right_panel);
		loadTexture(R.drawable.selection_box);
		loadTexture(R.drawable.selection_box_2);
		loadTexture(R.drawable.zero);
		loadTexture(R.drawable.hp_bar);
		loadTexture(R.drawable.status_weakness);
		loadTexture(R.drawable.mission_failed_message);
		loadTexture(R.drawable.mission_success_message);
		loadTexture(R.drawable.button_more);

		// wave_control
		loadTexture(R.drawable.wave_control_play);
		loadTexture(R.drawable.wave_control_ff);
		loadTexture(R.drawable.wave_control_fff);
		loadTexture(R.drawable.wave_control_pause);
		loadTexture(R.drawable.wave_control_panel);
		loadTexture(R.drawable.wave_control_next);

		// general textures
		loadTexture(R.drawable.button_bg);
		loadTexture(R.drawable.close);
		loadTexture(R.drawable.white);
		loadTexture(R.drawable.window_bg);
		
		// controls
		loadTexture(R.drawable.zoom_control_in);
		loadTexture(R.drawable.zoom_control_out);
		
		// sprite textures
		loadTexture(R.drawable.sprite_normal0001);
		loadTexture(R.drawable.sprite_normal0002);
		loadTexture(R.drawable.sprite_normal0003);
		loadTexture(R.drawable.sprite_normal0004);
		loadTexture(R.drawable.sprite_normal0005);
		loadTexture(R.drawable.sprite_normal0006);
		loadTexture(R.drawable.sprite_normal0007);
		loadTexture(R.drawable.sprite_normal0008);
		loadTexture(R.drawable.sprite_normal0009);
		loadTexture(R.drawable.sprite_normal0010);
		loadTexture(R.drawable.sprite_normal0011);
		loadTexture(R.drawable.sprite_normal0012);
		loadTexture(R.drawable.sprite_normal0013);
		loadTexture(R.drawable.sprite_normal0014);
		loadTexture(R.drawable.sprite_normal0015);
		loadTexture(R.drawable.sprite_normal0016);
		loadTexture(R.drawable.sprite_normal0017);
		loadTexture(R.drawable.sprite_normal0018);
		loadTexture(R.drawable.sprite_normal0019);
		loadTexture(R.drawable.sprite_normal0020);

		// towers
		loadTexture(R.drawable.tower_atlas);
		loadTexture(R.drawable.tower_boss);
		loadTexture(R.drawable.tower_box_1);
		loadTexture(R.drawable.tower_box_2);
		loadTexture(R.drawable.tower_box_3);
		loadTexture(R.drawable.tower_brutal_1);
		loadTexture(R.drawable.tower_brutal_2);
		loadTexture(R.drawable.tower_brutal_3);
		loadTexture(R.drawable.tower_circle_1);
		loadTexture(R.drawable.tower_cluster);
		loadTexture(R.drawable.tower_demo_1);
		loadTexture(R.drawable.tower_demo_2);
		loadTexture(R.drawable.tower_desire);
		loadTexture(R.drawable.tower_desire_2);
		loadTexture(R.drawable.tower_desire_3);
		loadTexture(R.drawable.tower_desire_4);
		loadTexture(R.drawable.tower_desolator_1);
		loadTexture(R.drawable.tower_desolator_2);
		loadTexture(R.drawable.tower_desolator_3);
		loadTexture(R.drawable.tower_diamond_1);
		loadTexture(R.drawable.tower_diamond_2);
		loadTexture(R.drawable.tower_dynamic);
		loadTexture(R.drawable.tower_dynamic_2);
		loadTexture(R.drawable.tower_flake_1);
		loadTexture(R.drawable.tower_flake_2);
		loadTexture(R.drawable.tower_heavy_1);
		loadTexture(R.drawable.tower_heavy_2);
		loadTexture(R.drawable.tower_heavy_3);
		loadTexture(R.drawable.tower_heavy_4);
		loadTexture(R.drawable.tower_heavy_5);
		loadTexture(R.drawable.tower_normal);
		loadTexture(R.drawable.tower_normal_2);
		loadTexture(R.drawable.tower_normal_3);
		loadTexture(R.drawable.tower_normal_4);
		loadTexture(R.drawable.tower_normal_5);
		loadTexture(R.drawable.tower_null);
		loadTexture(R.drawable.tower_regular);
		loadTexture(R.drawable.tower_regular_2);
		loadTexture(R.drawable.tower_regular_3);
		loadTexture(R.drawable.tower_void);

		// load tower assets
		loadTexture(R.drawable.sniper_asset);
		loadTexture(R.drawable.bullet);
		loadTexture(R.drawable.bullet_heavy);
		loadTexture(R.drawable.aoe_blast);
		loadTexture(R.drawable.box_burn);
		loadTexture(R.drawable.normal_pulse);
		loadTexture(R.drawable.lazer_background);
		loadTexture(R.drawable.lazer_overlay);
		loadTexture(R.drawable.demo_flake_shot);
		loadTexture(R.drawable.flake_freeze);
		loadTexture(R.drawable.demo_shot);
		loadTexture(R.drawable.aoe_stun);
	}

	public void reloadTextures() {
		DebugLog.d(TAG, "reloadTextures()");
		int len = resIdToHandle.size();
		for(int i = len - 1; i >= 0; i--) {
			int key = resIdToHandle.keyAt(i);
			int val = resIdToHandle.valueAt(i);
			int newHandle = reloadTexture(val, key);

			if(newHandle != val) {
				resIdToHandle.put(key, newHandle);
			}
		}
	}

	public void clearTextures() {
		if (resIdToHandle.size() == 0)
			return;
		int[] arr = new int[resIdToHandle.size()];
		for(int i = 0; i < resIdToHandle.size(); i++){
			arr[i] = resIdToHandle.get(resIdToHandle.keyAt(i));
		}

		GLES20.glDeleteTextures(resIdToHandle.size(), arr, 0);

		resIdToHandle.clear();
	}

	/**
	 * Loads a texture and increments the loaded texture counter.
	 * @param gl Unused
	 * @param resId The ID of the texture resource to be loaded
	 * @param etc1 Using ETC1?
	 * @return Returns a handle to the loaded texture
	 */
	private int loadTexture(int resId, boolean etc1) {
		int[] textures = new int[1];
		int handle;

		// Generate one texture pointer...
		GLES20.glGenTextures(1, textures, 0);

		handle = textures[0];

		// ...and bind it to our array
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, handle);

		// Create Nearest Filtered Texture
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		// Different possible texture parameters, e.g. GLES20.GL_CLAMP_TO_EDGE
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GL10.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GL10.GL_CLAMP_TO_EDGE);

		// if using etc1 compression
		if (etc1) {
			InputStream input = Core.context.getResources().openRawResource(resId);
			try {
				ETC1Util.loadTexture(GLES20.GL_TEXTURE_2D, 0, 0, GLES20.GL_RGB,
						GLES20.GL_UNSIGNED_SHORT_5_6_5, input);
			} catch (IOException e) {
				Log.w("Texture Manager", "Could not load texture: " + e);
			} finally {
				try {
					input.close();
				} catch (IOException e) {
					// ignore exception thrown from close.
				}
			}
			return textures[0];
		}

		// Get the texture from the Android resource directory
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inScaled = false;
		
		// Load up, and flip the texture:
		Bitmap bitmap = BitmapFactory.decodeResource(Core.context.getResources(), resId, opts);

		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

		int error = GLES20.glGetError();
		if (error != GLES20.GL_NO_ERROR) {
			// simple method to load textures failed, use the bashy method
			// instead!
			Log.e(TAG, "GLError: " + error + " (" + GLU.gluErrorString(error) + "): " + resId);
			
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			int[] pixels = new int[width * height];
			bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
			
			for (int i=0; i < pixels.length; i++) {
			    int argb = pixels[i];
			    pixels[i] = argb&0xff00ff00 | ((argb&0xff)<<16) | ((argb>>16)&0xff);
			}
			
			GLES20.glTexImage2D(GL10.GL_TEXTURE_2D, 0, GL10.GL_RGBA, width, height, 
				     0, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, IntBuffer.wrap(pixels));
		}

		// Clean up
		bitmap.recycle();

		return textures[0];
	}

	private int reloadTexture(int oldHandle, int resId) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;   // No pre-scaling

		int handle;

		handle = oldHandle;

		// Read in the resource
		final Bitmap bitmap = BitmapFactory.decodeResource(Core.context.getResources(), resId, options);

		// Bind to the texture in OpenGL
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, handle);

		// Create Nearest Filtered Texture
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		// Different possible texture parameters, e.g. GLES20.GL_CLAMP_TO_EDGE
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GL10.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GL10.GL_CLAMP_TO_EDGE);

		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

		int error = GLES20.glGetError();
		if (error != GLES20.GL_NO_ERROR) {
			// simple method to load textures failed, use the bashy method
			// instead!
			Log.e(TAG, "GLError: " + error + " (" + GLU.gluErrorString(error) + "): " + resId);
			
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			int[] pixels = new int[width * height];
			bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
			
			for (int i=0; i<pixels.length; i++) {
			    int argb = pixels[i];
			    pixels[i] = argb&0xff00ff00 | ((argb&0xff)<<16) | ((argb>>16)&0xff);
			}
			
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 
				     0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, IntBuffer.wrap(pixels));
		}

		// Clean up
		bitmap.recycle();

		return handle;
	}
}
