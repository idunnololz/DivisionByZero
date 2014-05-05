package com.ggstudios.divisionbyzero;

import com.ggstudios.divisionbyzero.Button.OnClickListener;
import com.ggstudios.divisionbyzero.TowerLibrary.TowerEvoTree;
import com.ggstudios.utils.BitmapUtils;
import com.ggstudios.utils.DebugLog;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.view.MotionEvent;

public class UpgradeDialog extends BaseDialog implements Clickable{
	private static final String TAG = "UpgradeDialog";

	private static final int WIDTH = 16;
	private static final int HEIGHT = 10;

	private static final int MAX_OPTIONS = 3;

	private Button btnClose;

	private Tower t;

	private Label lblTitle;

	private Paint fontPaint;
	private Paint fontPaintItem;

	private Rect rect;

	private int selectedIndex = -1;
	private int selectedIndexCost = -1;

	private int optionCount;
	private UpgradeOption[] options = new UpgradeOption[MAX_OPTIONS];
	private Button btnUpgrade;

	public static interface OnUpgradeSelectedListener {
		public void onUpgradeSelected(Tower t, int selection, int cost);
	}

	private OnUpgradeSelectedListener upgradeListener;

	public UpgradeDialog() {
	}

	public void setOnCloseClick(OnClickListener listener) {
		btnClose.setOnClickListener(listener);
	}

