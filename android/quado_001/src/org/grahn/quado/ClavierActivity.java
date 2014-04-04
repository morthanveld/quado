
package org.grahn.quado;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.grahn.quado.R;

public class ClavierActivity extends Activity implements Runnable, SensorEventListener 
{

    private static final String TAG = "ClavierAOA";
    private static final String ACTION_USB_PERMISSION = "org.grahn.quado.USB_PERMISSION";
    private static final int MSG_INFO = 10;
   
    private static final byte MSG_SIZE = (byte) 8;
    
    // Android msg.
    private static final byte HEARTBEAT = (byte) 0x1;
    private static final byte MOTOR = (byte) 0x2;
    
    // Arduino msg.
    private static final byte STATUS = (byte) 0xA;
    private static final byte SENSOR = (byte) 0xB;
    
    // Arduino status.
    private static final byte NOT_READY = (byte) 0x0;
    private static final byte READY = (byte) 0x1;

    private boolean mPermissionRequestPending = false;
    private UsbManager mUsbManager = null;
    private UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;

    private PendingIntent mPermissionIntent;
    
    //private static final byte DTA_MODE = (byte) 4;
    private TextView textStatus;
    private TextView textOrientation;
    
    private boolean mReady = false;
    
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    float[] mGravity;
    float[] mGeomagnetic;
    Float azimut;
    float mOrientation[] = new float[3];
    
