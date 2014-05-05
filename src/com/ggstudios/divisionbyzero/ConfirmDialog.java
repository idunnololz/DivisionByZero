package com.ggstudios.divisionbyzero;

import com.ggstudios.divisionbyzero.Button.OnClickListener;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;

public class ConfirmDialog extends BaseDialog {
	private static final String TAG = "ConfirmDialog";

	private static final float DIALOG_WIDTH = 10f;
	private static final float MARGIN = 0.5f;
	private static final float BUTTON_HEIGHT = 1.2f;
	
	private Label lblTitle, lblMsg;
	
	private String title, msg;
	
	private int buttonCount = 0;
	private boolean positiveExist = false, neutralExist = false, negativeExist = false;
	private Button btnPositive, btnNegative, btnNeutral;
	private String positiveText, neutralText, negativeText;
	private OnClickListener posListener, neuListener, negListener;
	
	private boolean built = false;
	
	private boolean titleExist = false;
	
	private RectF bound = new RectF();
	
	public ConfirmDialog() {
		super();
	}
	
	public void setTitle(String title) {
		this.title = title;
		
		titleExist = true;
		
		if(built) {
			lblTitle.setText(title);
		}
	} 
	
	public void setMessage(String message) {
		this.msg = message;
		
		if(built) {
			lblMsg.setText(title);
		}
	}
	
	public void setPositive(String text, OnClickListener listener) {
		if(!positiveExist){
			positiveExist = true;
			buttonCount++;
		}
		
		positiveText = text;
		posListener = listener;
		
		if(built) {
			btnPositive.setText(text);
			btnPositive.setOnClickListener(listener);
		}
	}
	
	public void setNeutral(String text, OnClickListener listener) {
		if(!neutralExist){
			neutralExist = true;
			buttonCount++;
		}
		
		neutralText = text;
		neuListener = listener;
		
		if(built) {
			btnNeutral.setText(text);
			btnNeutral.setOnClickListener(listener);
		}
	}
	
	public void setNegative(String text, OnClickListener listener) {
		if(!negativeExist){
			negativeExist = true;
			buttonCount++;
		}
		
		negativeText = text;
		negListener = listener;
		
		if(built) {
			btnNegative.setText(text);
			btnNegative.setOnClickListener(listener);
		}
	}
	
	public void build() {
		final float margin = MARGIN * Core.SDP;
		this.w = DIALOG_WIDTH * Core.SDP;
		
		setBackgroundTexture(R.drawable.panel);
		
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		paint.setTextSize(Core.SDP_H * 1.1f);
		paint.setAntiAlias(true);
		
		float l = margin;
		float t = margin;
		
		if(titleExist) {
			lblTitle = new Label(l, t, paint, title, w - (margin * 2));
			t += lblTitle.h;
		}
		lblMsg = new Label(l, t, paint, msg, w - (margin * 2));
		t += lblMsg.h;
		
		float btnH = BUTTON_HEIGHT * Core.SDP;
		float btnW = (w - margin * 2) / (buttonCount);

		this.h = t + btnH + margin * 2;
		
		t = h - margin - btnH;

		// let's match the dialog button placement with the version of the os...
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			// ICS and above button placement: Neg - Neu - Pos
			
			if(negativeExist) {
				btnNegative = new Button(l, t, btnW - margin / 2f, btnH, R.drawable.custom_button_bg, negativeText, paint);
				btnNegative.setOnClickListener(negListener);
				l += btnW;
			}
			
			if(neutralExist) {
				btnNeutral = new Button(l, t, btnW - margin / 2f, btnH, R.drawable.custom_button_bg, neutralText, paint);
				btnNeutral.setOnClickListener(neuListener);
				l += btnW;
			}
			
			if(positiveExist) {
				btnPositive = new Button(l + margin / 2f, t, btnW - margin / 2f, btnH, R.drawable.custom_button_bg, positiveText, paint);
				btnPositive.setOnClickListener(posListener);
			}
		} else {
			if(positiveExist) {
				btnPositive = new Button(l, t, btnW - margin / 2f, btnH, R.drawable.custom_button_bg, positiveText, paint);
				btnPositive.setOnClickListener(posListener);
				l += btnW;
			}
			
			if(neutralExist) {
				btnNeutral = new Button(l, t, btnW - margin / 2f, btnH, R.drawable.custom_button_bg, neutralText, paint);
				btnNeutral.setOnClickListener(neuListener);
				l += btnW;
			}
			
			if(negativeExist) {
				btnNegative = new Button(l + margin / 2f, t, btnW - margin / 2f, btnH, R.drawable.custom_button_bg, negativeText, paint);
				btnNegative.setOnClickListener(negListener);
			}
		}
		
		refresh();
		
		built = true;
		
		x = (Core.canvasWidth - w) / 2f;
		y = (Core.canvasHeight - h) / 2f;
		
		bound.left = 0;
		bound.top = 0;
		bound.right = w;
		bound.bottom = h;
	}
	
	@Override
	public void draw(float offX, float offY) {
		super.draw(0, 0);
		if(titleExist)
			lblTitle.draw(x, y);
		lblMsg.draw(x, y);
		
		btnPositive.draw(x, y);
		btnNegative.draw(x, y);
	}
	
	@Override
	public void refresh() {
		super.refresh();
		if(titleExist)
			lblTitle.refresh();
		lblMsg.refresh();
		
		if(positiveExist)
			btnPositive.refresh();
		if(neutralExist)
			btnNeutral.refresh();
		if(negativeExist)
			btnNegative.refresh();
	}
	
	@Override
	public boolean onTouchEvent(int action, float unusedX, float unusedY) {
		final float x = Core.originalTouchX - this.x;
		final float y = Core.originalTouchY - this.y;
		
		return btnPositive.onTouchEvent(action, x, y) ||
				btnNegative.onTouchEvent(action, x, y) ||
				bound.contains(x, y);
	}
}
