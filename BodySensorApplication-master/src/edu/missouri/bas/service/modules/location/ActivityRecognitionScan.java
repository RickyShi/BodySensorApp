package edu.missouri.bas.service.modules.location;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.ActivityRecognitionClient;

import edu.missouri.bas.service.ActivityRecognitionService;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class ActivityRecognitionScan  implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener {


	private Context mContext;

	private static final String TAG = "ActivityRecognition";
	private static ActivityRecognitionClient mActivityRecognitionClient;	
	private static PendingIntent mActivityRecognitionPendingIntent;

	public ActivityRecognitionScan(Context context) {
	mContext=context;
	mActivityRecognitionClient = null;
	mActivityRecognitionPendingIntent=null;
	}

	/**
	* Call this to start a scan - don't forget to stop the scan once it's done.
	* Note the scan will not start immediately, because it needs to establish a connection with Google's servers - you'll be notified of this at onConnected
	*/
	public void startActivityRecognitionScan()
	{
	mActivityRecognitionClient	= new ActivityRecognitionClient(mContext, this, this);
	mActivityRecognitionClient.connect();	
	}

	public void stopActivityRecognitionScan(){
			try
			{
			mActivityRecognitionClient.removeActivityUpdates(mActivityRecognitionPendingIntent);	
			requestDisconnection();
			} 
			catch (IllegalStateException e)
			{
				Log.d(TAG,"Exception Caught");
			}
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.d(TAG,"onConnectionFailed");		
	}
	/**
	* Connection established - start listening now
	*/

	@Override
	public void onDisconnected() {
	}

	private ActivityRecognitionClient getActivityRecognitionClient() 
	{
        if (mActivityRecognitionClient == null) 
        {
            mActivityRecognitionClient = new ActivityRecognitionClient(mContext, this, this);
        }
        return mActivityRecognitionClient;
    }

	 private void continueRequestActivityUpdates() 
	 {
	        /*
	         * Request updates, using the default detection interval.
	         * The PendingIntent sends updates to ActivityRecognitionIntentService
	         */
	        getActivityRecognitionClient().requestActivityUpdates(1000*30,createRequestPendingIntent());

	        // Disconnect the client
	       // requestDisconnection();
	    }

	 private void requestDisconnection() 
	 {
	        getActivityRecognitionClient().disconnect();
	 }

	@Override
	public void onConnected(Bundle connectionHint) 
	{
		// TODO Auto-generated method stub		
		continueRequestActivityUpdates();

	}

	 public PendingIntent getRequestPendingIntent() {
	        return mActivityRecognitionPendingIntent;
	    }

	    /**
	     * Sets the PendingIntent used to make activity recognition update requests
	     * @param intent The PendingIntent
	     */
	 public void setRequestPendingIntent(PendingIntent intent) {
	        mActivityRecognitionPendingIntent = intent;
	  }


	private PendingIntent createRequestPendingIntent() {

        // If the PendingIntent already exists
        if (null != getRequestPendingIntent()) {

            // Return the existing intent
            return mActivityRecognitionPendingIntent;

        // If no PendingIntent exists
        } else {
            // Create an Intent pointing to the IntentService
            Intent intent = new Intent(mContext, ActivityRecognitionService.class);

            /*
             * Return a PendingIntent to start the IntentService.
             * Always create a PendingIntent sent to Location Services
             * with FLAG_UPDATE_CURRENT, so that sending the PendingIntent
             * again updates the original. Otherwise, Location Services
             * can't match the PendingIntent to requests made with it.
             */
            PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            setRequestPendingIntent(pendingIntent);
            return pendingIntent;
        }

    }

}