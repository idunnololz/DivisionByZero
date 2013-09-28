package com.ggstudios.divisionbyzero;

public class ExtrasManager extends Drawable {

	private static final int DEFAULT_SIZE = 50;
	
	private PictureBox[] pool = new PictureBox[DEFAULT_SIZE];
	private int len = 0;
	private boolean loaded = false;
	
	public ExtrasManager() {}
	
	public void loadGlData() {
		for(int i = 0; i < pool.length; i++){
			pool[i] = new PictureBox(0, 0, Core.GeneralBuffers.tile, -1);
		}
		
		loaded = true;
	}
	
	public PictureBox obtain(float w, float h) {
		final PictureBox item = pool[len];
		item.setSize(w, h);
		item.isVisible = false;
		len++;
		return item;
	}
	
	@Override
	public void draw(float offX, float offY) {
		for(int i = 0; i < len; i++)
			pool[i].draw(offX, offY);
	}

	@Override
	public void refresh() {
		if(!loaded) return;
		for(int i = 0; i < pool.length; i++)
			pool[i].refresh();
	}

	public void removeDrawable(PictureBox pb) {
		int index = -1;
		for(int i = 0; i < len; i++)
			if(pool[i] == pb) {
				index = i;
			}
		
		if(index == -1) return;
		else {
			for(int i = index; i < len - 1; i++) {
				pool[i] = pool[i + 1];
			}
			pool[len - 1] = pb;
			len--;
		}
	}

}
