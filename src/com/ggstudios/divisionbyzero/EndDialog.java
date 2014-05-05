package com.ggstudios.divisionbyzero;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.graphics.Paint;

import com.ggstudios.divisionbyzero.Button.OnClickListener;
import com.ggstudios.divisionbyzero.Player.PlayerSnapshot;
import com.ggstudios.utils.DebugLog;

/**
 * This class defines the end screen where the user
 * can select options such as retry, or back to menu, etc.
 * 
 * This class needs to be able to update for animations,
 * handle touch events for button presses, and of course draw.
 * @author iDunnololz
 *
 */
public class EndDialog extends BaseDialog implements Clickable, Updatable {
	private static final String TAG = "EndScreen";

	private static final float WINDOW_WIDTH = 11;
	private static final float WINDOW_HEIGHT = 6f;

	private static final float TRANSITION_DURATION = 1f;

	private PictureBox title;

	private float time = 0;

	private boolean transitioning = false;

	private boolean loaded = false;

	private Button btnRetry, btnBack, btnOk;
	private List<Button> buttons = new ArrayList<Button>();

	/*
	 * Scores: Kills, Lives, Money Earned, Score
	 */
	private static final int NUM_SCORES = 4;
	private Label[] labels = new Label[NUM_SCORES];
	private DrawableString[] lblVals = new DrawableString[NUM_SCORES];
	private static final String[] LABEL_STRINGS = new String[] {
		"Kills",
		"Lives Left",
		"Earned",
		"Score"
	};

	private PlayerSnapshot snapshot;
	private boolean success;

	public EndDialog() {}

	public void build() {
		loaded = true;
		DebugLog.d(TAG, "build()");

		setBackgroundTexture(R.drawable.panel);

		w = WINDOW_WIDTH * Core.SDP;
		h = WINDOW_HEIGHT * Core.SDP;

		x = 0;
		y = 0;

		x = (Core.canvasWidth - w) / 2f;
		y = (Core.canvasHeight - h) / 2f;

		final float w = Core.SDP * 8f;
		final float h = Core.SDP * 1f;
		final float marginTop = Core.SDP_H;
		title = new PictureBox((this.w - w) / 2, marginTop, w, h, -1);

		final float marginL = (this.w / 2) - Core.SDP * 5f;
		final float marginR = (this.w / 2) + Core.SDP * 5f;
		final Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		paint.setTextSize(Core.fm.getFontSize());
		paint.setAntiAlias(true);
		float y = title.y + title.h + marginTop;
		float margin = Core.SDP * 0.1f;
		for(int i = 0; i < labels.length; i++) {
			labels[i] = new Label(marginL, y, paint, LABEL_STRINGS[i]);
			lblVals[i] = new DrawableString(marginR, y, Core.fm, "100000", DrawableString.ALIGN_RIGHT);

			y += labels[i].h + margin;
		}

		final float btnW = Core.SDP * 2.5f;
		final float btnH = Core.SDP;
		
		final float mR = Core.SDP_H;
		final float marginBetween = Core.SDP_H * 0.5f;

		btnRetry = new Button(this.w - btnW - mR, this.h - btnH - mR, btnW, btnH, R.drawable.custom_button_bg, "Retry", paint);
		btnBack = new Button(this.w - btnW * 2 - mR - marginBetween, this.h - btnH - mR, btnW, btnH, R.drawable.custom_button_bg, "Back", paint);
		btnOk = new Button(this.w - btnW - mR, this.h - btnH - mR, btnW, btnH, R.drawable.custom_button_bg, "Ok", paint);
		
		btnRetry.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				hide();
				Core.game.restart();
			}

		});

		btnBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				Core.handler.sendEmptyMessage(MainActivity.MSG_FINISH);
			}

		});
		
		btnOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				Core.handler.sendEmptyMessage(MainActivity.MSG_FINISH);
			}

		});

		if(snapshot != null) {
			// do some setup if we have data to setup
			lightSetup(success, snapshot);
		}

		super.refresh();
	}

	public void lightSetup(boolean success, PlayerSnapshot playerSnapshot) {
		this.snapshot = playerSnapshot;
		this.success = success;

		buttons.clear();
		
		if(success) {
			title.setTexture(R.drawable.mission_success_message);
			
			buttons.add(btnOk);
		} else {
			// failed...
			title.setTexture(R.drawable.mission_failed_message);
			
			buttons.add(btnBack);
			buttons.add(btnRetry);
		}

		lblVals[0].setText(String.valueOf(playerSnapshot.kills));
		if(playerSnapshot.lives < 0) {
			lblVals[1].setText(String.valueOf(0));
		} else {
			lblVals[1].setText(String.valueOf(playerSnapshot.lives));
		}
		lblVals[2].setText(String.valueOf(playerSnapshot.moneyEarned));
	}

	public void transitionIn() {
		transitioning = true;

		time = 0;

		transparency = 0;
		title.transparency = 0;

		Core.gu.addUiUpdatable(this);
	}

	@Override
	public boolean update(float dt) {
		time += dt;

		if(time >= TRANSITION_DURATION) {
			transitioning = false;
			return false;
		} else {
			float t = time/TRANSITION_DURATION;
			transparency = t*t;
			title.transparency = transparency;
			return true;
		}
	}
	
	@Override
	public boolean onTouchEvent(int action, float x_, float y_) {
		final float x = Core.originalTouchX - this.x;
		final float y = Core.originalTouchY - this.y;
		
		for(Button b : buttons)
			b.onTouchEvent(action, x, y);
		
		return true;
	}

	@Override
	public void draw(float offX, float offY) {
		super.draw(0, 0);
		if(transitioning) {
			title.draw(x, y);
		} else {
			title.draw(x, y);

			for(int i = 0; i < labels.length; i++) {
				labels[i].draw(x, y);
				lblVals[i].draw(x, y);
			}

			for(Button b : buttons)
				b.draw(x, y);
		}
	}

	@Override
	public void refresh() {
		if(!loaded) return;

		super.refresh();
		title.refresh();

		for(int i = 0; i < labels.length; i++) {
			labels[i].refresh();
			lblVals[i].refresh();
		}

		btnRetry.refresh();
		btnBack.refresh();
		btnOk.refresh();
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}
	
	@Override
	public void dismiss() {
		// if the user tries to dismiss this dialog, default to the back button...
		btnBack.click();
	}
}
