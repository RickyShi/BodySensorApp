package edu.missouri.bas;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.missouri.bas.service.SensorService;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

public class SuspesionTimePicker extends Activity {
	
	private int suspensionH;
	private int suspensionM;
	
	public Context ctx = SuspesionTimePicker.this;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.suspension_picker);
		TimePicker tpSuspension=(TimePicker)findViewById(R.id.tpSuspension);
		tpSuspension.setIs24HourView(true);
		tpSuspension.setCurrentHour(0);
		tpSuspension.setCurrentMinute(0);
		Button btnSuspension=(Button)findViewById(R.id.btnSuspension);
		
		tpSuspension.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
			
			@Override
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
				// TODO Auto-generated method stub
				suspensionH=hourOfDay;
				suspensionM=minute;				
			}
		});
		

		  
		btnSuspension.setOnClickListener(new View.OnClickListener(){

			public void onClick(View v) {
				if (suspensionH < 2){
					if (suspensionH==0 && suspensionM==0){
						Toast.makeText(getApplicationContext(),"Suspension Time must be longer than 1 minute!",Toast.LENGTH_LONG).show();
					} else {
						Intent startSuspension = new Intent(SensorService.INTENT_SUSPENSION);
						startSuspension.putExtra("H",suspensionH);
						startSuspension.putExtra("M",suspensionM);
						getApplicationContext().sendBroadcast(startSuspension);
						finish();
						Toast.makeText(getApplicationContext(),"Suspension time is: "+suspensionH+":"+suspensionM,Toast.LENGTH_LONG).show();
					}
				} 
				else {
					Toast.makeText(getApplicationContext(),"Suspension Time must be less than 2 hours!",Toast.LENGTH_LONG).show();
				}
			}
        });
		
	}
	

}
