package com.ggstudios.divisionbyzero;

import java.util.Arrays;

import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.ggstudios.divisionbyzero.DijkstraPathFinder.Node;
import com.ggstudios.utils.DebugLog;

public class Map extends Drawable implements Clickable {
	public static class Builder {
		private static final String TAG = "Map.Builder";

		private int across = Grid.DEFAULT_TILES_ACROSS, 
				down = Grid.DEFAULT_TILES_DOWN;

		private int mapW, mapH;
		private int lineColor = 0x80FFFFFF, lineThickness = 1;
		private int resId = -1;
		private int sX, sY, eX, eY;

		public Builder(){
			mapW = (int) Core.canvasWidth;
			mapH = (int) Core.canvasHeight;

			sX = 0;
			sY = 0;
			eX = across - 1;
			eY = down - 1;
		}

		public Builder setSize(int w, int h) {
			mapW = w;
			mapH = h;
			return this;
		}

		public Builder setGridSize(int rows, int columns) {
			down = rows;
			across = columns;
			return this;
		}

		public Builder setLineColor(int color) {
			lineColor = color;
			return this;
		}

		public Builder setLineThickness(int thickness) {
			lineThickness = thickness;
			return this;
		}

		public Builder setGridStart(int startX, int startY) {
			sX = startX;
			sY = startY;
			return this;
		}

		public Builder setGridEnd(int endX, int endY) {
			eX = endX;
			eY = endY;
			return this;
		}

		public Builder setBackground(int resId) {
			this.resId = resId;
			return this;
		}

		public Builder setType(int type) {
			switch(type) {
			case 0:
				setGridSize(9, 11);
				setGridStart(0, down/2);
				setGridEnd(across - 1, down/2);
				setBackground(R.drawable.map_bg_zone_1);
				setLineColor(0x30FFFFFF);
				break;
			case 1:
				setGridSize(11, 17);
				setGridStart(0, down/2);
				setGridEnd(across - 1, down/2);
				setBackground(R.drawable.map_bg_zone_1);
				setLineColor(0x30FFFFFF);
				break;
			default:
				DebugLog.e(TAG, "Error. Unknown map type.");
				break;
			}
			return this;
		}

		public Map build() {
			return new Map(mapW, mapH, across, down, lineColor, lineThickness,
					resId,
					sX, sY, eX, eY);
		}

		public int getTileW() {
			return mapW;
		}

		public int getTileH() {
			return mapH;
		}
	}

	private Grid grid;
	private int startGridX, startGridY;
	private int endGridX, endGridY;
	private float tileW, tileH;
	private DijkstraPathFinder pathFinder;

	private boolean[][] isBlocked;
	private boolean showPath = false;
	private PictureBox[] path;
	private PictureBox selectionBox;
	private PictureBox entranceMarker, exitMarker;
	private boolean showSelection = false;
	private Object[][] mapObj;

	private Node start, end;
	private Node ghostStart, ghostEnd;

	private float touchSlop;
	private float marginLeft;
	private float marginTop;
	private int across, down;

	// stores the amount to offset touch events to translate to
	// a touch on the map...
	private float touchOffX, touchOffY;

	private static final float MARKER_SIZE = 0.8f;

	private Map(int mapW, int mapH, int across, int down,
			int lineColor, int lineThickness, int bgResId,
			int startGridX, int startGridY, int endGridX, int endGridY) {

		this.across = across;
		this.down = down;

		start = new Node(0, 0);
		end = new Node(0, 0);

		ghostStart = new Node(0, 0);
		ghostEnd = new Node(0, 0);

		grid = new Grid(mapW, mapH, across, down);
		grid.setBackgroundResource(bgResId);
		grid.setLineSpec(lineColor, lineThickness);
		grid.useAsDrawable();

		tileW = grid.getTileWidth();
		tileH = grid.getTileHeight();

		marginLeft = grid.extraWidth / 2.0f;
		marginTop = grid.extraHeight / 2.0f;

		selectionBox = new PictureBox(0, 0, 
				tileW, tileH, R.drawable.selection_box_2);

		final float markerSize = MARKER_SIZE * tileW;
		final float markerHalfSize = markerSize / 2.0f;

		entranceMarker = new PictureBox((startGridX + 0.5f) * tileW - markerHalfSize, (startGridY + 0.5f) * tileW - markerHalfSize,
				markerSize, markerSize, R.drawable.bg_element_direction_mark);

		exitMarker = new PictureBox((endGridX + 0.5f) * tileW - markerHalfSize, (endGridY + 0.5f) * tileW - markerHalfSize,
				markerSize, markerSize, R.drawable.bg_element_direction_mark);

		isBlocked = new boolean[across][down];
		mapObj = new Object[across][down];
		for (boolean[] line : isBlocked) {
			Arrays.fill(line, false);
		}

		this.startGridX = startGridX;
		this.startGridY = startGridY;
		this.endGridX = endGridX;
		this.endGridY = endGridY;

		touchSlop = ViewConfiguration.get(Core.context).getScaledTouchSlop();
	}

