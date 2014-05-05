package com.ggstudios.divisionbyzero;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ggstudios.divisionbyzero.Button.OnClickListener;
import com.ggstudios.divisionbyzero.Game.OnGameReadyListener;
import com.ggstudios.divisionbyzero.MessageDialog.OnDismissListener;
import com.ggstudios.utils.DebugLog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class LevelManager implements Updatable{
	private final String TAG = "LevelManager";

	/**	
	 * Outlined here is a list of special commands...
	 */
	enum Command{
		SPAWN_UNIT,
		SLEEP,
		START,
		MESSAGE,
		SPECIAL,
		NULL
	};

	private static final char
	TYPE_INIT_LIVES	= 'l',
	TYPE_INIT_GOLD	= 'g',
	TYPE_SPAWN 		= 's',
	TYPE_SPAWN_BOSS = 'b',
	TYPE_SLEEP 		= 'p',	// pause
	TYPE_MESSAGE 	= 'm',
	TYPE_MAP		= 'a',
	TYPE_SPECIAL	= '!',
	TYPE_DIFFICULTY = '#',
	TYPE_LEVEL_NAME = '$'
	;

	/**
	 * An Event is a class that stores information
	 * about a game event. This game event can vary from
	 * a spawn event to a wait or message event.
	 */
	class Event {
		Command cmd = Command.NULL;

		Object args;
	}
	
	static class Group {
		int groupNumber = 0;
		int memberCount = 0;
		SpawnEvent groupLeader;
	}

	static class SpawnEvent {
		int waveNumber;
		int index;
		int numUnits;
		int pathIndex;		// which starting point to spawn from
		float spawnRate;	// in seconds...
		int hp, gold;

		float approxTime;
		float elapsed = 0f;
		
		Group group;

		public int enemyType;
	}
	
	public static interface WaveChangeListener {
		public void onWaveChange(int currentWave);
	}

	private List<Event> events = new ArrayList<Event>();
	private int mapType;
	private boolean customMap;
	private int customMapW, customMapH;
	private List<String> customMapArgs = new ArrayList<String>();
	private HashMap<Character, List<Character>> symbolMap = new HashMap<Character, List<Character>>();
	private int pathCount = 0;
	private int curEventIndex = 0;
	private boolean waitingForEventToFinish = false;
	private int initGold, initLives;
	private float sleepTime;
	private boolean paused = true;
	private int waveCount;
	private int curWave = 0;
	private SpawnEvent lastSpawnEvent;
	private int difficulty;
	private String levelName;
	private int spriteCount;
	private boolean mock = false;
	private StateManager stateMgr;
	private int totalSpriteGold = 0;
	private float sleepModifier = 1f;

	private WaveChangeListener waveChangeListener = null;
	
	public LevelManager(){
		stateMgr = StateManager.getInstance();
	}

	public void reset(){
		// clear
		sleepTime = 0f;
		curEventIndex = 0;
		curWave = 0;
		waitingForEventToFinish = false;

		if(Core.sm != null)
			Core.sm.reset();

		events.clear();
	}

	public void loadLevelFromFile(final Context context, final int resourceId){
		paused = true;

		waveCount = 0;
		pathCount = 0;

		customMap = false;
		customMapArgs.clear();

		final InputStream inputStream = context.getResources().openRawResource(
				resourceId);
		final InputStreamReader inputStreamReader = new InputStreamReader(
				inputStream);
		final BufferedReader bufferedReader = new BufferedReader(
				inputStreamReader);

		String nextLine;
		//read file

		reset();

		String[] temp;

		spriteCount = 0;
		totalSpriteGold = 0;
		
		Group group = new Group();
		int groupNumber = 0;
		group.groupNumber = groupNumber;
		
		try {
			while ((nextLine = bufferedReader.readLine()) != null)
			{
				if(nextLine.length() == 0 || nextLine.charAt(0) == '/'){
					// it's a comment... skip...
					continue;
				}

				int index = nextLine.indexOf("//");
				if(index != -1) {
					// comment found... remove it!
					nextLine = nextLine.substring(0, index);
				}
				
				char type = nextLine.charAt(0);

				nextLine = nextLine.substring(1);	// remove the first character
				nextLine = nextLine.trim();			// trim spaces

				Event event = new Event();

				switch(type) {
				case TYPE_SPAWN:
				case TYPE_SPAWN_BOSS:
					waveCount++;

					SpawnEvent se = new SpawnEvent();
					lastSpawnEvent = se;
					
					if(group.memberCount == 0) {
						group.groupLeader = se;
					}

					// this is how spawns will be set...
					// first s denotes a spawn command followed by a space

					// all subsequent arguments will be separated by a single space
					temp = nextLine.split("[ \t]+");

					// the first argument is the sprite type 
					// (refer to sprite sheet for more info)
					int unitType = Integer.parseInt(temp[0]);

					// the second argument is the number of sprite to spawn
					int numUnits = Integer.parseInt(temp[1]);

					// the third number is the spawn rate (the higher the number the slower the spawn rate)
					int spawnrate = Integer.parseInt(temp[2]);

					// the fifth value is the HP of the sprite
					int hp = Integer.parseInt(temp[3]);
					
					// gold reward
					int gold = Integer.parseInt(temp[4]);
					
					int pathId = 0;	// default path to the 'a' path...
					if(temp.length > 5) {
						pathId = temp[5].charAt(0) - 'a';
					}
					
					if(type == TYPE_SPAWN_BOSS) {
						unitType += Sprite.TYPE_BOSS;
					}
					
					se.waveNumber = waveCount - 1;
					se.numUnits = numUnits;
					se.spawnRate = spawnrate / 1000f;
					se.index = events.size();
					se.enemyType = unitType;
					se.gold = gold;
					se.hp = hp;
					se.pathIndex = pathId;
					group.memberCount++;
					se.group = group;

					se.approxTime = se.numUnits * se.spawnRate;

					spriteCount += numUnits;
					totalSpriteGold += spriteCount * gold; // update the total gold count...
					
					event.cmd = Command.SPAWN_UNIT;
					event.args = se;

					events.add(event);
					break;
				case TYPE_INIT_LIVES:
					initLives = Integer.parseInt(nextLine);
					break;
				case TYPE_INIT_GOLD:
					initGold = Integer.parseInt(nextLine);
					break;
				case TYPE_SLEEP:
					final float sleepTime = Integer.parseInt(nextLine) / 1000f;
					event.cmd = Command.SLEEP;
					event.args = sleepTime;

					if (lastSpawnEvent != null) {
						lastSpawnEvent.group.groupLeader.approxTime += sleepTime;
					}
					
					group = new Group();
					groupNumber++;
					group.groupNumber = groupNumber;

					events.add(event);
					break;
				case TYPE_MESSAGE:
					event.cmd = Command.MESSAGE;
					event.args = nextLine;

					events.add(event);
					break;
				case TYPE_MAP:
					String[] args = nextLine.trim().split(" ");
					if(args.length == 1)
						mapType = Integer.parseInt(args[0]);
					else {
						mapType = Integer.parseInt(args[0]);
						int w = Integer.parseInt(args[1]);
						int h = Integer.parseInt(args[2]);
						
						customMapW = w;
						customMapH = h;
						
						/*
						 * Note that the specification of paths is optional.
						 * If no path count is given, then that means we should
						 * just use the default path of the map.
						 * 
						 * Because custom path is optional, we detect if this feature
						 * is used by checking the argument for the number of path on
						 * the map is specified. If it is then this means that we
						 * are indeed using custom paths. Else we assume the default path
						 * is used.
						 */
						if(args.length > 3)
							pathCount = Integer.parseInt(args[3]);
						else
							pathCount = 0;

						for(int i = 0; i < h; i++) {
							customMapArgs.add(bufferedReader.readLine());
						}

						if(pathCount != 0) {
							// let's analyze the number of arguments after...
							int extraArgs = Integer.parseInt(bufferedReader.readLine());
							for(int i = 0; i < extraArgs; i++) {
								String line = bufferedReader.readLine();
								String[] tokens = line.split(" ");

								ArrayList<Character> chars = new ArrayList<Character>(tokens.length - 1);

								for(int j = 1; j < tokens.length; j++) {
									chars.add(tokens[j].charAt(0));
								}
								symbolMap.put(tokens[0].charAt(0), chars);
							}
						}

						customMap = true;
					}
					break;
				case TYPE_SPECIAL:
					if(mock) break;
					int arg = Integer.parseInt(nextLine);
					event.cmd = Command.SPECIAL;
					event.args = arg;

					switch(arg) {
					case 0:
						start();
						break;
					case 1:
						Core.game.setOnGameReadyListener(new OnGameReadyListener() {

							@Override
							public void onGameReady() {
								start();
							}
							
						});
						break;
					default:
						break;
					}

					events.add(event);
					break;
				case TYPE_DIFFICULTY:
					difficulty = Integer.parseInt(nextLine);
					break;
				case TYPE_LEVEL_NAME:
					levelName = nextLine.trim();
					break;
				default:
					DebugLog.e(TAG, "Error. Unsupported level command: " + type);
					break;
				}
			}
		} catch (IOException e) {
			DebugLog.e(TAG, e);
		}
	}

	public int getSpriteCount() {
		return spriteCount;
	}

	public int getMapType() {
		return mapType;
	}

	public boolean isCustomMap() {
		return customMap;
	}
	
	public int getCustomMapW() {
		return customMapW;
	}
	
	public int getCustomMapH() {
		return customMapH;
	}

	public List<String> getCustomMapArgs() {
		return customMapArgs;
	}
	
	public HashMap<Character, List<Character>> getCustomMapSymbolMap() {
		return symbolMap;
	}
	
	public int getPathCount() {
		return pathCount;
	}

	public int getInitLives() {
		return initLives;
	}

	public int getInitGold() {
		return initGold;
	}

	public boolean hasNextEvent() {
		return curEventIndex < events.size();
	}

	public void putBackLast() {
		curEventIndex--;
	}

	public Event getNextEvent() {
		if(curEventIndex == events.size())
			return null;
		return events.get(curEventIndex++);
	}

	public List<Event> getRawEvents() {
		return events;
	}

	public Event getEventAt(int index) {
		return events.get(index);
	}

	public int getWaveCount() {
		return waveCount;
	}

	public void setCurrentEventDone() {
		waitingForEventToFinish = false;
	}
	
	public void speedUpSleep() {
		sleepModifier = 5f;
	}
	
	public void resetSleepModifier() {
		sleepModifier = 1f;
	}
	
	public void setOnWaveChangeListener(WaveChangeListener listener) {
		waveChangeListener = listener;
	}

	@Override
	public boolean update(float dt) {
		if(paused)
			return true;
		if(sleepTime > 0){
			dt *= sleepModifier;
			if(lastSpawnEvent != null) {
				lastSpawnEvent.group.groupLeader.elapsed += dt;
			}
			sleepTime -= dt;
			return true;
		}

		if(!waitingForEventToFinish) {
			if(hasNextEvent()) {
				Event event = getNextEvent();
				switch(event.cmd) {
				case SPAWN_UNIT:
					DebugLog.d(TAG, "Spawning more enemies...");

					do {
						curWave++;
						SpawnEvent se = (SpawnEvent) event.args;
						lastSpawnEvent = se;
						Core.sm.queueSpawn(se);
						
						event = getNextEvent();
					} while(event != null && event.cmd == Command.SPAWN_UNIT);
					
					if(event != null)
						putBackLast();
					
					waitingForEventToFinish = true;
					
					Core.hud.showMessage("Wave " + (lastSpawnEvent.group.groupNumber + 1));
					if(waveChangeListener != null) {
						waveChangeListener.onWaveChange(curWave);
					}
					break;
				case SLEEP:
					sleepTime = ((Float) event.args);
					resetSleepModifier();
					DebugLog.d(TAG, "Sleeping for: " + sleepTime);
					break;
				case START:
					DebugLog.d(TAG, "Start event!");
					break;
				case MESSAGE: {
					final MessageDialog d = new MessageDialog();
					d.setMessage((String) event.args);
					d.setOnDismissListener(new OnDismissListener() {

						@Override
						public void onDismiss() {
							setCurrentEventDone();
						}

					});
					Core.glView.queueEvent(new Runnable() {

						@Override
						public void run() {
							d.build();
							d.show();
						}

					});
					waitingForEventToFinish = true;
					break; }
				case SPECIAL:
					DebugLog.d(TAG, "Special event!");
					// special events can do a variety of things...
					int arg = (Integer) event.args;
					switch(arg) {
					case 0:
						Core.glView.queueEvent(new Runnable() {

							@Override
							public void run() {
								boolean skipable = stateMgr.isOpeningSkipable();
								
								// set up the cinematics...
								float w = Core.SDP * 3f;
								float h = w;
								float centerX = (Core.canvasWidth - w) / 2f;
								float centerY = (Core.canvasHeight - h) / 2f;

								Paint p = new Paint();
								p.setTextSize(Core.SDP);
								p.setColor(Color.WHITE);
								p.setAntiAlias(true);
								
								float dim = Core.SDP * 2;
								final Button skip = new Button(Core.canvasWidth - dim, Core.canvasHeight - dim, dim, dim, "Skip", p) {
									@Override
									public void draw(float offX, float offY) {
										super.draw(0, 0);
									}
									
									@Override
									public boolean onTouchEvent(int action, float _x, float _y) {
										final float x = Core.originalTouchX;
										final float y = Core.originalTouchY;
										DebugLog.d(TAG, "x" + x + "y" + y);
										return super.onTouchEvent(action, x, y);
									}
								};
								
								final String[] numerators 	= {"2", "7", "9", "8", "4"};
								final String[] denominators = {"2", "3", "5" , "4", "1"};
								final String[] answers 		= {"1", "2", "1" , "2", "4"};

								final Button bg =  new Button(0, 0, Core.canvasWidth, Core.canvasHeight, -1) {
									@Override
									public boolean onTouchEvent(int action, float x, float y) {
										return true;
									}
								};
								final Rectangle rect = new Rectangle(0, 0, Core.canvasWidth, Core.canvasHeight, 0xFF000000);
								final PictureBox divisionMachine = new PictureBox(centerX, centerY, w, h, R.drawable.division_machine);
								final Label arg1 = new Label(-999, -999, p, "2");
								final Label arg2 = new Label(-999, -999, p, "6");
								final Label result = new Label(-999, -999, p, "3");
								final Label enemy = new Label(-999, -999, p, "0");
								final ExplosionGenerator explosion = new ExplosionGenerator(20);
								explosion.setPosition(divisionMachine.x + divisionMachine.w/2f, divisionMachine.y + divisionMachine.h/2f);
								explosion.setParticleTexture(R.drawable.aoe_blast);
								explosion.setParticleSize(Core.SDP * 2.2f);
								explosion.setSizeVariance(0.4f);
								explosion.setDistanceVariance(Core.SDP * 2.5f);
								explosion.setGenerationChance(0.5f);
								explosion.setInitialParticles(10);
								explosion.setVisible(false);
								explosion.build();
								
								final ParticleEngine sparks = new ParticleEngine(10);
								sparks.setParticleTexture(R.drawable.spark);
								sparks.setPosition(explosion.x, explosion.y);
								sparks.setParticleSize(Core.SDP / 8f, Core.SDP);
								sparks.setMaxVelocity(Core.SDP * 0.3f);
								sparks.setColor(1f, 0.7f, 0.5f);
								sparks.setVisible(false);
								sparks.build();
								
								Paint pp = new Paint();
								pp.setTextSize(Core.SDP_H * 0.8f);
								pp.setColor(Color.BLACK);
								pp.setAntiAlias(true);
								final Button bubble = new Button(-999, -999, Core.SDP * 3f, Core.SDP, R.drawable.speech_bubble, "Muahahahaha!", pp);
								
								final DrawableCollection<Drawable> drawables = new DrawableCollection<Drawable>();
								
								if(skipable) {
									drawables.addDrawable(skip);
								}
								
								drawables.addDrawable(explosion);
								drawables.addDrawable(sparks);
								drawables.addDrawable(divisionMachine);
								drawables.addDrawable(result);
								drawables.addDrawable(enemy);
								drawables.addDrawable(arg1);
								drawables.addDrawable(arg2);
								drawables.addDrawable(bubble);
								drawables.addDrawable(rect);
								
								Core.game.front.addClickable(bg);
								if(skipable) {
									Core.game.front.addClickable(skip);
								}
								
								Core.game.hud.addDrawableToTop(drawables);

								Core.offX = 0f;
								Core.offY = 0f;

								final UpdatableCollection c = new UpdatableCollection();
								
								final Updatable u = new Updatable() {
									final float DURATION = 4f;
									float time = 0f;
									final float START_X = divisionMachine.x + Core.SDP;
									final float START_Y = divisionMachine.y + Core.SDP;

									final float C_X = Core.SDP * 10f;
									final float C_Y = Core.SDP * 10f;
									
									int counter = 0;

									public void reset() {
										time = 0f;
									}

									@Override
									public boolean update(float dt) {
										time += dt;
										if(time < DURATION) {
											float t = time / DURATION;
											result.x = START_X + t * C_X;
											result.y = START_Y + t * C_Y;
											return true;
										} else {
											reset();
											result.setText(answers[counter]);
											counter++;
											if(counter == numerators.length) {
												counter = 0;
											}
											return false;
										}
									}

								};
								
								final Updatable finishUp = new Updatable() {
									final float DURATION = 3f;
									float time = 0f;
									
									@Override
									public boolean update(float dt) {
										time += dt;
										
										if(time < DURATION) {
											divisionMachine.transparency = 1f - time;
											return true;
										} else {
											pause();
											waitingForEventToFinish = false;
											Core.game.front.removeClickable(bg);
											Core.game.front.removeClickable(skip);
											Core.gu.stopCinematic();
											Core.game.hud.removeDrawable(drawables);
											Core.game.enableZoom();
											stateMgr.setOpeningSkipable(true);
											return false;
										}
									}
									
								};
								

								skip.setOnClickListener(new OnClickListener() {

									@Override
									public void onClick(Button sender) {
										DebugLog.d(TAG, "Clicked");
										c.add(finishUp);
										finishUp.update(3f);
									}
									
								});
								
								final Updatable enterMachine = new Updatable() {
									final float DURATION = 1f;
									
									float START_X = enemy.x;
									
									final float C_X = Core.SDP * 1.25f;
									
									float time = 0f;
									
									@Override
									public boolean update(float dt) {
										if(time == 0f) {
											START_X = enemy.x;
										}
										
										time += dt;
										
										if(time < DURATION) {
											float t = time / DURATION;
											enemy.x = START_X + t * C_X;
											return true;
										} else {
											enemy.transparency = 0f;
											arg1.transparency = 0f;
											arg2.transparency = 0f;
											result.transparency = 0f;
											
											c.add(finishUp);
											c.add(explosion);
											c.add(sparks);
											explosion.setVisible(true);
											sparks.setVisible(true);
											return false;
										}
									}
									
								};
								
								final Updatable killMachine = new Updatable() {
									final float DURATION = 1.5f;
									
									float START_X = enemy.x;
									float START_Y = enemy.y;
									
									final float C_X = Core.SDP * 1.25f;
									final float C_Y = Core.SDP * 1.25f;
									
									float time = 0f;
									
									@Override
									public boolean update(float dt) {
										if(time == 0f) {
											START_X = enemy.x;
											START_Y = enemy.y;		
										}
										
										time += dt;
										
										if(time < DURATION) {
											float t = time / DURATION;
											enemy.x = START_X + t * C_X;
											enemy.y = START_Y + t * C_Y;
											return true;
										} else {
											c.add(enterMachine);
											return false;
										}
									}
									
								};
								
								final Updatable wait = new Updatable() {
									final float DURATION = 2f;
									float time = 0f;
									
									@Override
									public boolean update(float dt) {
										time += dt;
										
										if(time >= DURATION) {
											// cause the 0 to proceed to kill the machine...
											// also remove the dialog box...
											bubble.isVisible = false;
											c.add(killMachine);
											return false;
										}
										return true;
									}
									
								};
								
								final Updatable evilEntrance = new Updatable() {
									final float START_X = divisionMachine.x - Core.SDP * 2f;
									final float START_Y = -Core.SDP;
									
									final float C_Y = divisionMachine.y - START_Y;
									
									final float DURATION = 3f;
									float time = 0f;
									@Override
									public boolean update(float dt) {
										time += dt;
										if(time < DURATION) {
											enemy.x = START_X;
											enemy.y = START_Y + time / DURATION * C_Y;
											
											return true;
										} else {
											bubble.x = enemy.x - bubble.w;
											bubble.y = enemy.y - bubble.h;
											c.add(wait);
											return false;
										}
									}
									
								};

								final Updatable inputUpdate = new Updatable() {
									private static final float DURATION = 4f;

									private final float START_X = divisionMachine.x - Core.SDP * 9f;
									private final float START_Y = divisionMachine.y + Core.SDP * 1.25f;

									private final float START_X2 = divisionMachine.x + Core.SDP * 1.25f;
									private final float START_Y2 = divisionMachine.y - Core.SDP * 9f;

									private final float C_X = Core.SDP * 10f;

									private final float C_Y2 = Core.SDP * 10f;

									private float time = 0f;

									int counter = 0;
									
									boolean evilEntered = false;
									
									@Override
									public boolean update(float dt) {
										time += dt;

										if(time < DURATION) {
											float t = time / DURATION;
											arg1.x = C_X * t + START_X;
											arg1.y = START_Y;

											arg2.x = START_X2;
											arg2.y = C_Y2 * t + START_Y2;
											return true;
										} else {
											c.add(u);
											time = 0;
											
											if(counter == 1 && !evilEntered) {
												evilEntered = true;
												c.add(evilEntrance);
											}
											
											arg2.setText(numerators[counter]);
											arg1.setText(denominators[counter]);
											counter++;
											if(counter == numerators.length) {
												counter = 0;
											}
											return true;	
										}
									}

								};
								
								Core.game.disableZoom();
								Core.gu.startCinematic(c);

								c.add(inputUpdate);
							}

						});
						waitingForEventToFinish = true;
						break;
					case 1:
						final float OFF_X = Core.SDP * -20f;
						
						final int MACHINES = 20;
						
						final PictureBox[] divisionMachines = new PictureBox[MACHINES];
						
						Core.game.map.setEnabled(false);
						
						Core.glView.queueEvent(new Runnable() {

							@Override
							public void run() {
								final float size = Core.SDP * 2f;
								final int COLUMN_SIZE = 5;
								final float x = -OFF_X + Core.offX;// - (Core.canvasWidth - size * COLUMN_SIZE) / 2f;
								for(int i = 0; i < divisionMachines.length; i++) {
									divisionMachines[i] = new PictureBox(x + (i/COLUMN_SIZE)*size, (i%COLUMN_SIZE)*size, 
											Core.SDP, Core.SDP, R.drawable.division_machine);
									Core.game.hud.addDrawable(divisionMachines[i]);
								}
							}
							
						});
						
						final Runnable tutPart6 = new Runnable() {
							@Override
							public void run() {
								MessageDialog d = new MessageDialog();
								d.setMessage("Good luck!");
								d.setShowHint(true);
								d.build();
								d.show();
								
								d.setOnDismissListener(new OnDismissListener() {

									@Override
									public void onDismiss() {
										Core.game.map.setEnabled(true);
									}
									
								});
							}							
						};
						
						final Runnable tutPart5 = new Runnable() {

							@Override
							public void run() {
								MessageDialog d = new MessageDialog();
								d.setMessage("Once you have finished building up a defensive line. " +
										"Press play to start the level");
								
								d.setShowHint(true);
								d.build();
								d.setPauseOnShown(false);
								d.show();
								
								RectF r = Core.hud.getRegion(Hud.HUD_WAVE_CONTROL);
								d.x = r.left - d.w;
								d.y = r.bottom - d.h;
								
								r.right = r.left + Core.SDP * 1.5f;
								
								final TargetRectangle rect = new TargetRectangle();
								rect.setBounds(r);
								rect.setColor(0xFFFF0000);
								rect.setStrokeWidth(Core.SDP * 0.2f);
								rect.build();

								Core.game.hud.addDrawableToTop(rect);
								Core.gu.addUiUpdatable(rect);
								
								d.setOnDismissListener(new OnDismissListener() {

									@Override
									public void onDismiss() {
										Core.game.hud.removeDrawable(rect);
										Core.gu.removeUiUpdatable(rect);
										Core.glView.queueEvent(tutPart6);
									}
									
								});
							}
							
						};

						
						final Runnable tutPart4 = new Runnable() {

							@Override
							public void run() {
								int gold = Core.player.getGold();
								Core.hud.setSelection(0);
								Core.game.mapClicked(0, 5);

								Core.game.mapClicked(1, 2);
								Core.game.mapClicked(1, 3);
								Core.game.mapClicked(1, 4);
								Core.hud.setSelection(-1);
								Core.player.setGold(gold);
								
								MessageDialog d = new MessageDialog();
								d.setMessage("An incomplete defensive line has already" +
										" been set up for you.");
								
								d.setShowHint(true);
								d.build();
								d.show();
								
								d.setOnDismissListener(new OnDismissListener() {

									@Override
									public void onDismiss() {
										Core.glView.queueEvent(tutPart5);
									}
									
								});
							}
							
						};

						final Runnable tutPart3 = new Runnable() {

							@Override
							public void run() {
								MessageDialog d = new MessageDialog();
								d.setMessage("The arrows on the left and right" +
										" show where enemies will spawn." +
										" You can place defensive structures" +
										" anywhere within the grid. However you" +
										" may not block.");
								d.setShowHint(true);
								d.build();
								
								d.setOnDismissListener(new OnDismissListener() {

									@Override
									public void onDismiss() {
										Core.glView.queueEvent(tutPart4);
									}
									
								});

								d.x = (Core.canvasWidth - d.w) / 2f;
								d.y = Core.SDP_H;
								
								d.show();
							}
							
						};
						
						final Runnable tutPart2 = new Runnable() {

							@Override
							public void run() {
								MessageDialog d = new MessageDialog();
								d.setMessage("The amount of resources you have as well" +
										" as the number of division machines still running in this" +
										" area is displayed here.");
								d.setShowHint(true);
								d.build();
								d.setPauseOnShown(false);

								final TargetRectangle rect = new TargetRectangle();
								rect.setBounds(Core.hud.getRegion(Hud.HUD_LEFT_PANEL));
								rect.setColor(0xFFFF0000);
								rect.setStrokeWidth(Core.SDP * 0.2f);
								rect.build();

								Core.game.hud.addDrawableToTop(rect);
								Core.gu.addUiUpdatable(rect);
								
								RectF r = Core.hud.getRegion(Hud.HUD_LEFT_PANEL);
								d.x = r.left;
								d.y = r.top - d.h;
								
								d.setOnDismissListener(new OnDismissListener() {

									@Override
									public void onDismiss() {
										Core.game.hud.removeDrawable(rect);
										Core.gu.removeUiUpdatable(rect);
										Core.glView.queueEvent(tutPart3);
									}
									
								});
								
								d.show();
							}
							
						};
						
						final Runnable tutPart1 = new Runnable() {

							@Override
							public void run() {
								MessageDialog d = new MessageDialog();
								d.setMessage("You can purchase defensive structures here." +
										" To buy a tower tap the desired tower" +
										" and tap a square on the field to place the tower there.\n\n" +
										"You can double tap a tower to find out more about it.");
								d.setShowHint(true);
								d.setPauseOnShown(false);
								d.build();
								
								final TargetRectangle rect = new TargetRectangle();
								rect.setBounds(Core.hud.getRegion(Hud.HUD_TOWER_MENU));
								rect.setColor(0xFFFF0000);
								rect.setStrokeWidth(Core.SDP * 0.2f);
								rect.build();

								Core.game.hud.addDrawableToTop(rect);
								Core.gu.addUiUpdatable(rect);
								
								RectF r = Core.hud.getRegion(Hud.HUD_RIGHT_PANEL);
								d.x = r.left;
								d.y = r.top - d.h;
								
								d.setOnDismissListener(new OnDismissListener() {

									@Override
									public void onDismiss() {
										Core.game.hud.removeDrawable(rect);
										Core.gu.removeUiUpdatable(rect);
										Core.glView.queueEvent(tutPart2);
									}
									
								});
								
								d.show();
							}
							
						};
						
						final Updatable scrollBack = new Updatable() {
							final float DURATION = 2f;
							
							final float START_X = Core.offX;
							
							float time = 0f;
							
							@Override
							public boolean update(float dt) {
								time += dt;
								
								if(time < DURATION) {
									float t = time / DURATION;
									Core.game.map.setOffsetX(START_X + t * t * -OFF_X + OFF_X);
									return true;
								} else {
									Core.game.map.setOffsetX(START_X);

									setCurrentEventDone();
									pause();
									
									Core.glView.queueEvent(new Runnable() {

										@Override
										public void run() {
											MessageDialog d = new MessageDialog();
											d.setMessage("A large number of zeros has been seen around the area. " +
													"They appear to be targeting the 20 machines here. " +
													"Fortunately, the zeros will have to pass through this field " +
													"to get to the machines, so we can set up a defensive " +
													"parameter here...");
											d.setShowHint(true);
											d.setOnDismissListener(new OnDismissListener() {
											
												@Override
												public void onDismiss() {
													Core.glView.queueEvent(tutPart1);
												}
												
											});
											d.build();
											d.show();
										}
										
									});
									return false;
								}
							}
							
						};
						
						final Updatable theDialogs = new Updatable() {
							final float DURATION = 3f;
							final float ALTERNATE = 0.5f;
							float time = 0f;
							
							@Override
							public boolean update(float dt) {
								time += dt;
								
								if(time < DURATION) {
									if((int)(time / ALTERNATE) % 2 == 0) {
									for(int i = 0; i < divisionMachines.length; i++) {
										divisionMachines[i].transparency = 0.5f;
									}
									} else {
										for(int i = 0; i < divisionMachines.length; i++) {
											divisionMachines[i].transparency = 1f;
										}
									}
									return true;
								} else {
									for(int i = 0; i < divisionMachines.length; i++) {
										divisionMachines[i].transparency = 1f;
									}
									
									Core.glView.queueEvent(new Runnable() {

										@Override
										public void run() {
											MessageDialog d = new MessageDialog();
											d.setMessage("The zeros appear to be targeting division machines. If any" +
													" zero enters a division machine, the machine will malfunciton and blow" +
													" up so we must stop any zeros from coming near them. " +
													" Your goal is to protect these machines from the zeros...");
											d.setShowHint(true);
											d.setOnDismissListener(new OnDismissListener() {

												@Override
												public void onDismiss() {
													Core.gu.addUiUpdatable(scrollBack);
												}
												
											});
											d.build();
											d.show();
										}
										
									});
									return false;
								}
							}
							
						};
						
						final Updatable scrollTo = new Updatable() {
							final float DURATION = 2f;
							
							final float START_X = Core.offX;
							
							float time = 0f;
							
							@Override
							public boolean update(float dt) {
								time += dt;
								
								if(time < DURATION) {
									float t = time / DURATION;
									Core.game.map.setOffsetX(START_X + t * t * OFF_X);
									return true;
								} else {
									Core.game.map.setOffsetX(OFF_X + START_X);
									Core.gu.addUiUpdatable(theDialogs);
									return false;
								}
							}
							
						};
						
						
						final MessageDialog intro = new MessageDialog();
						intro.setMessage("Recent reports show that a high number of zeros has" +
								" been spotted in the mainframe and they have been linked to" +
								" faliures in critical machines...");
						intro.setShowHint(true);
						intro.setOnDismissListener(new OnDismissListener() {

							@Override
							public void onDismiss() {
								Core.gu.addUiUpdatable(scrollTo);
							}

						});
						
						
						final MessageDialog d = new MessageDialog();
						d.setMessage("Welcome to Division By Zero!");
						d.setShowHint(true);
						d.setOnDismissListener(new OnDismissListener() {

							@Override
							public void onDismiss() {
								Core.glView.queueEvent(new Runnable() {

									@Override
									public void run() {
										intro.build();
										intro.show();
									}

								});
							}

						});
						Core.glView.queueEvent(new Runnable() {

							@Override
							public void run() {
								d.build();
								d.show();
							}

						});
						waitingForEventToFinish = true;
						break;
					default:
						break;
					}
					break;
				default:
					break;
				}
			}
		}
		return true;
	}

	public void start() {
		paused = false;
	}
	
	public void pause() {
		paused = true;
	}

	public boolean isStarted() {
		return !paused;
	}

	public int getCurrentWave() {
		return curWave;
	}

	public void skipToNextEvent() {
		if(hasNextEvent()) {
			Event e = getNextEvent();
			// skip all sleeps
			while(e.cmd == Command.SLEEP && hasNextEvent()) {
				e = getNextEvent();
			}
			putBackLast();
		} else {
			DebugLog.d(TAG, "No more events to process!");
		}

		sleepTime = 0;
		waitingForEventToFinish = false;
	}

	public void save(DataOutputStream stream) throws IOException {
		stream.writeInt(curEventIndex);
		stream.writeBoolean(waitingForEventToFinish);
		stream.writeFloat(sleepTime);
		stream.writeBoolean(paused);
		stream.writeInt(curWave);
		if(lastSpawnEvent == null) {
			stream.writeInt(-1);
		} else {
			stream.writeInt(lastSpawnEvent.index);
			stream.writeFloat(lastSpawnEvent.elapsed);
		}
	}

	public void load(DataInputStream stream) throws IOException {
		curEventIndex = stream.readInt();
		waitingForEventToFinish = stream.readBoolean();
		sleepTime = stream.readFloat();
		paused = stream.readBoolean();
		curWave = stream.readInt();
		int index = stream.readInt();
		if(index == -1) {
			lastSpawnEvent = null;
		} else {
			lastSpawnEvent = (SpawnEvent) events.get(index).args;
			lastSpawnEvent.elapsed = stream.readFloat();
		}
	}

	public int getLevelDifficulty() {
		return difficulty;
	}

	public String getLevelName() {
		return levelName;
	}

	public void setMockMode(boolean b) {
		mock = b;
	}
	
	public float getTotalSpriteGold() {
		return totalSpriteGold;
	}
}
