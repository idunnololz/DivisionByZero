package com.ggstudios.divisionbyzero;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.ggstudios.divisionbyzero.VBO.Alignment;
import com.ggstudios.utils.BufferUtils;
import com.ggstudios.utils.DebugLog;
import com.ggstudios.utils.ShaderUtils;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;

import static fix.android.opengl.GLES20.glVertexAttribPointer;

public class GameRenderer implements Renderer {
	private static final String TAG = "GameRenderer";

	public static final float CLEAR_COLOR_A = 1.0f;
	public static final float CLEAR_COLOR_R = 0.1f;
	public static final float CLEAR_COLOR_G = 0.1f;
	public static final float CLEAR_COLOR_B = 0.1f;

	private static final int PROFILE_REPORT_DELAY 	= 3 * 1000;
	private static final int FPS_RESTRICTION		= 32;
	private static final int ACTUAL_FPS_RESTRICTION = 1000/(FPS_RESTRICTION + 3);

	public static float[] transMatrix = new float[16];

	private int hProgram;

	private static int textureBufferHandle;

	private boolean multicoreDevice = false;

	public GameRenderer(Context context) {
		multicoreDevice = getNumCores() > 1;
	}

	private boolean buffersInitialized = false;
	private void loadBuffers(){
		DebugLog.d(TAG, "loadBuffers()");
		if(!buffersInitialized) {
			buffersInitialized = true;

			Core.GeneralBuffers.tile = new VBO();
			Core.GeneralBuffers.half_tile = new VBO();
			Core.GeneralBuffers.half_tile_not_centered = new VBO();
			Core.GeneralBuffers.tile_not_centered = new VBO();
			Core.GeneralBuffers.fullscreen = new VBO();
		}

		float f_w = Core.SDP;
		float h_w = Core.SDP_H;
		float hh_w = h_w / 2.0f;

		final float texture[] = {    		
				//Mapping coordinates for the vertices
				0, 0,
				1, 0,
				0, 1,
				1, 1, 
		};

		final float vertices2[] = {
				-h_w, 	-h_w, 	//Vertex 0
				h_w, 	-h_w, 	//v1
				-h_w, 	h_w,  	//v2
				h_w, 	h_w, 	//v3
		};

		Core.GeneralBuffers.tile.setVBO(Core.SDP, Core.SDP, 
				BufferUtils.copyToBuffer(vertices2), Alignment.CENTER);

		final float vertices4[] = {
				-hh_w, 	-hh_w,
				hh_w,	-hh_w,
				-hh_w,	hh_w,
				hh_w,	hh_w,
		};

		Core.GeneralBuffers.half_tile.setVBO(h_w, h_w, 
				BufferUtils.copyToBuffer(vertices4), Alignment.CENTER);

		final float vertices6[] = {
				0, 	0,
				h_w,	0,
				0,	h_w,
				h_w,	h_w,
		};
		Core.GeneralBuffers.half_tile_not_centered.setVBO(h_w, h_w, 
				BufferUtils.copyToBuffer(vertices6), Alignment.CENTER);

		final float vertices5[] = {
				0, 	0, 	//Vertex 0
				f_w, 	0, 	//v1
				0, 	f_w,  	//v2
				f_w, 	f_w, 	//v3
		};

		Core.GeneralBuffers.tile_not_centered.setVBO(Core.SDP, Core.SDP, 
				BufferUtils.copyToBuffer(vertices5), Alignment.TOP_LEFT);

		textureBufferHandle = BufferUtils.copyToBuffer(texture);

		float w = Core.canvasWidth;
		float h = Core.canvasHeight;

		final float vertices3[] = {
				//Vertices according to faces
				0, 	0, 	//Vertex 0
				w, 	0, 	//v1
				0, 	h, 	//v2
				w, 	h, 	//v3
		};

		Core.GeneralBuffers.fullscreen.setVBO((short)w, (short)h, 
				BufferUtils.copyToBuffer(vertices3));
	}

