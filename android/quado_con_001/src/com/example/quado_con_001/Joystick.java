package com.example.quado_con_001;

import android.R.color;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

public class Joystick 
{
	private PointF value;
	private PointF position;
	private PointF signalValue;
	
	private int canvasWidth;
	private int canvasHeight;
	
	private int mSize;
	private int halfSize; 
	
	private int m_pointerId;
	
	private Drawable background;
	private Drawable joystick;
	
	private String debug;
	
	public Joystick(Context context, float x, float y)
	{
		// Position joystick at x, y.
		position = new PointF(x, y);
		value = new PointF(x, y);
		
		mSize = 300;
		halfSize = mSize / 2;
			
		Resources res = context.getResources();
	    background = res.getDrawable(R.drawable.joystick_frame);
	    joystick = res.getDrawable(R.drawable.joystick_ball);
	    
	    m_pointerId = -1;
	    
	    debug = new String();
	    
	    signalValue = new PointF(0.0f, 0.0f);
	}
	
	void motionEvent(MotionEvent event)
	{
		int maskedAction = event.getActionMasked();
		
		for (int i = 0; i < event.getPointerCount(); i++)
		{
			int pointerId = event.getPointerId(i);
			
			if (m_pointerId == -1)
			{
				// No pointer assigned to joystick. Test if within bounding sphere.
				PointF f = new PointF();
				f.x = event.getX(pointerId);
				f.y = event.getY(pointerId);

				if (inside(f))
				{
					// Pointer inside bounding sphere.
					m_pointerId = pointerId;
				}
			}

			if (m_pointerId == pointerId)
			{
				// Pointer is assigned to joystick, proceed.
				switch (maskedAction) 
				{
				case MotionEvent.ACTION_DOWN:
				{
					break;
				}
				case MotionEvent.ACTION_POINTER_DOWN: 
				{
					PointF f = new PointF();
					f.x = event.getX(pointerId);
					f.y = event.getY(pointerId);

					if (inside(f))
					{
						updateValue(f);
					}
					else
					{
						reset();
					}

					break;
				}
				case MotionEvent.ACTION_MOVE: 
				{
					PointF f = new PointF();
					f.x = event.getX(pointerId);
					f.y = event.getY(pointerId);

					if (inside(f))
					{
						updateValue(f);
						debug = f.x + "   " + f.y;
					}
					else
					{
						// Clamp joystick position to border.
						float x = position.x * canvasWidth;
						float y = position.y * canvasHeight;
						f.x = f.x - x;
						f.y = f.y - y;
						float l = (float) Math.sqrt(f.x * f.x + f.y * f.y);
						f.x = f.x / l;
						f.y = f.y / l;
						f.x = f.x * halfSize;
						f.y = f.y * halfSize;
						
						debug = f.x + "   " + f.y;

						PointF p = new PointF(x + f.x, y + f.y);
						updateValue(p);
					}

					break;
				}
				case MotionEvent.ACTION_UP:
				{
					reset();
					break;
				}
				case MotionEvent.ACTION_POINTER_UP:
				{
					reset();
					break;
				}
				case MotionEvent.ACTION_CANCEL: 
				{
					reset();
					break;
				}
				}
			}
		}
	}
	
	void updateValue(PointF point)
	{
		// Update position of joystick.
		value.set(point);
		
		// Update signal value.
		float x = position.x * canvasWidth;
		float y = position.y * canvasHeight;
		x = point.x - x;
		y = point.y - y;
		x = x / halfSize;
		y = y / halfSize;
		signalValue.set(x, y);
	}
	
	void reset()
	{
		m_pointerId = -1;
		PointF p = new PointF(position.x * canvasWidth, position.y * canvasHeight);
		updateValue(p);
	}
	
	boolean inside(PointF point)
	{
		PointF p = new PointF(point.x - position.x * canvasWidth, point.y - position.y * canvasHeight);
		if (p.length() < halfSize)
		{
			return true;
		}
		return false;
	}

	void draw(Canvas canvas)
	{
		canvasWidth = canvas.getWidth();
		canvasHeight = canvas.getHeight(); 
		
		// Draw joystick base on canvas.
		canvas.save();
	    canvas.translate(position.x * canvas.getWidth() - halfSize, position.y * canvas.getHeight() - halfSize);
	    background.setBounds(0, 0, mSize, mSize);
	    background.draw(canvas);
	    canvas.restore();
	    
	    if (m_pointerId == -1)
	    {
	    	reset();
	    }

	    // Draw knob.
    	Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    	mPaint.setColor(Color.BLUE);
    	canvas.drawCircle(value.x, value.y, 20, mPaint);
	}
	
	PointF getValue()
	{
		return value;
	}
	
	PointF getSignalValue()
	{
		return signalValue;
	}
	
	String getDebugString()
	{
		return debug;
	}
}
