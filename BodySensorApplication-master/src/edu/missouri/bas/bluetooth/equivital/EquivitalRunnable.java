package edu.missouri.bas.bluetooth.equivital;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

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



//Ricky
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import com.equivital.sdk.ISemConnection;
import com.equivital.sdk.connection.SemBluetoothConnection;
import com.equivital.sdk.decoder.BadLicenseException;
import com.equivital.sdk.decoder.SDKLicense;
import com.equivital.sdk.decoder.SemDevice;
import com.equivital.sdk.decoder.events.AccelerometerSemMessageEventArgs;
import com.equivital.sdk.decoder.events.ISemDeviceAccelerometerEvents;
import com.equivital.sdk.decoder.events.ISemDeviceSummaryEvents;
import com.equivital.sdk.decoder.events.SemSummaryDataEventArgs;

import edu.missouri.bas.MainActivity;
import edu.missouri.bas.service.SensorService;



public class EquivitalRunnable implements Runnable, ISemDeviceSummaryEvents, ISemDeviceAccelerometerEvents {
	
	private static SemDevice device;
	String deviceAddress;
	String deviceName;
	String phoneID;
	public static final int CHEST_SENSOR_DATA = 109;
	public static final int CHEST_SENSOR_ACCELEORMETER_DATA = 111;
	public final static String BASE_PATH = "sdcard/TestResults/";
	// Lists to buffer the data sent to server.
	List<String> dataPoints=new ArrayList<String>();
	List<String> AccDataPoints=new ArrayList<String>();
	//List<String> AccDataMedianPoints=new ArrayList<String>();
	//List<String> AccDataDiffPoints=new ArrayList<String>();
	// List to store the chest accelerometer data in order to do compression later.
	List<Double> chestAccList = new ArrayList<Double>();
	// List to calculate the median value of a shifting windows.
	//List<Double> medianList;
	// Queue to implement the median shifting windows. 
	//LinkedList<Double> medianWindowQueue = new LinkedList<Double>();
	// var to store the average chest accelerometer data
	private double averageAcc = 0;
	// count used to discard first several outliers
	private int count = 0;
	// var generated after applying median filter
	//private double medianFilteredAcc = 0;
	// delay time 11s
	//private int delaySecond = 10;
	

	public EquivitalRunnable(String address,String name,String ID)
	{
		deviceAddress=address;
		deviceName=name;
		phoneID=ID;
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
		//Log.d("Chest Acc Info","chest data recorded:");
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
		 //Log.d("Chest Info","data recorded:"+bodyPosition);
	}
	
	@Override
	public void accelerometerDataReceived(SemDevice arg0,
			AccelerometerSemMessageEventArgs arg1) {
		// TODO Auto-generated method stub
		//updateAcceleormeterSummary(arg1.getResultant_mG(),arg1.getLateral_mG(),arg1.getLongitudinal_mG(),arg1.getVertical_mG());
		if (count<25*11){
			count++;
		} else {
			updateAcceleormeterSummary(arg1.getResultant_mG());
		}
		//Log.d("Chest Acc Info",count+"|||"+String.valueOf(arg1.getResultant_mG()));
		
	}
	
	//private void updateAcceleormeterSummary(double resultantAcc, double lateralAcc, double longitudinalAcc, double verticalAcc) {
	private void updateAcceleormeterSummary(double resultantAcc) {	
		// TODO Auto-generated method stub
		 //String AccelerometerDataFromChestSensor=String.valueOf(resultantAcc)+","+String.valueOf(lateralAcc)+","+String.valueOf(longitudinalAcc)+","+String.valueOf(verticalAcc);
		if (compressChestSensorAccelerometerData(resultantAcc)){
			//medianFilterChestSensorAccelerometerData used to apply the filter
			//not used now, so discard the methods
			//if (medianFilterChestSensorAccelerometerData(averageAcc,delaySecond)){
			String AccelerometerDataFromChestSensor = String.valueOf(averageAcc);
			//String AccelerometerDataFromChestSensorMedian = String.valueOf(medianFilteredAcc);
			//String AccelerometerDataFromChestSensorDiff = String.valueOf(medianFilteredAcc-averageAcc);
			Message msg=new Message();
			msg.what = CHEST_SENSOR_ACCELEORMETER_DATA;
			Bundle dataBundle = new Bundle();
			dataBundle.putString("ACC",AccelerometerDataFromChestSensor);
			//dataBundle.putString("medianACC",AccelerometerDataFromChestSensorMedian);	
			//dataBundle.putString("diffACC",AccelerometerDataFromChestSensorDiff);
			msg.obj=dataBundle;
			chestSensorAccDataHandler.sendMessage(msg);
			//Log.d("Chest Acc Info", "AVG Acc"+averageAcc);
			//Log.d("Chest Acc Info","filtered Acc:"+AccelerometerDataFromChestSensor);
			//}
		}
	}
	
