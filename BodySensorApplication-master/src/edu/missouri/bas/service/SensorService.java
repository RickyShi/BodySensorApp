package edu.missouri.bas.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import edu.missouri.bas.MainActivity;
import edu.missouri.bas.R;
import edu.missouri.bas.SensorConnections;
import edu.missouri.bas.SurveyScheduler;
import edu.missouri.bas.SurveyStatus;
import edu.missouri.bas.bluetooth.BluetoothRunnable;
import edu.missouri.bas.bluetooth.affectiva.AffectivaPacket;
import edu.missouri.bas.bluetooth.affectiva.AffectivaRunnable;
import edu.missouri.bas.bluetooth.equivital.EquivitalRunnable;
import edu.missouri.bas.datacollection.InternalSensor;
import edu.missouri.bas.service.modules.location.ActivityRecognitionScan;
import edu.missouri.bas.service.modules.location.DetectionRemover;
import edu.missouri.bas.service.modules.location.LocationControl;
import edu.missouri.bas.service.modules.sensors.SensorControl;
import edu.missouri.bas.survey.SurveyPinCheck;
import edu.missouri.bas.survey.XMLSurveyActivity;
import edu.missouri.bas.survey.XMLSurveyActivity.StartSound;
import edu.missouri.bas.survey.answer.SurveyAnswer;
import android.R.string;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.equivital.sdk.connection.SemBluetoothConnection;
import com.equivital.sdk.decoder.SDKLicense;
import com.equivital.sdk.decoder.SemDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationClient;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;










//Ricky 2013/12/09
import android.os.AsyncTask;

