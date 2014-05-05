package com.ggstudios.divisionbyzero;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import com.ggstudios.divisionbyzero.LevelManager.SpawnEvent;
import com.ggstudios.utils.DebugLog;

/**
 * Handles the spawning of units.
 * @author iDunnololz
 *
 */
public class SpawnManager implements Updatable{
	private static final String TAG = "SpawnManager";
	
	private static class SpawnTimer {
		float spawnRate;
		int unitsLeft;
		float waitTime;
		
		SpawnEvent se;
	}
	
	// defines the max number of spawn events that can be run concurrently...
	private static final int MAX_CONCURRENT = 30;
	
	private LinkedList<SpawnEvent> spawnQueue = new LinkedList<SpawnEvent>();
	private SpawnTimer[] timers = new SpawnTimer[MAX_CONCURRENT];
	private int eventsRunning = 0;
	
	public SpawnManager() {
		for(int i = 0; i < timers.length; i++)
			timers[i] = new SpawnTimer();
	}
	
	public boolean canProcessAnotherEvent() {
		return eventsRunning == MAX_CONCURRENT;
	}
	
	public int eventsRunning() {
		return eventsRunning;
	}
	
	/**
	 * Attempts to process the event concurrently. If the max concurrent
	 * limit has been reached, then the event will simply be queued.
	 * @param spawnEvent
	 */
	public void queueSpawn(SpawnEvent spawnEvent) {
		if(eventsRunning == MAX_CONCURRENT) {
			DebugLog.d(TAG, "No more concurrent slots available! Event has been queued instead!");
			spawnQueue.add(spawnEvent);
		} else {
			SpawnTimer t = timers[eventsRunning];
			t.spawnRate = spawnEvent.spawnRate;
			t.unitsLeft = spawnEvent.numUnits;
			t.waitTime = 0f;
			t.se = spawnEvent;
			eventsRunning++;
		}
	}
	
	public void save(DataOutputStream stream) throws IOException {
		stream.writeInt(eventsRunning);
		for(int i = 0; i < eventsRunning; i++) {
			SpawnTimer timer = timers[i];
			stream.writeInt(timer.se.index);
			stream.writeInt(timer.unitsLeft);
			stream.writeFloat(timer.spawnRate);
			stream.writeFloat(timer.waitTime);
		}
	}
	
	public void load(DataInputStream stream) throws IOException {
		eventsRunning = stream.readInt();
		for(int i = 0; i < eventsRunning; i++) {
			SpawnTimer timer = timers[i];
			timer.se = ((SpawnEvent) Core.lm.getEventAt(stream.readInt()).args);
			timer.unitsLeft = stream.readInt();
			timer.spawnRate = stream.readFloat();
			timer.waitTime = stream.readFloat();
		}
	}
	
	private void switchToNext() {
		SpawnEvent e = spawnQueue.removeFirst();
		SpawnTimer t = timers[eventsRunning];
		t.spawnRate = e.spawnRate;
		t.unitsLeft = e.numUnits;
		t.se = e;
		t.waitTime = 0f;
		
		eventsRunning++;
	}
	
	@Override
	public boolean update(float dt) {
		if(eventsRunning != 0) {
			for(int i = 0; i < eventsRunning; i++) {
				SpawnTimer timer = timers[i];
				
				timer.waitTime -= dt;
				timer.se.group.groupLeader.elapsed += dt;
				
				if(timer.unitsLeft == 0) {
					// do a swap with the last elem.
					timers[i] = timers[eventsRunning - 1];
					timers[eventsRunning - 1] = timer;
					eventsRunning--;
					i--;
					
					if(spawnQueue.size() != 0) {
						// a spawn slot freed up... if there 
						// are more spawn events, run them!
						switchToNext();
					}
					
					continue;
				} else if(timer.waitTime <= 0) {
					// remember the Sprite here is a partially loaded instance...
					Sprite s = Core.game.spriteMgr.obtain();
					SpawnEvent se = timer.se;
					
					s.setPathIndex(se.pathIndex);
					s.preload(se.hp, se.gold, se.enemyType);
					
					// Game#addEnemy() will load the sprite for us...
					Core.game.addEnemy(s);
					
					timer.unitsLeft--;
					
					timer.waitTime += timer.spawnRate;
				}
			}
			
			if(eventsRunning == 0 && spawnQueue.size() == 0) {
				Core.lm.setCurrentEventDone();
			}
		}
		return true;
	}

	public void reset() {
		eventsRunning = 0;
		spawnQueue.clear();
	}

}
