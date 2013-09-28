package com.ggstudios.divisionbyzero;

import com.ggstudios.divisionbyzero.Button.OnClickListener;
import com.ggstudios.divisionbyzero.TowerLibrary.TowerEvoTree;
import com.ggstudios.utils.BitmapUtils;
import com.ggstudios.utils.DebugLog;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.view.MotionEvent;

public class UpgradeWindow extends Drawable implements Clickable{
	private static final String TAG = "UpgradeWindow";
	
	private static final String VALUE_NA = "--";
	
	private int ninePatchResId;
	
	private static final int WIDTH = 16;
	private static final int HEIGHT = 10;
	
	private static final int MAX_OPTIONS = 3;

	private float width, height;
	private float x, y;
	
	private Button btnClose;
	
	private Tower t;
	private boolean visible = true;
	
	private PictureBox bg;
	private Label lblTitle;
	
	private Paint fontPaint;
	private Paint fontPaintItem;
	
	private Rect rect;
	
	private int optionCount;
	private UpgradeOption[] options = new UpgradeOption[MAX_OPTIONS];
	
	public static interface OnUpgradeSelectedListener {
		public void onUpgradeSelected(Tower t, int selection);
	}

	private OnUpgradeSelectedListener upgradeListener;
	
	public UpgradeWindow(int bg9PatchResId) {
		ninePatchResId = bg9PatchResId;
	}
	
	public void setOnCloseClick(OnClickListener listener) {
		btnClose.setOnClickListener(listener);
	}
	
	public void build() {
		DebugLog.d(TAG, "Building UpgradeWindow...");
		
		width = WIDTH * Core.SDP;
		height = HEIGHT * Core.SDP;
		
		x = (Core.canvasWidth - width) / 2.0f;
		y = (Core.canvasHeight - height) / 2.0f;
		
		bg = new PictureBox(x, y, width, height, -1);
		
		fontPaint = new Paint();
		fontPaint.setAntiAlias(true);
		fontPaint.setColor(0xFFFFFFFF);
		fontPaint.setTextSize(Core.SDP_H);
		
		fontPaintItem = new Paint();
		fontPaintItem.setAntiAlias(true);
		fontPaintItem.setColor(0xFFFFFFFF);
		fontPaintItem.setTextSize(Core.SDP_H * 0.9f);
		
		float w = Core.SDP * 0.5f;
		float h = w;
		btnClose = new Button(x + width - Core.SDP_H - w, y + Core.SDP_H, w, h, R.drawable.close);
		btnClose.setPadding((int) Core.SDP_H);
		
		lblTitle = new Label(x + Core.SDP_H, y + Core.SDP_H, fontPaint, "Upgrade Tower");
		
		rect = new Rect();
		rect.left = (int) x;
		rect.top = (int) y;
		rect.right = (int) (rect.left + width);
		rect.bottom = (int) (rect.top + height);
		
		float optionW = (width - Core.SDP * 2.0f) / options.length;
		float optionH = (height - lblTitle.h - Core.SDP * 2.0f);
		float cumX = x + Core.SDP;
		for(int i = 0; i < options.length; i++) {
			options[i] = new UpgradeOption(cumX, lblTitle.y + lblTitle.h, optionW, optionH);
			cumX += optionW;
		}
		itemBg = new PictureBox(0, 0, options[0].w, options[0].h, -1);
		selectedItemBg = new PictureBox(0, 0, options[0].w, options[0].h, -1);
		
		itemRect.right = (int) options[0].w;
		itemRect.bottom = (int) options[0].h;

		setupBg();
	}
	
	private void setupBg() {
		NinePatchDrawable drawable = (NinePatchDrawable) Core.context.getResources().getDrawable(ninePatchResId);
		drawable.setBounds(0, 0, (int)width, (int)height);
		
		Bitmap bgBitmap = Bitmap.createBitmap((int)width, (int)height, Bitmap.Config.ARGB_4444);
		Canvas canvas = new Canvas(bgBitmap);
		drawable.draw(canvas);
		bg.setTextureHandle(BitmapUtils.loadBitmap(bgBitmap, bg.textureHandle));
		
		drawable = (NinePatchDrawable) Core.context.getResources().getDrawable(R.drawable.item_border);
		drawable.setBounds(0, 0, (int)options[0].w, (int)options[0].h);
		
		bgBitmap = Bitmap.createBitmap((int)options[0].w, (int)options[0].h, Bitmap.Config.ARGB_4444);
		canvas = new Canvas(bgBitmap);
		drawable.draw(canvas);
		itemBg.setTextureHandle(BitmapUtils.loadBitmap(bgBitmap, itemBg.textureHandle));
	}

	public void updateContent(Tower t) {
		this.t = t;
		TowerEvoTree[] evo = t.evoTree.typeUpgrade[t.level + 1];
		
		optionCount = evo.length;
		if(!t.isMaxLevel()) {
			optionCount++;
		}
		
		float spacing = (width - optionCount * options[0].w) / (optionCount + 1);
		
		float cumX = spacing;
		for(int i = 0; i < evo.length; i++) {
			options[i].updateContent(evo[i], 0);
			options[i].x = x + cumX;
			cumX += spacing + options[i].w;
		}
		
		if(!t.isMaxLevel()) {
			options[optionCount - 1].updateContent(t.evoTree, t.level + 1);
			options[optionCount - 1].x = x + cumX;
		}
		
		DebugLog.d(TAG, "Options: " + optionCount);
	}

	@Override
	public void draw(float offX, float offY) {
		if(visible) {
			bg.draw(0, 0);
			btnClose.draw(0, 0);
			lblTitle.draw(0, 0);
			
			for(int i = 0; i < optionCount; i++) {
				options[i].draw(0, 0);
			}
		}
	}

