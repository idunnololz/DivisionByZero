package com.ggstudios.divisionbyzero;

import android.opengl.GLES20;
import android.util.Log;

public class Utils {
	public static final float PI = 3.1415927f;
    private static final float ATAN2_CF1 = (float) (3.1415927f / 4f); 
    private static final float ATAN2_CF2 = 3f * ATAN2_CF1; 
	
	//this string is used for converting num to string
	private static char[] ctemp = new char[4];
	private static char[] ctemp2 = new char[10];
	
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
		Core.matrix[0] = factor; 
		Core.matrix[5] = factor;
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
	
	public final static char[] numToString4(int n){
		ctemp[0] = '0';
		ctemp[1] = '0';
		ctemp[2] = '0';
		ctemp[3] = '0';

		int digit = 1;

		int counter = 3;

		int temp = 0;

		if(n == 0)
			return ctemp;

		while(digit <= n){
			temp = n % (digit * 10);
			temp /= digit;

			switch(temp){
			case 0:
				ctemp[counter] = '0';
				break;
			case 1:
				ctemp[counter] = '1';
				break;
			case 2:
				ctemp[counter] = '2';
				break;
			case 3:
				ctemp[counter] = '3';
				break;
			case 4:
				ctemp[counter] = '4';
				break;
			case 5:
				ctemp[counter] = '5';
				break;
			case 6:
				ctemp[counter] = '6';
				break;
			case 7:
				ctemp[counter] = '7';
				break;
			case 8:
				ctemp[counter] = '8';
				break;
			case 9:
				ctemp[counter] = '9';
				break;
			}

			digit *= 10;
			counter--;
		}

		return ctemp;
	}
	
	public final static char[] numToString(int n){
		ctemp2[0] = '0';
		ctemp2[1] = '0';
		ctemp2[2] = '0';
		ctemp2[3] = '0';
		ctemp2[4] = '0';
		ctemp2[5] = '0';
		ctemp2[6] = '0';
		ctemp2[7] = '0';
		ctemp2[8] = '0';
		ctemp2[9] = '0';

		int digit = 1;
		int counter = 9;

		int temp = 0;

		if(n == 0)
			return ctemp2;

		while(digit <= n){
			temp = n % (digit * 10);
			temp /= digit;

			switch(temp){
			case 0:
				ctemp2[counter] = '0';
				break;
			case 1:
				ctemp2[counter] = '1';
				break;
			case 2:
				ctemp2[counter] = '2';
				break;
			case 3:
				ctemp2[counter] = '3';
				break;
			case 4:
				ctemp2[counter] = '4';
				break;
			case 5:
				ctemp2[counter] = '5';
				break;
			case 6:
				ctemp2[counter] = '6';
				break;
			case 7:
				ctemp2[counter] = '7';
				break;
			case 8:
				ctemp2[counter] = '8';
				break;
			case 9:
				ctemp2[counter] = '9';
				break;
			}

			digit *= 10;
			counter--;
		}

		return ctemp2;
	}
	
	public static int findSmallestBase2(int n){
		int i;
		for(i = 1; i < n; i = (i << 1)){
			
		}
		return i;
	}
}