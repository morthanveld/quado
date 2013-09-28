package com.example.quado;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.TextView;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class quado extends Activity implements SensorEventListener 
{ 
	private UsbManager mUsbManager = UsbManager.getInstance(this);

	private UsbAccessory mAccessory;

	private ParcelFileDescriptor mFileDescriptor;
	private FileOutputStream mOutputStream;

	private BroadcastReceiver mReceiver;

	private SensorManager mSensorManager;
	private TextView text;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		//Creating TextView Variable
        text = (TextView) findViewById(R.id.textView1);
        text.setText("apa");
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); 
  	}

	@Override
	protected void onResume() 
	{
		text.setText("onResume()");
		super.onResume();
		mSensorManager.registerListener(this,
		        mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);

		setupDetachingAccessoryHandler();
		reOpenAccessoryIfNecessary();
	}
	
	protected void onPause()
	{
		super.onPause();
	    mSensorManager.unregisterListener(this);
	}

	private void reOpenAccessoryIfNecessary() 
	{
		text.setText("reOpenAccessory()");
		if (mOutputStream != null) 
		{
			return;
		}

		Intent intent = getIntent();
		String action = intent.getAction();
		if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) 
		{
			mAccessory = UsbManager.getAccessory(intent);
			openAccessory();
		}
	}

	private void openAccessory() 
	{
		try 
		{
			mFileDescriptor = mUsbManager.openAccessory(mAccessory);
			if (mFileDescriptor != null) 
			{
				FileDescriptor fd = mFileDescriptor.getFileDescriptor();
				mOutputStream = new FileOutputStream(fd);
			}
		} 
		catch (IllegalArgumentException ex) 
		{
			// Accessory detached while activity was inactive
			closeAccessory();
		}
	}

	private void setupDetachingAccessoryHandler() 
	{
		mReceiver = new BroadcastReceiver() 
		{
			@Override
			public void onReceive(Context context, Intent intent) 
			{
				String action = intent.getAction();
				if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) 
				{
					closeAccessory();
					finish();
				}
			}
		};

		IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mReceiver, filter);
	}

	private void closeAccessory() 
	{
		try 
		{
			if (mOutputStream != null) 
			{
				mOutputStream.close();
			}
			if (mFileDescriptor != null) 
			{
				mFileDescriptor.close();
			}
		} 
		catch (IOException e) 
		{	
		} 
		finally 
		{
			mOutputStream = null;
			mFileDescriptor = null;
			mAccessory = null;
		}
	}

	private void sendCommand(byte command) 
	{
		if (mOutputStream != null) 
		{
			try 
			{
				mOutputStream.write(command);
			} 
			catch (IOException e) 
			{
				// Do nothing
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) 
	{
		// TODO Auto-generated method stub
		text.setText("onAccuracy()");
	}

	@Override
	public void onSensorChanged(SensorEvent event) 
	{
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) 
		{
			// NO GOOD RESULT WITH THIS ONE!
			float[] values = event.values;
			float x = values[0];
			float y = values[1];
			float z = values[2];
			byte A = 'A';
			byte n = '\n';
			sendCommand(A);
			sendCommand(n);
			
	        //Sets the new text to TextView (runtime click event)
			text.setText(String.format("%.2f", x) + "\n" + String.format("%.2f", y) + "\n" + String.format("%.2f", z));
		}
		else if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) 
		{
			float[] values = event.values;
			float x = values[0];
			float y = values[1];
			float z = values[2];
			byte A = 'A';
			byte n = '\n';
			sendCommand(A);
			sendCommand(n);
			
	        //Sets the new text to TextView (runtime click event)
			text.setText(String.format("%.2f", x) + "\n" + String.format("%.2f", y) + "\n" + String.format("%.2f", z));
		}
	}
}
