package com.ggstudios.divisionbyzero;

import com.ggstudios.utils.BitmapUtils;
import com.ggstudios.utils.DebugLog;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class FontManager {
	// number of possible characters
	private static final String TAG = "FontManager";

	private static final char START_CHAR = ' ', END_CHAR = '~' + 1;
	private static final int CHAR_COUNT = END_CHAR - START_CHAR;

	// this should be a PoT
	private static final int MAX_TEXTURE_WIDTH = 512;

	int cellW = 0;
	int cellH = 0;

	float[] charWidth = new float[END_CHAR];
	TextureRegion[] charRegion = new TextureRegion[END_CHAR];

	private int textureHeight;
	int textureHandle;

	private Paint textPaint;

	// indicates whether a font has been specified yet
	private boolean hasFont = false;

	public FontManager() {
	}

	public void generateFont(float size){
		textPaint = new Paint();
		textPaint.setTextSize(size);
		textPaint.setAntiAlias(true);
		textPaint.setARGB(255, 255, 255, 255);

		hasFont = true;

		refresh();
	}

	public void generateFont(Paint painter){
		textPaint = painter;

		hasFont = true;

		refresh();
	}

	public int breakText(String input, int w){
		return textPaint.breakText(input, true, w, null);
	}

	public int getWidth(String input){
		return (int) textPaint.measureText(input);
	}
	
	public float getHeight() {
		return cellH;
	}
	
	public float getFontSize() {
		return textPaint.getTextSize();
	}

	public void refresh(){
		DebugLog.d(TAG, "refreshing...");
		if(!hasFont){ 
			DebugLog.d(TAG, "No font loaded... aborting...");
			return;
		}
		
		cellW = (int) textPaint.getFontSpacing();
		cellH = textPaint.getFontMetricsInt().descent - textPaint.getFontMetricsInt().ascent;

		int cols = MAX_TEXTURE_WIDTH / cellW;
		int rows = (int)Math.ceil(CHAR_COUNT / (float)cols);

		textureHeight = Utils.findSmallestBase2(rows * cellH);

		String strTemp = "";
		Bitmap bitmap = Bitmap.createBitmap(MAX_TEXTURE_WIDTH, textureHeight, Bitmap.Config.ARGB_8888);
		bitmap.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas(bitmap);

		float x = 0f;
		float y = textPaint.getTextSize() - textPaint.descent();

		for(int i = START_CHAR; i < END_CHAR; i++){
			// convert our character to string
			strTemp = Character.toString((char) (i));

			canvas.drawText(strTemp, x, y, textPaint);

			x += cellW;
			if((x + cellW) > MAX_TEXTURE_WIDTH) {
				x = 0f;
				y += cellH;
			}
		}

		textureHandle = BitmapUtils.loadBitmap(bitmap);

		char[] everyLetter = new char[END_CHAR];
		for(int i = 0; i < everyLetter.length; i++){
			everyLetter[i] = (char) i;
		}

		textPaint.getTextWidths(everyLetter, 0, END_CHAR, charWidth);

		// setup the array of character texture regions
		x = 0;                                          // Initialize X
		y = 0;                                          // Initialize Y
		for ( int c = START_CHAR; c < END_CHAR; c++ )  {         // FOR Each Character (On Texture)
			charRegion[c] = new TextureRegion( MAX_TEXTURE_WIDTH, textureHeight, x, y, cellW - 1, cellH - 1);  // Create Region for Character
			x += cellW;                              // Move to Next Char (Cell)
			if ( x + cellW > MAX_TEXTURE_WIDTH )  {
				x = 0;                                    // Reset X Position to Start
				y += cellH;                          // Move to Next Row (Cell)
			}
		}

		DebugLog.d(TAG, "Texture size:" + MAX_TEXTURE_WIDTH + ", " + textureHeight);
		DebugLog.d(TAG, "cellw:" +cellW + "cellh:" +cellH);
	}

	static class TextureRegion {

		//--Members--//
		public float u1, v1;                               // Top/Left U,V Coordinates
		public float u2, v2;                               // Bottom/Right U,V Coordinates

		private float texW, texH, w, h;
		
		/**
		 * Calculate U,V coordinates from specified texture coordinates
		 * @param texWidth	Width of the texture atlas
		 * @param texHeight Height of the texture atlas
		 * @param x			The x coordinate of the region
		 * @param y			The y coordinate of the region
		 * @param width		The width of the region
		 * @param height	The height of the region
		 */
		public TextureRegion(float texWidth, float texHeight, float x, float y, float width, float height)  {
			this.u1 = x / texWidth;                     // Calculate U1
			this.v1 = y / texHeight;                    // Calculate V1
			this.u2 = ( (x + width) / texWidth );       // Calculate U2
			this.v2 = ( (y + height) / texHeight );     // Calculate V2
			
			texW = texWidth;
			texH = texHeight;
			w = width;
			h = height;
		}
		
		public TextureRegion(float texWidth, float texHeight, float width, float height) {
			texW = texWidth;
			texH = texHeight;
			w = width;
			h = height;
		}

		public void setRegion(float x, float y) {
			this.u1 = x / texW;                     // Calculate U1
			this.v1 = y / texH;                    // Calculate V1
			this.u2 = ( (x + w) / texW );       // Calculate U2
			this.v2 = ( (y + h) / texH );     // Calculate V2
		}
	}
	
	public int getUnderlyingTextureWidth() {
		return MAX_TEXTURE_WIDTH;
	}
	
	public int getUnderlyingTextureHeight() {
		return textureHeight;
	}
}