public class SensorService extends Service implements
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener
{ 

    private final String TAG = "SensorService";   
	/*
	 * Android component variables used by the service
	 */
	public static Context serviceContext;
	private IBinder mBinder = new ServiceBinder<SensorService>(SensorService.this);

	/*
	 * Notification variables
	 */
	private NotificationManager notificationManager;
	private Notification notification;
	
	/*
	 * Sensor variables
	 */
	private LocationManager mLocationManager;
	private SensorManager mSensorManager;
	private SensorEventListener mSensorListener;
    private long stime;
	private SensorControl sensorControl = null;
	private static LocationControl locationControl = null;
	
	/*
	 * File I/O Variables 
	 */
	public final static String BASE_PATH = "sdcard/TestResults/";
	public String PATH_TOREAD;
	//public static File BASE_PATH;
	private final String[] surveyNames = {"CRAVING_EPISODE","DRINKING_FOLLOWUP",
			"MORNING_REPORT","RANDOM_ASSESSMENT","MOOD_DYSREGULATION","INITIAL_DRINKING"};
	private HashMap<String, String> surveyFiles;
	
	
	PowerManager mPowerManager;
	WakeLock serviceWakeLock;
	
	/*
	 * Alarm manager variables, for scheduling intents
	 */
	public static AlarmManager mAlarmManager;
	private PendingIntent scheduleSurvey;
	private PendingIntent drinkfollowupSurvey;
	/*
	 * Ricky 2/11
	 * Alarm manager variables, for scheduling intents
	 * start the Body Sensor App tomorrow and start the morning report
	 */
	public static AlarmManager bAlarmManager;
	private PendingIntent morningWakeUp;
	private PendingIntent morningReport;
	private int iWakeHour;
	private int iWakeMin;
	//private static PendingIntent scheduleLocation;
	private static PendingIntent scheduleCheck;
	private static PendingIntent triggerSound;
	private static PendingIntent triggerSound2;
	//private PendingIntent surveyIntent;

	/*
	 * Static intent actions
	 */
	public static final String ACTION_SCHEDULE_SURVEY = "INTENT_ACTION_SCHEDULE_SURVEY";

	public static final String ACTION_SCHEDULE_SENSOR = "INTENT_ACTION_SCHEDULE_SENSOR";
	
	public static final String ACTION_SCHEDULE_LOCATION = "INTENT_ACTION_SCHEDULE_LOCATION";
	
	public static final String ACTION_START_SENSORS="INTENT_ACTION_START_SENSORS";
	
	public static final String ACTION_STOP_LOCATIONCONTROL = "INTENT_ACTION_STOP_LOCATIONCONTROL";
	
	public static final String ACTION_SENSOR_DATA = "INTENT_ACTION_SENSOR_DATA";
	
	private static final String ACTION_TRIGGER_SOUND = "INTENT_ACTION_TRIGGER_SOUND";
	
	private static final String ACTION_TRIGGER_SOUND2 = "INTENT_ACTION_TRIGGER_SOUND2";	
	
	private static final String ACTION_SCHEDULE_CHECK = "INTENT_ACTION_SCHEDULE_CHECK";

	public static final String ACTION_TRIGGER_SURVEY = "INTENT_ACTION_TRIGGER_SURVEY";
	
	public static final String ACTION_START_SOUND    =  "INTENT_ACTION_START_SOUND";
	
	public static final String ACTION_CONNECT_BLUETOOTH = "INTENT_ACTION_CONNECT_BLUETOOTH";
	
	public static final String ACTION_DISCONNECT_BLUETOOTH = "INTENT_ACTION_DISCONNECT_BLUETOOTH";
	
	public static final String ACTION_GET_BLUETOOTH_STATE = "INTENT_ACTION_BLUETOOTH_STATE";
	
	public static final String ACTION_BLUETOOTH_RESULT = "INTENT_ACTION_BLUETOOTH_RESULT";
	
	public static final String ACTION_BLUETOOTH_STATE_RESULT = "INTENT_ACTION_BLUETOOTH_STATE_RESULT";
	
	public static final String ACTION_RECONNECT_CHESTSENSOR ="INTENT_ACTION_RECONNECT_CHESTSENSOR";	
	
	public static final String ACTION_START_RECONNECTING ="INTENT_ACTION_START_RECONNECTING";

	public static final String INTENT_EXTRA_BT_DEVICE = "EXTRA_DEVICE_ADR";
	
	public static final String INTENT_EXTRA_BT_MODE = "EXTRA_DEVICE_MODE";
	
	public static final String INTENT_EXTRA_BT_TYPE = "EXTRA_DEVICE_TYPE";
	
	public static final String INTENT_EXTRA_BT_UUID = "EXTRA_DEVICE_UUID";
	
	public static final String INTENT_EXTRA_BT_RESULT = "EXTRA_BLUETOOTH_RESULT";
	
	public static final String INTENT_EXTRA_BT_STATE = "EXTRA_BLUETOOTH_STATE";
	
	public static final String INTENT_EXTRA_BT_DEVICE_NAME = null;

	public static final String INTENT_EXTRA_BT_DEVICE_ADDRESS = null;

	public static final int MESSAGE_BLUETOOTH_DATA = 0;

	public static final int MESSAGE_LOCATION_DATA = 1;

	public static final int MESSAGE_SENSOR_DATA = 3;
	
	public static final String Listening ="Listening";
	
	public static final String ACTION_CONNECT_CHEST = "INTENT_ACTION_CONNECT_CHEST";
	
	public static final String KEY_ADDRESS = "KEY_ADDRESS";
	
	public static final int CHEST_SENSOR_DATA = 109;
	
    public static final String START_HOUR = "START_HOUR";
    
    public static final String END_HOUR = "END_HOUR";
    
    public static final String START_MIN = "START_MIN";
    
    public static final String END_MIN = "END_MIN";
    
    public static final String BED_TIME_INFO = "BED_TIME_INFO";
	public static final String BED_HOUR_INFO = "BED_HOUR_INFO";
	public static final String BED_MIN_INFO = "BED_MIN_INFO";
	public static final String BED_TIME = "BED_TIME";
    
    static String errMSG ="Please check your wifi or dataplan.\r\nThe phone is offline now.";
	
    static boolean IsScheduled = false;	
	
	boolean mExternalStorageAvailable = false;
	
	boolean mExternalStorageWriteable = false;
	
	//ADD ACTION STRING FOR DRINKING-FOLLOWUP
	public static final String ACTION_DRINK_FOLLOWUP = "INTENT_ACTION_DRINK_FOLLOWUP";
	
	public static final String ACTION_SCHEDULE_MORNING = "INTENT_ACTION_SCHEDULE_MORNING";
	
	public static Timer t1=new Timer();
	public static Timer t2=new Timer();
	public static Timer t3=new Timer();
	public static Timer t4=new Timer();
	public static Timer t5=new Timer();
	public static Timer t6=new Timer();
	
	//Ricky 2013/12/10
	//ADD TIMER FOR THE DRINK-FOLLOW TIMER
	public static Timer t7=new Timer();
	//Add count to stop the timer tasks
	private static int dCount = 1;
	//Flag for detecting whether there is still some drinkingfollowup being scheduled
	private static boolean dFlag = false;
	//static var for DrinkSurvey task;
	private static TimerTask drinkSurveyTask;
	//variable to detect whether the drinking follow-up survey is on the top 
	public static Boolean drinkUpFlag = false;
	//Add Four TimerTask var for drink-survey so later we could cancel the task instead of canceling the timer
    public static TimerTask alarmTask1;
    public static TimerTask alarmTask2;
    public static TimerTask alarmTask3;
    public static TimerTask alarmTask4;
    //Add Four TimerTask var for ramdom survey so later we could cancel the task instead of canceling the timer
    public static TimerTask rTask1;
    public static TimerTask rTask2;
    public static TimerTask rTask3;
    public static TimerTask rTask4;
    public static TimerTask rTask5;
    public static TimerTask rTask6;
    //Add one Timer
    public static Timer alarmTimer = new Timer();
		
	/*
	 * Bluetooth Variables
	 */
	Thread bluetoothThread;
	AffectivaRunnable affectivaRunnable;
	String bluetoothMacAddress = "default";
	BluetoothAdapter mBluetoothAdapter;
	
	/*
	 * Worker Threads
	 */
	Thread httpPostThread;
	Thread FileWriterThread;
	HttpPostThread httpPostRunnable;
	Runnable fileWriterRunnable;
	InternalSensor Accelerometer;
	InternalSensor LightSensor;
	InternalSensor Pressure;
	EquivitalRunnable equivitalThread;
	Notification mainServiceNotification;
	public static final int SERVICE_NOTIFICATION_ID = 1;
	
	
	private static SemDevice device;

	
	
	//Variables for storing chest sensor values
	
	
	
	ActivityRecognitionScan activityRecognition;
	DetectionRemover mDetectionRemover;
	public static int currentUserActivity=9;
	public static boolean IsRetrievingUpdates=false;
	LocationClient mLocationClient;
	
	
		
	private SoundPool mSoundPool;
	private int SOUND1=1;
	private int SOUND2=2;
	private HashMap<Integer, Integer> soundsMap;
	/*public StartSound ss;
	public StartSound2 ss2;
	public static StartSound mSound1;
	public static StartSound2 mSound2;*/
	//static Timer mTimer;
	int reconnectionAttempts=0;
	
	String wakeHour;
	String wakeMin;
	
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
			}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(){
		
		super.onCreate();
		Log.d(TAG,"Starting sensor service");
		mSoundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
        soundsMap = new HashMap<Integer, Integer>();
        soundsMap.put(SOUND1, mSoundPool.load(this, R.raw.bodysensor_alarm, 1));
        soundsMap.put(SOUND2, mSoundPool.load(this, R.raw.voice_notification, 1));       
		
		serviceContext = this;
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
		bluetoothMacAddress=mBluetoothAdapter.getAddress();
		mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		
		//Get location manager
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		activityRecognition=new ActivityRecognitionScan(getApplicationContext());
		activityRecognition.startActivityRecognitionScan();
		
		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		
		serviceWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorServiceLock");
		serviceWakeLock.acquire();
		
		
		
		//Initialize start time
		stime = System.currentTimeMillis();
		
        //Setup calendar object
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(stime);
		
		/*
		 * Setup notification manager
		 */

		notification = new Notification(R.drawable.icon2,"Recorded",System.currentTimeMillis());
		notification.defaults=0; 
		notification.flags|=Notification.FLAG_ONGOING_EVENT;
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		
		
		Intent notifyIntent = new Intent(Intent.ACTION_MAIN);
		notifyIntent.setClass(this, MainActivity.class);
		
		/*
		 * Display notification that service has started
		 */
        notification.tickerText="Sensor Service Running";
        PendingIntent contentIntent = PendingIntent.getActivity(SensorService.this, 0,
                notifyIntent, Notification.FLAG_ONGOING_EVENT);
        notification.setLatestEventInfo(SensorService.this, getString(R.string.app_name),
        		"Recording service started at: "+cal.getTime().toString(), contentIntent);
        
        notificationManager.notify(SensorService.SERVICE_NOTIFICATION_ID, notification);
        
       // locationControl = new LocationControl(this, mLocationManager, 1000 * 60, 200, 5000);	
	   
		IntentFilter activityResultFilter = 
				new IntentFilter(XMLSurveyActivity.INTENT_ACTION_SURVEY_RESULTS);
		SensorService.this.registerReceiver(alarmReceiver, activityResultFilter);

		IntentFilter sensorDataFilter = 
				new IntentFilter(SensorService.ACTION_SENSOR_DATA);
		SensorService.this.registerReceiver(alarmReceiver, sensorDataFilter);
		Log.d(TAG,"Sensor service created.");
	
		try {
			prepareIO();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		prepareAlarms();
		
		Intent startSensors=new Intent(SensorService.ACTION_START_SENSORS);
		this.sendBroadcast(startSensors);
		
		
		Intent scheduleCheckConnection = new Intent(SensorService.ACTION_SCHEDULE_CHECK);
		scheduleCheck = PendingIntent.getBroadcast(serviceContext, 0, scheduleCheckConnection , 0);
		mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+1000*60*5,1000*60*5,scheduleCheck);
		mLocationClient = new LocationClient(this, this, this);
		
		/* 
		 * @author Ricky 
		 * 2/11/14 start the random survey automatically
		 * 2/12 start random survey just after the service is triggered; 
		 * old design(reading the morning survey time as the beginning time) is commented.
		 */
		
		//END TIME 23:59
		int EndHour=23;
		int EndMin=59;
		
		//Compare Time part
		Calendar c = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
		String currentTime=dateFormat.format(c.getTime());
		String []cTime=currentTime.split(":");
		int StartHour=Integer.parseInt(cTime[0]);
		int StartMin=Integer.parseInt(cTime[1]);
		if (((EndHour-StartHour)*60+(EndMin-StartMin))<=60){
			Toast.makeText(getApplicationContext(),"Difference between Start and End Time must be at least one hour. Random Survey is canceled",Toast.LENGTH_LONG).show();
		}
		else {
			//Schedule part
			int Interval=(((EndHour-StartHour)*60)+(EndMin-StartMin))/6;
			int delay=Interval/2;
			int Increment=Interval+delay;
			int TriggerInterval=Interval-delay;
			Log.d(TAG,String.valueOf(Interval));
			
			Date dt1=new Date();				
			dt1.setHours(StartHour);
			dt1.setMinutes(StartMin+delay);
			Date dt2=new Date();
			dt2.setHours(StartHour);
			dt2.setMinutes(StartMin+Increment);				
			Date dt3=new Date();
			dt3.setHours(StartHour);
			dt3.setMinutes(StartMin+Increment+Interval);
			Date dt4=new Date();
			dt4.setHours(StartHour);
			dt4.setMinutes(StartMin+Increment+(Interval*2));
			Date dt5=new Date();
			dt5.setHours(StartHour);
			dt5.setMinutes(StartMin+Increment+(Interval*3));
			Date dt6=new Date();
			dt6.setHours(StartHour);
			dt6.setMinutes(StartMin+Increment+(Interval*4));
			rTask1 = new ScheduleSurvey(TriggerInterval);
			rTask2 = new ScheduleSurvey(TriggerInterval);
			rTask3 = new ScheduleSurvey(TriggerInterval);
			rTask4 = new ScheduleSurvey(TriggerInterval);
			rTask5 = new ScheduleSurvey(TriggerInterval);
			rTask6 = new ScheduleSurvey(TriggerInterval);				
			t1.schedule(rTask1,dt1);	
			t2.schedule(rTask2,dt2);
			t3.schedule(rTask3,dt3);
			t4.schedule(rTask4,dt4);
			t5.schedule(rTask5,dt5);
			t6.schedule(rTask6,dt6);
			setStatus(true);
			//End of Random Survey Schedule
			
			//Get Time for the morning trigger
			SharedPreferences bedTime = this.getSharedPreferences(BED_TIME, MODE_PRIVATE);
			wakeHour = bedTime.getString(BED_HOUR_INFO, "none");
			wakeMin = bedTime.getString(BED_MIN_INFO, "none");
			if (wakeHour.equals("none")||wakeMin.equals("none")){
				setMorningSurveyAlarm(11, 59);
			}
			
		}
	}
	
	
	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		// TODO Auto-generated method stub	
		mLocationClient.connect();
		return START_NOT_STICKY;
	}



	BroadcastReceiver checkRequestReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			Log.d(TAG, "Check Request Recieved");
			//int state=SemBluetoothConnection.getState();			
			if(action.equals(SensorService.ACTION_SCHEDULE_CHECK)){	
				
				Runtime info = Runtime.getRuntime();
			    long freeSize = info.freeMemory();
		        long totalSize= info.totalMemory();		        
		        long usedSize = totalSize - freeSize;
		        double temp=freeSize/(double)totalSize;
		        DecimalFormat percentFormat= new DecimalFormat("#.#%");
		        Calendar c=Calendar.getInstance();
				SimpleDateFormat curFormater = new SimpleDateFormat("MMMMM_dd"); 
				String dateObj =curFormater.format(c.getTime()); 		
				String file_name="MemoryUsage_"+dateObj+".txt";
				Calendar cal=Calendar.getInstance();
				cal.setTimeZone(TimeZone.getTimeZone("US/Central"));			
				
		        File f = new File(BASE_PATH,file_name);		
				String dataToWrite = String.valueOf(cal.getTime())+","+String.valueOf(usedSize/1024)+","+String.valueOf(totalSize/1024)+","+String.valueOf(freeSize/1024)+","+String.valueOf(percentFormat.format(temp));
				
				if(f != null){
					try {
						writeToFile(f, dataToWrite);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			else if(action.equals(SensorService.ACTION_START_SENSORS))
			{
				sensorThread.run();
			}
		}
		
	};
        
	private Runnable sensorThread = new Runnable() {
	    public void run() {
	    	Accelerometer=new InternalSensor(mSensorManager,Sensor.TYPE_ACCELEROMETER,SensorManager.SENSOR_DELAY_NORMAL,bluetoothMacAddress);
			Accelerometer.run();
			LightSensor=new InternalSensor(mSensorManager,Sensor.TYPE_LIGHT,SensorManager.SENSOR_DELAY_NORMAL,bluetoothMacAddress);
			LightSensor.run();
	    }
	};
	
	BroadcastReceiver soundRequestReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if(action.equals(SensorService.ACTION_START_SOUND)){
				Log.d(TAG,"Task Scheduled to run Sound Effects");
				startSound();				
			}
			else if(action.equals(SensorService.ACTION_TRIGGER_SOUND))
			{
				playSound(1,1.0f);
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		        v.vibrate(1000);   
			}
			else if(action.equals(SensorService.ACTION_TRIGGER_SOUND2))
			{
				playSound(2,1.0f);				 
			}

		}
		
	};
	
		
	
	 public void startSound()
	{  		 
	    
		 Intent scheduleTriggerSound = new Intent(SensorService.ACTION_TRIGGER_SOUND);
		 Intent scheduleTriggerSound2 = new Intent(SensorService.ACTION_TRIGGER_SOUND2);
		 triggerSound = PendingIntent.getBroadcast(serviceContext, 0, scheduleTriggerSound , 0);
		 triggerSound2 = PendingIntent.getBroadcast(serviceContext, 0, scheduleTriggerSound2 , 0);		 
		 mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime()+1000 ,triggerSound);	
		 mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime()+1000*20 ,triggerSound2);
		
	}
	
	 public class ConnectSensor extends TimerTask
	 {
		 String mAddress;
		 public ConnectSensor(String Mac_Address)
		 {
			 mAddress=Mac_Address;
			 
		 }
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Intent connectChest = new Intent(SensorService.ACTION_CONNECT_CHEST);	
			connectChest.putExtra(SensorService.KEY_ADDRESS,mAddress);
			serviceContext.sendBroadcast(connectChest);
		}
		 
		  
	 }
	
	public void playSound(int sound, float fSpeed) {
        AudioManager mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        float streamVolumeCurrent = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);        
        float volume = streamVolumeCurrent;
        mSoundPool.play(soundsMap.get(sound), volume, volume, 1, 0, fSpeed);  
       
       }


	private void prepareAlarms(){
		IntentFilter locationSchedulerFilter = new IntentFilter(ACTION_SCHEDULE_LOCATION);
		IntentFilter locationInterruptSchedulerFilter = new IntentFilter(ACTION_STOP_LOCATIONCONTROL);
		//IntentFilter surveyScheduleFilter = new IntentFilter(ACTION_SCHEDULE_SURVEY);
		IntentFilter surveyTest = new IntentFilter("ACTION_SURVEY_TEST");
		
		//ADD INTENTFILTER FOR DRINKING-FOLLOWUP
		IntentFilter followUpFilter = new IntentFilter(ACTION_DRINK_FOLLOWUP);
		IntentFilter MorningFilter = new IntentFilter(ACTION_SCHEDULE_MORNING);

	/*	Intent scheduleLocationIntent = new Intent(SensorService.ACTION_SCHEDULE_LOCATION);
		scheduleLocation = PendingIntent.getBroadcast(
				serviceContext, 0, scheduleLocationIntent, 0);
		mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + 10000, 1000 * 60 * 5, scheduleLocation);*/
		
		IntentFilter soundRequest=new IntentFilter(ACTION_START_SOUND);
		IntentFilter checkRequest=new IntentFilter(ACTION_SCHEDULE_CHECK);
		IntentFilter startSensors=new IntentFilter(ACTION_START_SENSORS);
		IntentFilter locationFoundFilter = new IntentFilter(LocationControl.INTENT_ACTION_LOCATION);
		IntentFilter sound1=new IntentFilter(ACTION_TRIGGER_SOUND);
		IntentFilter sound2=new IntentFilter(ACTION_TRIGGER_SOUND2);
		SensorService.this.registerReceiver(alarmReceiver, locationFoundFilter);
		SensorService.this.registerReceiver(alarmReceiver, locationSchedulerFilter);		
		SensorService.this.registerReceiver(alarmReceiver, locationInterruptSchedulerFilter);
		//SensorService.this.registerReceiver(alarmReceiver, surveyScheduleFilter);
		SensorService.this.registerReceiver(alarmReceiver, surveyTest);
		SensorService.this.registerReceiver(alarmReceiver, MorningFilter);
		//Register the drink-followup to the alarmReceiver
		SensorService.this.registerReceiver(alarmReceiver, followUpFilter);
		SensorService.this.registerReceiver(soundRequestReceiver,soundRequest);
		SensorService.this.registerReceiver(soundRequestReceiver,sound1);
		SensorService.this.registerReceiver(soundRequestReceiver,sound2);
		SensorService.this.registerReceiver(checkRequestReceiver,checkRequest);
		SensorService.this.registerReceiver(checkRequestReceiver,startSensors);
		IntentFilter chestSensorData = new IntentFilter(ACTION_CONNECT_CHEST);
		SensorService.this.registerReceiver(chestSensorReceiver,chestSensorData);
		}
	
	
	//Function to trigger random survey
	private class ScheduleSurvey extends TimerTask
	{
		int TriggerInterval;
		public ScheduleSurvey(int Time)
		{
			TriggerInterval=Time;			
		}

		@Override
		public void run() {					
		// TODO Auto-generated method stub
			Log.d("TAG",drinkUpFlag.toString());
		  if (drinkUpFlag==false){
			  Random rand=new Random();
			  int TriggerTime=rand.nextInt(TriggerInterval)+1;		 
			  Intent i = new Intent(serviceContext, SurveyPinCheck.class);
			  i.putExtra("survey_name", "RANDOM_ASSESSMENT");
			  i.putExtra("survey_file", "RandomAssessmentParcel.xml");	
			  scheduleSurvey = PendingIntent.getActivity(SensorService.this, 0,
					                i, Intent.FLAG_ACTIVITY_NEW_TASK);
			  mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
						SystemClock.elapsedRealtime()+1000*60*TriggerTime , scheduleSurvey);
		  }
		  else {
			  this.cancel();
			  Log.d(TAG, "Random Survey Canceled due to the drinkSurvey");
		  }
		}	
	}
	
	//Function to trigger drinking-followup survey
	private class DrinkSurvey extends TimerTask
	{
		int TriggerTime;
		public DrinkSurvey()
		{
			//TriggerTime=30*60*1000;
			//Test
			TriggerTime = 3000;
		}

		@Override
		public void run() {					
		// TODO Auto-generated method stub
			//if dFlag is true, it means the drinkingfollow Task is triggered.
			//dFlag = true;
			if ((dCount<=3)){				
				Intent i = new Intent(serviceContext, SurveyPinCheck.class);
				i.putExtra("survey_name", "DRINKING_FOLLOWUP");
				i.putExtra("survey_file", "DrinkingFollowup.xml");	
				drinkfollowupSurvey = PendingIntent.getActivity(SensorService.this, 0,
					                i, Intent.FLAG_ACTIVITY_NEW_TASK);
				mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
						SystemClock.elapsedRealtime()+TriggerTime , drinkfollowupSurvey);
				dCount++;
			}
			else {
				dCount = 1;
				this.cancel();
				//reset the dFlag 
				dFlag = false;
			}
		}	
	}
	
	/*
	 * Prepare IO that will be used for different recording tasks
	 */
	private void prepareIO() throws IOException{
		surveyFiles = new HashMap<String, String>();
		
		//If the base directory doesn't exist, create it
		File DIR = new File(BASE_PATH);
		if(!DIR.exists())
			DIR.mkdir();
		
		Log.d(TAG,"IO Prepared");
	}

	/*
	 * Unregister battery receiver and make sure the sensors are done
	 * before destroying the service
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy(){
	
		
		File f = new File(BASE_PATH,"SensorServiceEvents.txt");
		Calendar cal=Calendar.getInstance();
		try {
			writeToFile(f,"Destroyed at "+String.valueOf(cal.getTime()));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}			
		
		
		if(affectivaRunnable != null){
			affectivaRunnable.stop();
			try {
				bluetoothThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		//Ricky 1/28/2014 deal with equivitalThread stopping problem
		if(equivitalThread != null){
			equivitalThread.stop();
		}
		
		notificationManager.cancel(SensorService.SERVICE_NOTIFICATION_ID);
		
		SensorService.this.unregisterReceiver(alarmReceiver);		
		SensorService.this.unregisterReceiver(chestSensorReceiver);
		SensorService.this.unregisterReceiver(soundRequestReceiver);
		SensorService.this.unregisterReceiver(checkRequestReceiver);
		
		//If canceled, it will have some problems. Need to be handled later.
		mAlarmManager.cancel(scheduleSurvey);
		mAlarmManager.cancel(drinkfollowupSurvey);
		//mAlarmManager.cancel(scheduleLocation);	
		mAlarmManager.cancel(scheduleCheck);
		mAlarmManager.cancel(triggerSound);
		mAlarmManager.cancel(triggerSound2);
		mLocationClient.disconnect();
		activityRecognition.stopActivityRecognitionScan();		
		Accelerometer.stop();
		LightSensor.stop();
		
		CancelTask(alarmTask1);
		CancelTask(alarmTask2);
		CancelTask(alarmTask3);
		CancelTask(alarmTask4);
		CancelTask(drinkSurveyTask);
		CancelTask(rTask1);
		CancelTask(rTask2);
		CancelTask(rTask3);
		CancelTask(rTask4);
		CancelTask(rTask5);
		CancelTask(rTask6);
		/*
		 * If we try to cancel the timer, when we reuse the timer
		 * the system will show error msg
		CancelTimers(t1);
		CancelTimers(t2);
		CancelTimers(t3);
		CancelTimers(t4);
		CancelTimers(t5);
		CancelTimers(t6);
		CancelTimers(t7);
		*/		
		//CancelTimers(alarmTimer);
		PurgeTimers(t1);
		PurgeTimers(t2);
		PurgeTimers(t3);
		PurgeTimers(t4);
		PurgeTimers(t5);
		PurgeTimers(t6);
		PurgeTimers(t7);
		PurgeTimers(alarmTimer);
		setStatus(false);
		
		serviceWakeLock.release();
				
		Log.d(TAG,"Service Stopped.");
		
		super.onDestroy();
		if(device!=null){
		device.stop();
		}
	}
		
	
	
	BroadcastReceiver alarmReceiver = new BroadcastReceiver() {
		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
		  if(action.equals(SensorService.ACTION_STOP_LOCATIONCONTROL)){
				Log.d(TAG,"Stoping Location Upates");
				}
			else if(action.equals(SensorService.ACTION_SCHEDULE_LOCATION)){
				Log.d(TAG,"Received alarm event - schedule location");
				//locationControl.startRecording();				
				currentUserActivity=intent.getIntExtra("activity",9);
				Location mCurrentLocation=mLocationClient.getLastLocation();
				writeLocationToFile(mCurrentLocation);				
			}
		  	//Ricky 2/11/14
			/*
			else if(action.equals(SensorService.ACTION_SCHEDULE_SURVEY))
			{
				Log.d(TAG,"Received alarm event - schedule survey");								
				int StartHour=intent.getIntExtra(START_HOUR,0);
				int EndHour=intent.getIntExtra(END_HOUR,0);
				int StartMin=intent.getIntExtra(START_MIN,0);
				int EndMin=intent.getIntExtra(END_MIN,0);
				int Interval=(((EndHour-StartHour)*60)+(EndMin-StartMin))/6;
				int delay=Interval/2;
				int Increment=Interval+delay;
				int TriggerInterval=Interval-delay;
				Log.d(TAG,String.valueOf(Interval));
				
				Date dt1=new Date();				
				dt1.setHours(StartHour);
				dt1.setMinutes(StartMin+delay);
				Date dt2=new Date();
				dt2.setHours(StartHour);
				dt2.setMinutes(StartMin+Increment);				
				Date dt3=new Date();
				dt3.setHours(StartHour);
				dt3.setMinutes(StartMin+Increment+Interval);
				Date dt4=new Date();
				dt4.setHours(StartHour);
				dt4.setMinutes(StartMin+Increment+(Interval*2));
				Date dt5=new Date();
				dt5.setHours(StartHour);
				dt5.setMinutes(StartMin+Increment+(Interval*3));
				Date dt6=new Date();
				dt6.setHours(StartHour);
				dt6.setMinutes(StartMin+Increment+(Interval*4));
				rTask1 = new ScheduleSurvey(TriggerInterval);
				rTask2 = new ScheduleSurvey(TriggerInterval);
				rTask3 = new ScheduleSurvey(TriggerInterval);
				rTask4 = new ScheduleSurvey(TriggerInterval);
				rTask5 = new ScheduleSurvey(TriggerInterval);
				rTask6 = new ScheduleSurvey(TriggerInterval);				
				t1.schedule(rTask1,dt1);	
				t2.schedule(rTask2,dt2);
				t3.schedule(rTask3,dt3);
				t4.schedule(rTask4,dt4);
				t5.schedule(rTask5,dt5);
				t6.schedule(rTask6,dt6);
				setStatus(true);
			}
			*/
			else if(action.equals(SensorService.ACTION_SCHEDULE_MORNING))
			{
				if (wakeHour.equals("none")||wakeMin.equals("none")){
					iWakeHour = 11;
					iWakeMin = 59;
				} else {
					iWakeHour = Integer.parseInt(wakeHour);
					iWakeMin = Integer.parseInt(wakeMin);
				}
				bAlarmManager.cancel(morningReport);
				bAlarmManager.cancel(morningWakeUp);
				setMorningSurveyAlarm(iWakeHour,iWakeMin);
			}
		  	//ADD THE PROCESSING AFTER THE RECEIVER RECEIVE THE FOLLOWUP MSG
			else if(action.equals(SensorService.ACTION_DRINK_FOLLOWUP)){
				Log.d(TAG,"Received alarm event - schedule survey");
				//If timers already exists, cancel the current schedule tasks.
				//PurgeDrinkTimers();
				//trigger time is 30min 
				//long dIncrement = 30*60*1000;
				//test				
				if (dFlag == true){
					drinkSurveyTask.cancel();
					dFlag = false;
					dCount = 1; 
				}
				long dIncrement = 30*60*1000;
				Date dt7 = new Date(); 
				dt7.setMinutes(dt7.getMinutes()+30);
				drinkSurveyTask = new DrinkSurvey();
				t7.schedule(drinkSurveyTask,dt7,dIncrement);
				dFlag = true;
			}
			
			else if(action.equals(LocationControl.INTENT_ACTION_LOCATION)){
				Log.d(TAG,"Received alarm event - location found");
				Location foundLocation = 
						intent.getParcelableExtra(LocationControl.LOCATION_INTENT_KEY);

				if(foundLocation != null){
					HashMap<String, String> locationMap = 
							new HashMap<String, String>();
					
					locationMap.put("accuracy", foundLocation.getAccuracy()+"");
					
					locationMap.put("longi", foundLocation.getLongitude()+"");
					
					locationMap.put("lat", foundLocation.getLatitude()+"");			
					
					locationMap.put("source", foundLocation.getProvider());	
					
					writeLocationToFile(foundLocation);
					ActivityRecognitionService.IsRetrievingUpdates=false;
					
				}
			}
			else if (action.equals(XMLSurveyActivity.INTENT_ACTION_SURVEY_RESULTS)){
				Log.d(TAG,"Got survey results");
				Calendar cal = Calendar.getInstance();
				
				cal.setTimeInMillis(System.currentTimeMillis());
				
				@SuppressWarnings("unchecked")
				HashMap<String, List<String>> results = 
						(HashMap<String, List<String>>) intent.getSerializableExtra(XMLSurveyActivity.INTENT_EXTRA_SURVEY_RESULTS);
				String surveyName = 
						intent.getStringExtra(XMLSurveyActivity.INTENT_EXTRA_SURVEY_NAME);
				
				try {
					writeSurveyToFile(surveyName, results, intent.getLongExtra(XMLSurveyActivity.INTENT_EXTRA_COMPLETION_TIME,0L));
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG,"ERROR: Failed to write survey to file!");
				}
				Log.d(TAG,"Done writing file");
				
			}
		}
	};
	public static void CancelTask(TimerTask tTask){
		if  (tTask!=null)
		tTask.cancel();
	}
	public static void CancelTimers(Timer t)
	{
		//if(t1!=null&&t2!=null&&t3!=null&&t4!=null&&t5!=null&&t6!=null&&mTimer!=null)
		if(t!=null)
		{
		t.cancel();
		t.purge();	
		//mTimer.cancel();
		}
	}
	public static void PurgeTimers(Timer t)
	{
		//if(t1!=null&&t2!=null&&t3!=null&&t4!=null&&t5!=null&&t6!=null&&mTimer!=null)
		if(t!=null)
		{
		t.purge();	
		//mTimer.cancel();
		}
	}
	
	public static void setStatus(boolean value)
	{
		IsScheduled = value;		
	}
	
	
	public static boolean getStatus()
	{		
		return IsScheduled;
	}
	
	protected void writeLocationToFile(Location l){
		
		String toWrite;
		Calendar cl=Calendar.getInstance();
		SimpleDateFormat curFormater = new SimpleDateFormat("MMMMM_dd"); 
		String dateObj =curFormater.format(cl.getTime());
		File f = new File(BASE_PATH,"locations."+bluetoothMacAddress+"."+dateObj+".txt");
		
		Calendar cal=Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("US/Central"));	
		toWrite = String.valueOf(cal.getTime())+","+
			l.getLatitude()+","+l.getLongitude()+","+
			l.getAccuracy()+","+l.getProvider()+","+getNameFromType(currentUserActivity);
		if(f != null){
			try {
				writeToFile(f, toWrite);
				//sendDatatoServer("locations."+bluetoothMacAddress+"."+dateObj,toWrite);
				//Ricky
				TransmitData transmitData=new TransmitData();
				transmitData.execute("locations."+bluetoothMacAddress+"."+dateObj,toWrite);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}
	
	private String getNameFromType(int activityType) {
        switch(activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.UNKNOWN:
                return "unknown";
            case DetectedActivity.TILTING:
                return "tilting";
                
        }
        return "unknown";
    }
	
	
	protected void writeSurveyToFile(String surveyName, 
			HashMap<String, List<String>> surveyData, long time) throws IOException{
		Calendar cl=Calendar.getInstance();
		SimpleDateFormat curFormater = new SimpleDateFormat("MMMMM_dd"); 
		String dateObj =curFormater.format(cl.getTime());
		File f = new File(BASE_PATH,surveyName+"."+bluetoothMacAddress+"."+dateObj+".txt");
		Log.d(TAG,"File: "+f.getName());
		
		StringBuilder sb = new StringBuilder(100);
		
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(time);
		
		List<String> sorted = new ArrayList<String>(surveyData.keySet());
		Collections.sort(sorted);
		sb.append(c.getTime().toString());
		
		sb.append(",");

		for(int i = 0; i < sorted.size(); i++){
			String key = sorted.get(i);
			List<String> data = surveyData.get(key);
			sb.append(key+":");
			if(data == null)
				sb.append("-1");
			else{
				for(int j = 0; j < data.size(); j++){
					sb.append(data.get(j));
					if(i != data.size()-1)sb.append("");
				}
			}
			if(i != sorted.size()-1) sb.append(",");
		}
		sb.append("\n");
		//sendDatatoServer(surveyName+"."+bluetoothMacAddress+"."+dateObj,sb.toString());
		//Ricky 2013/12/09
		TransmitData transmitData=new TransmitData();
		transmitData.execute(surveyName+"."+bluetoothMacAddress+"."+dateObj,sb.toString());
				writeToFile(f,sb.toString());
		writeToFile(f,sb.toString());
	}
	
	protected void writeToFile(File f, String toWrite) throws IOException{
		FileWriter fw = new FileWriter(f, true);
		fw.write(toWrite+'\n');		
        fw.flush();
		fw.close();
	}
	
//------------------------------------------Chest Sensor Code Starts From Here ------------------------------------------------------
	
	BroadcastReceiver chestSensorReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(SensorService.ACTION_CONNECT_CHEST)){
				Toast.makeText(getApplicationContext(),"Intent Received",Toast.LENGTH_LONG).show();
				String address=intent.getStringExtra(KEY_ADDRESS);
				String deviceName=intent.getStringExtra("KEY_DEVICE_NAME");
				equivitalThread=new EquivitalRunnable(address,deviceName,bluetoothMacAddress);
				equivitalThread.run();
				Calendar c=Calendar.getInstance();
				SimpleDateFormat curFormater = new SimpleDateFormat("MMMMM_dd"); 
				String dateObj =curFormater.format(c.getTime()); 		
				String file_name="Mac_Address"+dateObj+".txt";
		        File f = new File(BASE_PATH,file_name);		
				
				if(f != null){
					try {
						writeToFile(f, address);
					} catch (IOException e) {
						e.printStackTrace();
					}
							
					
				}	
				
			    //connectToDevice(address);
			}
		}
		
	};

	

