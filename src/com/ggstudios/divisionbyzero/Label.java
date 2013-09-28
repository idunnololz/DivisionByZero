package com.ggstudios.divisionbyzero;

import com.ggstudios.utils.BitmapUtils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Label extends PictureBox {
	private Paint fontPaint;

	private String text;

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
		generateTexture();
	}

	public Label(float x, float y, Paint painter, String text) {
		super(x, y);

		fontPaint = painter;

		this.text = text;
		generateTexture();
	}

	public Label(float x, float y, Paint painter, String text, float fontSize) {
		super(x, y);

		fontPaint = painter;

		this.text = text;
		float temp = fontPaint.getTextSize();
		if(temp == fontSize)
			generateTexture();
		else {
			fontPaint.setTextSize(fontSize);
			generateTexture();
		}
	}

	public void setLocation(float x, float y){
		this.x = x;
		this.y = y;
	}

	public void setText(String text){
		if(this.text.equals(text)) return;

		this.text = text;
		Core.glView.queueEvent(rebuild);
	}

	private void generateTexture(){
		if(text == ""){ 
			this.textureHandle = -1;
			return;
		}

		String[] lines = text.split("\n");
		
		int numLines = lines.length;
		
		if(maxWidth > 0) { 
			for(String s : lines){
				int c = 0;
				char[] arr = s.toCharArray();
				while(c < arr.length) {
					int index = fontPaint.breakText(arr, c, arr.length - c, maxWidth, null);
					
					if(c != 0)
						numLines++;
					
					c += index;
				}
			}
		}
		
		Rect bounds = new Rect();
		fontPaint.getTextBounds(text, 0, text.length(), bounds);
		h = (bounds.bottom - bounds.top) * numLines;
		
		w = fontPaint.measureText(text);
		float heightOfOneLine = fontPaint.descent() - fontPaint.ascent();

		drawingW = Utils.findSmallestBase2((int)w);
		drawingH = Utils.findSmallestBase2((int)h);

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
					int index = fontPaint.breakText(arr, c, arr.length - c, maxWidth, null);
					
					int end = c + index;
					if(end < arr.length){
						while(arr[end] != ' ') {
							end--;
							index--;
						}
						end++;
						index++;
					}
					
					canvas.drawText(arr, c, index, 0, hh, fontPaint);
					c = end;
					hh += heightOfOneLine;
				}
			}
			
			h = hh - heightOfOneLine;
		} else {
			for(String s : lines){
				canvas.drawText(s, 0, hh, fontPaint);
				hh += heightOfOneLine;
			}	
		}
		setTextureHandle(BitmapUtils.loadBitmap(bitmap, textureHandle));
	}

	@Override
	public void refresh() {
		generateTexture();
	}

	public String getText() {
		return text;
	}
	
	public void setMaxWidth(float maxWidth) {
		this.maxWidth = (int) maxWidth;
	}
}

