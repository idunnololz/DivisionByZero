package com.ggstudios.divisionbyzero;

import com.ggstudios.utils.BitmapUtils;
import com.ggstudios.utils.DebugLog;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Label extends PictureBox {
	private static final String TAG = "Label";
	
	public static final int ALIGN_LEFT = 0, ALIGN_RIGHT = 1;
	
	private int alignment = ALIGN_LEFT;
	
	private Paint fontPaint;

	private String text;
	private String[] lines;

	private int maxWidth = -1;

	private Runnable rebuild = new Runnable() {

		@Override
		public void run() {
			generateTexture();
		}
		
	};
	
	protected Label() {
		super(0, 0);

		this.text = "";
		onTextChanged();
		refresh();
	}

	public Label(float x, float y, Paint painter, String text) {
		super(x, y);

		fontPaint = painter;

		this.text = text;
		onTextChanged();
		refresh();
	}
	
	public Label(float x, float y, Paint painter, String text, float maxWidth) {
		super(x, y);

		fontPaint = painter;

		this.text = text;
		setMaxWidth(maxWidth);
		onTextChanged();
		refresh();
	}

	public void setLocation(float x, float y){
		this.x = x;
		this.y = y;
	}

	public void setText(String text){
		if(this.text.equals(text)) return;

		this.text = text;
		onTextChanged();
		measure();
		Core.glView.queueEvent(rebuild);
	}
	
	private void onTextChanged() {
		if(text != null)
			lines = text.split("\n");
	}
	
	private void measure() {
		if(text == "" || text == null){
			w = 0;
			h = 0;
			return;
		}
		
		int numLines = lines.length;

		w = fontPaint.measureText(text);
		if(maxWidth > 0) { 
			for(String s : lines){
				int c = 0;
				char[] arr = s.toCharArray();
				while(c < arr.length) {
					int index = fontPaint.breakText(arr, c, arr.length - c, maxWidth, null);
					
					if(c != 0) {
						numLines++;
						w = maxWidth;
					}
					
					c += index;
				}
			}
		}
		
		Rect bounds = new Rect();
		float heightOfOneLine = fontPaint.descent() - fontPaint.ascent();
		fontPaint.getTextBounds(text, 0, text.length(), bounds);
		h = bounds.height() + (heightOfOneLine * (numLines - 1));
	}

	private void generateTexture(){
		if(text == "" || text == null){ 
			this.textureHandle = -1;
			return;
		}
		
		float heightOfOneLine = fontPaint.descent() - fontPaint.ascent();

		drawingW = Utils.findSmallestPot((int)w);
		drawingH = Utils.findSmallestPot((int)h);

		// now that we have our size information...
		generateBuffer();

		Bitmap bitmap = Bitmap.createBitmap((int)drawingW, (int)drawingH, Bitmap.Config.ARGB_4444);

		bitmap.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas(bitmap);

		float hh = fontPaint.getTextSize() - fontPaint.descent();
		if(maxWidth > 0) { 
			for(String s : lines){
				int c = 0;
				char[] arr = s.toCharArray();
				while(c < arr.length) {
					int count = arr.length - c;
					int index = fontPaint.breakText(arr, c, count, maxWidth, null);
					int end = c + index;

					if(end < arr.length){
						while(arr[end] != ' ') {
							end--;
							index--;
						}
						end++;
						index++;
					}
					
					if(c == end) {
						// something is wrong... we are going into an infinite loop
						// to break out, let's just draw the rest of the text...
						DebugLog.e(TAG, "infinite loop with string: " + s);
						index = count;
						end = arr.length;
					}
					
					canvas.drawText(arr, c, index, 0, hh, fontPaint);
					c = end;
					hh += heightOfOneLine;
				}
			}
			
			//h = hh - heightOfOneLine;
		} else {
			for(String s : lines){
				canvas.drawText(s, 0, hh, fontPaint);
				hh += heightOfOneLine;
			}	
		}
		setTextureHandle(BitmapUtils.loadBitmap(bitmap, textureHandle));
	}
	
	public void setAlignment(int alignment){
		this.alignment = alignment;
	}
 
	@Override 
	public void draw(float offX, float offY) {
		switch(alignment) {
		case ALIGN_LEFT:
			super.draw(offX, offY);
			break;
		case ALIGN_RIGHT:
			super.draw(offX - w, offY);
			break;
		}
	}
	
	@Override
	public void refresh() {
		measure();
		generateTexture();
	}

	public String getText() {
		return text;
	}
	
	public void setMaxWidth(float maxWidth) {
		if(maxWidth <= 0) {
			DebugLog.e(TAG, "Max width must be > 0.", new Exception());
			return;
		}
		
		this.maxWidth = (int) maxWidth;
	}
}