package edu.missouri.bas;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;

public class SurveyScheduler extends Activity {
	
	public int StartHours;
	public int StartMinutes;
	//Ricky 2/10
	//public int EndHours;
	//public int EndMinutes;
	private boolean mIsRunning=false;
	private static final String USER_PATH = "sdcard/BSAUserData/";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.survey_scheduler);
		TimePicker tpStartTime=(TimePicker)findViewById(R.id.tpStartTime);
		//TimePicker tpEndTime=(TimePicker)findViewById(R.id.tpEndTime);
		Button btnStartTimer=(Button)findViewById(R.id.btnStartTimer);
		Calendar c=Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
		String t=dateFormat.format(c.getTime());
		String []Time=t.split(":");
		int CurrentHours=Integer.parseInt(Time[0]);
		int CurrentMinutes=Integer.parseInt(Time[1]);
		StartHours=CurrentHours;
		StartMinutes=CurrentMinutes;
		
		//Ricky 2/10
		//end time: 23:59
		//Design to let the survey end time to midnight
		//EndHours=CurrentHours;
		//EndMinutes=CurrentMinutes;
		//EndHours=23;
		//EndMinutes=59;
		
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
								Intent i=new Intent(getApplicationContext(), SurveyStatus.class);
								startActivity(i);
					    		
					    		
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
					Intent i=new Intent(getApplicationContext(), SurveyStatus.class);
					startActivity(i);
			   }			     	
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
