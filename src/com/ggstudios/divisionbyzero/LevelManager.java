package com.ggstudios.divisionbyzero;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.ggstudios.utils.DebugLog;

import android.content.Context;

public class LevelManager implements Updatable{
	private final String TAG = "LevelManager";

	/**	
	 * Outlined here is a list of special commands...
	 *
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

	static class SpawnEvent {
		int index;
		int numUnits;
		float spawnRate;	// in seconds...
		
		float approxTime;
		float elapsed = 0f;

		ArrayList<Sprite> sprites = new ArrayList<Sprite>();

		public int enemyType;
	}

	private List<Event> events = new ArrayList<Event>();
	private int mapType;
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
	
	public LevelManager(){}

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

		try {
			while ((nextLine = bufferedReader.readLine()) != null)
			{
				if(nextLine.length() == 0 || nextLine.charAt(0) == '/'){
					// it's a comment... skip...
					continue;
				}

				char type = nextLine.charAt(0);

				nextLine = nextLine.substring(1);	// remove the first character
				nextLine = nextLine.trim();			// trim spaces

				Event event = new Event();

				switch(type) {
				case TYPE_SPAWN:
					waveCount++;
					
					SpawnEvent se = new SpawnEvent();
					lastSpawnEvent = se;
					
					// this is how spawns will be set...
					// first s denotes a spawn command followed by a space

					// all subsequent arguments will be separated by a single space
					temp = nextLine.split(" ");

					// the first argument is the image number/sprite type 
					// (refer to sprite sheet for more info)
					int unit = Integer.parseInt(temp[0]);

					// the second argument is the number of sprite to spawn
					int numUnits = Integer.parseInt(temp[1]);

					// the third number is the spawn rate (the higher the number the slower the spawn rate)
					int spawnrate = Integer.parseInt(temp[2]);

					// the fifth value is the HP of the sprite
					int hp = Integer.parseInt(temp[3]);

					// gold reward
					int gold = Integer.parseInt(temp[4]);

					se.numUnits = numUnits;
					se.spawnRate = spawnrate / 1000f;
					se.index = events.size();
					se.enemyType = unit;
					
					se.approxTime = se.numUnits * se.spawnRate;
					
					Sprite tmp;
					int enemy = unit;

					while(numUnits > 0){
						tmp = Sprite.getPreloadedInstance(0, 0, hp, gold, enemy);

						se.sprites.add(tmp);
						numUnits--;
					}

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
						lastSpawnEvent.approxTime += sleepTime;
					}
					
 					events.add(event);
					break;
				case TYPE_MESSAGE:
					event.cmd = Command.MESSAGE;
					event.args = nextLine;

					events.add(event);
					break;
				case TYPE_MAP:
					mapType = Integer.parseInt(nextLine);
					break;
				case TYPE_SPECIAL:
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

	public int getMapType() {
		return mapType;
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
		DebugLog.d(TAG, "Getting next event: " + curEventIndex);
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

	@Override
	public boolean update(float dt) {
		if(paused)
			return true;
		if(sleepTime > 0){
			if(lastSpawnEvent != null) {
				lastSpawnEvent.elapsed += dt;
			}
			sleepTime -= dt;
			return true;
		}
		
		if(!waitingForEventToFinish) {
			if(hasNextEvent()) {
				Event e = getNextEvent();
				switch(e.cmd) {
				case SPAWN_UNIT:
					curWave++;
					
					DebugLog.d(TAG, "Spawning more enemies...");
					
					SpawnEvent se = (SpawnEvent) e.args;
					lastSpawnEvent = se;
					Core.sm.queueSpawn(se);

					waitingForEventToFinish = true;
					break;
				case SLEEP:
					sleepTime = ((Float) e.args);
					DebugLog.d(TAG, "Sleeping for: " + sleepTime);
					break;
				case START:
					DebugLog.d(TAG, "Start event!");
					break;
				case MESSAGE:
					DebugLog.d(TAG, "Message event!");
					break;
				case SPECIAL:
					DebugLog.d(TAG, "Special event!");
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
	
	public int getCurrentWave() {
		return curWave;
	}
	
	public void skipToNextSpawn() {
		if(hasNextEvent()) {
			Event e = getNextEvent();
			while(e.cmd != Command.SPAWN_UNIT && hasNextEvent()) {
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
		stream.writeInt(lastSpawnEvent.index);
		stream.writeFloat(lastSpawnEvent.elapsed);
	}
	
	public void load(DataInputStream stream) throws IOException {
		curEventIndex = stream.readInt();
		waitingForEventToFinish = stream.readBoolean();
		sleepTime = stream.readFloat();
		paused = stream.readBoolean();
		curWave = stream.readInt();
		lastSpawnEvent = (SpawnEvent) events.get(stream.readInt()).args;
		lastSpawnEvent.elapsed = stream.readFloat();
	}

	public int getLevelDifficulty() {
		return difficulty;
	}

	public String getLevelName() {
		return levelName;
	}
}
