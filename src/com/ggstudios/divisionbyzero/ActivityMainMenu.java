package com.ggstudios.divisionbyzero;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Button;

public class ActivityMainMenu extends BaseActivity {
	public static final float ALPHA_BANNER = 0.2f;
	public static final float ALPHA_TITLE = 0.10f;

	private static final int ANIMATION_DURATION = 300;
	private static final int FADE_TIME = 1000;

	private static final Handler handler = new Handler();

	private RelativeLayout layout;
	private Button btnPlay;
	private Button btnSettings;
	private Button btnAbout;
	private Button btnExit;

	private ImageView imgTitle;
	private ImageView imgBanner;

	private boolean navigatedOut = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main_menu);

		layout = (RelativeLayout) findViewById(R.id.layout);
		btnPlay = (Button) findViewById(R.id.btnPlay);
		btnSettings = (Button) findViewById(R.id.btnSettings);
		btnAbout = (Button) findViewById(R.id.btnAbout);
		btnExit = (Button) findViewById(R.id.btnExit);
		imgTitle = (ImageView) findViewById(R.id.imgTitle);
		imgBanner = (ImageView) findViewById(R.id.imgBanner);

		if(savedInstanceState != null) {
			// resurrecting activity...
			navigatedOut = savedInstanceState.getBoolean("navigatedOut");
		}
	}

	@Override
	public void onSaveInstanceState(Bundle b) {
		super.onSaveInstanceState(b);
		b.putBoolean("navigatedOut", navigatedOut);
	}

	@Override
	public void onResume() {
		super.onResume();
		restoreButtons();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	private void restoreButtons() {
		if(navigatedOut) {
			navigateIn(null);
		}
	}

	private void fadeInBanner() {
		Animation fadeIn = new AlphaAnimation(ALPHA_BANNER, 1);
		fadeIn.setDuration(FADE_TIME);
		fadeIn.setFillAfter(true);

		Animation fadeIn2 = new AlphaAnimation(ALPHA_TITLE, 1);
		fadeIn2.setDuration(FADE_TIME);
		fadeIn2.setFillAfter(true);

		imgBanner.startAnimation(fadeIn);
		imgTitle.startAnimation(fadeIn2);
	}

	private void fadeOutBanner() {
		Animation fadeOut = new AlphaAnimation(1, ALPHA_BANNER);
		fadeOut.setDuration(FADE_TIME);
		fadeOut.setFillAfter(true);

		Animation fadeOut2 = new AlphaAnimation(1, ALPHA_TITLE);
		fadeOut2.setDuration(FADE_TIME);
		fadeOut2.setFillAfter(true);

		imgBanner.startAnimation(fadeOut);
		imgTitle.startAnimation(fadeOut2);
	}

	private void dismissView(final View v) {
		final float delta = layout.getWidth() - v.getLeft();
		TranslateAnimation animation = new TranslateAnimation(0, delta, 0, 0);
		animation.setDuration(ANIMATION_DURATION);
		animation.setFillAfter(true);
		v.startAnimation(animation);
	}
	
	private void introduceView(final View v) {
		final float delta = layout.getWidth() - v.getLeft();
		TranslateAnimation animation = new TranslateAnimation(delta, 0, 0, 0);
		animation.setDuration(ANIMATION_DURATION);
		animation.setFillAfter(true);
		v.startAnimation(animation);
	}

	private void navigateOut(Runnable onFinish) {
		if(navigatedOut) return; // prevent double tapping...
		
		navigatedOut = true;

		fadeOutBanner();

		dismissView(btnExit);

		final int delta = ANIMATION_DURATION/2;
		int delay = delta;

		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				dismissView(btnAbout);
			}

		}, delay);
		delay += delta;

		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				dismissView(btnSettings);
			}

		}, delay);
		delay += delta;

		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				dismissView(btnPlay);
			}

		}, delay);
		delay += delta;

		handler.postDelayed(onFinish, Math.max(delay, FADE_TIME));
	}
	
	private void navigateIn(Runnable onFinish) {
		navigatedOut = false;

		fadeInBanner();

		introduceView(btnPlay);

		final int delta = ANIMATION_DURATION/2;
		int delay = delta;

		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				introduceView(btnSettings);
			}

		}, delay);
		delay += delta;

		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				introduceView(btnAbout);
			}

		}, delay);
		delay += delta;

		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				introduceView(btnExit);
			}

		}, delay);
		delay += delta;

		if(onFinish != null)
			handler.postDelayed(onFinish, Math.max(delay, FADE_TIME));
	}

	public void onExitClicked(View v) {
		finish();
	}

	public void onSettingsClicked(View v) {
		navigateOut(new Runnable() {

			@Override
			public void run() {
				Intent i = new Intent(ActivityMainMenu.this, ActivitySettings.class);
				startActivity(i);				
			}

		});
	}

	public void onAboutClicked(View v) {
		navigateOut(new Runnable() {

			@Override
			public void run() {
				Intent i = new Intent(ActivityMainMenu.this, ActivityAbout.class);
				startActivity(i);				
			}

		});
	}

	public void onPlayClicked(View v) {
		navigateOut(new Runnable() {

			@Override
			public void run() {
				Intent i = new Intent(ActivityMainMenu.this, ActivityLevelSelection.class);
				startActivity(i);				
			}

		});

		//finish();
	}
}