    private WifiConnection wifiConnection;
    private Server server;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() 
    {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();
            
            if (ACTION_USB_PERMISSION.equals(action)) 
            {
                synchronized (this) 
                {
                    UsbAccessory accessory = UsbManager.getAccessory(intent);
                    
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) 
                    {
                        openAccessory(accessory);
                    } 
                    else 
                    {
                        Log.d(TAG, "permission denied for accessory " + accessory);
                    }
                    mPermissionRequestPending = false;
                }
            } 
            else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) 
            {
                UsbAccessory accessory = UsbManager.getAccessory(intent);
            
                if (accessory != null && accessory.equals(mAccessory)) 
                {
                    closeAccessory();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clavier);

        // Android Accessory
        mUsbManager = UsbManager.getInstance(this);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);

        if (getLastNonConfigurationInstance() != null) 
        {
            mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
            openAccessory(mAccessory);
        }
        
        // Sensor Manager
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // User Interface
        textStatus = (TextView) findViewById(R.id.textStatus);
        textStatus.setText("Offline");
        textStatus.setKeyListener(null);
        
        textOrientation = (TextView) findViewById(R.id.textOrientation);
        textOrientation.setKeyListener(null);
        textOrientation.setText("start");
        
        TextView wifiStatus = (TextView) findViewById(R.id.wifiStatus);
        
        // Create wifi connection.
        /*WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiConnection = new WifiConnection(wifiManager, wifiStatus);
        registerReceiver(wifiConnection, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiConnection.connect();
        */
        
        TextView serverStatus = (TextView) findViewById(R.id.serverStatus);
        
        server = new Server(serverStatus);
    }

    @Override
    public Object onRetainNonConfigurationInstance() 
    {
        if (mAccessory != null) 
        {
            return mAccessory;
        } 
        else 
        {
            return super.onRetainNonConfigurationInstance();
        }
    }

    @Override
    public void onResume() 
    {
        super.onResume();
        
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);

        if (mInputStream != null && mOutputStream != null) 
        {
            return;
        }

        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) 
        {
            if (mUsbManager.hasPermission(accessory)) 
            {
                openAccessory(accessory);
            } 
            else 
            {
                synchronized (mUsbReceiver) 
                {
                    if (!mPermissionRequestPending) 
                    {
                        mUsbManager.requestPermission(accessory, mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } 
        else 
        {
            Log.d(TAG, "mAccessory is null");
        }
    }

    @Override
    public void onDestroy() 
    {
    	// Close network.
    	server.destroy();
    	
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
        
        mSensorManager.unregisterListener(this);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) 
    {
        if (keyCode == KeyEvent.KEYCODE_BACK && mAccessory != null) 
        {
            Toast.makeText(this, "Can't destroy before unplug accessory. For navigation, use HOME key.", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void openAccessory(UsbAccessory accessory) 
    {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        
        if (mFileDescriptor != null) 
        {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
            Thread listenThread = new Thread(null, this, "accessoryListener");
            listenThread.start();
            Log.d(TAG, "accessory opened");
            
            textStatus.setText("online");
        } 
        else 
        {
            Log.d(TAG, "accessory open fail");
            textStatus.setText("offline");
        }
    }

    private void closeAccessory() 
    {
        try 
        {
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
            mFileDescriptor = null;
            mAccessory = null;
            finish();
        }
    }

    /**
     * Listen accessory's voice
     */
    @Override
    public void run() 
    {
        while (true) 
        {
        	if (mReady == false)
        	{
                Message mag = Message.obtain(mHandler, MSG_INFO);
                mag.obj = new String("waiting for arduino");
                mHandler.sendMessage(mag);

        		send(HEARTBEAT, (byte)0, (byte)0);
        		read();
        	}
        	else   	
        	{
        		byte a = (byte)Math.max(mOrientation[1] / (float)Math.PI * 255.0f, 0.0f);
        		byte b = (byte)Math.max(-mOrientation[1] / (float)Math.PI * 255.0f, 0.0f); 
        		byte c = (byte)Math.max(mOrientation[2] / (float)Math.PI * 255.0f, 0.0f);
        		byte d = (byte)Math.max(-mOrientation[2] / (float)Math.PI * 255.0f, 0.0f);
        		sendMotor(a, b, c, d);
        		read();
        	}
        
            try 
            {
				Thread.sleep(100);
			} 
            catch (InterruptedException e) 
            {
				e.printStackTrace();
			}
        }
    }
    
    private void send(byte type, byte index, byte value)
    {
    	byte msg[] = new byte[MSG_SIZE];
    	
    	msg[0] = type;
    	msg[1] = index;
    	msg[2] = value;

    	try 
        {
            mOutputStream.write(msg);
        } 
        catch (IOException e) 
        {
            Log.e(TAG, "write failed", e);
        }
    }
    
    private void sendMotor(byte a, byte b, byte c, byte d)
    {
    	byte msg[] = new byte[MSG_SIZE];
    	
    	msg[0] = MOTOR;
    	msg[1] = a;
    	msg[2] = b;
    	msg[3] = c;
    	msg[4] = d;

    	try 
        {
            mOutputStream.write(msg);
        } 
        catch (IOException e) 
        {
            Log.e(TAG, "write failed", e);
        }
    }
    
    private void read()
    {
    	int ret = 0;
        byte[] buffer = new byte[MSG_SIZE];
        try 
        {
			ret = mInputStream.read(buffer, 0, MSG_SIZE);
					
			if (ret > 0)
			{
				if (buffer[0] == STATUS)
				{
					// Status from Arduino.
					if (buffer[1] == READY)
					{
						mReady = true;
					}
				}
				else if (buffer[0] == SENSOR)
				{
					// Sensor from Arduino.
				}
			}
		} 
        catch (IOException e) 
        {
			e.printStackTrace();
		}
    }

    /**
     * Do UI reaction for accessory's voice
     */
    Handler mHandler = new Handler() 
    {
        @Override
        public void handleMessage(Message msg) 
        {
            switch (msg.what) 
            {
                case MSG_INFO:
                {
                	TextView txt = (TextView) findViewById(R.id.textOrientation);
                    txt.setText((String)msg.obj);
                	break;
                }
            }
        }
    };

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) 
	{
	}

	@Override
	public void onSensorChanged(SensorEvent event) 
	{
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			mGravity = event.values;
		}
		
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
		{
			mGeomagnetic = event.values;
		}
		
		if (mGravity != null && mGeomagnetic != null) 
		{
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
			if (success) 
			{
				SensorManager.getOrientation(R, mOrientation);
				azimut = mOrientation[0]; // orientation contains: azimut, pitch and roll
				
		        Message mag = Message.obtain(mHandler, MSG_INFO);
		        mag.obj = new String(mOrientation[0] + " " + mOrientation[1] + " " + mOrientation[2]);
		        mHandler.sendMessage(mag);
			}
		}
	}
}
