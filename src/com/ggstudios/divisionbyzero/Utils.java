package com.ggstudios.divisionbyzero;

import android.opengl.GLES20;

public class Utils {
	public static final float PI = (float) Math.PI;
	
	public static final void transformAndCommit(float x, float y, float scale){
		Core.matrix[3] += x * scale;
		Core.matrix[7] += y * scale;
		Core.matrix[0] = scale; 
		Core.matrix[5] = scale;
		
		GLES20.glUniformMatrix4fv(Core.U_TRANSLATION_MATRIX_HANDLE, 1, false, Core.matrix, 0);
	}
	
	public static final void translate(float x, float y){
		Core.matrix[3] += x;
		Core.matrix[7] += y;
	}
	
	public static final void translateAndCommit(float x, float y){
		Core.matrix[3] += x;
		Core.matrix[7] += y;
		GLES20.glUniformMatrix4fv(Core.U_TRANSLATION_MATRIX_HANDLE, 1, false, Core.matrix, 0);
	}
	

    private static final int           SIZE                 = 1024;
    private static final float        STRETCH            = (float) Math.PI;
    // Output will swing from -STRETCH to STRETCH (default: Math.PI)
    // Useful to change to 1 if you would normally do "atan2(y, x) / Math.PI"

    // Inverse of SIZE
    private static final int        EZIS            = -SIZE;
    private static final float[]    ATAN2_TABLE_PPY    = new float[SIZE + 1];
    private static final float[]    ATAN2_TABLE_PPX    = new float[SIZE + 1];
    private static final float[]    ATAN2_TABLE_PNY    = new float[SIZE + 1];
    private static final float[]    ATAN2_TABLE_PNX    = new float[SIZE + 1];
    private static final float[]    ATAN2_TABLE_NPY    = new float[SIZE + 1];
    private static final float[]    ATAN2_TABLE_NPX    = new float[SIZE + 1];
    private static final float[]    ATAN2_TABLE_NNY    = new float[SIZE + 1];
    private static final float[]    ATAN2_TABLE_NNX    = new float[SIZE + 1];

    static
    {
        for (int i = 0; i <= SIZE; i++)
        {
            float f = (float)i / SIZE;
            ATAN2_TABLE_PPY[i] = (float)(StrictMath.atan(f) * STRETCH / StrictMath.PI);
            ATAN2_TABLE_PPX[i] = STRETCH * 0.5f - ATAN2_TABLE_PPY[i];
            ATAN2_TABLE_PNY[i] = -ATAN2_TABLE_PPY[i];
            ATAN2_TABLE_PNX[i] = ATAN2_TABLE_PPY[i] - STRETCH * 0.5f;
            ATAN2_TABLE_NPY[i] = STRETCH - ATAN2_TABLE_PPY[i];
            ATAN2_TABLE_NPX[i] = ATAN2_TABLE_PPY[i] + STRETCH * 0.5f;
            ATAN2_TABLE_NNY[i] = ATAN2_TABLE_PPY[i] - STRETCH;
            ATAN2_TABLE_NNX[i] = -STRETCH * 0.5f - ATAN2_TABLE_PPY[i];
        }
    }

    /**
     * ATAN2
     */

    public static final float fastatan2(float y, float x)
    {
        if (x >= 0)
        {
            if (y >= 0)
            {
                if (x >= y)
                    return ATAN2_TABLE_PPY[(int)(SIZE * y / x + 0.5)];
                else
                    return ATAN2_TABLE_PPX[(int)(SIZE * x / y + 0.5)];
            }
            else
            {
                if (x >= -y)
                    return ATAN2_TABLE_PNY[(int)(EZIS * y / x + 0.5)];
                else
                    return ATAN2_TABLE_PNX[(int)(EZIS * x / y + 0.5)];
            }
        }
        else
        {
            if (y >= 0)
            {
                if (-x >= y)
                    return ATAN2_TABLE_NPY[(int)(EZIS * y / x + 0.5)];
                else
                    return ATAN2_TABLE_NPX[(int)(EZIS * x / y + 0.5)];
            }
            else
            {
                if (x <= y) // (-x >= -y)
                    return ATAN2_TABLE_NNY[(int)(SIZE * y / x + 0.5)];
                else
                    return ATAN2_TABLE_NNX[(int)(SIZE * x / y + 0.5)];
            }
        }
    }

	public static void rotate(float angle){
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);
		final float m0 = Core.matrix[0];
		final float m5 = Core.matrix[5];
		Core.matrix[0] = cos * m0; 
		Core.matrix[1] = sin * m5; 
		Core.matrix[5] = cos * m5; 
		Core.matrix[4] = -sin * m0; 
	}
	
	public static void rotate(float angle, float[] matrix){
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);
		matrix[0] = cos; 
		matrix[1] = sin; 
		matrix[5] = cos; 
		matrix[4] = -sin; 
	}
	
	public static void scale(float factor){
		Core.matrix[0] *= factor; 
		Core.matrix[5] *= factor;
	}

	public static void scale(float factor, float[] matrix){
		matrix[0] *= factor; 
		matrix[5] *= factor;
	}
	
	public static void scaleW(float factor){
		Core.matrix[0] = factor;
	}
	
	public static void scaleH(float factor){
		Core.matrix[5] = factor;
	}

	public static void scaleW(float factor, float[] matrix){
		matrix[0] = factor;
	}

	public static void resetMatrix(){
		// 1 0 0 0
		// 0 1 0 0 
		// 0 0 1 0
		// 0 0 0 1

		Core.matrix[0] = 1;
		Core.matrix[1] = 0;
		Core.matrix[3] = 0;
		Core.matrix[4] = 0;
		Core.matrix[5] = 1;
		Core.matrix[7] = 0;
	}
	
	/**
	 * Finds the smallest Power Of Two number that's bigger than n. Or more formally...
	 * Finds the smallest m such that m >= n and m = 2^d where d is a natural number.
	 * @param n The lower bound for the POT number.
	 * @return A POT number greater than n.
	 */
	public static int findSmallestPot(int n){
		int i;
		for(i = 1; i < n; i = (i << 1)){}
		return i;
	}
}