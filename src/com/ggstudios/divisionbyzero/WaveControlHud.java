package com.ggstudios.divisionbyzero;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.view.MotionEvent;

import com.ggstudios.divisionbyzero.Button.OnClickListener;
import com.ggstudios.divisionbyzero.LevelManager.Command;
import com.ggstudios.divisionbyzero.LevelManager.Event;
import com.ggstudios.divisionbyzero.LevelManager.SpawnEvent;
import com.ggstudios.utils.BitmapUtils;
import com.ggstudios.utils.BufferUtils;

public class WaveControlHud extends Drawable implements Clickable, Updatable {
	private static final String TAG = "WaveControlHud";

	private static final float WIDTH = 7.0f;
	private static final float HEIGHT = 0.70f;

	private static final float EVENT_WIDTH = 0.7f;
	private static final float EVENT_HEIGHT = 0.5f;

	float x, y;
	float width, height;

	private PictureBox bg;
	private Button btnSpeedControl;
	private Button btnPause;
	private Button btnNext;

	// game speeds (x1.0, x2.0, x3.0)
	private float[] speeds = {1.0f, 2.0f, 3.0f};
	private int currentSpeedIndex = -1;

	private WaveRectangle[] rects;
	private float waveStartX, waveEndX;

	private int currentWave = 0;
	private float waveOffset = 0;

	private static final float SKIP_DURATION = 0.5f;
	private boolean animateSkip;

	private static class Wave {
		// color to represent the wave
		int color;

		SpawnEvent event;
	}

	private Wave[] waves;

	public float getNextSpeed() {
		if(currentSpeedIndex == -1) {
			// start the game!
			currentSpeedIndex = 0;
			Core.game.beginFirstWave();
			return speeds[0];
		} else if(currentSpeedIndex == speeds.length - 1) {
			currentSpeedIndex = 0;
		} else {
			currentSpeedIndex++;
		}

		return speeds[currentSpeedIndex];
	}

	public void generateWaveInfomation() {
		final LevelManager lm = Core.lm;

		final List<Event> events = lm.getRawEvents();
		final int len = events.size();
		final int waveCount = lm.getWaveCount();
		waves = new Wave[waveCount];
		rects = new WaveRectangle[waveCount];

		int curWave = 0;

		float hue = 5;
		float delta = 60;
		float[] hsv = new float[3];
		hsv[1] = 0.5f;
		hsv[2] = 1f;

		for(int i = 0; i < len; i++) {
			Event e = events.get(i);

			if(e.cmd == Command.SPAWN_UNIT) {
				SpawnEvent se = (SpawnEvent) e.args;
				
				Wave w = waves[curWave] = new Wave();
				hsv[0] = hue;
				int color = Color.HSVToColor(hsv);
				w.color = color;
						
				switch(se.enemyType) {		
				case Sprite.TYPE_REGULAR:
					w.color = 0xFFB8B8B8;
					break;
				case Sprite.TYPE_SPEEDLING:
					w.color = 0xFF3B7DF7;
					break;
				case Sprite.TYPE_HEAVY:
					w.color = 0xFFEB6CE6;
					break;
				case Sprite.TYPE_GHOST:
					w.color = 0xFF51F071;
					break;
				case Sprite.TYPE_SPLITTER:
					w.color = 0xFFFAFA43;
					break;
				}
				
				w.event = se;

				hue += delta;
				if(hue >= 360)
					hue -= 360;

				curWave++;
			}
		}
	}

	public void buildWave() {
		float x = height * 4f;
		final float width = EVENT_WIDTH * Core.SDP;
		final float height = this.height * EVENT_HEIGHT;
		final float margin = Core.SDP * 0.1f;

		final float y = (this.height - height) / 2f;

		for(int i = 0; i < rects.length; i++) {
			final Wave w = waves[i];
			
			rects[i] = new WaveRectangle(x, (int)y, width, (int)height, w);
			x += width + margin;
		}
	}

	public WaveControlHud() {}

