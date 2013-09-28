package com.ggstudios.divisionbyzero;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class ActivitySplashScreen extends BaseActivity {
	private static final Handler handler = new Handler();
	
	private static final int SPLASH_SCREEN_TIME = 2000;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.splash_screen);
		
		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if(ActivitySplashScreen.this == null || ActivitySplashScreen.this.isFinishing()) {
					// this the splash screen was dismissed by the user then don't continue...
					return;
				}
				Intent i = new Intent(ActivitySplashScreen.this, ActivityMainMenu.class);
				startActivity(i);
				finish();
			}
			
		}, SPLASH_SCREEN_TIME);
	}
}
