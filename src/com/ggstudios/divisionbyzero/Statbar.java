package com.ggstudios.divisionbyzero;

class StatBar extends Drawable {
	public float x, y, w, h;
	private float max, val, val2;
	private float percent;
	private float percent2;
	
	private Rectangle bg, fill, fill2;
	
	private boolean showValue = false;
	private DrawableString dsVal;
	
	public StatBar(float x, float y, float w, float h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		
		build();
	}
	
	private void build() {
		bg = new Rectangle(x, y, w, h, 0xFF505050);
		fill = new Rectangle(x, y, w, h, 0xFFFFFFFF);
		fill2 = new Rectangle(x, y, w, h, 0xFFB1B1B1);
	}
	
	public void showValue(FontManager fm, float fontSize) {
		dsVal = new DrawableString(x, y, fm, "0");
		dsVal.setTextSize(fontSize);
		showValue = true;
	}
	
	private void recalculatePercent() {
		percent = val / max;
		
		if(percent > 1f) percent = 1f;
		
		fill.setScaleWidth(percent);
	}
	
	private void recalculateSecondary() {
		percent2 = val2 / max;
		if(percent2 > 1f) percent2 = 1f;
		fill2.setScaleWidth(percent2);
	}
	
	public void setMaxValue(float max) {
		this.max = max;
		recalculatePercent();
	}
	
	public void setValue(float val) {
		this.val = val;
		recalculatePercent();
		
		if(showValue) {
			dsVal.setText(String.valueOf(val));
			dsVal.x = this.x + fill.w * percent;
		}
	}
	
	public void setSecondaryValue(float val) {
		this.val2 = val;
		recalculateSecondary();
	}
	
	@Override
	public void draw(float offX, float offY) {
		if(showValue) {
			dsVal.draw(offX, offY);
		} else {
			bg.draw(offX, offY);
		}
		
		if(percent2 > percent)
			fill2.draw(offX, offY);
		
		fill.draw(offX, offY);
	}

	@Override
	public void refresh() {
		bg.refresh();
		fill.refresh();
		fill2.refresh();
		
		if(showValue)
			dsVal.refresh();
	}

	public float getValue() {
		return val;
	}
}
