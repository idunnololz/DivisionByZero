package com.ggstudios.divisionbyzero;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;

import com.ggstudios.divisionbyzero.Button.OnClickListener;
import com.ggstudios.utils.BitmapUtils;
import com.ggstudios.utils.DebugLog;

public class PopupMenu extends Drawable implements Clickable {
	private static final String TAG = "PopupMenu";

	private static final int BACKGROUND_RESOURCE = R.drawable.popup_bg;
	private static final int MAX_ITEMS = 6;

	private static final float BUTTON_SIZE = 2f;
	private static final float ICON_Y = 0.3f;
	private static final float ICON_SIZE = 0.9f;
	private static final float LABEL_Y = ICON_Y + ICON_SIZE + 0.1f;
	private static final float DISTANCE_FROM_ORIGIN = 1.5f;

	// duration of popup animation in seconds...
	private static final float POPUP_DURATION = 0.2f;

	private float buttonSize;
	private float distanceFromOrigin;

	private boolean visible = false;

	private ArrayList<MenuItem> items;
	private int itemsVisible = 0;

	private Paint textPaint;

	/**
	 * Creates a popup menu.
	 */
	public PopupMenu() {
		items = new ArrayList<MenuItem>(MAX_ITEMS);
		textPaint = new Paint();
		textPaint.setColor(0xFFFFFFFF);
		textPaint.setTextSize(Core.SDP_H);
	}

	/**
	 * Needs to be called after surface is created;
	 */
	public void onSurfaceCreated() {
		buttonSize = BUTTON_SIZE * Core.SDP;
		distanceFromOrigin = DISTANCE_FROM_ORIGIN * Core.SDP;
	}

	/**
	 * Adds a menu item without a label. Returns the index of the item;
	 * @param resId
	 * @param listener
	 * @return
	 */
	public int addItem(int resId, OnClickListener listener) {
		MenuItem item = new MenuItem(buttonSize, buttonSize, resId, false, listener);
		items.add(item);
		itemsVisible++;

		return items.size() - 1;
	}

	/**
	 * Adds a menu item with a label. Returns the index of the item;
	 * @param resId
	 * @param listener
	 * @return
	 */
	public int addItemWithLabel(int resId, OnClickListener listener) {
		MenuItem item = new MenuItem(buttonSize, buttonSize, resId, true, listener);
		items.add(item);
		itemsVisible++;

		return items.size() - 1;
	}

	public void clearItems() {
		items.clear();
		itemsVisible = 0;
	}

	private Bitmap genBgTexture(){
		Bitmap bitmap = Bitmap.createBitmap(
				Utils.findSmallestBase2((int)buttonSize), 
				Utils.findSmallestBase2((int)buttonSize), 
				Bitmap.Config.ARGB_8888);

		bitmap.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas(bitmap);

		Bitmap bg = BitmapFactory.decodeResource(
				Core.context.getResources(), BACKGROUND_RESOURCE);
		canvas.drawBitmap(bg, new Rect(0, 0, bg.getWidth(), bg.getHeight()), 
				new Rect(0, 0, (int)buttonSize, (int)buttonSize), new Paint());
		bg.recycle();

		return bitmap;
	}

	private void genAllTextures() {
		Bitmap bg = genBgTexture();

		for(MenuItem item : items) {
			item.build(bg);
		}

		bg.recycle();
	}

	/**
	 * Call this function once finished adding items to fin
	 */
	public void build() {
		refreshLayout();

		genAllTextures();
	}

	private void refreshLayout() {
		// depending on the number of items set up the angles
		switch(itemsVisible) {
		case 1:
			items.get(0).angle = (float) Math.PI / 2; 	//      |
			break;
		case 2:
			items.get(0).angle = (float) Math.PI / 2;		//    \   /
			items.get(1).angle = (float) (3 * Math.PI / 2);	//     \ /
			break;
		case 3:
			items.get(0).angle = (float) (5 * Math.PI / 6);		//		|
			items.get(1).angle = (float) (Math.PI / 6);		//		|
			items.get(2).angle = (float) (3 * Math.PI / 2);	// ____	| ____
			break;
		default:
			break;
		}
	}

