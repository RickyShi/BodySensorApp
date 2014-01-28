package edu.missouri.bas;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import edu.missouri.bas.activities.DeviceListActivity;
import edu.missouri.bas.bluetooth.BluetoothRunnable;
import edu.missouri.bas.service.SensorService;
import edu.missouri.bas.service.modules.location.ActivityRecognitionScan;

import edu.missouri.bas.survey.XMLSurveyMenu;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends ListActivity {

	private boolean mIsRunning=false;
	
	private BluetoothAdapter mAdapter= BluetoothAdapter.getDefaultAdapter();	
	private final static String TAG = "SensorServiceActivity";	
	public static final int REQUEST_ENABLE_BT = 3;
	public static final int INTENT_SELECT_DEVICES = 0;
	public static final int INTENT_DISCOVERY = 1;
	public static final int INTENT_VIEW_DEVICES = 2;	
	protected static final int START = 0;
	protected static final int STOP = 1;
	protected static final int SURVEY = 2;	
	protected static final int CONNECTIONS = 3;
	protected static final int SURVEY_STATUS = 4;
	public  final MainActivity thisActivity = this;
	//private final static String urlServer = "http://babbage.cs.missouri.edu/~rs79c/Android/upload.php";
	HttpURLConnection connection = null;
	DataOutputStream outputStream = null;
	DataInputStream inputStream = null;
	public  Context mainActivityContext=this;
	private static AlarmManager mAlarmManager;
	private static PendingIntent scheduleCheck;
	Runtime info;
	long freeSize; 
    long totalSize;
    long usedSize;
	Calendar cl=Calendar.getInstance();
	SimpleDateFormat curFormater = new SimpleDateFormat("MMMMM_dd"); 
	String dateObj =curFormater.format(cl.getTime());
	File path1 = new File(Environment.getExternalStorageDirectory(), "TestResults/chestsensor_"+dateObj+".txt");

	String chestsensorFilePath = path1.getAbsolutePath();
	
	
	
	String lineEnd = "\r\n";
	String twoHyphens = "--";
	String boundary =  "*****";
	

	String errMSG ="Please check your wifi or dataplan.\r\nThe phone is offline now.";

	int bytesRead, bytesAvailable, bufferSize;
	byte[] buffer;
	int maxBufferSize = 1*1024*1024;
	
	

	//action URL
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
       
        
    	String[] options = {"Start Service", "Stop Service", "Survey Menu",
		"External Sensor Connections","Schedule Survey Activity"};
    	
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
    			android.R.layout.simple_list_item_1, options);
    	
    	setListAdapter(adapter);    
        ListView listView = getListView();        
        listView.setOnItemClickListener(new OnItemClickListener() {

			
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
		    	switch(position){
	    		case START:
	    			startSService();
	    			break;
	    		case STOP:
	    			stopSService();
	    			//uploadFiles(urlServer,chestsensorFilePath);	    			
	    			//Toast.makeText(getApplicationContext(), "Successfully uploaded files.", Toast.LENGTH_LONG).show();
	    			break;
	    		case SURVEY: 
	    			startSurveyMenu();
	    			break;	    		
	    		case CONNECTIONS:
	    			startConnections();	    			
	    			break;
	    		case SURVEY_STATUS:
	    			startSurveyScheduler();	    			
	    			break;
		    	}
			}
        });
        
        if(mAdapter == null){
        	Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show();
        	finish();
        	return;
        }
        
        if (!mAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableIntent);
        }
        
        
        startSService();
                
        
    }
    
    
	
    private void uploadFiles(String urlServer,String Path) {
		// TODO Auto-generated method stub
    	
    	
    	try
    	{
    	FileInputStream fileInputStream = new FileInputStream(new File(Path) );
    	
    	URL url = new URL(urlServer);
    	connection = (HttpURLConnection) url.openConnection();
    	
    	// Allow Inputs & Outputs
    	connection.setDoInput(true);
    	connection.setDoOutput(true);
    	connection.setUseCaches(false);
    	
    	// Enable POST method
    	connection.setRequestMethod("POST");
    	
    	connection.setRequestProperty("Connection", "Keep-Alive");
    	connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
    	
    	outputStream = new DataOutputStream( connection.getOutputStream() );
    	outputStream.writeBytes(twoHyphens + boundary + lineEnd);
    	outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + Path +"\"" + lineEnd);
    	outputStream.writeBytes(lineEnd);
    	
    	bytesAvailable = fileInputStream.available();
    	bufferSize = Math.min(bytesAvailable, maxBufferSize);
    	buffer = new byte[bufferSize];
    	
    	// Read file
    	bytesRead = fileInputStream.read(buffer, 0, bufferSize);
    	
    	while (bytesRead > 0)
    	{
    	outputStream.write(buffer, 0, bufferSize);
    	bytesAvailable = fileInputStream.available();
    	bufferSize = Math.min(bytesAvailable, maxBufferSize);
    	bytesRead = fileInputStream.read(buffer, 0, bufferSize);
    	}
    	
    	outputStream.writeBytes(lineEnd);
    	outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
    	
    	// Responses from the server (code and message)
    	
    	int serverResponseCode = connection.getResponseCode();
    	String serverResponseMessage = connection.getResponseMessage();
    	
    	
    	fileInputStream.close();
    	outputStream.flush();
    	
    	InputStream is = connection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is, "utf-8");
        BufferedReader br = new BufferedReader(isr);
        String result = br.readLine();

        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
        
    	outputStream.close();
    	is.close();
    	}
    	catch (Exception e)
    	{
    	//Exception handling
    	e.printStackTrace();
    	setTitle(e.getMessage());    	
    	
    	}
    	   	
    	
	}
    private void startSurveyMenu(){
		Intent i = new Intent(getApplicationContext(), XMLSurveyMenu.class);
		startActivity(i);
    }
    
    private void startSurveyScheduler() {
		// TODO Auto-generated method stub
    	Intent i = new Intent(getApplicationContext(), SurveyStatus.class);
		startActivity(i);
	}
    private void startConnections(){
		Intent i = new Intent(getApplicationContext(), SensorConnections.class);
		startActivity(i);
    }
    
    private void stopSService() {
    	mIsRunning = false;     	
    	this.stopService(new Intent(MainActivity.this,SensorService.class));    	
    }
    private void startSService() {
        if (! mIsRunning) {
        	 mIsRunning = true;            
            Thread t = new Thread(){
        		public void run(){
        		getApplicationContext().startService(new Intent(MainActivity.this,SensorService.class));
        			}
        		 };
          t.start();
        }
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("SensorServiceActivity", "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session

            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, "Bluetooth could not be enabled, exiting",
                		Toast.LENGTH_LONG).show();
                finish();
            }
            break;
		case MainActivity.INTENT_SELECT_DEVICES:
			// When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) 
            {
				String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);				
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
				Toast.makeText(getApplicationContext(), "No device is selected",Toast.LENGTH_LONG).show();				
				
			}
			
		   break;
			
				
        }
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.bs_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getItemId() == R.id.Connect){
			if(mAdapter.isEnabled())
			{			
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, MainActivity.INTENT_SELECT_DEVICES);
            return true;
            }
			else
			{			    
				Toast.makeText(getApplicationContext(),"Enable BT before connecting",Toast.LENGTH_LONG).show();			
			}					
		}
		
		else if (item.getItemId() == R.id.Enable){
			if(mAdapter.isEnabled())
			{
				Toast.makeText(getApplicationContext(),"Bluetooth is already enabled ",Toast.LENGTH_LONG).show();
				
			}
			else
			{
				
				turnOnBt();				
			}
			
            return true;
		}
		
		else if (item.getItemId() == R.id.Disable){
			mAdapter.disable();
			Toast.makeText(getApplicationContext(),"Bluetooth is disabled",Toast.LENGTH_LONG).show();			
            return true;
		}
		return false;
	}
	
	public boolean turnOnBt() {
		// TODO Auto-generated method stub
		Intent Enable_Bluetooth=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(Enable_Bluetooth, 1234);
		return true;
	}
	
	

	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	
	
}

