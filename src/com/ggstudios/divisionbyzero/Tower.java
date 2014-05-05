package com.ggstudios.divisionbyzero;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import android.graphics.Rect;
import android.opengl.GLES20;

import com.ggstudios.divisionbyzero.FontManager.TextureRegion;
import com.ggstudios.divisionbyzero.TowerLibrary.TowerEvoTree;
import com.ggstudios.utils.BufferUtils;
import com.ggstudios.utils.DebugLog;

import static fix.android.opengl.GLES20.glVertexAttribPointer;

public class Tower extends PictureBox implements Updatable{
	private static final String TAG = "Tower";

	private static final int TEXTURE_ATLAS_WIDTH = 512;
	private static final int TEXTURE_ATLAS_HEIGHT = 512;
	private static final int TEXTURE_ATLAS_TILE_WIDTH = 64;
	private static final int TEXTURE_ATLAS_TILE_HEIGHT = 64;

	private static final float COOLOFF_TIME = 0.2f;

	TowerEvoTree evoTree;
	
	int level;
	private int towerType;
	private int cost;

	private int nextCost;
	private int totalCost = 0;

	private float attackSpeed;
	private float coolDown = 0.0f;

	private float rangeSquared;
	private float range;

	private int resellPrice;

	private boolean destroyed = false;
	private boolean enabled = true;

	private int totalDmgDealt = 0;

	private TextureRegion tr;

	int tileX, tileY;

	private static interface Special {
		void updateSpecial(float delta);
		void drawSpecial(float offX, float offY);
	}
	
	public static interface OnTowerChangeListener {
		void onDamageDealtChange(int totalDmgDealt);
		void onStatChange();
	}

	private OnTowerChangeListener towerChangeListener;
	private Special special;

	public void writeToStream(DataOutputStream stream) throws IOException { 
		stream.writeInt(evoTree.typeId);
		stream.writeInt(level);
		stream.writeInt(totalDmgDealt);
		stream.writeFloat(coolDown);
	}
	
	public Tower(int tileX, int tileY, DataInputStream stream) throws IOException {
		this(tileX, tileY, stream.readInt(), stream.readInt());
		
		totalDmgDealt = stream.readInt();
		coolDown = stream.readFloat();
	}
	
	public Tower(int tileX, int tileY, int type, int level) {
		this(tileX, tileY, TowerLibrary.getEvolutionTree(type), level);
	}
	
	public Tower(int tileX, int tileY, TowerEvoTree evoTree, int level) {
		super(0, 0);

		this.evoTree = evoTree;
		this.level = level;
		this.tileX = tileX;
		this.tileY = tileY;

		tr = new TextureRegion(TEXTURE_ATLAS_WIDTH, TEXTURE_ATLAS_HEIGHT, TEXTURE_ATLAS_TILE_WIDTH, TEXTURE_ATLAS_TILE_HEIGHT);

		refreshTowerInfo(false);
	}

	private void refreshTowerInfo(boolean textureChanged) {
		this.cost = evoTree.cost[level];
		this.towerType = evoTree.typeId;
		setTexture(evoTree.resId[level]);
		this.attackSpeed = evoTree.as[level];

		totalCost += cost;

		if(evoTree.resell != null && evoTree.resell.length > level) {
			resellPrice = evoTree.resell[level];
		} else {
			resellPrice = totalCost >> 1;
		}

		if(evoTree.maxLevel == level + 1) {
			nextCost = -1;
		} else {
			nextCost = evoTree.cost[level + 1];
		}

		setRange(evoTree.range[level] * Core.MAP_SDP);

		if(evoTree.hasSpecial[level]) {
			switch(towerType) {
			case TowerLibrary.TYPE_DYNAMIC:
				special = DYNAMIC_SPECIAL;
				break;
			case TowerLibrary.TYPE_BOSS:
				special = BOSS_SPECIAL;
				break;
			case TowerLibrary.TYPE_CLUSTER:
				special = CLUSTER_SPECIAL;
				break;
			case TowerLibrary.TYPE_BOX:
				special = BOX_SPECIAL;
				break;
			case TowerLibrary.TYPE_NORMAL:
				special = NORMAL_SPECIAL;
				break;
			case TowerLibrary.TYPE_DESIRE:
				special = DESIRE_SPECIAL;
				break;
			case TowerLibrary.TYPE_BRUTAL:
			case TowerLibrary.TYPE_DESOLATOR:
				special = BRUTAL_SPECIAL;	// set target to only ghosts...
				break;
			default:
				break;
			}
		}

		tr.setRegion(evoTree.taTileX[level] * TEXTURE_ATLAS_TILE_WIDTH, 
				evoTree.taTileY[level] * TEXTURE_ATLAS_TILE_HEIGHT);

		if(textureChanged)
			Core.game.towerManager.invalidate();
		
		if(towerChangeListener != null)
			towerChangeListener.onStatChange();
	}

