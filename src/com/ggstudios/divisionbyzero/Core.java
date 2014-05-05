package com.ggstudios.divisionbyzero;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;

public class Core{
	/*
	 * This is the number of drawables used in the top level of drawing.
	 * Since drawing is separated into several collections, we modify
	 * those collections to added or remove UI elements and not the
	 * top level drawable. So we can save a few CPU cycles if we make
	 * the top level drawable an fixed length array. 
	 * 
	 * WARNING: THIS CONSTANT IS USED IN GAMERENDERER. 
	 * Check onDraw before changing...
	 */
	public static final int PRIMARY_DRAWABLES_USED = 10;
	
	static GLSurfaceView glView;
	static GameRenderer gr;
	static GameUpdater gu;
	static Game game;
	static Grid grid;
	
	static int currentLevelResId;
	
	static float zoom = 1f;
	
	static Player player;
	
	static float[] matrix = new float[16];
	static float[] mixedMatrix = new float[16];
	
	static float originalTouchX, originalTouchY;

	final static int A_POSITION_HANDLE = 0;
	final static int A_TEX_COORD_HANDLE = 2;

	static int U_MIXED_MATRIX_HANDLE;
	static int U_TRANSLATION_MATRIX_HANDLE;
	static int U_TEXTURE_HANDLE;
	static int U_TEX_COLOR_HANDLE;
	
	static int indiceHandle = 0;
	
	static Hud hud;

	static boolean forceVisible;
	
	static float canvasWidth = -1;
	static float canvasHeight = -1;
	
	/**
	 * May be used as a hint on whether an object is on the screen or not.
	 */
	static float cullR, cullB;

	/**
	 * SDP and SDP_H are like DPs in Android and are density independent.
	 */
	static float SDP;
	static float SDP_H;
	
	/**
	 * This is the size of a tile on the current map.
	 */
	static float MAP_SDP;

	static Drawable[] drawables;
	static List<Clickable> clickables;

	static Handler handler;
	static Context context;
	
	static TextureManager tm;
	static FontManager fm;
	static LevelManager lm;
	static SpawnManager sm;
	
	static Handler guiHandler;
	
	static float offX = 0.0f, offY = 0.0f;
	
	// buffers for general use
	public static class GeneralBuffers{
		static VBO fullscreen;
		static VBO tile;
		static VBO tile_not_centered;
		static VBO map_tile;
		static VBO map_half_tile;
		static VBO half_tile;
		static VBO half_tile_not_centered;
	}
	
	public static void addClickable(Clickable c){
		synchronized(clickables){
			clickables.add(c);
		}
	}
	
	public static void removeClickable(Object c){
		synchronized(clickables){
			clickables.remove(c);
		}
	}
	
	public static void onZoomChanged() {
		cullR = (canvasWidth + SDP_H) / zoom;
		cullB = (canvasHeight + SDP_H) / zoom;
	}
	
	public static void reset() {
		glView = null;
		gr = null;
		game = null;
		gu = null;
		grid = null;
		
		player = null;

		hud  = null;

		drawables = null;
		clickables = null;

		handler = null;
		context = null;
		
		tm = null;
		fm = null;
		lm = null;
		sm = null;
		guiHandler = null;
	}
	
	public static void finishActivity() {
		((Activity)context).finish();
	}
}