package com.ggstudios.divisionbyzero;

import java.util.ArrayList;
import java.util.List;

import com.ggstudios.divisionbyzero.Button.OnClickListener;
import com.ggstudios.utils.BitmapUtils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;

public class InGameMenu extends Drawable implements Clickable {
	private static final String TAG = "InGameMenu";

	private static final float FONT_SIZE = 0.6f; 	// in SDPs

	private static final float ITEM_HEIGHT = 1f;	// in SDPs
	private static final float ITEM_LR_MARGIN = 0.3f;

	private static final String[] MENU_ITEMS = new String[] {
		"Restart",
		"Settings",
		"Quit"
	};

	private List<Drawable> drawables = new ArrayList<Drawable>();
	private List<Clickable> clickables = new ArrayList<Clickable>();

	private Paint textPaint;
	private float x, y, w, h;

	private int textureHandle;

	private PictureBox bg;

	private boolean visible = false;

	public InGameMenu() {
		textPaint = new Paint();
	}

	public void build(float menuButtonW, float menuButtonH) {
		textPaint.setTextSize(FONT_SIZE * Core.SDP);
		textPaint.setColor(Color.WHITE);

		float maxWidth = 0;

		for(int i = 0; i < MENU_ITEMS.length; i++) {
			float w = textPaint.measureText(MENU_ITEMS[i]);
			if(maxWidth < w) {
				maxWidth = w;
			}
		}

		float itemHeight = ITEM_HEIGHT * Core.SDP;
		float height = itemHeight * MENU_ITEMS.length;

		w = maxWidth + menuButtonW + ITEM_LR_MARGIN * Core.SDP * 2f;
		h = Math.max(height, menuButtonH);

		generateBg();
		drawables.add(bg);

		Paint bg = new Paint();
		bg.setColor(0x00000000);

		float bx = menuButtonW, by = 0f;
		for(int i = 0; i < MENU_ITEMS.length; i++) {
			Button temp = new Button(bx, by, w - menuButtonW, itemHeight, MENU_ITEMS[i], textPaint, bg);
			by += itemHeight;

			switch(i) {
			case 0:
				temp.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(Button sender) {
						Core.handler.post(new Runnable() {

							@Override
							public void run() {
								Core.game.restart();
							}
							
						});
					}
					
				});
				break;
			case 2:
				temp.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(Button sender) {
						Core.handler.sendEmptyMessage(MainActivity.MSG_FINISH);
					}
					
				});
				break;
			default:
				break;
			}

			drawables.add(temp);
			clickables.add(temp);
		}
	}

	private void generateBg() {
		android.graphics.drawable.Drawable drawable = Core.context.getResources().getDrawable(R.drawable.menu_bg);
		drawable.setBounds(0, 0, (int)w, (int)h);

		Bitmap bitmap = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.draw(canvas);

		textureHandle = BitmapUtils.loadBitmap(bitmap);

		bg = new PictureBox(0, 0, w, h, -1);
		bg.setTextureHandle(textureHandle);
	}

	@Override
	public void draw(float offX, float offY) {
		if(!visible) return;
		for(Drawable d : drawables) {
			d.draw(x, y);
		}
	}

	@Override
	public void refresh() {
		generateBg();
		for(Drawable d : drawables) {
			d.refresh();
		}
	}

	public float getWidth() {
		return w;
	}

	public float getHeight() {
		return h;
	}

	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void show() {
		visible = true;
	}

	public void hide() {
		visible = false;
	}
	
	@Override
	public boolean onTouchEvent(int action, int x_, int y_) {
		if(!visible) return false;
		final int x = (int) (x_ - this.x);
		final int y = (int) (y_ - this.y);
		for(Clickable c : clickables) {
			if(c.onTouchEvent(action, x, y)) return true;
		}
		
		if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
			hide();
		}
		return false;
	}
}
