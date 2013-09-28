package com.ggstudios.divisionbyzero;

import java.text.DecimalFormat;

import android.graphics.Paint;

public class InfoDialog extends Drawable {
	private static final String TAG = "InfoWindow";
	
	private static final float DEFAULT_WIDTH = 4.5f;
	private static final float DEFAULT_HEIGHT = 2.5f;
	
	private static final DecimalFormat df = new DecimalFormat("#.##");
	
	private float width, height;
	
	private PictureBox bg;
	
	private Label lblAttack;
	private Label lblDmgDealt;
	private Label lblAttackSpeed;
	private DrawableString name;
	private DrawableString attack;
	private DrawableString dmgDealt;
	private DrawableString attackSpeed;
	
	private float x, y;
	
	private boolean visible = true;
	
	public InfoDialog() {}
	
	public void build() {
		width = DEFAULT_WIDTH * Core.SDP;
		height = DEFAULT_HEIGHT * Core.SDP;
		
		bg = new PictureBox(0, 0, width, height, R.drawable.window_bg);
		
		final float margin = Core.SDP / 4f;
		final float maxH = Core.fm.getHeight();
		float y = margin;
		float x = margin + Core.SDP * 4f;
		
		Paint textPaint = new Paint();
		textPaint.setColor(0xFFFFFFFF);
		textPaint.setTextSize(Core.fm.getFontSize());
		textPaint.setAntiAlias(true);
		
		name = new DrawableString(margin, y, Core.fm, "");
		y += maxH;
		lblAttack = new Label(margin, y, textPaint, "Attack");
		attack = new DrawableString(x, y, Core.fm, "", DrawableString.ALIGN_RIGHT);
		y += maxH;
		lblDmgDealt = new Label(margin, y, textPaint, "Dmg Dealt");
		dmgDealt = new DrawableString(x, y, Core.fm, "", DrawableString.ALIGN_RIGHT);
		y += maxH;
		lblAttackSpeed = new Label(margin, y, textPaint, "Attack Speed");
		attackSpeed = new DrawableString(x, y, Core.fm, "", DrawableString.ALIGN_RIGHT);
	}
	
	public void setInfo(Tower t) {
		name.setText(t.getName());
		attack.setText(String.valueOf(t.getDamage()));
		dmgDealt.setText(String.valueOf(t.getTotalDmgDealt()));
		attackSpeed.setText(df.format(1/t.getAttackSpeed()));
	}
	
	@Override
	public void draw(float offX, float offY) {
		if(!visible) return; 
		
		bg.draw(x, y);
		
		lblAttack.draw(x, y);
		lblDmgDealt.draw(x, y);
		lblAttackSpeed.draw(x, y);
		
		name.draw(x, y);
		attack.draw(x, y);
		dmgDealt.draw(x, y);
		attackSpeed.draw(x, y);
	}

	@Override
	public void refresh() {
		bg.refresh();

		lblAttack.refresh();
		lblDmgDealt.refresh();
		lblAttackSpeed.refresh();
		
		name.refresh();
		attack.refresh();
		dmgDealt.refresh();
		attackSpeed.refresh();
	}
	
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
}
