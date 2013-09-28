package com.ggstudios.divisionbyzero;

import android.opengl.GLES20;

public class Shader {
	public static final String VERTEX_SHADER_CODE = 
			// This matrix member variable provides a hook to manipulate
			// the coordinates of the objects that use this vertex shader
			"uniform mat4 uConstantMatrix;   	\n" +
			"uniform mat4 uTransMatrix;   		\n" +

	        "attribute vec4 a_Position;  		\n" +
	        "attribute vec4 a_Color;  			\n" +
	        "attribute vec2 a_TexCoordinate;  	\n" +

			"varying vec2 v_TexCoordinate; 		\n" +  // This will be passed into the fragment shader.

	        "void main(){               		\n" +

	        "v_TexCoordinate = a_TexCoordinate;	\n" +

	        // the matrix must be included as a modifier of gl_Position
	        " gl_Position = a_Position * uTransMatrix * uConstantMatrix; \n" +

	        "}  \n";

	public static final String FRAGMENT_SHADER_CODE = 
			"precision mediump float;  				\n" +
					"uniform sampler2D u_Texture; 	\n" +   // The input texture.
					"uniform vec4 u_Color;     		\n" +

			"varying vec2 v_TexCoordinate; 	\n" +  // This will be passed into the fragment shader.

	        "void main(){              		\n" +
	        " 		gl_FragColor = u_Color * texture2D(u_Texture, v_TexCoordinate); \n" +
	        "}                         			\n";


	public static void setColorMultiply(float r, float g, float b, float a) {
		GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, r, g, b, a);
	}

	public static void resetColorMultiply() {
		GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1, 1, 1, 1);
	}
}
