package com.ggstudios.divisionbyzero;

import java.util.ArrayList;
import java.util.List;

import com.ggstudios.utils.BitmapUtils;
import com.ggstudios.utils.BufferUtils;
import com.ggstudios.utils.DebugLog;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import static fix.android.opengl.GLES20.glVertexAttribPointer;

public class TowerMenu extends Drawable implements Clickable, Updatable {
	private static final String TAG = "TowerMenu";
	/*
	 * HUD design:
	 * 
	 * |--------------------------------|
	 * |								|
	 * |								|
	 * |								| 
	 * |								|
	 * |					 ___________|
	 * |					|			|< HUD Tower Menu
	 * |--------------------------------|
	 * 
	 */

	// in SDP
	private static final float MENU_WIDTH = 7.0f;
	private static final float MENU_HEIGHT = 1.75f;

	private static final float ITEM_WIDTH = 1.55f;
	private static final float ITEM_HEIGHT = 1.55f;

	private float itemW;

	private float margin;

	private PictureBox bg;
	private PictureBox selectionBox;

	float width;
	float height;

	float x, y;

	private Rect rect = new Rect();

	private List<Tower> towers = new ArrayList<Tower>();
	private List<Drawable> drawables = new ArrayList<Drawable>();
	private List<Label> labels = new ArrayList<Label>();
	private List<Label> shadows = new ArrayList<Label>();
	private List<Rect> precalculatedClickBounds = new ArrayList<Rect>();

	private int maxDisplay;
	private int index = 0;
	private int end = 0;
	private int drawableLen = 0;
	private boolean itemSelected = false;
	private int itemSelectedIndex = -1;

	private Updatable selectionBoxUpdatable;
	private Rect thisRegion;

	// scroll bar variables:

	// 
	//     |--------| 
	// ____|________|_______
	// ^scrollLeft
	private static final int SCROLL_BAR_COLOR = 0xFF33B5E5;
	private static final float SCROLL_BAR_HEIGHT_SDP = 0.1f;
	private static final float FADE_TIME = 0.5f;
	private static final float SHOW_TIME = 0.5f;

	private float timeLeft;
	private int scrollBarId;
	private int scrollBarHandle;
	private float scrollBarWidth;
	private float scrollX = 0f;
	private float scrollLeft = 0f;	// denote the area to the left of the current scroll position
	private float totalW = 0f;
	private float maxScrollLeft = 0f;
	private float scrollBarTop;
	private float touchSlop;

	public TowerMenu(float x, float y) {
		this.x = x;
		this.y = y;

		width = MENU_WIDTH * Core.SDP;
		height = MENU_HEIGHT * Core.SDP;

		margin = ((MENU_HEIGHT - ITEM_HEIGHT)/ 2f) * Core.SDP;

		itemW = ITEM_WIDTH * Core.SDP + margin * 2;

		maxDisplay = (int)(MENU_WIDTH / ITEM_WIDTH);

		DebugLog.d(TAG, "maxDisplay: " + maxDisplay);

		bg = new PictureBox(0, 0, 
				width, height, R.drawable.right_panel);

		selectionBox = new PictureBox(0, 0, 
				Core.SDP * MENU_HEIGHT, Core.SDP * MENU_HEIGHT, R.drawable.selection_box);

		thisRegion = new Rect();
		thisRegion.left = 0;
		thisRegion.right = (int) width;
		thisRegion.top = 0;
		thisRegion.bottom = (int) height;

		touchSlop = ViewConfiguration.get(Core.context).getScaledTouchSlop();

		setIndex(0);
	}

	public void addTower(Tower tower) {
		towers.add(tower);
	}

	public void build() {
		synchronized(drawables) {
			drawables.clear();
			int n = 0;

			final float w = ITEM_WIDTH * Core.SDP;
			final float h = ITEM_HEIGHT * Core.SDP;

			Paint paintShadow = new Paint();
			paintShadow.setTypeface(Typeface.DEFAULT_BOLD);
			paintShadow.setAntiAlias(true);
			paintShadow.setTextSize(Core.SDP_H * 1.1f);
			paintShadow.setColor(0xB0000000);

			Paint paint = new Paint();
			paint.setTypeface(Typeface.DEFAULT_BOLD);
			paint.setAntiAlias(true);
			paint.setTextSize(Core.SDP_H * 1f);
			paint.setColor(0xE082dbe0);

			for(Tower t : towers) {
				t.build(w, h);
				t.x = itemW * n + margin;
				t.y = margin;
				n++;

				Rect r = new Rect();
				r.top = (int) (t.y);
				r.left = (int) (t.x);
				r.bottom = (int) (r.top + t.h);
				r.right = (int) (r.left + t.w);

				Label l = new Label(t.x, t.y, paint, String.valueOf(t.getCost()));
				l.x += t.w - l.w;//t.x + (t.w - l.w) / 2f;
				l.y += t.h - l.h;
				labels.add(l);

				Label shadow = new Label(l.x, l.y, paintShadow, l.getText());
				shadows.add(shadow);

				drawables.add(t);
				precalculatedClickBounds.add(r);
			}

			drawableLen = drawables.size();
		}
		totalW = towers.size() * itemW;
		maxScrollLeft = totalW - width;
		scrollBarWidth = (maxDisplay / (float)towers.size()) * width;
		genScrollBar((int) scrollBarWidth);
	}