	public void build(float w, float h) {
		this.w = w;
		this.h = h;

		generateBuffer();
	}

	private void setRange(float range) {
		this.range = range;
		this.rangeSquared = range * range;
	}

	@Override
	public void draw(float offX, float offY) {
		super.draw(offX, offY);

		if(special != null)
			special.drawSpecial(offX, offY);
	}

	public void drawSpecial(float offX, float offY) {
		if(special != null)
			special.drawSpecial(offX, offY);
	}

	public void drawSprite(float[] vertexBuffer, int offset, float offX, float offY)  {
		final float x1 = x + offX;
		final float y1 = y + offY;
		final float x2 = x1 + w;
		final float y2 = y1 + h;

		vertexBuffer[offset++] = x1;               // Add X for Vertex 0
		vertexBuffer[offset++] = y1;               // Add Y for Vertex 0
		vertexBuffer[offset++] = tr.u1;        // Add U for Vertex 0
		vertexBuffer[offset++] = tr.v1;        // Add V for Vertex 0

		vertexBuffer[offset++] = x2;               // Add X for Vertex 1
		vertexBuffer[offset++] = y1;               // Add Y for Vertex 1
		vertexBuffer[offset++] = tr.u2;        // Add U for Vertex 1
		vertexBuffer[offset++] = tr.v1;        // Add V for Vertex 1

		vertexBuffer[offset++] = x2;               // Add X for Vertex 2
		vertexBuffer[offset++] = y2;               // Add Y for Vertex 2
		vertexBuffer[offset++] = tr.u2;        // Add U for Vertex 2
		vertexBuffer[offset++] = tr.v2;        // Add V for Vertex 2

		vertexBuffer[offset++] = x1;               // Add X for Vertex 3
		vertexBuffer[offset++] = y2;               // Add Y for Vertex 3
		vertexBuffer[offset++] = tr.u1;        // Add U for Vertex 3
		vertexBuffer[offset++] = tr.v2;        // Add V for Vertex 3
	}

	private Sprite getTarget() {
		List<Sprite> list = Core.game.spriteMgr.getRawList();
		final int len = Core.game.spriteMgr.size();
		for(int i = 0; i < len; i++) {
			Sprite sprite = list.get(i);
			
			if(!sprite.isTargetable()) continue;

			final float sX = sprite.x, sY = sprite.y;
			final float u = (sX - x)*(sX - x);
			final float v = (sY - y)*(sY - y);

			if(rangeSquared > u + v){
				return sprite;
			}
		}
		return null;
	}

	private Sprite getGhostTarget() {
		List<Sprite> list = Core.game.spriteMgr.getRawList();
		final int len = Core.game.spriteMgr.size();
		for(int i = 0; i < len; i++) {
			Sprite sprite = list.get(i);

			if(!sprite.isGhost() || !sprite.isTargetable()) continue;

			final float sX = sprite.x, sY = sprite.y;
			final float u = (sX - x)*(sX - x);
			final float v = (sY - y)*(sY - y);

			if(rangeSquared > u + v){
				return sprite;
			}
		}
		return null;
	}

