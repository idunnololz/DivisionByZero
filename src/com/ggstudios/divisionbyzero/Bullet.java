package com.ggstudios.divisionbyzero;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class Bullet extends PictureBox implements Updatable{
	// this class controls the bullet and it's target...

	public static final int 
	TYPE_NORMAL = 1, 
	TYPE_SEEKING = 2,
	TYPE_AOE = 3, 
	TYPE_AOE_SEEKING = 4, 
	TYPE_COMPLEX = 5,	// complex bullets require special rendering needs (such as rotation)
	TYPE_COMPLEX_AOE_FROST = 6,
	TYPE_COMPLEX_AOE_STUN = 7,
	TYPE_GHOST = 8,
	TYPE_GHOST_AOE = 9
	;

	private float duration;
	private float extra;
	private int extra2;

	private int state;

	private Sprite target;
	private int dmg;
	private float velocity;
	private int type;

	private float angle;
	private float speedX, speedY;

	public Tower parent;

	private static float boundLeft, boundTop, boundRight, boundBottom;

	private BulletManager getBulletManager() {
		return Core.game.bulletMgr;
	}

	public static void setBounds(float l, float t, float r, float b) {
		boundLeft   = l;
		boundTop    = t;
		boundRight  = r;
		boundBottom = b;
	}

	protected Bullet(float x, float y) {
		super(x, y);
	}

	public Bullet(int x, int y, VBO vbo, int resId) {
		super(x, y, vbo, resId);
	}

	public void setDuration(float d) {
		duration = d;
	}
	
	public void setState(int state) {
		this.state = state;
	}
	
	public void setup(float x, float y, Sprite target, int dmg, float vel, int type, float extra) {
		setup(x, y, target, dmg, vel, type);

		this.extra = extra;
	}

	public void setup(float x, float y, Sprite target, int dmg, float vel,
			int type, float extra, int extra2) {
		setup(x, y, target, dmg, vel, type);

		this.extra = extra;
		this.extra2 = extra2;
	}
	
	public void setup(float x, float y, Sprite target, int dmg, float vel,
			int type, int extra) {
		setup(x, y, target, dmg, vel, type);

		this.extra2 = extra;
	}

	public void setup(float x, float y, Sprite target, 
			int dmg, float vel, int type) {
		this.x = x;
		this.y = y;
		this.target = target;
		this.dmg = dmg;
		this.velocity = vel;
		this.type = type;

		angle = (float) Math.atan2(target.x - x, target.y - y);
		speedX = (float) (Math.sin(angle)*velocity);
		speedY = (float) (Math.cos(angle)*velocity);

		state = Sprite.STATE_OK;

		switch(type) {
		case TYPE_COMPLEX_AOE_STUN:
		case TYPE_COMPLEX_AOE_FROST:
		case TYPE_COMPLEX:
			setAngle(angle);
			setVBO(Core.GeneralBuffers.map_tile);
			break;
		default:
			break;
		}
	}

	@Override
	public boolean update(float dt) {
		// do move here
		switch(type) {
		case TYPE_GHOST:
		{
			x += speedX * dt;
			y += speedY * dt;

			Sprite spriteHit;

			if(x < boundLeft || y < boundTop || x > boundRight || y > boundBottom){
				getBulletManager().removeDrawable(this);
				return false;
			} else if((spriteHit = getGhostHit(this, (int)x, (int)y)) != null) {
				// do apply damage here
				spriteHit.hitBy(this);
				getBulletManager().removeDrawable(this);
				return false;
			}
			return true;
		}
		case TYPE_GHOST_AOE:
			x += speedX * dt;
			y += speedY * dt;

			if(x < boundLeft || y < boundTop || x > boundRight || y > boundBottom){
				getBulletManager().removeDrawable(this);
				return false;
			} else if(getGhostHit(this, (int)x, (int)y) != null) {
				// do apply damage here
				showAoeExplosion();
				applyDamageInAoeGhost(x, y);
				getBulletManager().removeDrawable(this);
				return false;
			}
			break;
		case TYPE_SEEKING:
			if(Core.game.spriteMgr.len == 0) {
				getBulletManager().removeDrawable(this);
				return false;
			} else if(!target.isAlive()){
				target = Core.game.spriteMgr.get(0);
			}

			// AoE seeking is not meant to miss...
			angle = Utils.fastatan2(target.x - x, target.y - y );
			speedX = (float) (Math.sin(angle)*velocity);
			speedY = (float) (Math.cos(angle)*velocity);

			x += speedX * dt;
			y += speedY * dt;
			break;
		case TYPE_AOE_SEEKING:
			if(Core.game.spriteMgr.len == 0) {
				showAoeExplosion();
				getBulletManager().removeDrawable(this);
				return false;
			} else if(!target.isAlive()){
				target = Core.game.spriteMgr.get(0);
			}

			// AoE seeking is not meant to miss...
			angle = Utils.fastatan2(target.x - x, target.y - y );
			speedX = (float) (Math.sin(angle)*velocity);
			speedY = (float) (Math.cos(angle)*velocity);
		case TYPE_NORMAL:
		default:
			x += speedX * dt;
			y += speedY * dt;
			break;
		}

		Sprite spriteHit;

		if(x < boundLeft || y < boundTop || x > boundRight || y > boundBottom){
			getBulletManager().removeDrawable(this);
			return false;
		} else if((spriteHit = getEnemyHit(this, (int)x, (int)y)) != null) {
			// do apply damage here
			switch(type) {
			case TYPE_COMPLEX_AOE_STUN:
				showStun();
				applyDamageInAoe(x, y);
				getBulletManager().removeDrawable(this);
				break;
			case TYPE_COMPLEX_AOE_FROST:
				showFrost();
				applyDamageInAoe(x, y);
				getBulletManager().removeDrawable(this);
				break;
			case TYPE_AOE_SEEKING:
			case TYPE_AOE:
				showAoeExplosion();
				applyDamageInAoe(x, y);
				getBulletManager().removeDrawable(this);
				break;
			default:
				spriteHit.hitBy(this);
				getBulletManager().removeDrawable(this);
				break;
			}
			return false;
		}

		return true;
	}
	
	private Sprite getEnemyHit(Bullet b, int x, int y) {
		List<Sprite> list = Core.game.spriteMgr.getRawList();
		final int len = Core.game.spriteMgr.size();
		for (int i = len - 1; i > -1; i--) {
			Sprite sprite = list.get(i);
			if (sprite.isTargetable() && sprite.rect.contains(x, y)) {
				return sprite;
			}
		}
		return null;
	}
	
	private Sprite getGhostHit(Bullet b, int x, int y) {
		List<Sprite> list = Core.game.spriteMgr.getRawList();
		final int len = Core.game.spriteMgr.size();
		for (int i = len - 1; i > -1; i--) {
			Sprite sprite = list.get(i);
			if (sprite.isTargetable() && sprite.rect.contains(x, y)) {
				if (sprite.isGhost() && sprite.isTargetable()
						&& sprite.rect.contains(x, y)) {
					return sprite;
				}
			}
		}
		return null;
	}

	private void showAoeExplosion() {
		final float size = extra * 2;
		final PictureBox pb = Core.game.extrasMgr.obtain(size, size);
		pb.setTexture(R.drawable.aoe_blast);
		pb.x = x;
		pb.y = y;
		pb.isVisible = true;
		Core.gu.addUiUpdatable(new Updatable() {

			private static final float DURATION = 1.0f;
			private float total = 0f;

			@Override
			public boolean update(float dt) {
				total += dt;
				if(total >= DURATION) {
					Core.game.extrasMgr.removeDrawable(pb);
					return false;
				} else {
					pb.transparency = (1 - total/DURATION);
				}
				return true;
			}

		});
	}

	private void showStun() {
		final float size = extra * 2;
		final PictureBox pb = Core.game.extrasMgr.obtain(size, size);
		pb.setTexture(R.drawable.aoe_stun);
		pb.x = x;
		pb.y = y;
		pb.isVisible = true;
		Core.gu.addUiUpdatable(new Updatable() {

			private static final float DURATION = 0.5f;
			private float total = 0f;
			private static final float TOTAL_ROTATE = (float) (Math.PI * 0.25f);

			@Override
			public boolean update(float dt) {
				total += dt;
				if(total >= DURATION) {
					Core.game.extrasMgr.removeDrawable(pb);
					return false;
				} else {
					pb.transparency = (1 - total/DURATION);
					pb.setAngle((float) (TOTAL_ROTATE * total/DURATION));
				}
				return true;
			}

		});
	}

	private void showFrost() {
		final float size = extra * 2;
		final PictureBox pb = Core.game.extrasMgr.obtain(size, size);

		pb.setTexture(R.drawable.flake_freeze);

		pb.x = x;
		pb.y = y;
		pb.isVisible = true;
		Core.gu.addUiUpdatable(new Updatable() {

			private static final float DURATION = 0.5f;
			private float total = 0f;
			private static final float TOTAL_ROTATE = (float) (Math.PI * 0.25f);

			@Override
			public boolean update(float dt) {
				total += dt;
				if(total >= DURATION) {
					Core.game.extrasMgr.removeDrawable(pb);
					return false;
				} else {
					pb.transparency = (1 - total/DURATION);
					pb.setAngle((float) (TOTAL_ROTATE * total/DURATION));
				}
				return true;
			}

		});
	}

	public void applyDamageInAoe(float x, float y) {
		List<Sprite> list = Core.game.spriteMgr.getRawList();
		int len = Core.game.spriteMgr.size();

		float rangeSquared = extra * extra;

		for(int i = len - 1; i > -1; i--) {
			Sprite sprite = list.get(i);
			
			if(!sprite.isTargetable()) continue;

			final float s_x = sprite.x, s_y = sprite.y;
			final float u = (s_x - x)*(s_x - x);
			final float v = (s_y - y)*(s_y - y);

			if(rangeSquared > u + v){
				sprite.hitBy(this);
			}
		}
	}

	private void applyDamageInAoeGhost(float x, float y) {
		List<Sprite> list = Core.game.spriteMgr.getRawList();
		int len = Core.game.spriteMgr.size();

		float rangeSquared = extra * extra;

		for(int i = len - 1; i > -1; i--) {
			Sprite sprite = list.get(i);

			if(!sprite.isGhost() || !sprite.isTargetable()) continue;
			
			final float s_x = sprite.x, s_y = sprite.y;
			final float u = (s_x - x)*(s_x - x);
			final float v = (s_y - y)*(s_y - y);

			if(rangeSquared > u + v){
				sprite.hitBy(this);
			}
		}
	}

	public int getDamage() {
		return dmg;
	}

	public int getStatus() {
		return state;
	}

	public float getDuration() {
		return duration;
	}
	
	public float getExtra() {
		return extra;
	}

	public int getExtra2() {
		return extra2;
	}

	public void writeToStream(DataOutputStream stream) throws IOException {
		stream.writeFloat(x);
		stream.writeFloat(y);
		stream.writeInt(textureId);
		
		stream.writeFloat(duration);
		stream.writeFloat(extra);
		stream.writeInt(extra2);
		stream.writeInt(state);
		
		if(target != null && target.isAlive()) {
			stream.writeInt(Core.game.spriteMgr.indexOf(target));
		} else {
			stream.writeInt(-1);
		}
		
		stream.writeInt(dmg);
		stream.writeFloat(velocity);
		stream.writeInt(type);
		stream.writeFloat(angle);
		stream.writeFloat(speedX);
		stream.writeFloat(speedY);
	}
	
	public void loadFromStream(DataInputStream stream) throws IOException {
		x = stream.readFloat();
		y = stream.readFloat();
		
		setTexture(stream.readInt());
		
		duration = stream.readFloat();
		extra = stream.readFloat();
		extra2 = stream.readInt();
		state = stream.readInt();
		
		int targetIndex = stream.readInt();
		if(targetIndex == -1) {
			target = null;
		} else {
			target = Core.game.spriteMgr.get(targetIndex);
		}
		
		dmg = stream.readInt();
		velocity = stream.readFloat();
		type = stream.readInt();
		angle = stream.readFloat();
		speedX = stream.readFloat();
		speedY = stream.readFloat();
	}

}
