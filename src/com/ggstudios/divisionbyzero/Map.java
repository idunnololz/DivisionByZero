package com.ggstudios.divisionbyzero;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.ggstudios.divisionbyzero.PathFinder.Node;
import com.ggstudios.utils.DebugLog;

public class Map extends Drawable implements Clickable {
	public static class Builder {
		private static final String TAG = "Map.Builder";

		private int across = 1, down = 1;

		private int lineColor = 0x80FFFFFF, lineThickness = 1;
		private int resId = -1;
		private int sX, sY, eX, eY;

		public Builder(){
			sX = 0;
			sY = 0;
			eX = across - 1;
			eY = down - 1;
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
			case 2:
				setGridSize(11, 11);
				setGridStart(0, 0);
				setGridEnd(across - 1, down - 1);
				setBackground(R.drawable.map_bg_zone_1);
				setLineColor(0x30FFFFFF);
				break;
			case 10:
				// elemental td clone...
				setGridSize(17, 14);
				setGridStart(5, 0);
				setGridEnd(8, 0);
				setBackground(R.drawable.eletd);
				setLineColor(0x30FFFFFF);
				break;
			case 12:
				setBackground(R.drawable.lv_12);
				setLineColor(0x30FFFFFF);
				break;
			default:
				DebugLog.e(TAG, "Error. Unknown map type.");
				break;
			}
			return this;
		}

		public Map build() {
			return new Map(across, down, lineColor, lineThickness,
					resId,
					sX, sY, eX, eY);
		}

		public int getTilesDown() {
			return down;
		}

