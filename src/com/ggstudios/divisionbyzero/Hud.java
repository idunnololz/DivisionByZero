package com.ggstudios.divisionbyzero;

import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Rect;

import com.ggstudios.divisionbyzero.Button.OnClickListener;
import com.ggstudios.divisionbyzero.Player.OnGoldChangedListener;
import com.ggstudios.divisionbyzero.Player.OnLivesChangedListener;
import com.ggstudios.utils.DebugLog;

public class Hud extends Drawable implements Clickable, Updatable{
	private static final String TAG  = "Hud";
	private static final float OFF_SCREEN = -999f;
	
	private PictureBox leftPanel;
	private TowerMenu rightPanel;
	private WaveControlHud waveHud;
	private PictureBox factionIcon;

	private Rect leftPanelRect = new Rect();
	private Rect rightPanelRect = new Rect();

	private Button btnMore;
	private Label lblGold;
	private Label lblLives;

	private DrawableString txtGold;
	private DrawableString txtLives;

	private ShowLeftPanel leftUpdatable;
	private ShowRightPanel rightUpdatable;
	
	private ZoomControl zoomControl;
	
	private boolean supportsMultitouch;
	
	private InGameMenu menu;
	
	public Hud() { 
		zoomControl = new ZoomControl();
		waveHud = new WaveControlHud();
		menu = new InGameMenu();
	}

	public void build() {
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
		rightPanel.addTower(new Tower(0, 0, TowerLibrary.HEAVY, 0));
		rightPanel.addTower(new Tower(0, 0, TowerLibrary.SPECIALIST, 0));
		rightPanel.addTower(new Tower(0, 0, TowerLibrary.DIAMOND, 0));
		rightPanel.addTower(new Tower(0, 0, TowerLibrary.CIRCLE, 0));
		rightPanel.build();

		btnMore = new Button(leftPanel.w - Core.SDP, leftPanel.h - Core.SDP, Core.SDP, Core.SDP, R.drawable.button_more);
		btnMore.setPadding((int)Core.SDP_H);
		lblGold = new Label(Core.SDP * 2.5f, Core.SDP_H * 0.5f, p, "Gold");
		lblLives = new Label(Core.SDP * 2.5f, Core.SDP * 0.75f, p, "Lives");
		txtGold = new DrawableString(Core.SDP * 6.5f, Core.SDP_H * 0.5f, Core.fm, "-",
				DrawableString.ALIGN_RIGHT);
		txtLives = new DrawableString(Core.SDP * 6.5f, Core.SDP * 0.75f, Core.fm, "-", 
				DrawableString.ALIGN_RIGHT);

		menu.build(btnMore.w, btnMore.h);
		
		btnMore.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				DebugLog.d(TAG, "c");
				
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
						}
					}

				}, 
				new OnGoldChangedListener() {

					@Override
					public void onGoldChanged(int gold) {
						txtGold.setText(String.valueOf(gold));
					}

				});
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
				t/=duration;
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
		menu.draw(0, 0);
		btnMore.draw(leftPanel.x, leftPanel.y);

		txtGold.draw(leftPanel.x, leftPanel.y);
		txtLives.draw(leftPanel.x, leftPanel.y);

		if(!supportsMultitouch)
			zoomControl.draw(offX, offY);
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
		return true;
	}

	public void registerClickables(ClickableCollection c) {
		c.addClickable(this);
	}

	public Tower getSelectedTower() {
		return rightPanel.getSelectedTower();
	}
	
	@Override
	public boolean onTouchEvent(int action, int x_, int y_) {
		final int x = Core.originalTouchX;
		final int y = Core.originalTouchY;
		
		if(rightPanel.onTouchEvent(action, x, y) || waveHud.onTouchEvent(action, x, y) || 
				btnMore.onTouchEvent(action, x - (int)leftPanel.x, y - (int)leftPanel.y) ||
				menu.onTouchEvent(action, x, y)) {
			return true;
		}
		return leftPanelRect.contains(x, y) || 
				rightPanelRect.contains(x, y) ||
				zoomControl.onTouchEvent(action, x, y);	
	}

	/**
	 * Called once per level.
	 */
	public void prepareForLevel() {
		waveHud.reset();
		waveHud.generateWaveInfomation();
	}

}
