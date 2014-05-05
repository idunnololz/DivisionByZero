package com.ggstudios.divisionbyzero;

import java.lang.ref.WeakReference;
import java.util.PriorityQueue;

import com.ggstudios.utils.DebugLog;

/**
 * PathFinder uses a weird variation of Djikstra's path finding
 * algorithm. This modified version will maintain a graph of paths
 * to a destination node. Because of this, the algorithm will continue
 * to run until every vertex in the graph has been processed.
 *
 * The runtime is Theta(4*points).
 * 
 * @author iDunnololz
 *
 */
public class PathFinder {
	private static final String TAG = "DijkstraPathFinder"; 

	private static final int INFINITY = 10000;
	
	public Node[][] nodes;
	private WeakReference<Map> map;
	private PriorityQueue<Node> unvisited;

	private int across, down;

	public PathFinder(Map map){
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
	}
	
	/**
	 * Updates the {@link #nodes} variable with a path to the destination.
	 *
	 * @param startPoint	The starting point.
	 * @param endPoint		The end point.
	 * @return				Returns true if there exists a path from the starting point to the end point.
	 * 						Else returns false.
	 */
	public boolean findPath(Node startPoint, Node endPoint) {
		// use reverse-dijkstra's alg
		final Map map = this.map.get();
		
		if(map == null) {
			DebugLog.e(TAG, "Error! Map has been gc'd!", new Exception());
			return false;
		}
		
		final int dx = startPoint.x;
		final int dy = startPoint.y;
		final int sx = endPoint.x;
		final int sy = endPoint.y;
		
		boolean pathExist = false;
		
		if(map.isBlocked(sx,sy)) return false;
		
		for (int x = 0; x < across; x++) {
			for (int y = 0; y < down; y++) {
				nodes[x][y].cost = INFINITY;
			}
		}

		nodes[sx][sy].cost = 0;
		
		// temp. commented out because this will reset the end square's parent...
		//nodes[sx][sy].parent = null;

		Node current;

		unvisited.add(nodes[sx][sy]);

		while(!unvisited.isEmpty()){
			current = unvisited.poll();

			for(int x = -1; x < 2; x++) {
				for(int y = -1; y < 2; y++) {
					if (x == 0 && y == 0) {
						continue;
					}
					
					final int nx = x + current.x;
					final int ny = y + current.y;
					if (nx < 0 || nx >= across || ny < 0 || ny >= down) {
						continue;
					}
					
					float cost = 1f;
					
					if(Math.abs(x) + Math.abs(y) == 2) {
						// diagonal...
						cost = 1.41f;
						if(map.isBlocked(nx, current.y) || map.isBlocked(current.x, ny)) {
							continue;
						}
					}
					
					final Node n = nodes[nx][ny];
					
					if (!map.isBlocked(n.x,n.y)) {
						// the cost to get to this node is cost the current plus the movement
						// cost to reach this node.

						float nextStepCost = current.cost + cost;

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

		public Node(Node n) {
			this.x = n.x;
			this.y = n.y;
			this.parent = n.parent;
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
