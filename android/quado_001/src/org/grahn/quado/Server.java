package org.grahn.quado;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Handler;
import android.widget.TextView;

public class Server
{
	private ServerSocket serverSocket;
	private Thread serverThread = null;
	private TextView serverStatus;
	Handler updateConversationHandler;
	
	private int m_serverPort;
	
	private byte[] m_inputData;
	private byte[] m_outputData;

	public Server(TextView status)
	{
		m_serverPort = 1414;
		m_inputData = new byte[8];
		m_outputData = new byte[8];
		
		m_outputData[0] = 'A';
		m_outputData[1] = 'B';
		m_outputData[2] = 'C';
		m_outputData[3] = 'D';
		
		this.serverThread = new Thread(new ServerThread(this));
        this.serverThread.start();
        serverStatus = status;
        
        
        updateConversationHandler = new Handler();
	}
	
	public void destroy()
	{
		try 
		{
			serverSocket.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	public void setInputData(byte[] inputData)
	{
		m_inputData = inputData;
	}
	
	public byte[] getInputData()
	{
		return m_inputData;
	}
	
	public void setOutputData(byte[] outputData)
	{
		//m_outputData = outputData;
	}
	
	public byte[] getOutputData()
	{
		return m_outputData;
	}
	
	
	class ServerThread implements Runnable
	{
		private Server m_server;
		
		public ServerThread(Server server)
		{
			m_server = server;
		}
		
		public void run() 
		{
			Socket socket = null;
			try 
			{
				serverSocket = new ServerSocket(m_serverPort);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}

			while (!Thread.currentThread().isInterrupted()) 
			{
				try 
				{
					socket = serverSocket.accept();
					
					// Set no TCP delay.
					socket.setTcpNoDelay(true);
					
					CommunicationThread commThread = new CommunicationThread(m_server, socket);
					new Thread(commThread).start();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	class CommunicationThread implements Runnable 
	{
		private Server m_server;
		private Socket clientSocket;
		private BufferedReader input;
		
		public CommunicationThread(Server server, Socket clientSocket) 
		{
			this.m_server = server;
			this.clientSocket = clientSocket;
			try 
			{
				this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
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
					updateConversationHandler.post(new sendDataThread(m_server, clientSocket, m_server.getOutputData()));
					updateConversationHandler.sendEmptyMessage(1414);
					
					// Wait until empty message has been removed by sendDataThread.
					while (updateConversationHandler.hasMessages(1414))
					{
					}

					String read = input.readLine();
					if (read != null)
					{
						byte[] data = read.getBytes();
						updateConversationHandler.post(new handleDataThread(m_server, clientSocket, data));
					}
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	class handleDataThread implements Runnable 
	{
		private Server m_server;
		private byte[] data;
		private Socket socket;
		
		public handleDataThread(Server server, Socket socket, byte[] data) 
		{
			this.m_server = server;
			this.socket = socket;
			this.data = data;
		}

		public void run() 
		{
			if (data.length > 0)
			{
				m_server.setInputData(data);
				float a = (data[0] / 127.0f * 2.0f - 1.0f);
				float b = (data[1] / 127.0f * 2.0f - 1.0f);
				float c = (data[2] / 127.0f * 2.0f - 1.0f);
				float d = (data[3] / 127.0f * 2.0f - 1.0f);
				//serverStatus.setText("Client Says: "+ a + " " + b + " " + c + " " + d);
			}
		}
	}
	
	class sendDataThread implements Runnable 
	{
		private Server m_server;
		private byte[] data;
		private Socket socket;
		
		public sendDataThread(Server server, Socket socket, byte[] data) 
		{
			this.m_server = server;
			this.data = data;
			this.socket = socket;
		}

		public void run() 
		{
			PrintWriter out;
			try 
			{
				Thread.sleep(100);
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
				out.println(new String(this.data));
				out.flush();
				
				// Remove message to tell handler we can start listening on socket.
				updateConversationHandler.removeMessages(1414);
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			} catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}
	}	
}
