package com.ggstudios.divisionbyzero;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SpriteManager extends DrawableCollection<Sprite> {
	private static final int DEFAULT_CAPACITY = 100;
	private int capacity = DEFAULT_CAPACITY;
	
	public void initialize() {
		drawables.clear();
		for(int i = 0; i < capacity; i++) {
			Sprite s = new Sprite();
			drawables.add(s);
		}
	}
	
	public void setCapacity(int capacity){
		if(capacity <= this.capacity) return;
		
		int diff = capacity - this.capacity;
		for(int i = 0; i < diff; i++) {
			Sprite s = new Sprite();
			drawables.add(s);			
		}
	}
	
	public Sprite obtain() {
		if(len == drawables.size()) {
			// if we have exhausted our list of sprites
			// allocate one on the fly and add it
			drawables.add(new Sprite());
		}
		
		Sprite s = drawables.get(len++);
		s.isVisible = false;
		
		return s;
	}

	public void save(DataOutputStream stream) throws IOException {
		stream.writeInt(capacity);
		stream.writeInt(len);
		for(int i = 0; i < len; i++) {
			Sprite s = drawables.get(i);
			s.writeToStream(stream);
		}
	}

	public void load(DataInputStream stream) throws IOException {
		setCapacity(stream.readInt());
		int len = stream.readInt();
		for(int i = 0; i < len; i++) {
			Sprite s = obtain();
			s.loadFromStream(stream);
			Core.game.addEnemy(s);
		}
	}
}
