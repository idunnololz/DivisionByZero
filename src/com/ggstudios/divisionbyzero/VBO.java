package com.ggstudios.divisionbyzero;

public class VBO {
	public int handle = -1;
	public float width;
	public float height;
	
	enum Alignment{
		TOP_LEFT,
		CENTER
	}
	
	Alignment alignment;
	
	public VBO() {}
	
	public void setVBO(float w, float h, int handle) {
		setVBO(w, h, handle, Alignment.TOP_LEFT);
	}
	
	public void setVBO(float w, float h, int handle, Alignment alignment) {
		width = w;
		height = h;
		
		this.handle = handle;
		this.alignment = alignment;
	}
}
