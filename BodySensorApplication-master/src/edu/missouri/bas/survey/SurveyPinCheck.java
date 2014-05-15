package edu.missouri.bas.survey;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import edu.missouri.bas.R;
import edu.missouri.bas.activities.AdminManageActivity;
import edu.missouri.bas.service.SensorService;


public class SurveyPinCheck extends Activity {
	
	private TextView mText;
	private EditText mEdit;
	private Button pButton;
	private Button bButton;
	private String surveyName;
	private String surveyFile;
	private MediaPlayer mp;
	// Ricky 3/6/14
	// define a dialog for future force close
	private AlertDialog diaLog;
	//Ricky 4/1/14
	private int randomSeq;
	public static Context SurveyPinContext;
	String errMSG ="Please check your wifi or dataplan.\r\nThe phone is offline now.";

    private class StartSound extends TimerTask {
    	@Override    	
    	public void run(){ 
        mp = MediaPlayer.create(getApplicationContext(), R.raw.bodysensor_alarm);
    	mp.start();
    	}
    }
    
    private class SurveyNotCompletedAlarm extends TimerTask{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d("PINAlarm","Final ALarm");
			diaLog.cancel();
			finish();
			this.cancel();
			
		}
    	
    }
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setVolumeControlStream(AudioManager.STREAM_ALARM);
		setContentView(R.layout.survey_pincheck);
		//call pin-Dialog function
		createPinAlertDialog();	   
		
		SurveyPinContext = this;
		SharedPreferences shp = getSharedPreferences("PINLOGIN", Context.MODE_PRIVATE);
	    String ID = shp.getString(AdminManageActivity.ASID, "");
		
	    bButton = (Button)findViewById(R.id.button_exit);
	    pButton = (Button)findViewById(R.id.button_pin);
	    
	    /*
         * Here to alarm the user to do the survey
         * Ricky 2014/2/13
         */
        Date aTime1 = new Date();
        Date aTime2 = new Date();
        Date aTime3 = new Date();
        Date aTime4 = new Date();
        aTime1.setMinutes(aTime1.getMinutes()+5);
        aTime2.setMinutes(aTime2.getMinutes()+10);
        aTime3.setMinutes(aTime3.getMinutes()+15);
        aTime4.setMinutes(aTime4.getMinutes()+15);
        aTime4.setSeconds(aTime4.getSeconds()+20);
        SensorService.alarmTask1 = new StartSound();
        SensorService.alarmTask2 = new StartSound();
        SensorService.alarmTask3 = new StartSound();
        SensorService.alarmTask4 = new SurveyNotCompletedAlarm();
        SensorService.alarmTimer.schedule(SensorService.alarmTask1, aTime1);
        SensorService.alarmTimer.schedule(SensorService.alarmTask2, aTime2);
        SensorService.alarmTimer.schedule(SensorService.alarmTask3, aTime3);
        SensorService.alarmTimer.schedule(SensorService.alarmTask4, aTime4);
	    //End of the alarm part
        
	    surveyName = getIntent().getStringExtra("survey_name");
		surveyFile = getIntent().getStringExtra("survey_file");
		// Alarm when the following two kind of survey is triggered
		if(surveyName.equalsIgnoreCase("RANDOM_ASSESSMENT") && surveyFile.equalsIgnoreCase("RandomAssessmentParcel.xml"))
		{				
			randomSeq = getIntent().getIntExtra("random_sequence", 0);
			if (randomSeq == 6)
				SensorService.setStatus(false);
			String rsID = String.valueOf(randomSeq);
			Calendar rsT = Calendar.getInstance();
			String rsDate = rsT.get(Calendar.DAY_OF_MONTH)+"/"+(rsT.get(Calendar.MONTH)+1)+"/"+rsT.get(Calendar.YEAR);
			String uID = String.valueOf(ID);
			TriggerSignal triggerSignal = new TriggerSignal();
			triggerSignal.execute(uID,rsDate,rsID);
			Timer t=new Timer();
			t.schedule(new  StartSound(),1000*5);			
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	        v.vibrate(1000);
		}
		//ADD VOICE AND VIBRATE CONTROL TO THE MORNING REPORT
		if(surveyName.equalsIgnoreCase("MORNING_REPORT_ALARM") && surveyFile.equalsIgnoreCase("MorningReportParcel.xml"))
		{	
			//Automatically triggered Morning Survey
	        TransmitData transmitData=new TransmitData();
//			transmitData.execute("Trigger."+ID+"."+Calendar.getInstance().get(Calendar.MONTH)+"_"+Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
//					Calendar.getInstance().getTime().toString()+",Morning Survey is automatically started\n");
	        transmitData.execute("Event."+ID,Calendar.getInstance().getTime().toString()+",Morning Survey is automatically started");
			Timer t=new Timer();
			t.schedule(new  StartSound(),1000*5);			
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	        v.vibrate(1000);
	        surveyName = "MORNING_REPORT";	        
		}
		//ADD VOICE AND VIBRATE CONTROL TO THE DRINKFOLLOWUP
		if(surveyName.equalsIgnoreCase("DRINKING_FOLLOWUP") && surveyFile.equalsIgnoreCase("DrinkingFollowup.xml"))
		{	
			SensorService.drinkUpFlag =true;
			Timer t=new Timer();
			t.schedule(new  StartSound(),1000*5);			
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	        v.vibrate(1000);
		}
		
	    bButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				onBackPressed();
				
			}
		});
	    
	    pButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				createPinAlertDialog();
			}	
		});
	    
	}

	public void onBackPressed(){
		    new AlertDialog.Builder(this)
		        .setTitle("Are you sure you want to exit?")
		        .setMessage("Did you complete the Survey?")
		        .setCancelable(false)
		        .setNegativeButton(R.string.no, null)
		        .setPositiveButton(R.string.yes, new android.content.DialogInterface.OnClickListener() {

		            public void onClick(DialogInterface arg0, int arg1) {
		            	SurveyPinCheck.super.onBackPressed();
		            }
		        }).create().show();
		
		return;
	}
	
	private void createPinAlertDialog(){
		LayoutInflater factory=LayoutInflater.from(SurveyPinCheck.this);  
	    //get view from my settings pin_number
	    final View DialogView=factory.inflate(R.layout.pin_number, null); 
	    AlertDialog.Builder alertDialogBuilder  =new AlertDialog.Builder(SurveyPinCheck.this);
	    alertDialogBuilder.setTitle("Checking identity")  
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
				        	if (mp!=null)
				        		mp.stop();
				        	dialog.cancel();
			        		Intent launchSurvey = 
									new Intent(getApplicationContext(), XMLSurveyActivity.class);
							launchSurvey.putExtra("survey_file", surveyFile);
							launchSurvey.putExtra("survey_name", surveyName);
							if (surveyName.equalsIgnoreCase("RANDOM_ASSESSMENT"))
								launchSurvey.putExtra("random_sequence", randomSeq);
							startActivity(launchSurvey);
							finish();
			        	}
			        	else {
			        		//New AlertDialog to show instruction.
			        		new AlertDialog.Builder(SurveyPinCheck.this)
			        		.setTitle("Pin number is incorrect.")
			        		.setMessage("Please Press OK to exit and retry with your pin number.")
			        		.setPositiveButton("OK", null)
			        		.create().show();
			        	}			        	
			        	dialog.cancel(); 
			        }  
	    })
	    .setNegativeButton(android.R.string.no, 
	    		new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				dialog.cancel();
			}
	    }
	    		);
	    diaLog = alertDialogBuilder.create();
	    diaLog.show();
	}
	
	// ProgressDialog designed to show better UI. Not used now.
	/*
	private void createProgressDialog(){
		mydialog=ProgressDialog.show(SurveyPinCheck.this, "Checking Pin Number", "login",true);  
        new Thread()  
        {  
            public void run()  
            {  
                try  
                {  
                    sleep(1000);  
                }catch(Exception e)  
                {  
                    e.printStackTrace();  
                }finally  
                {  				                        
                    mydialog.dismiss(); 
                }  
            }  
        }.start();
	}
	*/
	private void cancelAllTimerTask()
	{	
		if(SensorService.alarmTimer!=null)
		{
	    	CancelTask(SensorService.alarmTask1);
			CancelTask(SensorService.alarmTask2);
			CancelTask(SensorService.alarmTask3);
			CancelTask(SensorService.alarmTask4);
			PurgeTimers(SensorService.alarmTimer);
		}		
	}
	public void CancelTask(TimerTask tTask){
		if  (tTask!=null)
		tTask.cancel();
	}
	public void PurgeTimers(Timer t)
	{
		if(t!=null)
		{
		t.purge();	
		}
	}
	protected void onDestroy(){
    	//cancelAllTimerTask();
    	super.onDestroy();
    }
	
	private class TransmitData extends AsyncTask<String,Void, Boolean>
	{

		@Override
		protected Boolean doInBackground(String... strings) {
			// TODO Auto-generated method stub
			 String fileName=strings[0];
	         String dataToSend=strings[1];
	         if(checkDataConnectivity())
	 		{
	         HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Crt/writeArrayToFile.php");
	         //HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Test/writeArrayToFile.php");
	         List<NameValuePair> params = new ArrayList<NameValuePair>();
	         //file_name 
	         params.add(new BasicNameValuePair("file_name",fileName));        
	         //data                       
	         params.add(new BasicNameValuePair("data",dataToSend));
	         try {
	         	        	
	             request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
	             HttpResponse response = new DefaultHttpClient().execute(request);
	             if(response.getStatusLine().getStatusCode() == 200){
	                 String result = EntityUtils.toString(response.getEntity());
	                 Log.d("Sensor Data Point Info",result);                
	                // Log.d("Wrist Sensor Data Point Info","Data Point Successfully Uploaded!");
	             }
	             return true;
	         } 
	         catch (Exception e) 
	         {	             
	             e.printStackTrace();
	             return false;
	         }
	 	  }
	     	
	     else 
	     {
	     	Log.d("Sensor Data Point Info","No Network Connection:Data Point was not uploaded");
	     	Toast.makeText(SurveyPinContext, errMSG, Toast.LENGTH_LONG).show();
	     	return false;
	      } 
		    
		}
		
	}
	 public static boolean checkDataConnectivity() {
	    	ConnectivityManager connectivity = (ConnectivityManager) SurveyPinContext
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connectivity != null) {
				NetworkInfo[] info = connectivity.getAllNetworkInfo();
				if (info != null) {
					for (int i = 0; i < info.length; i++) {
						if (info[i].getState() == NetworkInfo.State.CONNECTED) {
							return true;
						}
					}
				}
			}
			return false;
	}
	 private class TriggerSignal extends AsyncTask<String,Void, Boolean>
		{

			@Override
			protected Boolean doInBackground(String... strings) {
				// TODO Auto-generated method stub
		         String UID=strings[0];
		         String Date=strings[1];
		         String RSID=strings[2];
		         if(checkDataConnectivity())
		 		{
		         HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Crt/compliance.php");
		         List<NameValuePair> params = new ArrayList<NameValuePair>();
		         params.add(new BasicNameValuePair("category","trigger"));                            
		         params.add(new BasicNameValuePair("UID",UID));
		         params.add(new BasicNameValuePair("Date",Date));
		         params.add(new BasicNameValuePair("RSID",RSID));
		         try {
		         	        	
		             request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
		             HttpResponse response = new DefaultHttpClient().execute(request);
		             if(response.getStatusLine().getStatusCode() == 200){
		                 String result = EntityUtils.toString(response.getEntity());
		                 Log.d("Sensor Data Point Info",result);                
		                // Log.d("Wrist Sensor Data Point Info","Data Point Successfully Uploaded!");
		             }
		             return true;
		         } 
		         catch (Exception e) 
		         {	             
		             e.printStackTrace();
		             return false;
		         }
		 	  }
		     	
		     else 
		     {
		     	Log.d("Sensor Data Point Info","No Network Connection:Data Point was not uploaded");
		     	Toast.makeText(SurveyPinContext, errMSG, Toast.LENGTH_LONG).show();
		     	return false;
		      } 
			    
			}
			
		}
}
