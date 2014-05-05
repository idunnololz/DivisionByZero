package com.ggstudios.divisionbyzero;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.opengl.GLES20;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.ggstudios.divisionbyzero.Button.OnClickListener;
import com.ggstudios.divisionbyzero.PathFinder.Node;
import com.ggstudios.divisionbyzero.LevelManager.SpawnEvent;
import com.ggstudios.divisionbyzero.LevelMap.LevelNode;
import com.ggstudios.divisionbyzero.PopupMenu.MenuItem;
import com.ggstudios.divisionbyzero.StateManager.UserLevelData;
import com.ggstudios.divisionbyzero.UpgradeDialog.OnUpgradeSelectedListener;
import com.ggstudios.utils.BufferUtils;
import com.ggstudios.utils.DebugLog;
	
public class Game {
	private static final String TAG = "Game";

	public static final int STATE_CLEAN_START = 1,
			STATE_SAVED = 2,
			STATE_KILLED = 3;

	private static final float MAX_ZOOM = 2f, MIN_ZOOM = 1f;

	private boolean isGlDataLoaded;

	private static final String MESSAGE_INSUFFICIENT_FUNDS = "Insufficient funds";
	private static final String MESSAGE_INVALID_BUILD_LOCATION = "Can't build there!";
	private static final String MESSAGE_BLOCKING = "Can't block path!";

	// this is a list of drawables drawn in the order listed...
	private DrawableCollection<Drawable> backgroundElements;
	TowerManager towerManager;
	BulletManager bulletMgr;
	ExtrasManager extrasMgr;
	SpriteManager spriteMgr;
	DrawableCollection<Drawable> hud;
	DialogManager dialogs;

	ClickableCollection front;

	private Hud hudSystem;

	private Map.Builder mapBuilder = new Map.Builder();
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

	/* Used to show the current selected tower's radius */
	private Circle towerRadius;
	
	/* Used to show the radius of certain tower in other
	 * situations such as during tower construction... */
	private Circle secondaryTowerRadius;
	
	private UpgradeDialog upgradeDialog;

	private ScaleGestureDetector scaleDetector;
	private GestureDetector gestureDetector;

	private Drawable[] drawables;

	private InfoDialog infoDialog;
	private EndDialog endDialog;

	private boolean gameEnded = false;

	private int levelId;

	private StateManager stateMgr;

	private WaveControlDialog waveDialog;
	private ConfirmDialog confirmDialog;
	
	private boolean zoomEnabled = true;

	// used by a layer mask to clip the map layer
	private int clipL, clipT, clipW, clipH;
	
	private boolean pauseScreenShown = false;
	
	private GameUpdater originalGu;
	
	interface OnGameReadyListener {
		void onGameReady();
	}
	
	private OnGameReadyListener gameReadyListener;
	
	public Game() {
		DebugLog.d(TAG, "Game()");
		state = STATE_CLEAN_START;
		
		isGlDataLoaded = false;

		stateMgr = StateManager.getInstance();

		Core.lm = new LevelManager();
		Core.tm = new TextureManager();
		Core.fm = new FontManager();
		Core.sm = new SpawnManager();

		Core.grid = new Grid();
		originalGu = Core.gu = new GameUpdater();
		gameUpdater = Core.gu;
		levelMgr = Core.lm;

		Core.drawables = new Drawable[Core.PRIMARY_DRAWABLES_USED];
		Core.clickables = new ArrayList<Clickable>();

		// bind our drawables for easier access...
		drawables = Core.drawables;

		scaleDetector = new ScaleGestureDetector(Core.context,
				new ScaleListener());
		gestureDetector = new GestureDetector(Core.context,
				new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDoubleTap(MotionEvent event) {
				int action = Event.ACTION_DOUBLE_TAP;

				Core.originalTouchX = (int) event.getX();
				Core.originalTouchY = (int) event.getY();
				final int eX = (int) (event.getX() / Core.zoom);
				final int eY = (int) (event.getY() / Core.zoom);

				for (Clickable c : Core.clickables) {
					if (c.onTouchEvent(action, eX, eY)) {
						return true;
					}
				}
				return false;
			}
		});

		player = Core.player = new Player(0, 0);
		popup = new PopupMenu();

