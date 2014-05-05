package com.ggstudios.divisionbyzero;

import java.io.IOException;
import java.util.List;

import com.ggstudios.divisionbyzero.LevelMap.ExtraLevelInfo;
import com.ggstudios.divisionbyzero.LevelMap.LevelNode;
import com.ggstudios.divisionbyzero.StateManager.OnSavedStateChanged;
import com.ggstudios.divisionbyzero.StateManager.UserLevelData;
import com.ggstudios.utils.DebugLog;
import com.ggstudios.widget.LevelMapBackground;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;

public class ActivityLevelSelection extends BaseActivity {
	private static final String TAG = ActivityLevelSelection.class.getSimpleName();

	private static final int LEVEL_DEPTH_DP = 40;

	//private ScrollView superView;
	private RelativeLayout layout;
	private ImageView imgTitle;
	private ImageView imgBanner;
	private TextView txtPageTitle;
	private LevelMapBackground levelMapBg;
	private Button btnResume;

	private DisplayMetrics displayMetrics;

	private static final int ANIMATION_DURATION = 300;

	private LevelMap lm;
	private StateManager stateMgr;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		stateMgr = StateManager.getInstance();

		setContentView(R.layout.level_selection);

		//superView = (ScrollView) findViewById(R.id.superView);
		layout = (RelativeLayout) findViewById(R.id.layout);
		imgTitle = (ImageView) findViewById(R.id.imgTitle);
		imgBanner = (ImageView) findViewById(R.id.imgBanner);
		txtPageTitle = (TextView) findViewById(R.id.txtPageTitle);
		levelMapBg = (LevelMapBackground) findViewById(R.id.levelMap);
		btnResume = (Button) findViewById(R.id.btnResume);

