package edu.missouri.bas.bluetooth;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.UUID;
import edu.missouri.bas.SensorConnections;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;



public abstract  class BluetoothRunnable implements Runnable{

	protected final boolean D = true;
	private static final String TAG = "BluetoothRunnable";
	public static final String KEY_OLD_STATE = "CURRENT_STATE";
	public static final String KEY_NEW_STATE = "NEW_STATE";
	public static final String KEY_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	public static final String KEY_DEVICE_NAME = "DEVICE_NAME";
	
	protected final UUID mUUID;
	protected final String SERVICE_NAME = "BluetoothSensorService";
	
	protected final BluetoothAdapter mBluetoothAdapter;
	
	protected Handler mHandler;
	
	protected static int mState;
	protected int mMode;
	protected final int mSocketType;

	protected BluetoothSocket mBluetoothSocket;
	protected BluetoothDevice mConnectedDevice;
	
	protected boolean retry = true;
	protected volatile boolean recording = false;
	protected volatile boolean done = false;
	protected volatile boolean deliverPackets = false;
	
	protected FileWriter fileWriter;
	protected BufferedWriter bufferedWriter;
	protected File outputFile;
	public static final String ACTION_STATE_CHANGE = "ACTION_BLUETOOTH_STATE";	
	public Calendar cal;
	public Handler bluetoothHandler;
	
	 
	
