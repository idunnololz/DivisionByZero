package com.ggstudios.divisionbyzero;

public class TowerLibrary {
	
	static class TowerEvoTree {
		int typeId;
		int maxLevel;
		
		String name[];
		
		int dmg[];
		boolean hasSpecial[];	//false = manual, true = auto
		//bullet speed, aoe and range are expressed in G(grid) units where 1G = the length of one side of a grid square
		float bs[];
		float range[];
		float aoe[];
		// attack speeds are inverse, meaning a tower with an attack speed of 1 will attack 2x faster than a tower with as of 2
		float as[];		// this is in SECONDS
		int cost[];
		int resell[];	// optional...
		
		// this variable is used for storing
		// values for special abilities...
		Object extra;
		
		int resId[];
		
		// texture atlas coordinates
		int taTileX[];
		int taTileY[];
		
		TowerEvoTree[][] typeUpgrade;
		
		public TowerEvoTree(
				int typeId,
				String[] name,
				int[] dmg, boolean[] hasSpecial, float[] bs,
				float[] range, float[] aoe, float[] as, int[] cost, int[] resell,
				Object extra, int[] resId,
				TowerEvoTree[][] typeUpgrade, int[] taTileX, int[] taTileY){
			
			this.typeId = typeId;
			
			maxLevel = cost.length;
			
			this.name = name;
			this.dmg = dmg;
			this.hasSpecial = hasSpecial;
			this.bs = bs;
			this.range = range;
			this.aoe = aoe;
			this.as = as;
			this.cost = cost;
			this.resell = resell;
			this.extra = extra;
			this.resId = resId;
			this.typeUpgrade = typeUpgrade;
			this.taTileX = taTileX;
			this.taTileY = taTileY;
		}
	}
	
	public static final int 
	TYPE_DYNAMIC = 1,
	TYPE_BOSS = 2,
	TYPE_REGULAR = 3,
	
	TYPE_HEAVY = 4,
	TYPE_CLUSTER = 5,
	TYPE_BOX = 6,
	
	TYPE_SPECIALIST = 7,
	TYPE_NORMAL = 8,
	TYPE_DESIRE = 9,
	TYPE_VOID = 10,
	TYPE_NULL = 11,
	
	TYPE_DIAMOND = 12,
	TYPE_DEMO = 13,
	TYPE_FLAKE = 14,
	
	TYPE_CIRCLE = 15,
	TYPE_BRUTAL = 16,
	TYPE_DESOLATOR = 17;
	
	public static final TowerEvoTree DYNAMIC = new TowerEvoTree(
			TYPE_DYNAMIC,
			new String[] {"Dynamic 1", "Dynamic 2"},
			/*dmg*/ 	new int[] {60, 120},
			/*hasSS*/ 	new boolean[] {false, true},
			/*bs*/ 		new float[] {10f, -1f},
			/*range*/ 	new float[] {5f, 9f},
			/*aoe*/ 	null,
			/*as*/ 		new float[] {1.6f, 1.6f},
			/*cost*/ 	new int[] {400, 500},
			/*resell*/ 	null,
			/*duration*/null,
			/*resId*/ 	new int[] {R.drawable.tower_dynamic, R.drawable.tower_dynamic_2},
			/*n-type*/ 	null,
			/*ta-x*/	new int[] {3, 4},
			/*ta-y*/	new int[] {0, 0}
			);
	
	public static final TowerEvoTree BOSS = new TowerEvoTree(
			TYPE_BOSS,
			new String[] {"Boss"},
			/*dmg*/ 	new int[] {25},
			/*hasSS*/ 	new boolean[] {true},
			/*bs*/ 		new float[] {10f},
			/*range*/ 	new float[] {3f},
			/*aoe*/ 	null,
			/*as*/ 		new float[] {0.3f},
			/*cost*/ 	new int[] {500},
			/*resell*/ 	null,
			/*durat8ion*/null,
			/*resId*/ 	new int[] {R.drawable.tower_boss},
			/*n-type*/ 	null,
			/*ta-x*/	new int[] {5},
			/*ta-y*/	new int[] {0}
			);
	
