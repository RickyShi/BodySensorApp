package edu.missouri.bas.survey;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import edu.missouri.bas.R;

public class SurveyPinCheck extends Activity {
	
	ProgressDialog mydialog;
	TextView mText;
	EditText mEdit;
	Button pButton;
	Button bButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.survey_pincheck);
		//call pin-Dialog function
		createPinAlertDialog();	     
	    	     
	    bButton = (Button)findViewById(R.id.button_exit);
	    pButton = (Button)findViewById(R.id.button_pin);
	    
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
		        .setMessage("This action will cancel the Survey.")
		        .setCancelable(false)
		        .setNegativeButton(android.R.string.no, null)
		        .setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {

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
			        	//Log.d("wtest",pin);
			        	//createProgressDialog();
			        	if (pin.equals("1234")){
			        	//To do sth here
			        	//send the intent and trigger new Activity....
			        		//createProgressDialog();
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
	
	// ProgressDialog designed to show better UI. Not used for now.
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
