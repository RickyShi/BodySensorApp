package com.equivital.sdk.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Vector;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;

import com.equivital.sdk.ISemConnection;
import com.equivital.sdk.ISemConnectionEvents;
import com.equivital.sdk.ISemConnectionManager;
import com.equivital.sdk.SemDataReceivedEventArgs;
import edu.missouri.bas.SensorConnections;
import edu.missouri.bas.service.SensorService;



/**
 * Implementation of the Equivital ISemConnection interface for the Android Bluetooth Stack 
 * @author Steve Riggall, Hidalgo Limited.
 */
public class SemBluetoothConnection implements ISemConnection
{
    // Unique UUID for this application
    private static final UUID SERIAL_PROFILE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static String equivitalAddress;

    // Member fields
    private final BluetoothAdapter mBluetoothAdapter;
    private static ConnectThread mConnectThread;
    private static ConnectedThread mConnectedThread;
    private static int mState;
    private String iConnectedBluetoothDeviceAddress = "";
    private String iConnectedBluetoothDeviceName = "";
    private Vector<ISemConnectionEvents> _eventHandlers = new Vector<ISemConnectionEvents>();
	private Vector<ISemBluetoothConnectionEvents> _eventHandlersBT = new Vector<ISemBluetoothConnectionEvents>();
	private String _portAddress = "";
	private SemDataReceivedEventArgs _args = new SemDataReceivedEventArgs();
	protected static Handler sensorHandler;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_RECONNECTING = 4;
    public boolean reconnectionStarted=false;
    public static int Count=0;
    public static int Count1=0;
    static Timer timer1;
    PendingIntent startReconnection;
    

	/**
	 * Adds an event listener
	 */
	public void addEventHandler(ISemConnectionEvents eventHandler)
	{
		if(!_eventHandlers.contains(eventHandler)) _eventHandlers.add(eventHandler);
	}

	/**
	 * Removes an event handler.
	 */
	public void removeEventHandler(ISemConnectionEvents eventHandler)
	{
		if(_eventHandlers.contains(eventHandler)) _eventHandlers.remove(eventHandler);
	}

	/**
	 * Adds an event listener for bluetooth events
	 */
	public void addBTEventHandler(ISemBluetoothConnectionEvents eventHandler)
	{
		if(!_eventHandlersBT.contains(eventHandler)) _eventHandlersBT.add(eventHandler);
	}

	/**
	 * Removes an event handler for bluetooth events.
	 */
	public void removeBTEventHandler(ISemBluetoothConnectionEvents eventHandler)
	{
		if(_eventHandlersBT.contains(eventHandler)) _eventHandlersBT.remove(eventHandler);
	}

	
	@SuppressWarnings("unchecked")
	Vector<ISemConnectionEvents> getHandlers()
	{
		return (Vector<ISemConnectionEvents>)_eventHandlers.clone();
	}
	
	@SuppressWarnings("unchecked")
	Vector<ISemBluetoothConnectionEvents> getHandlersBT()
	{
		return (Vector<ISemBluetoothConnectionEvents>)_eventHandlersBT.clone();
	}
	
	protected void fireDataReceivedEvent(SemDataReceivedEventArgs args)
	{
		if(_eventHandlers.size() > 0)
		{
			for(ISemConnectionEvents thisHandler : getHandlers())
			{
				thisHandler.dataReceived(this, args);
			}
		}
	}

	protected void fireConnectionClosedEvent()
	{
		if(_eventHandlers.size() > 0)
		{
			for(ISemConnectionEvents thisHandler : getHandlers())
			{
				thisHandler.connectionClosed(this);
			}
		}
	}
	
	protected void fireConnectionFailedEvent(IOException e)
	{
		if(_eventHandlersBT.size() > 0)
		{
			String message = (e == null) ? "Failed to connect" : e.getLocalizedMessage();
			for(ISemBluetoothConnectionEvents thisHandler : getHandlersBT())
			{
				thisHandler.connectionFailed(message);
			}
		}
	}
	

	protected void fireConnectionSucceededEvent()
	{
		if(_eventHandlersBT.size() > 0)
		{
			for(ISemBluetoothConnectionEvents thisHandler : getHandlersBT())
			{
				thisHandler.connectionSucceeded();
			}
		}
	}

	/**
	 * Factory method used to create a new bluetooth based ISemConnection interface based on the device MAC address.
	 * @param deviceAddress The MAC address of the device to connect to  in the form AA:BB:CC:DD:EE:FF
	 * @return An object that represents the specified bluetooth device.
	 */
	public static ISemConnection createConnection(String deviceAddress)
	{
		equivitalAddress=deviceAddress;			
		return new SemBluetoothConnection(deviceAddress);		
	}
	
