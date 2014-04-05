package com.example.quado_con_001;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.Handler;
import android.widget.TextView;

public class Client 
{
	private Socket m_socket;
	private static final String m_serverIp = "192.168.1.1";
	private static final int m_serverPort = 1414;
	private TextView m_status;
	private Handler m_communicationHandler;
	
	private byte[] m_inputData;
	private byte[] m_outputData;
	
	public Client(TextView status)
	{
		m_status = status;
		m_communicationHandler = new Handler();
		this.m_inputData = new byte[8];
		this.m_outputData = new byte[8];
		
		/*
		m_outputData[0] = 'A';
		m_outputData[1] = 'B';
		m_outputData[2] = 'B';
		m_outputData[3] = 'A';
		*/
		
		new Thread(new ClientThread(this)).start();
	}
	
	/*
	 * Function for setting outgoing data on socket.
	 */
	public void setOutputData(byte[] data)
	{
		this.m_outputData = data;
	}
	
	public byte[] getOutputData()
	{
		return this.m_outputData;
	}
	
	/*
	 * Open up a socket and create a communication thread.
	 */
	class ClientThread implements Runnable 
	{
		private Client m_client;
		
		public ClientThread(Client client)
		{
			m_client = client;
		}
		
		public void run() 
		{
			try 
			{
				InetAddress serverAddr = InetAddress.getByName(m_serverIp);
				m_socket = new Socket(serverAddr, m_serverPort);
				
				m_status.setText("connected to quado");
				new Thread(new CommunicationThread(m_client, m_socket)).start();
			} 
			catch (UnknownHostException e1) 
			{
				m_status.setText("unknown host");
				e1.printStackTrace();
			} 
			catch (IOException e1) 
			{
				m_status.setText("io exception");
				e1.printStackTrace();
			}
		}
	}
	
	/*
	 * Communication thread reads from socket and posts work to communication handler.
	 */
	class CommunicationThread implements Runnable 
	{
		private Socket m_clientSocket;
		private BufferedReader m_input;
		private Client m_client;
		
		public CommunicationThread(Client client, Socket clientSocket) 
		{
			this.m_client = client;
			this.m_clientSocket = clientSocket;
			try 
			{
				this.m_input = new BufferedReader(new InputStreamReader(this.m_clientSocket.getInputStream()));
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}

		public void run() 
		{
			while (!Thread.currentThread().isInterrupted()) 
			{
				try 
				{
					String read = m_input.readLine();
					m_communicationHandler.post(new handleDataThread(this.m_client, this.m_clientSocket, read.getBytes()));
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	/*
	 * Called for handling data received on socket.
	 */
	class handleDataThread implements Runnable 
	{
		private byte[] m_data;
		private Socket m_socket;
		private Client m_client;
		
		public handleDataThread(Client client, Socket socket, byte[] data) 
		{
			this.m_client = client;
			this.m_socket = socket;
			this.m_data = data;
		}

		public void run() 
		{
			String test = new String(m_data);
			m_status.setText(test);
			
			byte[] d = this.m_data;
			
			if (d.length > 0)
			{
				m_communicationHandler.post(new sendDataThread(this.m_socket, m_client.getOutputData()));
			}
			
		}
	}
	
	/*
	 * Called for sending data on socket.
	 */
	class sendDataThread implements Runnable 
	{
		private byte[] m_data;
		private Socket m_socket;
		
		public sendDataThread(Socket socket, byte[] data) 
		{
			this.m_socket = socket;
			this.m_data = data;
		}

		public void run() 
		{
			PrintWriter out;
			try 
			{
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(m_socket.getOutputStream())), true);
				out.println(new String(this.m_data));
				out.flush();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}	
}
