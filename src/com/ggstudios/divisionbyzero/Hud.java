package com.ggstudios.divisionbyzero;

import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.RectF;

import com.ggstudios.divisionbyzero.Button.OnClickListener;
import com.ggstudios.divisionbyzero.LevelManager.WaveChangeListener;
import com.ggstudios.divisionbyzero.Player.OnGoldChangedListener;
import com.ggstudios.divisionbyzero.Player.OnLivesChangedListener;
import com.ggstudios.utils.DebugLog;

public class Hud extends Drawable implements Clickable, Updatable{
	private static final String TAG  = "Hud";
	private static final float OFF_SCREEN = -999f;
	private static final float DEFAULT_MESSAGE_DURATION = 2f;
	private static final float TEXT_CHANGE_ANIMATION_DURATION = 0.4f;
	
	public static final int HUD_LEFT_PANEL = 1,
			HUD_RIGHT_PANEL = 2,
			HUD_TOWER_MENU = 3,
			HUD_WAVE_CONTROL = 4;
	
	private PictureBox leftPanel;
	private TowerMenu rightPanel;
	private WaveControlHud waveHud;
	private PictureBox factionIcon;

	private RectF leftPanelRect = new RectF();
	private RectF rightPanelRect = new RectF();

	private Button btnMore;
	private Label lblGold;
	private Label lblLives;
	private Label lblWave;

	private DrawableString txtGold;
	private DrawableString txtLives;
	private DrawableString txtWave;

	private ShowLeftPanel leftUpdatable;
	private ShowRightPanel rightUpdatable;
	
	private ZoomControl zoomControl;
	
	private boolean supportsMultitouch;
	
	private InGameMenu menu;
	
	private static final int MAX_MESSAGES = 6;
	private int activeMessages = 0;
	private Message[] messages = new Message[MAX_MESSAGES];
	
	private boolean animateGold = false, animateLives = false;
	private float goldAnimationTime = 0f, livesAnimationTime = 0f;
	
	private boolean built = false;
	
	public Hud() { 
		zoomControl = new ZoomControl();
		waveHud = new WaveControlHud();
		menu = new InGameMenu();
	}

	public void build() {
		if(built) {
			// rebuild will do less work and will not reset the state of some of the UI elements...
			rebuild();
			return;
		}
		built = true;
		
		menu = new InGameMenu();
		DebugLog.d(TAG, "build()");
		
		supportsMultitouch = Core.context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH);
		
		zoomControl.build();

		leftPanel = new PictureBox(OFF_SCREEN, OFF_SCREEN, 
				Core.SDP * 7.0f, Core.SDP * 2.5f, R.drawable.right_panel);

		rightPanel = new TowerMenu(OFF_SCREEN, OFF_SCREEN);

		waveHud.setPos(OFF_SCREEN, OFF_SCREEN);
		waveHud.build();
		waveHud.buildWave();

		factionIcon = new PictureBox(Core.SDP_H / 2, Core.SDP_H / 2,
				Core.SDP * 2.0f, Core.SDP * 2.0f, R.drawable.faction_bronze_icn);

		Paint p = new Paint();
		p.setColor(0xFFFFFFFF);
		p.setTextSize(Core.SDP_H * 0.9f);
		p.setAntiAlias(true);

		rightPanel.addTower(new Tower(0, 0, TowerLibrary.REGULAR, 0));
		rightPanel.addTower(new Tower(0, 0, TowerLibrary.SPECIALIST, 0));
		rightPanel.addTower(new Tower(0, 0, TowerLibrary.FLAKE, 0));
		rightPanel.addTower(new Tower(0, 0, TowerLibrary.BRUTAL, 0));
		rightPanel.build();

		btnMore = new Button(leftPanel.w - Core.SDP, leftPanel.h - Core.SDP, Core.SDP, Core.SDP, R.drawable.button_more);
		btnMore.setPadding((int)Core.SDP_H);
		lblGold = new Label(Core.SDP * 2.5f, Core.SDP * 0.25f, p, "Gold");
		lblLives = new Label(Core.SDP * 2.5f, Core.SDP * 0.75f, p, "Lives");
		lblWave = new Label(Core.SDP * 2.5f, Core.SDP * 1.25f, p, "Wave");
		txtGold = new DrawableString(Core.SDP * 6.5f, Core.SDP * 0.25f, Core.fm, "-",
				DrawableString.ALIGN_RIGHT);
		txtLives = new DrawableString(Core.SDP * 6.5f, Core.SDP * 0.75f, Core.fm, "-", 
				DrawableString.ALIGN_RIGHT);
		txtWave = new DrawableString(Core.SDP * 6.5f, Core.SDP * 1.25f, Core.fm, "-", 
				DrawableString.ALIGN_RIGHT);

		menu.build(btnMore.w, btnMore.h);
		
