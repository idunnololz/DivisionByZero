package com.ggstudios.divisionbyzero;

import com.ggstudios.utils.DebugLog;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";

	public static final String BUNDLE_LEVEL = "level";
	public static final String BUNDLE_LEVEL_ID = "level_id";
	public static final String BUNDLE_LOAD = "load";

	private RelativeLayout loadView;

	public static final int MSG_SWITCH_TO_LOAD_SCREEN = 1,
			MSG_SWITCH_TO_GLSURFACEVIEW = 2,
			MSG_FINISH = 3,
			MSG_START_LOADING_GAME_GRAPHICS = 4;

	protected final Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_SWITCH_TO_LOAD_SCREEN:
				playLoadingAnimation();
				//Core.glView.onPause();
				loadView.setVisibility(View.VISIBLE);
				return true;
			case MSG_SWITCH_TO_GLSURFACEVIEW:
				//Core.glView.onResume();
				loadView.setVisibility(View.GONE);
				stopAnimation();
				return true;
			case MSG_FINISH:
				finish();
				return true;
			case MSG_START_LOADING_GAME_GRAPHICS:
				//Core.glView.onResume();
				Core.glView.setVisibility(View.VISIBLE);
				return true;
			default:
				return false;
			}
		}
	});

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		DebugLog.d(TAG, "onCreate()");

		Core.handler = handler;
		Core.context = this;

		//remove title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		//remove notification bar
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		Core.game = new Game();

		setContentView(R.layout.activity_main);

		Core.glView = (CustomGLSurfaceView) findViewById(R.id.glView);
		loadView = (RelativeLayout) findViewById(R.id.loadView);
		//Core.glView.onPause();
		Core.glView.setVisibility(View.GONE);

		playLoadingAnimation();
		
		final Bundle b = getIntent().getExtras();
		if(b.containsKey(BUNDLE_LEVEL)) {
			final int levelResId = b.getInt(BUNDLE_LEVEL);

			new Thread() {
				@Override
				public void run() {
					Core.game.loadLevel(levelResId, b.getInt(BUNDLE_LEVEL_ID));
					handler.sendEmptyMessage(MSG_START_LOADING_GAME_GRAPHICS);
				}
			}.start();
		} else if(b.containsKey(BUNDLE_LOAD)) {
			new Thread() {
				@Override
				public void run() {
					handler.sendEmptyMessage(MSG_START_LOADING_GAME_GRAPHICS);
					StateManager.getInstance().loadGame();
				}
			}.start();
		}
	}
	
	private void playLoadingAnimation() {
		Animation animation = new AlphaAnimation(0, 1);
		animation.setRepeatMode(Animation.REVERSE);
		animation.setRepeatCount(Animation.INFINITE);
		animation.setDuration(500);
		
		View v = findViewById(R.id.txtLoading);
		v.startAnimation(animation);
	}
	
	private void stopAnimation() {
		View v = findViewById(R.id.txtLoading);
		v.clearAnimation();
	}

	@Override
	protected void onPause() {
		DebugLog.d(TAG, "onPause()");
		super.onPause();
		Core.glView.onPause();
		Core.game.onPause();
	}

	@Override
	protected void onResume() {
		DebugLog.d(TAG, "onResume()");
		super.onResume();
		Core.glView.onResume();
		Core.game.onResume();
	}

	@Override
	protected void onStop() {
		DebugLog.d(TAG, "onStop()");
		super.onStop();

		if(isFinishing() && !Core.game.isGameOver()) {
			StateManager.getInstance().saveGame();
		}
	}

	long mLastTouchTime = 0;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// do something

		Core.game.onTouchEvent(event);

		final long time = System.currentTimeMillis();

		if (event.getAction() == MotionEvent.ACTION_MOVE && time - mLastTouchTime < 32) {
			// Sleep so that the main thread doesn't get flooded with UI events.
			try {
				Thread.sleep(32);
			} catch (InterruptedException e) {
				// No big deal if this sleep is interrupted.
			}
		}
		mLastTouchTime = time;
		return true;
	}

	@Override
	public void onBackPressed() {
		DebugLog.d(TAG, "onBackPressed()");
		finish();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(Core.game != null)
			Core.game.gameDone();
	}
}
