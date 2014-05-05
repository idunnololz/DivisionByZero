package com.ggstudios.divisionbyzero;

import java.util.ArrayList;
import java.util.List;

import com.ggstudios.divisionbyzero.Button.OnClickListener;
import com.ggstudios.divisionbyzero.TowerLibrary.TowerEvoTree;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class TowerInfoDialog extends BaseDialog {
	private static final float DIALOG_WIDTH = 7f;
	private static final float MARGIN = 0.3f;
	
	private List<Drawable> drawables = new ArrayList<Drawable>();
	
	private DrawableString title;
	private Label lblDamage, lblRange, lblAttackSpeed, lblDesc;
	private StatBar statDamage, statRange, statAttackSpeed;
	private Button btnOk;
	
	private float margin;
	
	private RectF rect = new RectF();
	
	public TowerInfoDialog() {}
	
	public void build() {
		w = DIALOG_WIDTH * Core.SDP;
		
		margin = MARGIN * Core.SDP;
		
		float y = margin;
		float x = margin;
		float r = w - margin;
		
		float statW = Core.SDP * 3f;
		float statH = Core.SDP * 0.25f;
		
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		paint.setAntiAlias(true);
		paint.setTextSize(Core.fm.getFontSize() * 0.9f);
		
		float statBarMargin = Core.SDP * 0.1f;
		
		title = new DrawableString(x, y, Core.fm, "<title>");
		y += title.height + margin;
		lblDamage = new Label(x, y, paint, "Damage");
		statDamage = new StatBar(r - statW, y + statBarMargin, statW, statH);
		y += lblDamage.h;// + margin;
		lblRange = new Label(x, y, paint, "Range");
		statRange = new StatBar(r - statW, y + statBarMargin, statW, statH);
		y += lblRange.h;// + margin;
		lblAttackSpeed = new Label(x, y, paint, "Attack speed");
		statAttackSpeed = new StatBar(r - statW, y + statBarMargin, statW, statH);
		y += lblAttackSpeed.h + margin;
		lblDesc = new Label(x, y, paint, "", w - margin * 2);
		btnOk = new Button(x, y, w - margin * 2, Core.SDP, R.drawable.custom_button_bg, "Ok", paint);
		
		btnOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				hide();
			}
			
		});
		
		h = y + Core.SDP * 4f;
		
		statDamage.setMaxValue(TowerLibrary.MAX_DMG);
		statRange.setMaxValue(TowerLibrary.MAX_RANGE);
		statAttackSpeed.setMaxValue(TowerLibrary.MAX_ATK_SPEED);
		
		drawables.add(title);
		drawables.add(lblDamage);
		drawables.add(lblRange);
		drawables.add(lblAttackSpeed);
		drawables.add(statDamage);
		drawables.add(statRange);
		drawables.add(statAttackSpeed);
		drawables.add(lblDesc);
		drawables.add(btnOk);
		
		setBackgroundTexture(R.drawable.panel);
	}
	
	private void refreshRect() {
		rect.left = 0;
		rect.top = 0;
		rect.right = w;
		rect.bottom = h;
	}
	
	public void lightSetup(TowerEvoTree evoTree, int level) {
		title.setText(evoTree.name[level]);
		statDamage.setValue(evoTree.dmg[level]);
		statRange.setValue(evoTree.range[level]);
		statAttackSpeed.setValue(1f/evoTree.as[level]);
		lblDesc.setText(evoTree.getDescription());
		
		btnOk.y = lblDesc.y + lblDesc.h + margin;
		
		h = btnOk.y + btnOk.h + margin;
		
		x = (Core.canvasWidth - w) / 2f;
		y = (Core.canvasHeight - h) / 2f;
		
		refreshRect();
		notifyChanged();
	}
	
	@Override
	public boolean onTouchEvent(int action, float x_, float y_) {
		final float x = Core.originalTouchX - this.x;
		final float y = Core.originalTouchY - this.y;
		
		@SuppressWarnings("unused")
		boolean unused = btnOk.onTouchEvent(action, x, y) || rect.contains(x, y);
		return true;	// block all touch events...
	}

	@Override
	public void draw(float offX, float offY) {
		super.draw(0, 0);
		
		for(Drawable d : drawables) {
			d.draw(x, y);
		}
	}
	
	@Override
	public void refresh() {
		super.refresh();
		
		for(Drawable d : drawables) {
			d.refresh();
		}
	}
	
	@Override
	public void show() {
		Core.gu.pause();
		super.show();
	}
	
	@Override
	public void hide() {
		super.hide();
		Core.gu.unpause();
	}
}
