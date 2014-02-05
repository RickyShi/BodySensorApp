package edu.missouri.bas.survey;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import edu.missouri.bas.R;
import edu.missouri.bas.service.SensorService;
import edu.missouri.bas.survey.XMLSurveyActivity.StartSound;

public class SurveyPinCheck extends Activity {
	
	private ProgressDialog mydialog;
	private TextView mText;
	private EditText mEdit;
	private Button pButton;
	private Button bButton;
	private String surveyName;
	private String surveyFile;
	
	private MediaPlayer mp;
    
    

    public class StartSound extends TimerTask {
    	@Override    	
    	public void run(){ 
        mp = MediaPlayer.create(getApplicationContext(), R.raw.bodysensor_alarm);
    	mp.start();
    	}
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.survey_pincheck);
		//call pin-Dialog function
		createPinAlertDialog();	     
	    	     
	    bButton = (Button)findViewById(R.id.button_exit);
	    pButton = (Button)findViewById(R.id.button_pin);
	    
	    surveyName = getIntent().getStringExtra("survey_name");
		surveyFile = getIntent().getStringExtra("survey_file");
		// Alarm when the following two kind of survey is triggered
		if(surveyName.equalsIgnoreCase("RANDOM_ASSESSMENT") && surveyFile.equalsIgnoreCase("RandomAssessmentParcel.xml"))
		{
			Timer t=new Timer();
			t.schedule(new  StartSound(),1000*5);			
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	        v.vibrate(1000);
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
		new AlertDialog.Builder(SurveyPinCheck.this)
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
			        	if (pin.equals("1234")){
			        	//Send the intent and trigger new Survey Activity....
			        	dialog.cancel();
		        		Intent launchSurvey = 
								new Intent(getApplicationContext(), XMLSurveyActivity.class);
						launchSurvey.putExtra("survey_file", surveyFile);
						launchSurvey.putExtra("survey_name", surveyName);
						startActivity(launchSurvey);
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
	    		)
	    .create().show();
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
}
