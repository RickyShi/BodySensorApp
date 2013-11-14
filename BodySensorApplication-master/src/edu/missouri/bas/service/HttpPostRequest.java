package edu.missouri.bas.service;


import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;

public class HttpPostRequest {
	
	private List<NameValuePair> nameValuePairs;
	private HttpPost destinationUri;
	private String keyName;
	
	public HttpPostRequest(String destinationUri, String keyName, List<NameValuePair> postData)
			throws IllegalArgumentException{
		this.nameValuePairs = postData;
		this.keyName = keyName;
		/*try{
			this.destinationUri = new HttpPost(destinationUri);
		}catch(IllegalArgumentException e){
			throw e;
		}*/
	}
	
	public HttpPostRequest(HttpPost destination, List<NameValuePair> postData){
		this.nameValuePairs = postData;
		//this.destinationUri = destination;
	}
	
	public HttpPost getDestination(){
		return destinationUri;
	}
	
	public List<NameValuePair> getPostData(){
		return nameValuePairs;
	}
	
	public String getKeyName(){
		return keyName;
	}
	
}
