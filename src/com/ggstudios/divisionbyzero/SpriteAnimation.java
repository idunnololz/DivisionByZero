package com.ggstudios.divisionbyzero;

public class SpriteAnimation implements Updatable {
	private static final int MAX_FRAMES = 20;

	private float time = 0f;
	private int[] textureIds = new int[MAX_FRAMES];
	private int[] textureHandles = new int[MAX_FRAMES];
	private float[] holdTimes = new float[MAX_FRAMES];
	private int frames = 0;

	private int currentFrame;

	private boolean loop = true;

	public static interface OnAnimationComplete {
		public void onAnimationComplete();
	}

	private OnAnimationComplete animationCompleteListener;

	public void setLoop(boolean loop) {
		this.loop = loop;
	}

	public void addFrame(int texId, float holdTime) {
		textureIds[frames] = texId;
		textureHandles[frames] = Core.tm.get(texId);

		if(frames != 0)
			holdTimes[frames] = holdTimes[frames - 1] + holdTime;

		frames++;
	}

	public int getTextureHandle() {
		return textureHandles[currentFrame];
	}

	@Override
	public boolean update(float dt) {
		time += dt;

		if(time > holdTimes[currentFrame]) {
			if(currentFrame + 1 >= frames) {
				if(loop) {
					currentFrame = 0;
					time = 0f;
					//time -= holdTimes[frames - 1];
				} else {
					if(animationCompleteListener != null)
						animationCompleteListener.onAnimationComplete();
					return false;
				}
			} else {
				currentFrame++;
			}
		}
		return true;
	}

	public void refresh() {
		for(int i = 0; i < frames; i++) {
			textureHandles[i] = Core.tm.get(textureIds[i]);
		}
	}

	public void reset() {
		frames = 0;
	}

	public void setOnAnimationCompleteListener(OnAnimationComplete animationCompleteListener) {
		this.animationCompleteListener = animationCompleteListener;
	}

}