	public void setIndex(int index) {
		this.index = index;
		this.end = index + maxDisplay;

		DebugLog.d(TAG, "Beginning index set to: " + index + " end: " + end);
	}

	@Override
	public void draw(float offX, float offY) {
		bg.draw(x, y);

		// enable clipping...
		GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
		GLES20.glScissor((int)x, 0, (int) (width), (int)Core.canvasHeight);

		if(itemSelected)
			selectionBox.draw(x + scrollLeft, y);

		for(int i = index; i < end && i < drawableLen; i++){
			drawables.get(i).draw(x + scrollLeft, y);
			shadows.get(i).draw(x + scrollLeft, y);
			labels.get(i).draw(x + scrollLeft, y);
		}
		GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

		// draw our scroll bar...
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, scrollBarId);

		Utils.resetMatrix();
		Utils.translateAndCommit(x + scrollX, y + scrollBarTop);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, scrollBarHandle);
		glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, 0, 0);

		Shader.setColorMultiply(1f, 1f, 1f, timeLeft / FADE_TIME);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		Shader.resetColorMultiply();
	}

	@Override
	public void refresh() {
		bg.refresh();
		selectionBox.refresh();
		for(int i = 0; i < drawableLen; i++){
			drawables.get(i).refresh();
			labels.get(i).refresh();
			shadows.get(i).refresh();
		}

		genScrollBar((int) scrollBarWidth);
	}

	@Override
	public boolean update(float dt) {
		if(timeLeft < 0) return true;
		timeLeft -= dt;
		if(timeLeft < 0) timeLeft = 0;
		return true;
	}

	private int lastX, lastY;
	private int startingX, startingY;
	private boolean canceled = false;

	public Tower getSelectedTower() {
		if(itemSelected)
			return towers.get(itemSelectedIndex);
		else
			return null;
	}

	public boolean onTouchEvent(int action, int x, int y) {
		switch(action) {
		case MotionEvent.ACTION_UP:
			if(canceled) return false;

			final int xx = (int) (Core.originalTouchX - this.x - scrollLeft);
			final int yy = (int) (Core.originalTouchY - this.y);

			// only those drawn can be clicked...
			for(int i = index; i < end && i < drawableLen; i++){
				final Rect r = precalculatedClickBounds.get(i);

				if(r.contains(xx, yy)) {
					itemSelectedIndex = i;

					if(!itemSelected) {
						selectionBox.x = r.left - margin;
						itemSelected = true;
					} else {
						if(selectionBoxUpdatable != null)
							Core.gu.removeUiUpdatable(selectionBoxUpdatable);

						selectionBoxUpdatable = new Updatable() {

							private final float FINAL_X = r.left - margin;
							private final float START_X = selectionBox.x;
							private final float d = FINAL_X - START_X;
							private float time = 0.0f;
							private float duration = 1f;

							@Override
							public boolean update(float dt) {
								time += dt;
								if(time < duration) {
									float t = time;
									t /= duration/2;
									if (t < 1){ 
										selectionBox.x = d/2*t*t + START_X;
									} else {
										t--;
										selectionBox.x = -d/2 * (t*(t-2) - 1) + START_X;
									}
									return true;
								} else {
									selectionBox.x = FINAL_X;
									return false;
								}
							}

						};
						Core.gu.addUiUpdatable(selectionBoxUpdatable);
					}
					return true;
				}
			}
			return false;
		case MotionEvent.ACTION_MOVE:
			if(!rect.contains(x, y)) return false;

			if(canceled) {
				// do scrolling here...
				final int deltaX = x - lastX;

				scrollLeft += deltaX;
				if(scrollLeft > 0) scrollLeft = 0;
				else if(scrollLeft < -maxScrollLeft) scrollLeft = -maxScrollLeft;

				scrollX = (-scrollLeft / totalW) * width;

				// calculate which items to draw...
				int startIndex = (int) (-scrollLeft / itemW);
				index = startIndex < 0 ? 0 : startIndex;
				end = startIndex + maxDisplay + 1;

				// refresh fade counter
				timeLeft = FADE_TIME + SHOW_TIME;
			} else {
				final int deltaX2 = x - startingX;
				final int deltaY2 = y - startingY;

				if(Math.abs(deltaX2) > touchSlop || Math.abs(deltaY2) > touchSlop) {
					canceled = true;
				}
			}

			lastX = x;
			lastY = y;

			return true;
		case MotionEvent.ACTION_DOWN:
			if(!rect.contains(x, y)) return false;

			canceled = false;

			lastX = x;
			lastY = y;

			startingX = x;
			startingY = y;

			return true;
		default:
			return false;
		}
	}

	private void genScrollBar(int scrollBarWidth){
		final int scrollBarHeight = (int) (SCROLL_BAR_HEIGHT_SDP * Core.SDP);

		Bitmap bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);

		bitmap.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas(bitmap);

		Paint paint = new Paint();
		paint.setColor(SCROLL_BAR_COLOR);
		canvas.drawRect(0, 0, 4, 4, paint);

		scrollBarTop = height - scrollBarHeight;

		scrollBarId = BitmapUtils.loadBitmap(bitmap, scrollBarId);

		scrollBarHandle = BufferUtils.createRectangleBuffer(scrollBarWidth, scrollBarHeight);

	}

	public void notifyPositionChanged() {
		rect.left = (int) x;
		rect.right = (int) (rect.left + width);
		rect.top = (int) y;
		rect.bottom = (int) (rect.top + height);;
	}
}
