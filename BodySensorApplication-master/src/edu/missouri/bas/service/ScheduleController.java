package edu.missouri.bas.service;

import java.util.Timer;
import java.util.TimerTask;

public abstract class ScheduleController {
	public static final String LOCATION_INTENT_KEY = "location_intent_key";
	public static final String INTENT_ACTION_LOCATION ="intent_action_location";
	
	protected Timer scheduleTimer;
	
	protected volatile boolean running = false;
	protected long duration;
	
	public boolean isRunning(){
		return running;
	}
	
	public void startRecording(){
		if(scheduleTimer != null) 
			scheduleTimer.cancel();
		
		scheduleTimer = new Timer();
		
		
		setup();
		
		running = true;
		
		scheduleTimer.schedule(new TimerTask(){
			@Override
			public void run() {
				executeTimer();
			}
		}, duration);
		
		running = false;
		
		scheduleTimer = null;
	}
	
	protected abstract void setup();
	
	protected abstract void executeTimer();
	
	public void cancel(){
		if(scheduleTimer != null){
			scheduleTimer.cancel();
		}
	}
}
