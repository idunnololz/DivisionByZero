package com.ggstudios.divisionbyzero;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.NinePatchDrawable;

import com.ggstudios.utils.BitmapUtils;

public abstract class BaseDialog extends PictureBox implements Clickable {
	private int bgResId;
	private boolean dirty = false;
	
	private boolean visible = false;
	
	public BaseDialog() {
		super(0, 0);
	}

	public void setBackgroundTexture(int resId) {
		bgResId = resId;
	}

	private void build() {
		NinePatchDrawable drawable = (NinePatchDrawable) Core.context.getResources().getDrawable(bgResId);
		drawable.setBounds(0, 0, (int)w, (int)h);

		Bitmap bgBitmap = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bgBitmap);
		drawable.draw(canvas);
		setTextureHandle(BitmapUtils.loadBitmap(bgBitmap, textureHandle));
	}
	
	@Override
	public void draw(float offX, float offY) {
		if(dirty) {
			refresh();
			dirty = false;
		}
		super.draw(offX, offY);
	}
	
	public void notifyChanged() {
		dirty = true;
	}

	@Override
	public void refresh() {
		super.refresh();
		if(bgResId == 0) return;
		build();
	}

	public void show() {
		if(visible) return;
		
		visible = true;
		Core.game.dialogs.addDrawableToTop(this);
	}

	public void hide() {
		if(!visible) return;
		
		visible = false;
		Core.game.dialogs.removeDrawableStrict(this);
	}
	
	public boolean isCancelable() {
		return true;
	}

	public void dismiss() {
		if(isCancelable()) {
			hide();
		}
	}
}
