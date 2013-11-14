package edu.missouri.bas;



import com.equivital.sdk.connection.SemBluetoothConnection;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import edu.missouri.bas.activities.DeviceListActivity;
import edu.missouri.bas.bluetooth.BluetoothRunnable;
import edu.missouri.bas.service.SensorService;


public class SensorConnections extends Activity {
	static TextView tvSetWristStatus;
	static TextView tvSetChestStatus;
	Button 	 btnConnectWrist;
	Button   btnConnectChest;
	Button   btnCheckState;
	ToggleButton tbBluetooth;
	public static final int INTENT_CONNECT_WRIST = 0;
	public static final int INTENT_CONNECT_CHEST = 1;
	protected static final String TAG = "SensorConnections";	
	public static final int MESSAGE_STATE_CHANGE = 2;
	public static final int BLUETOOTH_STATE_CHANGE = 3;
	
	BluetoothAdapter btAdapter=BluetoothAdapter.getDefaultAdapter();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connections);
		tvSetWristStatus=(TextView)findViewById(R.id.tvSetWristStatus);	
		tvSetChestStatus=(TextView)findViewById(R.id.tvSetChestStatus);	
		int state=BluetoothRunnable.getState();
		switch(state)
		{
		case BluetoothRunnable.BluetoothState.CONNECTED:
        	tvSetWristStatus.setText("Connected");	
        	Log.d("BluetoothState from Handler","Connected" );
			break;
		case BluetoothRunnable.BluetoothState.CONNECTING:
			tvSetWristStatus.setText("Attempting to connect...");					
			break;
		case BluetoothRunnable.BluetoothState.FAILED:
			tvSetWristStatus.setText("Failed to connect");
			break;
		case BluetoothRunnable.BluetoothState.FINISHING:
			tvSetWristStatus.setText("Connection finishing...");
			break;
		case BluetoothRunnable.BluetoothState.LISTENING:
			tvSetWristStatus.setText("Listening for a connection...");
			break;
		case BluetoothRunnable.BluetoothState.NONE:
			tvSetWristStatus.setText("Not Connected ");
			break;
		case BluetoothRunnable.BluetoothState.STOPPED:
			tvSetWristStatus.setText("Bluetooth thread was stopped");
			break;
		default: 
		    tvSetWristStatus.setText("An error has occured");
			break;
		}
		
		 int chestSensorState=SemBluetoothConnection.getState();
		 switch (chestSensorState) {
         case SemBluetoothConnection.STATE_CONNECTED:
         	tvSetChestStatus.setText("Connected");	
         	Log.d("BluetoothState from Handler","Connected" );
				break;
			case SemBluetoothConnection.STATE_CONNECTING:
				tvSetChestStatus.setText("Attempting to Connect...");					
				break;
			
			case SemBluetoothConnection.STATE_LISTEN:
				tvSetChestStatus.setText("Listening for a Connection...");
				break;
			case SemBluetoothConnection.STATE_NONE:
				tvSetChestStatus.setText("Not Connected");
				break;
			case SemBluetoothConnection.STATE_RECONNECTING:
				tvSetChestStatus.setText("Reconnecting...");
				break;			
			default: 
				tvSetChestStatus.setText("An error has occured");
				break;
         
         }
		tbBluetooth = (ToggleButton) findViewById(R.id.tbBluetooth);
		btnConnectWrist=(Button)findViewById(R.id.btnConnectWrist);
		btnConnectChest=(Button)findViewById(R.id.btnConnectChest);
		btnCheckState=(Button)findViewById(R.id.btnCheckState);
		if(btAdapter.enable())
		{
			tbBluetooth.setChecked(true);
		}
		
		else
		{
			
			tbBluetooth.setChecked(false);
		}
		
		
		tbBluetooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		        if (isChecked) 
		        {
		            turnOnBt();
		        } 
		        else 
		        {
		        	btAdapter.disable();
			        
		        }
		    }
		});
		
		btnConnectWrist.setOnClickListener(new View.OnClickListener(){

			public void onClick(View v) {
				
				if(btAdapter.isEnabled())
				{			
				Intent serverIntent = new Intent(getApplicationContext(),DeviceListActivity.class);
	            startActivityForResult(serverIntent,SensorConnections.INTENT_CONNECT_WRIST);	            
	            }
				else
				{
				    
					Toast.makeText(getApplicationContext(),"Enable BT before connecting",Toast.LENGTH_LONG).show();
					
					
				}
				
				
			}
        });
		
		btnConnectChest.setOnClickListener(new View.OnClickListener(){

			public void onClick(View v) {
        		
				if(btAdapter.isEnabled())
				{			
				Intent serverIntent = new Intent(getApplicationContext(),DeviceListActivity.class);
	            startActivityForResult(serverIntent,SensorConnections.INTENT_CONNECT_CHEST);	            
	            }
				else
				{
				    
					Toast.makeText(getApplicationContext(),"Enable BT before connecting",Toast.LENGTH_LONG).show();
					
					
				}
				
			}
        });
		
		btnCheckState.setOnClickListener(new View.OnClickListener(){

			public void onClick(View v) {
				getState();
				
				
			}
        });
		
		/*IntentFilter intentFilter = new IntentFilter(BluetoothRunnable.ACTION_STATE_CHANGE);
        this.registerReceiver(br, intentFilter);*/
	}
	
	protected void getState(){
		Intent i = new Intent(SensorService.ACTION_GET_BLUETOOTH_STATE);
		this.sendBroadcast(i);
}
	
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("SensorServiceActivity", "onActivityResult " + resultCode);
        switch (requestCode) {
        
		case SensorConnections.INTENT_CONNECT_WRIST:
			// When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) 
            {
				String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				BluetoothDevice device = btAdapter.getRemoteDevice(address);		
				String name=device.getName();
				if(device.getName().contains("AffectivaQ"))
				{
				Intent connectIntent = new Intent(SensorService.ACTION_CONNECT_BLUETOOTH);
				connectIntent.putExtra(SensorService.INTENT_EXTRA_BT_DEVICE,address);
				connectIntent.putExtra(SensorService.INTENT_EXTRA_BT_MODE,
						BluetoothRunnable.BluetoothMode.CLIENT);
				connectIntent.putExtra(SensorService.INTENT_EXTRA_BT_TYPE,
						BluetoothRunnable.BluetoothSocketType.INSECURE);
				connectIntent.putExtra(SensorService.INTENT_EXTRA_BT_UUID,
						"00001101-0000-1000-8000-00805F9B34FB");				
				this.sendBroadcast(connectIntent);
				
				
				}
				else
				{
					
					Toast.makeText(getApplicationContext(),"Please select the devices with 'Affectiva' Prefix",Toast.LENGTH_LONG).show();
				
				}
			}
			else
			{
				Toast.makeText(getApplicationContext(), "No device is selected",Toast.LENGTH_LONG).show();				
				
			}
			
		   break;
			
		   
		case SensorConnections.INTENT_CONNECT_CHEST:
			if (resultCode == Activity.RESULT_OK) 
            {
				String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				BluetoothDevice device = btAdapter.getRemoteDevice(address);
				if(device.getName().contains("EQ"))
				{
					Toast.makeText(getApplicationContext(), "Entered the loop",Toast.LENGTH_LONG).show();
					Intent connectChest = new Intent(SensorService.ACTION_CONNECT_CHEST);	
					connectChest.putExtra(SensorService.KEY_ADDRESS,address);
					connectChest.putExtra("KEY_DEVICE_NAME",device.getName());
					this.sendBroadcast(connectChest);
				}
				else
				{					
					Toast.makeText(getApplicationContext(),"Please select the devices with 'EQ' Prefix",Toast.LENGTH_LONG).show();
				
				}
			}
			else
			{
				Toast.makeText(getApplicationContext(), "No device is selected",Toast.LENGTH_LONG).show();				
				
			}
			
		   break;
				
        }
    }

	public boolean turnOnBt() {
		// TODO Auto-generated method stub
		Intent Enable_Bluetooth=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(Enable_Bluetooth, 1234);
		return true;
	}
	

	 public static final Handler stateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
           if(msg.what==MESSAGE_STATE_CHANGE) 
        	   {
                switch (msg.arg1) {
                case BluetoothRunnable.BluetoothState.CONNECTED:
                	tvSetWristStatus.setText("Connected");	
                	Log.d("BluetoothState from Handler","Connected" );
					break;
				case BluetoothRunnable.BluetoothState.CONNECTING:
					tvSetWristStatus.setText("Attempting to Connect...");					
					break;
				case BluetoothRunnable.BluetoothState.FAILED:
					tvSetWristStatus.setText("Failed to Connect");
					break;
				case BluetoothRunnable.BluetoothState.FINISHING:
					tvSetWristStatus.setText("Connection Finishing...");
					break;
				case BluetoothRunnable.BluetoothState.LISTENING:
					tvSetWristStatus.setText("Listening for a Connection...");
					break;
				case BluetoothRunnable.BluetoothState.NONE:
					tvSetWristStatus.setText("Not Connected");
					break;
				case BluetoothRunnable.BluetoothState.STOPPED:
					tvSetWristStatus.setText("Bluetooth thread was stopped");
					break;
				default: 
				    tvSetWristStatus.setText("An error has occured");
					break;
                
                }
            }
        }
    };
	
    public static final Handler chestSensorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
           if(msg.what==BLUETOOTH_STATE_CHANGE) 
        	   {
                switch (msg.arg1) {
                case SemBluetoothConnection.STATE_CONNECTED:
                	tvSetChestStatus.setText("Connected");	
                	Log.d("BluetoothState from Handler","Connected" );
					break;
				case SemBluetoothConnection.STATE_CONNECTING:
					tvSetChestStatus.setText("Attempting to Connect...");					
					break;
				
				case SemBluetoothConnection.STATE_LISTEN:
					tvSetChestStatus.setText("Listening for a Connection...");
					break;
				case SemBluetoothConnection.STATE_NONE:
					tvSetChestStatus.setText("Not Connected");					
					break;
				case SemBluetoothConnection.STATE_RECONNECTING:
					tvSetChestStatus.setText("Reconnecting...");
					break;				
				default: 
					tvSetChestStatus.setText("An error has occured");
					break;
                
                }     
            }
        }
    };
    
    
	
	
	
}

