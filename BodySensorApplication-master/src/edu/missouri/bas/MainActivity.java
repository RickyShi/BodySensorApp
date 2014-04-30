package edu.missouri.bas;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.missouri.bas.activities.AdminManageActivity;
import edu.missouri.bas.activities.DeviceListActivity;
import edu.missouri.bas.bluetooth.BluetoothRunnable;
import edu.missouri.bas.service.SensorService;
import edu.missouri.bas.survey.XMLSurveyMenu;

public class MainActivity extends ListActivity {

	private String TAG2 = "TAG~~~~~~~~~~~~~~~~~~~";
	
	
	private BluetoothAdapter mAdapter= BluetoothAdapter.getDefaultAdapter();	
	private final static String TAG = "SensorServiceActivity";	
	public static final int INTENT_REQUEST_MAMAGE = 4;
	public static final int REQUEST_ENABLE_BT = 3;
	public static final int INTENT_SELECT_DEVICES = 0;
	public static final int INTENT_DISCOVERY = 1;
	public static final int INTENT_VIEW_DEVICES = 2;	
	protected static final int START = 0;
	protected static final int STOP = 1;
	protected static final int SURVEY = 2;	
	protected static final int CONNECTIONS = 3;
	protected static final int BED_STATUS = 4;
	protected static final int SUSPENSION = 5;
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
	
	private TextView mText;
	private EditText mEdit;
	
	EditText userPin;
	InputMethodManager imm;
	SharedPreferences shp;
	Editor editor;
	String ID;
	String PWD;
	public static PublicKey pubk = null;

	//action URL
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
       
