package com.ggstudios.divisionbyzero;

import java.util.ArrayList;
import java.util.Collections;

public class UpdatableCollection implements Updatable{
	private ArrayList<Updatable> items = new ArrayList<Updatable>();
	private int len = 0;

	public void add(Updatable d) {
		if(items.size() == len) {
			items.add(d);
		} else {
			items.set(len, d);
		}

		len++;
	}

	public void remove(int index) {
		Collections.swap(items, index, len - 1);

		len--;
	}

	public void remove(Updatable item) {
		int index = items.indexOf(item);

		if(index == -1 || index >= len)
			return;
		Collections.swap(items, index, len - 1);

		len--;
	}

	public void clear() {
		len = 0;
	}

	public int find(Updatable item) {
		for(int i = len - 1; i >= 0; i--)
			if (items.get(i).equals(item))
				return i;
		return -1;
	}

	@Override
	public boolean update(float dt) {
		for(int i = len - 1; i >= 0; i--) {
			if( !items.get(i).update(dt) ) {
				remove(i);
			}
		}

		return true;
	}

	public int size() {
		return len;
	}
}