	/**
	 * Factory method used to create a new bluetooth based ISemConnection interface with no particular device specified.
	 * @return An empty bluetooth connection.
	 */
	public static ISemConnection createConnection()
	{
		return new SemBluetoothConnection(null);
	}
	
	private SemBluetoothConnection(String address) 
	{ 
		
		/*SensorService.serviceContext.registerReceiver(reconnectionRequestReciever,reconnectionRequest);
		
		SensorService.serviceContext.registerReceiver(startReconnectionReciever,startRequest);*/
		
		_portAddress = address;

		// Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        
        if(address != null) connectToDeviceWithAddress(address);
        
	}	
	
	/**
	 * Connect to the specified device via MAC address. 
	 * @param address MAC address in the form AA:BB:CC:DD:EE:FF
	 */
	public void connect(String address)
	{
		if(mState == STATE_NONE || mState == STATE_RECONNECTING )
		{
			_portAddress = address;
			if(address != null) connectToDeviceWithAddress(address);
		}
	}
	
	private void pushData(byte[] buffer, int size)
	{
		_args.data = buffer;
		_args.count = size;
		_args.offset = 0;
		
		fireDataReceivedEvent(_args);
	}

	/**
	 * Gets the bluetooth manager.
	 * For Android Bluetooth connections there is no manager so this method always returns null.
	 * @return A connection manager instance (or null if the connection is unmanaged)
	 */
	
	public ISemConnectionManager getManager()
	{
		return null;
	}

	/**
	 * Gets the connection information as a string.
	 * @return The connection string.
	 */
	
	public String getConnectionInfo()
	{
		return "Bluetooth Port [" + _portAddress + "]";
	}

	/**
	 * Determines whether a bluetooth device is currently connected.
	 * @return True if a device is connected.
	 */
	
	public boolean isConnected()
	{
		return isSocketConnected();
	}
	
	/**
	 * Determines whether there is a connect attempt in progress.
	 * @return True if a bluetooth device is attempting to connect.
	 */
	public boolean isConnecting()
	{
		return isSocketConnecting();
	}
	
	/**
	 * Determines if the underlying connection is closed. 
	 * @return True if the connection has been closed.
	 */
	public boolean isClosed()
	{
		return !isSocketConnected() && !isSocketConnecting();
	}

	/**
	 * Writes the specified data to the underlying bluetooth device connection.
	 * @param dataToWrite The data to write to the connection as a byte array.
	 * @return True if successful.
	 */
	
	public boolean write(byte[] dataToWrite)
	{
		if(mState == STATE_NONE) return false;
		
		writeData(dataToWrite, dataToWrite.length);
		return true;
	}

	/**
	 * Writes the specified string to the underlying bluetooth device connection.
	 * @param dataToWrite THe string information.
	 * @return True if successful. 
	 */
	
	public boolean write(String dataToWrite)
	{
		return write(dataToWrite.getBytes());
	}

	/**
	 * Closes the underlying bluetooth connection.
	 */
	
	public void close()
	{
		if(mState != STATE_NONE) disconnect();
	}
	
    
    /**
     * Start the service. */
    private synchronized void start()
    {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        
        setState(STATE_LISTEN);
    }

