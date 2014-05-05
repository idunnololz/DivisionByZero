package com.ggstudios.divisionbyzero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This collection has a very important purpose.
 * Once all drawables are added to it, it attempts
 * to not invoke any form of gc by removing elements
 * in the following way:
 * 
 * To remove an element, it swaps it with the last 
 * non-deleted element and the decrements the size of
 * the list. This way the element isn't removed...
 * it is simply excluded from draw/refresh loops.
 * 
 * When new elements are added, they will overwrite
 * previously deleted elements.
 * 
 * @author iDunnololz
 *
 */
public class DrawableCollection<T extends Drawable> extends Drawable{
	protected List<T> drawables = new ArrayList<T>();
	protected int len = 0;

	public void addDrawable(T d) {
		if(drawables.size() == len) {
			drawables.add(d);
		} else {
			drawables.set(len, d);
		}

		len++;
	}

	public void addDrawableToTop(T d) {
		drawables.add(0, d);

		len++;
	}

	public void removeDrawable(int index) {
		Collections.swap(drawables, index, len - 1);

		onObjectRemoved();
	}

	public void removeDrawable(T enemySprite) {
		final int index = drawables.indexOf(enemySprite);
		if(index != -1) {
			removeDrawable(index);
		}
	}

	/**
	 * Removes the specified drawable while retaining the order
	 * of the list.
	 * @param enemySprite
	 */
	public synchronized void removeDrawableStrict(T enemySprite) {
		onObjectRemoved();
		drawables.remove(enemySprite);
		//toRemove.add(enemySprite);
	}
	
	protected void onObjectRemoved() {
		len--;
	}

	public void clear() {
		len = 0;
		drawables.clear();
	}

	@Override
	public synchronized void draw(float offX, float offY) {
		for(int i = len - 1; i >= 0; i--) {
			drawables.get(i).draw(offX, offY);
		}
	}

	@Override
	public void refresh() {
		for(int i = len - 1; i >= 0; i--) {
			drawables.get(i).refresh();
		}
	}

	public List<T> getRawList() {
		return drawables;
	}

	public int size() {
		return len;
	}

	public T get(int index) {
		return drawables.get(index);
	}

	public int indexOf(T t) {
		return drawables.indexOf(t);
	}
}