//---------------------------------------Code to upload data to the server----------------------------------------------//
	//Ricky 2013/12/09
	//Use AsyncTask to deal with not responding things
	private class TransmitData extends AsyncTask<String,Void, Boolean>
	{

		@Override
		protected Boolean doInBackground(String... strings) {
			// TODO Auto-generated method stub
			 String fileName=strings[0];
	         String dataToSend=strings[1];
	         if(checkDataConnectivity())
	 		{
	         //HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Crt/writeArrayToFile.php");
	         HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Test/writeArrayToFile.php");
	         List<NameValuePair> params = new ArrayList<NameValuePair>();
	         //file_name 
	         params.add(new BasicNameValuePair("file_name",fileName));        
	         //data                       
	         params.add(new BasicNameValuePair("data",dataToSend));
	         try {
	         	        	
	             request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
	             HttpResponse response = new DefaultHttpClient().execute(request);
	             if(response.getStatusLine().getStatusCode() == 200){
	                 String result = EntityUtils.toString(response.getEntity());
	                 Log.d("Sensor Data Point Info",result);                
	                // Log.d("Wrist Sensor Data Point Info","Data Point Successfully Uploaded!");
	             }
	             return true;
	         } 
	         catch (Exception e) 
	         {	             
	             e.printStackTrace();
	             return false;
	         }
	 	  }
	     	
	     else 
	     {
	     	Log.d("Sensor Data Point Info","No Network Connection:Data Point was not uploaded");
	     	Toast.makeText(serviceContext, errMSG, Toast.LENGTH_LONG).show();
	     	return false;
	      } 
		    
		}
		
	}
	/*	
	public static void sendDatatoServer(String FileName,String DataToSend)
	{
		if (checkDataConnectivity())
    	{

		//HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Crt/writeArrayToFile.php");
		//new test URL	
		HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Test/writeArrayToFile.php");
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        //file_name 
        params.add(new BasicNameValuePair("file_name",FileName));
        //data
        //Modified to deal with the additional line for location data
        //Ricky 2013/11/21
        //params.add(new BasicNameValuePair("data",DataToSend+"\n"));
        params.add(new BasicNameValuePair("data",DataToSend));

        
        try {
            request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse response = new DefaultHttpClient().execute(request);
            if(response.getStatusLine().getStatusCode() == 200){
                String result = EntityUtils.toString(response.getEntity());
                //Toast.makeText(serviceContext,"Data Point Successfully Uploaded", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            
           Toast.makeText(serviceContext,"Error during HTTP POST REQUEST",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    	}
    	else Toast.makeText(serviceContext, errMSG, Toast.LENGTH_LONG).show();
    }
	*/	
	
	 public static boolean checkDataConnectivity() {
	    	ConnectivityManager connectivity = (ConnectivityManager) serviceContext
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connectivity != null) {
				NetworkInfo[] info = connectivity.getAllNetworkInfo();
				if (info != null) {
					for (int i = 0; i < info.length; i++) {
						if (info[i].getState() == NetworkInfo.State.CONNECTED) {
							return true;
						}
					}
				}
			}
			return false;
		}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
	}

	/*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }
    
    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }
    
    private void setMorningSurveyAlarm(int h, int i){
    	Calendar tT = Calendar.getInstance();
    	/*
    	 *	If current time is before 3 A.M, it means the user maybe overnight. 
    	 *	Keep alarm triggered at the same day.
    	 *	Otherwise set trigger time to tomorrow.
    	 */
		if (tT.get(Calendar.HOUR_OF_DAY)>3) {
			tT.set(Calendar.DAY_OF_MONTH, tT.get(Calendar.DAY_OF_MONTH)+1);
		}
		tT.set(Calendar.HOUR_OF_DAY, h);
		tT.set(Calendar.MINUTE, i);
		
		bAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent mIntent = new Intent(SensorService.serviceContext, MainActivity.class);
		morningWakeUp = PendingIntent.getActivity(SensorService.serviceContext, 0,
				mIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		bAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
				tT.getTimeInMillis() ,86400000, morningWakeUp);
		Intent mRIntent = new Intent(SensorService.serviceContext, SurveyPinCheck.class);
		mRIntent.putExtra("survey_name", "MORNING_REPORT");
		mRIntent.putExtra("survey_file", "MorningReportParcel.xml");
		morningReport = PendingIntent.getActivity(SensorService.serviceContext, 0, mRIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		//trigger morning report 60 seconds later than MainActivity is restarted by bAlarmManager 
		bAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
				tT.getTimeInMillis()+1000*60,86400000, morningReport);
    }
 }