		for(int i = 0; i < MAX_MESSAGES; i++) {
			messages[i] = new Message(0, 0, Core.fm, "");
			activeMessages = 0;
		}
		
		btnMore.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(Button sender) {		
				menu.setPosition(btnMore.x + leftPanel.x, btnMore.y + btnMore.h - menu.getHeight() + leftPanel.y);
				menu.show();
			}
			
		});

		Core.player.setListeners(
				new OnLivesChangedListener() {

					@Override
					public void onLivesChanged(int lives) {
						if(lives < 0) {
							Core.game.onGameOver(false);
						} else {
							txtLives.setText(String.valueOf(lives));
							
							livesAnimationTime = 0f;
							animateLives = true;
						}
					}

				}, 
				new OnGoldChangedListener() {

					@Override
					public void onGoldChanged(int gold) {
						txtGold.setText(String.valueOf(gold));
						
						goldAnimationTime = 0f;
						animateGold = true;
					}

				});
		
		Core.lm.setOnWaveChangeListener(new WaveChangeListener() {

			@Override
			public void onWaveChange(int currentWave) {
				txtWave.setText(String.valueOf(currentWave));
			}
			
		});
	}
	
	public void rebuild() {
		// currently rebuild doesn't actually resize anything...
		// TODO: make rebuild resize things!
		
		menu.build(btnMore.w, btnMore.h);
	}
	
	public boolean isBuilt() {
		return built;
	}

	private class ShowLeftPanel implements Updatable {
		// see the comment on leftPanel above for the use of this variable
		private final float FINAL_X = Core.SDP_H / 2.0f;
		private final float START_X = -leftPanel.w;
		private final float d = FINAL_X - START_X;
		private float time = 0.0f;
		private float duration = 1f;

		@Override
		public boolean update(float dt) {
			time += dt;

			if(time < duration) {
				float t = time;
				t/=duration;
				leftPanel.x = -d * t * (t - 2) + START_X;
				return true;
			} else {
				leftPanel.x = FINAL_X;

				leftPanelRect.left = (int) 0;
				leftPanelRect.right = (int) (leftPanel.x + leftPanel.w);
				leftPanelRect.top = (int) leftPanel.y;
				leftPanelRect.bottom = (int) (Core.canvasHeight);
				return false;
			}
		}

	}

	private class ShowRightPanel implements Updatable {

		private final float FINAL_X = Core.canvasWidth - (Core.SDP_H / 2.0f) - rightPanel.width;
		private final float START_X = Core.canvasWidth;
		private final float d = FINAL_X - START_X;
		private float time = 0.0f;
		private float duration = 1f;

		@Override
		public boolean update(float dt) {
			time += dt;

			if(time < duration) {
				float t = time;
				t /= duration;
				rightPanel.x = -d * t * (t - 2) + START_X;
				waveHud.x = rightPanel.x;
				return true;
			} else {
				rightPanel.x = FINAL_X;
				waveHud.x = FINAL_X;

				rightPanelRect.left = (int) rightPanel.x;
				rightPanelRect.right = (int) (Core.canvasWidth);
				rightPanelRect.top = (int) leftPanel.y;
				rightPanelRect.bottom = (int) (Core.canvasHeight);
				rightPanel.notifyPositionChanged();
				return false;
			}
		}
	}

	public void show() {
		leftPanel.y = Core.canvasHeight - leftPanel.h - (Core.SDP_H / 2.0f);
		rightPanel.y = leftPanel.y;
		waveHud.y = leftPanel.y + leftPanel.h - waveHud.height;

		if(leftUpdatable != null) {
			Core.gu.removeUiUpdatable(leftUpdatable);
			Core.gu.removeUiUpdatable(rightUpdatable);
		}
		leftUpdatable = new ShowLeftPanel();
		rightUpdatable = new ShowRightPanel();
		
		Core.gu.addUiUpdatable(leftUpdatable);
		Core.gu.addUiUpdatable(rightUpdatable);
	}

	@Override
	public void draw(float offX, float offY) {
		leftPanel.draw(0, 0);
		rightPanel.draw(0, 0);
		waveHud.draw(0, 0);

		// left panel stuff
		factionIcon.draw(leftPanel.x, leftPanel.y);
		lblGold.draw(leftPanel.x, leftPanel.y);
		lblLives.draw(leftPanel.x, leftPanel.y);
		lblWave.draw(leftPanel.x, leftPanel.y);
		
		menu.draw(0, 0);
		btnMore.draw(leftPanel.x, leftPanel.y);

		txtGold.draw(leftPanel.x, leftPanel.y);
		txtLives.draw(leftPanel.x, leftPanel.y);
		txtWave.draw(leftPanel.x, leftPanel.y);
		
		if(!supportsMultitouch)
			zoomControl.draw(0, 0);
		
		for(int i = 0; i < activeMessages; i++) {
			messages[i].draw(0, 0);
		}
	}

	@Override
	public void refresh() {
		leftPanel.refresh();
		rightPanel.refresh();
		waveHud.refresh();

		factionIcon.refresh();
		lblGold.refresh();
		lblLives.refresh();
		btnMore.refresh();

		menu.refresh();
		
		txtGold.refresh();
		txtLives.refresh();
		
		zoomControl.refresh();
	}

	@Override
	public boolean update(float dt) {
		rightPanel.update(dt);
		waveHud.update(dt);
		
		for(int i = 0; i < activeMessages; i++) {
			if(!messages[i].update(dt)) {
				int end = activeMessages - 1;
				Message temp = messages[end];
				messages[end] = messages[i];
				messages[i] = temp;
				activeMessages--;
				// since this element changed
				// we need to reupdate it.
				i--;
			}
		}
		
		if(animateGold) {
			goldAnimationTime += dt;
			
			if(goldAnimationTime < TEXT_CHANGE_ANIMATION_DURATION) {
				float t = goldAnimationTime / TEXT_CHANGE_ANIMATION_DURATION;
				txtGold.setScale(0.2f * t * (t - 2) + 1.2f);
			} else {
				txtGold.setScale(1f);
				animateGold = false;
			}
		}
		
		if(animateLives) {
			livesAnimationTime += dt;
			
			if(livesAnimationTime < TEXT_CHANGE_ANIMATION_DURATION) {
				float t = livesAnimationTime / TEXT_CHANGE_ANIMATION_DURATION;
				txtLives.setScale(0.2f * t * (t - 2) + 1.2f);
			} else {
				txtLives.setScale(1f);
				animateLives = false;
			}
		}
		
		return true;
	}

	public void registerClickables(ClickableCollection c) {
		c.addClickable(this);
	}

	public Tower getSelectedTower() {
		return rightPanel.getSelectedTower();
	}
	
	@Override
	public boolean onTouchEvent(int action, float x_, float y_) {
		final float x = Core.originalTouchX;
		final float y = Core.originalTouchY;
		
		if(rightPanel.onTouchEvent(action, x, y) || waveHud.onTouchEvent(action, x, y) || 
				btnMore.onTouchEvent(action, x - (int)leftPanel.x, y - (int)leftPanel.y) ||
				menu.onTouchEvent(action, x, y)) {
			return true;
		}
		return leftPanelRect.contains(x, y) || 
				rightPanelRect.contains(x, y) ||
				(!supportsMultitouch && zoomControl.onTouchEvent(action, x, y));	
	}

	/**
	 * Called once per level.
	 */
	public void prepareForLevel() {
		waveHud.reset();
		waveHud.generateWaveInfomation();
	}
	
	public void hideMenu() {
		menu.hide();
	}

	public void setGameSpeed(int speed) {
		waveHud.setSpeed(speed);
	}
	
	public void showMessage(String msg) {
		Message free;
		if(activeMessages == MAX_MESSAGES) {
			float minY = messages[0].y;
			free = messages[0];
			for(int i = 1; i < MAX_MESSAGES; i++) {
				if(messages[i].y < minY) {
					minY = messages[i].y;
					free = messages[i];
				}
			}
		} else {
			free = messages[activeMessages];
		}
		
		for(int i = 0; i < activeMessages; i++) {
			messages[i].y -= free.height;
		}
		
		free.y = leftPanel.y - free.height;
		free.x = leftPanel.x;
		free.setText(msg);
		free.time = 0;
		free.duration = DEFAULT_MESSAGE_DURATION;
		
		if(activeMessages != MAX_MESSAGES) {
			activeMessages++;
		}
	}

	private static class Message extends DrawableString implements Updatable {
		float time;
		float duration;
		
		public Message(float x, float y, FontManager fm, String input) {
			super(x, y, fm, input);
		}

		@Override
		public boolean update(float dt) {
			time += dt;
			if(time < duration) {
				float t = time/duration;
				setTransparency(1 - t * t);
			} else {
				setTransparency(1f);
				return false;
			}
			return true;
		}
		
	}
	
	public RectF getRegion(int hudElementId) {
		switch(hudElementId) {
		case HUD_LEFT_PANEL:
			return leftPanelRect;
		case HUD_RIGHT_PANEL:
			return rightPanelRect;
		case HUD_TOWER_MENU:{
			RectF rect = new RectF(rightPanelRect);
			rect.bottom = (int) (rect.top + rightPanel.height);
			return rect;}
		case HUD_WAVE_CONTROL:{
			RectF rect = new RectF(rightPanelRect);
			rect.top = (int) (rect.top + rightPanel.height);
			return rect;}
		default:
			DebugLog.e(TAG, "Attempting to retreieve invalid region",
					new Exception());
			return null;
		}
	}

	public void setSelection(int selected) {
		rightPanel.setSelectedIndex(selected);
	}
}