	public void registerObject(int tileX, int tileY, Object obj) {
		mapObj[tileX][tileY] = obj;
	}

	public void unregisterObject(int tileX, int tileY) {
		mapObj[tileX][tileY] = null;
	}

	public Object getObject(int tileX, int tileY) {
		return mapObj[tileX][tileY];
	}

	public void initPathFinder() {
		pathFinder = new DijkstraPathFinder(this);
	}

	public boolean findPath() {
		boolean pathExist = pathFinder.findPath();
		if(pathExist) {
			start.parent = pathFinder.nodes[startGridX][startGridY];

			// select a spawn point that makes sense and is offscreen
			if(startGridX == 0)
				start.x = -1;
			else
				start.x = startGridX;

			if(startGridY == 0)
				start.y = -1;
			else
				start.y = startGridY;

			pathFinder.nodes[endGridX][endGridY].parent = end;

			// select a end point that makes sense and is offscreen
			if(endGridX == across - 1)
				end.x = across;
			else
				end.x = endGridX;

			if(endGridY == down - 1)
				end.y = across;
			else
				end.y = endGridY;

			ghostStart.x = start.x;
			ghostStart.y = start.y;
			ghostStart.parent = ghostEnd;
			ghostEnd.x = end.x;
			ghostEnd.y = end.y;
		}
		return pathExist;
	}

	public Node[][] getPath() {
		return pathFinder.nodes;
	}

	public Node getPathStart() {
		return start;
	}

	public Node getGhostPathStart() {
		return ghostStart;
	}

	public Node getPathNode(int x, int y) {
		if(x == start.x && y == start.y)
			return start;
		if(x == end.x && y == end.y)
			return end;
		return pathFinder.nodes[x][y];
	}

	public void drawPath() {
		//		if(path == null)
		//			path = new PictureBox[grid.getTilesAcross() * grid.getTilesDown()];
		//		
		//		synchronized(path) {
		//			final int across = grid.getTilesAcross();
		//			final int down = grid.getTilesDown();
		//			final float tile = grid.getTileHeight();
		//
		//			for(int i = 0; i < across; i++) {
		//				for(int j = 0; j < down; j++) {
		//					Node n = pathFinder.nodes[i][j].parent;
		//					if(n != null && !isBlocked(i, j)) {
		//
		//						if(n.x == i) {
		//							if(n.y < j) {
		//								path[i * down + j] = new PictureBox(i * tile, j * tile, 
		//										Core.GeneralBuffers.tile_not_centered, R.drawable.path_u);
		//							} else {
		//								path[i * down + j] = new PictureBox(i * tile, j * tile, 
		//										Core.GeneralBuffers.tile_not_centered, R.drawable.path_d);
		//							}
		//						} else {
		//							if(n.x < i) {
		//								path[i * down + j] = new PictureBox(i * tile, j * tile, 
		//										Core.GeneralBuffers.tile_not_centered, R.drawable.path_l);
		//							} else {
		//								path[i * down + j] = new PictureBox(i * tile, j * tile, 
		//										Core.GeneralBuffers.tile_not_centered, R.drawable.path_r);
		//							}
		//						}
		//					} else {
		//						path[i * down + j] = new PictureBox(i * tile, j * tile, 
		//								Core.GeneralBuffers.tile_not_centered, R.drawable.right_panel);
		//					}
		//				}
		//			}
		//			showPath = true;
		//		}
	}

	public int getTilesAcross() {
		return grid.getTilesAcross();
	}

	public int getTilesDown() {
		return grid.getTilesDown();
	}

	public int getStartTileX() {
		return startGridX;
	}

	public int getStartTileY() {
		return startGridY;
	}

	public int getEndTileX() {
		return endGridX;
	}

	public int getEndTileY() {
		return endGridY;
	}

	public boolean isBlocked(int tileX, int tileY) {
		return isBlocked[tileX][tileY];
	}

	public boolean isTileOnPath(int tileX, int tileY) {
		Node n = getPathStart();
		while(n != null) {
			if(tileX == n.x && tileY == n.y) {
				return true;
			}
			n = n.parent;
		}
		return false;
	}

