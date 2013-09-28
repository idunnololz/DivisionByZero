package com.ggstudios.divisionbyzero;

import com.ggstudios.utils.BitmapUtils;
import com.ggstudios.utils.BufferUtils;
import com.ggstudios.utils.DebugLog;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Grid extends PictureBox {
	//the grid class is responsible for  drawing the game grid guide
	//and is capable of drawing extra guides for multipurpose uses...

	private static final String TAG = "Grid";
	
	//original is 18x13
	public static final int DEFAULT_TILES_ACROSS = 18;
	public static final int DEFAULT_TILES_DOWN = 13;

	public boolean perfectFit;
	public float extraWidth, extraHeight;

	private float tileW;
	private float tileH;

	private int potWidth, potHeight;

	private float gridW, gridH;
	private int tilesAcross = DEFAULT_TILES_ACROSS, tilesDown = DEFAULT_TILES_DOWN;

	private int bgResId = -1;
	private int lineColor = 0x80FFFFFF, lineThickness = 1;

	public Grid(){
		super(0, 0);
	}

	public Grid(float width, float height){
		super(0, 0);
		remeasure(width, height);
	}

	public Grid(float width, float height, int across, int down){
		super(0, 0);
		this.tilesDown = down;
		this.tilesAcross = across;
		remeasure(width, height);
	}

	public void remeasure(final float width, final float height){
		if(width == 0 || height == 0) return;

		w = width;
		h = height;

		if( height * ((float)tilesAcross/(float)tilesDown) > width){	//find the limiting factor
			//if it's width then
			h = width * ((float)tilesDown/(float)tilesAcross);
		}else{
			//it's height
			w = height * ((float)tilesAcross/(float)tilesDown);
		}

		perfectFit = tilesAcross/(float)tilesDown == width/height;

		tileW = w/tilesAcross;
		tileH = h/tilesDown;

		gridW = tileW*tilesAcross;
		gridH = tileH*tilesDown;

		if(!perfectFit){
			extraWidth = width - gridW;
			extraHeight = height - gridH;
		}
	}

	public void setBackgroundResource(int resId) {
		bgResId = resId;
	}

	public void setLineSpec(int lineColor, int lineThickness) {
		this.lineColor = lineColor;
		this.lineThickness = lineThickness;
	}

	public void useAsDrawable() {
		// caller wants to use this as a drawable...
		// generate textures and do initialization
		// needed to draw this grid.

		generateGrid(tilesAcross, tilesDown);
	}
	
	private int generateGrid(int tilesAcross, int tilesDown) {
		DebugLog.d(TAG, "generateGrid()");

		gridW = tilesAcross * tileW;
		gridH = tilesDown * tileH;

		potWidth = Utils.findSmallestBase2((int)gridW);
		potHeight = Utils.findSmallestBase2((int)gridH);

		Bitmap bitmap = Bitmap.createBitmap((int)(potWidth), (int) (potHeight), Bitmap.Config.RGB_565);

		bitmap.eraseColor(Color.argb((int)(GameRenderer.CLEAR_COLOR_A * 255), 
				(int)(GameRenderer.CLEAR_COLOR_R * 255), 
				(int)(GameRenderer.CLEAR_COLOR_G * 255), 
				(int)(GameRenderer.CLEAR_COLOR_B * 255)));

		Canvas c = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setColor(lineColor);
		paint.setStrokeWidth(lineThickness);
		
		if(bgResId > 0) {
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inScaled = false;

			Bitmap bg = BitmapFactory.decodeResource(Core.context.getResources(), bgResId, opts);

			Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
			p.setDither(true);
			p.setAntiAlias(true);
			
			c.drawBitmap(bg, new Rect(0, 0, bg.getWidth(), bg.getHeight()), 
					new Rect(0, 0, (int) (gridW + lineThickness), (int)(gridH + lineThickness)), 
					p);
			bg.recycle();
			
		}

		for(int i = 0; i < tilesAcross + 1; i++) {
			final float p = i * tileH;
			c.drawLine(p, 0, p, gridH, paint);
		}

		for(int i = 0; i < tilesDown + 1; i++) {
			final float p = i * tileW + 1;
			c.drawLine(0, p, gridW, p, paint);
		}

		textureHandle = BitmapUtils.loadBitmap(bitmap, textureHandle);

		handle = BufferUtils.createRectangleBuffer(potWidth, potHeight);

		this.x = 0;//this.extraWidth / 2;
		this.y = 0;//this.extraHeight / 2;

		w = gridW;
		h = gridH;
		
		drawingW = potWidth;
		drawingH = potHeight;
		
		return handle;
	}

	public int getTextureHandle() {
		return textureHandle;
	}

	public float getW() {
		return gridW;
	}

	public float getH() {
		return gridH;
	}

	public float getTileWidth() {
		return tileW;
	}

	public float getTileHeight() {
		return tileH;
	}

	public int getTilesAcross() {
		return tilesAcross;
	}

	public int getTilesDown() {
		return tilesDown;
	}

	@Override
	public void refresh() {
		generateGrid(tilesAcross, tilesDown);
	}
}
