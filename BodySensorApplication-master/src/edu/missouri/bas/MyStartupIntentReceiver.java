/**
 * @author Ricky
 * -----------Restart the morning alarm by detecting the System Boot to fix the System-shutSown-Clear-Alarm problem-----------
 * Receiver is declared in the Manifest.XML 
 */
package edu.missouri.bas;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import edu.missouri.bas.service.SensorService;

public class  MyStartupIntentReceiver extends BroadcastReceiver{
	
	private String action="android.intent.action.MAIN";  
	private String category="android.intent.category.LAUNCHER";
	private final int DELAY_TIME = 30000;
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		final Context t = context;
		Intent s = new Intent(context,MainActivity.class);
		s.setAction(action);
		s.addCategory(category);
		s.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(s);
		Handler h = new Handler();
		h.postDelayed(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				Intent startScheduler = new Intent(SensorService.ACTION_SCHEDULE_MORNING_RESTART);
				t.sendBroadcast(startScheduler);
			}
			
		}, DELAY_TIME);		
	}
}