	@Override
	public boolean update(float dt) {
		if(!enabled) return true;
		if(special != null) {
			special.updateSpecial(dt);
		} else if(coolDown <= 0) {
			Sprite target = getTarget();

			if(target != null) {
				// fire
				Bullet b = Core.game.obtainBullet();
				b.parent = this;
				switch(towerType) {
				case TowerLibrary.TYPE_HEAVY:
					b.setTexture(R.drawable.bullet_heavy);

					b.setup(x, y, target, evoTree.dmg[level], 
							evoTree.bs[level] * Core.MAP_SDP, Bullet.TYPE_AOE, evoTree.aoe[level] * Core.MAP_SDP);
					break;
				case TowerLibrary.TYPE_FLAKE:
					b.setTexture(R.drawable.demo_flake_shot);

					b.setup(x, y, target, evoTree.dmg[level], 
							evoTree.bs[level] * Core.MAP_SDP, Bullet.TYPE_COMPLEX_AOE_FROST,
							evoTree.aoe[level] * Core.MAP_SDP, /* Slow percentage */ ((int[])evoTree.extra)[level]);
					b.setState(Sprite.STATE_SLOW);
					b.setDuration(SLOW_TIME);
					break;
				case TowerLibrary.TYPE_DEMO:
					b.setTexture(R.drawable.demo_shot);

					b.setup(x, y, target, evoTree.dmg[level], 
							evoTree.bs[level] * Core.MAP_SDP, Bullet.TYPE_COMPLEX_AOE_STUN,
							evoTree.aoe[level] * Core.MAP_SDP);
					b.setState(Sprite.STATE_STUN);
					b.setDuration(/* stun time */ ((float[])evoTree.extra)[level]);
					break;
				case TowerLibrary.TYPE_NULL:
					b.setTexture(R.drawable.bullet);

					b.setup(x, y, target, evoTree.dmg[level], 
							evoTree.bs[level] * Core.MAP_SDP, Bullet.TYPE_SEEKING, 
							/* dmg multiplier */((float[])evoTree.extra)[level]);
					b.setState(Sprite.STATE_DECAY);
					break;
				default:
					b.setTexture(R.drawable.bullet);

					b.setup(x, y, target, evoTree.dmg[level], 
							evoTree.bs[level] * Core.MAP_SDP, Bullet.TYPE_NORMAL);
					break;
				}


				Core.gu.addGameUpdatable(b);
				b.isVisible = true;

				coolDown = attackSpeed;
			} else {
				coolDown = COOLOFF_TIME;
			}
		}

		coolDown -= dt;
		return !destroyed;
	}

	public int getUpgradeCost() {
		return nextCost;
	}

	public void upgrade() {
		if(evoTree.maxLevel == level + 1) {
			DebugLog.e(TAG, "Attempting to upgrade tower over bounds");
		} else {
			level++;
		}

		refreshTowerInfo(true);
	}

	public void upgrade(int selection) {
		if(evoTree.maxLevel <= level + 1) {
			selection++;
		}
		if(selection == 0) {
			upgrade();
			return;
		}
		evoTree = evoTree.typeUpgrade[level + 1][selection - 1];
		level = 0;

		refreshTowerInfo(true);
	}

	public boolean isMaxLevel() {
		return evoTree.maxLevel == level + 1;
	}
	
	public void setTowerChangeListener(OnTowerChangeListener listener) {
		towerChangeListener = listener;
	}

	public int getSellPrice() {
		return resellPrice;
	}

	public void destroy() {
		destroyed = true;
	}

	public float getRange() {
		return range;
	}
	
	/**
	 * Gets the tower range in grid units.
	 * @return Tower range in grid units.
	 */
	public float getGridRange() {
		return range/Core.MAP_SDP;
	}

	public boolean hasTypeUpgrade() {
		return evoTree.typeUpgrade != null && evoTree.typeUpgrade.length > level + 1 && evoTree.typeUpgrade[level + 1] != null;
	}

	public int getDamage() {
		return evoTree.dmg[level];
	}

	public int getCost() {
		return cost;
	}

	private static int scopeTexture;
	private static int bulletTexture;
	private static int handle;

	private static int boxBurnTextureHandle;
	private static int boxBurnVertexHandle;

	private static int normalPulseTextureHandle;
	private static int normalPulseVertexHandle;

	private static int desireLaserTextureHandle;
	private static int desireLaserTextureHandle2;
	private static int desireLaserVertexHandle;

	private static final float SCOPE_LENGTH = 1f;

	public static final float SLOW_TIME = 4f;
	
