package com.ggstudios.divisionbyzero;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.content.SharedPreferences;

import com.ggstudios.divisionbyzero.Player.PlayerSnapshot;
import com.ggstudios.utils.DebugLog;

/**
 * This class manages all saving/loading/state management
 * @author iDunnololz
 *
 */
public class StateManager {
	private static final String TAG = "StateManager";
	private static final String USER_PREF_NAME = "usr_pref";
	private static final String PREF_OPENING_SKIPABLE = "openingSkipable";
	
	private static final String SAVE_LEVEL_DATA_NAME = "lev.dat";
	private static final String SAVE_FILE_NAME = "save_1.dat";

	private static StateManager instance;

	private Game game;
	private Context context;
	private Object partialLoadLock = new Object();
	private Object loadWaitLock = new Object();
	private boolean loading;

	private OnSavedStateChanged onSavedStateChanged;
	
	private List<UserLevelData> levData = new ArrayList<UserLevelData>();
	
	private LevelMap levelMap;
	
	private Random random = new Random();
	
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;
	
	private boolean openingSkipable = false;
	
	public static class UserLevelData {
		int id;
		int status;
		
		public void writeToStream(DataOutputStream stream) throws IOException {
			stream.writeInt(id);
			stream.writeInt(status);
		}
		
		public void readFromStream(DataInputStream stream) throws IOException {
			id = stream.readInt();
			status = stream.readInt();
		}
	}

	public static interface OnSavedStateChanged {
		public void onSavedStateChanged();
	}

	public static void initialize(Context context) {
		instance = new StateManager(context);
	}

	public static StateManager getInstance() {
		return instance;
	}

	private StateManager(Context context) { 
		this.context = context; 
		
		levelMap = new LevelMap(context);
		prefs = context.getSharedPreferences(USER_PREF_NAME, 0);
		editor = prefs.edit();
	}
	
	public LevelMap getLevelMap() {
		return levelMap;
	}

	public void saveLevelData() {
		FileOutputStream outputStream;

		try {
			outputStream = context.openFileOutput(SAVE_LEVEL_DATA_NAME, Context.MODE_PRIVATE);
			saveLevelData(new DataOutputStream(outputStream));

			outputStream.close();
		} catch (Exception e) {
			DebugLog.e(TAG, e);
		}
	}
	
	public void loadLevelData() {
		FileInputStream inputStream;

		try {
			inputStream = context.openFileInput(SAVE_LEVEL_DATA_NAME);
			loadLevelData(new DataInputStream(inputStream));

			inputStream.close();
		} catch (Exception e) {
			DebugLog.e(TAG, e);
		}
	}
	
	public List<UserLevelData> getUserLevelData() {
		return levData;
	}

	public void saveGame() {
		DebugLog.d(TAG, "saving game...");
		FileOutputStream outputStream;

		try {
			outputStream = context.openFileOutput(SAVE_FILE_NAME, Context.MODE_PRIVATE);
			save(outputStream);

			outputStream.close();
		} catch (Exception e) {
			DebugLog.e(TAG, e);
		}

		if(onSavedStateChanged != null)
			onSavedStateChanged.onSavedStateChanged();
		
		DebugLog.d(TAG, "save complete!");
	}

	public boolean isSavedGame() {
		try {
			context.openFileInput(SAVE_FILE_NAME);
			return true;
		} catch (FileNotFoundException e) {
			return false;
		}
	}

	public void clearSavedGame() {
		context.deleteFile(SAVE_FILE_NAME);

		if(onSavedStateChanged != null)
			onSavedStateChanged.onSavedStateChanged();
	}
	
	public void clearLevelData() {
		context.deleteFile(SAVE_LEVEL_DATA_NAME);
	}

	public void loadGame() {
		FileInputStream inputStream;

		try {
			inputStream = context.openFileInput(SAVE_FILE_NAME);
			load(inputStream);

			inputStream.close();
		} catch (Exception e) {
			DebugLog.e(TAG, e);
		}
	}