		AlphaAnimation alpha = new AlphaAnimation(0f, 0f);
		alpha.setDuration(0); // Make animation instant
		alpha.setFillAfter(true); // Tell it to persist after the animation ends
		// And then on your layout
		layout.startAnimation(alpha);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			imgTitle.setAlpha((int)(ActivityMainMenu.ALPHA_TITLE * 255));
			imgBanner.setAlpha((int)(ActivityMainMenu.ALPHA_BANNER * 255));
		} else {
			imgTitle.setImageAlpha((int)(ActivityMainMenu.ALPHA_TITLE * 255));
			imgBanner.setImageAlpha((int)(ActivityMainMenu.ALPHA_BANNER * 255));	
		}

		displayMetrics = getResources().getDisplayMetrics();

		lm = stateMgr.getLevelMap();

		final ViewTreeObserver vto = txtPageTitle.getViewTreeObserver();  
		if(vto.isAlive()) {
			vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {  
				@Override  
				public void onGlobalLayout() {
					if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
						txtPageTitle.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					} else {
						txtPageTitle.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}

					Animation a = new AlphaAnimation(0f, 1f);
					a.setDuration(ANIMATION_DURATION);
					txtPageTitle.startAnimation(a);

					loadMap();
				}

			}); 
		}
	}

	private void refreshSavedState() {
		if(stateMgr.isSavedGame()) {
			btnResume.setVisibility(View.VISIBLE);
		} else {
			btnResume.setVisibility(View.GONE);
		}
	}

	@Override
	public void onResume() {
		DebugLog.d(TAG, "onResume()");
		super.onResume();

		refreshSavedState();
		refreshLevelData();

		stateMgr.setOnSavedStateChanged(new OnSavedStateChanged() {

			@Override
			public void onSavedStateChanged() {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						refreshSavedState();
					}
					
				});
			}

		});
	}

	@Override
	public void onPause() {
		super.onPause();

		stateMgr.setOnSavedStateChanged(null);
	}

	private void loadMap() {
		new Thread() {

			@Override
			public void run() {
				try {
					lm.loadFrom(R.raw.level_overview);
				} catch (IOException e) {
					DebugLog.e(TAG, e);
				}

				SparseArray<LevelNode> arr = lm.getRaw();

				final Activity act = ActivityLevelSelection.this;

				// pre-measure the size of a point once so that we can do centering
				// or other layout stuff...
				int imgSize;
				{
					final ImageView img = new ImageView(act);
					img.setImageResource(R.drawable.level_point_unreachable);
					img.measure(MeasureSpec.makeMeasureSpec(RelativeLayout.LayoutParams.WRAP_CONTENT, MeasureSpec.EXACTLY), 
							MeasureSpec.makeMeasureSpec(RelativeLayout.LayoutParams.WRAP_CONTENT, MeasureSpec.EXACTLY));
					imgSize = img.getMeasuredWidth();
					imgSize = Math.min(imgSize, 100);
				}

				int marginLeft = toPixels(10);

				// the margin between points calculated by doing totalDistance / num_points;
				int pointMargin = (layout.getWidth() - marginLeft * 2 - imgSize) / lm.getTopNode().getHeight();

				ImageView lastView = null;

				for(int i = 0; i < arr.size(); i++){
					final LevelNode node = arr.valueAt(i);

					// after we are done loading... draw the tree out!
					final ImageView img = new ImageView(act);
					// add a bit of padding to make it easier to click...
					img.setPadding(toPixels(10), toPixels(10), toPixels(10), toPixels(10));

					setLevelNodeView(img, node.status);

					img.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View arg0) {
							onLevelNodeClicked(node);
						}

					});

					final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
							RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

					params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
					params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
					params.setMargins(marginLeft + node.depth * pointMargin, (layout.getHeight() - imgSize) / 2 + toPixels(node.hintY * LEVEL_DEPTH_DP * -1), 0, 0);

					act.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							layout.addView(img, params);
						}

					});

					node.view = img;

					lastView = img;
				}

				final ImageView lv = lastView;

				final ViewTreeObserver vto = lastView.getViewTreeObserver();  
				if(vto.isAlive()) {
					vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

						@SuppressWarnings("deprecation")
						@Override
						public void onGlobalLayout() {
							lv.getViewTreeObserver().removeGlobalOnLayoutListener(this);

							DebugLog.d(TAG, "layout height: " + layout.getHeight());
							DebugLog.d(TAG, "map height: " + levelMapBg.getHeight());


							DebugLog.d(TAG, "t: " + levelMapBg.getTop());
							final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
									RelativeLayout.LayoutParams.MATCH_PARENT, layout.getHeight());
							params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
							params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);

							levelMapBg.setData(lm.getRaw());
							levelMapBg.setLayoutParams(params);
							layout.requestLayout();

							Animation animation = new AlphaAnimation(0f, 1f);
							animation.setDuration(ANIMATION_DURATION);
							animation.setFillAfter(true);
							animation.setFillEnabled(true);
							layout.startAnimation(animation);

							loadLevelData();
						}

					});
				}
			}

		}.start();
	}

	private void setLevelNodeView(ImageView img, int status) {
		int resId;
		switch(status) {
		case LevelNode.STATUS_CLOSED:
			resId = R.drawable.level_point_unreachable;
			break;
		case LevelNode.STATUS_OPEN:
			resId = R.drawable.level_point_reachable;
			break;
		case LevelNode.STATUS_COMPLETED:
			resId = R.drawable.level_point_complete;
			break;
		default:
			resId = R.drawable.level_point_red;
			break;
		}
		img.setImageResource(resId);
	}

	private void onLevelNodeClicked(final LevelNode node) {
		LayoutInflater inflater = getLayoutInflater();
		final View v = inflater.inflate(R.layout.level_selection_dialog, null);

		if(!node.isLoaded()) {
			new Thread() {
				@Override
				public void run() {
					node.loadLevelData(getApplicationContext());

					final ExtraLevelInfo info = node.getExtraInfo();
					
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							((TextView) v.findViewById(R.id.txtSubtext)).setText("lv_" + node.id);
						}

					});

					if(info == null) return;

					final StringBuilder str = new StringBuilder();
					str.append("Difficulty: ");
					str.append(info.getDifficulty());
					str.append("\nMap size: ");
					str.append(info.getMapSize());
					str.append("\nWaves: ");
					str.append(info.waveCount);

					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							((TextView) v.findViewById(R.id.txtTitle)).setText(info.levelName);
							((TextView) v.findViewById(R.id.txtText)).setText(str.toString());
						}

					});
				}
			}.start();
		}

		Button b = (Button) v.findViewById(R.id.btnStart);

		if(node.status == LevelNode.STATUS_CLOSED)
			b.setEnabled(false);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(v);

		final AlertDialog dialog = builder.create();
		dialog.show();
		
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				DebugLog.d(TAG, "Node clicked: " + node.id);

				int id = node.getLevelResId(getApplicationContext());
				if(id == 0) {
					DebugLog.e(TAG, "Resource not found! lv_" + node.id);
				} else {
					Intent i = new Intent(ActivityLevelSelection.this, MainActivity.class);
					i.putExtra(MainActivity.BUNDLE_LEVEL, id);
					i.putExtra(MainActivity.BUNDLE_LEVEL_ID, node.id);
					startActivity(i);
					
					dialog.dismiss();
				}
			}

		});
	}

	private void loadLevelData() {	
		stateMgr.loadLevelData();

		List<UserLevelData> data = stateMgr.getUserLevelData();
		for(UserLevelData dat : data) {
			lm.setCompleted(dat.id);
		}

		refreshLevelData();
	}

	private void refreshLevelData() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				SparseArray<LevelNode> raw = lm.getRaw();
				final int len = raw.size();
				for(int i = 0; i < len; i++) {
					final LevelNode ln = raw.valueAt(i);
					ImageView img = (ImageView) ln.view;
					setLevelNodeView(img, ln.status);
				}
			}

		});
	}

	public int toPixels(int dp) {
		return (int)((dp * displayMetrics.density) + 0.5);
	}

	public int toDps(int px) {
		return (int) ((px/displayMetrics.density)+0.5);
	}

	public void onResumeClicked(View v) {
		Intent i = new Intent(ActivityLevelSelection.this, MainActivity.class);
		i.putExtra(MainActivity.BUNDLE_LOAD, 1);
		startActivity(i);
	}
}
