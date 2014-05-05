package com.ggstudios.divisionbyzero;

import com.ggstudios.divisionbyzero.Button.OnClickListener;

import android.graphics.Paint;

public class PauseMenu extends Drawable implements Clickable {

	PictureBox bg;
	Label paused;
	Button btnResume;
	
	public PauseMenu() {}
	
	public void build() {
		Paint paint = new Paint();
		paint.setColor(0xFFFFFFFF);
		paint.setTextSize(Core.SDP * 1.5f);
		
		bg = new PictureBox(0, 0, 
				Core.canvasWidth, Core.canvasHeight, R.drawable.right_panel);
		paused = new Label(0, 0, paint, "Paused");
		paused.x = (Core.canvasWidth - paused.w) / 2.0f;
		paused.y = (Core.canvasHeight - paused.h) / 2.0f;

		Paint paint2 = new Paint();
		paint2.setColor(0xFFFFFFFF);
		paint2.setTextSize(Core.SDP * 0.8f);
		paint2.setAntiAlias(true);
		float btnW = Core.SDP * 4.0f, btnH = Core.SDP * 4.0f;
		btnResume = new Button(Core.canvasWidth - btnW, Core.canvasHeight - btnH,
				btnW, btnH, R.drawable.button_bg, "Resume", paint2);
		
		btnResume.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				Core.game.onResumeClick();
			}
			
		});
	}
	
	@Override
	public void draw(float offX, float offY) {
		bg.draw(0, 0);
		paused.draw(0, 0);
		btnResume.draw(0, 0);
	}

	@Override
	public void refresh() {
		if(bg == null) return;
		bg.refresh();
		paused.refresh();
		btnResume.refresh();
	}
	
	@Override
	public boolean onTouchEvent(int action, float x_, float y_) {
		final float x = Core.originalTouchX;
		final float y = Core.originalTouchY;
		btnResume.onTouchEvent(action, x, y);
		return true;
	}

}
