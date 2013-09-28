package com.ggstudios.divisionbyzero;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

import android.graphics.Rect;
import android.opengl.GLES20;

import com.ggstudios.divisionbyzero.DijkstraPathFinder.Node;
import com.ggstudios.utils.DebugLog;

import static fix.android.opengl.GLES20.glVertexAttribPointer;

/**
 * Enemy sprite class. Drawable.
 * Note that the drawable is centered.
 * @author iDunnololz
 *
 */
public class Sprite extends PictureBox implements Updatable{
	private static final String TAG = "EnemySprite";

	private static int WEAKNESS_TEXTURE = -1;

	public static final int STATE_OK		= 0x00000000;
	public static final int STATE_STUN		= 0x00000001;
	public static final int STATE_SLOW		= 0x00000010;
	public static final int STATE_IMMUNE	= 0x00000100;
	public static final int STATE_BOSS		= 0x00001000;
	public static final int STATE_DEATH		= 0x00010000;
	public static final int STATE_BURN		= 0x00100000;
	public static final int STATE_WEAKNESS	= 0x01000000;
	public static final int STATE_DECAY 	= 0x10000000;
	
	public static final int
	TYPE_REGULAR = 1,
	TYPE_SPEEDLING = 2,
	TYPE_HEAVY = 3,
	TYPE_GHOST = 4,
	TYPE_SPLITTER = 5,
	TYPE_MINI = 6 /* spawned by splitter */;

	private float pathOffX, pathOffY;

	private float velocity;
	private float speedX, speedY;

	private int hp;
	private int maxHp;
	
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
	Rect rect = new Rect();

	private float timeLeft;

	private static final float HP_HEIGHT = 0.1f;
	private int hpTexture;
	private float percentHp;

	private boolean isAlive = true;
	private boolean obstacle = true;

	private SpriteAnimation animation;

	private static final SpriteAnimation ANIMATION_NORMAL = new SpriteAnimation();

	private Random random;

	public static void doOnce() {
		Core.gu.addGameUpdatable(ANIMATION_NORMAL);
	}

