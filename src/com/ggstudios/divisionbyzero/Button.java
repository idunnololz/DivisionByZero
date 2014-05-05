package com.ggstudios.divisionbyzero;

import com.ggstudios.utils.BitmapUtils;
import com.ggstudios.utils.DebugLog;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.view.MotionEvent;

public class Button extends PictureBox implements Clickable {
	private static final String TAG = "Button";

	private Paint paintFg, paintBg;

	private boolean isPressed = false;

	private String text;

	private Object tag;

	private Label lblText;
	
	private boolean enabled = true;
	
	private int bgResId = -1;
	
	interface OnClickListener{
		void onClick(Button sender);
	}

	private OnClickListener onClickListener;

	private RectF boundRect = new RectF();
	private RectF padding = new RectF();

	protected Button(float w, float h) {
		super(0, 0);

		this.w = w;
		this.h = h;

		initialize();
	}

	/**
	 * 
	 * @param x Button's x coordinate
	 * @param y Button's y coordinate
	 * @param w Button width
	 * @param h Button height
	 * @param textureHandle a handle to the texture to be drawn on the button
	 */
	public Button(float x, float y, float w, float h, int resId) {
		super(x, y, w, h, -1);

		bgResId = resId;
		
		initialize();
		buildBg();
	}

	public Button(float x, float y, float w, float h, int resId, String text, Paint fg) {
		super(x, y, w, h, -1);

		this.text = text;
		paintFg = fg;

		paintBg = new Paint();
		paintBg.setAntiAlias(true);

		bgResId = resId;
		
		initialize();
		buildBg();
	}

	public Button(float x, float y, float w, float h, String text, Paint fg) {
		super(x, y, w, h, -1);

		this.text = text;
		paintFg = fg;

		initialize();
	}

	public Button(float x, float y, float w, float h, String text){
		super(x, y, w, h, -1);
		this.text = text;

		// after supplying the super constructor with our altered width and height
		// we need to store the true width and height back
		this.w = (int)w;
		this.h = (int)h;

		initialize();
	}

	private void initialize() {
		calculateBoundRect();
		
		if(paintFg == null) {
			paintFg = new Paint();
			paintFg.setTextSize(Core.SDP * 0.4f);
			paintFg.setAntiAlias(true);
			paintFg.setColor(0xFFFFFFFF);
		}
		lblText = new Label(0, 0, paintFg, text);
		
		onTextChanged();
	}
	
	private void buildBg() {
		if(bgResId == -1) return;
		
		Drawable drawable = Core.context.getResources().getDrawable(bgResId);
		drawable.setBounds(0, 0, (int)w, (int)h);
		
		Bitmap bitmap = Bitmap.createBitmap((int) w, (int) h, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bitmap);
		drawable.draw(c);
		
		int textureHandle = BitmapUtils.loadBitmap(bitmap);
		
		setTextureHandle(textureHandle);
	}
	
	private void onTextChanged() {
		lblText.x = (w - lblText.w) / 2f;
		lblText.y = (h - lblText.h) / 2f;
	}

	private void calculateBoundRect() {
		boundRect.left = 0 - padding.left;
		boundRect.top = 0 - padding.top;
		boundRect.bottom = h + padding.bottom;
		boundRect.right = w + padding.right;
	}

	public void setPadding(int l, int t, int r, int b) {
		padding.left = l;
		padding.top = t;
		padding.right = r;
		padding.bottom = b;
		calculateBoundRect();
	}

	public void setPadding(int p) {
		padding.left = p;
		padding.top = p;
		padding.right = p;
		padding.bottom = p;
		calculateBoundRect();
	}

	public void setPaddingBottom(int marginBottom) {
		padding.bottom = marginBottom;
		calculateBoundRect();
	}

	public void setOnClickListener(OnClickListener listener){
		this.onClickListener = listener;
	}

	@Override
	public void refresh(){
		super.refresh();
		
		if(lblText == null) return;
		lblText.refresh();
		
		buildBg();
	}

	public void setLocation(int x, int y){
		this.x = x;
		this.y = y;
	}

	@Override
	public void draw(float offX, float offY) {
		if(!isVisible) return;
		
		if(isPressed){
			GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1.4f, 1.4f, 1.2f, 1.0f);
			if(textureHandle != -1)
				super.draw(offX, offY);
			if(text != null)
				lblText.draw(x + offX, y + offY);
			GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1.0f, 1.0f, 1.0f, 1.0f);
		}else{
			if(textureHandle != -1)
				super.draw(offX, offY);
			if(text != null)
				lblText.draw(x + offX, y + offY);
		}
	}

	@Override
	public boolean onTouchEvent(int action, float x, float y) {
		if(!enabled) return false;
		
		final float finalX = x - this.x;
		final float finalY = y - this.y;
		
		switch(action) {
		case MotionEvent.ACTION_DOWN:
			if(boundRect.contains(finalX, finalY)){
				isPressed = true;
				return true;
			}else{
				isPressed = false;
				return false;
			}
		case MotionEvent.ACTION_MOVE:
			if(boundRect.contains(finalX, finalY)){
				return true;
			}else{
				isPressed = false;
				return false;
			}
		case MotionEvent.ACTION_UP:
			if(boundRect.contains(finalX, finalY)){

				if(isPressed){
					isPressed = false;
					if(onClickListener != null)
						onClickListener.onClick(this);

					return true;
				}
				return false;
			}
			return false;
		case MotionEvent.ACTION_CANCEL:
			isPressed = false;
			return false;
		default:
			return false;
		}
	}
	
	public Object getTag() {
		return tag;
	}

	public void setTag(Object tag) {
		this.tag = tag;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setText(String text) {
		lblText.setText(text);
		this.text = text;
		
		onTextChanged();
	}

	public void click() {
		if(onClickListener != null)
			onClickListener.onClick(this);		
	}
}