	public static final TowerEvoTree REGULAR = new TowerEvoTree(
			TYPE_REGULAR,
			new String[] {"Regular 1", "Regular 2", "Regular 3"},
			/*dmg*/ 	new int[] {5, 10, 20},
			/*hasSS*/ 	new boolean[] {false, false, false},
			/*bs*/ 		new float[] {10f, 10f, 10f},
			/*range*/ 	new float[] {3f, 3f, 3f},
			/*aoe*/ 	null,
			/*as*/ 		new float[] {1.8f, 1.7f, 1.6f},
			/*cost*/ 	new int[] {100, 150, 200},
			/*resell*/ 	new int[] {100},
			/*duration*/null,
			/*resId*/ 	new int[] {R.drawable.tower_regular, R.drawable.tower_regular_2, R.drawable.tower_regular_3},
			/*n-type*/ 	new TowerEvoTree[][] {null, null, null, new TowerEvoTree[] {DYNAMIC, BOSS}},
			/*ta-x*/	new int[] {0, 1, 2},
			/*ta-y*/	new int[] {0, 0, 0}
			);
	
	public static final TowerEvoTree CLUSTER = new TowerEvoTree(
			TYPE_CLUSTER,
			new String[] {"Cluster"},
			/*dmg*/ 	new int[] {30},
			/*hasSS*/ 	new boolean[] {true},
			/*bs*/ 		new float[] {4f},
			/*range*/ 	new float[] {7f},
			/*aoe*/ 	new float[] {1f},
			/*as*/ 		new float[] {5f},
			/*cost*/ 	new int[] {600},
			/*resell*/ 	null,
			/*duration*/null,
			/*resId*/ 	new int[] {R.drawable.tower_cluster},
			/*n-type*/ 	null,
			/*ta-x*/	new int[] {6},
			/*ta-y*/	new int[] {1}
			);
	
	public static final TowerEvoTree BOX = new TowerEvoTree(
			TYPE_BOX,
			new String[] {"Box 1", "Box 2", "Box 3"},
			/*dmg*/ 	new int[] {10, 20, 30},
			/*hasSS*/ 	new boolean[] {true, true, true},
			/*bs*/ 		new float[] {-1f, -1f, -1f},
			/*range*/ 	new float[] {4.5f, 4.7f, 5f},
			/*aoe*/ 	null,
			/*as*/ 		new float[] {4f, 3.5f, 3f},
			/*cost*/ 	new int[] {250, 400, 500},
			/*resell*/ 	null,
			/*duration*/null,
			/*resId*/ 	new int[] {R.drawable.tower_box_1, R.drawable.tower_box_2, R.drawable.tower_box_3},
			/*n-type*/ 	null,
			/*ta-x*/	new int[] {3, 4, 5},
			/*ta-y*/	new int[] {1, 1, 1}
			);
	
	public static final TowerEvoTree HEAVY = new TowerEvoTree(
			TYPE_HEAVY,
			new String[] {"Heavy 1", "Heavy 2", "Heavy 3", "Heavy 4", "Heavy 5"},
			/*dmg*/ 	new int[] {10, 20, 30, 50, 80},
			/*hasSS*/ 	new boolean[] {true, true, true, true, true},
			/*bs*/ 		new float[] {10f, 11f, 12f, 13f, 14f},
			/*range*/ 	new float[] {3f, 3.2f, 3.4f, 4f, 4.5f},
			/*aoe*/ 	new float[] {1f, 1.1f, 1.2f, 1.3f, 1.5f},
			/*as*/ 		new float[] {3f, 2.5f, 2.2f, 2f, 1.8f},
			/*cost*/ 	new int[] {300, 300, 300, 400, 500},
			/*resell*/ 	null,
			/*duration*/null,
			/*resId*/ 	new int[] {R.drawable.tower_heavy_1, R.drawable.tower_heavy_2, 
					R.drawable.tower_heavy_3, R.drawable.tower_heavy_4, R.drawable.tower_heavy_5},
			/*n-type*/ 	new TowerEvoTree[][] {null, null, new TowerEvoTree[] {BOX}, null, new TowerEvoTree[] {CLUSTER} },
			/*ta-x*/	new int[] {6, 7, 0, 1, 2},
			/*ta-y*/	new int[] {0, 0, 1, 1, 1}
			);
	
	public static final TowerEvoTree DESIRE = new TowerEvoTree(
			TYPE_DESIRE,
			new String[] {"Desire 1", "Desire 2", "Desire 3", "Desire 4"},
			/*dmg*/ 	new int[] {20, 40, 60, 80},
			/*hasSS*/ 	new boolean[] {true, true, true, true},
			/*bs*/ 		null,
			/*range*/ 	new float[] {3f, 3f, 3f, 3f},
			/*aoe*/ 	null,
			/*as*/ 		new float[] {2f, 1.9f, 1.8f, 1.7f, 1.5f},
			/*cost*/ 	new int[] {300, 500, 600, 800},
			/*resell*/ 	null,
			/*duration*/null,
			/*resId*/ 	new int[] {R.drawable.tower_desire, R.drawable.tower_desire_2, 
					R.drawable.tower_desire_3, R.drawable.tower_desire_4},
			/*n-type*/ 	null,
			/*ta-x*/	new int[] {4, 5, 6, 7},
			/*ta-y*/	new int[] {2, 2, 2, 2}
			);
	
