package com.ggstudios.divisionbyzero;

import android.os.Bundle;
import android.widget.ImageView;

public class ActivityAbout extends BaseActivity {

	private ImageView imgTitle;
	private ImageView imgBanner;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.about);
		
		imgTitle = (ImageView) findViewById(R.id.imgTitle);
		imgBanner = (ImageView) findViewById(R.id.imgBanner);

		imgTitle.setAlpha((int)(ActivityMainMenu.ALPHA_TITLE * 255));
		imgBanner.setAlpha((int)(ActivityMainMenu.ALPHA_BANNER * 255));
		
	}
}