    private synchronized void connectToDeviceWithAddress(String aAddress)
    {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())
        {
        	if (aAddress != null && aAddress.length() > 0)
        	{
        		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(aAddress);

        		// Cancel any thread attempting to make a connection
        		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        		// Cancel any thread currently running a connection
        		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        		setState(STATE_NONE); 
        		//reconnectDevice();
        		// Start the thread to connect with the given device
        		mConnectThread = new ConnectThread(device);
        		if(mConnectThread.isDeviceValid())
        		{
        			mConnectThread.start();
        			setState(STATE_CONNECTING);
        		}
        	}
        }
    }
	    
    /**
     * Returns the current device MAC address.
     * @return MAC address in the form AA:BB:CC:DD:EE:FF
     */
    public String getConnectedBluetoothDeviceAddress()
    {
    	return iConnectedBluetoothDeviceAddress;
    }

    /**
     * Get the name of the connected device.
     * @return The device name.
     */
    public String getConnectedBluetoothDeviceName()
    {
    	return iConnectedBluetoothDeviceName;
    }
	    
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device)
    {
    	iConnectedBluetoothDeviceAddress = device.getAddress();
    	iConnectedBluetoothDeviceName = device.getName();
    	
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        if(mConnectedThread.isValidDevice())
        {
        	mConnectedThread.setPriority(Thread.NORM_PRIORITY);
        	mConnectedThread.start();
        	setState(STATE_CONNECTED);
        }
    }

    
    /**
     * Stop all threads
     */
    public synchronized static void disconnect()
    {
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        setState(STATE_NONE);
        //reconnectDevice();
		//fireConnectionClosedEvent();
    }
    
    private synchronized void writeData(byte[] bufferToWrite, int sizeOfBuffer)
    {
    	if(mConnectedThread != null)
    	{
    		mConnectedThread.writeData(bufferToWrite, sizeOfBuffer);
    	}
    }

    private boolean isSocketConnected()
    {
    	return (getState() == STATE_CONNECTED);
    }
    
    private boolean isSocketConnecting()
    {
    	return (getState() == STATE_CONNECTING);
    }
    
   
    private synchronized static void setState(int state)
    {
        mState = state; 
        /*if(mState==0 && equivitalAddress!=null)
        {        	
        	  reconnectDevice();        	
        }*/
        sensorHandler=SensorConnections.chestSensorHandler;
        sensorHandler.obtainMessage(SensorConnections.BLUETOOTH_STATE_CHANGE,state,-1).sendToTarget();
    }
	  	
   
	
	
    public static synchronized int getState()
    {
        return mState;
    }

    private void connectionFailed(IOException e)
    {   
    	Log.d("SEM","Connection Failed");
    	if(reconnectionStarted!=true)
    	{
    		reconnectDevice();
    		reconnectionStarted=true;
        }
    }

    private void connectionSucceeded()
    {
        fireConnectionSucceededEvent();
    }
    
    private void connectionLost(IOException e)
    {     	
    	Log.d("SEM","Connection Lost");
    	if(reconnectionStarted!=true)
    	{
    		reconnectDevice();
    		reconnectionStarted=true;
        }
    }

    

    public void reconnectDevice()
    {    	    	    		    		
		timer1=new Timer();
		if (!SensorService.cancelBlueToothFlag){
			Reconnect mReconnectDevice=new Reconnect();
			timer1.scheduleAtFixedRate(mReconnectDevice,1000,3000);   
			setState(STATE_RECONNECTING);
		}
    }
					
		
    public class Reconnect extends TimerTask{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d("SEM","Reconnection Started");
			if(isConnected()!=true && Count<=10 && !SensorService.cancelBlueToothFlag)
			{
			    connect(equivitalAddress);
			    Count++;
			}
			else
			{
				timer1.cancel();
				Count=0;
				reconnectionStarted=false;												
				if(isConnected()!=true)
				{
					fireConnectionClosedEvent();					
			        setState(STATE_NONE);	
			        if (!SensorService.cancelBlueToothFlag){
			        	Intent i=new Intent(SensorService.ACTION_START_SOUND);
			        	SensorService.serviceContext.sendBroadcast(i);
			        }
				}
			}
			
		} 
    }
    
    
   
	/**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        public Boolean isDeviceValid()
        {
        	return mmSocket != null;
        }
        
        public ConnectThread(BluetoothDevice device)
        {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try
            {
                tmp = device.createRfcommSocketToServiceRecord(SERIAL_PROFILE_UUID);
            } 
            catch (IOException e)
            {
            	tmp = null;
            	connectionFailed(e);
            }
            mmSocket = tmp;
        }

        public void run()
        {
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try 
            {
                // This is a blocking call and will only return on a successful connection or an exception
                mmSocket.connect();
            }
            catch (IOException e)
            {
                connectionFailed(e);
                // Close the socket
                try
                {
                    mmSocket.close();
                }
                catch (IOException e2)
                {
                }
                // Start the service over to restart listening mode
                SemBluetoothConnection.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (SemBluetoothConnection.this)
            {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel()
        {
            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
            }
        }
    }

   
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        
        public boolean isValidDevice()
        {
        	return mmInStream != null && mmOutStream != null;
        }
        
        public ConnectedThread(BluetoothSocket socket)
        {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e)
            {
            	if(tmpIn != null)
            	{
	            	tmpIn = null;
            	}
                connectionFailed(e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        volatile boolean sentinel = false;
       
        public void run()
        {
        	if(!isValidDevice()) return;
        	
            connectionSucceeded();
            
            final int maxBufSize = 2048;
            byte[] data = new byte[maxBufSize];

            // Keep listening to the InputStream while connected
            while (!sentinel)
            {
                try
                {
                    // Read from the InputStream
                	int length = mmInStream.read(data, 0, maxBufSize);
                    if(length > 0)
                    {
                    	pushData(data, length);
                    }
	                
                    try
                    {
                    	if(length < maxBufSize) Thread.sleep(50);
                    }
                    catch(Exception e)
                    {
                        // Interrupted exception
                    }
                }
                catch (IOException e)
                {
                    connectionLost(e);
                    break;
                }
            }
        }

        public void writeData(byte[] bufferToWrite, int bufferSize) 
        {
        	if(mmOutStream != null)
    		{
    			try
				{
					mmOutStream.write(bufferToWrite, 0, bufferSize);
				} catch (IOException e)
				{
				}
    		}
        }
        
        public void cancel()
        {
        	sentinel = true;
        	
            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
            }
        }
    }


	   
}
