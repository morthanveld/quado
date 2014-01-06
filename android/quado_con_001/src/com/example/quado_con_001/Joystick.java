package com.example.quado_con_001;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

public class Joystick {
	
	private boolean active = false;
	private PointF value;
	private PointF position;
	
	private int mSize;
	private int halfSize; 
	
	private Drawable background;
	private Drawable joystick;
	
	public Joystick(Context context, float x, float y)
	{
		// Position joystick at x, y.
		position = new PointF(x, y);
		
		mSize = 300;
		halfSize = mSize / 2;
		
		Resources res = context.getResources();
	    background = res.getDrawable(R.drawable.joystick_frame);
	    joystick = res.getDrawable(R.drawable.joystick_ball);
	}
	
	void motionEvent(PointF point)
	{
		if (inside(point))
		{
			active = true;
		}
		else
		{
			active = false;
		}
	}
	
	boolean inside(PointF point)
	{
		PointF p = new PointF(point.x - position.x, point.y - position.y);
		if (p.length() < halfSize)
		{
			return true;
		}
		dqwdqwqwdws
		return false;
	}

	void draw(Canvas canvas)
	{
		// Draw joystick on canvas.
		canvas.save();
	    canvas.translate(position.x * canvas.getWidth() - halfSize, position.y * canvas.getHeight() - halfSize);
	    background.setBounds(0, 0, mSize, mSize);
	    background.draw(canvas);
	    canvas.restore();
		
		// Draw joystick position.
	    if (active)
	    {
	    	canvas.translate(position.x * canvas.getWidth() - halfSize / 2, position.y * canvas.getHeight() - halfSize / 2);
	    	joystick.setBounds(0, 0, halfSize, halfSize);
	    	joystick.draw(canvas);
	    	canvas.restore();
	    }
	    
	}
	
	PointF getValue()
	{
		return value;
	}
}
