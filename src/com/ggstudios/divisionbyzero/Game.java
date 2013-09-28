package com.ggstudios.divisionbyzero;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.opengl.GLES20;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.ggstudios.divisionbyzero.Button.OnClickListener;
import com.ggstudios.divisionbyzero.LevelManager.SpawnEvent;
import com.ggstudios.divisionbyzero.LevelMap.LevelNode;
import com.ggstudios.divisionbyzero.PopupMenu.MenuItem;
import com.ggstudios.divisionbyzero.StateManager.UserLevelData;
import com.ggstudios.divisionbyzero.UpgradeWindow.OnUpgradeSelectedListener;
import com.ggstudios.utils.BufferUtils;
import com.ggstudios.utils.DebugLog;

public class Game {
	private static final String TAG = "Game";

	public static final int
	STATE_CLEAN_START 	= 1,	// state when the level has just been loaded
	STATE_SAVED			= 2,	// state upon resuming
	STATE_INCONISTENT	= 3;	// error state.

	private static final float MAX_ZOOM = 2f, MIN_ZOOM = 1f;

	private boolean isGlDataLoaded;

	DrawableCollection<Drawable> backgroundElements;
	TowerManager towerManager;
	DrawableCollection<Sprite> spriteElements;
	BulletManager bulletMgr;
	ExtrasManager extrasMgr;
	DrawableCollection<Drawable> hud;

	ClickableCollection front;
	ClickableCollection back;
	ClickableCollection mid;

	private Hud hudSystem;

	Map.Builder mapBuilder = new Map.Builder();
	Map map;

	private LineGuide lg;
	Player player;
	PauseMenu pauseMenu;

	PopupMenu popup;

	private GameUpdater gameUpdater;
	private LevelManager levelMgr;

	private int popupUpgradeIndex;
	private int popupSellIndex;

	private int state;

	private Circle towerRadius;
	private UpgradeWindow upgradeWindow;

	private ScaleGestureDetector scaleDetector;	

	private Drawable[] drawables;

	private InfoDialog infoDialog;
	private EndDialog endDialog;

	private boolean gameEnded = false;

	private int levelId;

	private StateManager stateMgr;

	private WaveControlDialog waveDialog;

	public Game() {
		isGlDataLoaded = false;

		stateMgr = StateManager.getInstance();

		//set up grid view
		Core.grid = new Grid();

		Core.lm = new LevelManager();
		Core.tm = new TextureManager();
		Core.fm = new FontManager();
		Core.sm = new SpawnManager();

		Core.gu = new GameUpdater();
		gameUpdater = Core.gu;
		levelMgr = Core.lm;

		Core.drawables = new Drawable[Core.PRIMARY_DRAWABLES_USED];
		Core.clickables = new ArrayList<Clickable>();

		// bind our drawables for easier access...
		drawables = Core.drawables;

		Core.player = new Player(0, 0);

		scaleDetector = new ScaleGestureDetector(Core.context, new ScaleListener());

		player = Core.player;
		popup = new PopupMenu();

		backgroundElements = new DrawableCollection<Drawable>();
		towerManager = new TowerManager();
		spriteElements = new DrawableCollection<Sprite>();
		hud = new DrawableCollection<Drawable>();
		bulletMgr = new BulletManager();
		extrasMgr = new ExtrasManager();

		front = new ClickableCollection();
		back = new ClickableCollection();
		mid = new ClickableCollection();

		Core.hud = hudSystem = new Hud();

		pauseMenu = new PauseMenu();
		upgradeWindow = new UpgradeWindow(R.drawable.panel);
		infoDialog = new InfoDialog();
		endDialog = new EndDialog();
		waveDialog = new WaveControlDialog();
	}

	public void onSurfaceCreated() {
		DebugLog.d(TAG, "onSurfaceCreated()");

		if (!isGlDataLoaded) {
			loadGlData();
		}

		switch(state) {
		case STATE_CLEAN_START:
			setupScreen();
			Core.gu.start();
			state = STATE_SAVED;
			break;
		default:
			break;
		}

		Tower.init();
	}