	public static void refreshResources() {
		WEAKNESS_TEXTURE = Core.tm.get(R.drawable.status_weakness);

		ANIMATION_NORMAL.reset();

		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0001, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0002, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0003, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0004, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0005, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0006, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0007, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0008, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0009, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0010, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0011, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0012, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0013, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0014, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0015, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0016, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0017, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0018, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0019, 0.1f);
		ANIMATION_NORMAL.addFrame(R.drawable.sprite_normal0020, 0.1f);
	}

	/**
	 * This static function will initialize a partially loaded instance of
	 * a sprite that can be loaded late. This is useful for reloading sprites
	 * before an instance of GL can be obtained.
	 * 
	 * @param x
	 * @param y
	 * @param hp
	 * @param gold
	 * @param type
	 * @return
	 */
	public static Sprite getPreloadedInstance(float x, float y, int hp, int gold, int type) {
		Sprite t = new Sprite(0,0);
		t.x = x;
		t.y = y;
		t.hp = hp;
		t.maxHp = hp;
		t.gold = gold;
		t.type = type;

		t.partiallyLoaded = true;

		t.percentHp = 1.0f;
		
		return t;
	}

	public static Sprite getSampleInstance(int hp, int gold, int type) {
		Sprite t = new Sprite(0,0);
		t.hp = hp;
		t.maxHp = hp;
		t.gold = gold;
		t.type = type;

		t.partiallyLoaded = true;

		t.percentHp = 1.0f;

		return t;
	}

	private Sprite(float x, float y) {
		super(x, y);

		random = StateManager.getInstance().getRandom();
	}

	boolean needToLoad() {
		return partiallyLoaded;
	}

	@Override
	public void draw(float offX, float offY) {
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
		partiallyLoaded = false;
		currentWayPoint = map.getPathStart();
		
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
			currentWayPoint = map.getGhostPathStart();
			break;
		case TYPE_SPLITTER:
			super.setVBO(Core.GeneralBuffers.map_half_tile);
			super.setScale(0.7f);

			animation = ANIMATION_NORMAL;
			break;
		default:
			DebugLog.e(TAG, "Sprite type does not exist.");
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
			Core.game.reportKilledSprite(this);
		
			//Sprite s = Sprite.getPreloadedInstance(this.x, this.y, hp, gold, TYPE_GHOST)
			//Core.game.addEnemy(s);
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

		if(animation != null)
			setTextureHandle(animation.getTextureHandle());

		return true;
	}

	public void hitBy(Bullet b) {
		final int dmg = b.getDamage();
		applyDamage(dmg);

		if(b.parent != null) {
			b.parent.updateDamageDealt(dmg);
		}

		int s = b.getStatus();

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
				slowAmount = b.getExtra2() / 100f;
			}
			break;
		case STATE_STUN:
			state |= s;
			stunDuration = b.getDuration();
			break;
		case STATE_DECAY:
			state |= s;
			int d = (int) (b.getExtra() * maxHp / hp);
			applyDamage(d);
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
			if(damage < 5) return;
			damage /= 2;
			break;
		default:
			break;
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
		stream.writeInt(hp);
		stream.writeInt(gold);
		stream.writeInt(type);

		stream.writeFloat(x);
		stream.writeFloat(y);

		stream.writeFloat(pathOffX);
		stream.writeFloat(pathOffY);

		stream.writeFloat(speedX);
		stream.writeFloat(speedY);
		stream.writeInt(hp);
		stream.writeInt(maxHp);
		stream.writeInt(state);
		stream.writeFloat(slowAmount);
		stream.writeFloat(slowDuration);
		stream.writeFloat(weaknessDuration);
		stream.writeFloat(stunDuration);

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

	public static Sprite createFromStream(DataInputStream stream) throws IOException {
		Map map = Core.game.map;

		Sprite s = getPreloadedInstance(0, 0, stream.readInt(), stream.readInt(), stream.readInt());
		s.load(map);

		s.x = stream.readFloat();
		s.y = stream.readFloat();

		s.pathOffX = stream.readFloat();
		s.pathOffY = stream.readFloat();

		s.speedX = stream.readFloat();
		s.speedY = stream.readFloat();
		s.hp = stream.readInt();
		s.maxHp = stream.readInt();
		s.state = stream.readInt();

		s.slowAmount = stream.readFloat();
		s.slowDuration = stream.readFloat();
		s.weaknessDuration = stream.readFloat();
		s.stunDuration = stream.readFloat();

		s.percentHp = s.hp / (float)s.maxHp;
		
		int x = stream.readInt();
		int y = stream.readInt();
		if(x == -1) {
			s.lastWayPoint = null;
		} else {
			s.lastWayPoint = map.getPathNode(x, y);
		}
		
		int nodeX = stream.readInt();
		int nodeY = stream.readInt();
		if(s.type == TYPE_GHOST) {
			Node n = map.getGhostPathStart();
			if(n.x == nodeX && n.y == nodeY) {
				s.currentWayPoint = n;
			} else {
				s.currentWayPoint = map.getPathNode(nodeX, nodeY);
			}
		} else {
			s.currentWayPoint = map.getPathNode(nodeX, nodeY);
		}
		
		s.timeLeft = stream.readFloat();
		return s;
	}

	public boolean isObstacle() {
		return obstacle;
	}

	public float getMovementSpeed() {
		return STATS[type].speed;
	}

	public int getMaxHp() {
		return maxHp;
	}
	
	public String getDesc() {
		return STATS[type].desc;
	}

	private static class SpriteStats {
		float speed; 	// in MAP_SDP
		String desc;

		private SpriteStats(float speed, String desc) {
			this.speed = speed;
			this.desc = desc;
		}
	}
	
	private static final SpriteStats STATS[] = new SpriteStats[] {
		new SpriteStats(0, null),	// null sprite
		new SpriteStats(1.2f, "A regular enemy."),
		new SpriteStats(1.6f, "A very fast enemy."),
		new SpriteStats(0.8f, "This enemy may be slow, but it ignore all insignificant damage and halves all other damage. This unit is immune to slow."),
		new SpriteStats(1.0f, "This enemy can pass through towers and other obstacles!"),
		new SpriteStats(1.0f, "This enemy will split into smaller enemies once killed."),
		new SpriteStats(1.0f, "This is a child from a splitter enemy.")
	};

	public int getGoldReward() {
		return gold;
	}
}