	public boolean isInBounds(int tileX, int tileY) {
		return (tileX >= 0 && tileX < across && 
				tileY >= 0 && tileY < down);
	}

	public void setBlocked(int tileX, int tileY, boolean b) {
		isBlocked[tileX][tileY] = b;
	}

	@Override
	public void draw(float offX, float offY) {
		grid.draw(offX, offY);

		if(showPath) {
			synchronized(path) {
				for(PictureBox p : path) {
					p.draw(offX, offY);
				}
			}
		}

		entranceMarker.draw(offX, offY);
		exitMarker.draw(offX, offY);

		if(showSelection) {
			selectionBox.draw(offX, offY);
		}
	}

	@Override
	public void refresh() {
		grid.refresh();
		selectionBox.refresh();
		entranceMarker.refresh();
		exitMarker.refresh();
		if(path != null) {
			for(PictureBox p : path) {
				p.refresh();
			}
		}
	}

	private float nearestX, nearestY;
	private int tileX, tileY;

	private void genNearest(float x, float y) {
		tileX = (int)(x / tileW);
		tileY = (int)(y / tileH);
		nearestX = (tileX) * tileW;// + marginLeft;
		nearestY = (tileY) * tileH;// + marginTop;
	}

	public float getTileW() {
		return tileW;
	}

	public float getTileH() {
		return tileH;
	}

	public float getWidth() {
		return grid.getW();
	}

	public float getHeight() {
		return grid.getH();
	}

	public float getDrawingWidth() {
		return grid.drawingW;
	}

	public float getDrawingHeight() {
		return grid.drawingH;
	}

	public float getMarginLeft() {
		return marginLeft;
	}

	public float getMarginTop() {
		return marginTop;
	}
	
	private int lastX, lastY;
	private int startingX, startingY;
	private boolean canceled;
	private boolean focused = false;

	private static final float MAP_BOUND = Core.SDP;

	private void onDragStarted() {
		canceled = true;
		showSelection = false;

		Core.game.hideTowerRadius();
	}

	@Override
	public boolean onTouchEvent(int action, int x, int y) {
		switch(action) {
		case MotionEvent.ACTION_DOWN:
			genNearest(touchOffX + x, touchOffY + y);

			if(isInBounds(tileX, tileY)) {
				selectionBox.x = nearestX;
				selectionBox.y = nearestY;

				showSelection = true;

				Core.game.showTowerRadius(Core.hud.getSelectedTower(), nearestX + tileW / 2, nearestY + tileW / 2);
			}

			lastX = x;
			lastY = y;

			startingX = x;
			startingY = y;

			canceled = false;
			focused = true;

			return true;
		case MotionEvent.ACTION_MOVE:
			if(!focused) return true;

			genNearest(touchOffX + x, touchOffY + y);

			if(canceled) {
				final int deltaX = x - lastX;
				final int deltaY = y - lastY;

				if( Core.canvasWidth - Core.offX - deltaX < MAP_BOUND || 
						Core.canvasWidth + Core.offX + deltaX < MAP_BOUND) {

				} else
					Core.offX += deltaX;

				if( Core.canvasHeight - Core.offY - deltaY < MAP_BOUND ||
						Core.canvasHeight + Core.offY + deltaY < MAP_BOUND ) {

				} else
					Core.offY += deltaY;

				Core.game.onScrolled();
			} else {
				final int deltaX2 = x - startingX;
				final int deltaY2 = y - startingY;

				selectionBox.x = nearestX;
				selectionBox.y = nearestY;

				if(Math.abs(deltaX2) > touchSlop || Math.abs(deltaY2) > touchSlop) {
					onDragStarted();
				}
			}

			lastX = x;
			lastY = y;

			return true;
		case MotionEvent.ACTION_UP:
			if(!focused) return true;

			showSelection = false;
			focused = false;

			Core.game.hideTowerRadius();

			if(!canceled)
				Core.game.mapClicked((int)((touchOffX + x) / tileW), (int)((touchOffY + y) / tileH));

			return true;
		case MotionEvent.ACTION_CANCEL:
			showSelection = false;
			focused = false;

			Core.game.hideTowerRadius();
			return false;
		default:
			return false;
		}
	}

	public void setOffset(float offX, float offY) {
		Core.offX = offX;
		Core.offY = offY;

		touchOffX = grid.x - Core.offX;
		touchOffY = grid.y - Core.offY;

		Core.game.onScrolled();
	}

	public void onScroll() {
		touchOffX = grid.x - Core.offX;
		touchOffY = grid.y - Core.offY;
	}
}
