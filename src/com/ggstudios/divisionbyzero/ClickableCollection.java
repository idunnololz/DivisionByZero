package com.ggstudios.divisionbyzero;

import java.util.ArrayList;
import java.util.Collections;

public class ClickableCollection implements Clickable{

	private ArrayList<Clickable> clickables = new ArrayList<Clickable>();
	private int len = 0;
	
	public void addClickable(Clickable d) {
		if(clickables.size() == len) {
			clickables.add(d);
		} else {
			clickables.set(len, d);
		}
		
		len++;
	}
	
	public void removeClickable(Clickable clickable) {
		final int index = clickables.indexOf(clickable);
		if(index != -1) {
			Collections.swap(clickables, index, len - 1);

			len--;
		}
	}
	
	public void removeClickable(int index) {
		Collections.swap(clickables, index, len - 1);
		
		len--;
	}
	
	public void clear() {
		clickables.clear();
		len = 0;
	}

	@Override
	public boolean onTouchEvent(int action, int x, int y) {
		for(int i = len - 1; i >= 0; i--) {
			if(clickables.get(i).onTouchEvent(action, x, y))
				return true;
		}
		return false;
	}

}