	/**
	 * Starts the pop up animation around the coordinates given.
	 * @param x
	 * @param y
	 */
	public void popup(float x, float y) {
		for(final MenuItem item : items) {
			if(!item.isVisible) continue;

			final float startX = x - (item.w / 2.0f);
			final float startY = y - (item.h / 2.0f);

			item.transparency = 0.0f;
			item.setPosition(startX, startY);

			Core.gu.addUiUpdatable(new Updatable() {
				final float dX = (float) Math.cos(item.angle) * distanceFromOrigin;
				final float dY = (float) Math.sin(item.angle) * -distanceFromOrigin;

				float time = 0.0f;

				@Override
				public boolean update(float dt) {
					time += dt;
					if(time < POPUP_DURATION) {
						float t = time / POPUP_DURATION;
						item.transparency = t * t;
						item.setPosition(dX * t * t + startX, dY * t * t + startY);
						return true;
					} else {
						item.transparency = 1;
						item.setPosition(startX + dX, startY + dY);
						return false;
					}
				}

			});
		}
		visible = true;
	}

	@Override
	public void draw(float offX, float offY) {
		if(visible) {
			//noob.draw(offX, offY);
			for(MenuItem item: items) {
				item.draw(offX, offY);
			}
		}
	}

	@Override
	public void refresh() {
		genAllTextures();
	}

	@Override
	public boolean onTouchEvent(int action, int x_, int y_) {
		switch(action) {
		case MotionEvent.ACTION_CANCEL:
			for(MenuItem item: items) {
				item.onTouchEvent(action, x_, y_);
			}
			return true;
		default:
			if(!visible) return false;

			final int x = (int) (x_ - Core.offX);
			final int y = (int) (y_ - Core.offY);

			for(MenuItem item: items) {
				if(item.onTouchEvent(action, x, y))
					return true;
			}
			return false;
		}
	}

	public class MenuItem extends Button {
		private boolean hasLabel;
		private int icId;
		private DrawableString label;

		private float angle;

		private MenuItem(float w, float h, int icResId, 
				boolean hasLabel, OnClickListener listener) {

			super(w, h);

			setTextureType(TYPE_TEXTURE_ONLY);

			icId = icResId;
			this.hasLabel = hasLabel;
			setOnClickListener(listener);
		}

		public void show() {
			if(isVisible) return;
			isVisible = true;
			itemsVisible++;
		}

		public void hide() {
			if(!isVisible) return;
			isVisible = false;
			itemsVisible--;
		}

		public void setPosition(float x, float y) {
			final float dx = x - this.x;
			final float dy = y - this.y;

			this.x = x;
			this.y = y;

			if(hasLabel) {
				label.x += dx;
				label.y += dy;
			}

		}

		private void build(Bitmap bg) {
			generateTexture(bg);
			refresh();

			if(hasLabel) {
				if(label == null)
					label = new DrawableString(x + (this.w / 2.0f), y + LABEL_Y * Core.SDP, Core.fm, "",
							DrawableString.ALIGN_CENTER);
				else
					label.refresh();
			}

			DebugLog.d(TAG, "Built. W" + w + "H" + h);
		}

		private void generateTexture(Bitmap initialBitmap) {
			Bitmap bitmap = Bitmap.createBitmap(initialBitmap);
			Canvas canvas = new Canvas(bitmap);

			Rect rect = new Rect();
			float iconSize = ICON_SIZE * Core.SDP;

			if(hasLabel) {
				rect.top = (int) (ICON_Y * Core.SDP);
			} else {
				rect.top = (int) ((h - iconSize) / 2.0f);
			}
			rect.bottom = (int) (rect.top + iconSize);
			rect.left = (int) ((w - iconSize) / 2.0f);
			rect.right = (int) (rect.left + iconSize);

			Bitmap ic = BitmapFactory.decodeResource(
					Core.context.getResources(), icId);
			canvas.drawBitmap(ic, new Rect(0, 0, ic.getWidth(), ic.getHeight()), 
					rect, new Paint());
			ic.recycle();

			int texHandle = BitmapUtils.loadBitmap(bitmap, textureHandle);

			this.drawingW = bitmap.getWidth();
			this.drawingH = bitmap.getHeight();

			setTextureHandle(texHandle);
		}

		public void draw(float offX, float offY) {
			super.draw(offX, offY);

			if(hasLabel)
				label.draw(offX, offY);
		}

		/**
		 * This function should not be called externally. {@link MenuItem#build(Bitmap)} is called instead.
		 */
		@Override
		public final void refresh() {
			super.refresh();
		}

		public void setLabel(String text) {
			if(!hasLabel) {
				DebugLog.e(TAG, "Attempting to set label of item that does not have one.");
				return;
			}
			label.setText(text);
		}
	}

	public MenuItem getItem(int index) {
		return items.get(index);
	}

	public void hide() {
		visible = false;
	}
}