	@Override
	public void refresh() {
		setupBg();
		bg.refresh();
		itemBg.refresh();
		btnClose.refresh();
		lblTitle.refresh();
		selectedItemBg.refresh();
		
		for(int i = 0; i < options.length; i++)
			options[i].refresh();
	}

	private void onItemSelected(int i) {
		upgradeListener.onUpgradeSelected(t, i);
	}
	
	@Override
	public boolean onTouchEvent(int action, int x_, int y_) {
		if (!visible) return false;

		final int x = Core.originalTouchX;
		final int y = Core.originalTouchY;
		
		switch(action) {
		case MotionEvent.ACTION_DOWN:
			for(int i = 0; i < optionCount; i++) {
				if(itemRect.contains((int)(x - options[i].x), (int) (y - options[i].y))) {
					options[i].selected = true;
					
					onItemSelected(i);
				} else {
					options[i].selected = false;
				}
			}
			return rect.contains(x, y);
		default:
			btnClose.onTouchEvent(action, x, y);
			return rect.contains(x, y);
		}
		
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
		this.btnClose.onTouchEvent(MotionEvent.ACTION_CANCEL, 0, 0);
	}

	public void setOnUpgradeSelectedListener(OnUpgradeSelectedListener upgradeListener) {
		this.upgradeListener = upgradeListener;
	}

	private PictureBox itemBg, selectedItemBg;
	private Rect itemRect = new Rect();
	
	private class UpgradeOption extends Drawable implements Clickable {

		private static final float MARGIN = 0.5f;
		
		private float w, h;
		private float x, y;
		
		private PictureBox ic;
		private Label lblDmg, lblRange, lblAtkSpeed, lblDesc;
		
		private DrawableString dsCost;
		private DrawableString dsName;
		private DrawableString dsDmg;
		private DrawableString dsRange;
		private DrawableString dsAs;
		private DrawableString dsDesc;
		
		private boolean selected = false;
		
		public UpgradeOption(float x, float y, float w, float h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			
			final float mid = w / 2f;
			final float divide = 3 * w / 5.0f;
			final float margin = MARGIN * Core.SDP;
			final float margin2 = MARGIN * Core.SDP * 2.0f;
			
			float posY = margin / 2f;
			
			dsName = new DrawableString(margin, posY, Core.fm, "");
			posY += dsName.height + margin;
			
			ic = new PictureBox(margin + Core.SDP_H, posY, 
					w - margin2 - Core.SDP, w - margin2 - Core.SDP, -1);
			posY += ic.h + margin;
			dsCost = new DrawableString(mid, posY, Core.fm, "", DrawableString.ALIGN_CENTER);
			posY += dsCost.height + margin;
			lblDmg = new Label(0, posY, fontPaintItem, "Damage:");
			lblDmg.x = divide - lblDmg.w;
			dsDmg = new DrawableString(divide, posY, Core.fm, "");
			posY += lblDmg.h;
			lblRange = new Label(0, posY, fontPaintItem, "Range:");
			lblRange.x = divide - lblRange.w;
			dsRange = new DrawableString(divide, posY, Core.fm, "");
			posY += lblRange.h;
			lblAtkSpeed = new Label(0, posY, fontPaintItem, "Atk. Speed:");
			lblAtkSpeed.x = divide - lblAtkSpeed.w;
			dsAs = new DrawableString(divide, posY, Core.fm, "");
			posY += lblAtkSpeed.h;
			lblDesc = new Label(0, posY, fontPaintItem, "Range:");
			lblDesc.x = divide - lblDesc.w;
			posY += lblDesc.h;
			
			dsDesc = new DrawableString(margin, posY, Core.fm, "");
		}
		
		private void setDsValue(DrawableString ds, int value) {
			if(value > 0)
				ds.setText(String.valueOf(value));
			else
				ds.setText(VALUE_NA);
		}
		
		private void setDsValue(DrawableString ds, float value) {
			if(value > 0)
				ds.setText(String.valueOf(value));
			else
				ds.setText(VALUE_NA);
		}

		public void updateContent(final TowerEvoTree evoTree, final int level) {
			ic.setTexture(evoTree.resId[level]);
			dsCost.setText("$" + String.valueOf(evoTree.cost[level]));
			dsName.setText(evoTree.name[level]);
			
			setDsValue(dsDmg, evoTree.dmg[level]);
			setDsValue(dsRange, evoTree.range[level]);
			setDsValue(dsAs, evoTree.as[level]);

			//final float margin2 = MARGIN * Core.SDP * 2.0f;
			//dsDesc.setText("This is a test. This is a test. This is a test.", (int)(w - margin2));
		}
		
		@Override
		public void draw(float offX, float offY) {
//			if(!selected) {
				itemBg.draw(x, y);
//			} else {
//				selectedItemBg.draw(x, y);
//			}
			dsName.draw(x, y);
			dsCost.draw(x, y);
			dsDmg.draw(x, y);
			dsRange.draw(x, y);
			dsAs.draw(x, y);
			dsDesc.draw(x, y);
			
			ic.draw(x, y);
			lblDmg.draw(x, y);
			lblRange.draw(x, y);
			lblAtkSpeed.draw(x, y);
		}

		@Override
		public void refresh() {
			dsName.refresh();
			dsCost.refresh();
			dsDmg.refresh();
			dsRange.refresh();
			dsAs.refresh();
			dsDesc.refresh();
			
			ic.refresh();
			lblDmg.refresh();
			lblRange.refresh();
			lblAtkSpeed.refresh();
		}

		@Override
		public boolean onTouchEvent(int action, int x, int y) {
			// TODO Auto-generated method stub
			return false;
		}
	}
}
