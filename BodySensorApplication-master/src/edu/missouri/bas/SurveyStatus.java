package edu.missouri.bas;
import edu.missouri.bas.service.SensorService;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SurveyStatus extends Activity{

	public static String IsScheduled="IS_SCHEDULED";
	public static int Scheduled = 1;
	Button btnSchedule;	
	Button btnReturn;
	static TextView tvSetSurveyStatus;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.survey_status);
		btnSchedule=(Button)findViewById(R.id.btnSchedule);
		btnReturn=(Button)findViewById(R.id.btnReturn);
		
		tvSetSurveyStatus=(TextView)findViewById(R.id.tvSetSurveyStatus);
		//Ricky 2/11/2014 Not for random survey anymore
		/*
		if(SensorService.getStatus())
		{
			
			tvSetSurveyStatus.setText("Scheduled");
		}
		*/
		SharedPreferences bedTime = this.getSharedPreferences(SensorService.BED_TIME, MODE_PRIVATE);
		String wakeTime = bedTime.getString(SensorService.BED_TIME_INFO, "none")+" A.M.";
		//Log.d(SurveyScheduler.BED_TIME, bedTime.getString(SurveyScheduler.BED_TIME_INFO, "none"));
		if (wakeTime.equals("none A.M.")){
			tvSetSurveyStatus.setText("12:00 P.M.");
		} else {
			tvSetSurveyStatus.setText(wakeTime);
		}
		//text for wake_up Time
		btnSchedule.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(), SurveyScheduler.class);
				startActivity(i);
			}
        });
		
		btnReturn.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(), MainActivity.class);
				startActivity(i);
				finish();
			}
        });
		
		
		
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	
	
	
	
	

}
