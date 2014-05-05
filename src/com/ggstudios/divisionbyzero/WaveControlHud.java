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
import com.ggstudios.divisionbyzero.LevelManager.Group;
import com.ggstudios.divisionbyzero.LevelManager.SpawnEvent;
import com.ggstudios.utils.BitmapUtils;
import com.ggstudios.utils.BufferUtils;
import com.ggstudios.utils.DebugLog;

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
	
	public static final int 
	SPEED_NORMAL = 0, 
	SPEED_FAST = 1, 
	SPEED_VERY_FAST = 2;

	// game speeds (x1.0, x2.0, x3.0)
	private float[] speeds = {1.0f, 2.0f, 3.0f};
	private int currentSpeedIndex = -1;

	private WaveRectangle[] rects;
	private float waveStartX, waveEndX;

	private int currentWaveGroup = 0;
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

		for(int i = 0; i < len; i++) {
			Event e = events.get(i);

			if(e.cmd == Command.SPAWN_UNIT) {
				SpawnEvent se = (SpawnEvent) e.args;
				
				Wave w = waves[curWave] = new Wave();
				w.color = Sprite.getSpriteStats(se.enemyType).color;
				
				w.event = se;

				curWave++;
			}
		}
	}

	public void buildWave() {
		if(rects.length == 0) return;
		
		float x = height * 4f;
		final float width = EVENT_WIDTH * Core.SDP;
		final float height = this.height * EVENT_HEIGHT;
		final float margin = Core.SDP * 0.1f;

		final float y = (this.height - height) / 2f;

		int lastGroupNumber = -1;
		
		float right = 0;
		
		for(int i = 0; i < rects.length; i++) {
			final Wave w = waves[i];
			
			Group group = w.event.group;
			
			if(lastGroupNumber != group.groupNumber) {
				x += margin;
				right = x + width;
			}
			
			rects[i] = new WaveRectangle(x, (int)y, right - x, (int)height, w);

			x += (width / group.memberCount);
			
			lastGroupNumber = group.groupNumber;
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

				onSpeedChanged();
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
	
	public void setSpeed(int speed) {
		Core.gu.setTimeMultiplier(speeds[speed]);
		currentSpeedIndex = speed;
		onSpeedChanged();
	}
	
	private void onSpeedChanged() {
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
		
		onSpeedChanged();

		if(rects != null) {
			for(int i = 0; i < rects.length; i++) {
				rects[i].refresh();
			}
		}
	}
	
	@Override
	public boolean onTouchEvent(int action, float x, float y) {
		final float finalX = x - this.x;
		final float finalY = y - this.y;
		
		boolean handled = btnSpeedControl.onTouchEvent(action, finalX, finalY) || 
				btnPause.onTouchEvent(action, finalX, finalY) ||
				btnNext.onTouchEvent(action, finalX, finalY);
				
		if(handled) return true;
		
		if(action == MotionEvent.ACTION_DOWN && x >= this.x + waveStartX && x <= this.x + waveStartX + waveEndX) {
			final int offX = (int) (x - this.x - waveOffset);
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
			int curWaveIndex = Core.lm.getCurrentWave() - 1;
			if(curWaveIndex < 0) return true;
			
			Group curGroup = waves[curWaveIndex].event.group;
			int newWaveGroup = curGroup.groupNumber;
			if(newWaveGroup != currentWaveGroup) {
				animateSkip();
			}
			currentWaveGroup = newWaveGroup;
			float newOff = 0;
			int groupLeaderIndex = curGroup.groupLeader.waveNumber;

			if(currentWaveGroup >= 0) {
				WaveRectangle curRect = rects[groupLeaderIndex];
				Wave w = waves[groupLeaderIndex];
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

		public void setSize(float newW, float newH){
			w = newW;
			h = newH;

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
