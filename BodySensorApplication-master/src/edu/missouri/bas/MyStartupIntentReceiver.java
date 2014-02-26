/**
 * @author Ricky
 * -----------Restart the morning alarm by detecting the System Boot to fix the System-shutSown-Clear-Alarm problem-----------
 * Receiver is declared in the Manifest.XML 
 */
package edu.missouri.bas;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;

public class  MyStartupIntentReceiver extends BroadcastReceiver{

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
		MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.voice_notification);
		mediaPlayer.start();
		Log.d("M1","onCreate");
	}
}