		public int getTilesAcross() {
			return across;
		}
	}

	private Grid grid;
	private float tileW, tileH;
	private List<PathFinder> paths = new ArrayList<PathFinder>();

	private boolean[][] permablocked;
	private boolean[][] blocked;
	private boolean[][] buildableGround;
	private boolean showPath = false;
	private PictureBox[] path;
	private PictureBox selectionBox;
	private List<PictureBox> markers = new ArrayList<PictureBox>();
	private boolean showSelection = false;
	private Object[][] mapObj;

	private List<Node> oStartNodes, oEndNodes;
	private List<Node> startNodes, endNodes;
	private List<Node> ghostStartNodes, ghostEndNodes;

	private float touchSlop;
	private float marginLeft;
	private float marginTop;
	private int width, height;
	private int across, down;

	private boolean disabled = false;

	/* The following variables are for handling touch events */
	/**/private float lastX, lastY;
	private float startingX, startingY;
	private boolean canceled;
	private boolean focused = false;

	private static final float MAP_BOUND = Core.SDP;
	
	// stores the amount to offset touch events to translate to
	// a touch on the map...
	private float touchOffX, touchOffY;

	private static final float MARKER_SIZE = 0.8f;
	/* END OF TOUCH EVENT VARIABLES */

	private Map(int across, int down,
			int lineColor, int lineThickness, int bgResId,
			int startGridX, int startGridY, int endGridX, int endGridY) {

		this.across = across;
		this.down = down;

		oStartNodes = new ArrayList<Node>();
		oEndNodes = new ArrayList<Node>();
		startNodes = new ArrayList<Node>();
		endNodes = new ArrayList<Node>();

		ghostStartNodes = new ArrayList<Node>();
		ghostEndNodes = new ArrayList<Node>();

		Node startNode = new Node(startGridX, startGridY);
		Node endNode = new Node(endGridX, endGridY);
		Node startGhostNode = new Node(0, 0);
		Node endGhostNode = new Node(0, 0);

		oStartNodes.add(startNode);
		oEndNodes.add(endNode);
		ghostStartNodes.add(startGhostNode);
		ghostEndNodes.add(endGhostNode);

		grid = new Grid(width, height, across, down);
		grid.setBackgroundResource(bgResId);
		grid.setLineSpec(lineColor, lineThickness);

		onSizeChanged();
		
		blocked = new boolean[across][down];
		buildableGround = new boolean[across][down];
		permablocked = new boolean[across][down];
		mapObj = new Object[across][down];
		for (boolean[] line : blocked) {
			Arrays.fill(line, false);
		}
		for (boolean[] line : buildableGround) {
			Arrays.fill(line, true);
		}
		for (boolean[] line : permablocked) {
			Arrays.fill(line, false);
		}
	}
	
	public void resize(int newW, int newH) {
		width = newW;
		height = newH;
		onSizeChanged();
	}
	
	private void onSizeChanged() {
		grid.sizeChanged(width, height);
		grid.useAsDrawable();
		
		tileW = grid.getTileWidth();
		tileH = grid.getTileHeight();
		
		selectionBox = new PictureBox(0, 0, tileW, tileH, R.drawable.selection_box_2);
		
		setupMarkers();
		
		marginLeft = grid.extraWidth / 2.0f;
		marginTop = grid.extraHeight / 2.0f;

		touchSlop = ViewConfiguration.get(Core.context).getScaledTouchSlop();
	}
	
	private void setupMarkers() {
		markers.clear();
		
		final float markerSize = MARKER_SIZE * tileW;
		
		for(Node n : oStartNodes) {
			PictureBox entranceMarker = new PictureBox((n.x + 0.5f) * tileW, (n.y + 0.5f) * tileW,
					markerSize, markerSize, R.drawable.bg_element_direction_mark, true);
	
			setupEntranceMarker(entranceMarker, n.x, n.y);
			
			markers.add(entranceMarker);
		}
		
		for(Node n : oEndNodes) {
			PictureBox exitMarker = new PictureBox((n.x + 0.5f) * tileW, (n.y + 0.5f) * tileW,
					markerSize, markerSize, R.drawable.bg_element_direction_mark, true);
			
			setupExitMarker(exitMarker, n.x, n.y);
			
			markers.add(exitMarker);
		}
	}
	
	private void setupEntranceMarker(PictureBox entranceMarker, int startGridX, int startGridY) {
		if(startGridX == 0 && startGridY == 0) {
			// set entrance marker to be tilted...
			entranceMarker.setAngle((float)-(Math.PI * 0.25));
		} else if(startGridX == across - 1 && startGridY == down - 1) {
			// set entrance marker to be tilted...
			entranceMarker.setAngle((float)(Math.PI * 0.25));
		} else if(startGridX == 0) {
			entranceMarker.setAngle(0);
		} else if(startGridY == 0) {
			entranceMarker.setAngle((float)-(Math.PI * 0.5));
		} else if(startGridX == across - 1) {
			// need to do a 180
			entranceMarker.setAngle((float)(Math.PI));
		} else if(startGridY == down - 1) {
			entranceMarker.setAngle((float)(Math.PI * 0.5));
		}
	}
	
	private void setupExitMarker(PictureBox exitMarker, int endGridX, int endGridY) {
		if(endGridX == 0 && endGridY == 0) {
			// set entrance marker to be tilted...
			exitMarker.setAngle((float)(Math.PI * 0.25));
		} else if(endGridX == across - 1 && endGridY == down - 1) {
			// set entrance marker to be tilted...
			exitMarker.setAngle((float)-(Math.PI * 0.25));
		} else if(endGridX == 0) {
			exitMarker.setAngle((float)-(Math.PI));
		} else if(endGridY == 0) {
			exitMarker.setAngle((float)(Math.PI * 0.5));
		} else if(endGridX == across - 1) {
			exitMarker.setAngle(0);
		} else if(endGridY == down - 1) {
			exitMarker.setAngle((float)-(Math.PI * 0.5));		
		}
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

	/**
	 * Initializes the path finder once all key information has been set.
	 */
	public void initPathFinder() {
		// we hope that when this function is called
		// the number of start/end node pairs has been set
		// and WILL NOT CHANGE in the future as this function
		// needs to be called whenever that happens and 
		// this can be very costly to call...
		
		paths.clear();
		
		for(int i = 0; i < oStartNodes.size(); i++) {
			PathFinder p = new PathFinder(this);
			paths.add(p);
		}
	}
	
	public void forceFullFindPath() {
		startNodes.clear();
		endNodes.clear();
		findPath();
	}

	public boolean findPath() {
		boolean pathExist = true;
		for(int i = 0; i < paths.size(); i++) {
			Node sn = oStartNodes.get(i);
			Node dn = oEndNodes.get(i);
			pathExist &= paths.get(i).findPath(sn, dn);
			if(!pathExist) break;
		}
		if(pathExist && startNodes.size() == 0) {
			for(int i = 0; i < oStartNodes.size(); i++) {
				PathFinder pathFinder = paths.get(i);
				
				Node startNode = new Node(oStartNodes.get(i));
				Node endNode = new Node(oEndNodes.get(i));
				startNodes.add(startNode);
				endNodes.add(endNode);
				
				int startGridX = startNode.x;
				int startGridY = startNode.y;
				int endGridX = endNode.x;
				int endGridY = endNode.y;
				
				if(ghostStartNodes.size() == i) {
					ghostStartNodes.add(new Node(0, 0));
					ghostEndNodes.add(new Node(0, 0));
				}
				
				Node ghostStartNode = ghostStartNodes.get(i);
				Node ghostEndNode = ghostEndNodes.get(i);

				startNode.parent = pathFinder.nodes[startGridX][startGridY];

				// select a spawn point that makes sense and is offscreen
				if(startGridX == 0)
					startNode.x = -1;
				else
					startNode.x = startGridX;

				if(startGridY == 0)
					startNode.y = -1;
				else
					startNode.y = startGridY;

				pathFinder.nodes[endGridX][endGridY].parent = endNode;

				// select a end point that makes sense and is offscreen
				if(endGridX == across - 1)
					endNode.x = across;
				else
					endNode.x = endGridX;

				if(endGridY == down - 1)
					endNode.y = down;
				else
					endNode.y = endGridY;

				ghostStartNode.x = startNode.x;
				ghostStartNode.y = startNode.y;
				ghostStartNode.parent = ghostEndNode;
				ghostEndNode.x = endNode.x;
				ghostEndNode.y = endNode.y;
			}
		}
		return pathExist;
	}

	public Node getPathStart() {
		return getPathStart(0);
	}
	
	public Node getPathStart(int pathIndex) {
		return startNodes.get(pathIndex);
	}

	public Node getGhostPathStart() {
		return getGhostPathStart(0);
	}
	
	public Node getGhostPathStart(int pathIndex) {
		return ghostStartNodes.get(pathIndex);
	}

	public Node getClosestNode(float x, float y, int pathIndex) {
		tileX = (int)(x / tileW);
		tileY = (int)(y / tileH);

		return getPathNode(tileX, tileY, pathIndex);
	}

	public Node getPathNode(int x, int y, int pathIndex) {
		// TODO: Optimize...
		for(Node n : startNodes) {
			if(x == n.x && y == n.y)
				return n;
		}
		for(Node n : endNodes) {
			if(x == n.x && y == n.y)
				return n;
		}
		return paths.get(pathIndex).nodes[x][y];
	}

	public int getTilesAcross() {
		return grid.getTilesAcross();
	}

	public int getTilesDown() {
		return grid.getTilesDown();
	}

	public boolean isBlocked(int tileX, int tileY) {
		return blocked[tileX][tileY];
	}

	public boolean canBuildAt(int tileX, int tileY) {
		return buildableGround[tileX][tileY];
	}

	public boolean isInBounds(int tileX, int tileY) {
		return (tileX >= 0 && tileX < across && 
				tileY >= 0 && tileY < down);
	}

	public void setBlocked(int tileX, int tileY, boolean b) {
		if(!permablocked[tileX][tileY])
			blocked[tileX][tileY] = b;
	}

	public void setPermablocked(int tileX, int tileY, boolean b) {
		permablocked[tileX][tileY] = b;
	}

	public void setCanBuildAt(int tileX, int tileY, boolean b) {
		buildableGround[tileX][tileY] = b;
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

		for(PictureBox m : markers) {
			m.draw(offX, offY);
		}
		
		if(showSelection) {
			selectionBox.draw(offX, offY);
		}
	}

	@Override
	public void refresh() {
		grid.refresh();
		selectionBox.refresh();
		for(PictureBox m : markers) {
			m.refresh();
		}
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

	private void onDragStarted() {
		canceled = true;
		showSelection = false;

		Core.game.hideSecondaryTowerRadius();
	}

	@Override
	public boolean onTouchEvent(int action, float x, float y) {
		if(disabled) return false;

		switch(action) {
		case MotionEvent.ACTION_DOWN:
			genNearest(touchOffX + x, touchOffY + y);

			if(isInBounds(tileX, tileY)) {
				selectionBox.x = nearestX;
				selectionBox.y = nearestY;

				showSelection = true;

				Core.game.showSecondaryTowerRadius(Core.hud.getSelectedTower(), nearestX + tileW / 2, nearestY + tileW / 2);
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
				final float deltaX = x - lastX;
				final float deltaY = y - lastY;

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
				final float deltaX2 = x - startingX;
				final float deltaY2 = y - startingY;

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

			Core.game.hideSecondaryTowerRadius();

			if(!canceled)
				Core.game.mapClicked((int)((touchOffX + x) / tileW), (int)((touchOffY + y) / tileH));

			return true;
		case MotionEvent.ACTION_CANCEL:
			showSelection = false;
			focused = false;

			Core.game.hideSecondaryTowerRadius();
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

	public void setOffsetX(float offX) {
		Core.offX = offX;

		touchOffX = grid.x - Core.offX;

		Core.game.onScrolled();
	}

	public void setOffsetY(float offY) {
		Core.offY = offY;

		touchOffY = grid.y - Core.offY;

		Core.game.onScrolled();
	}

	public void onScroll() {
		touchOffX = grid.x - Core.offX;
		touchOffY = grid.y - Core.offY;
	}

	public void setEnabled(boolean enabled) {
		disabled = !enabled;
	}

	public void setStartEndNodes(List<Node> startNodes, List<Node> endNodes) {
		if(startNodes.size() != endNodes.size())
			throw new RuntimeException("Error. Size of starting nodes and ending nodes do not match.");		
		
		this.oStartNodes = startNodes;
		this.oEndNodes = endNodes;
		
		this.startNodes.clear();
		this.endNodes.clear();
		
		setupMarkers();
	}
}
