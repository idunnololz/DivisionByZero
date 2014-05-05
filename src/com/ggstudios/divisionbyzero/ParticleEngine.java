package com.ggstudios.divisionbyzero;

import static fix.android.opengl.GLES20.glVertexAttribPointer;

import java.util.Random;

import com.ggstudios.utils.BufferUtils;
import com.ggstudios.utils.DebugLog;

import android.opengl.GLES20;

public class ParticleEngine extends Drawable implements Updatable{
	private static final String TAG = "ParticleEngine";
	private static final float DEFAULT_PARTICLE_LIFESPAN = 1f;
	private static final float DEFAULT_CREATION_CHANCE = 0.5f;
	private static final float DEFAULT_CREATION_DURATION = 0.03f;
	private static final float DEFAULT_MAX_VELOCITY = 1f;
	private static final int DEFAULT_INITIAL_PARTICLES = 5;
	
	protected float x, y;
	
	private Particle[] particles;
	private int textureId, textureHandle;
	private float w, h;
	private int handle;
	private int len = 0;
	private float creationChance = DEFAULT_CREATION_CHANCE;
	private float creationDuration = DEFAULT_CREATION_DURATION;
	private float time = 0f;
	protected float particleLifespan = DEFAULT_PARTICLE_LIFESPAN;
	private int initialParticles = DEFAULT_INITIAL_PARTICLES;
	
	private float a = 1f, r = 1f, g = 1f, b = 1f;
	
	private float maxVel;
	
	protected Random rand;
	
	private boolean visible = true;
	
	private int generationCount = 0;

	public ParticleEngine(int particleNumber) {
		particles = new Particle[particleNumber];

		for(int i = 0; i < particles.length; i++) {
			particles[i] = new Particle();
		}
		
		maxVel = DEFAULT_MAX_VELOCITY * Core.SDP;
		
		rand = StateManager.getInstance().getRandom();
	}
	
	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void setParticleSize(float f) {
		setParticleSize(f, f);
	}
	
	public void setParticleSize(float w, float h) {
		if(w <= 0 || h <= 0) {
			DebugLog.e(TAG, "Particle width and height must be > 0", new Exception());
			return;
		}
		this.w = w;
		this.h = h;
	}

	public void setParticleTexture(int textureId) {
		this.textureId = textureId;
	}
	
	public void setGenerationChance(float chance) {
		creationChance = chance;
	}
	
	public void setInitialParticles(int init) {
		initialParticles = init;
	}
	
	public void setMaxVelocity(float f) {
		maxVel = f;
	}

	public void build() {
		refresh();
		
		int initParticles = Math.min(initialParticles, particles.length);
		for(int i = 0; i < initParticles; i++) {
			setupParticle(particles[i]);
		}
		len = initParticles;
		generationCount = len;
	}

	@Override
	public void draw(float offX, float offY) {
		if(!visible) return;
		
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, handle);
		glVertexAttribPointer(Core.A_POSITION_HANDLE, 2, GLES20.GL_FLOAT, false, 0, 0);

		Shader.setColorMultiply(r, g, b, a);
		for(int i = 0; i < len; i++) {
			drawParticle(particles[i]);
		}
		Shader.resetColorMultiply();
		Core.gr.resetBlendFunc();
	}
	
	public void setColor(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

	protected void drawParticle(Particle p) {
		Utils.resetMatrix();
		Utils.rotate(p.extra1);
		Utils.translateAndCommit(p.x, p.y);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

	@Override
	public void refresh() {
		final float hw = w / 2f;
		final float hh = h / 2f;
		final float[] arr = new float[] {
				-hw, -hh,
				hw,  -hh,
				-hw, hh,
				hw,  hh
		};

		handle = BufferUtils.copyToBuffer(arr);
		
		textureHandle = Core.tm.get(textureId);
	}

	@Override
	public boolean update(float dt) {
		time += dt;
		
		for(int i = 0; i < len; i++) {
			Particle p = particles[i];
			
			if(p.lifeSpan <= 0) {
				recycle(i);
			} else {
				p.lifeSpan -= dt;
				
				updateParticle(p);
			}
		}
		
		if(time >= creationDuration) {
			if(rand.nextFloat() <= creationChance) {
				// We got lucky! Create a new particle if we can...
				if(len != particles.length) {
					setupParticle(particles[len]);

					generationCount++;
					len++;
					
					if(generationCount == particles.length) {
						time = Float.NEGATIVE_INFINITY;
					}
				}
			}
			time -= creationDuration;
		}
		return true;
	}
	
	protected void updateParticle(Particle p) {
		p.x += p.velX;
		p.y += p.velY;
	}

	protected void setupParticle(Particle p) {
		p.x = x;
		p.y = y;
		p.velX = (rand.nextFloat() - 0.5f) * maxVel;
		p.velY = (rand.nextFloat() - 0.5f) * maxVel;
		p.lifeSpan = particleLifespan;
		p.extra1 = Utils.fastatan2(p.velX, p.velY);
	}

	private void recycle(int index) {
		Particle p = particles[len - 1];
		particles[len - 1] = particles[index];
		particles[index] = p;
		len--;
	}

	protected static class Particle {
		float x, y;
		float velX, velY;
		float lifeSpan;
		
		float extra1, extra2;
	}

	public void setVisible(boolean c) {
		visible = c;
	}
}