	private static float scopeLength;

	public static void init() {
		{
			scopeLength = Core.SDP * SCOPE_LENGTH;

			final float h = Core.SDP / 16f;

			final float arr[] = {
					0, -h,
					scopeLength, -h,
					0, h,
					scopeLength, h
			};

			scopeTexture = Core.tm.get(R.drawable.sniper_asset);
			bulletTexture = Core.tm.get(R.drawable.bullet);
			handle = BufferUtils.copyToBuffer(arr);
		}

		{
			// box stuff...
			final float off = -(Core.MAP_SDP * 0.1f);
			final float size = (Core.MAP_SDP * 1.1f);

			final float arr[] = {
					off, off,
					size, off,
					off, size,
					size, size
			};

			boxBurnTextureHandle = Core.tm.get(R.drawable.box_burn);
			boxBurnVertexHandle = BufferUtils.copyToBuffer(arr);
		}

		{
			final float range = Core.MAP_SDP * 2.0f;

			// normal stuff...
			final float arr[] = {
					-range, -range, 	//Vertex 0
					range, 	-range, 	//v1
					-range, range, 		//v2
					range, 	range, 		//v3
			};

			normalPulseTextureHandle = Core.tm.get(R.drawable.normal_pulse);
			normalPulseVertexHandle = BufferUtils.copyToBuffer(arr);
		}

		{
			final float size = Core.MAP_SDP * 1f;
			final float h = size / 2f;

			final float arr[] = {
					-h, 0,
					h, 0,
					-h, -size,
					h, -size
			};

			desireLaserTextureHandle = Core.tm.get(R.drawable.lazer_background);
			desireLaserTextureHandle2 = Core.tm.get(R.drawable.lazer_overlay);
			desireLaserVertexHandle = BufferUtils.copyToBuffer(arr);
		}
	}

