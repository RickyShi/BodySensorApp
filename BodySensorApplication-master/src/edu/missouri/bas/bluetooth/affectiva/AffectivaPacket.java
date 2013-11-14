package edu.missouri.bas.bluetooth.affectiva;

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

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import edu.missouri.bas.service.SensorService;
import edu.missouri.bas.service.modules.sensors.AccelerometerReading;

public class AffectivaPacket{
	private AccelerometerReading accelerometer;
	private float eda;
	private float battery;
	private float temperature;
	private String sequenceNum;
	static String previousTime=null;
	static String currentTime;
	
    
	//public static AffectivaPacket serviceContext;
	
	static String errMSG ="Please check your wifi or dataplan.\r\nThe phone is offline now.";
	protected Handler networkHandler;
	
	public AffectivaPacket(String seq, AccelerometerReading accel, float battery,
			float temp, float eda){
		this.sequenceNum = seq;
		this.accelerometer = accel;
		this.battery = battery;
		this.temperature = temp;
		this.eda = eda;
		//serviceContext=this;
	}
	
	
	public static  AffectivaPacket packetFromString(String s){

		String[] splitString = s.split(",");
		if(splitString.length != 7) return null;
		else{
			String seq = splitString[0];
	
			Calendar cal=Calendar.getInstance();
			cal.setTimeZone(TimeZone.getTimeZone("US/Central"));			
			currentTime=String.valueOf(cal.getTime());				
			String datatoWrite=currentTime+","+splitString[3]+","+
							splitString[2]+","+
							splitString[1]+","+
					        splitString[4]+","+
					        splitString[5]+","+
					        splitString[6];
			/*Code to write wrist sensor data to a CSV file */
			Calendar c=Calendar.getInstance();
			SimpleDateFormat curFormater = new SimpleDateFormat("MMMMM_dd"); 
			String dateObj =curFormater.format(c.getTime()); 
			String file_name="wristsensor_"+dateObj+".txt";	
			//SensorService.sendDatatoServer("wristsensor_"+dateObj,datatoWrite);
	        File f = new File(SensorService.BASE_PATH,file_name);	
	        
	        
			try {
				writeToFile(f,datatoWrite);
				sendDatatoServer("wristsensor_"+dateObj,datatoWrite);				
				Message m=new Message();
				//m=ss.mDataHandler.obtainMessage(101,"wristsensor_"+dateObj+","+datatoWrite);				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				//
			
			
					
			
			
			return new AffectivaPacket(seq,
					new AccelerometerReading(
							Float.parseFloat(splitString[3]),
							Float.parseFloat(splitString[2]),
							Float.parseFloat(splitString[1])),
					Float.parseFloat(splitString[4]),
					Float.parseFloat(splitString[5]),
					Float.parseFloat(splitString[6]));
		}
			
		}
		
	
	
	public static void sendDatatoServer(String FileName,String DataToSend)
	{
		if(checkDataConnectivity())
		{
        HttpPost request = new HttpPost("http://babbage.cs.missouri.edu/~rs79c/Android/EDAResponse.php");
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
                Log.d("Wrist Sensor Data Point Info",result);
               // Log.d("Wrist Sensor Data Point Info","Data Point Successfully Uploaded!");
            }
        } catch (Exception e) {
            
            e.printStackTrace();
        }
	}
    	
    else {
    	Log.d("Wrist Sensor Data Point Info","No Network Connection:Data Point was not uploaded");
        
    	 }
    }
	
	
	
	protected static boolean checkDataConnectivity() {
    	
		boolean value=SensorService.checkDataConnectivity();
		return value;
	}
	
	protected static void writeToFile(File f, String toWrite) throws IOException{
		FileWriter fw = new FileWriter(f, true);
		fw.write(toWrite+'\n');		
        fw.flush();
		fw.close();
	}
	
	
	
	
	
	
	public AccelerometerReading getAccelerometer(){
		return accelerometer;
	}
	
	public float getEda(){
		return eda;
	}
	
	public float getBattery(){
		return battery;
	}
	
	public float getTemperature(){
		return temperature;
	}
	
	public String getSequenceNum(){
		return sequenceNum;
	}
	
	@Override
	public  String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(sequenceNum+" ");
		sb.append(accelerometer.toString());
		sb.append(" EDA (microsiemens): "+eda);
		sb.append(" Battery: "+battery);
		sb.append(" Temperature (celsius): "+temperature);
		return sb.toString();
	}

	public String toCSV() {
		StringBuilder sb = new StringBuilder();
		sb.append(sequenceNum+",");
		sb.append(accelerometer.toCSV());
		sb.append(","+eda);
		sb.append(","+battery);
		sb.append(","+temperature);
		return sb.toString();
	}
	
	
}
