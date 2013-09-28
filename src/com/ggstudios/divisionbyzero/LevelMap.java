package com.ggstudios.divisionbyzero;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import com.ggstudios.divisionbyzero.Map.Builder;

import android.content.Context;
import android.util.SparseArray;

public class LevelMap {

	private Context context;

	private InputStream inputStream;
	private InputStreamReader inputStreamReader;
	private BufferedReader bufferedReader;

	private SparseArray<LevelNode> arr = new SparseArray<LevelNode>();

	public LevelMap(Context context) {
		this.context = context;
	}

	public void loadFrom(int resId) throws IOException {
		inputStream = context.getResources().openRawResource(resId);
		inputStreamReader = new InputStreamReader(inputStream);
		bufferedReader = new BufferedReader(inputStreamReader);

		String line;
		while((line = getNextMeaningfulLine()) != null) {
			LevelNode n = new LevelNode();
			n.id = Integer.valueOf(line);
			line = getNextMeaningfulLine();
			n.hintX = Integer.valueOf(line);
			line = getNextMeaningfulLine();
			n.hintY = Integer.valueOf(line);
			line = getNextMeaningfulLine();
			n.childCount = Integer.valueOf(line);

			for(int i = 0; i < n.childCount; i++) {
				line = getNextMeaningfulLine();
				n.childIds[i] = Integer.valueOf(line);
			}

			arr.put(n.id, n);
		}

		// once we are all done with putting in the ids
		// we can then link our tree together
		// beginning with the first node...

		LinkedList<LevelNode> toLink = new LinkedList<LevelNode>();
		LevelNode top = arr.get(0);
		top.depth = 0;
		toLink.add(top);

		while(toLink.size() != 0) {
			LevelNode n = toLink.get(0);
			n.status = LevelNode.STATUS_CLOSED;
			for(int i = 0; i < n.childCount; i++) {
				final int childId = n.childIds[i];
				final LevelNode node = arr.get(childId);
				node.depth = n.depth + 1;
				n.children.add(node);
				toLink.add(node);
			}
			toLink.removeFirst();
		}

		arr.get(0).status = LevelNode.STATUS_OPEN;
	}

	public void setCompleted(int levelId) {
		LevelNode ln = arr.get(levelId);
		ln.status = LevelNode.STATUS_COMPLETED;
		for(LevelNode n : ln.children) {
			if(n.status == LevelNode.STATUS_CLOSED) {
				n.status = LevelNode.STATUS_OPEN;
			}
		}
	}

	private String getNextMeaningfulLine() throws IOException {
		String line;

		while((line = bufferedReader.readLine()) != null) {

			line = line.trim();
			if(line.length() == 0 || line.charAt(0) == '/') {
				// skip all empty/commented lines
				continue;
			}

			return line;
		}

		return null;
	}

	public SparseArray<LevelNode> getRaw() {
		return arr;
	}

	public LevelNode getNode(int id) {
		return arr.get(id);
	}

	public LevelNode getTopNode() {
		return arr.get(0);
	}

	public static class ExtraLevelInfo {
		String levelName;
		int waveCount;
		int difficulty;
		int mapW, mapH;

		private static final int MAX_TILES_SMALL = 122;
		private static final int MAX_TILES_MEDIUM = 200;		

		private static final int 
		DIFFICULTY_UNRATED = 0,
		DIFFICULTY_EASY = 1,
		DIFFICULTY_MEDIUM = 2,
		DIFFICULTY_HARD = 3,
		DIFFICULTY_BRUTAL = 4,
		DIFFICULTY_EX = 5;

		String getMapSize() {
			int totalTiles = mapW * mapH;
			if(totalTiles < MAX_TILES_SMALL) {
				return "Small";
			} else if(totalTiles > MAX_TILES_MEDIUM) {
				return "Large";
			} else {
				return "Medium";
			}
		}

		public String getDifficulty() {
			switch(difficulty) {
			case DIFFICULTY_UNRATED:
				return "Unrated";
			case DIFFICULTY_EASY:
				return "Easy";
			case DIFFICULTY_MEDIUM:
				return "Medium";
			case DIFFICULTY_HARD:
				return "Hard";
			case DIFFICULTY_BRUTAL:
				return "Brutal";
			case DIFFICULTY_EX:
				return "EX";
			default:
				return "UNDEFINED";
			}
		}
	}

	public static class LevelNode {
		public static final int 
		STATUS_CLOSED = 0, 
		STATUS_OPEN = 1,
		STATUS_COMPLETED = 2;

		public int depth;

		public int status;

		public int id;
		public int childCount;
		public int[] childIds = new int[3];

		public int hintX, hintY;

		public Object view;

		private boolean loaded = false;

		private ExtraLevelInfo levelInfo;

		public List<LevelNode> children = new LinkedList<LevelNode>();

		public int getHeight() {
			if(childCount == 0)
				return 0;
			else {
				int max = 0;
				for(LevelNode n : children) {
					max = Math.max(max, n.getHeight());
				}

				return max + 1;
			}
		}

		public boolean isLoaded() {
			return loaded;
		}

		public void loadLevelData(Context context) {
			LevelManager lm = new LevelManager();
			int id = getLevelResId(context);
			if(id != 0) {
				lm.loadLevelFromFile(context, id);

				levelInfo = new ExtraLevelInfo();
				levelInfo.difficulty = lm.getLevelDifficulty();
				levelInfo.levelName = lm.getLevelName();
				Map.Builder builder = new Map.Builder();
				builder.setType(lm.getMapType());
				levelInfo.mapW = builder.getTileW();
				levelInfo.mapH = builder.getTileH();
				levelInfo.waveCount = lm.getWaveCount();
			}
		}

		public int getLevelResId(Context context) {
			return context.getResources().getIdentifier( "lv_" + id , "raw" , context.getPackageName());
		}

		public ExtraLevelInfo getExtraInfo() {
			return levelInfo;
		}
	}
}
