package com.ggstudios.divisionbyzero;

public class DialogManager extends DrawableCollection<BaseDialog> implements Clickable{

	@Override
	public boolean onTouchEvent(int action, float x, float y) {
		for(int i = 0; i < len; i++) {
			if(drawables.get(i).onTouchEvent(action, x, y))
				return true;
		}
		return false;
	}

}
