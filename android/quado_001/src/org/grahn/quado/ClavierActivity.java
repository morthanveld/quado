
package org.grahn.quado;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.grahn.quado.R;

public class ClavierActivity extends Activity implements Runnable 
{

    private static final String TAG = "ClavierAOA";
    private static final String ACTION_USB_PERMISSION = "org.grahn.quado.USB_PERMISSION";
    private static final int MESSAGE_NIGHT = 1;
    private static final int MSG_RUNNING = 10;

    // Device To Accessory
   /* private static final byte DTA_PLAY = (byte) 1;
    private static final byte DTA_STOP = (byte) 2;

    // Accessory To Device
    private static final byte ATD_NIGHT = (byte) 3;*/
    
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

        textStatus = (TextView) findViewById(R.id.textStatus);
        textStatus.setText("Offline");
        textStatus.setKeyListener(null);
        
        textOrientation = (TextView) findViewById(R.id.textOrientation);
        textOrientation.setKeyListener(null);
        textOrientation.setText("start");
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
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
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

    View.OnTouchListener mTouchListener = new View.OnTouchListener() 
    {
        @Override
        public boolean onTouch(View v, MotionEvent event) 
        {
/*            if (mOutputStream == null) 
            {
                return false;
            }

            // Make packet
            byte[] buffer = new byte[2];
            if (event.getAction() == MotionEvent.ACTION_DOWN) 
            {
                buffer[0] = DTA_PLAY;
                //buffer[1] = (byte) TONE_MAPPING.get(v.getId()).intValue();
            } 
            else if (event.getAction() == MotionEvent.ACTION_UP) 
            {
                buffer[0] = DTA_STOP;
                buffer[1] = 0;
            } 
            else 
            {
                return false;
            }

            // Send packet
            try 
            {
                mOutputStream.write(buffer);
            } 
            catch (IOException e) 
            {
                Log.e(TAG, "write failed", e);
                return false;
            }
            */
            return false;
        }
    };

    /**
     * Listen accessory's voice
     */
    @Override
    public void run() 
    {
        int ret = 0;
        /*byte[] buffer = new byte[MSG_SIZE];
        int i;
        */
        
        Message mag = Message.obtain(mHandler, MSG_RUNNING);
        mag.obj = Integer.valueOf(101);
        mHandler.sendMessage(mag);
        
        while (true) 
        {
        	if (mReady == false)
        	{
        		send(HEARTBEAT, (byte)0, (byte)0);
        		read();
        	}
        	else   	
        	{
        		send(MOTOR, (byte)0, (byte)0);
        		read();
        	}
        	/*
            try 
            {
                ret = mInputStream.read(buffer);
                
            } 
            catch (IOException e) 
            {
                break;
            }
         
            i = 0;
            while (i < ret) 
            {
                int len = ret - i;

                
                switch (buffer[i]) 
                {
                    case ATD_NIGHT:
                        if (len >= 2) 
                        {
                            Message m = Message.obtain(mHandler, MESSAGE_NIGHT);
                            m.obj = Integer.valueOf(buffer[i + 1]);
                            mHandler.sendMessage(m);
                        }
                        i += 2;
                        break;
                }
            }
            */
        	           
            try 
            {
				Thread.sleep(1000);
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
                case MESSAGE_NIGHT:
                    if ((Integer) msg.obj > 0) 
                    {
                        Toast.makeText(ClavierActivity.this, "It's night! I'm going HOME!", Toast.LENGTH_SHORT).show();
                        //enableButtons(false);
                    } 
                    else
                    {
                        Toast.makeText(ClavierActivity.this, "Good morning! Give me some creative work!", Toast.LENGTH_SHORT).show();
                        //enableButtons(true);
                    }
                    break;
                case MSG_RUNNING:
                {
                	TextView txt = (TextView) findViewById(R.id.textOrientation);
                    txt.setText("running");
                	break;
                }
            }
        }
    };
}
