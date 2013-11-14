package edu.missouri.bas.service;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class HttpPostThread implements Runnable {

	LinkedBlockingQueue<HttpPostRequest> sendQueue;
	private volatile boolean running = false;
	Context serviceContext;
	private HttpPost destinationUri;
	private final String TAG = "HttpPostThread";
	
	//Standard sleep time is 20 seconds
	private final long SLEEP_STANDARD = 1000 * 20;
	//No data to send, sleep an extra 30 seconds
	private final long SLEEP_NO_DATA = 1000 * 30;
	//No data connectivity, sleep for 5 minutes
	private final long SLEEP_NO_CONNECTIVITY = 1000 * 60 * 1;
	
	FileWriter fw = null;
	File temp;
	//uri: http://dslsrv8.cs.missouri.edu/BAS/InsertTest.php
	
	public HttpPostThread(Context c, String destinationUri){
		 sendQueue = new LinkedBlockingQueue<HttpPostRequest>();
		 this.serviceContext = c;
		 try{
			 this.destinationUri = new HttpPost(destinationUri);
		 }catch(IllegalArgumentException e){
			 throw e;
		 }
		 
		 temp = new File("sdcard/TestResults/PostTest.txt");
	}
	
	
	public void run() {
		HttpPost postRequest;
		boolean success = false;
		int timeoutCounter;
		while(running){
			if(checkDataConnectivity() && sendQueue.size() >= 1){
				Log.d(TAG,"Can send.");
				List<NameValuePair> toSend = preparePostRequest();
				sendQueue = new LinkedBlockingQueue<HttpPostRequest>();
				HttpClient httpClient = new DefaultHttpClient();
				if(toSend != null && toSend.size() >= 1){
					timeoutCounter = 0;
					success = false;
					while(!success && timeoutCounter < 10){
						postRequest = destinationUri;
						try {
							postRequest.setEntity(
									new UrlEncodedFormEntity(toSend));
							HttpResponse response = httpClient.execute(postRequest);
							HttpEntity entity = response.getEntity();
							//TODO: This is bad, fix it
							if(entity != null){
								Log.d(TAG,"result Entity is not null:\n");
								/* try {
										fw = new FileWriter(temp, true);
										//String result = EntityUtils.toString(entity);
										//Log.d(TAG,"result: "+result);
										//fw.write("result: "+ result+"\n\n");
										//for(NameValuePair pair: toSend){
											//fw.write(pair.getName()+"\t"+pair.getValue()+"\n");
										//}
										//Log.d(TAG,"Done writing to file.");
										fw.close();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}*/
							}
							success = true;
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
							Log.e(TAG,"Bad data in http post thread");
							timeoutCounter = 5;
							success = false;
						} catch (ClientProtocolException e) {
							e.printStackTrace();
							timeoutCounter++;
							success = false;
						} catch (IOException e) {
							e.printStackTrace();
							timeoutCounter++;
							success = false;
						}
						//Successfully Sent, sleep to optimize battery
						sleep(this.SLEEP_STANDARD);
					}
					
				}
				//No data to send, wait 30 seconds;
				else{
					sleep(this.SLEEP_NO_DATA);
				}
			}
			//No connectivity, wait 5 minutes
			else{
				sleep(this.SLEEP_NO_CONNECTIVITY);
			}
		}
	}
	
	public void start(){
		running = true;
	}
	public void stop(){
		running = false;
	}
	public synchronized void post(HttpPostRequest p){
		//Log.d(TAG,"Adding request to queue");
		sendQueue.add(p);
	}
	
	protected synchronized List<NameValuePair> preparePostRequest(){
		//Iterate over request queue
			//create new BaseValuePairs, with key = request.getKeyName()+[x]
		//return completed request
		//Log.d(TAG,"Preparing for post");
		List<NameValuePair> dataList = new LinkedList<NameValuePair>();
		for(HttpPostRequest request: sendQueue){
			String key = request.getKeyName()+"[]";
			StringBuilder value = new StringBuilder();
			for(NameValuePair pair: request.getPostData()){
				//Log.d("HTTPPostthread","Sub Key:"+pair.getName()+"\tSub Value:"+pair.getValue());
				value.append(pair.getName()+"^"+pair.getValue()+"#");
			}
			//Log.d("HTTPPostThread","Key: "+key+"\tValue:"+value.toString());
			dataList.add(new BasicNameValuePair(key, value.toString()));
		}
		//Log.d(TAG,"Preparing request string, length: "+dataList.size());
		return dataList;
	}
	
	protected boolean checkDataConnectivity(){
		ConnectivityManager connectivity = 
				(ConnectivityManager) serviceContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		if(connectivity != null) {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if(info != null){
				for(int i = 0; i < info.length; i++){
					if(info[i].getState() == NetworkInfo.State.CONNECTED){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	protected void sleep(long duration){
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