	public int getState() {
		return state;
	}

	public void notifySurfaceChanged() {
		setupScreen();
	}

	/**
	 * Removes all drawables, clickables and updatables and sets it up fresh!
	 */
	private void cleanSlate() {
		// reset clickable:
		Core.clickables.clear();
		front.clear();
		mid.clear();
		back.clear();

		Core.addClickable(front);
		Core.addClickable(mid);
		Core.addClickable(back);

		// reset drawables:
		synchronized(Core.gr) {
			backgroundElements.clear();
			towerManager.clear();
			spriteElements.clear();
			bulletMgr.clear();
			hud.clear();
		}

		Core.gu.clearGameUpdatables();
		Core.gu.clearUiUpdatables();
	}

	/**
	 * GL data is guaranteed to be setup by this point.
	 * 
	 * Do all SIZE DEPENDANT initialization here...
	 * This will be called whenever canvas size changes...
	 */
	private void setupScreen() {
		DebugLog.d(TAG, "setupScreen()");

		Core.clickables.clear();
		front.clear();
		mid.clear();
		back.clear();

		Core.addClickable(front);
		Core.addClickable(mid);
		Core.addClickable(back);

		backgroundElements.clear();
		hud.clear();

		Core.fm.generateFont(Core.SDP_H * 0.9f);

		mapBuilder.setSize((int)Core.canvasWidth, (int) Core.canvasHeight);
		map = mapBuilder.build();
		map.initPathFinder();
		map.findPath();
		Core.MAP_SDP = map.getTileW();

		refresh();

		clipW = (int)(map.getWidth() * Core.zoom);
		clipH = (int)(map.getHeight() * Core.zoom);

		extrasMgr.loadGlData();

		pauseMenu.build();

		bulletMgr.setBulletBounds(0 - Core.MAP_SDP, 0 - Core.MAP_SDP, map.getWidth() + Core.MAP_SDP, map.getHeight() + Core.MAP_SDP);

		// set up the background
		towerRadius = new Circle(0, 0, 10.0f, 30);
		towerRadius.visible = false;

		backgroundElements.addDrawable(towerRadius);
		backgroundElements.addDrawable(map);

		// set up hud here...
		lg = new LineGuide();

		hudSystem.build();
		lg.hide();
		hud.addDrawable(lg);
		hud.addDrawable(hudSystem);
		infoDialog.build();
		infoDialog.setVisible(false);
		hud.addDrawable(infoDialog);

		hudSystem.show();
		hudSystem.registerClickables(front);

		Core.gu.removeUiUpdatable(hudSystem);
		Core.gu.addUiUpdatable(hudSystem);

		// center the map at the start... vee
		map.setOffset(map.getMarginLeft(), map.getMarginTop());

		// set up the popup menu
		buildPopupMenu(popup);

		front.addClickable(popup);

		// this is to add a "buffer" to the array for multi-threading purposes
		// so that one thread will never go out of bounds of the array
		spriteElements.addDrawable(Sprite.getPreloadedInstance(-1000, -1000, 0, 0, 0));
		spriteElements.addDrawable(Sprite.getPreloadedInstance(-1000, -1000, 0, 0, 0));
		spriteElements.addDrawable(Sprite.getPreloadedInstance(-1000, -1000, 0, 0, 0));
		spriteElements.removeDrawable(0);
		spriteElements.removeDrawable(0);
		spriteElements.removeDrawable(0);

		// initialize upgrade window
		upgradeWindow.build();
		upgradeWindow.setVisible(false);
		front.addClickable(upgradeWindow);
		hud.addDrawableToTop(upgradeWindow);

		System.gc();

		upgradeWindow.setOnCloseClick(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				upgradeWindow.setVisible(false);
			}

		});

		upgradeWindow.setOnUpgradeSelectedListener(new OnUpgradeSelectedListener() {

			@Override
			public void onUpgradeSelected(Tower t, int selection) {
				int cost = t.getUpgradeCost(selection);
				if(player.getGold() >= cost) {

					player.deductGold(cost);
					t.upgrade(selection);

					refreshPopupInfo(t);
				}

				upgradeWindow.setVisible(false);
			}

		});

		Core.addClickable(map);

		waveDialog.build();
		hud.addDrawableToTop(waveDialog);
		front.addClickable(waveDialog);

		endDialog.build();
		hud.addDrawableToTop(endDialog);
		front.addClickable(endDialog);

		if(gameUpdater.isPaused()) {
			// if the game is paused, then we need to recreate the paused screen
			showPauseScreen();
		}
	}

	private void buildPopupMenu(final PopupMenu popup) {
		popup.onSurfaceCreated();
		popup.clearItems();

		popupUpgradeIndex = popup.addItemWithLabel(R.drawable.popup_ic_upgrade, new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				Tower t = (Tower) sender.getTag();
				if(t.hasTypeUpgrade()) {
					upgradeWindow.updateContent(t);
					upgradeWindow.setVisible(true);
				} else if(!t.isMaxLevel()) {
					if(player.getGold() >= t.getUpgradeCost()) {
						player.deductGold(t.getUpgradeCost());
						t.upgrade();

						refreshPopupInfo(t);
					}
				}
			}

		});

		popupSellIndex = popup.addItemWithLabel(R.drawable.popup_ic_sell, new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				Tower t = (Tower) sender.getTag();
				player.awardGold(t.getSellPrice());
				map.unregisterObject(t.tileX, t.tileY);
				map.setBlocked(t.tileX, t.tileY, false);

				towerManager.removeDrawable(t);
				t.destroy();

				popup.hide();
				infoDialog.setVisible(false);
				towerRadius.visible = false;

				map.findPath();
			}

		});

		popup.addItem(R.drawable.popup_ic_close, new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				cancelSelection();
			}

		});

		popup.build();
	}

	public void cancelSelection() {
		popup.hide();
		infoDialog.setVisible(false);
		towerRadius.visible = false;
	}

	/**
	 * First function called after the constructor...
	 * Do initial level setup here
	 * 
	 * GL is not guaranteed to be available here.
	 * 
	 * @param resId Resource Id of the level file
	 */
	public void loadLevel(int resId, int levId) {
		DebugLog.d(TAG, "loadLevel()");
		levelId = levId;
		gameEnded = false;		
		Core.currentLevelResId = resId;

		Core.lm.loadLevelFromFile(Core.context, resId);

		Core.player.setLives(Core.lm.getInitLives());
		Core.player.setGold(Core.lm.getInitGold());

		hudSystem.prepareForLevel();

		backgroundElements.clear();
		towerManager.clear();
		spriteElements.clear();
		bulletMgr.clear();
		hud.clear();

		drawables[0] = new Drawable() {

			@Override
			public void draw(float offX, float offY) {
				Core.forceVisible = false;

				final float zoom = Core.zoom;
				Utils.scale(zoom, Core.mixedMatrix);
				GLES20.glUniformMatrix4fv(Core.U_MIXED_MATRIX_HANDLE, 1, false, Core.mixedMatrix, 0);
				Utils.scale(1f/zoom, Core.mixedMatrix);

				GLES20.glScissor(clipL, clipT, clipW, clipH);
				GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
			}

			@Override
			public void refresh() { /* do nothing */}

		};
		drawables[1] = backgroundElements;
		drawables[2] = towerManager;
		drawables[3] = spriteElements;
		drawables[4] = bulletMgr;
		drawables[5] = new Drawable() {

			@Override
			public void draw(float offX, float offY) {
				GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
			}

			@Override
			public void refresh() {
				// do nothing!
			}

		};

		drawables[6] = extrasMgr;
		drawables[7] = popup;
		drawables[8] = new Drawable() {

			@Override
			public void draw(float offX, float offY) {
				Core.forceVisible = true;

				GLES20.glUniformMatrix4fv(Core.U_MIXED_MATRIX_HANDLE, 1, false, Core.mixedMatrix, 0);

			}

			@Override
			public void refresh() {
				// do nothing!
			}

		};
		drawables[9] = hud;

		mapBuilder.setType(Core.lm.getMapType());

		state = STATE_CLEAN_START;

		Core.gu.addGameUpdatable(Core.lm);
		Core.gu.addGameUpdatable(Core.sm);
	}

	public void onLoadFinished() {
		Core.handler.sendEmptyMessage(MainActivity.MSG_SWITCH_TO_GLSURFACEVIEW);
	}

	/**
	 * Do all first time SIZE INDEPENDENT GL initialization here.
	 * 
	 * All screen size dependent GL loading should be done in {@link #setupScreen()}
	 */
	public void loadGlData() {
		DebugLog.d(TAG, "loadGlData()");

		initializeBuffers();

		Core.tm.loadGameTextures();

		bulletMgr.growPool(200);
		bulletMgr.loadGlData();

		isGlDataLoaded = true;

		towerManager.loadGlData();

		Sprite.doOnce();
	}

	private int clipL, clipT, clipW, clipH;

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float temp = Core.zoom;

			if(!scaling){
				lg.hide();

				for(Clickable c : Core.clickables) {
					c.onTouchEvent(MotionEvent.ACTION_CANCEL, 0, 0);
				}
			}

			scaling = true;
			float newZoom = Core.zoom * detector.getScaleFactor();

			// Don't let the object get too small or too large.
			setZoom(newZoom);

			float a_x = (detector.getFocusX() / Core.canvasWidth)	* ((Core.canvasWidth  / Core.zoom) - (Core.canvasWidth  / temp));
			float a_y = (detector.getFocusY() / Core.canvasHeight)	* ((Core.canvasHeight  / Core.zoom) - (Core.canvasHeight / temp));
			Core.offX += a_x;
			Core.offY += a_y;

			onScrolled();

			return true;
		}
	}

	private int activePointerId = -1;
	private boolean scaling = false;

	public void onTouchEvent(MotionEvent event) {		
		if( state == STATE_CLEAN_START ) return;

		scaleDetector.onTouchEvent(event);

		switch(event.getAction()){
		case MotionEvent.ACTION_DOWN: {
			activePointerId = event.getPointerId(0);

			scaling = false;

			Core.originalTouchX = (int) event.getX();
			Core.originalTouchY = (int) event.getY();
			final int eX = (int)(event.getX() / Core.zoom);
			final int eY = (int)(event.getY() / Core.zoom);

			lg.setXY(Core.originalTouchX, Core.originalTouchY);
			lg.show();

			for(Clickable c : Core.clickables) {
				if(c.onTouchEvent(event.getAction(), eX, eY))
					break;
			}
			break;
		}

		case MotionEvent.ACTION_MOVE: {
			if (!scaleDetector.isInProgress() && activePointerId != -1) {
				final int pointerIndex = event.findPointerIndex(activePointerId);
				if(pointerIndex == -1){ 
					activePointerId = -1; 
					return;
				}
				Core.originalTouchX = (int) event.getX(pointerIndex);
				Core.originalTouchY = (int) event.getY(pointerIndex);
				final int eX = (int)(event.getX(pointerIndex) / Core.zoom);
				final int eY = (int)(event.getY(pointerIndex) / Core.zoom);

				lg.setXY(Core.originalTouchX, Core.originalTouchY);

				for(Clickable c : Core.clickables) {
					if(c.onTouchEvent(event.getAction(), eX, eY))
						break;
				}
			}
			break;
		}

		case MotionEvent.ACTION_CANCEL:
			activePointerId = -1;
			break;

		case MotionEvent.ACTION_UP: {
			if (activePointerId != -1) {
				final int pointerIndex = event.findPointerIndex(activePointerId);
				Core.originalTouchX = (int) event.getX(pointerIndex);
				Core.originalTouchY = (int) event.getY(pointerIndex);
				final int eX = (int)(event.getX(pointerIndex) / Core.zoom);
				final int eY = (int)(event.getY(pointerIndex) / Core.zoom);

				activePointerId = -1;

				lg.hide();

				for(Clickable c : Core.clickables) {
					if(c.onTouchEvent(event.getAction(), eX, eY))
						break;
				}
			}

			break;
		}

		case MotionEvent.ACTION_POINTER_UP: {
			final int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) 
					>> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
					final int pointerId = event.getPointerId(pointerIndex);
					if (pointerId == activePointerId) {
						activePointerId = -1;
					}
					break;
		}
		}

	}

	public void mapClicked(int tileX, int tileY) {
		Tower t = hudSystem.getSelectedTower();
		if(map.isInBounds(tileX, tileY)) {
			if(t != null && !map.isBlocked(tileX, tileY)
					&& Core.player.getGold() >= t.getCost()) {

				final int tx = (int) ((tileX + 0.5f) * map.getTileW());
				final int ty = (int) ((tileY + 0.5f) * map.getTileH());

				synchronized(spriteElements) {
					List<Sprite> list = spriteElements.getRawList();
					final int len = spriteElements.size();
					for(int i = 0; i < len; i++) {
						Sprite s = list.get(i);
						if(s.isObstacle() && s.rect.contains(tx, ty)) {
							// there are sprites where this tower is going to be placed!
							return;
						}
					}

					map.setBlocked(tileX, tileY, true);

					if(map.findPath()) {
						// cancel any previous selections
						cancelSelection();

						placeTower(tileX, tileY, t.getTowerType(), t.getLevel());
						Core.player.deductGold(t.getCost());

						for(int i = 0; i < len; i++) {
							list.get(i).updatePath();
						}

						//map.drawPath();
					} else {
						// if there exists no path from the spawn point to the end
						// then the tower placement is invalid...

						// thus revert map and cancel the tower placement
						map.setBlocked(tileX, tileY, false);
					}
				}
			} else if(map.isBlocked(tileX, tileY)) {
				Object o = map.getObject(tileX, tileY);
				if(o instanceof Tower) {
					Tower tower = (Tower)o;

					refreshPopupInfo(tower);

					popup.popup(tower.x, tower.y);
					infoDialog.setInfo(tower);
					infoDialog.setVisible(true);
				}
			}
		}
	}

	public void placeTower(int tileX, int tileY, int type, int level) {
		map.setBlocked(tileX, tileY, true);

		final int tx = (int) ((tileX + 0.5f) * map.getTileW());
		final int ty = (int) ((tileY + 0.5f) * map.getTileH());

		// the tower placement is valid.
		// construct the tower and add it to the drawables
		Tower tower = new Tower(tileX, tileY, type, level);
		tower.setVBO(Core.GeneralBuffers.map_tile);
		tower.x = tx;
		tower.y = ty;

		towerManager.addDrawable(tower);
		Core.gu.addGameUpdatable(tower);

		map.registerObject(tileX, tileY, tower);
	}

	public void placeTower(int tileX, int tileY, DataInputStream stream) throws IOException {
		map.setBlocked(tileX, tileY, true);

		final int tx = (int) ((tileX + 0.5f) * map.getTileW());
		final int ty = (int) ((tileY + 0.5f) * map.getTileH());

		// the tower placement is valid.
		// construct the tower and add it to the drawables
		Tower tower = new Tower(tileX, tileY, stream);
		tower.setVBO(Core.GeneralBuffers.map_tile);
		tower.x = tx;
		tower.y = ty;

		towerManager.addDrawable(tower);
		Core.gu.addGameUpdatable(tower);

		map.registerObject(tileX, tileY, tower);
	}

	private void refreshPopupInfo(Tower tower) {
		MenuItem item = popup.getItem(popupUpgradeIndex);
		if(tower.hasTypeUpgrade()) {
			item.setLabel("TYPE");
		} else if (tower.isMaxLevel()) {
			item.setLabel("MAXED");
		} else {
			item.setLabel(String.valueOf(tower.getUpgradeCost()));
		}

		item.setTag(tower);

		MenuItem sell = popup.getItem(popupSellIndex);
		sell.setLabel(String.valueOf(tower.getSellPrice()));
		sell.setTag(tower);

		showTowerRadius(tower);
	}

	public void showTowerRadius(Tower tower) {
		showTowerRadius(tower, tower.x, tower.y);
	}

	public void showTowerRadius(Tower tower, float x, float y) {
		if(tower == null) return;
		towerRadius.update(x, y, tower.getRange());
		towerRadius.visible = true;
	}

	public void hideTowerRadius() {
		towerRadius.visible = false;
	}

	public void reportFinishedSprite(Sprite enemySprite) {
		Core.player.decrementLives();
		spriteElements.removeDrawableStrict(enemySprite);

		checkWinCondition();
	}

	public void reportKilledSprite(Sprite enemySprite) {
		player.incrementKill();
		player.awardGold(enemySprite.getGoldReward());
		spriteElements.removeDrawableStrict(enemySprite);

		checkWinCondition();
	}

	public void addEnemy(Sprite s) {
		if(s.needToLoad())
			s.load(map);
		Core.gu.addGameUpdatable(s);
		spriteElements.addDrawable(s);		
	}

	public Bullet obtainBullet() {
		return bulletMgr.obtain();
	}

	public void gameDone() {
		Core.gu.gameDone();
	}

	private boolean prevPauseState;

	public void onPause() {
		if(!gameEnded) {
			// if the game isn't over..
			onPauseClick();

			prevPauseState = Core.gu.isPaused();
			Core.gu.pause();
		}
	}

	public void onResume() {
		if(prevPauseState) {
			// if we were already paused... do nothing
		} else {
			Core.gu.unpause();	
		}
	}

	public Sprite getEnemyHit(Bullet b, int x, int y) {
		List<Sprite> list = spriteElements.getRawList();
		final int len = spriteElements.size();
		for(int i = len - 1; i > -1; i--) {
			Sprite sprite = list.get(i);
			if(sprite.rect.contains(x, y)) {
				return sprite;
			}
		}
		return null;
	}

	public Sprite getGhostHit(Bullet b, int x, int y) {
		List<Sprite> list = spriteElements.getRawList();
		final int len = spriteElements.size();
		for(int i = len - 1; i > -1; i--) {
			Sprite sprite = list.get(i);
			if(sprite.isGhost() && sprite.rect.contains(x, y)) {
				return sprite;
			}
		}
		return null;
	}

	public void beginFirstWave() {
		Core.lm.start();
	}

	public void onPauseClick() {
		DebugLog.d(TAG, "pausing");

		if(Core.gu.isPaused())
			return;

		Core.gu.pause();
		showPauseScreen();
	}

	public void showPauseScreen() {
		hud.addDrawableToTop(pauseMenu);
		front.addClickable(pauseMenu);
	}

	public void onResumeClick() {
		DebugLog.d(TAG, "resuming");
		hud.removeDrawableStrict(pauseMenu);
		front.removeClickable(pauseMenu);
		Core.gu.unpause();
	}

	private void initializeBuffers() {
		if(Core.GeneralBuffers.map_tile == null) {
			Core.GeneralBuffers.map_tile = new VBO();
			Core.GeneralBuffers.map_half_tile = new VBO();
		}
	}

	private static final int MAX_TEXTURES = 300;

	public void refresh() {
		DebugLog.d(TAG, "refresh()");

		initializeBuffers();

		float halfW = Core.MAP_SDP / 2;
		{
			final float arr[] = {
					-halfW, -halfW, 	//Vertex 0
					halfW, -halfW, 	//v1
					-halfW,  halfW,  	//v2
					halfW,  halfW, 	//v3
			};
			Core.GeneralBuffers.map_tile.setVBO(Core.MAP_SDP, Core.MAP_SDP, BufferUtils.copyToBuffer(arr), VBO.Alignment.CENTER);
		}
		halfW /= 2f;
		{
			final float arr[] = {
					-halfW, -halfW, 	//Vertex 0
					halfW, -halfW, 	//v1
					-halfW,  halfW,  	//v2
					halfW,  halfW, 	//v3
			};
			Core.GeneralBuffers.map_half_tile.setVBO(Core.MAP_SDP / 2f, Core.MAP_SDP / 2f, BufferUtils.copyToBuffer(arr), VBO.Alignment.CENTER);
		}

		final short[] indices = new short[MAX_TEXTURES * 6];  // Create Temp Index Buffer
		final int len = indices.length;                       // Get Index Buffer Length
		short j = 0;                                    // Counter
		for ( int i = 0; i < len; i+= 6, j += 4)  {  // FOR Each Index Set (Per Sprite)
			indices[i + 0] = (short)( j + 0 );           // Calculate Index 0
			indices[i + 1] = (short)( j + 1 );           // Calculate Index 1
			indices[i + 2] = (short)( j + 2 );           // Calculate Index 2
			indices[i + 3] = (short)( j + 2 );           // Calculate Index 3
			indices[i + 4] = (short)( j + 3 );           // Calculate Index 4
			indices[i + 5] = (short)( j + 0 );           // Calculate Index 5
		}

		Core.indiceHandle = BufferUtils.copyToBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indices, len);

		Core.fm.refresh();

		Sprite.refreshResources();
		endDialog.refresh();


		for(Drawable d : Core.drawables){
			if(d != null) {
				d.refresh();
			}
		}
		System.gc();
	}

	public void onScrolled() {
		clipL = (int) (Core.offX * Core.zoom);
		//float diffH = map.getDrawingHeight() - map.getHeight();
		clipT = (int) 0;

		map.onScroll();
	}

	/**
	 * Sets the zoom level. If the zoom level set will be normalized between 
	 * {@link #MIN_ZOOM} and {@link #MAX_ZOOM}
	 * @param zoom	New zoom level
	 * @return		True if the zoom was set. False if no changes were made.
	 */
	public boolean setZoom(float zoom) {
		float newZoom = Math.max(MIN_ZOOM, Math.min(zoom, MAX_ZOOM));

		if(newZoom == Core.zoom) return false;
		else {
			Core.zoom = newZoom;
			Core.onZoomChanged();

			clipW = (int)(map.getWidth() * Core.zoom);
			clipH = (int)(map.getHeight() * Core.zoom);
			onScrolled();

			return true;
		}
	}

	public void skipToNext() {
		levelMgr.skipToNextSpawn();
	}

	public void onGameOver(boolean hasWon) {
		if(gameEnded) return;
		gameEnded = true;

		stateMgr.clearSavedGame();

		showEndScreen(hasWon);
	}

	public boolean isGameOver() {
		return gameEnded;
	}

	private void showEndScreen(boolean success) {
		endDialog.lightSetup(success, player.getSnapshot());
		endDialog.transitionIn();
	}

	public int getLevelResourceId() {
		return Core.currentLevelResId;
	}

	public void onGameWon() {
		// game won!
		stateMgr.getLevelMap().setCompleted(levelId);
		UserLevelData data = new UserLevelData();
		data.id = levelId;
		data.status = LevelNode.STATUS_COMPLETED;
		stateMgr.getUserLevelData().add(data);
		stateMgr.saveLevelData();

		onGameOver(true);
	}

	public void checkWinCondition() {
		// check win conditions here...
		if(!gameEnded && !levelMgr.hasNextEvent() && Core.sm.eventsRunning() == 0 && spriteElements.size() == 0) {
			// game over... player won!

			onGameWon();
		}
	}

	public int getLevelId() {
		return levelId;
	}

	public void showWaveInfo(SpawnEvent event) {
		waveDialog.lightSetup(event);
		waveDialog.show();
	}

	public void restart() {
		Core.handler.sendEmptyMessage(MainActivity.MSG_SWITCH_TO_LOAD_SCREEN);

		for(Clickable c : Core.clickables) {
			c.onTouchEvent(MotionEvent.ACTION_CANCEL, 0, 0);
		}

		Core.gu.gameDone();
		Core.gu = new GameUpdater();
		cleanSlate();
		loadLevel(Core.currentLevelResId, Core.game.getLevelId());

		Core.glView.queueEvent(new Runnable() {

			@Override
			public void run() {
				cancelSelection();
				loadGlData();
				onSurfaceCreated();
				onLoadFinished();
			}

		});
	}
}
