package edu.missouri.bas.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;


import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class ActivityRecognitionService extends IntentService{

private static final String TAG ="ActivityRecognition";
public final static String BASE_PATH = "sdcard/TestResults/";
ActivityRecognitionResult result;
public static int currentUserActivity=9;
public static boolean IsRetrievingUpdates=false;
public static boolean IsIntentSent = false;
PendingIntent scheduleLocation;
AlarmManager mAlarmManager;
LocationManager mLocationManager;
IntentFilter mIntentFilter;


public ActivityRecognitionService() {
super("ActivityRecognitionService");
}

/**
* Google Play Services calls this once it has analysed the sensor data
*/
@Override
protected void onHandleIntent(Intent intent) {
   if (ActivityRecognitionResult.hasResult(intent)) {
	   result=null;
	   result = ActivityRecognitionResult.extractResult(intent);
	    setCurrentUserActivity(result.getMostProbableActivity().getType(),result.getMostProbableActivity().getConfidence());
		Log.d(TAG, "ActivityRecognitionResult: "+getNameFromType(result.getMostProbableActivity().getType()));
      }
}



@Override
public void onDestroy() {
	// TODO Auto-generated method stub
	super.onDestroy();
	
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

public  void setCurrentUserActivity(int Activity,int Confidence)
{
	
	currentUserActivity=Activity;
	switch (currentUserActivity) {
	case DetectedActivity.IN_VEHICLE:		
	case DetectedActivity.ON_BICYCLE:		
	case DetectedActivity.ON_FOOT:
		if(Confidence>=75)
		{
    		Intent i=new Intent("INTENT_ACTION_SCHEDULE_LOCATION");
    		i.putExtra("activity",currentUserActivity);
			this.sendBroadcast(i);			
		}
	case DetectedActivity.TILTING:			
	case DetectedActivity.STILL:
    case DetectedActivity.UNKNOWN:    	
	default:
		  //stopLocationUpdates();
  }
}

@Override
public void onCreate() {
	// TODO Auto-generated method stub
	super.onCreate();	
	
}

}