	/**
	 * Saves a snapshot of the current game... This function will block.
	 * This function requires that the game state stays constant...
	 * @param bw
	 * @throws IOException
	 */
	public void save(OutputStream bw) throws IOException {
		game = Core.game;

		DataOutputStream stream = new DataOutputStream(bw);

		// we need to dump all meaningful information
		// such that we can recreate the current situation perfectly.

		// save level id
		stream.writeInt(game.getLevelResourceId());
		stream.writeInt(game.getLevelId());

		// save player information
		PlayerSnapshot snapshot = Core.player.getSnapshot();
		stream.writeInt(snapshot.gold);
		stream.writeInt(snapshot.kills);
		stream.writeInt(snapshot.lives);
		stream.writeInt(snapshot.moneyEarned);

		{
			// save tower information
			TowerManager tm = game.towerManager;
			List<Tower> towers = tm.getRawList();
			final int len = tm.size();
			stream.writeInt(len);
			for(int i = 0; i < len; i++) {
				Tower t = towers.get(i);
				stream.writeInt(t.tileX);
				stream.writeInt(t.tileY);
				t.writeToStream(stream);
			}
		}

		Core.lm.save(stream);
		Core.sm.save(stream);
		
		game.spriteMgr.save(stream);
		game.bulletMgr.writeToStream(stream);

		stream.flush();
	}

	public void load(InputStream is) throws IOException {
		game = Core.game;

		synchronized(partialLoadLock) {
			loading = true;

			DataInputStream stream = new DataInputStream(is);

			// reload the level using the saved data...
			// the order of these loads depend on the order in which the data was saved
			// see save()...
			
			// load level id
			int levelResId = stream.readInt();
			int levelId = stream.readInt();
			Core.game.loadLevel(levelResId, levelId);

			try {
				partialLoadLock.wait();
			} catch (InterruptedException e) {/* do nothing */}

			// load player information
			PlayerSnapshot snapshot = new PlayerSnapshot();
			snapshot.gold = stream.readInt();
			snapshot.kills = stream.readInt();
			snapshot.lives = stream.readInt();
			snapshot.moneyEarned = stream.readInt();

			game.player.loadFromSnapshot(snapshot);

			{
				// load tower information
				int len = stream.readInt();
				for(int i = 0; i < len; i++) {
					int tileX = stream.readInt();
					int tileY = stream.readInt();
					game.placeTower(tileX, tileY, stream);
				}
			}

			Core.lm.load(stream);
			Core.sm.load(stream);
			
			game.spriteMgr.load(stream);

			// force a refresh of the path...
			game.map.forceFullFindPath();

			game.bulletMgr.readFromStream(stream);

			synchronized(loadWaitLock) {
				loading = false;
				
				loadWaitLock.notify();
			}
		}
	}

	public boolean isLoading() {
		return loading;
	}

	public void continueLoadingSaveFile() {
		synchronized(partialLoadLock) {
			partialLoadLock.notify();
		}
	}

	public void waitForLoadToFinish() {
		synchronized(loadWaitLock) {
			if(!loading) return;
			try {
				loadWaitLock.wait();
			} catch (InterruptedException e) {/* do nothing */}
		}
	}

	public void setOnSavedStateChanged(OnSavedStateChanged onSavedStateChanged) {
		this.onSavedStateChanged = onSavedStateChanged;
	}
	
	private void saveLevelData(DataOutputStream stream) throws IOException {
		final int len = levData.size();
		stream.writeInt(len);
		for(UserLevelData dat : levData) {
			dat.writeToStream(stream);
		}
	}
	
	private void loadLevelData(DataInputStream stream) throws IOException {
		final int len = stream.readInt();
		for(int i = 0; i < len; i++) {
			UserLevelData dat = new UserLevelData();
			dat.readFromStream(stream);
			levData.add(dat);
		}
	}
	
	public Random getRandom() {
		return random;
	}
	
	public void loadUserInfo() {
		setOpeningSkipable(prefs.getBoolean(PREF_OPENING_SKIPABLE, false));
	}
	
	public void saveUserInfo() {
		editor.putBoolean(PREF_OPENING_SKIPABLE, openingSkipable);
		editor.commit();
	}

	public boolean isOpeningSkipable() {
		return openingSkipable;
	}

	public void setOpeningSkipable(boolean openingSkipable) {
		this.openingSkipable = openingSkipable;
	}

	public void clearUserData() {
		prefs.edit().clear().commit();
		loadUserInfo();
	}
}
