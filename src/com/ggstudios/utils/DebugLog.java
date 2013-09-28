package com.ggstudios.utils;

import android.util.Log;

public class DebugLog {
	private static final boolean isLoggingEnabled = true;
	
	public static void d(String tag, String msg){
		if(isLoggingEnabled)
			Log.d(tag, msg);
	}
	
	public static void e(String tag, String msg){
		if(isLoggingEnabled)
			Log.e(tag, msg);
	}
	
	public static void e(String tag, String msg, Throwable t){
		if(isLoggingEnabled)
			Log.e(tag, msg, t);
	}
	
	public static void e(String tag, Throwable t){
		if(isLoggingEnabled)
			Log.e(tag, "", t);
	}
}
