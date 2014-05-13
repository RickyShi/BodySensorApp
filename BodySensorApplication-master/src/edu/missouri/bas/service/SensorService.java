package edu.missouri.bas.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
//Ricky 2013/12/09
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.equivital.sdk.decoder.SemDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationClient;

import edu.missouri.bas.MainActivity;
import edu.missouri.bas.R;
import edu.missouri.bas.activities.AdminManageActivity;
import edu.missouri.bas.bluetooth.affectiva.AffectivaRunnable;
import edu.missouri.bas.bluetooth.equivital.EquivitalRunnable;
import edu.missouri.bas.datacollection.InternalSensor;
import edu.missouri.bas.service.modules.location.ActivityRecognitionScan;
import edu.missouri.bas.service.modules.location.DetectionRemover;
import edu.missouri.bas.service.modules.location.LocationControl;
import edu.missouri.bas.service.modules.sensors.SensorControl;
import edu.missouri.bas.survey.SurveyPinCheck;
import edu.missouri.bas.survey.XMLSurveyActivity;


public class SensorService extends Service implements
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener
{ 
	public static boolean mIsRunning=false;
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
	//Ricky 2014/4/1
	private int randomSeq = -1;
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
	// Ricky 3/5/14
	private PendingIntent AccLightRestart;
	// Ricky 4/29/14
	private PendingIntent restartRandom;
	// Ricky 5/01/14
	private PendingIntent midPendingIntent;
	private int iWakeHour;
	private int iWakeMin;
	//private static PendingIntent scheduleLocation;
	private static PendingIntent scheduleCheck;
	private static PendingIntent activityRecogRestart;
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
	
	//Ricky 2014/4/15 for suspension
	public static final String INTENT_SUSPENSION = "INTENT_SUSPENSION";
	//break suspesion
	public static final String INTENT_BREAK_SUSPENSION = "INTENT_BREAK_SUSPENSION";
	
	public static final String INTENT_EXTRA_BT_DEVICE_NAME = null;

	public static final String INTENT_EXTRA_BT_DEVICE_ADDRESS = null;
	//Ricky 4/29/2014
	public static final String ACTION_RESTART_RANDOM_SURVEY = "ACTION_RESTART_RANDOM_SURVEY";
	//Ricky 5/01/2014
	public static final String ACTION_START_MIDNIGHT_CHECK = "ACTION_START_MIDNIGHT_CHECK";
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
	public static final String ACTION_SCHEDULE_MORNING_RESTART = "INTENT_ACTION_SCHEDULE_MORNING_RESTART";
	
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
	public static final String ACTION_ACTIVITY_RECOG_RESTART = "ACTION_ACTIVITY_RECOG_RESTART";
	
		
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
	
	private String wakeHour;
	private String wakeMin;
	//END TIME 23:59
	private int EndHour=23;
	private int EndMin=59;
	private SharedPreferences bedTime;
	private Editor bedEditor;
	int StartHour;
	int StartMin;
	//boolean MornReportIsDone; 
	public static boolean suspendFlag = false;
	//4/27/2014 Ricky for bluetooth
	public static boolean cancelBlueToothFlag = false;
	public static  ArrayAdapter<String> adapter;
	//Id and Password
	//2014/2/25
	private static String ID;
	private static String PWD;	
	
	public static String getPWD() {
		return PWD;
	}
	
	public static String getID() {
		return ID;
	}
	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	//Ricky 3/14 midNightCheck Timer Task Part
	public static boolean bedFlag = false;
	public static TimerTask midNightCheckTask;
	public static Timer midNightCheckTimer = new Timer();
	public class midNightCheck extends TimerTask {
    	@Override    	
    	public void run(){ 
	    	Log.d("wtest","midNightCheck");
	    	//---------------log--------------------
	    	File f = new File(BASE_PATH,"Trigger.txt");
			try {
				writeToFile(f,"MidNIght is Checking "+String.valueOf(Calendar.getInstance().getTime())+" Flag: "+String.valueOf(bedFlag));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//----------end log---------------------
	    	if (!bedFlag){
		    	//PARTIALLY STOP ALL SENSORS & RANDOM SURVEY
				stopPartialService();				
				//Schedule partially sensor restart
				setMorningSensorRestart(Integer.parseInt(wakeHour),Integer.parseInt(wakeMin));
				setMorningSurveyAlarm(Integer.parseInt(wakeHour),Integer.parseInt(wakeMin));
				Log.d("wtest",wakeHour+":"+wakeMin);
	    	}
	    	bedFlag = false;
	    	//Random Survey is all done
	    	//set RandomIsScheduled flag to false
	    	setStatus(false);
    	}
	}
	//end of midNightCheckTimer Task Part
	
	//Ricky 4/15 Suspension TimerTask Part
	public static Timer suspendTimer = new Timer();
	public static TimerTask suspendTimerTask;
	private class StopSuspension extends TimerTask {
    	@Override    	
    	public void run(){ 
    		Intent breakSuspension = new Intent(SensorService.INTENT_BREAK_SUSPENSION);
			getApplicationContext().sendBroadcast(breakSuspension);
    	}
    }
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(){
		
		super.onCreate();
		//Get ID and PWD
		SharedPreferences shp = getSharedPreferences("PINLOGIN", Context.MODE_PRIVATE);
	    ID = shp.getString(AdminManageActivity.ASID, "");
	    PWD = shp.getString(AdminManageActivity.ASPWD, "");
	    
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
		
		//Get Time for the morning trigger
		//Only trigger the morning-surveyAlarm when wakeTime is never set.
		bedTime = this.getSharedPreferences(BED_TIME, MODE_PRIVATE);
		wakeHour = bedTime.getString(BED_HOUR_INFO, "none");
		wakeMin = bedTime.getString(BED_MIN_INFO, "none");
		bAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		if (wakeHour.equals("none")||wakeMin.equals("none")){
			setMorningSurveyAlarm(11, 56);			
		}
		
		//MornReportIsDone = bedTime.getBoolean("MornReportDone", false);
		StartHour = bedTime.getInt("RandomSurveyStartHour", 11);
		StartMin = bedTime.getInt("RandomSurveyStartMin", 59);
		
		/** 
		 * @author Ricky 
		 * 3/4/14 
		 * First Check whether random survey is already triggered.
		 * set the random survey start time at 11:59 if morning survey is never set.
		 * Otherwise use old settings stored in the local storage.
		 * (for the case when App is revoked from unexpected crash)
		 */		
		//Schedule part
		if (!getStatus()){
			//if (!MornReportIsDone){
			//	triggerRandomSurvey(11,59);
			//} else {			
				triggerRandomSurvey(StartHour,StartMin);
			//}
		}
		
		//Ricky 3/14 Midnight Timer schedule part		
		Calendar cT = Calendar.getInstance();		
		Intent startmidNightCheck=new Intent(SensorService.ACTION_START_MIDNIGHT_CHECK);
		midPendingIntent = PendingIntent.getBroadcast(serviceContext, 0, startmidNightCheck, 0);
		//1min after the program open, send midnight check boradcast
		bAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP,cT.getTimeInMillis()+1000*60,86400000,midPendingIntent);
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
				Log.d(TAG,"Sensor Start");
			}
			else if(action.equals(SensorService.ACTION_ACTIVITY_RECOG_RESTART)){
				activityRecognition=new ActivityRecognitionScan(getApplicationContext());
				activityRecognition.startActivityRecognitionScan();
				//mLocationClient = new LocationClient(SensorService.this, SensorService.this, SensorService.this);
				mLocationClient.connect();
				Log.d(TAG,"Location Start");
			}
		}
		
	};
        
	private Runnable sensorThread = new Runnable() {
	    public void run() {
	    	Accelerometer=new InternalSensor(mSensorManager,Sensor.TYPE_ACCELEROMETER,SensorManager.SENSOR_DELAY_NORMAL,ID);
			Accelerometer.run();
			LightSensor=new InternalSensor(mSensorManager,Sensor.TYPE_LIGHT,SensorManager.SENSOR_DELAY_NORMAL,ID);
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
		IntentFilter RestartMorningFilter = new IntentFilter(ACTION_SCHEDULE_MORNING_RESTART);
		IntentFilter RestartRandomSurveyFilter = new IntentFilter(ACTION_RESTART_RANDOM_SURVEY);

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
		IntentFilter activityRecognizationRequest =new IntentFilter(ACTION_ACTIVITY_RECOG_RESTART);
		//Ricky 2014/4/15 IntentFilter for suspension & break_suspension
		IntentFilter suspension =new IntentFilter(INTENT_SUSPENSION);
		IntentFilter breakSuspension =new IntentFilter(INTENT_BREAK_SUSPENSION);
		SensorService.this.registerReceiver(alarmReceiver, locationFoundFilter);
		SensorService.this.registerReceiver(alarmReceiver, locationSchedulerFilter);		
		SensorService.this.registerReceiver(alarmReceiver, locationInterruptSchedulerFilter);
		//SensorService.this.registerReceiver(alarmReceiver, surveyScheduleFilter);
		SensorService.this.registerReceiver(alarmReceiver, surveyTest);
		SensorService.this.registerReceiver(alarmReceiver, MorningFilter);
		SensorService.this.registerReceiver(alarmReceiver, RestartMorningFilter);
		//Register the drink-followup to the alarmReceiver
		SensorService.this.registerReceiver(alarmReceiver, followUpFilter);
		//Ricky 2014/4/15 register suspension in the alarmReceiver
		SensorService.this.registerReceiver(alarmReceiver, suspension);
		SensorService.this.registerReceiver(alarmReceiver, breakSuspension);
		SensorService.this.registerReceiver(soundRequestReceiver,soundRequest);
		SensorService.this.registerReceiver(soundRequestReceiver,sound1);
		SensorService.this.registerReceiver(soundRequestReceiver,sound2);
		SensorService.this.registerReceiver(checkRequestReceiver,checkRequest);
		SensorService.this.registerReceiver(checkRequestReceiver,startSensors);
		SensorService.this.registerReceiver(checkRequestReceiver,activityRecognizationRequest);
		IntentFilter chestSensorData = new IntentFilter(ACTION_CONNECT_CHEST);
		SensorService.this.registerReceiver(chestSensorReceiver,chestSensorData);
		//4/27 refresh button intentFilter
		IntentFilter blueToothRefresh=new IntentFilter(ACTION_GET_BLUETOOTH_STATE);
		SensorService.this.registerReceiver(alarmReceiver, blueToothRefresh);
		SensorService.this.registerReceiver(alarmReceiver, RestartRandomSurveyFilter);
		//5/01/2014 midNight check prepare
		IntentFilter midCheckFilter=new IntentFilter(ACTION_START_MIDNIGHT_CHECK);
		SensorService.this.registerReceiver(alarmReceiver, midCheckFilter);
		}
	
	
	//Function to trigger random survey
	private class ScheduleSurvey extends TimerTask
	{
		int TriggerInterval;
		//Random Sequence
		int RandomID = 0;
		
		public ScheduleSurvey(int Time)
		{
			TriggerInterval=Time;			
		}
		
		public ScheduleSurvey(int Time, int ID)
		{
			TriggerInterval=Time;
			RandomID=ID;
		}

		@Override
		public void run() {					
		// TODO Auto-generated method stub
			Log.d(TAG,"drinkUpFlag: "+drinkUpFlag.toString());
			Log.d("wtest","random is running: "+RandomID);
		  if (drinkUpFlag==false){
			  Random rand=new Random();
			  int TriggerTime=rand.nextInt(TriggerInterval)+1;		 
			  Intent i = new Intent(serviceContext, SurveyPinCheck.class);
			  i.putExtra("survey_name", "RANDOM_ASSESSMENT");
			  i.putExtra("survey_file", "RandomAssessmentParcel.xml");
			  if (RandomID!=0)
				  i.putExtra("random_sequence", RandomID);
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
		if(Accelerometer != null){
			Accelerometer.stop();
		}
		if(LightSensor != null){
			LightSensor.stop();
		}
		
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
		//Ricky 3/14
		CancelTask(midNightCheckTask);
		
		/*
		 * If we try to cancel the timer, when we reuse the timer
		 * the system will show error MSG
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
		//Ricky 3/14
		PurgeTimers(midNightCheckTimer);
		setStatus(false);
		CancelTask(suspendTimerTask);
		PurgeTimers(suspendTimer);
		suspendFlag = false;
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
			else if(action.equals(SensorService.ACTION_RESTART_RANDOM_SURVEY))
			{
				triggerRandomSurvey(StartHour,StartMin);
			}
			else if(action.equals(SensorService.ACTION_SCHEDULE_MORNING))
			{	
				wakeHour = bedTime.getString(BED_HOUR_INFO, "none");
				wakeMin = bedTime.getString(BED_MIN_INFO, "none");
				if (wakeHour.equals("none")||wakeMin.equals("none")){
					iWakeHour = 11;
					iWakeMin = 59;
				} else {
					iWakeHour = Integer.parseInt(wakeHour);
					iWakeMin = Integer.parseInt(wakeMin);
				}
				bAlarmManager.cancel(morningReport);
				bAlarmManager.cancel(morningWakeUp);
				bAlarmManager.cancel(restartRandom);
				Calendar bRT = Calendar.getInstance();
				setMorningSurveyAlarm(iWakeHour,iWakeMin);
				if (bRT.get(Calendar.HOUR_OF_DAY)>=21){
					bAlarmManager.cancel(AccLightRestart);
					//PARTIALLY STOP ALL SENSORS & RANDOM SURVEY
					stopPartialService();				
					//Schedule partially sensor restart
					setMorningSensorRestart(iWakeHour,iWakeMin);
					SensorService.bedFlag = true;
				}
			}
			else if(action.equals(SensorService.ACTION_SCHEDULE_MORNING_RESTART))
			{
				if (wakeHour.equals("none")||wakeMin.equals("none")){
					iWakeHour = 11;
					iWakeMin = 59;
				} else {
					iWakeHour = Integer.parseInt(wakeHour);
					iWakeMin = Integer.parseInt(wakeMin);
				}
				
				setMorningSurveyAlarm(iWakeHour,iWakeMin);
				Log.d(TAG,"BOOT BROADCAST RECEIVED");
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
				//Ricky 4/1
				//dealing with random sequence
				if (surveyName.equalsIgnoreCase("RANDOM_ASSESSMENT")){
					randomSeq = intent.getIntExtra("random_sequence", 0);
					String rsID = String.valueOf(randomSeq);
					Calendar rsT = Calendar.getInstance();
					String rsDate = rsT.get(Calendar.DAY_OF_MONTH)+"/"+(rsT.get(Calendar.MONTH)+1)+"/"+rsT.get(Calendar.YEAR);
					String uID = String.valueOf(ID);
					CompletedSignal completedSignal = new CompletedSignal();
					completedSignal.execute(uID,rsDate,rsID);
				}				
				try {
					writeSurveyToFile(surveyName, results, intent.getLongExtra(XMLSurveyActivity.INTENT_EXTRA_COMPLETION_TIME,0L));
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG,"ERROR: Failed to write survey to file!");
				}
				Log.d(TAG,"Done writing file");
				/**
				 * @author Ricky 
				 * 2014/3/3
				 * 1st check whether survey is morning report
				 * then check whether mornReport was submitted today
				 * If not, do the following thing. Otherwise, do nothing. 
				 * 2nd store random survey start time
				 * 3rd call random survey functions
				 * 4th store morningReoprtIsDone flag to local file
				 */
				if (surveyName.equals("MORNING_REPORT")){
					//if (!MornReportIsDone){
					Log.d("wtest","Morning trigger time");
					Calendar c = Calendar.getInstance();
					SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
					String currentTime=dateFormat.format(c.getTime());
					String []cTime=currentTime.split(":");
					StartHour=Integer.parseInt(cTime[0]);
					StartMin=Integer.parseInt(cTime[1]);		
					if (((EndHour-StartHour)*60+(EndMin-StartMin))<=60){
						Toast.makeText(getApplicationContext(),"Difference between Start and End Time must be at least one hour. Random Survey is canceled",Toast.LENGTH_LONG).show();
					}
					else {
						//2nd part
						bedEditor = bedTime.edit();
						bedEditor.putInt("RandomSurveyStartHour", StartHour);
						bedEditor.putInt("RandomSurveyStartMin", StartMin);
						//bedEditor.putBoolean("MornReportDone", true);
						bedEditor.commit();
						//3rd part														
						triggerRandomSurvey(StartHour,StartMin);							
					}
					//}
				}
			}
			else if (action.equals(SensorService.INTENT_SUSPENSION)){
				adapter.remove("Suspension");
				adapter.add("Break Suspension");
    			suspendFlag = true;
    			//cancel random survey
    			mAlarmManager.cancel(scheduleSurvey);
    			CancelTask(rTask1);
    			CancelTask(rTask2);
    			CancelTask(rTask3);
    			CancelTask(rTask4);
    			CancelTask(rTask5);
    			CancelTask(rTask6);
    			PurgeTimers(t1);
    			PurgeTimers(t2);
    			PurgeTimers(t3);
    			PurgeTimers(t4);
    			PurgeTimers(t5);
    			PurgeTimers(t6);
    			//end
    			//add Timer Task to cancel break suspension
    			Date StopSuspendDate = new Date();
    			int SuspendH = intent.getIntExtra("H", 0);
    			int SuspendM = intent.getIntExtra("M", 0);
    			if (!(SuspendH == 0 && SuspendM == 0)){
    				StopSuspendDate.setHours(StopSuspendDate.getHours()+SuspendH);
    				StopSuspendDate.setMinutes(StopSuspendDate.getMinutes()+SuspendM);
	    			suspendTimerTask = new StopSuspension();
	    			suspendTimer.schedule(suspendTimerTask, StopSuspendDate);
    			}
    			Calendar nowT= Calendar.getInstance();
				TransmitData transmitData=new TransmitData();
				//danick
				try {
					transmitData.execute("Event."+ID,encryption(nowT.getTime().toString()+", Start Suspension"));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (action.equals(SensorService.INTENT_BREAK_SUSPENSION)){
				CancelTask(suspendTimerTask);
				PurgeTimers(suspendTimer);
				adapter.remove("Break Suspension");
				adapter.add("Suspension");
    			suspendFlag = false;
    			StartHour = bedTime.getInt("RandomSurveyStartHour", 11);
    			StartMin = bedTime.getInt("RandomSurveyStartMin", 59);
    			triggerRandomSurvey(StartHour,StartMin);
				Toast.makeText(getApplicationContext(),"Break SUSPENSION",Toast.LENGTH_LONG).show();
				Calendar nowT= Calendar.getInstance();
				TransmitData transmitData=new TransmitData();
				//danick
				try {
					transmitData.execute("Event."+ID,encryption(nowT.getTime().toString()+", Break Suspension"));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if (action.equals(SensorService.ACTION_GET_BLUETOOTH_STATE)){
				if(equivitalThread != null){
					equivitalThread.stop();
				}
			}
			else if (action.equals(SensorService.ACTION_START_MIDNIGHT_CHECK)){
				Date midNightT = new Date();
				midNightT.setHours(23);
				midNightT.setMinutes(58);
				//for testing
				//midNightT.setMinutes(midNightT.getMinutes()+1);
				CancelTask(midNightCheckTask);
				PurgeTimers(midNightCheckTimer);
				Log.d("wtest",wakeHour+":"+wakeMin+" midnight check is secheduled");
				midNightCheckTask = new midNightCheck();
				midNightCheckTimer.schedule(midNightCheckTask, midNightT);				
			}
		}
	};
	public void CancelTask(TimerTask tTask){
		if  (tTask!=null)
		tTask.cancel();
	}
	public void CancelTimers(Timer t)
	{
		if(t!=null)
		{
		t.cancel();
		t.purge();	
		}
	}
	public void PurgeTimers(Timer t)
	{
		if(t!=null)
		{
		t.purge();	
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
		File f = new File(BASE_PATH,"locations."+ID+"."+dateObj+".txt");
		
		Calendar cal=Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("US/Central"));	
		toWrite = String.valueOf(cal.getTime())+","+
			l.getLatitude()+","+l.getLongitude()+","+
			l.getAccuracy()+","+l.getProvider()+","+getNameFromType(currentUserActivity);
		
		//danick
		String toWriteArr = null;
		try {
			toWriteArr = encryption(toWrite);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(f != null){
			try {
				writeToFile(f, toWriteArr);
				//writeToFile(f, toWrite);
				//sendDatatoServer("locations."+bluetoothMacAddress+"."+dateObj,toWrite);
				//Ricky
				TransmitData transmitData=new TransmitData();
				transmitData.execute("locations."+ID+"."+dateObj,toWriteArr);
				//transmitData.execute("locations."+ID+"."+dateObj,toWrite);
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
		File f = new File(BASE_PATH,surveyName+"."+ID+"."+dateObj+".txt");
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
		//Ricky 2014/4/1
		//dealing with the random sequence
		if (surveyName.equalsIgnoreCase("RANDOM_ASSESSMENT")) {
			sb.append(",seq:"+randomSeq);
		}
		sb.append("\n");
		
		/************************************************************************
		 * 
		 * Chen 
		 * 
		 * Data encryption
		 * Stringbuilder sb -> String ensb
		 * 
		 * 
		 */
		String ensb = null;
		try {
			ensb = encryption(sb.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		//sendDatatoServer(surveyName+"."+bluetoothMacAddress+"."+dateObj,sb.toString());
		//Ricky 2013/12/09
		TransmitData transmitData=new TransmitData();
		transmitData.execute(surveyName+"."+ID+"."+dateObj,ensb);
		//transmitData.execute(surveyName+"."+ID+"."+dateObj,sb.toString());
//		File f2 = new File(BASE_PATH,surveyName+"."+bluetoothMacAddress+"."+dateObj+"enc.txt");
		writeToFileEnc(f,ensb);
/*        File fb = new File(BASE_PATH,"2enc.txt");
        writeToFile2b(fb,Base64.decode(ensb,Base64.DEFAULT));*/
		//transmitData.execute(surveyName+"."+bluetoothMacAddress+"."+dateObj,sb.tostring());
//		writeToFile(f,sb.toString());
	}
	
	
	//Chen
	private String encryption(String string) throws Exception {
		// TODO Auto-generated method stub
		
		//generate symmetric key
		KeyGenerator keygt = KeyGenerator.getInstance("AES");
		keygt.init(128);
		SecretKey symkey =keygt.generateKey(); 
		
		//get it encoded
		byte[] aes_ba = symkey.getEncoded();
		
		//create cipher
		SecretKeySpec skeySpec = new SecretKeySpec(aes_ba, "AES");  
        Cipher cipher = Cipher.getInstance("AES");  
        cipher.init(Cipher.ENCRYPT_MODE,skeySpec);
		
        //encryption
        byte [] EncSymbyteArray =cipher.doFinal(string.getBytes());
		
        //encrypt symKey with PublicKey
        Key pubKey = getPublicKey();
        
        //RSA cipher
        Cipher cipherAsm = Cipher.getInstance("RSA", "BC");
        cipherAsm.init(Cipher.ENCRYPT_MODE, pubKey);
        
        //RSA encryption
        byte [] asymEncsymKey = cipherAsm.doFinal(aes_ba);
        
//        File f3 = new File(BASE_PATH,"enc.txt");
//        File f3key = new File(BASE_PATH,"enckey.txt");
//        File f3file = new File(BASE_PATH,"encfile.txt");
//        writeToFile2(f3,f3key,f3file, asymEncsymKey, EncSymbyteArray);
        
        //byte != new String
        //return new String(byteMerger(asymEncsymKey, EncSymbyteArray));
        return Base64.encodeToString(byteMerger(asymEncsymKey, EncSymbyteArray),Base64.DEFAULT);
        
	}
	
//	protected void writeToFile2(File f,File fk,File ff, byte[] keyToWrite , byte[] fileToWrite) throws IOException{
//
//        FileOutputStream output = new FileOutputStream(f);
//        output.write(keyToWrite);
//        output.write(fileToWrite);
//        output.close();
//        
//        FileOutputStream output2 = new FileOutputStream(fk);
//        output2.write(keyToWrite);
//        output2.close();
//        FileOutputStream output3 = new FileOutputStream(ff);
//        output3.write(fileToWrite);
//        output3.close();
//
//	}
	
	protected void writeToFile2b(File f,byte[] keyToWrite) throws IOException{

        FileOutputStream output = new FileOutputStream(f);
        output.write(keyToWrite);
        output.close();


	}

	public static byte[] byteMerger(byte[] byte_1, byte[] byte_2){  
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];  
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);  
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);  
        return byte_3;  
    }  
	
	public  PublicKey getPublicKey() throws Exception {
		// TODO Auto-generated method stub
        InputStream is = getResources().openRawResource(R.raw.publickey);
		ObjectInputStream ois = new ObjectInputStream(is);

		BigInteger m = (BigInteger)ois.readObject();
		BigInteger e = (BigInteger)ois.readObject();
	    RSAPublicKeySpec keySpec = new RSAPublicKeySpec(m, e);
		
	   
	    KeyFactory fact = KeyFactory.getInstance("RSA", "BC");
	    PublicKey pubKey = fact.generatePublic(keySpec);
	    
		return pubKey; 
	}

	protected void writeToFile(File f, String toWrite) throws IOException{
		FileWriter fw = new FileWriter(f, true);
		fw.write(toWrite+'\n');		
        fw.flush();
		fw.close();
	}
	
	protected void writeToFileEnc(File f, String toWrite) throws IOException{
		FileWriter fw = new FileWriter(f, true);
		fw.write(toWrite);		
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
				equivitalThread=new EquivitalRunnable(address,deviceName,ID);
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
	         HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Crt/writeArrayToFileDec.php");
	         //HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Test/writeArrayToFile.php");
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
    /**
     * @author Ricky
     * @param h wakeHour
     * @param i wakeMin
     * Not using {@link #bAlarmManager.setRepeating} 
     * Instead, Call the {@link #bAlarmManager.set} function everyday.
     */
    private void setMorningSurveyAlarm(int h, int i){
    	Calendar tT = Calendar.getInstance();
    	//test
    	Calendar nowT = Calendar.getInstance();
    	File f = new File(BASE_PATH,"Trigger.txt");
		try {
			writeToFile(f,"Morning Alarm re-schedules at "+String.valueOf(tT.getTime()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	/**
    	 * @author Ricky
    	 *	1st
    	 *	If current time is in [0,3] A.M, it means the user maybe overnight. 
    	 *	Keep alarm triggered at the same day.
    	 *	Otherwise[21:00,23:59] set trigger time to tomorrow.
    	 *	2nd wakeUp App	
    	 *	3rd random survey
    	 *	4th set Morning Report/30 seconds delay
    	 */
		if (tT.get(Calendar.HOUR_OF_DAY)>=3) {
			tT.set(Calendar.DAY_OF_MONTH, tT.get(Calendar.DAY_OF_MONTH)+1);
		}
		tT.set(Calendar.HOUR_OF_DAY, h);
		tT.set(Calendar.MINUTE, i);
			
		Intent mIntent = new Intent(SensorService.serviceContext, MainActivity.class);
		morningWakeUp = PendingIntent.getActivity(SensorService.serviceContext, 0,
				mIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		bAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP,tT.getTimeInMillis()-1000*60 ,86400000, morningWakeUp);
		//bAlarmManager.set(AlarmManager.RTC_WAKEUP,tT.getTimeInMillis(),morningWakeUp);
		Intent mRIntent = new Intent(SensorService.serviceContext, SurveyPinCheck.class);
		mRIntent.putExtra("survey_name", "MORNING_REPORT_ALARM");
		mRIntent.putExtra("survey_file", "MorningReportParcel.xml");
		morningReport = PendingIntent.getActivity(SensorService.serviceContext, 0, mRIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
		//trigger morning report 30 seconds later than MainActivity is restarted by bAlarmManager 
		//bAlarmManager.set(AlarmManager.RTC_WAKEUP,tT.getTimeInMillis()+1000*30,morningReport);
		bAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP,tT.getTimeInMillis()+1000*30,86400000,morningReport);
		
		/**
		 * @author Ricky
		 * 4/29/2014  restart random survey
		 * currently restart random survey when the app is started.
		 * Maybe later could change it to made a decision at 12:00 P.M.
		 * Maybe in the broadcast receiver to schedule a timer task to do that based on the flag whether morning survey is done. 
		 */
		Intent fIntent = new Intent(SensorService.ACTION_RESTART_RANDOM_SURVEY);
		restartRandom = PendingIntent.getBroadcast(serviceContext, 0, fIntent, 0);
		bAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP,tT.getTimeInMillis()-1000*20,86400000,restartRandom);
		//end restart random survey		
		
		TransmitData transmitData=new TransmitData();
//		transmitData.execute("Trigger."+ID+"."+nowT.get(Calendar.MONTH)+"_"+nowT.get(Calendar.DAY_OF_MONTH),
//				nowT.getTime().toString()+",Schduleing Morning Survey which will be called at "+tT.get(Calendar.HOUR_OF_DAY)+":"+tT.get(Calendar.MINUTE));
		
		//danick
		try {
			transmitData.execute("Event."+ID,encryption(nowT.getTime().toString()+",Schduling Morning Survey which will be called at "+tT.get(Calendar.HOUR_OF_DAY)+":"+tT.get(Calendar.MINUTE)));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    /**
     * @author Ricky
     * @param h
     * @param i
     */
    private void setMorningSensorRestart(int h, int i){
    	Calendar tT = Calendar.getInstance();
    	//test
    	File f = new File(BASE_PATH,"Trigger.txt");
		try {
			writeToFile(f,"Sensor Service re-schedules at "+String.valueOf(tT.getTime()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	/**
    	 * @author Ricky
    	 *	4th start phone build-in sensor Collection part if they are null/30 seconds delay
    	 */
		if (tT.get(Calendar.HOUR_OF_DAY)>=3) {
			tT.set(Calendar.DAY_OF_MONTH, tT.get(Calendar.DAY_OF_MONTH)+1);
		}
		tT.set(Calendar.HOUR_OF_DAY, h);
		tT.set(Calendar.MINUTE, i);				
		//trigger Acc/Light Sensors 60 seconds later than MainActivity is restarted by bAlarmManager 
		Intent startACCLightSensors=new Intent(SensorService.ACTION_START_SENSORS);
		AccLightRestart = PendingIntent.getBroadcast(serviceContext, 0, startACCLightSensors, 0);
		bAlarmManager.set(AlarmManager.RTC_WAKEUP,tT.getTimeInMillis()+1000*60,AccLightRestart);
		//Test
		//Calendar testT = Calendar.getInstance();
		//bAlarmManager.set(AlarmManager.RTC_WAKEUP,testT.getTimeInMillis()+1000*20,AccLightRestart);		
		
		//trigger mLocationClient
		//Trigger ActivityReconization 70 seconds later than MainActivity is restarted by bAlarmManager 
		Intent activityRecogR =new Intent(SensorService.ACTION_ACTIVITY_RECOG_RESTART);
		activityRecogRestart = PendingIntent.getBroadcast(serviceContext, 0, activityRecogR , 0);
		bAlarmManager.set(AlarmManager.RTC_WAKEUP,tT.getTimeInMillis()+1000*70,activityRecogRestart);
		//bAlarmManager.set(AlarmManager.RTC_WAKEUP,testT.getTimeInMillis()+1000*30,activityRecogRestart);
    }
    /**
     * @author Ricky
     * @param startH
     * @param startM
     */
    @SuppressWarnings("deprecation")
	private void triggerRandomSurvey(int startH, int startM){
    	//if (!getStatus()){
    		//first cancel the old setting before apply the new settings.
    		mAlarmManager.cancel(scheduleSurvey);
    		CancelTask(rTask1);
    		CancelTask(rTask2);
    		CancelTask(rTask3);
    		CancelTask(rTask4);
    		CancelTask(rTask5);
    		CancelTask(rTask6);
    		PurgeTimers(t1);
    		PurgeTimers(t2);
    		PurgeTimers(t3);
    		PurgeTimers(t4);
    		PurgeTimers(t5);
    		PurgeTimers(t6);
    		//end of canceling part
    		
	    	int Interval=(((EndHour-startH)*60)+(EndMin-startM))/6;
			int delay=Interval/2;
			int Increment=Interval+delay;
			int TriggerInterval=Interval-delay;
			
			Date currentT = new Date();
			//test
			Calendar test = Calendar.getInstance();
	    	File f = new File(BASE_PATH,"Trigger.txt");
			try {
				writeToFile(f,"Random Survey re-schedules at "+String.valueOf(test.getTime().toString()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Date dt1=new Date();				
			dt1.setHours(startH);
			dt1.setMinutes(startM+delay);
			Date dt2=new Date();
			dt2.setHours(startH);
			dt2.setMinutes(startM+Increment);				
			Date dt3=new Date();
			dt3.setHours(startH);
			dt3.setMinutes(startM+Increment+Interval);
			Date dt4=new Date();
			dt4.setHours(startM);
			dt4.setMinutes(startM+Increment+(Interval*2));
			Date dt5=new Date();
			dt5.setHours(startH);
			dt5.setMinutes(startM+Increment+(Interval*3));
			Date dt6=new Date();
			dt6.setHours(startH);
			dt6.setMinutes(startM+Increment+(Interval*4));
			/*
			setRandomSchedule(t1,rTask1,dt1,currentT,TriggerInterval);
			setRandomSchedule(t2,rTask2,dt2,currentT,TriggerInterval);
			setRandomSchedule(t3,rTask3,dt3,currentT,TriggerInterval);
			setRandomSchedule(t4,rTask4,dt4,currentT,TriggerInterval);
			setRandomSchedule(t5,rTask5,dt5,currentT,TriggerInterval);
			setRandomSchedule(t6,rTask6,dt6,currentT,TriggerInterval);
			*/
			if (dt1.after(currentT)){
				rTask1 = new ScheduleSurvey(TriggerInterval,1);
	    		t1.schedule(rTask1,dt1);
	    		Log.d("wtest", "status: "+getStatus()+" rTask1 is scheduled.");
	    	}
			if (dt2.after(currentT)){
				rTask2 = new ScheduleSurvey(TriggerInterval,2);
	    		t2.schedule(rTask2,dt2);
	    	}
			if (dt3.after(currentT)){
				rTask3 = new ScheduleSurvey(TriggerInterval,3);
	    		t3.schedule(rTask3,dt3);
	    	}
			if (dt4.after(currentT)){
				rTask4 = new ScheduleSurvey(TriggerInterval,4);
	    		t4.schedule(rTask4,dt4);
	    	}
			if (dt5.after(currentT)){
				rTask5 = new ScheduleSurvey(TriggerInterval,5);
	    		t5.schedule(rTask5,dt5);
	    	}
			if (dt6.after(currentT)){
				rTask6 = new ScheduleSurvey(TriggerInterval,6);
	    		t6.schedule(rTask6,dt6);
	    		Log.d("wtest", "rTask6 is scheduled.");
	    	}
			setStatus(true);
			Log.d("wtest","status: "+getStatus()+" Random Survey is Scheduled");
    	//}
    }
    
    /**
     * @author Ricky
     * @param t Timer
     * @param rT TimerTask
     * @param dt TriggerTimeMin
     * @param currentT current Date
     * @param TriggerInterval triggerInterval Time
     */
    private void setRandomSchedule(Timer t, TimerTask rT, Date dt, Date currentT,int TriggerInterval){
    	if (dt.after(currentT)){
    		rT = new ScheduleSurvey(TriggerInterval);
    		t.schedule(rT,dt);
    	}
    }
    
    private void stopPartialService(){
    	File f = new File(BASE_PATH,"SensorServiceEvents.txt");
		Calendar cal=Calendar.getInstance();
		try {
			writeToFile(f,"Partially Destroyed at "+String.valueOf(cal.getTime()));
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
		
		if(equivitalThread != null){
			equivitalThread.stop();
			cancelBlueToothFlag = true;
		}
				
		//If canceled, it will have some problems. Need to be handled later.
		mAlarmManager.cancel(scheduleSurvey);
		//mAlarmManager.cancel(scheduleLocation);	
		mLocationClient.disconnect();
		activityRecognition.stopActivityRecognitionScan();
		
		if(Accelerometer != null){
			Accelerometer.stop();
		}
		if(LightSensor != null){
			LightSensor.stop();
		}
		
		CancelTask(rTask1);
		CancelTask(rTask2);
		CancelTask(rTask3);
		CancelTask(rTask4);
		CancelTask(rTask5);
		CancelTask(rTask6);
		//for drinking followup
		CancelTask(alarmTask1);
		CancelTask(alarmTask2);
		CancelTask(alarmTask3);
		CancelTask(alarmTask4);
		CancelTask(drinkSurveyTask);
		//CancelTask(midNightCheckTask);
		//Ricky 3/14
		PurgeTimers(t1);
		PurgeTimers(t2);
		PurgeTimers(t3);
		PurgeTimers(t4);
		PurgeTimers(t5);
		PurgeTimers(t6);
		//4/26 t7 for drink follow_up
		PurgeTimers(t7);
		//Ricky 3/14
		//PurgeTimers(midNightCheckTimer);
		Log.d(TAG,"Service Partially Stopped.");
		
		if(device!=null){
		device.stop();
		}
    }
    
    private class CompletedSignal extends AsyncTask<String,Void, Boolean>
	{

		@Override
		protected Boolean doInBackground(String... strings) {
			// TODO Auto-generated method stub
	         String UID=strings[0];
	         String Date=strings[1];
	         String RSID=strings[2];
	         if(checkDataConnectivity())
	 		{
	         HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Crt/compliance.php");
	         List<NameValuePair> params = new ArrayList<NameValuePair>();
	         params.add(new BasicNameValuePair("category","complete"));                            
	         params.add(new BasicNameValuePair("UID",UID));
	         params.add(new BasicNameValuePair("Date",Date));
	         params.add(new BasicNameValuePair("RSID",RSID));
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
 }

