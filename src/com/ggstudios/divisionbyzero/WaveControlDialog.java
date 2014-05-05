package com.ggstudios.divisionbyzero;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.ggstudios.divisionbyzero.Button.OnClickListener;
import com.ggstudios.divisionbyzero.LevelManager.SpawnEvent;

public class WaveControlDialog extends BaseDialog implements Clickable, Updatable {

	private static final float WIDTH = 10f;
	private static final float HEIGHT = 6f;
	
	private static final DecimalFormat df = new DecimalFormat("#.##");
	
	private List<Drawable> drawables = new ArrayList<Drawable>();
	private List<DrawableString> vals = new ArrayList<DrawableString>();
	
	private Label lblDesc;
	
	private Button btnClose;
	private RectF rect = new RectF();
	
	public WaveControlDialog () { }
	
	public void build() {
		setBackgroundTexture(R.drawable.panel);
		
		drawables.clear();
		vals.clear();
		
		w = WIDTH * Core.SDP;
		h = HEIGHT * Core.SDP;
		
		x = (Core.canvasWidth - w) / 2f;
		y = (Core.canvasHeight - h) / 2f;
		
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.WHITE);
		paint.setTextSize(Core.fm.getFontSize());
		
		float marginL = Core.SDP_H;
		float marginR = w - Core.SDP_H; 
		float marginT = Core.SDP_H;
		
		rect.left = 0;
		rect.top = 0;
		rect.right = w;
		rect.bottom = h;
		
		float bw = Core.SDP_H;
		btnClose = new Button(w - bw - Core.SDP_H * 0.9f, Core.SDP_H * 0.9f, bw, bw, R.drawable.close);
		btnClose.setPadding((int)Core.SDP_H);
		btnClose.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				hide();
			}
			
		});
		
		DrawableString str = new DrawableString(marginL, marginT, Core.fm, "");
		str.setTextSize(Core.SDP_H);
		marginT += Core.SDP_H * 1.3f;
		Label lblHealth = new Label(marginL, marginT, paint, "Health");
		DrawableString valHealth = new DrawableString(marginR, marginT, Core.fm, "", DrawableString.ALIGN_RIGHT);
		marginT += Core.SDP_H * 1.1f;
		Label lblMs = new Label(marginL, marginT, paint, "Movement speed");
		DrawableString valMs = new DrawableString(marginR, marginT, Core.fm, "", DrawableString.ALIGN_RIGHT);
		marginT += Core.SDP_H * 1.1f;
		Label lblGold = new Label(marginL, marginT, paint, "Gold reward");
		DrawableString valGold = new DrawableString(marginR, marginT, Core.fm, "", DrawableString.ALIGN_RIGHT);
		marginT += Core.SDP_H * 1.1f;
		Label lblCount = new Label(marginL, marginT, paint, "Enemies");
		DrawableString valCount = new DrawableString(marginR, marginT, Core.fm, "", DrawableString.ALIGN_RIGHT);
		
		marginT += Core.SDP_H * 1.2f;
		
		Paint p2 = new Paint(paint);
		p2.setColor(Color.LTGRAY);
		
		lblDesc = new Label(marginL, marginT, p2, "Desc");
		lblDesc.setMaxWidth(w - Core.SDP);

		drawables.add(btnClose);
		drawables.add(lblHealth);
		drawables.add(lblMs);
		drawables.add(lblGold);
		drawables.add(lblCount);
		drawables.add(lblDesc);
		
		vals.add(str);
		vals.add(valHealth);
		vals.add(valMs);
		vals.add(valGold);
		vals.add(valCount);
		
		super.refresh();
	}
	
	@Override
	public void draw(float offX, float offY) {
		super.draw(0, 0);
		
		for(Drawable d : drawables) {
			d.draw(x, y);
		}
		
		for(Drawable d : vals) {
			d.draw(x, y);
		}
	}
	
	@Override
	public boolean update(float dt) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean onTouchEvent(int action, float x_, float y_) {
		final float x = Core.originalTouchX - this.x;
		final float y = Core.originalTouchY - this.y;
		
		return btnClose.onTouchEvent(action, x, y) || rect.contains(x, y);		
	}
	
	@Override
	public void refresh() {
		super.refresh();
		
		for(Drawable d : drawables) {
			d.refresh();
		}
		
		for(Drawable d : vals) {
			d.refresh();
		}
	}
	
	public void lightSetup(SpawnEvent event) {
		Sprite.SpriteStats stats = Sprite.getSpriteStats(event.enemyType);
		vals.get(0).setText(stats.name);
		vals.get(1).setText(String.valueOf(event.hp));
		vals.get(2).setText(df.format(stats.speed));
		vals.get(3).setText(String.valueOf(event.gold));
		vals.get(4).setText(String.valueOf(event.numUnits));
		
		lblDesc.setText(stats.desc);
	}
}