	public void build() {
		DebugLog.d(TAG, "Building UpgradeWindow...");

		w = WIDTH * Core.SDP;
		h = HEIGHT * Core.SDP;

		x = (Core.canvasWidth - w) / 2.0f;
		y = (Core.canvasHeight - h) / 2.0f;

		setBackgroundTexture(R.drawable.panel);
		super.refresh();

		fontPaint = new Paint();
		fontPaint.setAntiAlias(true);
		fontPaint.setColor(0xFFFFFFFF);
		fontPaint.setTextSize(Core.SDP_H);

		fontPaintItem = new Paint();
		fontPaintItem.setAntiAlias(true);
		fontPaintItem.setColor(0xFFFFFFFF);
		fontPaintItem.setTextSize(Core.SDP_H * 0.9f);

		float btnW = Core.SDP * 0.5f;
		float btnH = btnW;
		btnClose = new Button(w - Core.SDP_H - btnW, Core.SDP_H, btnW, btnH, R.drawable.close);
		btnClose.setPadding((int) Core.SDP_H);

		lblTitle = new Label(Core.SDP_H, Core.SDP_H, fontPaint, "Upgrade Tower");

		rect = new Rect();
		rect.left = (int) 0;
		rect.top = (int) 0;
		rect.right = (int) (rect.left + w);
		rect.bottom = (int) (rect.top + h);

		float optionW = (w - Core.SDP);
		float optionH = (h - lblTitle.h - Core.SDP * 1.5f) / options.length;
		float optionX = Core.SDP_H;
		float optionY = lblTitle.y + lblTitle.h + Core.SDP_H;
		for(int i = 0; i < options.length; i++) {
			options[i] = new UpgradeOption(optionX, optionY, optionW, optionH);
			optionY += optionH;
		}
		itemBg = new PictureBox(0, 0, options[0].w, options[0].h, -1);
		selectedItemBg = new PictureBox(0, 0, options[0].w, options[0].h, -1);

		itemRect.right = (int) options[0].w;
		itemRect.bottom = (int) options[0].h;

		Paint paint = new Paint();
		paint.setTextSize(Core.SDP * 0.4f);
		paint.setAntiAlias(true);
		paint.setColor(0xFFFFFFFF);
		btnUpgrade = new Button(w - Core.SDP * 5f, Core.SDP * 0.2f, Core.SDP * 3.5f, Core.SDP, 
				R.drawable.dialog_button_disabled, "Select an upgrade", paint);
		btnUpgrade.setEnabled(false);
		btnUpgrade.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				upgradeListener.onUpgradeSelected(t, selectedIndex, selectedIndexCost);
			}

		});

		setupTextures();
	}

	private void setupTextures() {
		NinePatchDrawable drawable = (NinePatchDrawable) Core.context.getResources().getDrawable(R.drawable.item_border);
		drawable.setBounds(0, 0, (int)options[0].w, (int)options[0].h);

		Bitmap bgBitmap = Bitmap.createBitmap((int)options[0].w, (int)options[0].h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bgBitmap);
		drawable.draw(canvas);
		itemBg.setTextureHandle(BitmapUtils.loadBitmap(bgBitmap, itemBg.textureHandle));


		drawable = (NinePatchDrawable) Core.context.getResources().getDrawable(R.drawable.item_border_selected);
		drawable.setBounds(0, 0, (int)options[0].w, (int)options[0].h);

		bgBitmap = Bitmap.createBitmap((int)options[0].w, (int)options[0].h, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bgBitmap);
		drawable.draw(canvas);
		selectedItemBg.setTextureHandle(BitmapUtils.loadBitmap(bgBitmap, itemBg.textureHandle));
	}

	public void updateContent(Tower t) {
		this.t = t;
		cancelSelection();

		TowerEvoTree[] evo = t.evoTree.typeUpgrade[t.level + 1];

		btnUpgrade.setText("Select an upgrade");
		btnUpgrade.setEnabled(false);
		btnUpgrade.setTexture(R.drawable.dialog_button_disabled);

		optionCount = evo.length;
		if(!t.isMaxLevel()) {
			optionCount++;
		}

		int i = 0;
		// order it so that the next upgrade appears in front...
		if(!t.isMaxLevel()) {
			options[0].updateContent(t.evoTree, t.level + 1);
			i++;
		}
		
		for(int j = 0; j < evo.length; j++, i++) {
			options[i].updateContent(evo[j], 0);
		}
	}

	@Override
	public void draw(float offX, float offY) {
		super.draw(0, 0);
		btnClose.draw(x, y);
		lblTitle.draw(x, y);
		btnUpgrade.draw(x, y);

		for(int i = 0; i < optionCount; i++) {
			options[i].draw(x, y);
		}
	}

	@Override
	public void refresh() {
		super.refresh();

		setupTextures();
		itemBg.refresh();
		btnClose.refresh();
		lblTitle.refresh();
		selectedItemBg.refresh();
		btnUpgrade.refresh();

		for(int i = 0; i < options.length; i++)
			options[i].refresh();
	}

	private void onItemSelected(int i) {
		int oldIndex = selectedIndex;
		selectedIndex = i;
		selectedIndexCost = options[i].getCost();
		
		if(oldIndex == -1) {
			btnUpgrade.setText("Upgrade");
			btnUpgrade.setEnabled(true);
			btnUpgrade.setTexture(R.drawable.dialog_button);
		}
	}

	@Override
	public boolean onTouchEvent(int action, float x_, float y_) {
		final float x = Core.originalTouchX - this.x;
		final float y = Core.originalTouchY - this.y;

		switch(action) {
		case MotionEvent.ACTION_DOWN:
			for(int i = 0; i < optionCount; i++) {
				if(itemRect.contains((int)(x - options[i].x), (int) (y - options[i].y))) {
					cancelSelection();
					options[i].selected = true;

					onItemSelected(i);
				}
			}
		default:
			return btnClose.onTouchEvent(action, x, y) ||
					btnUpgrade.onTouchEvent(action, x, y) ||
					/*rect.contains(x, y)*/true;
		}

	}

	private void cancelSelection() {
		selectedIndex = -1;
		selectedIndexCost = -1;
		for(int j = 0; j < optionCount; j++) {
			options[j].selected = false;
		}
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
		private StatBar statDmg, statRange, statAtkSpeed;

		private boolean selected = false;
		private int cost = 0;
		
		public UpgradeOption(float x, float y, float w, float h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;

			final float margin = MARGIN * Core.SDP_H;

			float posY = margin;
			
			final float textRight = w - margin;

			dsName = new DrawableString(margin, posY, Core.fm, "");
			dsCost = new DrawableString(textRight, posY, Core.fm, "", DrawableString.ALIGN_RIGHT);
			posY += dsName.height;

			ic = new PictureBox(margin, posY, h - posY - margin, h - posY - margin, -1);
			posY += margin/2f;
			
			final float marginL = Core.SDP * 0.3f;
			final float marginR = marginL;
			final float marginT = Core.SDP * 0.2f;
			final float statW = Core.SDP * 1.9f, statH = Core.SDP * 0.25f;
			final float statMarginT = Core.SDP * 0.1f;

			final float textLeft = ic.x + ic.w + marginL;

			lblDmg = new Label(textLeft, posY, fontPaintItem, "Damage");
			statDmg = new StatBar(lblDmg.x + lblDmg.w + marginL, lblDmg.y + statMarginT, statW, statH);
			lblRange = new Label(statDmg.x + statDmg.w + marginL, posY, fontPaintItem, "Range");
			statRange = new StatBar(lblRange.x + lblRange.w + marginL, lblRange.y + statMarginT, statW, statH);
			lblAtkSpeed = new Label(statRange.x + statRange.w + marginL, posY, fontPaintItem, "Atk. Speed");
			statAtkSpeed = new StatBar(lblAtkSpeed.x + lblAtkSpeed.w + marginL, lblAtkSpeed.y + statMarginT, statW, statH);

			statDmg.setMaxValue(TowerLibrary.MAX_DMG);
			statRange.setMaxValue(TowerLibrary.MAX_RANGE);
			statAtkSpeed.setMaxValue(TowerLibrary.MAX_ATK_SPEED);

			posY += lblDmg.h + marginT;

			Paint descPaint = new Paint(fontPaintItem);
			descPaint.setColor(Color.LTGRAY);
			descPaint.setTextSize(fontPaintItem.getTextSize() * 0.8f);
			lblDesc = new Label(textLeft, posY, descPaint, "Desc here.");
			lblDesc.setMaxWidth(w - lblDesc.x - marginR);
		}

		public void updateContent(final TowerEvoTree evoTree, final int level) {
			ic.setTexture(evoTree.resId[level]);
			cost = evoTree.cost[level];
			dsCost.setText("$" + String.valueOf(cost));
			dsName.setText(evoTree.name[level]);

			statDmg.setValue(evoTree.dmg[level]);
			statRange.setValue(evoTree.range[level]);
			statAtkSpeed.setValue(1f/evoTree.as[level]);

			lblDesc.setText(evoTree.getDescription());
		}
		
		public int getCost() {
			return cost;
		}

		@Override
		public void draw(float offX, float offY) {
			final int x = (int) (offX + this.x);
			final int y = (int) (offY + this.y);

			if(!selected) {
				itemBg.draw(x, y);
			} else {
				selectedItemBg.draw(x, y);
			}
			dsName.draw(x, y);
			dsCost.draw(x, y);
			statDmg.draw(x, y);
			statRange.draw(x, y);
			statAtkSpeed.draw(x, y);

			ic.draw(x, y);
			lblDmg.draw(x, y);
			lblRange.draw(x, y);
			lblAtkSpeed.draw(x, y);
			lblDesc.draw(x, y);
		}

		@Override
		public void refresh() {
			dsName.refresh();
			dsCost.refresh();
			statDmg.refresh();
			statRange.refresh();
			statAtkSpeed.refresh();

			ic.refresh();
			lblDmg.refresh();
			lblRange.refresh();
			lblAtkSpeed.refresh();
			lblDesc.refresh();
		}

		@Override
		public boolean onTouchEvent(int action, float x, float y) {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	@Override
	public void show() {
		super.show();
		Core.gu.pause();
	}
	
	@Override
	public void hide() {
		super.hide();
		Core.gu.unpause();
	}
}
