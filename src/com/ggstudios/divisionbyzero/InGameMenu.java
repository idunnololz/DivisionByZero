package com.ggstudios.divisionbyzero;

import java.util.ArrayList;
import java.util.List;

import com.ggstudios.divisionbyzero.Button.OnClickListener;
import com.ggstudios.utils.BitmapUtils;
import com.ggstudios.utils.DebugLog;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.NinePatchDrawable;
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
	
	private ConfirmDialog verifyDialog;

	public InGameMenu() {
		DebugLog.d(TAG, "Instance created!");
		
		textPaint = new Paint();
		textPaint.setColor(Color.WHITE);
		
		verifyDialog = new ConfirmDialog();
	}

	public void build(float menuButtonW, float menuButtonH) {
		drawables.clear();
		textPaint.setTextSize(FONT_SIZE * Core.SDP);
		
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
		
		bg = new PictureBox(0, 0, w*1.2f, h*1.2f, -1);

		verifyDialog.setMessage("Are you sure you want to restart this level?");
		verifyDialog.setPositive("Yes", new OnClickListener(){

			@Override
			public void onClick(Button sender) {
				verifyDialog.hide();
				Core.game.restart();				
			}
			
		});
		
		verifyDialog.setNegative("No", new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				verifyDialog.hide();
			}
			
		});
		
		verifyDialog.build();

		generateBg(menuButtonH);
		drawables.add(bg);

		float bx = menuButtonW + ITEM_LR_MARGIN * Core.SDP, by = bg.y;
		for(int i = 0; i < MENU_ITEMS.length; i++) {
			Button temp = new Button(bx, by, w - menuButtonW, itemHeight, MENU_ITEMS[i], textPaint);
			by += itemHeight;

			switch(i) {
			case 0:
				temp.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(Button sender) {
						Core.handler.post(new Runnable() {

							@Override
							public void run() {
								hide();
								verifyDialog.show();
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

	private float factor = 0;
	private void generateBg(float desiredSideButtonHeight) {
		DebugLog.d(TAG, "BG gen'd!");
		android.graphics.drawable.NinePatchDrawable drawable = (NinePatchDrawable) Core.context.getResources().getDrawable(R.drawable.menu_bg);
		
		float height = drawable.getIntrinsicHeight();

		// get the factor we need to scale this drawableto make it look good...
		if(desiredSideButtonHeight > 0)
			// subtract the 11 or so pixels protruding upwards...
			factor = desiredSideButtonHeight / (height - 7);
		else {
			if(factor == 0) factor = 1f;
		}
		
		int scaledWidth = (int) (w / factor);
		int scaledHeight = (int) (h / factor);
		drawable.setBounds(0, 0, scaledWidth, scaledHeight);
		
		Bitmap bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.draw(canvas);

		textureHandle = BitmapUtils.loadBitmap(bitmap);

		
		//bg.x -= (w - scaledWidth) * 0.25f;
		bg.y -= (h - scaledHeight) * 0.25f;
		/*
		bg.w *= factor;
		bg.h *= factor;
		*/
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
		DebugLog.d(TAG, "Refreshed!");
		for(Drawable d : drawables) {
			d.refresh();
		}
		generateBg(-1);
		
		verifyDialog.refresh();
	}

	public float getWidth() {
		return w;
	}

	public float getHeight() {
		return h;
	}

	public void setPosition(float x, float y) {
		DebugLog.d(TAG, "Pos set!");
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
	public boolean onTouchEvent(int action, float x_, float y_) {
		if(!visible) return false;
		final float x = x_ - this.x;
		final float y = y_ - this.y;
		for(Clickable c : clickables) {
			if(c.onTouchEvent(action, x, y)) return true;
		}
		
		if(action == MotionEvent.ACTION_UP) {
			hide();
		}
		return false;
	}
}
