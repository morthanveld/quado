package com.example.quado_con_001;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

public class MultitouchView extends View {

  private static final int SIZE = 60;

  private SparseArray<PointF> mActivePointers;
  private Paint mPaint;
  private int[] colors = { Color.BLUE, Color.GREEN, Color.MAGENTA,
      Color.BLACK, Color.CYAN, Color.GRAY, Color.RED, Color.DKGRAY,
      Color.LTGRAY, Color.YELLOW };

  private Paint textPaint;
  private Drawable myImage;
  
  private Joystick rightJoystick;
  private Joystick leftJoystick;


  public MultitouchView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initView();
    
    Resources res = context.getResources();
    myImage = res.getDrawable(R.drawable.joystick_frame);
    
    rightJoystick = new Joystick(context, 0.8f, 0.7f);
    leftJoystick = new Joystick(context, 0.2f, 0.7f);
  }

  private void initView() {
    mActivePointers = new SparseArray<PointF>();
    mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // set painter color to a color you like
    mPaint.setColor(Color.BLUE);
    mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    textPaint.setTextSize(20);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) 
  {
    rightJoystick.motionEvent(event);
    leftJoystick.motionEvent(event);
    
    invalidate();

    return true;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
        
    rightJoystick.draw(canvas);
    leftJoystick.draw(canvas);

    canvas.drawText("Pitch: " + 12.5f, 10, 40 , textPaint);   
    canvas.drawText("Yaw: " + 12.5f, 10, 60 , textPaint);
    canvas.drawText("Value: " + rightJoystick.getSignalValue().x + " " + rightJoystick.getSignalValue().y, 10, 80 , textPaint);
    canvas.drawText("Debug: " + rightJoystick.getDebugString(), 10, 100 , textPaint);
  }

} 