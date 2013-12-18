package edu.missouri.bas.datacollection;

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

import edu.missouri.bas.service.SensorService;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

//Ricky 2013/12/09
import android.os.AsyncTask;

public class InternalSensor implements Runnable, SensorEventListener {
	
	static SensorManager mSensorManager;
	int SensorType;
	int SamplingRate;
	static int Count=0;
	static String Temp=null;
	String identifier;
	List<String> dataPoints=new ArrayList<String>();
	Calendar c=Calendar.getInstance();
    SimpleDateFormat curFormater = new SimpleDateFormat("MMMMM_dd");

	public InternalSensor(SensorManager sensorManager,int sensorType,int samplingRate,String uniqueIdentifier)
	{
	    mSensorManager = sensorManager;
		SensorType=sensorType;
		SamplingRate=samplingRate;
		identifier=uniqueIdentifier;
	}	
	
	
	@Override
	public void run() 
	{  // TODO Auto-generated method stub
		Log.d("Sensor Number",String.valueOf(SensorType));
		setup(SensorType,SamplingRate);		
	}

	public void setup(int sensorType, int samplingRate) 
	{
		// TODO Auto-generated method stub
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(sensorType),samplingRate);
	}

	
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	public String getDate()
	{ 
   		String dateObj =curFormater.format(c.getTime()); 
   		return dateObj;
	}
	
	public String getTimeStamp()
	{
		Calendar cal=Calendar.getInstance();
   		cal.setTimeZone(TimeZone.getTimeZone("US/Central"));
		return String.valueOf(cal.getTime());
	}

	@Override
	public void onSensorChanged(SensorEvent event) {				
		// TODO Auto-generated method stub
   		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) 
	      {	
	    		String Accelerometer_Values = getTimeStamp()+","+event.values[0]+","+event.values[1]+","+event.values[2];
	    		String file_name="Accelerometer."+identifier+"."+getDate()+".txt";
	            File f = new File(SensorService.BASE_PATH,file_name);	           	   
                dataPoints.add(Accelerometer_Values+";");
	            if(dataPoints.size()==80)
	            {
	            	    List<String> subList = dataPoints.subList(0,41);
	     	            String data=subList.toString();	     	            
	     	            String formatedData=data.replaceAll("[\\[\\]]","");	
	     	            //Ricky 2013/12/09
	     	            //sendDatatoServer("Accelerometer."+identifier+"."+getDate(),formatedData);
	     	            TransmitData transmitData=new TransmitData();
	     	            transmitData.execute("Accelerometer."+identifier+"."+getDate(),formatedData);
	     	            subList.clear(); 	     	            
	     	    }
	    		try {
					writeToFile(f,Accelerometer_Values);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	      
	      }
   			 else if (event.sensor.getType() == Sensor.TYPE_LIGHT) 
	        {
	            //TODO: get values 
   				 
   				String LightIntensity= getTimeStamp()+","+event.values[0];
	        	String file_name="LightSensor."+identifier+"."+getDate()+".txt";
	            File f = new File(SensorService.BASE_PATH,file_name);	              
                dataPoints.add(LightIntensity+";");
	            if(dataPoints.size()==80)
	             {
	            	    List<String> subList = dataPoints.subList(0,41);
	     	            String data=subList.toString();	     	            
	     	            String formattedData=data.replaceAll("[\\[\\]]","");
	     	            //Ricky 2013/12/09
	     	            //sendDatatoServer("LightSensor."+identifier+"."+getDate(),formattedData);
	     	            TransmitData transmitData=new TransmitData();
	     	            transmitData.execute("LightSensor."+identifier+"."+getDate(),formattedData);
	     	            subList.clear();  
	     	      }	            
	    		try {
					writeToFile(f,LightIntensity);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    				        
	       }
	       
	}
	
	protected static void writeToFile(File f, String toWrite) throws IOException{
		FileWriter fw = new FileWriter(f, true);
		fw.write(toWrite+'\n');		
        fw.flush();
		fw.close();
	}
	
	
	
	protected static boolean checkDataConnectivity() {    	
		boolean value=SensorService.checkDataConnectivity();
		return value;
	}
	
	public void stop()
	{
		mSensorManager.unregisterListener(this);
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
        params.add(new BasicNameValuePair("data",DataToSend));
        try {
        	        	
            request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse response = new DefaultHttpClient().execute(request);
            if(response.getStatusLine().getStatusCode() == 200){
                String result = EntityUtils.toString(response.getEntity());
                Log.d("Sensor Data Point Info",result);                
               // Log.d("Wrist Sensor Data Point Info","Data Point Successfully Uploaded!");
            }
        } catch (Exception e) {
            
            e.printStackTrace();
        }
	  }
    	
    else {
    	Log.d("Sensor Data Point Info","No Network Connection:Data Point was not uploaded");
        
    	 }
    }
    */
}
