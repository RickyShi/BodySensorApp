package edu.missouri.bas.bluetooth.equivital;

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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.equivital.sdk.ISemConnection;
import com.equivital.sdk.connection.SemBluetoothConnection;
import com.equivital.sdk.decoder.BadLicenseException;
import com.equivital.sdk.decoder.SDKLicense;
import com.equivital.sdk.decoder.SemCalibrationParameterType;
import com.equivital.sdk.decoder.SemDevice;
import com.equivital.sdk.decoder.SemOperatingModeType;
import com.equivital.sdk.decoder.events.AccelerometerSemMessageEventArgs;
import com.equivital.sdk.decoder.events.BatteryVoltageEventArgs;
import com.equivital.sdk.decoder.events.HeartRateEventArgs;
import com.equivital.sdk.decoder.events.ISemDeviceAccelerometerEvents;
import com.equivital.sdk.decoder.events.ISemDeviceBatteryEvents;
import com.equivital.sdk.decoder.events.ISemDeviceBreathingRateEvents;
import com.equivital.sdk.decoder.events.ISemDeviceHeartRateEvents;
import com.equivital.sdk.decoder.events.ISemDeviceSummaryEvents;
import com.equivital.sdk.decoder.events.ISemDeviceTimingEvents;
import com.equivital.sdk.decoder.events.QualityConfidenceEventArgs;
import com.equivital.sdk.decoder.events.RRIntervalEventArgs;
import com.equivital.sdk.decoder.events.RespirationRateEventArgs;
import com.equivital.sdk.decoder.events.SEMDateTimeDataEventArgs;
import com.equivital.sdk.decoder.events.SemSummaryDataEventArgs;
import com.equivital.sdk.decoder.events.SynchronisationTimerEventArgs;
import com.google.android.gms.location.DetectedActivity;




//Ricky
import android.os.AsyncTask;
import edu.missouri.bas.service.SensorService;



public class EquivitalRunnable implements Runnable, ISemDeviceSummaryEvents, ISemDeviceAccelerometerEvents {
	
	private static SemDevice device;
	String deviceAddress;
	String deviceName;
	String phoneAddress;
	public static final int CHEST_SENSOR_DATA = 109;
	public static final int CHEST_SENSOR_ACCELEORMETER_DATA = 111;
	public final static String BASE_PATH = "sdcard/TestResults/";
	List<String> dataPoints=new ArrayList<String>();

