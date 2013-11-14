package edu.missouri.bas.bluetooth.affectiva;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;
import edu.missouri.bas.bluetooth.BluetoothRunnable;
import edu.missouri.bas.service.SensorService;


public class AffectivaRunnable extends BluetoothRunnable {

	private static final String TAG = "AffectivaRunnable";
	private final static String FILE_HEADER="date,SeqNum,AccelZ,AccelY,AccelX,Battery,Temp,EDA";
	private final String BASE_PATH = "sdcard/TestResults/";
	
	public static final String INTENT_ACTION_AFFECTIVA_DATA = "INTENT_AFFECTIVA_DATA";
	
	protected final int MAX_NEG = 6;
	protected final int PACKET_SIZE = 34;
	
	protected final int MAX_DECIMALS = 6;
	
	public AffectivaRunnable(Handler handler, BluetoothDevice device,
			UUID uuid, int mode, int type, File outputFile) {
		super(handler, device, uuid, mode, type, outputFile);
		try {
			if( outputFile != null) bufferedWriter.write(FILE_HEADER);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void connectedFunction() {
		int decimalCount = 0;
		int remainingCharacters = 3;
		byte[] rawPacket = new byte[100];
		int read = 0;
		while(mState == BluetoothState.CONNECTED && remainingCharacters > 0){
			//Read one byte
			try {
				read += mBluetoothSocket.getInputStream().read(rawPacket, read, 1);
			} catch (IOException e) {
				e.printStackTrace();
				lostConnection();
				break;
			}
			String temp = "";
			try {
				temp = new String(rawPacket, read-1, 1, "ISO-8859-1");
				if(temp.equals(".")){
					if(decimalCount <= MAX_DECIMALS)
						decimalCount++;
					else remainingCharacters = 0;
						
				}
				else{
					if(decimalCount == MAX_DECIMALS)
						remainingCharacters--;
				}
			} catch (UnsupportedEncodingException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch(StringIndexOutOfBoundsException e1){
				e1.printStackTrace();
			}
		}
		if(mState == BluetoothState.CONNECTED){
			AffectivaPacket p = null;
			try {
				p = AffectivaPacket.packetFromString(
						new String(rawPacket,0,read,"ISO-8859-1"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			//If p is not null the packet is a valid affectiva packet
			if(p != null){
				//Log.d(TAG,"Read: "+p.toString());
				if(mHandler != null)
					mHandler.obtainMessage(SensorService.MESSAGE_BLUETOOTH_DATA, p).sendToTarget();
			}
			else{
				Log.d(TAG,"Read: invalid packet");
			}
		}
	}
}