		backgroundElements = new DrawableCollection<Drawable>();
		towerManager = new TowerManager();
		hud = new DrawableCollection<Drawable>();
		dialogs = new DialogManager();
		bulletMgr = new BulletManager();
		extrasMgr = new ExtrasManager();
		spriteMgr = new SpriteManager();

		front = new ClickableCollection();

		Core.hud = hudSystem = new Hud();

		pauseMenu = new PauseMenu();
		upgradeDialog = new UpgradeDialog();
		infoDialog = new InfoDialog();
		endDialog = new EndDialog();
		waveDialog = new WaveControlDialog();
		confirmDialog = new ConfirmDialog();
	}

	public void onSurfaceCreated() {
		DebugLog.d(TAG, "onSurfaceCreated()");

		if (!isGlDataLoaded) {
			loadGlData();
		}

		switch (state) {
		case STATE_CLEAN_START:
			setupScreen();

			final Rectangle rect = new Rectangle(0, 0, Core.canvasWidth, Core.canvasHeight, 0xFF000000) {
				@Override
				public void draw(float x, float y) {
					super.draw(0, 0);
				}
			};

			hud.addDrawableToTop(rect);
			// add some special effects when the level starts...
			Core.gu.addUiUpdatable(new Updatable() {
				private final float DURATION = 1f;
				private float time = 0f;

				@Override
				public boolean update(float dt) {
					time += dt;

					if(time >= DURATION) {
						hud.removeDrawableStrict(rect);
						Core.zoom = 1f;
						Core.offX = (Core.canvasWidth - map.getWidth() * Core.zoom) / 2f;
						Core.offY = (Core.canvasHeight - map.getHeight() * Core.zoom) / 2f;
						Core.onZoomChanged();
						onScrolled();
						
						onGameReady();
						return false;
					} else {
						float t = time / DURATION;
						rect.transparency = 1f - t * t;
						Core.zoom = 0.6f + 0.4f * t * t;
						Core.offX = (Core.canvasWidth - map.getWidth() * Core.zoom) / 2f;
						Core.offY = (Core.canvasHeight - map.getHeight() * Core.zoom) / 2f;
						Core.onZoomChanged();
						onScrolled();
					}
					return true;
				}

			});
			state = STATE_SAVED;
			break;
		default:
			break;
		}

		Tower.init();
	}

	public void setOnGameReadyListener(OnGameReadyListener listener) {
		gameReadyListener = listener;
	}
	
	/**
	 * Called once the game interface has been setup and all animations have completed
	 */
	protected void onGameReady() {
		if(gameReadyListener != null)
			gameReadyListener.onGameReady();
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
		front.clear();

		// reset drawables:
		if(Core.gr != null) {
			synchronized (Core.gr) {
				backgroundElements.clear();
				towerManager.clear();
				spriteMgr.clear();
				bulletMgr.clear();
				hud.clear();
			}
		} else {
			backgroundElements.clear();
			towerManager.clear();
			spriteMgr.clear();
			bulletMgr.clear();
			hud.clear();
		}

		Core.gu.clearGameUpdatables();
		Core.gu.clearUiUpdatables();
	}

	/**
	 * GL data is guaranteed to be setup by this point.
	 * 
	 * Do all SIZE DEPENDANT initialization here... This will be called whenever
	 * canvas size changes...
	 */
	private void setupScreen() {
		DebugLog.d(TAG, "setupScreen()");

		Core.clickables.clear();
		front.clear();

		Core.addClickable(front);

		backgroundElements.clear();
		hud.clear();

		Core.fm.generateFont(Core.SDP_H * 0.9f);
		
		// reset screen state...
		Core.zoom = 1f;
		Core.offX = (Core.canvasWidth - map.getWidth() * Core.zoom) / 2f;
		Core.offY = (Core.canvasHeight - map.getHeight() * Core.zoom) / 2f;
		
		Core.onZoomChanged();
		onScrolled();
				
		map.resize((int)Core.canvasWidth, (int)Core.canvasHeight);
		Core.MAP_SDP = map.getTileW();
		
		bulletMgr.loadGlData();

		initializeBuffers();

		clipW = (int) (map.getWidth() * Core.zoom);
		clipH = (int) (map.getHeight() * Core.zoom);

		extrasMgr.loadGlData();

		pauseMenu.build();

		bulletMgr.setBulletBounds(0 - Core.MAP_SDP, 0 - Core.MAP_SDP,
				map.getWidth() + Core.MAP_SDP, map.getHeight() + Core.MAP_SDP);

		// set up the background
		secondaryTowerRadius = new Circle(0, 0, 10.0f, 30);
		secondaryTowerRadius.visible = false;
		
		towerRadius = new Circle(0, 0, 10.0f, 30);
		towerRadius.visible = false;

		backgroundElements.addDrawable(secondaryTowerRadius);
		backgroundElements.addDrawable(towerRadius);
		backgroundElements.addDrawable(map);

		// set up hud here...
		lg = new LineGuide();

		boolean hudBuilt = hudSystem.isBuilt();
		hudSystem.build();
		lg.hide();
		hud.addDrawable(lg);
		hud.addDrawable(dialogs);
		hud.addDrawable(hudSystem);
		infoDialog.build();
		infoDialog.setVisible(false);
		hud.addDrawable(infoDialog);

		if(!hudBuilt) {
			hudSystem.show();
		}
		hudSystem.registerClickables(front);

		Core.gu.removeUiUpdatable(hudSystem);
		Core.gu.addUiUpdatable(hudSystem);

		// center the map at the start...
		map.setOffset(map.getMarginLeft(), map.getMarginTop());

		// set up the popup menu
		buildPopupMenu(popup);

		front.addClickable(popup);
		front.addClickable(dialogs);

		// initialize upgrade window
		upgradeDialog.build();
		upgradeDialog.setOnCloseClick(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				upgradeDialog.hide();
			}

		});

		upgradeDialog
		.setOnUpgradeSelectedListener(new OnUpgradeSelectedListener() {

			@Override
			public void onUpgradeSelected(Tower t, int selection, int cost) {
				if (player.getGold() >= cost) {

					player.deductGold(cost);
					t.upgrade(selection);

					refreshPopupInfo(t);
				} else {
					showMessage(MESSAGE_INSUFFICIENT_FUNDS);
				}

				upgradeDialog.hide();
			}

		});

		Core.addClickable(map);

		waveDialog.build();

		confirmDialog.build();

		endDialog.build();

		if (gameUpdater.isPaused()) {
			// if the game is paused, then we need to show paused screen
			showPauseScreen();
		}
	}

	private void buildPopupMenu(final PopupMenu popup) {
		popup.onSurfaceCreated();
		popup.clearItems();

		popupUpgradeIndex = popup.addItemWithLabel(R.drawable.popup_ic_upgrade,
				new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				Tower t = (Tower) sender.getTag();
				if (t.hasTypeUpgrade()) {
					upgradeDialog.updateContent(t);
					upgradeDialog.show();
				} else if (!t.isMaxLevel()) {
					if (player.getGold() >= t.getUpgradeCost()) {
						player.deductGold(t.getUpgradeCost());
						t.upgrade();

						refreshPopupInfo(t);
					} else {
						showMessage(MESSAGE_INSUFFICIENT_FUNDS);
					}
				}
			}

		});

		popupSellIndex = popup.addItemWithLabel(R.drawable.popup_ic_sell,
				new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				Tower t = (Tower) sender.getTag();
				player.awardGold(t.getSellPrice());
				map.unregisterObject(t.tileX, t.tileY);
				map.setBlocked(t.tileX, t.tileY, false);
				map.setCanBuildAt(t.tileX, t.tileY, true);

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

	private void showMessage(String string) {
		hudSystem.showMessage(string);
	}

	public void cancelSelection() {
		popup.hide();
		infoDialog.setVisible(false);
		hideTowerRadius();
		hideSecondaryTowerRadius();
	}

	/**
	 * First function called after the constructor... Do initial level setup
	 * here
	 * 
	 * GL is not guaranteed to be available here.
	 * 
	 * @param resId Resource Id of the level file
	 * @param levId A unique Id of the current level
	 */
	public void loadLevel(int resId, int levId) {
		DebugLog.d(TAG, "loadLevel()");
		levelId = levId;
		gameEnded = false;
		Core.currentLevelResId = resId;

		Core.lm.loadLevelFromFile(Core.context, resId);

		mapBuilder.setType(Core.lm.getMapType());
		
		if(Core.lm.isCustomMap()) {
			mapBuilder.setGridSize(Core.lm.getCustomMapH(), Core.lm.getCustomMapW());
		}
		map = mapBuilder.build();
		
		// set up custom map options if this is a custom map
		if (Core.lm.isCustomMap()) {
			List<String> args = Core.lm.getCustomMapArgs();
			int pathCount = Core.lm.getPathCount();
			List<Node> startNodes = new ArrayList<Node>(pathCount);
			List<Node> endNodes = new ArrayList<Node>(pathCount);

			for(int i = 0; i < pathCount; i++) {
				Node n1 = new Node(0, 0);
				Node n2 = new Node(0, 0);

				startNodes.add(n1);
				endNodes.add(n2);
			}

			int tileX = 0;
			int tileY = 0;
			for (String l : args) {
				tileX = 0;
				for (int i = 0; i < l.length(); i++) {
					char c = l.charAt(i);

					if (c == '#') {
						map.setCanBuildAt(tileX, tileY, false);
					} else if (c == '*') {
						map.setBlocked(tileX, tileY, true);
						map.setPermablocked(tileX, tileY, true);
					} else if (c == '@') {
						map.setCanBuildAt(tileX, tileY, false);
						map.setBlocked(tileX, tileY, true);
						map.setPermablocked(tileX, tileY, true);
					} else if (c >= 'a' && c <= 'z') {
						// lower case characters define entry points
						Node n = startNodes.get(c - 'a');
						n.x = tileX;
						n.y = tileY;
					} else if (c >= 'A' && c <= 'Z') {
						// upper case characters define exit points
						Node n = endNodes.get(c - 'A');
						n.x = tileX;
						n.y = tileY;
					} else if (c >= '0' && c <= '9') {
						List<Character> list = Core.lm.getCustomMapSymbolMap().get(c);
						for(char ch : list) {
							if (ch >= 'a' && ch <= 'z') {
								// lower case characters define entry points
								Node n = startNodes.get(ch - 'a');
								n.x = tileX;
								n.y = tileY;
							} else if (ch >= 'A' && ch <= 'Z') {
								// upper case characters define exit points
								Node n = endNodes.get(ch - 'A');
								n.x = tileX;
								n.y = tileY;
							}
						}
					}

					tileX++;
				}
				tileY++;
			}

			if(pathCount != 0)
				map.setStartEndNodes(startNodes, endNodes);
		}

		map.initPathFinder();
		map.findPath();

		spriteMgr.setCapacity(Core.lm.getSpriteCount());

		Core.player.setLives(Core.lm.getInitLives());
		Core.player.setGold(Core.lm.getInitGold());

		hudSystem.prepareForLevel();

		backgroundElements.clear();
		towerManager.clear();
		spriteMgr.clear();
		bulletMgr.clear();
		hud.clear();

		drawables[0] = new Drawable() {

			@Override
			public void draw(float offX, float offY) {
				Core.forceVisible = false;

				final float zoom = Core.zoom;
				Utils.scale(zoom, Core.mixedMatrix);
				GLES20.glUniformMatrix4fv(Core.U_MIXED_MATRIX_HANDLE, 1, false,
						Core.mixedMatrix, 0);
				Utils.scale(1f / zoom, Core.mixedMatrix);

				GLES20.glScissor(clipL, clipT, clipW, clipH);
				GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
			}

			@Override
			public void refresh() { /* do nothing */ }

		};
		drawables[1] = backgroundElements;
		drawables[2] = towerManager;
		drawables[3] = spriteMgr;
		drawables[4] = bulletMgr;
		drawables[5] = new Drawable() {

			@Override
			public void draw(float offX, float offY) {
				GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
			}

			@Override
			public void refresh() { /* do nothing */ }

		};

		drawables[6] = extrasMgr;
		drawables[7] = popup;
		drawables[8] = new Drawable() {

			@Override
			public void draw(float offX, float offY) {
				Core.forceVisible = true;

				GLES20.glUniformMatrix4fv(Core.U_MIXED_MATRIX_HANDLE, 1, false,
						Core.mixedMatrix, 0);
			}

			@Override
			public void refresh() {/* do nothing */}

		};
		drawables[9] = hud;
		
		state = STATE_CLEAN_START;

		Core.gu.addGameUpdatable(Core.lm);
		Core.gu.addGameUpdatable(Core.sm);
	}

	public void onLoadFinished() {
		Core.handler.sendEmptyMessage(MainActivity.MSG_SWITCH_TO_GLSURFACEVIEW);
		if(!Core.gu.isAlive())
			Core.gu.start();
	}

	/**
	 * Do all first time SIZE INDEPENDENT GL initialization here.
	 * 
	 * All screen size dependent GL loading should be done in
	 * {@link #setupScreen()}
	 */
	public void loadGlData() {
		DebugLog.d(TAG, "loadGlData()");

		Core.tm.loadGameTextures();

		initializeBuffers();

		spriteMgr.initialize();

		bulletMgr.growPool(200);

		isGlDataLoaded = true;

		towerManager.loadGlData();

		Sprite.doOnce();
	}
	
	private class ScaleListener extends
	ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float temp = Core.zoom;

			if (!scaling) {
				lg.hide();

				for (Clickable c : Core.clickables) {
					c.onTouchEvent(MotionEvent.ACTION_CANCEL, 0, 0);
				}
			}

			scaling = true;
			float newZoom = Core.zoom * detector.getScaleFactor();

			// Don't let the object get too small or too large.
			setZoom(newZoom);

			float a_x = (detector.getFocusX() / Core.canvasWidth)
					* ((Core.canvasWidth / Core.zoom) - (Core.canvasWidth / temp));
			float a_y = (detector.getFocusY() / Core.canvasHeight)
					* ((Core.canvasHeight / Core.zoom) - (Core.canvasHeight / temp));
			Core.offX += a_x;
			Core.offY += a_y;

			onScrolled();

			return true;
		}
	}

	private int activePointerId = -1;	// variable used to keep track of the current active pointer
	private boolean scaling = false;	// flag used to indicate whether scaling is in effect...

	public void onTouchEvent(MotionEvent event) {
		if (state == STATE_CLEAN_START)
			return;

		if (gestureDetector.onTouchEvent(event))
			return;

		if(zoomEnabled)
			scaleDetector.onTouchEvent(event);
		
		// Uncomment this line to cause single tap to auto complete level
		// onGameWon();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			activePointerId = event.getPointerId(0);

			scaling = false;

			Core.originalTouchX = event.getX();
			Core.originalTouchY = event.getY();
			final float eX = event.getX() / Core.zoom;
			final float eY = event.getY() / Core.zoom;

			lg.setXY(Core.originalTouchX, Core.originalTouchY);
			lg.show();

			for (Clickable c : Core.clickables) {
				if (c.onTouchEvent(event.getAction(), eX, eY))
					break;
			}
			break;
		}

		case MotionEvent.ACTION_MOVE: {
			if (!scaleDetector.isInProgress() && activePointerId != -1) {
				final int pointerIndex = event
						.findPointerIndex(activePointerId);
				if (pointerIndex == -1) {
					activePointerId = -1;
					return;
				}
				Core.originalTouchX = event.getX(pointerIndex);
				Core.originalTouchY = event.getY(pointerIndex);
				final float eX = event.getX(pointerIndex) / Core.zoom;
				final float eY = event.getY(pointerIndex) / Core.zoom;

				lg.setXY(Core.originalTouchX, Core.originalTouchY);

				for (Clickable c : Core.clickables) {
					if (c.onTouchEvent(event.getAction(), eX, eY))
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
				final int pointerIndex = event
						.findPointerIndex(activePointerId);
				Core.originalTouchX = event.getX(pointerIndex);
				Core.originalTouchY = event.getY(pointerIndex);
				final float eX = event.getX(pointerIndex) / Core.zoom;
				final float eY = event.getY(pointerIndex) / Core.zoom;

				activePointerId = -1;

				lg.hide();

				for (Clickable c : Core.clickables) {
					if (c.onTouchEvent(event.getAction(), eX, eY))
						break;
				}
			}

			break;
		}

		case MotionEvent.ACTION_POINTER_UP: {
			final int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
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
		//map.drawPath();
		if (map.isInBounds(tileX, tileY)) {
			if (map.isBlocked(tileX, tileY) && !map.canBuildAt(tileX, tileY)) {
				Object o = map.getObject(tileX, tileY);
				if (o instanceof Tower) {
					Tower tower = (Tower) o;

					refreshPopupInfo(tower);

					popup.popup(tower.x, tower.y);
					infoDialog.setInfo(tower);
					infoDialog.setVisible(true);
				}
			} else if (!map.canBuildAt(tileX, tileY)) {
				showMessage(MESSAGE_INVALID_BUILD_LOCATION);
			} else if (t != null && Core.player.getGold() < t.getCost()) {
				showMessage(MESSAGE_INSUFFICIENT_FUNDS);
			} else if (t != null) {
				final int tx = (int) ((tileX + 0.5f) * map.getTileW());
				final int ty = (int) ((tileY + 0.5f) * map.getTileH());

				synchronized (spriteMgr) {
					List<Sprite> list = spriteMgr.getRawList();
					final int len = spriteMgr.size();
					for (int i = 0; i < len; i++) {
						Sprite s = list.get(i);
						if (s.isObstacle() && s.rect.contains(tx, ty)) {
							// there are sprites where this tower is going to be
							// placed!
							return;
						}
					}

					map.setBlocked(tileX, tileY, true);

					if (map.findPath()) {
						map.setCanBuildAt(tileX, tileY, false);
						// cancel any previous selections
						cancelSelection();

						// the tower placement is valid.
						// construct the tower and add it to the drawables
						Tower tower = new Tower(tileX, tileY, t.getTowerType(),
								t.getLevel());
						tower.setVBO(Core.GeneralBuffers.map_tile);
						tower.x = tx;
						tower.y = ty;

						towerManager.addDrawable(tower);
						Core.gu.addGameUpdatable(tower);

						map.registerObject(tileX, tileY, tower);

						Core.player.deductGold(t.getCost());

						for (int i = 0; i < len; i++) {
							list.get(i).updatePath();
						}
					} else {
						// if there exists no path from the spawn point to the
						// end
						// then the tower placement is invalid...

						// thus revert map and cancel the tower placement
						map.setBlocked(tileX, tileY, false);

						showMessage(MESSAGE_BLOCKING);
					}
				}
			}
		}
	}

	public void placeTower(int tileX, int tileY, DataInputStream stream)
			throws IOException {
		map.setBlocked(tileX, tileY, true);
		map.setCanBuildAt(tileX, tileY, false);

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
		if (tower.hasTypeUpgrade()) {
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
	
	public void showSecondaryTowerRadius(Tower tower, float x, float y) {
		if(tower == null) return;
		secondaryTowerRadius.update(x, y, tower.getRange());
		secondaryTowerRadius.visible = true;
	}

	public void hideSecondaryTowerRadius() {
		secondaryTowerRadius.visible = false;
	}
	
	public void showTowerRadius(Tower tower) {
		showTowerRadius(tower, tower.x, tower.y);
	}

	public void showTowerRadius(Tower tower, float x, float y) {
		if (tower == null)
			return;
		
		towerRadius.update(x, y, tower.getRange());
		towerRadius.visible = true;
	}

	public void hideTowerRadius() {
		towerRadius.visible = false;
	}

	public void reportFinishedSprite(Sprite enemySprite) {
		Core.player.decrementLives();
		spriteMgr.removeDrawableStrict(enemySprite);

		checkWaveClear();
	}

	public void reportKilledSprite(Sprite enemySprite) {
		player.incrementKill();
		player.awardGold(enemySprite.getGoldReward());
		if (enemySprite.hasDeathAnimation()) {
			enemySprite.playDeathAnimation();
			// the sprite will take care of the checking for
			// win condition and removal stuff...
		} else {
			spriteMgr.removeDrawableStrict(enemySprite);
			checkWaveClear();
		}
	}
	
	public void checkWaveClear() {
		if(spriteMgr.size() == 0 && Core.sm.eventsRunning() == 0) {
			// we are done this wave/set of waves!
			
			// check if the player beat the level...
			checkWinCondition();
			
			// if the player did indeed beat the level then gameEnded would be set
			// let's check that
			if(!gameEnded) {
				// game is not over... player did not beat the level yet
				// let's save the player time by increasing sleep time!
				Core.lm.speedUpSleep();
			}
		}
	}

	public void addEnemy(Sprite s) {
		if (s.needToLoad())
			s.load(map);
		Core.gu.addGameUpdatable(s);
	}

	public Bullet obtainBullet() {
		return bulletMgr.obtain();
	}

	public void gameDone() {
		originalGu.stopUpdaterAndWait();
	}

	private boolean prevPauseState;

	public void onPause() {
		if (!gameEnded) {
			// if the game isn't over..
			onPauseClick();

			prevPauseState = Core.gu.isPaused();
			Core.gu.pause();
		}
	}

	public void onResume() {
		if (prevPauseState) {
			// if we were already paused... do nothing
		} else {
			Core.gu.unpause();
		}
	}

	public void beginFirstWave() {
		Core.lm.start();
	}

	public boolean isPaused() {
		return Core.gu.isPaused();
	}

	public void onPauseClick() {
		DebugLog.d(TAG, "onPauseClick");

		if (Core.gu.isPaused())
			return;

		Core.gu.pause();
		showPauseScreen();
	}

	public void showPauseScreen() {
		pauseScreenShown = true;
		hud.addDrawableToTop(pauseMenu);
		front.addClickable(pauseMenu);
	}

	public void onResumeClick() {
		DebugLog.d(TAG, "resuming");
		pauseScreenShown = false;
		hud.removeDrawableStrict(pauseMenu);
		front.removeClickable(pauseMenu);
		Core.gu.unpause();
	}

	private void initializeBuffers() {
		if (Core.GeneralBuffers.map_tile == null) {
			Core.GeneralBuffers.map_tile = new VBO();
			Core.GeneralBuffers.map_half_tile = new VBO();
		}

		float halfW = Core.MAP_SDP / 2;
		{
			final float arr[] = { -halfW, -halfW, // Vertex 0
					halfW, -halfW, // v1
					-halfW, halfW, // v2
					halfW, halfW, // v3
			};
			Core.GeneralBuffers.map_tile.setVBO(Core.MAP_SDP, Core.MAP_SDP,
					BufferUtils.copyToBuffer(arr), VBO.Alignment.CENTER);
		}
		halfW /= 2f;
		{
			final float arr[] = { -halfW, -halfW, // Vertex 0
					halfW, -halfW, // v1
					-halfW, halfW, // v2
					halfW, halfW, // v3
			};
			Core.GeneralBuffers.map_half_tile.setVBO(Core.MAP_SDP / 2f,
					Core.MAP_SDP / 2f, BufferUtils.copyToBuffer(arr),
					VBO.Alignment.CENTER);
		}

		final short[] indices = new short[MAX_TEXTURES * 6]; // Create Temp
		// Index Buffer
		final int len = indices.length; // Get Index Buffer Length
		short j = 0; // Counter
		for (int i = 0; i < len; i += 6, j += 4) { // FOR Each Index Set (Per
			// Sprite)
			indices[i + 0] = (short) (j + 0); // Calculate Index 0
			indices[i + 1] = (short) (j + 1); // Calculate Index 1
			indices[i + 2] = (short) (j + 2); // Calculate Index 2
			indices[i + 3] = (short) (j + 2); // Calculate Index 3
			indices[i + 4] = (short) (j + 3); // Calculate Index 4
			indices[i + 5] = (short) (j + 0); // Calculate Index 5
		}

		Core.indiceHandle = BufferUtils.copyToBuffer(
				GLES20.GL_ELEMENT_ARRAY_BUFFER, indices, len);

		Sprite.refreshResources();
	}

	private static final int MAX_TEXTURES = 300;

	public void refresh() {
		DebugLog.d(TAG, "refresh()");

		initializeBuffers();

		Core.fm.refresh();

		endDialog.refresh();
		confirmDialog.refresh();
		upgradeDialog.refresh();
		waveDialog.refresh();

		for (Drawable d : Core.drawables) {
			if (d != null) {
				d.refresh();
			}
		}
	}

	public void onScrolled() {
		clipL = (int) (Core.offX * Core.zoom);
		clipT = (int) ((map.getHeight() - (Core.offY + Core.canvasHeight)* Core.zoom));	

		map.onScroll();
	}

	/**
	 * Sets the zoom level. If the zoom level set will be normalized between
	 * {@link #MIN_ZOOM} and {@link #MAX_ZOOM}
	 * 
	 * @param zoom
	 *            New zoom level
	 * @return True if the zoom was set. False if no changes were made.
	 */
	public boolean setZoom(float zoom) {
		float newZoom = Math.max(MIN_ZOOM, Math.min(zoom, MAX_ZOOM));

		if (newZoom == Core.zoom)
			return false;
		else {
			Core.zoom = newZoom;
			Core.onZoomChanged();

			clipW = (int) (map.getWidth() * Core.zoom);
			clipH = (int) (map.getHeight() * Core.zoom);
			onScrolled();

			return true;
		}
	}

	public void skipToNext() {
		levelMgr.skipToNextEvent();
	}

	public void onGameOver(boolean hasWon) {
		if (gameEnded)
			return;
		gameEnded = true;
		
		for(int i = 0; i < towerManager.size(); i++) {
			towerManager.get(i).setEnabled(false);
		}

		stateMgr.clearSavedGame();

		showEndScreen(hasWon);
	}

	public boolean isGameOver() {
		return gameEnded;
	}

	private void showEndScreen(boolean success) {
		endDialog.lightSetup(success, player.getSnapshot());
		endDialog.show();
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
		if (!gameEnded && !levelMgr.hasNextEvent()
				&& Core.sm.eventsRunning() == 0 && spriteMgr.size() == 0) {
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

	public void dismissAll() {
		cancelSelection();
		waveDialog.hide();
		hudSystem.hideMenu();
	}

	public void restart() {
		Intent i = new Intent(Core.context, MainActivity.class);
		i.putExtra(MainActivity.BUNDLE_LEVEL, Core.currentLevelResId);
		i.putExtra(MainActivity.BUNDLE_LEVEL_ID, Core.game.getLevelId());
		
		// Stop the updater thread before we finish the activity, to decrease 
		// risk of crashing...
		Core.gu.stopUpdaterAndWait();
		
		Core.finishActivity();
		Core.context.startActivity(i);
		
//		Core.handler.sendEmptyMessage(MainActivity.MSG_SWITCH_TO_LOAD_SCREEN);
//		
//		for (Clickable c : Core.clickables) {
//			c.onTouchEvent(MotionEvent.ACTION_CANCEL, 0, 0);
//		}
//
//		dismissAll();
//		
//		Core.gu.gameDone();
//		Core.gu = new GameUpdater();
//		cleanSlate();
//		loadLevel(Core.currentLevelResId, Core.game.getLevelId());
//		
//		Core.glView.queueEvent(new Runnable() {
//
//			@Override
//			public void run() {
//				try {
//					loadGlData();
//					onSurfaceCreated();
//					onLoadFinished();
//				} catch (Exception e) {
//					// this means that the user pressed the back button
//					// before the level could load... don't do anything...
//					DebugLog.e(TAG, "Exception while restarting level.", e);
//				}
//			}
//
//		});
	}

	/**
	 * Called once a game save file is loaded.
	 */
	public void onSaveFileLoaded() {
		if (Core.lm.isStarted())
			hudSystem.setGameSpeed(WaveControlHud.SPEED_NORMAL);
	}

	public boolean onBackPressed() {
		if(pauseScreenShown) {
			onResumeClick();
			return true;
		}

		if (dialogs.size() != 0) {
			final BaseDialog d = dialogs.get(0);
			dialogs.get(0).dismiss();
			return true;
		}
		return false;
	}

	public void onQuit() {
		DebugLog.d(TAG, "onQuit()");
		
		state = STATE_KILLED;
		Core.gu.clearUiUpdatables();
		Core.gu.clearGameUpdatables();
		
		setOnGameReadyListener(null);
	}

	public void enableZoom() {
		zoomEnabled = true;
	}
	
	public void disableZoom() {
		zoomEnabled = false;
	}

	public void restarted() {
		state = STATE_CLEAN_START;
	}
}
