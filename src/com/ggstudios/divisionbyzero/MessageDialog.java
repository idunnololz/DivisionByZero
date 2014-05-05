package com.ggstudios.divisionbyzero;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;

public class MessageDialog extends BaseDialog {
	private static final float DESIRED_WIDTH = 7f;
	private static final float WIDTH_PERCENT = 0.7f;
	private static final float MARGIN = 0.5f;
	
	private Paint textPaint;
	private Paint hintPaint;
	private String msg;
	private Label lblMsg;
	private Label lblHint;
	private Rect bounds;
	private PictureBox img;
	
	private List<Drawable> drawables = new ArrayList<Drawable>();
	
	private Rectangle bg;
	
	private int imgResId;
	
	private boolean showHint = false;
	private boolean pauseOnShown = true;
	
	private OnDismissListener dismissListener;
	
	public static interface OnDismissListener {
		public void onDismiss();
	}
	
	public MessageDialog() { 
		bounds = new Rect();
		
		textPaint = new Paint();
		textPaint.setAntiAlias(true);
		textPaint.setColor(Color.WHITE);
		textPaint.setTextSize(Core.fm.getFontSize());

		hintPaint = new Paint(textPaint);
		hintPaint.setColor(Color.LTGRAY);
	}
	
	public void setMessage(String msg) {
		this.msg = msg;
	}
	
	public void setOnDismissListener(OnDismissListener listener) {
		dismissListener = listener;
	}
	
	public void setShowHint(boolean b) {
		showHint = b;
	}
	
	public void setImage(int resId) {
		imgResId = resId;
	}
	
	private void updateBounds() {
		bounds.left = 0;
		bounds.top = 0;
		bounds.right = (int) w;
		bounds.bottom = (int) h;
	}
	
	public void build() {
		w = Math.min(Core.canvasWidth * WIDTH_PERCENT, DESIRED_WIDTH * Core.SDP);
		final float margin = MARGIN * Core.SDP;
		
		setBackgroundTexture(R.drawable.panel);
		
		float offX = margin;
		
		if(imgResId != 0) {
			float imgW = Core.SDP * 2f;
			float imgH = imgW;
			img = new PictureBox(margin, margin, imgW, imgH, imgResId);
			
			offX = margin * 2 + imgW;
			
			drawables.add(img);
		}
		
		lblMsg = new Label(offX, margin, textPaint, msg, w - margin - offX);
		
		drawables.add(lblMsg);
		
		if(showHint) {
			lblHint = new Label(0, lblMsg.y + lblMsg.h + textPaint.getTextSize(), hintPaint, "Tap to continue >");
			lblHint.x = w - margin - lblHint.w;
			h = lblMsg.h + textPaint.getTextSize() + lblHint.h + margin * 2;
			
			drawables.add(lblHint);
		} else {
			h = lblMsg.h + margin * 2;
		}
		
		x = (Core.canvasWidth - w) / 2f;
		y = (Core.canvasHeight - h) / 2f;

		bg = new Rectangle(0, 0, Core.canvasWidth, Core.canvasHeight, 0xFF000000);
		bg.transparency = 0.1f;
		
		updateBounds();
		
		super.refresh();
	}
	
	@Override
	public void draw(float offX, float offY) {
		bg.draw(0, 0);
		super.draw(0, 0);
		
		for(Drawable d : drawables)
			d.draw(x, y);
	}
	
	@Override
	public void refresh() {
		super.refresh();
		bg.refresh();
		
		for(Drawable d : drawables)
			d.refresh();
	}
	
	@Override
	public boolean onTouchEvent(int action, float x_, float y_) {
	//	final int x = (int) (Core.originalTouchX - this.x);
	//	final int y = (int) (Core.originalTouchY - this.y);
		if(action == MotionEvent.ACTION_UP) {
			hide();
			return true;
		}
		
		return true;
	}
	
	@Override
	public void show() {
		super.show();
		if(pauseOnShown)
			Core.gu.pause();
	}
	
	@Override
	public void hide() {
		if(dismissListener != null) {
			dismissListener.onDismiss();
		}
		super.hide();
		Core.gu.unpause();
	}

	public void setPauseOnShown(boolean b) {
		pauseOnShown = b;
	}
}
