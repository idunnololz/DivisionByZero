package com.ggstudios.divisionbyzero;

import android.app.Application;

public class ApplicationMain extends Application {
	@Override
	public void onCreate() {
		StateManager.initialize(getApplicationContext());
	}
}
