package com.ggstudios.divisionbyzero;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.NinePatchDrawable;

import com.ggstudios.utils.BitmapUtils;

public class BaseDialog extends PictureBox {

	private int bgResId;

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
	public void refresh() {
		super.refresh();
		build();
	}
}