	public EquivitalRunnable(String address,String name,String macAddress)
	{
		deviceAddress=address;
		deviceName=name;
		phoneAddress=macAddress;
		SDKLicense sdk = SemDevice.getLicense();
		sdk.applicationName = "Test Harness";
		sdk.developerName = "Java Version";
		sdk.licenseCode = "ZAP0Q9FLGo/XwrdBBAtdFk8jK7i/6fXFMzKiaCtC7jNvChtpMoOxSaH7tdqtFkmMbjUaskRyLGFCTGVJdNlrFjfbBjSGng9NGL4pnJ49TRTNR8Zmq0E9wnydpo3Du8RAcBVdGYjTjTctplrJ/cYHPHxOnbY5QuHYkY3dXBF3CSE=";
		try
		{
			device = new SemDevice();
			device.setSummaryDataEnabled(true);
		} 
		catch (BadLicenseException e1)
		{
			Log.d("EquivitalRunnable","ERROR:License Code and Developer Name don't match");
			return;
		}
		
	}	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		connectToDevice(deviceAddress);
	}

	private void connectToDevice(String address) 
	{
		Log.d("EquivitalRunnable","Trying to connect to the device");
	   	Log.d("EquivitalRunnable","Entered connectToDevice Method");
		// TODO Auto-generated method stub
	    device.addSummaryEventListener(this);	   	
		device.setSummaryDataEnabled(true);
		//Here to registered the Accelerometer Listener
		device.addAccelerometerEventListener(this);
		ISemConnection connection = SemBluetoothConnection.createConnection(address);		
		device.start(connection);
	}

	@Override
	public void summaryDataUpdated(SemDevice arg0, SemSummaryDataEventArgs arg1) {
		// TODO Auto-generated method stub
		updateSummary(arg1.getSummary().getMotion().name(),arg1.getSummary().getOrientation().name(),
				arg1.getSummary().getBreathingRate().getBeltSensorRate(),
				arg1.getSummary().getBreathingRate().getEcgDerivedRate(),arg1.getSummary().getBreathingRate().getImpedanceRate(),
				arg1.getSummary().getHeartRate().getEcgRate(),arg1.getSummary().getQualityConfidence().getBeltQuality(),
				arg1.getSummary().getQualityConfidence().getECGQuality(),
				arg1.getSummary().getQualityConfidence().getImpedanceQuality(),
				arg1.getSummary().getQualityConfidence().getHeartRateConfidence(),
				arg1.getSummary().getQualityConfidence().getBreathingRateConfidence(),arg1.getSummary().getGalvanicSkinResistance());
		Log.d("Chest Acc Info","chest data recorded:");
	}

	private void updateSummary(String motion, String bodyPosition,
			double beltSensorRate, double ecgDerivedRate,double impedanceRate,
			double ecgRate, double beltQuality, double ecgQuality,
			double impedanceQuality, double heartRateConfidence,
			double breathingRateConfidence,double GSR) {
		// TODO Auto-generated method stub
			/* 
			 * 1/22 Ricky Reduce ecgDerivedRate,impedanceRate,impedanceQuality
			 */
			/*
			 String dataFromChestSensor=motion+","+bodyPosition+","+String.valueOf(beltSensorRate)+","+String.valueOf(ecgDerivedRate)+","+
					 String.valueOf(impedanceRate)+","+String.valueOf(ecgRate)+","+String.valueOf(beltQuality)+","+String.valueOf(ecgQuality)+","+
					 String.valueOf(impedanceQuality)+","+String.valueOf(heartRateConfidence)+","+String.valueOf(breathingRateConfidence)+","+String.valueOf(GSR);
			*/
		String dataFromChestSensor=motion+","+bodyPosition+","+String.valueOf(beltSensorRate)+","+String.valueOf(ecgRate)+","+String.valueOf(beltQuality)+","+String.valueOf(ecgQuality)+","+
				 String.valueOf(heartRateConfidence)+","+String.valueOf(breathingRateConfidence)+","+String.valueOf(GSR);
		 Message msgData=new Message();
		 msgData.what = CHEST_SENSOR_DATA;
		 Bundle dataBundle = new Bundle();
		 dataBundle.putString("DATA",dataFromChestSensor);
		 msgData.obj=dataBundle;
		 chestSensorDataHandler.sendMessage(msgData);
		 //Log.d("Chest Acc Info","data recorded:"+bodyPosition);
	}
	
	Handler chestSensorDataHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what==CHEST_SENSOR_DATA)
			{
				Bundle resBundle =  (Bundle)msg.obj;
				writeChestSensorDatatoCSV(String.valueOf(resBundle.getString("DATA")));
				
			}
			if(msg.what==CHEST_SENSOR_ACCELEORMETER_DATA)
			{
				Bundle resBundle =  (Bundle)msg.obj;
				writeChestSensorAccelerometerDatatoCSV(String.valueOf(resBundle.getString("ACC")));
				
			}
			
		}
		
	};

	private void writeChestSensorDatatoCSV(String chestSensorData)
	{
		// TODO Auto-generated method stub
		//Toast.makeText(serviceContext,"Trying to write to the file",Toast.LENGTH_LONG).show();
		Calendar c=Calendar.getInstance();
		SimpleDateFormat curFormater = new SimpleDateFormat("MMMMM_dd"); 
		String dateObj =curFormater.format(c.getTime()); 		
		String file_name="chestsensor."+deviceName+"."+dateObj+".txt";	
		Calendar cal=Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("US/Central"));	
        File f = new File(BASE_PATH,file_name);		
		String dataToWrite = String.valueOf(cal.getTime())+","+chestSensorData;			   
        dataPoints.add(dataToWrite+";");
        if(dataPoints.size()==57)
        {
        	    List<String> subList = dataPoints.subList(0,56);
 	            String data=subList.toString();
 	            String formattedData=data.replaceAll("[\\[\\]]","");
 	            //sendDatatoServer("chestsensor"+"."+phoneAddress+"."+deviceName+"."+dateObj,formattedData);
 	            TransmitData transmitData=new TransmitData();
 	            transmitData.execute("chestsensor"+"."+phoneAddress+"."+deviceName+"."+dateObj,formattedData);
 	            Log.d("Equivital","Data Point Sent");
 	            subList.clear();  
 	            subList=null;
 	    } 	
		if(f != null){
			try {
				writeToFile(f, dataToWrite);
			} catch (IOException e) {
				e.printStackTrace();
			}				
			
		}	
	}
	
	@Override
	public void accelerometerDataReceived(SemDevice arg0,
			AccelerometerSemMessageEventArgs arg1) {
		// TODO Auto-generated method stub
		updateAcceleormeterSummary(arg1.getResultant_mG());
		 Log.d("Chest Acc Info","data recorded:");
		
	}
	
	private void updateAcceleormeterSummary(double resultantAccelerometer) {
		// TODO Auto-generated method stub
		 String AccelerometerDataFromChestSensor=String.valueOf(resultantAccelerometer);
		 Message msg=new Message();
		 msg.what = CHEST_SENSOR_ACCELEORMETER_DATA;
		 Bundle dataBundle = new Bundle();
		 dataBundle.putString("ACC",AccelerometerDataFromChestSensor);
		 msg.obj=dataBundle;
		 chestSensorDataHandler.sendMessage(msg);
		 Log.d("Chest Acc Info","data recorded:"+AccelerometerDataFromChestSensor);
	}
	

	private void writeChestSensorAccelerometerDatatoCSV(String chestSensorAccelerometerData)
	{
		// TODO Auto-generated method stub
		//Toast.makeText(serviceContext,"Trying to write to the file",Toast.LENGTH_LONG).show();
		Calendar c=Calendar.getInstance();
		SimpleDateFormat curFormater = new SimpleDateFormat("MMMMM_dd"); 
		String dateObj =curFormater.format(c.getTime()); 		
		String file_name="chestAccelerometer."+deviceName+"."+dateObj+".txt";	
		Calendar cal=Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("US/Central"));	
        File f = new File(BASE_PATH,file_name);		
		String dataToWrite = String.valueOf(cal.getTime())+","+chestSensorAccelerometerData;			   
        dataPoints.add(dataToWrite+";");
        /*
        if(dataPoints.size()==57)
        {
        	    List<String> subList = dataPoints.subList(0,56);
 	            String data=subList.toString();
 	            String formattedData=data.replaceAll("[\\[\\]]","");
 	            //sendDatatoServer("chestsensor"+"."+phoneAddress+"."+deviceName+"."+dateObj,formattedData);
 	            TransmitData transmitData=new TransmitData();
 	            transmitData.execute("chestAccelerometer"+"."+phoneAddress+"."+deviceName+"."+dateObj,formattedData);
 	            Log.d("Equivital","Accelerometer Data Point Sent");
 	            subList.clear();  
 	            subList=null;
 	    } 
 	    */	
		if(f != null){
			try {
				writeToFile(f, dataToWrite);
			} catch (IOException e) {
				e.printStackTrace();
			}				
			
		}	
	}

	@Override
	public void highResolutionAccelerometerDataReceived(SemDevice arg0,
			AccelerometerSemMessageEventArgs arg1) {
		// TODO Auto-generated method stub
		
	}
	
	//Ricky 2013/12/09
	private class TransmitData extends AsyncTask<String,Void, Boolean>
	{

		@Override
		protected Boolean doInBackground(String... strings) {
			// TODO Auto-generated method stub
			 String fileName=strings[0];
	         String dataToSend=strings[1];
	         if(checkDataConnectivity())
	 		{
	         HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Crt/writeArrayToFile.php");
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
	     	return false;
	      } 
		    
		}
		
	}
	/*
	public static void sendDatatoServer(String FileName,String DataToSend)
	{
		if(checkDataConnectivity())
		{
        
		//HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Crt/writeArrayToFile.php");
		//new test URL	
		HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Test/writeArrayToFile.php");
		List<NameValuePair> params = new ArrayList<NameValuePair>();
        //file_name 
        params.add(new BasicNameValuePair("file_name",FileName));
        //data
        params.add(new BasicNameValuePair("data",DataToSend+"\n"));
        
        try {
            request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse response = new DefaultHttpClient().execute(request);
            if(response.getStatusLine().getStatusCode() == 200){
                String result = EntityUtils.toString(response.getEntity());
                Log.d("Chest Sensor Data Point Info",result);              
            }
        } catch (Exception e) {
            
            e.printStackTrace();
        }
	}
    	
    else {
    	Log.d("Chest Sensor Data Point Info","No Network Connection:Data Point was not uploaded");        
    	 }
    }
	*/
	protected static boolean checkDataConnectivity() {
    	
		boolean value=SensorService.checkDataConnectivity();
		return value;
	}
	
	 protected void writeToFile(File f, String toWrite) throws IOException{
			FileWriter fw = new FileWriter(f, true);
			fw.write(toWrite+'\n');		
	        fw.flush();
			fw.close();
		}
	

	
}
