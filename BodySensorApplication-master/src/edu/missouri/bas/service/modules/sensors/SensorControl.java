package edu.missouri.bas.service.modules.sensors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import edu.missouri.bas.service.ScheduleController;
import edu.missouri.bas.service.SensorService;

//TODO: Convert intent broadcasting to handler
public class SensorControl extends ScheduleController{

	/*
	 * Sensor variables
	 */
	private SensorManager mSensorManager;
	private SensorEventListener sensorEventListener;
	
	private volatile double[] average = {0.0, 0.0, 0.0};
	private double readings;
	
	Context serviceContext;
	
	public static final String SENSOR_AVERAGE = "INTENT_EXTRA_SENSOR_AVG";
	protected static final String DATA_KEY_AVG = "DATA_KEY_AVG";
	
	public SensorControl(SensorManager sensorManager,
			Context serviceContext, long duration){
		this.duration = duration;
		mSensorManager = sensorManager;
		this.sensorEventListener = prepareListener();
		this.serviceContext = serviceContext;
	}
	
	/*public SensorControl(SensorManager sensorManager, 
			SensorEventListener listener){
		mSensorManager = sensorManager;
		this.sensorEventListener = listener;
	}*/
	
	@Override
	protected void setup() {
		average = new double[3];
		readings = 0;
		running = true;
        mSensorManager.registerListener(sensorEventListener, 
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 10);
       	
	
	}

	@Override
	protected void executeTimer() {
		running = false;
		mSensorManager.unregisterListener(sensorEventListener);
		
		average[0] /= readings;
		average[1] /= readings;
		average[2] /= readings;
		
		double[] avg = {average[0], average[1], average[2]};		
		Intent i = new Intent(SensorService.ACTION_SENSOR_DATA);
		i.putExtra(SENSOR_AVERAGE, avg);
		serviceContext.sendBroadcast(i);
		//serviceContext = null;
	}
	
	protected double[] getAverage(){
		double[] avg = {average[0], average[1], average[2]};
		return avg;
	}
	
	private SensorEventListener prepareListener(){
		return new SensorEventListener(){
			
			public void onAccuracyChanged(Sensor sensor, int accuracy) {	}
			
			public void onSensorChanged(SensorEvent event) {
				float[] values = event.values;
				
		    	synchronized (this) {
		    		if(running){
		    			readings++;
		    			average[0] += values[0];
		    			average[1] += values[1];
		    			average[2] += values[2];
		    			
					    
	}
		    	}
			}
		
		    	};
	}
	}
	

