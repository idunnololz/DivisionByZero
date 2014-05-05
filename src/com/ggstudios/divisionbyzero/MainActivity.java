package com.ggstudios.divisionbyzero;

import com.ggstudios.utils.DebugLog;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
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
	private Game game;

	public static final int MSG_SWITCH_TO_LOAD_SCREEN = 1,
			MSG_SWITCH_TO_GLSURFACEVIEW = 2,
			MSG_FINISH = 3,
			MSG_START_LOADING_GAME_GRAPHICS = 4;

	protected final Handler handler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			DebugLog.d(TAG, "handler got: " + msg.what);
			if(Core.context == null) return false;
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

	@SuppressLint("NewApi")
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

		game = Core.game = new Game();

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
					game.loadLevel(levelResId, b.getInt(BUNDLE_LEVEL_ID));
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
		game.onPause();
	}

	@Override
	protected void onResume() {
		DebugLog.d(TAG, "onResume()");
		super.onResume();
		Core.glView.onResume();
		game.onResume();
	}

	/**
	 * Save game strategy:
	 * We want to minimalize the number of saves we do as it can be a costly operation.
	 * Thus we deploy such a strategy: If the user knowingly kills the game (press back button)
	 * then we will save the game before the user leaves. If the user puts the game in the
	 * background (press home) then it will not same yet... if the system sees the need to
	 * kill the game we will save then. 
	 * 
	 * This strategy covers most of the bases but is not fool proof. It has one weakness,
	 * that is if the user leaves the game via hold button, then discard the app's history,
	 * (ie goes to app history page then swipe to remove the game's history) then our app
	 * will not be notified and thus we will not be able to save. One can argue that a 
	 * user that performs these actions should know full well that the game will not save,
	 * but non the less it is not a desired outcome. 
	 *
	 * Later on as a todo, we may allow the user to choose a save strategy but for now
	 * this solution is quiet effective at minimalizing the number of saves we need to do.
	 */

	@Override
	protected void onStop() {
		DebugLog.d(TAG, "onStop()");
		super.onStop();

		if(isFinishing()) {
			new Thread() {
				@Override
				public void run() {
					if(!game.isGameOver()) {
						StateManager.getInstance().saveGame();
					}
					StateManager.getInstance().saveUserInfo();
				}
			}.start();
		}
	}

	@Override
	protected void onDestroy() {
		DebugLog.d(TAG, "onDestroy()");
		super.onDestroy();
		if(game != null)
			game.gameDone();

		if(!isFinishing()) {
			new Thread() {
				@Override
				public void run() {
					if(!game.isGameOver()) {
						StateManager.getInstance().saveGame();
					}
					StateManager.getInstance().saveUserInfo();
				}
			}.start();
		}
	}

	long mLastTouchTime = 0;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// do something

		game.onTouchEvent(event);

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
	
    // When an android device changes orientation usually the activity is destroyed and recreated with a new 
    // orientation layout. This method, along with a setting in the the manifest for this activity
    // tells the OS to let us handle it instead.
    //
    // This increases performance and gives us greater control over activity creation and destruction for simple 
    // activities. 
    // 
    // Must place this into the AndroidManifest.xml file for this activity in order for this to work properly 
    //   android:configChanges="keyboardHidden|orientation"
    //   optionally 
    //   android:screenOrientation="landscape"
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	DebugLog.d(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }

	@Override
	public void onBackPressed() {
		DebugLog.d(TAG, "onBackPressed()");

		if(game.onBackPressed()) {
			// if the game consumed the event, then just return...
			return;
		}

		game.onQuit();

		Core.context = null;
		
		// if the game doesn't want to do anything, we finish..
		finish();
	}
}