	Handler chestSensorDataHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			//if(msg.what==CHEST_SENSOR_DATA)
			//{
				Bundle resBundle =  (Bundle)msg.obj;
				writeChestSensorDatatoCSV(String.valueOf(resBundle.getString("DATA")));
				//Log.d("wtest","call function");				
			//}			
		}
		
	};
	
	Handler chestSensorAccDataHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){			
			//if(msg.what==CHEST_SENSOR_ACCELEORMETER_DATA)
			//{
				Bundle resBundle =  (Bundle)msg.obj;
				writeChestSensorAccelerometerDatatoCSV(String.valueOf(resBundle.getString("ACC")),"chestAccelerometer");
				//writeChestSensorAccelerometerDatatoCSV(String.valueOf(resBundle.getString("ACC")),"chestAccelerometerAVG");
				//writeChestSensorAccelerometerDatatoCSV(String.valueOf(resBundle.getString("medianACC")),"chestAccelerometerMedian");
				//writeChestSensorAccelerometerDatatoCSV(String.valueOf(resBundle.getString("diffACC")),"chestAccelerometerDiff");
			//}			
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
        /*
        // Deal with the delay when applying the filter
        int delayMillis = 1000*(delaySecond+1);
        cal.setTimeInMillis(cal.getTime().getTime() + delayMillis);
		*/
		String dataToWrite = String.valueOf(cal.getTime())+","+chestSensorData;		
        dataPoints.add(dataToWrite+";");
        
        
        //chen
        File encStat = new File(BASE_PATH,"encStat.txt");
        String endataToWrite = null;
         try {
        	 endataToWrite = encryption(dataToWrite);
         } catch (Exception e) {
         	// TODO Auto-generated catch block
         	e.printStackTrace();
         }
        
        if(dataPoints.size()==57)
        {
        	    List<String> subList = dataPoints.subList(0,56);
 	            String data=subList.toString();
 	            String formattedData=data.replaceAll("[\\[\\]]","");
 	           
 	            //chen
 	            String enformattedData = null;
 	            try {
 	            	long st = Calendar.getInstance().getTimeInMillis();
 	            	enformattedData = encryption(formattedData);
 	            	long ed = Calendar.getInstance().getTimeInMillis();
 	            	writeToFile(encStat,String.valueOf(ed-st));
 	            } catch (Exception e) {
 	            	// TODO Auto-generated catch block
 	            	e.printStackTrace();
 	            }
 	            
 	            
 	            //sendDatatoServer("chestsensor"+"."+phoneAddress+"."+deviceName+"."+dateObj,formattedData);
 	            TransmitData transmitData=new TransmitData();
 	            //transmitData.execute("chestsensor"+"."+phoneID+"."+deviceName+"."+dateObj,formattedData);
 	            transmitData.execute("chestsensor"+"."+phoneID+"."+deviceName+"."+dateObj,enformattedData);
 	            //Log.d("Equivital","Chest Summary Data Point Sent");
 	            subList.clear();  
 	            subList=null;
 	    } 	    	
		if(f != null){
			try {
				//writeToFile(f, dataToWrite);
				writeToFileEnc(f, endataToWrite);
			} catch (IOException e) {
				e.printStackTrace();
			}				
			
		}	
	}	

	private void writeChestSensorAccelerometerDatatoCSV(String chestSensorAccelerometerData, String fileName)
	{
		// TODO Auto-generated method stub
		//Toast.makeText(serviceContext,"Trying to write to the file",Toast.LENGTH_LONG).show();
		Calendar c=Calendar.getInstance();
		SimpleDateFormat curFormater = new SimpleDateFormat("MMMMM_dd"); 
		String dateObj =curFormater.format(c.getTime()); 		
		String file_name=fileName+"."+deviceName+"."+dateObj+".txt";	
		Calendar cal=Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("US/Central"));	
        File f = new File(BASE_PATH,file_name);		
		String dataToWrite = String.valueOf(cal.getTime())+","+chestSensorAccelerometerData;
		//if (fileName.equals("chestAccelerometerAVG")){
		AccDataPoints.add(dataToWrite+";");
		
		//chen
		String endataToWrite = null;
         try {
        	 endataToWrite = encryption(dataToWrite);
         } catch (Exception e) {
         	// TODO Auto-generated catch block
         	e.printStackTrace();
         }
        
        if(AccDataPoints.size()==57)
        {
        	    List<String> subList = AccDataPoints.subList(0,56);
 	            String data=subList.toString();
 	            String formattedData=data.replaceAll("[\\[\\]]","");
 	            //sendDatatoServer("chestsensor"+"."+phoneAddress+"."+deviceName+"."+dateObj,formattedData);
 	            
 	            //chen
 	            String enformattedData = null;
 	            try {
 	            	enformattedData = encryption(formattedData);
 	            } catch (Exception e) {
 	            	// TODO Auto-generated catch block
 	            	e.printStackTrace();
 	            }
 	            
 	            
 	            TransmitData transmitData=new TransmitData();
 	            //transmitData.execute(fileName+"."+phoneID+"."+deviceName+"."+dateObj,formattedData);
 	            transmitData.execute(fileName+"."+phoneID+"."+deviceName+"."+dateObj,enformattedData);
 	            Log.d("Equivital","AVG Accelerometer Data Point Sent");
 	            subList.clear();  
 	            subList=null;
 	    }
		/*}
		else if (fileName.equals("chestAccelerometerMedian")){
			AccDataMedianPoints.add(dataToWrite+";");
	        
	        if(AccDataMedianPoints.size()==57)
	        {
	        	    List<String> subList = AccDataMedianPoints.subList(0,56);
	 	            String data=subList.toString();
	 	            String formattedData=data.replaceAll("[\\[\\]]","");
	 	            //sendDatatoServer("chestsensor"+"."+phoneAddress+"."+deviceName+"."+dateObj,formattedData);
	 	            TransmitData transmitData=new TransmitData();
	 	            transmitData.execute(fileName+"."+phoneAddress+"."+deviceName+"."+dateObj,formattedData);
	 	            Log.d("Equivital","Median Accelerometer Data Point Sent");
	 	            subList.clear();  
	 	            subList=null;
	 	    }
		} else if (fileName.equals("chestAccelerometerDiff")){
			AccDataDiffPoints.add(dataToWrite+";");
	        
	        if(AccDataDiffPoints.size()==57)
	        {
	        	    List<String> subList = AccDataDiffPoints.subList(0,56);
	 	            String data=subList.toString();
	 	            String formattedData=data.replaceAll("[\\[\\]]","");
	 	            //sendDatatoServer("chestsensor"+"."+phoneAddress+"."+deviceName+"."+dateObj,formattedData);
	 	            TransmitData transmitData=new TransmitData();
	 	            transmitData.execute(fileName+"."+phoneAddress+"."+deviceName+"."+dateObj,formattedData);
	 	            Log.d("Equivital","Diff Accelerometer Data Point Sent");
	 	            subList.clear();  
	 	            subList=null;
	 	    }
		} 
 	    */	
		if(f != null){
			try {
				//writeToFile(f, dataToWrite);
				writeToFileEnc(f, endataToWrite);
			} catch (IOException e) {
				e.printStackTrace();
			}				
			
		}	
	}
	
	//Ricky 1/29 Function to compress the Chest Accelerometer Data
	//1G = 1000 mG = 9.8 m/s^2
	private Boolean compressChestSensorAccelerometerData(Double rawChestSensorAccelerometerDataMilli_Gs){
		double chestSensorAccelerometerData = rawChestSensorAccelerometerDataMilli_Gs/100;
		if (chestAccList.size()<=25){
			chestAccList.add(chestSensorAccelerometerData);
			return false;
		}
		else {
			averageAcc = 0;
			for (int i=0;i<chestAccList.size();i++){
				averageAcc +=chestAccList.get(i); 
			}
			averageAcc /= chestAccList.size();
			/*
			//median part
			Collections.sort(chestAccList);
			int medianPosition = chestAccList.size()/2;
			if (chestAccList.size()%2==0){
				medianFilteredAcc = (chestAccList.get(medianPosition)+chestAccList.get(medianPosition-1))/2;
			} else {
				medianFilteredAcc = chestAccList.get(medianPosition);
			}
			*/
			chestAccList.clear();
			chestAccList.add(chestSensorAccelerometerData);
			return true;
		}
	}
	
	/**
	 * @author Ricky 2/6
	 * Funtion to apply the median filter.
	 * @param compressedAVGChestSensorAccelerometerData: Avg value of 1 second data
	 * @param filterSize: filter shifting window size (second)
	 */
	/*
	private Boolean medianFilterChestSensorAccelerometerData(Double compressedAVGChestSensorAccelerometerData, int filterSize){
		if (medianWindowQueue.size()<=filterSize){
			medianWindowQueue.add(compressedAVGChestSensorAccelerometerData);
			return false;
		} else {
			medianList = new ArrayList<Double>(medianWindowQueue);
			Collections.sort(medianList);
			int medianPosition = medianList.size()/2;
			if (medianList.size()%2==0){
				medianFilteredAcc = (medianList.get(medianPosition)+medianList.get(medianPosition-1))/2;
			} else {
				medianFilteredAcc = medianList.get(medianPosition);
			}
			medianList.clear();
			medianWindowQueue.poll();						
			medianWindowQueue.add(compressedAVGChestSensorAccelerometerData);			
			return true;
		}
		
		
	}
	*/
	@Override
	public void highResolutionAccelerometerDataReceived(SemDevice arg0,
			AccelerometerSemMessageEventArgs arg1) {
		// TODO Auto-generated method stub
		// Not Used. No implementation here.
		
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
	        Key pubKey = MainActivity.pubk;
	        
	        //RSA cipher
	        Cipher cipherAsm = Cipher.getInstance("RSA", "BC");
	        cipherAsm.init(Cipher.ENCRYPT_MODE, pubKey);
	        
	        //RSA encryption
	        byte [] asymEncsymKey = cipherAsm.doFinal(aes_ba);
	        
//	        File f3 = new File(BASE_PATH,"enc.txt");
//	        File f3key = new File(BASE_PATH,"enckey.txt");
//	        File f3file = new File(BASE_PATH,"encfile.txt");
//	        writeToFile2(f3,f3key,f3file, asymEncsymKey, EncSymbyteArray);
	        
	        //byte != new String
	        //return new String(byteMerger(asymEncsymKey, EncSymbyteArray));
	        return Base64.encodeToString(byteMerger(asymEncsymKey, EncSymbyteArray),Base64.DEFAULT);
	        
		}
		
		public static byte[] byteMerger(byte[] byte_1, byte[] byte_2){  
	        byte[] byte_3 = new byte[byte_1.length+byte_2.length];  
	        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);  
	        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);  
	        return byte_3;  
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
	 
	 protected void writeToFileEnc(File f, String toWrite) throws IOException{
			FileWriter fw = new FileWriter(f, true);
			fw.write(toWrite);		
	        fw.flush();
			fw.close();
		}
	 
	 public void stop(){
		 count = 0;
		 chestAccList.clear();
		 //medianList.clear();
		 //medianWindowQueue.clear();
		 device.stop();
		 device.removeAccelerometerEventListener(this);
	 }
	

	
}
