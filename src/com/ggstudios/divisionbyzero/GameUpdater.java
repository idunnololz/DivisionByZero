package com.ggstudios.divisionbyzero;

import com.ggstudios.utils.DebugLog;

import android.os.SystemClock;

public class GameUpdater extends Thread {
	private static final String TAG = "GameUpdater";

	private Object pauseLock = new Object();
	private boolean paused = false;

	private long lastTime;

	private UpdatableCollection gameUpdatables = new UpdatableCollection();
	private UpdatableCollection miscUpdatables = new UpdatableCollection();
	private boolean inGame = true;
	
	private float timeMultiplier = 1.0f;
	// min. time elapsed since an update and max...
	private int minUpdate = 12, maxUpdate = 16;

	private boolean cinematicPlaying = false;
	private Updatable cinematic = null;
	
	private Object stopLock = new Object();
	
	public GameUpdater() { }

	public void pause() {
		DebugLog.d(TAG, "pause");
		synchronized(pauseLock){
			paused = true;
		}
	}

	public void unpause() {
		DebugLog.d(TAG, "unpause");
		synchronized(pauseLock){
			paused = false;
			pauseLock.notifyAll();
		}
	}

	public void addGameUpdatable(Updatable item) {
		gameUpdatables.add(item);
	}
	
	public void clearGameUpdatables() {
		gameUpdatables.clear();
	}
	
	public void removeGameUpdatable(Updatable item) {
		gameUpdatables.remove(item);
	}
	
	public void addUiUpdatable(Updatable item) {
		miscUpdatables.add(item);
	}
	
	public void clearUiUpdatables() {
		miscUpdatables.clear();
	}
	
	public void removeUiUpdatable(Updatable item) {
		miscUpdatables.remove(item);
	}
	
	public boolean hasUiUpdatable(Updatable item) {
		return miscUpdatables.find(item) != -1;
	}
	
	/**
	 * Sets up the current state of the game updater to
	 * play a cinematic. That is, no other updatable will
	 * be processed until the cinematic updatable has completed.
	 * @param cinematicUpdatable Updatable to be playing for the cinematic
	 */
	public void startCinematic(Updatable cinematicUpdatable) {
		cinematicPlaying = true;
		cinematic = cinematicUpdatable;
	}
	
	public void stopCinematic(){
		cinematic = null;
		cinematicPlaying = false;
	}
	
	@Override
	public void start(){
		super.start();
		DebugLog.d(TAG, "Started");
	}
	
	public void setTimeMultiplier(float multiplier) {
		timeMultiplier = multiplier;
		minUpdate = (int) (12/(timeMultiplier * 1.0f));
		maxUpdate = (int) (16/(timeMultiplier * 1.0f));
	}
	
	public float getTimeMultiplier() {
		return timeMultiplier;
	}

	@Override
	public void run() {
		lastTime = SystemClock.uptimeMillis();
		while (inGame) {			
			final long time = SystemClock.uptimeMillis();
            final long timeDelta = time - lastTime;
            long finalDelta = timeDelta;
            if (timeDelta > minUpdate) {
                float secondsDelta = (time - lastTime) * 0.001f;
                if (secondsDelta > 0.1f) {
                	// if the update time is more than 0.1f
                	// then force 0.1f as update time to avoid collision problems
                    secondsDelta = 0.1f;
                }
                lastTime = time;
                
                if(cinematicPlaying) {
                	cinematic.update(secondsDelta);
                } else {
                	gameUpdatables.update(secondsDelta * timeMultiplier);
                	miscUpdatables.update(secondsDelta);
                }
            }
            
            if (finalDelta < maxUpdate) {
                try {
                    Thread.sleep(maxUpdate - finalDelta);
                } catch (InterruptedException e) {/* do nothing */}
            }

			synchronized(pauseLock){
				if(paused){
					while (paused) {
						try {
							pauseLock.wait();
						} catch (InterruptedException e) {/* do nothing */}
					}
				}
			}
		}
		
		synchronized(stopLock) {
			DebugLog.d(TAG, "GameUpdate has stopped");
			stopLock.notify();
		}
	}

	public void stopUpdaterAndWait() {
		synchronized(stopLock) {
			// the thread already stopped... 
			// just return instead of waiting forever...
			if(!isRunning()) return;
			
			inGame = false;
			synchronized(pauseLock){
				paused = false;
				pauseLock.notify();
			}
			try {
				stopLock.wait();
			} catch (InterruptedException e) {/*do nothing*/}
		}
	}
	
	public boolean isRunning() {
		synchronized(stopLock) {
			return inGame;
		}
	}

	public boolean isPaused() {
		return paused;
	}
}
