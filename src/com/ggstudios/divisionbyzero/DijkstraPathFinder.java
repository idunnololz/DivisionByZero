package com.ggstudios.divisionbyzero;

import java.lang.ref.WeakReference;
import java.util.PriorityQueue;

import com.ggstudios.utils.DebugLog;

public class DijkstraPathFinder {
	private static final String TAG = "DijkstraPathFinder"; 
	
	public Node[][] nodes;
	private WeakReference<Map> map;

	private PriorityQueue<Node> unvisited;

	static final int INFINITY = 10000;

	int across, down;

	// TODO: Construct the neighbor map once so to increase the init cost but decrease
	// subsequent calls. 

	public DijkstraPathFinder(Map map){
		this.map = new WeakReference<Map>(map);

		across = map.getTilesAcross();
		down = map.getTilesDown();

		nodes = new Node[across][down];
		resetNodes(nodes);

		unvisited = new PriorityQueue<Node>(across * down);
	}

	private void resetNodes(Node[][] nodes) {
		for (int x = 0; x < across; x++) {
			for (int y = 0; y < down; y++) {
				nodes[x][y] = new Node(x,y);
			}
		}

		final int lastX = across - 1;
		final int lastY = down - 1;

		for (int x = 0; x < across; x++) {
			for (int y = 0; y < down; y++) {
				final Node n = nodes[x][y];

				if(x != 0) {
					if(y != 0) {
						if(x != lastX) {
							if (y != lastY) {
								n.n = new Node[4];
								n.n[0] = nodes[x - 1][y];
								n.n[1] = nodes[x + 1][y];
								n.n[2] = nodes[x][y - 1];
								n.n[3] = nodes[x][y + 1];
							} else {
								n.n = new Node[3];
								n.n[0] = nodes[x - 1][y];
								n.n[1] = nodes[x + 1][y];
								n.n[2] = nodes[x][y - 1];
							}
						} else {
							if (y != lastY) {
								n.n = new Node[3];
								n.n[0] = nodes[x - 1][y];
								n.n[1] = nodes[x][y - 1];
								n.n[2] = nodes[x][y + 1];
							} else {
								n.n = new Node[2];
								n.n[0] = nodes[x - 1][y];
								n.n[1] = nodes[x][y - 1];
							}
						}
					} else {
						if(x != lastX) {
							n.n = new Node[3];
							n.n[0] = nodes[x - 1][y];
							n.n[1] = nodes[x + 1][y];
							n.n[2] = nodes[x][y + 1];
						} else {
							n.n = new Node[2];
							n.n[0] = nodes[x - 1][y];
							n.n[1] = nodes[x][y + 1];
						}
					}
				} else {
					if(y != 0) {
						if (y != lastY) {
							n.n = new Node[3];
							n.n[0] = nodes[x + 1][y];
							n.n[1] = nodes[x][y - 1];
							n.n[2] = nodes[x][y + 1];
						} else {
							n.n = new Node[2];
							n.n[0] = nodes[x + 1][y];
							n.n[1] = nodes[x][y - 1];
						}
					} else {
						n.n = new Node[2];
						n.n[0] = nodes[x + 1][y];
						n.n[1] = nodes[x][y + 1];
					}
				}
			}
		}
	}
	
	/**
	 * Updates the {@link #nodes} varaible with a path to the desination.
	 * @return	Returns true if a path exists from the specified start.
	 * 			Else returns false.
	 */
	public boolean findPath() {
		// use reverse-dijkstra's alg
		final Map map = this.map.get();
		
		final int sx = map.getEndTileX();
		final int sy = map.getEndTileY();
		
		final int dx = map.getStartTileX();
		final int dy = map.getStartTileY();
		boolean pathExist = false;
		
		if(map.isBlocked(sx,sy)) return false;
		
		for (int x = 0; x < across; x++) {
			for (int y = 0; y < down; y++) {
				nodes[x][y].cost = INFINITY;
			}
		}

		nodes[sx][sy].cost = 0;
		nodes[sx][sy].parent = null;

		Node current;

		unvisited.add(nodes[sx][sy]);

		while(!unvisited.isEmpty()){
			//Collections.sort(unvisited);
			current = unvisited.poll();

			for(Node n : current.n) {
				if (!map.isBlocked(n.x,n.y)) {
					// the cost to get to this node is cost the current plus the movement
					// cost to reach this node.

					float nextStepCost = current.cost + 1;

					if (nextStepCost < n.cost) {
						unvisited.remove(n);
						n.cost = nextStepCost;
						n.parent = current;
						unvisited.add(n);
					}
					
					if(n.x == dx && n.y == dy)
						pathExist = true;
				}
			}
		}
		
		return pathExist;
	}

	public static class Node implements Comparable<Node> {
		/** The x coordinate of the node */
		int x;
		/** The y coordinate of the node */
		int y;
		/** The path cost for this node */
		private float cost;
		/** The parent of this node, how we reached it in the search */
		public Node parent;
		private Node[] n;

		/**
		 * Create a new node
		 * 
		 * @param x The x coordinate of the node
		 * @param y The y coordinate of the node
		 */
		public Node(int x, int y) {
			this.x = x;
			this.y = y;
		}

		/**
		 * @see Comparable#compareTo(Object)
		 */
		public int compareTo(Node other) {
			if (cost < other.cost) {
				return -1;
			} else if (cost > other.cost) {
				return 1;
			} else {
				return 0;
			}
		}
	}
}
