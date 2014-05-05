package com.ggstudios.divisionbyzero;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

import android.graphics.Rect;
import android.opengl.GLES20;

import com.ggstudios.divisionbyzero.PathFinder.Node;
import com.ggstudios.divisionbyzero.SpriteAnimation.OnAnimationComplete;
import com.ggstudios.utils.DebugLog;

import static fix.android.opengl.GLES20.glVertexAttribPointer;

/**
 * Enemy sprite class. Drawable.
 * Note that the drawable is centered.
 * @author iDunnololz
 *
 */
public class Sprite extends PictureBox implements Updatable{
	private static final String TAG = "Sprite";

	private static int WEAKNESS_TEXTURE = -1;

	// flags for sprite state...
	public static final int STATE_OK		= 0x00000000;
	public static final int STATE_STUN		= 0x00000001;
	public static final int STATE_SLOW		= 0x00000002;
	public static final int STATE_IMMUNE	= 0x00000004;
	public static final int STATE_BOSS		= 0x00000008;
	public static final int STATE_DEATH		= 0x00000010;
	public static final int STATE_BURN		= 0x00000020;
	public static final int STATE_WEAKNESS	= 0x00000040;
	public static final int STATE_DECAY 	= 0x00000080;
	
	public static final int
	TYPE_REGULAR = 1,
	TYPE_SPEEDLING = 2,
	TYPE_HEAVY = 3,
	TYPE_GHOST = 4,
	TYPE_SPLITTER = 5,
	TYPE_MINI = 6 /* spawned by splitter */,
	TYPE_UNDYING = 7,
	TYPE_REGENERATOR = 8;

	public static final int
	/*
	 * TYPE_BOSS is used as a check to see if a sprite is a boss.
	 * All boss units have the following effects:
	 * 	- Halves the duration of all status changing effects
	 *  - God armor - Reduces all incoming damage by 60%
	 */
	TYPE_BOSS = 999,
	/*
	 * Regular boss is the base boss type. It has no other special abilities.
	 */
	TYPE_BOSS_REGULAR = 1000,
	/*
	 * Extremely fast boss.
	 */
	TYPE_BOSS_REALLY_FAST = 1001,
	/*
	 * Supreme heavy boss completely ignores all status changing effects.
	 */
	TYPE_BOSS_SUPREME_HEAVY = 1002,
	/*
	 * Hopper boss can "hop" over towers!
	 */
	TYPE_BOSS_HOPPER = 1010;
	
	private float pathOffX, pathOffY;

	private float velocity;
	private float speedX, speedY;

	private float hp;
	private float maxHp;
	
	private int state = STATE_OK;
	private float slowAmount = 0f;
	private float slowDuration = 0f;
	private float weaknessDuration = 0f;
	private float stunDuration = 0f;

	private int gold;
	private int type;

	private boolean partiallyLoaded = false;

	private Node lastWayPoint;
	private Node currentWayPoint;

	// public for fast collision detection
	public Rect rect = new Rect();

	private float timeLeft;

	private static final float HP_HEIGHT = 0.1f;
	private int hpTexture;
	private float percentHp;
	
	private boolean isAlive = true;
	private boolean obstacle = true;
	private boolean targetable = true;

	private boolean hasDeathAnimation = false;
	private int pathIndex;
	
	private SpriteAnimation animation;
	private SpriteAnimation spawnAnimation;
	private SpriteAnimation deathAnimation;

	private static final SpriteAnimation ANIMATION_NORMAL = new SpriteAnimation();
	private static final SpriteAnimation ANIMATION_REGENERATOR = new SpriteAnimation();

	private Random random;
	
	private Map map;

	public static void doOnce() {
		Core.gu.addGameUpdatable(ANIMATION_NORMAL);
		Core.gu.addGameUpdatable(ANIMATION_REGENERATOR);
	}

	public static void refreshResources() {
		WEAKNESS_TEXTURE = Core.tm.get(R.drawable.status_weakness);

		ANIMATION_NORMAL.reset();

		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0001, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0002, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0003, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0004, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0005, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0006, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0007, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0008, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0009, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0010, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0011, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0012, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0013, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0014, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0015, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0016, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0017, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0018, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0019, 0.033f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0020, 0.033f);
		