	public void setPos(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void build() {
		width = WIDTH * Core.SDP;
		height = HEIGHT * Core.SDP;

		bg = new PictureBox(0, 0, 
				width, height, R.drawable.wave_control_panel);

		final float buttonW = height * 2.0f;
		float nextX = height * 2.0f;

		btnSpeedControl = new Button(0, 0, 
				buttonW, height, R.drawable.wave_control_play);
		btnPause = new Button(nextX, 0,
				buttonW, height, R.drawable.wave_control_pause);
		nextX += buttonW;
		btnNext = new Button(width - buttonW, 0, buttonW, height, R.drawable.wave_control_next);

		waveStartX = nextX;
		waveEndX = btnNext.x - btnPause.x - buttonW;

		// add a bit of padding to the bottom of the button,
		// to make it easier to click
		btnSpeedControl.setPaddingBottom((int) Core.SDP_H);
		btnPause.setPaddingBottom((int)Core.SDP_H);
		btnNext.setPaddingBottom((int)Core.SDP_H);

		btnSpeedControl.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				Core.gu.setTimeMultiplier(getNextSpeed());

				switch(currentSpeedIndex) {
				case 0:
					btnSpeedControl.setTexture(R.drawable.wave_control_ff);
					break;
				case 1:
					btnSpeedControl.setTexture(R.drawable.wave_control_fff);
					break;
				case 2:
					btnSpeedControl.setTexture(R.drawable.wave_control_play);
					break;
				}
			}

		});

		btnPause.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				Core.game.onPauseClick();
			}

		});

		btnNext.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				Core.game.skipToNext();
				animateSkip();
			}
		});
	}
	
	private void animateSkip() {
		time = 0;
		initVal = waveOffset;
		animateSkip = true;
	}

	@Override
	public void draw(float offX, float offY) {
		bg.draw(x, y);
		btnSpeedControl.draw(x, y);
		btnPause.draw(x, y);
		btnNext.draw(x, y);

		if(rects != null) {
			// enable clipping...
			GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
			GLES20.glScissor((int)(x + waveStartX), 0, (int) (waveEndX), (int)Core.canvasHeight);

			final float waveX = x + waveOffset;

			for(int i = 0; i < rects.length; i++) {
				rects[i].draw(waveX, y);
			}

			GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
		}
	}

	@Override
	public void refresh() {
		bg.refresh();
		btnSpeedControl.refresh();
		btnPause.refresh();
		btnNext.refresh();

		if(rects != null) {
			for(int i = 0; i < rects.length; i++) {
				rects[i].refresh();
			}
		}
	}
	
	@Override
	public boolean onTouchEvent(int action, int x, int y) {
		final int finalX = (int) (x - this.x);
		final int finalY = (int) (y - this.y);
		
		boolean handled = btnSpeedControl.onTouchEvent(action, finalX, finalY) || 
				btnPause.onTouchEvent(action, finalX, finalY) ||
				btnNext.onTouchEvent(action, finalX, finalY);
				
		if(handled) return true;
		
		if(action == MotionEvent.ACTION_DOWN) {
			final int offX = (int) (x - this.x + waveOffset);
			final int offY = (int) (y - this.y);

			for(int i = 0; i < rects.length; i++) {
				if(rects[i].rect.contains(offX, offY)) {
					Core.game.showWaveInfo(rects[i].getWaveInfo().event);

					return true;
				}
			}
		}
		
		return false;
	}

	private float time;
	private float initVal;

	@Override
	public boolean update(float dt) {
		if(rects != null) {
			// calc additional offset
			int newWave = Core.lm.getCurrentWave() - 1;
			if(newWave != currentWave) {
				animateSkip();
			}
			currentWave = newWave;
			float newOff = 0;

			if(currentWave >= 0) {
				WaveRectangle curRect = rects[currentWave];
				Wave w = waves[currentWave];
				SpawnEvent se = w.event;

				final float done = se.elapsed / se.approxTime;
				// the 1.1f is to add a bit of padding to ensure that 
				// the entire block will pass through...
				newOff = (int)(curRect.x - rects[0].x + done * (curRect.w * 1.1f)) * -1;
			}

			if(animateSkip) {
				time += dt;

				if(time < SKIP_DURATION) {
					float t = time/SKIP_DURATION;
					waveOffset = -(newOff - initVal) * t*(t-2) + initVal;
				} else {
					animateSkip = false;
				}

			} else {
				waveOffset = newOff;
			}
		}
		return true;
	}
	
	static class WaveRectangle extends PictureBox {
		private int color;
		private Paint paint;

		private boolean dirty = false;
		
		public Rect rect = new Rect();
		
		private Wave info;

		/**
		 * Creates a drawable square object
		 * 
		 * @param x	X
		 * @param y Y
		 * @param w Width
		 * @param h Height
		 * @param a Alpha
		 * @param r Red
		 * @param g Green
		 * @param b Blue
		 */

		public WaveRectangle(float x, float y, float w, float h, Wave info){
			super(0, 0);
			this.x = x;
			this.y = y;

			this.w = w;
			this.h = h;
			
			this.info = info;
			
			rect.left = (int)x;
			rect.top = (int)y;
			rect.right = (int) (rect.left + w);
			rect.bottom = (int) (rect.top + w + Core.MAP_SDP);
			
			color = info.color;
			paint = new Paint();

			refresh();
		}
		
		private void generateTexture() {
			Bitmap bitmap = Bitmap.createBitmap((int)w, (int)h, Bitmap.Config.ARGB_8888);
			
			int borderW = (int)(Core.SDP * 0.07f);
			
			paint.setAntiAlias(true);
			
			Canvas canvas = new Canvas(bitmap);
			paint.setColor(color);
			canvas.drawRect(0, 0, w, h, paint);
			paint.setColor(Color.DKGRAY);
			canvas.drawRect((int)borderW, (int)borderW, (int)(w - borderW), (int)(h - borderW), paint);
			
			textureHandle = BitmapUtils.loadBitmap(bitmap, textureHandle);
		}

		public void setSize(float new_w, float new_h){
			w = new_w;
			h = new_h;

			dirty = true;
		}

		private void rebuild() {
			generateTexture();
			handle = BufferUtils.createRectangleBuffer(w, h);
		}

		@Override
		public void draw(float offX, float offY) {
			if(dirty) {
				rebuild();
				dirty = false;
			}
			
			super.draw(offX, offY);
		}

		@Override
		public void refresh() {
			rebuild();
		}
		
		public Wave getWaveInfo() {
			return info;
		}
	}
	
	public void reset() {
		currentSpeedIndex = -1;
	}
}