	private final Special DYNAMIC_SPECIAL = new Special() {
		// imitate sniper effect...
		static final float FADE_TIME = 0.5f;
		float fadeCounter = 0.0f;

		boolean showEff;
		float angle;
		Sprite target = null;

		float targetDistance = 0f;

		@Override
		public void updateSpecial(float delta) {
			final float cd = coolDown;
			if(target != null) {
				if(target.isAlive())
					angle = Utils.fastatan2(x - target.x, y - target.y);
				else target = null;
			} else if(cd <= 1) {
				target = getTarget();
				if(target != null) {
					angle = Utils.fastatan2(x - target.x, y - target.y);
					showEff = true;
				}
			} else {
				showEff = false;
			}

			if(cd <= 0) {
				coolDown = attackSpeed;

				if(target != null) {
					fadeCounter = FADE_TIME;
					target.hitBy(Tower.this);
					targetDistance = (float) (Math.sqrt((target.x - x) * (target.x - x) + (target.y - y) * (target.y - y)) / scopeLength);
				}

				target = null;
				showEff = false;
			}

			if(fadeCounter > 0f) {
				fadeCounter -= delta;
			}
		}

		@Override
		public void drawSpecial(float offX, float offY) {
			final float finalX = x + offX;
			final float finalY = y + offY;

			if(showEff){
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, scopeTexture);

				Utils.resetMatrix();
				Utils.rotate(Utils.PI/2.0f + angle);
				Utils.translateAndCommit(finalX, finalY);

				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, handle);
				glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, 0, 0);

				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
			}

			if(fadeCounter > 0f) {
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bulletTexture);

				Utils.resetMatrix();
				Utils.scaleW(targetDistance);
				Utils.rotate(Utils.PI/2.0f + angle);
				Utils.translateAndCommit(finalX, finalY);

				GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1.0f, 1.0f, 1.0f,( fadeCounter/FADE_TIME));
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, handle);
				glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, 0, 0);

				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

				GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1.0f, 1.0f, 1.0f, 1.0f);
			}
		}

	};

	private final Special BOSS_SPECIAL = new Special() {

		int positionIndex = 0;
		float[] position = {
				-0.5f, -0.5f,
				0.5f, -0.5f,
				-0.5f, 0f,
				0.5f, 0f,
				-0.5f, 0.5f,
				0.5f, 0.5f
		};

		static final int MAX_POSITION = 6;

		@Override
		public void updateSpecial(float delta) {
			if(coolDown <= 0) {
				Sprite target = getTarget();

				if(target != null) {
					// fire
					Bullet b = Core.game.obtainBullet();
					b.setTexture(R.drawable.bullet);
					b.setup(x + position[positionIndex<<1] * w, y + position[(positionIndex<<1) + 1] * h, target, evoTree.dmg[level], 
							evoTree.bs[level] * Core.SDP, Bullet.TYPE_NORMAL);
					b.parent = Tower.this;

					Core.gu.addGameUpdatable(b);
					b.isVisible = true;

					coolDown = attackSpeed;

					positionIndex++;
					if(positionIndex == MAX_POSITION) {
						positionIndex = 0;
					}
				}
			}
		}

		@Override
		public void drawSpecial(float offX, float offY) { }

	};

	private final Special CLUSTER_SPECIAL = new Special() {
		private static final float BURST_SPEED = 0.2f;

		private int burstLeft = 0;
		private float burstCd = BURST_SPEED;

		private boolean startBurst = false;

		@Override
		public void updateSpecial(float delta) {
			if(coolDown <= 0) {
				burstLeft = (Integer) evoTree.extra;

				Sprite target = getTarget();

				if(target != null) {
					Bullet b = Core.game.obtainBullet();

					b.parent = Tower.this;

					b.setTexture(R.drawable.bullet_heavy);

					b.setup(x, y, target, evoTree.dmg[level], 
							evoTree.bs[level] * Core.SDP, Bullet.TYPE_AOE_SEEKING, evoTree.aoe[level] * Core.MAP_SDP);

					Core.gu.addGameUpdatable(b);
					b.isVisible = true;

					coolDown = attackSpeed;
					burstLeft--;

					startBurst = true;
				}
			}

			if(startBurst) {
				burstCd -= delta;
			}

			if(burstCd <= 0f) {
				Sprite target = getTarget();

				if(target != null) {
					Bullet b = Core.game.obtainBullet();

					b.parent = Tower.this;

					b.setTexture(R.drawable.bullet_heavy);

					b.setup(x, y, target, evoTree.dmg[level], 
							evoTree.bs[level] * Core.SDP, Bullet.TYPE_AOE_SEEKING, evoTree.aoe[level] * Core.MAP_SDP);

					Core.gu.addGameUpdatable(b);
					b.isVisible = true;

					burstCd = BURST_SPEED;
					burstLeft--;

					if(burstLeft == 0) {
						startBurst = false;
					}
				}
			}
		}

		@Override
		public void drawSpecial(float offX, float offY) {
		}

	};

	private final Special BOX_SPECIAL = new Special() {
		private static final float BURN_DURATION = 2f;

		private Rect rect = new Rect();
		private float effectTimeLeft = 0f; 
		private float burnCd = 0f; 

		private static final float TRANSPARENCY_MAX = 0.7f, TRANSPARENCY_MIN = 0.3f;
		private float transparency = TRANSPARENCY_MIN;
		private boolean fading = false;

		@Override
		public void updateSpecial(float delta) {
			if(coolDown <= 0) {
				Sprite target = getTarget();

				if(target != null) {
					final float l = (int)(target.x / Core.MAP_SDP) * Core.MAP_SDP;
					final float t = (int)(target.y / Core.MAP_SDP) * Core.MAP_SDP;

					rect.left = (int) l;
					rect.top = (int) t;
					rect.bottom = (int) (rect.top + Core.MAP_SDP);
					rect.right = (int) (rect.left + Core.MAP_SDP);

					effectTimeLeft = BURN_DURATION;

					coolDown = attackSpeed;

					transparency = 0f;
					burnCd = 0f;
				}
			}

			if(effectTimeLeft > 0) {
				effectTimeLeft -= delta;
				if(burnCd > 0)
					burnCd -= delta;

				if(burnCd <= 0) {
					burnCd = (Float) evoTree.extra;

					List<Sprite> list = Core.game.spriteMgr.getRawList();
					final int len = Core.game.spriteMgr.size();
					for(int i = 0; i < len; i++) {
						Sprite sprite = list.get(i);
						if(!sprite.isTargetable()) continue;

						if(rect.contains((int)sprite.x, (int)sprite.y)) {
							sprite.hitBy(Tower.this);
						}
					}
				}

				if(fading) {
					transparency -= delta / 2f;
					if(transparency <= TRANSPARENCY_MIN) {
						transparency = TRANSPARENCY_MIN;
						fading = false;
					}
				} else {
					transparency += delta / 2f;
					if(transparency >= TRANSPARENCY_MAX) {
						transparency = TRANSPARENCY_MAX;
						fading = true;
					}
				}
			}
		}

		@Override
		public void drawSpecial(float offX, float offY) {
			final float finalX = rect.left + offX;
			final float finalY = rect.top + offY;

			if(effectTimeLeft > 0) {
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, boxBurnTextureHandle);

				Utils.resetMatrix();
				Utils.translateAndCommit(finalX, finalY);

				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, boxBurnVertexHandle);
				glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, 0, 0);

				GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1.0f, 1.0f, 1.0f, transparency);

				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

				GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1.0f, 1.0f, 1.0f, 1.0f);
			}
		}

	};

	private final Special NORMAL_SPECIAL = new Special() {
		private static final float FADE_DURATION = 0.4f;	// in seconds
		private static final float EFFECT_DURATION = 0.4f;	// in seconds

		private static final float TOTAL_EFFECT_DURATION = FADE_DURATION + EFFECT_DURATION;

		private boolean showEffect = false;
		private float effectTimeLeft = 0f;
		private float transparency = 1f;
		private float scale = 0f;

		private boolean scaling = false;

		@Override
		public void updateSpecial(float delta) {
			if(showEffect) {
				effectTimeLeft -= delta;

				if(effectTimeLeft <= 0)
					showEffect = false;
				else if (effectTimeLeft > FADE_DURATION) {
					scaling = true;

					scale = (TOTAL_EFFECT_DURATION - effectTimeLeft) / EFFECT_DURATION;
					scale *= scale;
				} else {
					if(scaling) {
						scaling = false;

						List<Sprite> list = Core.game.spriteMgr.getRawList();
						final int len = Core.game.spriteMgr.size();
						for(int i = 0; i < len; i++) {
							Sprite sprite = list.get(i);
							
							if(!sprite.isTargetable()) continue;

							final float s_x = sprite.x, s_y = sprite.y;
							final float u = (s_x - x)*(s_x - x);
							final float v = (s_y - y)*(s_y - y);

							if(rangeSquared > u + v){
								sprite.hitBy(Tower.this);
							}
						}
					}

					scale = 1f;
					transparency = effectTimeLeft / FADE_DURATION;
				}
			}

			if(coolDown <= 0) {
				boolean hitSomething = false;

				List<Sprite> list = Core.game.spriteMgr.getRawList();
				final int len = Core.game.spriteMgr.size();
				for(int i = 0; i < len; i++) {
					Sprite sprite = list.get(i);

					final float s_x = sprite.x, s_y = sprite.y;
					final float u = (s_x - x)*(s_x - x);
					final float v = (s_y - y)*(s_y - y);

					if(rangeSquared > u + v){
						hitSomething = true;
						break;
					}
				}

				if(hitSomething) {
					effectTimeLeft = TOTAL_EFFECT_DURATION;
					coolDown = attackSpeed;
					transparency = 1f;
					scale = 0f;

					showEffect = true;
				}
			}
		}

		@Override
		public void drawSpecial(float offX, float offY) {
			if(showEffect){
				final float finalX = x + offX;
				final float finalY = y + offY;

				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, normalPulseTextureHandle);
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, normalPulseVertexHandle);

				Utils.resetMatrix();
				Utils.scale(scale);
				Utils.translateAndCommit(finalX, finalY);

				glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, 0, 0);

				GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1.0f, 1.0f, 1.0f, transparency);

				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
				GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1.0f, 1.0f, 1.0f, 1.0f);
			}
		}

	};

	private final Special DESIRE_SPECIAL = new Special() {
		private static final float FADE_TIME = 0.8f;

		private float effectTimeLeft = 0f;

		private float angle;

		private float len;

		@Override
		public void updateSpecial(float delta) {
			if(coolDown <= 0) {
				Sprite target = getTarget();

				if(target != null) {
					// there was a sprite in range...

					angle = (float) Utils.fastatan2(x - target.x, y - target.y);

					//calc who was hit
					float dX = target.x - x; 
					float dY = target.y - y;

					final float multiplier = Math.abs(Core.game.map.getWidth() / dX);

					dX *= multiplier;
					dY *= multiplier;

					final float a = dX*dX + dY*dY;

					len = (float) (Math.sqrt(a) / Core.MAP_SDP);

					List<Sprite> list = Core.game.spriteMgr.getRawList();
					final int len = Core.game.spriteMgr.size();
					for(int i = 0; i < len; i++) {
						Sprite sprite = list.get(i);

						if(!sprite.isTargetable()) continue;

						float fX = x - sprite.x;
						float fY = y - sprite.y;

						float b = 2*(fX*dX + fY*dY);
						float c = (fX*fX + fY*fY) - sprite.h * sprite.h;

						float discriminant = b*b-4*a*c;

						if( discriminant >= 0 ) {
							discriminant = (float) Math.sqrt( discriminant );
							float t1 = (-b + discriminant)/(2*a);

							if( t1 >= 0 && t1 <= 1 ) {
								sprite.hitBy(Tower.this);
							}
						}
					}

					coolDown = attackSpeed;

					effectTimeLeft = FADE_TIME;
				}
			}

			if (effectTimeLeft > 0) {
				effectTimeLeft -= delta;
			}
		}

		@Override
		public void drawSpecial(float offX, float offY) {
			if(effectTimeLeft > 0){
				final float interpolatedTime = (effectTimeLeft / FADE_TIME);
				final float transparency = -1f * interpolatedTime *(interpolatedTime-2);

				final float finalX = x + offX;
				final float finalY = y + offY;

				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, desireLaserTextureHandle);
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, desireLaserVertexHandle);

				Utils.resetMatrix();
				Utils.scaleH(len);
				Utils.rotate(angle);
				Utils.translateAndCommit(finalX, finalY);

				GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);

				glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, 0, 0);

				GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1.0f, 0.0f, 0.0f, transparency);

				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

				GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1.0f, 1.0f, 1.0f, transparency);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, desireLaserTextureHandle2);
				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

				GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1.0f, 1.0f, 1.0f, 1.0f);

				Core.gr.resetBlendFunc();
			}
		}

	};

	private final Special BRUTAL_SPECIAL = new Special() {

		@Override
		public void updateSpecial(float delta) {
			if(coolDown <= 0) {
				Sprite target = getGhostTarget();

				if(target != null) {
					// fire
					Bullet b = Core.game.obtainBullet();
					b.parent = Tower.this;

					if (towerType == TowerLibrary.TYPE_BRUTAL) {	
						b.setTexture(R.drawable.bullet);

						b.setup(x, y, target, evoTree.dmg[level], 
								evoTree.bs[level] * Core.MAP_SDP, Bullet.TYPE_GHOST);
					} else {
						b.setTexture(R.drawable.bullet);

						b.setup(x, y, target, evoTree.dmg[level], 
								evoTree.bs[level] * Core.MAP_SDP, Bullet.TYPE_GHOST_AOE,
								evoTree.aoe[level] * Core.MAP_SDP);
					}


					Core.gu.addGameUpdatable(b);
					b.isVisible = true;

					coolDown = attackSpeed;
				} else {
					coolDown = COOLOFF_TIME;
				}
			}
		}

		@Override
		public void drawSpecial(float offX, float offY) {}

	};

	public void updateDamageDealt(int dmg) {
		totalDmgDealt += dmg;
		
		if(towerChangeListener != null) {
			towerChangeListener.onDamageDealtChange(totalDmgDealt);
		}
	}

	public int getTotalDmgDealt() {
		return totalDmgDealt;
	}

	public String getName() {
		return evoTree.name[level];
	}

	public float getAttackSpeed() {
		return attackSpeed;
	}

	public int getTowerType() {
		return evoTree.typeId;
	}

	public int getLevel() {
		return level;
	}
	
	public String getDescription() {
		return evoTree.getDescription();
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