        Log.d(TAG2, "OnCreate~~~~~~~~~~~~~~~~~~~");
        try {
			pubk = getPublicKey();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	String[] options = {"Start Service", "Stop Service", "Survey Menu",
		"External Sensor Connections","Bed Report","Suspension"};
    	
    	ArrayList<String> buttons = new ArrayList<String>();
    	buttons.addAll(Arrays.asList(options));
    	SensorService.adapter = new ArrayAdapter<String>(this,
    			android.R.layout.simple_list_item_1, buttons);
    	
    	setListAdapter(SensorService.adapter);    
        ListView listView = getListView();        
        listView.setOnItemClickListener(new OnItemClickListener() {

			
			@SuppressWarnings("deprecation")
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
		    	switch(position){
	    		case START:
	    			if (SensorService.suspendFlag) {
		    			SuspensionHint();
	    			} else
		    			startSService();
	    			break;
	    		case STOP:
	    			if (SensorService.suspendFlag)
	    				SuspensionHint();
	    			else {
	    				stopSService();
	    				finish();
	    			}
	    			//uploadFiles(urlServer,chestsensorFilePath);	    			
	    			//Toast.makeText(getApplicationContext(), "Successfully uploaded files.", Toast.LENGTH_LONG).show();
	    			break;
	    		case SURVEY:
	    			if (SensorService.suspendFlag)
	    				SuspensionHint();
	    			else
	    				startSurveyMenu();
	    			break;	    		
	    		case CONNECTIONS:
	    			if (SensorService.suspendFlag)
	    				SuspensionHint();
	    			else
	    				startConnections();	    			
	    			break;
	    		case BED_STATUS:
	    			if (SensorService.suspendFlag)
	    				SuspensionHint();
	    			else {
		    			Calendar cTime = Calendar.getInstance();
		    			int cHour = cTime.get(Calendar.HOUR_OF_DAY);
		    			if (cHour>=21 || cHour<3){
		    			//if (cHour!=0){
		    				createPinAlertDialog();
		    			} else {
		    				bedTimeCheckDialog();
		    			}
	    			}
	    			break;
	    		case SUSPENSION:
	    			if (!SensorService.suspendFlag) {
	    				SuspensionCheck();
	    			} else {
	    				breakSuspensionCheck();
	    			}
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
        
        
        ////startSService();
        // startServer? into check       
        //
        //check if device is assigned with an ID
        shp = getSharedPreferences("PINLOGIN", Context.MODE_PRIVATE);
        ID = shp.getString(AdminManageActivity.ASID, "");
        PWD = shp.getString(AdminManageActivity.ASPWD, "");
        editor = shp.edit();
        
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        
        Log.d(TAG2,"id is "+ID);
        
        if(ID.equals("")){
        	Intent serverIntent = new Intent(this, AdminManageActivity.class);
            startActivityForResult(serverIntent, MainActivity.INTENT_REQUEST_MAMAGE);
            
            
            imm.toggleSoftInput(0, InputMethodManager.RESULT_HIDDEN);
        	
        }else if(PWD.equals("")){
        	//set password
        	
        	UserSetPinDialog(this, ID).show();
        	
        }else{
        	Log.d(TAG2,"pwd is "+shp.getString(AdminManageActivity.ASPWD, "get fail?"));
        	startSService();
        }
        
    }
    
    private Dialog UserSetPinDialog(Context context, final String ID) {  
        LayoutInflater inflater = LayoutInflater.from(this);  
        final View textEntryView = inflater.inflate(  
                R.layout.pin_login, null);  
        TextView alert_text = (TextView) textEntryView.findViewById(R.id.pin_text);
        alert_text.setText("Please input 4-digit PIN for User: "+ID);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);  
        builder.setCancelable(false);
        builder.setTitle("Set User PIN");  
        //builder.setMessage("Please input 4-digit PIN for User: "+ID);
        builder.setView(textEntryView);  
        builder.setPositiveButton("OK",  
                new DialogInterface.OnClickListener() {  
                    public void onClick(DialogInterface dialog, int whichButton) {  
                    	
                    	//check networking
                    	
                    	userPin = (EditText) textEntryView.findViewById(R.id.pin_edit);
                    	String newPin = userPin.getText().toString();
                    	Log.d(TAG2, "get from edittext is "+newPin);
                    	
                    	HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Crt/validateUser.php");
         		        
         		        List<NameValuePair> params = new ArrayList<NameValuePair>();
         		        
         		        //file_name 
         		        params.add(new BasicNameValuePair("userID",ID));        
         		        //function
         		        params.add(new BasicNameValuePair("pre","3"));
         		        //data                       
         		        params.add(new BasicNameValuePair("password",newPin));

         		        	        	
         		        try {
         					request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
         				
         		        
         		        HttpResponse response = new DefaultHttpClient().execute(request);
         		        if(response.getStatusLine().getStatusCode() == 200){
         		            String result = EntityUtils.toString(response.getEntity());
         		            Log.d("~~~~~~~~~~http post result3 ",result);     
         		            
         		            if(result.equals("NewUserIsCreated")){
         		            	//new pwd created
         		 				//format check
         		 				
         		 				editor.putString(AdminManageActivity.ASPWD, newPin);
         		 				editor.commit();
         		 				PWD = shp.getString(AdminManageActivity.ASPWD, "");
         		            	
         		 				startSService();
         		            }else{
         		            	
         		            	//imm.toggleSoftInput(0, InputMethodManager.RESULT_SHOWN);
         	 					//imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
         	 					
         		            	Toast toast = Toast.makeText(getApplicationContext(), "Set PIN failed. Please try again.", Toast.LENGTH_SHORT);
         		            	toast.show();
         		            	//set return code
         		            	
         		            	finish();
         		            }
         		            
         		        } 
         		        } catch (Exception e) {
         					// TODO Auto-generated catch block
         		        	
         		        	imm.toggleSoftInput(0, InputMethodManager.RESULT_SHOWN);
     	 					imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
     	 					
     	 					Toast toast = Toast.makeText(getApplicationContext(), "Set PIN failed. Please try again.", Toast.LENGTH_SHORT);
     		            	toast.show();
     		            	//set return code
     		            	
     		            	finish();
         					e.printStackTrace();
         				}
 
         		        
                    }  
                });  
        builder.setNegativeButton("Cancel",  
                new DialogInterface.OnClickListener() {  
                    public void onClick(DialogInterface dialog, int whichButton) {  
                    	
                    	imm.toggleSoftInput(0, InputMethodManager.RESULT_SHOWN);
 	 					imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                        finish(); 
                    }  
                });  
        return builder.create();  
    }
    
	private PublicKey getPublicKey() throws Exception {
		// TODO Auto-generated method stub
        InputStream is = getResources().openRawResource(R.raw.publickey);
		ObjectInputStream ois = new ObjectInputStream(is);

		BigInteger m = (BigInteger)ois.readObject();
		BigInteger e = (BigInteger)ois.readObject();
	    RSAPublicKeySpec keySpec = new RSAPublicKeySpec(m, e);
		
	   
	    KeyFactory fact = KeyFactory.getInstance("RSA", "BC");
	    PublicKey pubKey = fact.generatePublic(keySpec);
	    
		return pubKey; 
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
    	SensorService.mIsRunning = false;     	
    	this.stopService(new Intent(MainActivity.this,SensorService.class));    	
    }
    private void startSService() {
    	if (ID.equals("") || PWD.equals("")){
    		IDPWDCheckDialog();
    	}
    	else {	
	        if (! SensorService.mIsRunning) {
	        	SensorService.mIsRunning = true;            
	            Thread t = new Thread(){
	        		public void run(){
	        		getApplicationContext().startService(new Intent(MainActivity.this,SensorService.class));
	        			}
	        		 };
	          t.start();
	        }
    	}
    }
    
    
    
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("SensorServiceActivity", "onActivityResult " + resultCode);
        Log.d(TAG2, "OnActivityResult~~~~~~~~~~~ "+ resultCode);
        
        switch (requestCode) {
        case INTENT_REQUEST_MAMAGE:
        	/*if(resultCode == Activity.RESULT_CANCELED){
        		
        		
        		
        	}else if(resultCode == Activity.RESULT_OK){
        		
        		
        	}else */
        	if(resultCode == 8){//set user PIN
        		
        		ID = shp.getString(AdminManageActivity.ASID, "");
        		UserSetPinDialog(this, ID).show();

        	
        	}else if(resultCode == 9){//regular management
        		
        		stopSService();
        		finish();
        		
        	}else{
        		
        		
        	}
        	
        	
        	break;
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
			if (SensorService.suspendFlag)
				SuspensionHint();
			else {
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
		}
		
		else if (item.getItemId() == R.id.Enable){
			if (SensorService.suspendFlag)
				SuspensionHint();
			else {
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
		}
		
		else if (item.getItemId() == R.id.Disable){
			if (SensorService.suspendFlag)
				SuspensionHint();
			else {
				mAdapter.disable();
				Toast.makeText(getApplicationContext(),"Bluetooth is disabled",Toast.LENGTH_LONG).show();			
	            return true;
			}
		}
		else if(item.getItemId() == R.id.manage){
			if (SensorService.suspendFlag)
				SuspensionHint();
			else {
				Intent serverIntent = new Intent(this, AdminManageActivity.class);
	            startActivityForResult(serverIntent, MainActivity.INTENT_REQUEST_MAMAGE);
			}
		}
		else 
			return false;
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
	
	private void createPinAlertDialog(){		
		LayoutInflater factory=LayoutInflater.from(MainActivity.this);  
	    //get view from my settings pin_number
	    final View DialogView=factory.inflate(R.layout.pin_number, null);  
		new AlertDialog.Builder(MainActivity.this)
	    .setTitle("Checking identity")  
	    .setCancelable(false)
	    .setView(DialogView)//using user defined view
	    .setPositiveButton(android.R.string.yes,   
	    		new DialogInterface.OnClickListener() {		          
			        @Override  
			        public void onClick(DialogInterface dialog, int which) {  
			        	mEdit = (EditText)DialogView.findViewById(R.id.edit_pin);
			        	mText = (TextView)DialogView.findViewById(R.id.text_pin);
			        	String pin = mEdit.getText().toString();
			        	if (pin.equals(SensorService.getPWD())){
			        	//Send the intent and trigger new Survey Activity....
			        	bedAlertDialog();			        	
			        	dialog.cancel();
			        	}
			        	else {
			        		//New AlertDialog to show instruction.
			        		new AlertDialog.Builder(MainActivity.this)
			        		.setTitle("Pin number is incorrect.")
			        		.setMessage("Please Press OK to exit and retry with your pin number.")
			        		.setPositiveButton("OK", null)
			        		.create().show();
			        	}			        	
			        	dialog.cancel();
			        }  
	    })
	    .create().show();
	}
	
	private void bedAlertDialog(){		
		new AlertDialog.Builder(MainActivity.this)
	    .setTitle("Bed Report")
	    .setMessage("Confirm that you are going to bed for the night.")
	    .setCancelable(false)
	    .setPositiveButton(R.string.yes,   
	    		new DialogInterface.OnClickListener() {		          
			        @Override  
			        public void onClick(DialogInterface dialog, int which) { 
			        	startSurveyScheduler();
			        	dialog.cancel();
			        	}
	    })
	    .setNegativeButton(R.string.no, null)
	    .create().show();
	}
	
	private void IDPWDCheckDialog(){		
		new AlertDialog.Builder(MainActivity.this)
	    .setTitle("ID is not set")
	    .setMessage("Please use the admin tools to set ID and password.")
	    .setCancelable(false)
	    .setPositiveButton(android.R.string.yes,   
	    		new DialogInterface.OnClickListener() {		          
			        @Override  
			        public void onClick(DialogInterface dialog, int which) { 
			        	dialog.cancel();
			    		finish();
			        	}
	    })
	    .create().show();
	}
	
	private void bedTimeCheckDialog(){		
		new AlertDialog.Builder(MainActivity.this)
	    .setTitle("Bed Report")
	    .setMessage("You should do bed report after 9:00 P.M. or ealier than 3:00 A.M.")
	    .setCancelable(false)
	    .setPositiveButton(android.R.string.yes,   
	    		new DialogInterface.OnClickListener() {		          
			        @Override  
			        public void onClick(DialogInterface dialog, int which) { 
			        	dialog.cancel();
			        	}
	    })
	    .create().show();
	}
	
	private void SuspensionHint(){		
		new AlertDialog.Builder(MainActivity.this)
	    .setTitle("The App is in suspension state.")
	    .setMessage("You should break suspension first before you click this button.")
	    .setCancelable(false)
	    .setPositiveButton(android.R.string.yes,   
	    		new DialogInterface.OnClickListener() {		          
			        @Override  
			        public void onClick(DialogInterface dialog, int which) { 
			        	dialog.cancel();
			        	}
	    })
	    .create().show();
	}
	
	private void SuspensionCheck(){
	    new AlertDialog.Builder(this)
	        .setTitle("Are you sure to do the suspension?")
	        .setCancelable(false)
	        .setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {

	            public void onClick(DialogInterface arg0, int arg1) {
	    			arg0.cancel();
	    			Intent i=new Intent(getApplicationContext(), SuspesionTimePicker.class);
					startActivity(i);	            	
	            }
	        })
	        .setNegativeButton(android.R.string.no, new android.content.DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dialog.cancel();
				}
	        	
	        })
	        .create().show();
	
	return;
	}
	
	private void breakSuspensionCheck(){
	    new AlertDialog.Builder(this)
	        .setTitle("Are you sure to break the suspension?")
	        .setCancelable(false)
	        .setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {

	            public void onClick(DialogInterface arg0, int arg1) {
	    			Intent breakSuspension = new Intent(SensorService.INTENT_BREAK_SUSPENSION);
					getApplicationContext().sendBroadcast(breakSuspension);
	    			arg0.cancel();
	            }
	        })
	        .setNegativeButton(android.R.string.no, new android.content.DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
	        	
	        })
	        .create().show();
	
	return;
	}
	
	public void onBackPressed(){
		    new AlertDialog.Builder(this)		        
		        .setTitle("The Back Button is disabled.")
		        .setMessage("The back button will terminate the program.")
		        .setCancelable(false)
		        .setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface arg0, int arg1) {
		            	arg0.cancel();
		            }
		        }).create().show();		
		return;
	}
}