	public static final TowerEvoTree NULL = new TowerEvoTree(
			TYPE_NULL,
			new String[] {"Null", "Void"},
			/*dmg*/ 	new int[] {0, 0},
			/*hasSS*/ 	new boolean[] {false, false},
			/*bs*/ 		new float[] {8f, 8f},
			/*range*/ 	new float[] {3f, 3.5f},
			/*aoe*/ 	null,
			/*as*/ 		new float[] {1.8f, 1.6f},
			/*cost*/ 	new int[] {500, 600},
			/*resell*/ 	null,
			/*extra*/	new float[] {22f, 40f},
			/*resId*/ 	new int[] {R.drawable.tower_null, R.drawable.tower_void},
			/*n-type*/ 	null,
			/*ta-x*/	new int[] {0, 1},
			/*ta-y*/	new int[] {3, 3}
			);
	
	public static final TowerEvoTree NORMAL = new TowerEvoTree(
			TYPE_NORMAL,
			new String[] {"Normal 1", "Normal 2", "Normal 3", "Normal 4"},
			/*dmg*/ 	new int[] {20, 30, 40, 50},
			/*hasSS*/ 	new boolean[] {true, true, true, true},
			/*bs*/ 		null,
			/*range*/ 	new float[] {2f, 2f, 2f, 2f},
			/*aoe*/ 	null,
			/*as*/ 		new float[] {2.6f, 2.4f, 2.2f, 2f},
			/*cost*/ 	new int[] {200, 300, 300, 400},
			/*resell*/ 	null,
			/*duration*/null,
			/*resId*/ 	new int[] {R.drawable.tower_normal_2, 
					R.drawable.tower_normal_3, R.drawable.tower_normal_4, R.drawable.tower_normal_5},
			/*n-type*/ 	null,
			/*ta-x*/	new int[] {0, 1, 2, 3},
			/*ta-y*/	new int[] {2, 2, 2, 2}
			);
	
	public static final TowerEvoTree SPECIALIST = new TowerEvoTree(
			TYPE_SPECIALIST,
			new String[] {"Specialist"},
			/*dmg*/ 	new int[] {10},
			/*hasSS*/ 	new boolean[] {false},
			/*bs*/ 		new float[] {10f},
			/*range*/ 	new float[] {4f},
			/*aoe*/ 	null,
			/*as*/ 		new float[] {3f},
			/*cost*/ 	new int[] {200},
			/*resell*/ 	null,
			/*duration*/null,
			/*resId*/ 	new int[] {R.drawable.tower_normal},
			/*n-type*/ 	new TowerEvoTree[][] {null, new TowerEvoTree[] {NORMAL, DESIRE, NULL}},
			/*ta-x*/	new int[] {7},
			/*ta-y*/	new int[] {1}
			);
	
	public static final TowerEvoTree DEMO = new TowerEvoTree(
			TYPE_DEMO,
			new String[] {"Demo 1", "Demo 2"},
			/*dmg*/ 	new int[] {20, 40},
			/*hasSS*/ 	new boolean[] {true, true},
			/*bs*/ 		new float[] {10f, 10f},
			/*range*/ 	new float[] {3.5f, 4f},
			/*aoe*/ 	new float[] {1f, 1f},
			/*as*/ 		new float[] {3f, 3f},
			/*cost*/ 	new int[] {300, 400},
			/*resell*/ 	null,
			/*extra*/	new float[] {0.8f, 1f},
			/*resId*/ 	new int[] {R.drawable.tower_demo_1, R.drawable.tower_demo_2},
			/*n-type*/ 	null,
			/*ta-x*/	new int[] {4, 5},
			/*ta-y*/	new int[] {3, 3}
			);
	
	public static final TowerEvoTree FLAKE = new TowerEvoTree(
			TYPE_FLAKE,
			new String[] {"Flake 1", "Flake 2"},
			/*dmg*/ 	new int[] {10, 10},
			/*hasSS*/ 	new boolean[] {false, false},
			/*bs*/ 		new float[] {14f, 14f},
			/*range*/ 	new float[] {3.5f, 4f},
			/*aoe*/ 	new float[] {1.5f, 1.7f},
			/*as*/ 		new float[] {2.5f, 2f},
			/*cost*/ 	new int[] {300, 400},
			/*resell*/ 	null,
			/*extra*/	new int[] {60, 50},
			/*resId*/ 	new int[] {R.drawable.tower_flake_1, R.drawable.tower_flake_2},
			/*n-type*/ 	null,
			/*ta-x*/	new int[] {6, 7},
			/*ta-y*/	new int[] {3, 3}
			);
	
