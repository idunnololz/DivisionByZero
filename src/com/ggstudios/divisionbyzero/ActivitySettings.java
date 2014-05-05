package com.ggstudios.divisionbyzero;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ActivitySettings extends BaseActivity {
	private static final int FADE_TIME = 500;
	
	private RelativeLayout mainLayout;
	
	private ImageView imgTitle;
	private ImageView imgBanner;
	private StateManager stateMgr;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		stateMgr = StateManager.getInstance();
		
		setContentView(R.layout.settings);
		
		mainLayout = (RelativeLayout) findViewById(R.id.mainLayout);
		imgTitle = (ImageView) findViewById(R.id.imgTitle);
		imgBanner = (ImageView) findViewById(R.id.imgBanner);

		imgTitle.setAlpha((int)(ActivityMainMenu.ALPHA_TITLE * 255));
		imgBanner.setAlpha((int)(ActivityMainMenu.ALPHA_BANNER * 255));
		
		Animation animation = new AlphaAnimation(0f, 1f);
		animation.setDuration(FADE_TIME);
		mainLayout.startAnimation(animation);
	}
	
	public void onWipeDataClicked(View v) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
		View dialogView = inflater.inflate(R.layout.custom_dialog, null);
		builder.setView(dialogView)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                stateMgr.clearLevelData();
                stateMgr.clearUserData();
                
                // burn all data cached in memory >:D
                stateMgr.getUserLevelData().clear();
                
                AlertDialog.Builder b = new AlertDialog.Builder(ActivitySettings.this);
                b.setMessage("User data has been wiped.");
                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
                b.create().show();
            }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                
            }
        });      
		((TextView) dialogView.findViewById(R.id.text)).setText("This will wipe all user data. Proceed?");
		
		builder.create().show();
	}
}
