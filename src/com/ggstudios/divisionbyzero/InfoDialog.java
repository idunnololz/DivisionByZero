package com.ggstudios.divisionbyzero;

import java.util.ArrayList;
import java.util.List;

import com.ggstudios.divisionbyzero.Tower.OnTowerChangeListener;

import android.graphics.Paint;

/**
 * This is not a true dialog. This "dialog" displays information regarding a tower
 * such as it's range, attack speed and damage dealt so far.
 * @author iDunnololz
 *
 */
public class InfoDialog extends Drawable {
	private static final String TAG = "InfoDialog";
	
	private static final float DEFAULT_WIDTH = 6.5f;
	private static final float DEFAULT_HEIGHT = 2.5f;
	
	private float width, height;
	
	private PictureBox bg;
	
	private List<Drawable> drawables = new ArrayList<Drawable>();
	
	private Label lblDmg;
	private Label lblDmgDealt;
	private Label lblAttackSpeed;
	private Label lblRange;
	private DrawableString name;
	private StatBar statDmg, statRange, statAs;
	private DrawableString dmgDealt;
	
	private float x, y;
	
	private boolean visible = true;
	
	private Tower tower;
	
	public InfoDialog() {}
	
	public void build() {
		width = DEFAULT_WIDTH * Core.SDP;
		height = DEFAULT_HEIGHT * Core.SDP;
		
		bg = new PictureBox(0, 0, width, height, R.drawable.window_bg);
		
		final float margin = Core.SDP / 4f;
		final float maxH = Core.fm.getHeight();
		float y = margin;
		float x = margin + Core.SDP * 3f;
		float statW = Core.SDP * 3f;
		float statH = Core.SDP * 0.25f;
		float statMargin = Core.SDP * 0.1f;
		
		Paint textPaint = new Paint();
		textPaint.setColor(0xFFFFFFFF);
		textPaint.setTextSize(Core.fm.getFontSize() * 0.9f);
		textPaint.setAntiAlias(true);
		
		name = new DrawableString(margin, y, Core.fm, "");
		y += maxH;
		lblDmg = new Label(margin, y, textPaint, "Damage");
		statDmg = new StatBar(x, y + statMargin, statW, statH);
		statDmg.setMaxValue(TowerLibrary.MAX_DMG);
		y += lblDmg.h;
		lblRange = new Label(margin, y, textPaint, "Range");
		statRange = new StatBar(x, y + statMargin, statW, statH);
		statRange.setMaxValue(TowerLibrary.MAX_RANGE);
		y += lblRange.h;
		lblAttackSpeed = new Label(margin, y, textPaint, "Attack Speed");
		statAs = new StatBar(x, y + statMargin, statW, statH);
		statAs.setMaxValue(TowerLibrary.MAX_ATK_SPEED);
		y += lblAttackSpeed.h;
		lblDmgDealt = new Label(margin, y, textPaint, "Dmg Dealt");
		dmgDealt = new DrawableString(x + statW, y, Core.fm, "", DrawableString.ALIGN_RIGHT);
		dmgDealt.setTextSize(textPaint.getTextSize());
		
		drawables.add(bg);
		drawables.add(name);
		drawables.add(lblDmg);
		drawables.add(lblRange);
		drawables.add(lblAttackSpeed);
		drawables.add(lblDmgDealt);
		drawables.add(statDmg);
		drawables.add(statRange);
		drawables.add(statAs);
		drawables.add(dmgDealt);
	}
	
	public void setInfo(Tower t) {
		if(tower != null) {
			// we need to unregister our listener from the
			// other tower...
			tower.setTowerChangeListener(null);
		}
		
		this.tower = t;
		
		tower.setTowerChangeListener(new OnTowerChangeListener() {

			@Override
			public void onDamageDealtChange(int totalDmgDealt) {
				dmgDealt.setText(String.valueOf(totalDmgDealt));
			}

			@Override
			public void onStatChange() {
				setInfo(tower);
			}
			
		});
		
		name.setText(t.getName());
		dmgDealt.setText(String.valueOf(t.getTotalDmgDealt()));
		
		int level = tower.level + 1;
		if(tower.level + 1 != tower.evoTree.maxLevel) {
			statDmg.setSecondaryValue(tower.evoTree.dmg[level]);
			statRange.setSecondaryValue(tower.evoTree.range[level]);
			statAs.setSecondaryValue(1/tower.evoTree.as[level]);
		}
		
		final float b1 = statDmg.getValue();
		final float b2 = statRange.getValue();
		final float b3 = statAs.getValue();
		
		final float c1 = t.getDamage() - statDmg.getValue();
		final float c2 = t.getGridRange() - statRange.getValue();
		final float c3 = 1/t.getAttackSpeed() - statAs.getValue();
		
		Core.gu.addUiUpdatable(new Updatable() {
			final float DURATION = 0.5f;
			float time = 0f;

			@Override
			public boolean update(float dt) {
				time += dt;
				if(time < DURATION) {
					float t = time / DURATION * 2;
					if (t < 1) {
						final float temp = t*t/2;
						statDmg.setValue(c1*temp + b1);
						statRange.setValue(c2*temp + b2);
						statAs.setValue(c3*temp + b3);
					} else {
						t--;
						final float temp = -(t*(t-2)-1)/2;
						statDmg.setValue(c1*temp + b1);
						statRange.setValue(c2*temp + b2);
						statAs.setValue(c3*temp + b3);
					}
					return true;
				} else {
					statDmg.setValue(b1+c1);
					statRange.setValue(b2+c2);
					statAs.setValue(b3+c3);
					return false;
				}
			}
		});
	}
	
	@Override
	public void draw(float offX, float offY) {
		if(!visible) return; 
		
		for(Drawable d : drawables)
			d.draw(x, y);
	}

	@Override
	public void refresh() {
		bg.refresh();

		for(Drawable d : drawables)
			d.refresh();
	}
	
	
	public void setVisible(boolean visible) {
		this.visible = visible;
		if(!visible) {
			if(tower != null) {
				tower.setTowerChangeListener(null);
				tower = null;
			}
			statDmg.setValue(0);
			statRange.setValue(0);
			statAs.setValue(0);
		}
	}
}