	public static final TowerEvoTree DIAMOND = new TowerEvoTree(
			TYPE_DIAMOND,
			new String[] {"Diamond"},
			/*dmg*/ 	new int[] {10},
			/*hasSS*/ 	new boolean[] {false},
			/*bs*/ 		new float[] {10f},
			/*range*/ 	new float[] {3f},
			/*aoe*/ 	null,
			/*as*/ 		new float[] {3f},
			/*cost*/ 	new int[] {200},
			/*resell*/ 	null,
			/*duration*/null,
			/*resId*/ 	new int[] {R.drawable.tower_diamond_1},
			/*n-type*/ 	new TowerEvoTree[][] {null, new TowerEvoTree[] {DEMO, FLAKE}},
			/*ta-x*/	new int[] {2},
			/*ta-y*/	new int[] {3}
			);
	
	public static final TowerEvoTree BRUTAL = new TowerEvoTree(
			TYPE_BRUTAL,
			new String[] {"Brutal 1", "Brutal 2", "Brutal 3"},
			/*dmg*/ 	new int[] {60, 90, 150},
			/*hasSS*/ 	new boolean[] {true, true, true},
			/*bs*/ 		new float[] {13f, 13f, 13f},
			/*range*/ 	new float[] {4f, 4.5f, 5f},
			/*aoe*/ 	null,
			/*as*/ 		new float[] {1.5f, 1.3f, 1f},
			/*cost*/ 	new int[] {300, 350, 400},
			/*resell*/ 	null,
			/*duration*/null,
			/*resId*/ 	new int[] {R.drawable.tower_brutal_1, R.drawable.tower_brutal_2, R.drawable.tower_brutal_3},
			/*n-type*/ 	null,
			/*ta-x*/	new int[] {1, 2, 3},
			/*ta-y*/	new int[] {4, 4, 4}
			);
	
	public static final TowerEvoTree DESOLATOR = new TowerEvoTree(
			TYPE_DESOLATOR,
			new String[] {"Desolator 1", "Desolator 2", "Desolator 3"},
			/*dmg*/ 	new int[] {20, 40, 80},
			/*hasSS*/ 	new boolean[] {true, true, true},
			/*bs*/ 		new float[] {10f, 10f, 10f},
			/*range*/ 	new float[] {3.2f, 3.4f, 3.6f},
			/*aoe*/ 	null,
			/*as*/ 		new float[] {2.5f, 2.4f, 2.3f},
			/*cost*/ 	new int[] {200, 350, 400},
			/*resell*/ 	null,
			/*duration*/null,
			/*resId*/ 	new int[] {R.drawable.tower_desolator_1, R.drawable.tower_desolator_2, R.drawable.tower_desolator_3},
			/*n-type*/ 	null,
			/*ta-x*/	new int[] {4, 5, 6},
			/*ta-y*/	new int[] {4, 4, 4}
			);
	
	public static final TowerEvoTree CIRCLE = new TowerEvoTree(
			TYPE_CIRCLE,
			new String[] {"Circle"},
			/*dmg*/ 	new int[] {5},
			/*hasSS*/ 	new boolean[] {false},
			/*bs*/ 		new float[] {10f},
			/*range*/ 	new float[] {3f},
			/*aoe*/ 	null,
			/*as*/ 		new float[] {2.5f},
			/*cost*/ 	new int[] {150},
			/*resell*/ 	null,
			/*duration*/null,
			/*resId*/ 	new int[] {R.drawable.tower_circle_1},
			/*n-type*/ 	new TowerEvoTree[][] {null, new TowerEvoTree[] {BRUTAL, DESOLATOR}},
			/*ta-x*/	new int[] {0},
			/*ta-y*/	new int[] {4}
			);
	
	private static final TowerEvoTree[] evoTrees = new TowerEvoTree[] {
		null,
		DYNAMIC,
		BOSS,
		REGULAR,
		HEAVY,
		CLUSTER,
		BOX,
		SPECIALIST,
		NORMAL,
		DESIRE,
		null,	// place holder...
		NULL,	// dmg dealt is based on how low the target hp is
		DIAMOND,
		DEMO,
		FLAKE,
		CIRCLE,
		BRUTAL,
		DESOLATOR
	};
	
	public static TowerEvoTree getEvolutionTree(int towerType) {
		return evoTrees[towerType];
	}
}