		ANIMATION_REGENERATOR.reset();
		
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0001, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0002, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0003, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0004, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0005, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0006, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0007, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0008, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0009, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0010, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0011, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0012, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0013, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0014, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0015, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0016, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0017, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0018, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0019, 0.1f);
		ANIMATION_REGENERATOR.addFrame(R.drawable.sprite_regenerator0020, 0.1f);
	}

	public Sprite() {
		super(0, 0);

		random = StateManager.getInstance().getRandom();
		deathAnimation = new SpriteAnimation();
		deathAnimation.setLoop(false);
		spawnAnimation = new SpriteAnimation();
		spawnAnimation.setLoop(false);
	}
	
	public void preload(float hp, int gold, int type) {
		this.hp = hp;
		maxHp = hp;
		this.gold = gold;
		this.type = type;
		
		targetable = true;

		this.partiallyLoaded = true;

		this.percentHp = 1.0f;
	}

	boolean needToLoad() {
		return partiallyLoaded;
	}

	@Override
	public void draw(float offX, float offY) {
		if(animation != null)
			setTextureHandle(animation.getTextureHandle());
		
		super.draw(offX, offY);

		if(!cull) {
			if((state & STATE_WEAKNESS) != 0) {
				float m0 = Core.matrix[0]; 
				float m1 = Core.matrix[1]; 
				float m5 = Core.matrix[5]; 
				float m4 = Core.matrix[4]; 

				Utils.rotate(weaknessDuration);
				// commit matrix change
				GLES20.glUniformMatrix4fv(Core.U_TRANSLATION_MATRIX_HANDLE, 1, false, Core.matrix, 0);

				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, WEAKNESS_TEXTURE);

				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, handle);
				glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, 0, 0);

				GLES20.glBlendFunc(GLES20.GL_DST_COLOR, GLES20.GL_ONE_MINUS_SRC_ALPHA);
				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

				Core.gr.resetBlendFunc();

				Core.matrix[0] = m0;
				Core.matrix[1] = m1;
				Core.matrix[5] = m5;
				Core.matrix[4] = m4;
			}

			Core.matrix[0] = percentHp;
			Core.matrix[5] = HP_HEIGHT;
			Core.matrix[7] -= (h/2);
			// commit matrix change
			GLES20.glUniformMatrix4fv(Core.U_TRANSLATION_MATRIX_HANDLE, 1, false, Core.matrix, 0);

			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, hpTexture);

			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		}
	}

	@Override
	public void refresh() {
		super.refresh();

		hpTexture = Core.tm.get(R.drawable.hp_bar);
	}

	public void load(Map map) {
		this.map = map;
		partiallyLoaded = false;
		currentWayPoint = map.getPathStart(pathIndex);
		
		isVisible = true;
		
		switch(type) {
		case TYPE_REGULAR:
			super.setVBO(Core.GeneralBuffers.map_half_tile);

			animation = ANIMATION_NORMAL;
			break;
		case TYPE_SPEEDLING:
			super.setVBO(Core.GeneralBuffers.map_half_tile);

			animation = ANIMATION_NORMAL;
			break;
		case TYPE_HEAVY:
			super.setVBO(Core.GeneralBuffers.map_tile);

			animation = ANIMATION_NORMAL;
			break;
		case TYPE_GHOST:
			super.setVBO(Core.GeneralBuffers.map_half_tile); 
			super.transparency = 0.5f;

			animation = ANIMATION_NORMAL;

			obstacle = false;

			// ghosts have a special path
			currentWayPoint = map.getGhostPathStart(pathIndex);
			break;
		case TYPE_SPLITTER:
			super.setVBO(Core.GeneralBuffers.map_tile);
			super.setScale(0.7f);

			animation = ANIMATION_NORMAL;
			break;
		case TYPE_MINI:
			super.setVBO(Core.GeneralBuffers.map_half_tile);
			super.setScale(0.5f);

			animation = ANIMATION_NORMAL;
			break;
		case TYPE_UNDYING:
			super.setVBO(Core.GeneralBuffers.map_half_tile);
			super.setScale(1f);
			
			animation = ANIMATION_NORMAL;
			
			spawnAnimation.addFrame(R.drawable.sprite_undying0002, 0.05f);
			spawnAnimation.addFrame(R.drawable.sprite_undying0003, 0.05f);
			spawnAnimation.addFrame(R.drawable.sprite_undying0004, 0.05f);
			spawnAnimation.addFrame(R.drawable.sprite_undying0005, 0.05f);
			spawnAnimation.addFrame(R.drawable.sprite_undying0006, 0.05f);
			spawnAnimation.addFrame(R.drawable.sprite_undying0007, 0.05f);
			spawnAnimation.addFrame(R.drawable.sprite_undying0008, 0.05f);
			spawnAnimation.addFrame(R.drawable.sprite_undying0009, 0.05f);
			spawnAnimation.addFrame(R.drawable.sprite_undying0010, 0.05f);
			spawnAnimation.addFrame(R.drawable.sprite_undying0011, 0.05f);
			spawnAnimation.addFrame(R.drawable.sprite_undying0012, 0.05f);
			spawnAnimation.addFrame(R.drawable.sprite_undying0013, 0.05f);
			spawnAnimation.addFrame(R.drawable.sprite_undying0014, 0.05f);
			spawnAnimation.addFrame(R.drawable.sprite_undying0015, 0.05f);
			spawnAnimation.addFrame(R.drawable.sprite_undying0016, 0.05f);
			break;
		case TYPE_REGENERATOR: 
			super.setVBO(Core.GeneralBuffers.map_half_tile);
			super.setScale(1f);
			
			animation = ANIMATION_REGENERATOR;
			break;
		default:
			DebugLog.e(TAG, "Sprite type does not exist. Type: " + type);
			break;
		}
		
		velocity = STATS[type].speed * Core.MAP_SDP;

		float randomness = (Core.MAP_SDP - vbo.width) / Core.MAP_SDP;
		float base = (1f - randomness) / 2f;

		pathOffX = random.nextFloat() * randomness + base;
		pathOffY = random.nextFloat() * randomness + base;

		x = (currentWayPoint.x + pathOffX) * Core.MAP_SDP;
		y = (currentWayPoint.y + pathOffY) * Core.MAP_SDP;

		updateRect();

		timeLeft = 0.0f;

		refresh();
	}

	private void updateRect() {
		rect.left = (int) (x - w / 2.0f);
		rect.top = (int) (y - h / 2.0f);
		rect.right = (int) (rect.left + w);
		rect.bottom = (int) (rect.top + h);
	}

	@Override
	public boolean update(float dt) {
		if(!isAlive) {
			targetable = false;
			state = STATE_OK;
			
			if(type == TYPE_UNDYING && spawnAnimation != null) {
				final SpriteAnimation ani = animation;
				animation = spawnAnimation;
				spawnAnimation.setOnAnimationCompleteListener(new OnAnimationComplete() {

					@Override
					public void onAnimationComplete() {
						spawnAnimation = null;
						
						// resurrect this sprite
						isAlive = true;
						targetable = true;
						animation = ani;
						hp = maxHp;
						percentHp = 1f;
						Core.gu.addGameUpdatable(Sprite.this);
					};
					
				});
				Core.gu.addGameUpdatable(spawnAnimation);
			} else {
				Core.game.reportKilledSprite(this);
			}
		
			if(type == TYPE_SPLITTER) {
				// spawn 4 MINIs each with 1/4th the hp
				// and no gold bonus...
				
				for(int i = 0; i < 4; i++) {
					Sprite s = Core.game.spriteMgr.obtain();
					
					s.preload(maxHp / 4, 0, TYPE_MINI);

					Core.game.addEnemy(s);
					
					// we have to do this after adding enemy 
					// as adding the enemy will auto set it's position
					s.x = this.x;
					s.y = this.y;
					
					if(this.lastWayPoint == null) {
						// if we somehow lost our parent node, get the closest node to this unit...
						s.currentWayPoint = map.getClosestNode(s.x, s.y, pathIndex);
					} else
						s.currentWayPoint = this.lastWayPoint;
				}
			}
			return false;
		}

		if(state != STATE_OK) {
			if((state & STATE_SLOW) != 0) {
				dt *= slowAmount;
				slowDuration -= dt;
				
				if(slowDuration <= 0f) {
					state -= STATE_SLOW;
				}
			}
			if((state & STATE_WEAKNESS) != 0) {
				weaknessDuration -= dt;
				
				if(weaknessDuration <= 0f) {
					state -= STATE_WEAKNESS;
				}
			}
			
			// this status should be the last status to be
			// processes because processing stun will cause
			// the function to return prematurely
			if((state & STATE_STUN) != 0) {
				stunDuration -= dt;
				
				if(stunDuration <= 0f) {
					state -= STATE_STUN;
				}
				return true;
			}
		}
		
		if(type == TYPE_REGENERATOR && hp < maxHp) {
			float regenerationRate;	// in hp/second
			
			// regenerators regenerate 1/10th of their hp every second...
			regenerationRate = (maxHp / 10f);
			
			hp += regenerationRate * dt;
			percentHp = (float)hp / maxHp;
		}

		timeLeft -= dt;

		if(timeLeft <= 0) {
			lastWayPoint = currentWayPoint;
			currentWayPoint = currentWayPoint.parent;

			if(currentWayPoint == null) {
				// we have reached the end of the path...
				Core.game.reportFinishedSprite(this);

				isAlive = false;
				return false;
			} else {
				final float deltaX = (currentWayPoint.x + pathOffX) * Core.MAP_SDP - x;
				final float deltaY = (currentWayPoint.y + pathOffY) * Core.MAP_SDP - y;
				final float angle = Utils.fastatan2(deltaX, deltaY);
				speedX = (float) (Math.sin(angle) * velocity);
				speedY = (float) (Math.cos(angle) * velocity);

				if(Math.abs(speedY) < Math.abs(speedX)) {
					timeLeft = (deltaX) / speedX;
				} else {
					timeLeft = (deltaY) / speedY;
				}
			}
		}

		x += speedX * dt;
		y += speedY * dt;

		updateRect();

		return true;
	}

	public void hitBy(Bullet b) {
		final int dmg = b.getDamage();
		applyDamage(dmg);

		if(b.parent != null) {
			b.parent.updateDamageDealt(dmg);
		}

		int s = b.getStatus();
		
		if(type > TYPE_BOSS) {
			// half durations if this is a boss
			b.setDuration(b.getDuration() / 2f);
		}
		
		if(type == TYPE_BOSS_SUPREME_HEAVY) {
			// null status effects if this s a heavy boss...
			s = STATE_OK;
		}

		switch(s) {
		case STATE_OK:	// don't do anything
			break;
		case STATE_WEAKNESS:
			state |= s;
			weaknessDuration = b.getDuration();
			break;
		case STATE_SLOW:
			if(type != TYPE_HEAVY) {
				state |= s;
				slowDuration = b.getDuration();
				slowAmount = 1f - (b.getExtra2() / 100f);
			}
			break;
		case STATE_STUN:
			if(type != TYPE_HEAVY) {
				state |= s;
				stunDuration = b.getDuration();
			}
			break;
		case STATE_DECAY:
			state |= s;
			int d = (int) (b.getExtra() * maxHp / hp);
			applyDamage(d);
			
			if(b.parent != null) {
				b.parent.updateDamageDealt(d);
			}
			break;
		default:
			break;
		}
	}

	public void hitBy(Tower t) {
		final int dmg = t.getDamage();
		applyDamage(dmg);

		t.updateDamageDealt(dmg);
	}

	private void applyDamage(int damage) {
		switch(type) {
		case TYPE_HEAVY:
			// heavy units take half damage...
			int half = damage / 2;
			half = Math.min(half, 30);
			
			damage -= half;
			break;
		default:
			break;
		}
		
		if(type > TYPE_BOSS) {
			// this is a boss unit
			// apply the effect of god armor
			damage *= 0.4f;
		}

		if((state & STATE_WEAKNESS) != 0) {
			damage *= 1.3f;
		}

		hp -= damage;

		// normalize hp so that hp bars will display correctly
		if(hp <= 0) { 
			hp = 0; 
			isAlive = false;
		}

		percentHp = hp / (float)maxHp;
	}

	public boolean isAlive() {
		return isAlive;
	}

	public void updatePath() {
		if(lastWayPoint == null) return;

		// force a path update on the next update call
		currentWayPoint = lastWayPoint;
		timeLeft = 0;
	}

	public boolean isGhost() {
		return this.type == TYPE_GHOST;
	}

	public void writeToStream(DataOutputStream stream) throws IOException {
		stream.writeFloat(hp);
		stream.writeInt(gold);
		stream.writeInt(type);

		stream.writeFloat(x);
		stream.writeFloat(y);

		stream.writeFloat(pathOffX);
		stream.writeFloat(pathOffY);

		stream.writeFloat(speedX);
		stream.writeFloat(speedY);
		stream.writeFloat(hp);
		stream.writeFloat(maxHp);
		stream.writeInt(state);
		stream.writeFloat(slowAmount);
		stream.writeFloat(slowDuration);
		stream.writeFloat(weaknessDuration);
		stream.writeFloat(stunDuration);
		
		stream.writeInt(pathIndex);

		if(lastWayPoint == null) {
			stream.writeInt(-1);
			stream.writeInt(-1);
		} else {
			stream.writeInt(lastWayPoint.x);
			stream.writeInt(lastWayPoint.y);
		}
		stream.writeInt(currentWayPoint.x);
		stream.writeInt(currentWayPoint.y);

		stream.writeFloat(timeLeft);
	}

	public void loadFromStream(DataInputStream stream) throws IOException {
		Map map = Core.game.map;

		preload(stream.readFloat(), stream.readInt(), stream.readInt());
		load(map);

		x = stream.readFloat();
		y = stream.readFloat();

		pathOffX = stream.readFloat();
		pathOffY = stream.readFloat();

		speedX = stream.readFloat();
		speedY = stream.readFloat();
		hp = stream.readFloat();
		maxHp = stream.readFloat();
		state = stream.readInt();

		slowAmount = stream.readFloat();
		slowDuration = stream.readFloat();
		weaknessDuration = stream.readFloat();
		stunDuration = stream.readFloat();
		
		pathIndex = stream.readInt();

		percentHp = hp / (float)maxHp;
		
		int x = stream.readInt();
		int y = stream.readInt();
		if(x == -1) {
			lastWayPoint = null;
		} else {
			lastWayPoint = map.getPathNode(x, y, pathIndex);
		}
		
		int nodeX = stream.readInt();
		int nodeY = stream.readInt();
		if(type == TYPE_GHOST) {
			Node n = map.getGhostPathStart();
			if(n.x == nodeX && n.y == nodeY) {
				currentWayPoint = n;
			} else {
				currentWayPoint = map.getPathNode(nodeX, nodeY, pathIndex);
			}
		} else {
			currentWayPoint = map.getPathNode(nodeX, nodeY, pathIndex);
		}
		
		timeLeft = stream.readFloat();
	}

	public boolean isObstacle() {
		return obstacle;
	}

	public float getMovementSpeed() {
		return STATS[type].speed;
	}

	public float getMaxHp() {
		return maxHp;
	}
	
	public String getDesc() {
		return STATS[type].desc;
	}

	public static class SpriteStats {
		String name;
		
		int color;
		float speed; 	// in MAP_SDP
		String desc;

		private SpriteStats(String name, int color, float speed, String desc) {
			this.name = name;
			this.color = color;
			this.speed = speed;
			this.desc = desc;
		}
	}
	
	private static final SpriteStats STATS[] = new SpriteStats[] {
		/*				Name				Color		speed	sprite description				*/
		new SpriteStats("Regular Enemy",	0x00000000, 0, 		null),	// null sprite
		new SpriteStats("Regular Enemy",	0xFFB8B8B8, 1.3f, 	"A regular enemy."),
		new SpriteStats("Speedling",		0xFF3B7DF7, 1.8f, 	"A very fast enemy."),
		new SpriteStats("Heavy",			0xFFEB6CE6, 1.0f, 	"This enemy may be slow, but it reduces all damage taken. This unit is immune to slow and stun."),
		new SpriteStats("Ghost",			0xFF51F071, 1.3f, 	"This enemy can pass through towers and other obstacles!"),
		new SpriteStats("Splitter",			0xFFFAFA43, 1.3f, 	"This enemy will split into smaller enemies once killed."),
		new SpriteStats("Mini",				0x00000000, 1.3f, 	"This is a child from a splitter enemy."),
		new SpriteStats("Undying",			0xFFEA0000, 1.3f,	"This enemy can revive once after dying"),
		new SpriteStats("Regenerator",		0xFFFFBB00, 1.3f, 	"This enemy can regenerate if it has not taken damage for an amount of time")
	};
	
	private static final SpriteStats BOSS_STATS[] = new SpriteStats[] {
		/*				Name				Color		speed	sprite description				*/
		new SpriteStats("Regular Enemy",	0x00000000, 0, 		null),	// null sprite
		new SpriteStats("Regular Boss",		0xFFB8B8B8, 1.3f, 	"A regular boss."),
		new SpriteStats("Supreme Heavy",	0xFF3B7DF7, 1.3f, 	"This boss ignores all stuns and slows."),
	};
	
	public static SpriteStats getSpriteStats(int type) {
		return STATS[type];
	}

	public int getGoldReward() {
		return gold;
	}

	public boolean isTargetable() {
		return targetable;
	}
	
	public boolean hasDeathAnimation() {
		return hasDeathAnimation;
	}
	
	private OnAnimationComplete deathComplete = new OnAnimationComplete() {

		@Override
		public void onAnimationComplete() {
			Core.game.spriteMgr.removeDrawableStrict(Sprite.this);
			Core.game.checkWaveClear();
		}
		
	};
	
	public void playDeathAnimation() {
		deathAnimation.setOnAnimationCompleteListener(deathComplete);
		animation = deathAnimation;
		Core.gu.addGameUpdatable(deathAnimation);
	}
	
	public void setPathIndex(int pathIndex) {
		this.pathIndex = pathIndex;
	}
}
