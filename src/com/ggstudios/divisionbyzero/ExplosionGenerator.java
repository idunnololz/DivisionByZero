package com.ggstudios.divisionbyzero;

import android.opengl.GLES20;

public class ExplosionGenerator extends ParticleEngine {
	private static final float DEFAULT_DISTANCE_VARIANCE = 1f;
	private static final float DEFAULT_SIZE_VARIANCE = 0.25f;
	
	private float distanceVariance;
	private float sizeVariance;
	
	public ExplosionGenerator(int maxParticles) {
		super(maxParticles);
		
		distanceVariance = Core.SDP * DEFAULT_DISTANCE_VARIANCE;
		sizeVariance = DEFAULT_SIZE_VARIANCE;
	}
	
	public void setDistanceVariance(float var) {
		distanceVariance = var;
	}
	
	public void setSizeVariance(float var) {
		sizeVariance = var;
	}
	
	@Override
	protected void setupParticle(Particle p) {
		p.x = x + (rand.nextFloat() - 0.5f) * distanceVariance;
		p.y = y + (rand.nextFloat() - 0.5f) * distanceVariance;
		p.extra1 = 1f + (rand.nextFloat() - 0.5f) * sizeVariance;
		p.extra2 = p.extra1;
		p.lifeSpan = particleLifespan;
	}
	
	@Override
	protected void updateParticle(Particle p) {
		p.extra2 = p.extra1 * ((p.lifeSpan / particleLifespan) * 0.5f + 0.5f);
	}
	
	@Override
	protected void drawParticle(Particle p) {
		Utils.resetMatrix();
		Utils.scale(p.extra2);
		Utils.translateAndCommit(p.x, p.y);

		GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1f, 0.72f, 0.2f, p.lifeSpan / particleLifespan);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1f, 1f, 1f, 1f);
	}
}