	public BluetoothRunnable(Handler handler, BluetoothDevice device,
			UUID uuid, int mode, int type,
			File outputFile){
		this.mHandler = handler;
		this.mConnectedDevice = device;
		this.mUUID = uuid;
		this.mMode = mode;
		this.mState = BluetoothState.NONE;
		this.mSocketType = type;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(outputFile != null){
			this.outputFile = outputFile;
				try {
					fileWriter = new FileWriter(this.outputFile);
				} catch (IOException e) {
					Log.e(TAG,"Couldn't open output file");
					e.printStackTrace();
					return;
				}
			if(fileWriter != null)
				bufferedWriter = new BufferedWriter(fileWriter);
		}	
		cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		SensorConnections sc=new SensorConnections();
		bluetoothHandler=sc.stateHandler;
		
	}
	
	
	
	
	public void run() {
		retry = true;
		while(retry){
			
			/*
			 * retryCheck() returns a boolean that will be used
			 * to determine if the thread should retry.
			 * 
			 * Subclasses can override it to return true if
			 * the thread should automatically retry in case 
			 * of failure.  
			 * 
			 * retryCheck may need to sleep or something in between attempts.
			 */
			retry = retryCheck();
			
			
			
			/*
			 * Attempt to setup a server socket and listen for
			 * a connection.  Will throw IOExceptions if there
			 * is a problem, and return null if it fails to 
			 * connect.
			 */
			if(mMode == BluetoothMode.SERVER){
				try {
					mBluetoothSocket = listenForConnection();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			/*
			 * Connect to another device's server socket.
			 * Will throw an IOException if there is a problem
			 * or return null if it fails to connect.
			 */
			else if(mMode == BluetoothMode.CLIENT){
				try {
					if(D)Log.d(TAG,"Trying to connect as client");
					mBluetoothSocket = connectRemoteDevice();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			/*
			 * State should now be set to connected.  While the
			 * state remains connected and the done flag is not
			 * set it will repeatedly call the (abstract) 
			 * connectedFunction() method.  Subclasses will override
			 * connectedFuntion() with whatever functionality they
			 * need to perform once the connections have been
			 * established.
			 * 
			 * If the thread somehow reaches this point when it
			 * has failed to connect, it will skip this and move
			 * on to cleaning up I/O streams and sockets.
			 */
			while(/*!done &&*/ mState == BluetoothState.CONNECTED){
				connectedFunction();
				/*try {
					mBluetoothSocket.getInputStream();
				} catch (IOException e) {
					e.printStackTrace();
					lostConnection();
					stop();
				}*/
			}
			
			/*
			 * The thread has now finished it main execution block 
			 * and needs to clean up after itself.  The state is
			 * set to finishing to indicate this.
			 * 
			 * If the socket failed to connect the state will
			 * remain FAILED.
			 */
			if(mState == BluetoothState.CONNECTED || mState == BluetoothState.STOPPED){
				setState(BluetoothState.FINISHING);
			}
			
			/* 
			 * The I/O streams are closed first to prevent any
			 * file corruption due to unclosed streams.
			 */
			try {
				closeStreams();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			/*
			 * The sockets are then closed. 
			 * 
			 * In Android's Bluetooth implementation, closing a
			 * connected socket's Input/Output stream will also 
			 * result in the socket being closed, so I/O exceptions
			 * may be thrown depending on the order things are 
			 * closed in.  BluetoothSockets may also throw I/O
			 * Exceptions when they are closed.
			 */
			if(mState == BluetoothState.FINISHING){
				try {
					closeSockets();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			/*
			 * Everything is finished, we'll set the state to none
			 * just so it shows up in the log if necessary.
			 */
			setState(BluetoothState.NONE);
		}
		if(mHandler != null) mHandler.obtainMessage(MessageCode.THREAD_FINISHED).sendToTarget();
		
	}
	
	protected void setState(int newState){
		if(D) Log.d(TAG,"Changed from "+mState+" to "+newState);
		Bundle states = getStateBundle();
		states.putInt(KEY_NEW_STATE, newState);
		if(mHandler != null) mHandler.obtainMessage(MessageCode.STATE_CHANGED, states);
		mState = newState;		
		bluetoothHandler.obtainMessage(SensorConnections.MESSAGE_STATE_CHANGE,newState,-1).sendToTarget();
        
		
		
		
		
	}
	
	public Bundle getStateBundle() {
		Bundle states = new Bundle();
		states.putInt(KEY_OLD_STATE, mState);
		states.putString(BluetoothRunnable.KEY_DEVICE_ADDRESS, mConnectedDevice.getAddress());
		states.putString(BluetoothRunnable.KEY_DEVICE_NAME, mConnectedDevice.getName());
		return states;
	}
	
	protected BluetoothSocket listenForConnection() throws IOException{
		BluetoothSocket tempSocket;
		
		/*
		 * Prepare server socket using UUID and SDP service name
		 */
		BluetoothServerSocket serverSocket = 
				mBluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, mUUID);
		
		/*
		 * Socket is ready to listen for connections,
		 * set state before calling .accept() because
		 * it will block until a device connects or 
		 * it times out.
		 */
		setState(BluetoothState.LISTENING);
		
		//Listen for connections
		tempSocket = serverSocket.accept();
		
		/*
		 * If temp socket is null, the ServerSocket
		 * didn't receive a connection and timed out
		 * or it failed to connect for some reason.
		 * 
		 * Otherwise, it successfully connected and 
		 * we set the state accordingly and return
		 * the connected socket.
		 */
		if(tempSocket != null){
			connectSuccessful(tempSocket.getRemoteDevice());
			return tempSocket;
		}
		else{
			listenFailed();
			return null;
		}
	}

	protected BluetoothSocket connectRemoteDevice() throws IOException{
		
		BluetoothSocket tempSocket = null;
		
		/*
		 * Prepare the BluetoothSocket that will be used
		 * to connect to the remote device.
		 * 
		 * Depending on the socket security type specified
		 * at initialization, we'll use different methods 
		 * to create the socket.
		 * 
		 * The else case should never happen, but it's
		 * better to include it in case additional
		 * functionality is added later. 
		 * 
		 * See Android documentation for more information
		 * about socket types.
		 */
		if(mSocketType == BluetoothSocketType.INSECURE){
			if(D) Log.d(TAG,"Using insecure socket");
			tempSocket = mConnectedDevice.createInsecureRfcommSocketToServiceRecord(mUUID);
		}
		else if(mSocketType == BluetoothSocketType.SECURE){
			tempSocket = mConnectedDevice.createRfcommSocketToServiceRecord(mUUID);
		}
		else	return tempSocket;
		
		/*
		 * Set to state to connecting
		 */
		setState(BluetoothState.CONNECTING);
		
		/*
		 * Attempt to connect to the device
		 */
		try {
			tempSocket.connect();
			
		} catch (IOException e) {
			//Connect failed, try to clean up the socket
			connectFailed();
			try {
				tempSocket.close();
			} catch (IOException e1) {
				//Also failed to close, this probably isn't a problem
				e1.printStackTrace();
			}
			
			//Also set the state to failed for logging/debugging
			e.printStackTrace();
			
		}
		
		/*
		 * Successfully connected.  Set the state and
		 * then return the socket.
		 */
		if (mState != BluetoothState.FAILED) connectSuccessful(tempSocket.getRemoteDevice());
		
		/*
		 * Return the socket.  It may be null but the
		 * state variable will reflect the state of
		 * the socket.
		 */
		return tempSocket;
	}
	
	protected boolean closeSockets() throws IOException{
		mBluetoothSocket.close();
		if(mHandler != null) mHandler.obtainMessage(
				MessageCode.SOCKETS_CLOSED).sendToTarget();
		return false;
	}
	
	protected boolean closeStreams() throws IOException{
		if(outputFile != null){
			bufferedWriter.close();
		}
		if(mHandler != null) mHandler.obtainMessage(
				MessageCode.STREAMS_CLOSED).sendToTarget();
		return false;
	}
	
	protected void listenFailed() {
		setState(BluetoothState.FAILED);
		if(mHandler != null) mHandler.obtainMessage(
				MessageCode.LISTEN_FAILED).sendToTarget();
		Log.e(TAG,"Listen failed");
	}
	
	protected void connectFailed() {
		setState(BluetoothState.FAILED);
		if(mHandler != null) mHandler.obtainMessage(
				MessageCode.CONNECT_FAILED).sendToTarget();
		Log.e(TAG,"Connect failed");
	}
	
	protected void connectSuccessful(BluetoothDevice d){
		setState(BluetoothState.CONNECTED);
		if(mHandler != null) mHandler.obtainMessage(
				MessageCode.CONNECTED,
				d).sendToTarget();
		if(D) Log.d(TAG,"Connect successful");
	}

	protected void lostConnection(){
		setState(BluetoothState.FAILED);
		if(mHandler != null) mHandler.obtainMessage(
				MessageCode.DISCONNECTED, 
				mBluetoothSocket.getRemoteDevice()).sendToTarget();
		if(D) Log.d(TAG,"Lost connection to: "+mBluetoothSocket.getRemoteDevice());
	}
	
	public void stop(){
		recording = false;
		done = true;
		if(mState == BluetoothState.CONNECTED)
			setState(BluetoothState.STOPPED);
		if(mHandler != null) 
			mHandler.obtainMessage(MessageCode.REMOTE_STOPPED).sendToTarget();
	}
	
	public void startRecording(){
		recording = true;
	}
	public void stopRecording(){
		recording = false;
	}
	/*
	 * Used to check if the thread should attempt to
	 * connect more than once.  More advanced
	 * implementations may need to check the state to
	 * decide if the thread should retry.  Thread.sleep()
	 * may also need to be called (but probably not).
	 */
	public boolean retryCheck(){
		return false;
	}
	
	/*
	 * Abstract method used for the main functionality
	 * of the connection (i.e. receiving or sending data).
	 * 
	 * mBluetoothSocket should have a working connection 
	 * if this is called.  
	 */
	protected abstract void connectedFunction();
	
	
	/*
	 * Enumerated types for the Bluetooth thread
	 */
	
	/*
	 * State is used to track the current state of the thread.
	 * A normal thread lifecycle shoud look like
	 * (Server Socket)
	 * None -> Listening -> Connected -> Finishing/Stopped -> None
	 * or
	 * (Client Socket)
	 * None -> Connecting -> Connected -> Finishing/Stopped -> None
	 */
	/*public enum BluetoothState{
		NONE, LISTENING, CONNECTING, CONNECTED,
		FINISHING, FAILED, STOPPED;
	}*/
	public class BluetoothState{
		public static final int NONE = 0;
		public static final int LISTENING = 1;
		public static final int CONNECTING = 2;
		public static final int CONNECTED = 3;
		public static final int FINISHING = 4;
		public static final int FAILED = 5;
		public static final int STOPPED = 6;
		private BluetoothState(){}
	}
	/*
	 * Indicates what socket type the thread should be using
	 */
	/*public enum BluetoothMode{
		SERVER, CLIENT;
	}*/
	
	public class BluetoothMode{
		public static final int SERVER = 0;
		public static final int CLIENT = 1;
		private BluetoothMode(){}
	}
	/*
	 * Depending on the remote device, secure or insecure
	 * sockets may be necessary
	 * 
	 * Secure may force the devices to exchange a secure
	 * PIN or to manually unpair if there is a problem.
	 */
	/*public enum BluetoothSocketType{
		SECURE, INSECURE;
	}*/
	
	public class BluetoothSocketType{
		
		public static final int SECURE = 0;
		public static final int INSECURE = 1;
		
		private BluetoothSocketType(){}
	}
	
	/*
	 * Used for messages that are sent back to the UI thread
	 * via Handler.  MessageCode.whatever.getCode() will
	 * return the int value.  
	 */
	public class MessageCode{
		public static final int PACKET_RECEIVED = 0;
		public static final int CONNECT_FAILED = 1;
		public static final int LISTEN_FAILED = 2;
		public static final int STATE_FINISHING = 3;
		public static final int STREAMS_CLOSED = 4;
		public static final int SOCKETS_CLOSED = 6;
		public static final int REMOTE_STOPPED = 7;
		public static final int CONNECTED = 8;
		public static final int THREAD_FINISHED = 9;
		public static final int DISCONNECTED = 10;
		public static final int STATE_CHANGED = 11;
		
		private MessageCode(){ }
	}

	public static int getState() {
		return mState;
	}
	
	public void setHandler(Handler handler){
		this.mHandler = handler;
	}

	public void deliverPackets(boolean b) {
		deliverPackets = b;
	}
}