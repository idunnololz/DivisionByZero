package com.ggstudios.divisionbyzero;

import com.ggstudios.utils.BitmapUtils;
import com.ggstudios.utils.DebugLog;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.view.MotionEvent;

public class Button extends PictureBox implements Clickable {
	private static final String TAG = "Button";

	private Paint paintFg, paintBg;

	private boolean isPressed = false;

	private String text;

	private Object tag;

	Align alignment;

	protected static final int 
	TYPE_TEXTURE_ONLY = 1,
	TYPE_TEXTURE_WITH_TEXT = 2,
	TYPE_PAINT_WITH_TEXT = 3;

	private int type = TYPE_TEXTURE_ONLY;

	interface OnClickListener{
		void onClick(Button sender);
	}

	private OnClickListener onClickListener;

	private Rect boundRect = new Rect();
	private Rect padding = new Rect();

	private int bgResId;

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
		super((int)x, (int)y, (int)w, (int)h, resId);

		initialize();
	}

	public Button(float x, float y, float w, float h, int resId, String text, Paint fg) {
		super((int)x, (int)y, (int)w, (int)h, -1);

		bgResId = resId;

		this.text = text;
		type = TYPE_TEXTURE_WITH_TEXT;
		alignment = Align.CENTER;
		paintFg = fg;

		paintBg = new Paint();
		paintBg.setAntiAlias(true);

		refresh();
		initialize();
	}

	public Button(float x, float y, float w, float h, String text, Paint fg, Paint bg) {
		super((int)x, (int)y, (int)w, (int)h, -1);

		this.text = text;
		type = TYPE_PAINT_WITH_TEXT;
		alignment = Align.CENTER;
		paintFg = fg;

		paintBg = bg;

		refresh();
		initialize();
	}

	public Button(float x, float y, float w, float h, String text){
		super((int)x, (int)y, 
				Utils.findSmallestBase2((int)w), Utils.findSmallestBase2((int)h), -1);
		this.text = text;

		// after supplying the super constructor with our altered width and height
		// we need to store the true width and height back
		this.w = (int)w;
		this.h = (int)h;

		generateButtonTexture(Align.CENTER);

		initialize();
	}

	public Button(float x, float y, float w, float h, String text, Align textAlign){
		super((int)x, (int)y, 
				Utils.findSmallestBase2((int)w), Utils.findSmallestBase2((int)h), -1);
		this.text = text;

		// after supplying the super constructor with our altered width and height
		// we need to store the true width and height back
		this.w = (int)w;
		this.h = (int)h;

		generateButtonTexture(textAlign);

		initialize();
	}

	private void initialize() {
		calculateBoundRect();
	}

	private void calculateBoundRect() {
		boundRect.left = 0 - padding.left;
		boundRect.top = 0 - padding.top;
		boundRect.bottom = (int) h + padding.bottom;
		boundRect.right = (int) w + padding.right;
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

	protected void setTextureType(int type) {
		this.type = type;
	}

	private void generateButtonTexture(Align alignment){
		DebugLog.d(TAG, "generating texture");

		Bitmap bitmap = Bitmap.createBitmap(Utils.findSmallestBase2((int)w), Utils.findSmallestBase2((int)h), 
				Bitmap.Config.ARGB_8888);

		bitmap.eraseColor(Color.TRANSPARENT);
		Canvas canvas = new Canvas(bitmap);
		switch(type) {
		case TYPE_TEXTURE_ONLY:
			return;
		case TYPE_TEXTURE_WITH_TEXT:
			Bitmap bg = BitmapFactory.decodeResource(Core.context.getResources(), bgResId);

			canvas.drawBitmap(bg, new Rect(0, 0, bg.getWidth(), bg.getHeight()), 
					new Rect(0, 0, (int)w, (int)h), paintBg);
			bg.recycle();
			break;
		case TYPE_PAINT_WITH_TEXT:
			canvas.drawRect(0, 0, w, h, paintBg);
			break;
		}

		float textX;
		float textY = (h - paintFg.ascent() - paintFg.descent()) / 2.0f;

		switch(alignment){
		case CENTER:
		{
			float textWidth = paintFg.measureText(text);
			textX = (w - textWidth) / 2.0f;
			break;
		}
		case LEFT:
			textX = Core.SDP / 2.0f;
			break;
		case RIGHT:
			float textWidth = paintFg.measureText(text);
			textX = w - textWidth - Core.SDP / 2.0f;
			break;
		default:
			textX = 0.0f;
			break;
		}

		canvas.drawText(text, textX, textY, paintFg);

		this.drawingW = bitmap.getWidth();
		this.drawingH = bitmap.getHeight();

		setTextureHandle(BitmapUtils.loadBitmap(bitmap));

		// remake the vbo
		super.refresh();
	}

	public void setOnClickListener(OnClickListener listener){
		this.onClickListener = listener;
	}

	@Override
	public void refresh(){
		super.refresh();
		switch(type) {
		case TYPE_TEXTURE_ONLY:
			break;
		case TYPE_TEXTURE_WITH_TEXT:
			generateButtonTexture(alignment);
			break;
		case TYPE_PAINT_WITH_TEXT:
			generateButtonTexture(alignment);
			break;
		}
	}

	public void changeLocation(int x, int y){
		this.x = x;
		this.y = y;
	}

	@Override
	public void draw(float offX, float offY) {
		if(isPressed){
			GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1.4f, 1.4f, 1.2f, 1.0f);
			super.draw(offX, offY);
			GLES20.glUniform4f(Core.U_TEX_COLOR_HANDLE, 1.0f, 1.0f, 1.0f, 1.0f);
		}else{
			super.draw(offX, offY);
		}
	}

	@Override
	public boolean onTouchEvent(int action, int x, int y) {
		final int finalX = (int) (x - this.x);
		final int finalY = (int) (y - this.y);
		
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
				return false;
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
}
