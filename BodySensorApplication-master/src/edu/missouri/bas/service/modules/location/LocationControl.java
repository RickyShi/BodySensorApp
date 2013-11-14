package edu.missouri.bas.service.modules.location;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import edu.missouri.bas.service.ScheduleController;

//TODO: Convert intent broadcasting to handler
public class LocationControl extends ScheduleController{
	
	private final int TIME_THRESHOLD;
	private final int ACCURACY_THRESHOLD;
	
	private LocationManager locationManager;

	private Location bestLocation;
	
	int checkInterval;
	float minDistance;
	
	Context serviceContext;
	
	public LocationControl(Context serviceContext,
			LocationManager locationManager, int time, int accuracy, long duration){
		
		this.locationManager = locationManager;
		this.serviceContext = serviceContext;
		this.duration = duration;
		
		TIME_THRESHOLD = time;
		ACCURACY_THRESHOLD = accuracy;
		
		bestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		
		if(bestLocation != null) Log.d("LocationControl",bestLocation.toString());
	}

	@Override
	protected void setup(){
		for(String provider: locationManager.getAllProviders()){
			locationManager.requestLocationUpdates(provider,
					checkInterval, minDistance, locationListener);
		}		
	}
	
	@Override
	protected void executeTimer(){
		locationManager.removeUpdates(locationListener);
		running = false;
		Location bestLocation = getLastCachedBestLocation();
		Intent i = new Intent(INTENT_ACTION_LOCATION);
		i.putExtra(LOCATION_INTENT_KEY, bestLocation);
		serviceContext.sendBroadcast(i);
	}
	
	
	public Location getLastCachedNetworkLocation(){
		return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	}
	
	public Location getLastCachedGPSLocation(){
		return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	}
	
	public Location getLastCachedBestLocation(){
		if(bestLocation != null){
			synchronized(bestLocation){
				return bestLocation;
			}
		}
		return null;
	}
	
	public Location updateBestLocation(Location newLocation, Location bestLocation){
		if(running == false) return bestLocation;
		if(bestLocation == null) return newLocation;
		
		long timeDifference = newLocation.getTime() - bestLocation.getTime();
		boolean isNewer = timeDifference > 0;
		
		if(timeDifference > TIME_THRESHOLD)
			return newLocation;
		else if(timeDifference < -TIME_THRESHOLD)
			return bestLocation;
		
		int accuracyDifference = (int)(newLocation.getAccuracy() - bestLocation.getAccuracy());
		boolean fromSameProvider = isSameProvider(newLocation.getProvider(), bestLocation.getProvider());
		
		if(accuracyDifference < 0){
			return newLocation;
		}
		else if(isNewer && (accuracyDifference == 0)){
			return newLocation;
		}
		else if(isNewer && (accuracyDifference <= ACCURACY_THRESHOLD) && fromSameProvider){
			return newLocation;
		}
		
		return bestLocation;
	}
	
	private boolean isSameProvider(String provider1, String provider2) {
		if(provider1 == null) return provider2 == null;
		return provider1 == provider2;
	}
	
	LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			Log.d("LocationControl","Got new location");
			if(bestLocation != null){
				synchronized(bestLocation){
						bestLocation = updateBestLocation(location, bestLocation);
				}
			}
	    }

	    public void onStatusChanged(String provider, int status, Bundle extras) {}

	    public void onProviderEnabled(String provider) {}

	    public void onProviderDisabled(String provider) {}
	};
}
