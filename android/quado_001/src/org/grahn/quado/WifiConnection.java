package org.grahn.quado;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.TextView;

public class WifiConnection extends BroadcastReceiver 
{
	private WifiManager m_wifiManager;
	private TextView m_wifiStatus;
	
	public WifiConnection(WifiManager wifiManager, TextView wifiStatus)
	{
		// Check for wifi is disabled
		if (wifiManager.isWifiEnabled() == false)
		{   
			// If wifi disabled then enable it
			wifiManager.setWifiEnabled(true);
		} 

				
		m_wifiManager = wifiManager; 
		m_wifiStatus = wifiStatus;
	}
	
	public void connect()
	{
		m_wifiStatus.setText("Connecting ...");
		//m_wifiManager.startScan();
		
		String ssid = new String("defcon");
		String key = new String("overlord");
		
		WifiConfiguration wifiConfig = new WifiConfiguration();
		wifiConfig.SSID = String.format("\"%s\"", ssid);
		wifiConfig.preSharedKey = String.format("\"%s\"", key);
		wifiConfig.priority = 1;

		//WifiManager wifiManager = (WifiManager)this.getSystemService(WIFI_SERVICE);
		int netId = m_wifiManager.addNetwork(wifiConfig);
		m_wifiManager.disconnect();
		m_wifiManager.enableNetwork(netId, true);
		m_wifiManager.reconnect();
	}
	
	public void onReceive(Context arg0, Intent arg1) 
	{
        //List<ScanResult> wifiList = m_wifiManager.getScanResults(); 
		//m_wifiStatus.setText("#: " + wifiList.size());
		
		WifiInfo info = m_wifiManager.getConnectionInfo();
		m_wifiStatus.setText(info.getSSID());
	}
}
