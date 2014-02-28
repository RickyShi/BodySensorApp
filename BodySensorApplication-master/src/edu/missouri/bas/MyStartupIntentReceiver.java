/**
 * @author Ricky
 * -----------Restart the morning alarm by detecting the System Boot to fix the System-shutSown-Clear-Alarm problem-----------
 * Receiver is declared in the Manifest.XML 
 */
package edu.missouri.bas;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class  MyStartupIntentReceiver extends BroadcastReceiver{
	
	private String action="android.intent.action.MAIN";  
	private String category="android.intent.category.LAUNCHER";
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		/*
		if (wakeHour.equals("none")||wakeMin.equals("none")){
			iWakeHour = 11;
			iWakeMin = 59;
		} else {
			iWakeHour = Integer.parseInt(wakeHour);
			iWakeMin = Integer.parseInt(wakeMin);
		}
		setMorningSurveyAlarm(iWakeHour,iWakeMin);
		*/
		
		Intent s = new Intent(context,MainActivity.class);
		s.setAction(action);
		s.addCategory(category);
		s.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(s);
	}
}
