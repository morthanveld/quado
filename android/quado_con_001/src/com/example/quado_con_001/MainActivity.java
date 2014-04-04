package com.example.quado_con_001;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity 
{
	private WifiConnection wifiConnection;
	private Client m_client;
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		TextView wifiStatus = (TextView) findViewById(R.id.wifiStatus);

		// Create wifi connection.
		/*
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiConnection = new WifiConnection(wifiManager, wifiStatus);
		registerReceiver(wifiConnection, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		wifiConnection.connect();
		*/
		
		
		m_client = new Client(wifiStatus);
		
		
		MultitouchView view = (MultitouchView) findViewById(R.id.multitouchView1);
		view.registerClient(m_client);
	}
	
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