	private void initObjects(){
		loadBuffers();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig arg1) {		
		DebugLog.d(TAG, "onSurfaceCreated()");

		// Set the background frame color
		GLES20.glClearColor(CLEAR_COLOR_R, CLEAR_COLOR_G, CLEAR_COLOR_B, CLEAR_COLOR_A);	//black

		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		GLES20.glDisable(GLES20.GL_DITHER);

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);

		int vertexShader = ShaderUtils.compileShader(GLES20.GL_VERTEX_SHADER, Shader.VERTEX_SHADER_CODE);
		int fragmentShader = ShaderUtils.compileShader(GLES20.GL_FRAGMENT_SHADER, Shader.FRAGMENT_SHADER_CODE);

		hProgram = GLES20.glCreateProgram();             // create empty OpenGL Program

		GLES20.glAttachShader(hProgram, vertexShader);   // add the vertex shader to program
		GLES20.glAttachShader(hProgram, fragmentShader); // add the fragment shader to program

		GLES20.glBindAttribLocation(hProgram, Core.A_POSITION_HANDLE, "a_Position");
		GLES20.glBindAttribLocation(hProgram, Core.A_TEX_COORD_HANDLE, "a_TexCoordinate");

		GLES20.glLinkProgram(hProgram);                  // creates OpenGL program executables

		// get handles
		Core.U_TRANSLATION_MATRIX_HANDLE 	= GLES20.glGetUniformLocation(hProgram, "uTransMatrix");
		Core.U_TEXTURE_HANDLE 				= GLES20.glGetUniformLocation(hProgram, "u_Texture");	
		Core.U_MIXED_MATRIX_HANDLE 			= GLES20.glGetUniformLocation(hProgram, "uConstantMatrix");
		Core.U_TEX_COLOR_HANDLE 			= GLES20.glGetUniformLocation(hProgram, "u_Color");

		/*
		 * Set up GL for use for the first time. These
		 * calls are made once and only once to improve performance.
		 */
		// Add program to OpenGL environment
		GLES20.glUseProgram(hProgram);

		//set blend on
		GLES20.glEnable(GLES20.GL_BLEND);
		resetBlendFunc();

		// enable our custom buffers
		GLES20.glEnableVertexAttribArray(Core.A_TEX_COORD_HANDLE);
		GLES20.glEnableVertexAttribArray(Core.A_POSITION_HANDLE);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

		GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1.0f, 1.0f, 1.0f, 1.0f);
	}

	@Override
	public synchronized void onDrawFrame(GL10 gl) {
		if(!multicoreDevice)
			restrictFps();
		
		logReport();

		beginScene();
		
		Core.drawables[0].draw(Core.offX, Core.offY);
		Core.drawables[1].draw(Core.offX, Core.offY);
		Core.drawables[2].draw(Core.offX, Core.offY);
		Core.drawables[3].draw(Core.offX, Core.offY);
		Core.drawables[4].draw(Core.offX, Core.offY);
		Core.drawables[5].draw(Core.offX, Core.offY);
		Core.drawables[6].draw(Core.offX, Core.offY);
		Core.drawables[7].draw(Core.offX, Core.offY);
		Core.drawables[8].draw(Core.offX, Core.offY);
		Core.drawables[9].draw(Core.offX, Core.offY);
	}

	private long startTimeInNano = System.currentTimeMillis();
	private int frames = 0;
	private static final int SECONDS = PROFILE_REPORT_DELAY / 1000;

	private void logReport() {
		frames++;
		if(System.currentTimeMillis() - startTimeInNano >= PROFILE_REPORT_DELAY) {
			DebugLog.d(TAG, "Profile Report:\nfps: " + (float)frames/SECONDS);
			frames = 0;
			startTimeInNano = System.currentTimeMillis();
		}
	}

	private long endTime, dt, startTime = System.currentTimeMillis();

	private void restrictFps() {
		endTime = System.currentTimeMillis();
		dt = endTime - startTime;
		if (dt < ACTUAL_FPS_RESTRICTION) {
			try {
				Thread.sleep(ACTUAL_FPS_RESTRICTION - dt);
			} catch (InterruptedException e) {
				DebugLog.e(TAG, e);
			}
		}
		startTime = System.currentTimeMillis();
	}

	private void beginScene(){
		// Redraw background color
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
	}

	/**
	 * If this function is called, then all BUFFERS, TEXTURES and other GL stuff have all been disposed
	 * DO NOT ASSUME ANYTHING IS NOT DISPOSED AND RELOAD EVERYTHING!
	 */
	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		DebugLog.d(TAG, "surface size: " + width + ", " + height);

		startTime = System.currentTimeMillis();

		boolean surfaceChanged = false;
		if(Core.canvasWidth != -1 && (Core.canvasWidth != width || Core.canvasHeight != height) ) {
			surfaceChanged = true;
		}

		GLES20.glViewport(0, 0, width, height);

		Core.tm.reloadTextures();

		Core.grid.remeasure(width, height);
		Core.canvasWidth = width;
		Core.canvasHeight = height;
		Core.SDP = Core.grid.getTileWidth();
		Core.SDP_H = Core.SDP/2;

		Core.onZoomChanged();

		initObjects();

		float[] tempTransMatrix = new float[16];
		float[] tempProjMatrix = new float[16];

		// this projection matrix is applied to object coordinates
		// in the onDrawFrame() method
		tempProjMatrix[0] = 2.0f/(float)width;
		tempProjMatrix[5] = -2.0f/(float)height;
		tempProjMatrix[15] = 1;

		//set matrix right the first time
		Matrix.setIdentityM(tempTransMatrix, 0);
		Matrix.setIdentityM(Core.matrix, 0);

		tempTransMatrix[3] = -1.0f;
		tempTransMatrix[7] = 1.0f;

		Matrix.multiplyMM(Core.mixedMatrix, 0, tempProjMatrix, 0, tempTransMatrix, 0);

		DebugLog.d(TAG, "done onSurfaceChanged");
		
		Core.game.onSurfaceCreated();
		if(surfaceChanged)
			Core.game.notifySurfaceChanged();
		
		if(Core.game.getState() == Game.STATE_KILLED) {
			Core.game.restarted();
		} else if(Core.game.getState() != Game.STATE_CLEAN_START) {
			// don't refresh if the game hasn't even been set up yet
			Core.game.refresh();
		}

		restoreTextureHandle();

		// if we are loading a level... we are not done just yet
		StateManager stateMgr = StateManager.getInstance();

		if(stateMgr.isLoading()) {
			boolean before = Core.gu.isPaused();

			Core.gu.pause();

			stateMgr.continueLoadingSaveFile();
			stateMgr.waitForLoadToFinish();

			if(!before) {
				Core.gu.unpause();
			}
			
			Core.game.onSaveFileLoaded();
		}

		Core.game.onLoadFinished();
	}

	public void restoreTextureHandle() {
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textureBufferHandle);
		glVertexAttribPointer(Core.A_TEX_COORD_HANDLE, 2, GLES20.GL_FLOAT, false, 0, 0);
	}

	public void resetBlendFunc() {
		//GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
	}

	/**
	 * Gets the number of cores available in this device, across all processors.
	 * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
	 * @return The number of cores, or 1 if failed to get result
	 */
	private int getNumCores() {
		//Private Class to display only CPU devices in the directory listing
		class CpuFilter implements FileFilter {
			@Override
			public boolean accept(File pathname) {
				//Check if filename is "cpu", followed by a single digit number
				if(Pattern.matches("cpu[0-9]+", pathname.getName())) {
					return true;
				}
				return false;
			}      
		}

		try {
			//Get directory containing CPU info
			File dir = new File("/sys/devices/system/cpu/");
			//Filter to only list the devices we care about
			File[] files = dir.listFiles(new CpuFilter());
			//Return the number of cores (virtual CPU devices)
			return files.length;
		} catch(Exception e) {
			//Default to return 1 core
			return 1;
		}
	}
}
