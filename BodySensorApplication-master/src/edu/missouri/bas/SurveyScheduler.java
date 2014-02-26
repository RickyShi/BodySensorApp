package edu.missouri.bas;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import edu.missouri.bas.service.SensorService;
import edu.missouri.bas.survey.SurveyPinCheck;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

public class SurveyScheduler extends Activity {
	
	public int StartHours;
	public int StartMinutes;
	//Ricky 2/10
	//public int EndHours;
	//public int EndMinutes;
	private boolean mIsRunning=false;
	//private static final String USER_PATH = "sdcard/BSAUserData/";
	
	public Context ctx = SurveyScheduler.this;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.survey_scheduler);
		TimePicker tpStartTime=(TimePicker)findViewById(R.id.tpStartTime);
		//TimePicker tpEndTime=(TimePicker)findViewById(R.id.tpEndTime);
		Button btnStartTimer=(Button)findViewById(R.id.btnStartTimer);
		
		//set the default start time as current time.
		Calendar c=Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
		String t=dateFormat.format(c.getTime());
		String []Time=t.split(":");
		int CurrentHours=Integer.parseInt(Time[0]);
		int CurrentMinutes=Integer.parseInt(Time[1]);
		StartHours=CurrentHours;
		StartMinutes=CurrentMinutes;
		
	  	tpStartTime.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
			
			@Override
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
				// TODO Auto-generated method stub
				StartHours=hourOfDay;
				StartMinutes=minute;
				
			}
		});
		
		// Ricky 2/10
		/*  
		  tpEndTime.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
			
			@Override
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
				// TODO Auto-generated method stub
				EndHours=hourOfDay;
				EndMinutes=minute;
				
			}
		});
		*/
		  
		btnStartTimer.setOnClickListener(new View.OnClickListener(){

			public void onClick(View v) {
				if (StartHours < 12){				
					String timeToWrite = StartHours+":"+StartMinutes;
					String hourToWrite = String.valueOf(StartHours);
					String minuateToWrite = String.valueOf(StartMinutes);
					SharedPreferences bedTime = ctx.getSharedPreferences(SensorService.BED_TIME, MODE_PRIVATE);
					Editor editor = bedTime.edit();
					editor.putString(SensorService.BED_TIME_INFO, timeToWrite);
					editor.putString(SensorService.BED_HOUR_INFO, hourToWrite);
					editor.putString(SensorService.BED_MIN_INFO, minuateToWrite);
					editor.commit();
					//Log.d(BED_TIME, bedTime.getString(BED_TIME_INFO, "none"));
					//If current time is before 3 A.M, set the alarm Day to be the current Day.
					
					//Send Broadcast. And SensorService will handle it in the onReceive function.
					Intent startScheduler = new Intent(SensorService.ACTION_SCHEDULE_MORNING);
					getApplicationContext().sendBroadcast(startScheduler);
					Intent i=new Intent(getApplicationContext(), SurveyStatus.class);
					startActivity(i);
					finish();
				} 
				else {
					Toast.makeText(getApplicationContext(),"Start Time must be earlier than 12:00 P.M.",Toast.LENGTH_LONG).show();
				}
				//Ricky 2/11 instead of using file storage, we use sharePreferences
				/*
				File bedDir = new File(USER_PATH);
				if (!bedDir.exists()){
					bedDir.mkdirs();
				}
				File bedTime = new File(USER_PATH,"time.txt");
				if (bedTime.exists()){
					bedTime.delete();					
				}
				try {
					bedTime.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.d("bedTime","Create File ERROR");
				}
				try {
					writeToFile(bedTime, StartHours+":"+StartMinutes);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				*/
				//Ricky 2/10
				/*
				Calendar c=Calendar.getInstance();
				SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
				String currentTime=dateFormat.format(c.getTime());
				String selectedStartTime=String.valueOf(StartHours)+":"+String.valueOf(StartMinutes);
				String selectedEndTime=String.valueOf(EndHours)+":"+String.valueOf(EndMinutes);
				Date CurrentTime = null;
				try {
					CurrentTime = dateFormat.parse(currentTime);
					
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Date StartTime = null;
				Date EndTime = null;
				try {
					StartTime = dateFormat.parse(selectedStartTime);
					EndTime=dateFormat.parse(selectedEndTime);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			    if(!(StartTime.after(CurrentTime) || StartTime.equals(CurrentTime)))
			    {
			    	
			    	Toast.makeText(getApplicationContext(),"Start Time must be greater than equal to the Current Time",Toast.LENGTH_LONG).show();
			    }
			    /*
			    else if(!(EndTime.after(StartTime)))
			    {
			    	Toast.makeText(getApplicationContext(),"End Time must be greater than the Start Time",Toast.LENGTH_LONG).show();
			    	
			    }
			    
			    else if(!((EndTime.getHours()-StartTime.getHours())>0))
			    	{
			    		Toast.makeText(getApplicationContext(),"Difference between Start and End Time must be atleast one hour",Toast.LENGTH_LONG).show();
				    }
			   else if((EndTime.getHours()-StartTime.getHours())==1)
			    {
			    		if(!((EndTime.getMinutes()-StartTime.getMinutes())>=0)){
			    			
			    			Toast.makeText(getApplicationContext(),"Difference between Start and End Time must be atleast one hour",Toast.LENGTH_LONG).show();
							   
			    		}
			    		
			    	 else
					   {
					    		Intent startScheduler = new Intent(SensorService.ACTION_SCHEDULE_SURVEY);	
					    		int Hours=StartTime.getHours();
					    		int Minutes=StartTime.getMinutes();
					    		int EndHours=EndTime.getHours();
					    		int EndMinutes=EndTime.getMinutes();
					    		startScheduler.putExtra(SensorService.START_HOUR,Hours);					    		
					    		startScheduler.putExtra(SensorService.END_HOUR,EndHours);
					    		startScheduler.putExtra(SensorService.START_MIN,Minutes);					    		
					    		startScheduler.putExtra(SensorService.END_MIN,EndMinutes);
								getApplicationContext().sendBroadcast(startScheduler);
								Toast.makeText(getApplicationContext(),"Message sent to the service",Toast.LENGTH_LONG).show();								
								//Intent i=new Intent(getApplicationContext(), SurveyStatus.class);
								//startActivity(i);
								finish();
					    		
					    		
					 }
			    				   		
			    }
			    */	
				/*
			   else {
				   
				    Intent startDrinkScheduler = new Intent(SensorService.ACTION_SCHEDULE_SURVEY);	
		    		int Hours=StartTime.getHours();
		    		int Minutes=StartTime.getMinutes();
		    		int EndHours=EndTime.getHours();
		    		int EndMinutes=EndTime.getMinutes();
		    		startDrinkScheduler.putExtra(SensorService.START_HOUR,Hours);					    		
		    		startDrinkScheduler.putExtra(SensorService.END_HOUR,EndHours);
		    		startDrinkScheduler.putExtra(SensorService.START_MIN,Minutes);					    		
		    		startDrinkScheduler.putExtra(SensorService.END_MIN,EndMinutes);
		    		getApplicationContext().sendBroadcast(startDrinkScheduler);
					Toast.makeText(getApplicationContext(),"Message sent to the service",Toast.LENGTH_LONG).show();					
					//Intent i=new Intent(getApplicationContext(), SurveyStatus.class);
					//startActivity(i);
					finish();
			 */
			}
        });
		
	}
	
	protected void writeToFile(File f, String toWrite) throws IOException{
		FileWriter fw = new FileWriter(f, true);
		fw.write(toWrite);		
        fw.flush();
		fw.close();
	}

	
	
	

}
