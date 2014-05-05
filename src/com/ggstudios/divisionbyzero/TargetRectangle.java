package com.ggstudios.divisionbyzero;

import com.ggstudios.utils.BitmapUtils;
import com.ggstudios.utils.BufferUtils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * The main purpose of this class is to provide a way to draw attention
 * to something on screen through the use of some UI element.
 * @author iDunnololz
 *
 */
public class TargetRectangle extends PictureBox implements Updatable{
	// used for animation...
	private static final float DURATION = 0.3f;
	private boolean state = false;
	private float time = 0f;
	
	/**
	 * Used for drawing the rectangle...
	 */
	Paint paint;
	
	public TargetRectangle() {
		super(0, 0);
		
		paint = new Paint();
	}
	
	public void setColor(int c) {
		paint.setColor(c);
	}
	
	public void setStrokeWidth(float w) {
		paint.setStrokeWidth(w);
	}
		
	public void setBounds(RectF rect) {
		w = rect.right - rect.left;
		h = rect.bottom - rect.top;
		x = rect.left + w/2f;
		y = rect.top + h/2f;
	}
	
	public void build() {
		Bitmap bmp = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bmp);
		paint.setStyle(Paint.Style.STROKE);
		c.drawRect(0, 0, w, h, paint);
		setTextureHandle(BitmapUtils.loadBitmap(bmp));
		
		float halfW = w / 2f;
		float halfH = h / 2f;
		float[] rect = {
			-halfW, -halfH,
			halfW, -halfH,
			-halfW, halfH,
			halfW, halfH,
		};
		
		handle = BufferUtils.copyToBuffer(rect);
	}
	
	@Override
	public void draw(float _x, float _y) {
		super.draw(0, 0);
	}

	@Override
	public boolean update(float dt) {
		time += dt;
		if(time > DURATION) {
			state = !state;
			if(state) {
				setScale(1.05f);
			} else {
				setScale(1f);
			}
			time -= DURATION;
		}
		return true;
	}

	@Override
	public void refresh() {
		build();
	}
}
