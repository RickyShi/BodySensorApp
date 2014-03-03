/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.missouri.bas.activities;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import edu.missouri.bas.MainActivity;
import edu.missouri.bas.R;
import edu.missouri.bas.service.SensorService;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
@SuppressWarnings("deprecation")
public class AdminManageActivity extends TabActivity {
    
	private TabHost tabHost; 
	private String TAG = "TAG~~~~~~~~~~~~~~~~~~~";
	String AsIdHint; 
	String RmIdHint; 
	String currentAssID;
	SharedPreferences shp;
	SharedPreferences bedTime;
	Editor editor;
	Editor editor2;
	EditText asID;
    EditText deasID;
    Button AssignButton;
    Button RemoveButton;
    public static final String ASID = "AsId";
    public static final String ASPWD = "AsPwd";
    Context ctx;
    InputMethodManager imm;
    EditText adminPin;
    TextView alert_text;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "OnCreate!!~~~");
        
        ctx = this;
        // Setup the window
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        // Set result CANCELED incase the user backs out
        //setResult(Activity.RESULT_CANCELED);
        
        ////////////////////////////////////////////////////////////////////
                
        tabHost = getTabHost();    
        LayoutInflater.from(this).inflate(R.layout.activity_admin_manage, tabHost.getTabContentView(), true);    
        tabHost.addTab(tabHost.newTabSpec("Assign ID").setIndicator("Assign ID", null).setContent(R.id.tab_assign));   
        tabHost.addTab(tabHost.newTabSpec("Remove ID").setIndicator("Remove ID", null).setContent(R.id.tab_logoff));   
            
        setContentView(tabHost);    
        

        shp = getSharedPreferences("PINLOGIN", Context.MODE_PRIVATE);
        editor = shp.edit();
        bedTime = ctx.getSharedPreferences(SensorService.BED_TIME, MODE_PRIVATE);
        editor2 = bedTime.edit();
        asID = (EditText) findViewById(R.id.assigned_ID);
        deasID = (EditText) findViewById(R.id.deassigned_ID);
        AssignButton = (Button) findViewById(R.id.btn_assign);
        RemoveButton = (Button) findViewById(R.id.btn_remove);
        
        
        imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.toggleSoftInput(0, InputMethodManager.RESULT_SHOWN);
        
        //imm.showSoftInput(asID, InputMethodManager.RESULT_SHOWN); 
        imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, InputMethodManager.RESULT_HIDDEN); 
        
        asID.setFocusable(true);
        asID.setFocusableInTouchMode(true);
               
        asID.requestFocus();
        
        setListeners();
        
       
        
        
        Dialog adminPin = AdminPinDialog(this);
        adminPin.show();
       
        setResult(9);
        setHints();
    }

    
    private Dialog AdminPinDialog(Context context) {  
        LayoutInflater inflater = LayoutInflater.from(this);  
        final View textEntryView = inflater.inflate(  
                R.layout.pin_login, null);  
        alert_text = (TextView) textEntryView.findViewById(R.id.pin_text);
        alert_text.setText("Please input PIN for administrator");
        AlertDialog.Builder builder = new AlertDialog.Builder(context);  
        builder.setCancelable(false);
        builder.setTitle("Verify Admin PIN");  
        builder.setView(textEntryView);  
        //builder.setMessage("Please input PIN for administrator");
        builder.setPositiveButton("OK",  
                new DialogInterface.OnClickListener() {  
                    public void onClick(DialogInterface dialog, int whichButton) {  
                    	
                    	//check networking
                    	
                    	adminPin = (EditText) textEntryView.findViewById(R.id.pin_edit);
                    	String AdPin = adminPin.getText().toString();
                    	Log.d(TAG, "get from edittext is "+AdPin);
                    	
                    	HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Crt/validateUser.php");
         		        
         		        List<NameValuePair> params = new ArrayList<NameValuePair>();
         		        
         		        //file_name 
         		        params.add(new BasicNameValuePair("userID","0000"));        
         		        //function
         		        params.add(new BasicNameValuePair("pre","1"));
         		        //data                       
         		        params.add(new BasicNameValuePair("password",AdPin));

         		        	        	
         		        try {
         					request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
         				
         		        
         		        HttpResponse response = new DefaultHttpClient().execute(request);
         		        if(response.getStatusLine().getStatusCode() == 200){
         		            String result = EntityUtils.toString(response.getEntity());
         		            Log.d("~~~~~~~~~~http post result",result);     
         		            
         		            if(result.equals("AdminIsChecked")){
         		            	//do nothing
         		            	
         		            	//setResult(8);
         		            }else if(result.equals("AdminPinIsInvalid")){
         		            	
         		            	imm.toggleSoftInput(0, InputMethodManager.RESULT_SHOWN);
         	 					imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
         	 					
         		            	Toast toast = Toast.makeText(getApplicationContext(), "Admin PIN is wrong, exit! Please try again.", Toast.LENGTH_SHORT);
         		            	toast.show();
         		            	//set return code
/*         	 					if(shp.getString(ASID, "").equals("")){
         	 						setResult(9);
         	 					}else{
         	 						setResult(Activity.RESULT_CANCELED);
         	 					}*/
         	 					//setResult(9);
         		            	finish();
         		            }else{
         		            	
         		            	imm.toggleSoftInput(0, InputMethodManager.RESULT_SHOWN);
         	 					imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
         	 					
         		            	Toast toast = Toast.makeText(getApplicationContext(), "Verify failed. Please try again.", Toast.LENGTH_SHORT);
         		            	toast.show();
         		            	//set return code
/*         	 					if(shp.getString(ASID, "").equals("")){
         	 						setResult(9);
         	 					}else{
         	 						setResult(Activity.RESULT_CANCELED);
         	 					}*/
         		            	finish();
         		            }
         		            
         		        } 
         		        } catch (Exception e) {
         					// TODO Auto-generated catch block
         		        	
         		        	imm.toggleSoftInput(0, InputMethodManager.RESULT_SHOWN);
     	 					imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
     	 					
         		        	Toast toast = Toast.makeText(getApplicationContext(), "Failed. Please try again, with network connection.", Toast.LENGTH_SHORT);
     		            	toast.show();
     		            	//set return code
/*     	 					if(shp.getString(ASID, "").equals("")){
     	 						setResult(9);
     	 					}else{
     	 						setResult(Activity.RESULT_CANCELED);
     	 					}*/
     		            	
     		            	finish();
         					e.printStackTrace();
         				}
 
         		        
                    }  
                });  
        builder.setNegativeButton("Cancel",  
                new DialogInterface.OnClickListener() {  
                    public void onClick(DialogInterface dialog, int whichButton) {  
                    	
                    	imm.toggleSoftInput(0, InputMethodManager.RESULT_SHOWN);
 	 					imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
 	 					
 	 					//set return code
/* 	 					if(shp.getString(ASID, "").equals("")){
 	 						setResult(9);
 	 					}else{
 	 						setResult(Activity.RESULT_CANCELED);
 	 					}*/
 	 					setResult(9);
                        finish(); 
                    }  
                });  
        return builder.create();  
    }
    
    
    
    
    
    private void setHints() {
		// TODO Auto-generated method stub
    	
    	asID.setText("");
		currentAssID = shp.getString(ASID, "");
		Log.d(TAG, "set Hints is "+shp.getString(ASID,""));
        if(currentAssID.equals("")){
        	AsIdHint = "Four-digit ID goes here";
        	RmIdHint = "Not set yet";		        	
        }
        else{
        	AsIdHint = "Current " + shp.getString(ASID, "");
        	RmIdHint = shp.getString(ASID, "");
        }
        
        asID.setHint(AsIdHint);
        deasID.setHint(RmIdHint);
	}
    
    private void setListeners() {
		// TODO Auto-generated method stub
    	Log.d(TAG, "Ontabchangedlistener!!~~~");
    	 tabHost.setOnTabChangedListener(new OnTabChangeListener(){
    		 
 			

 			@Override
 			public void onTabChanged(String arg0) {
 				// TODO Auto-generated method stub
 				Log.d(TAG,"~~"+arg0);
 				
 				
 				setHints();
 				
 				if(arg0.equals("Assign ID")){
 					imm.toggleSoftInput(0, InputMethodManager.RESULT_HIDDEN);
 					
 					Log.d(TAG ,"assign id ");
 					

 				}else{
 					
 					imm.toggleSoftInput(0, InputMethodManager.RESULT_SHOWN);
 					imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
 					
 					Log.d(TAG,"deassign id");
 					
 				}
 			}

 			
         });
         
         
         AssignButton.setOnClickListener(new View.OnClickListener() {
 			
 			@Override
 			public void onClick(View v) {
 				// TODO Auto-generated method stub
 				Log.d(TAG ,"assign btn "+asID.getText().toString());
 				
 				//editor.putString(ASID, asID.getText().toString());
 				//format check
 				
 				//editor.putString(ASPWD, "");
 				//editor.commit();
 				//setHints();
 				
 				
 				
            	//check networking
            	
            	
            	String asedID = asID.getText().toString();
            	Log.d(TAG, "get from edittext is "+asedID);
            	
            	HttpPost request = new HttpPost("http://dslsrv8.cs.missouri.edu/~rs79c/Server/Crt/validateUser.php");
 		        
 		        List<NameValuePair> params = new ArrayList<NameValuePair>();
 		        
 		        //file_name 
 		        params.add(new BasicNameValuePair("userID",asedID));        
 		        //function
 		        params.add(new BasicNameValuePair("pre","2"));
 		        //data                       
 		        //params.add(new BasicNameValuePair("password",""));

 		        	        	
 		        try {
 					request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
 				
 		        
 		        HttpResponse response = new DefaultHttpClient().execute(request);
 		        if(response.getStatusLine().getStatusCode() == 200){
 		            String result = EntityUtils.toString(response.getEntity());
 		            Log.d("~~~~~~~~~~http post result2 ",result);     
 		            
 		            if(result.equals("UserIDIsNotSet")){
 		            	//add in web page first
 		            	
 		            	String s1 = "This ID is not in Database, please double check or add it via web page by administrator first.";
 		            	buildDialog1(ctx, s1).show();
 		            	setResult(9);
 		            	
 		            }else if(result.equals("UserIDIsUsed")){
 		            	String s2 = "This ID has been used before. Delete it via web page by administrator first.";
 		                buildDialog2(ctx, s2).show();
 		                setResult(9);
 		            	 		            	
 		            }else if(result.equals("UserIDIsNotActive")){
 		            	//assign
 		            	String s3 = "Do you want to assign this ID: "+asedID;
 		            	buildDialog2(ctx, s3).show();
 		            	
 		            }else{
 		            	String s4 = "The ID format seems not applicable, please try again.";
 		            	buildDialog1(ctx, s4).show();
 		            	setResult(9);
 		            	
 		            }
 		            
 		        }
 		        } catch (Exception e) {
 					// TODO Auto-generated catch block
 					e.printStackTrace();
 					String s4 = "The ID format seems not applicable, please try again.";
		            buildDialog1(ctx, s4).show();
		            setResult(9);
 				}
 		        
 				
 			}
 		});
         
         
         
         
         RemoveButton.setOnClickListener(new View.OnClickListener() {
 			
 			@Override
 			public void onClick(View v) {
 				// TODO Auto-generated method stub
 				Log.d(TAG ,"remove btn ");
 				
 				//add a confirm dialog
 				

 				setHints();
 				Log.d(TAG,"cur is "+currentAssID);
 				
 				if(!currentAssID.equals("")){
 					Dialog alertDialog = new AlertDialog.Builder(ctx)
 					.setCancelable(false)
 					.setTitle("Confirm Remove")
 					.setMessage("Do you really want to remove this ID and all related data from the device? ")
 					.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
 	                     
 	                    @Override 
 	                    public void onClick(DialogInterface dialog, int which) { 
 	                        // TODO Auto-generated method stub  
 	        				editor.putString(ASID, "");	        				
 	        				editor.putString(ASPWD, "");
 	        				editor.commit();
 	        				editor2.putString(SensorService.BED_TIME_INFO, "none");	        				
 	        				editor2.putString(SensorService.BED_HOUR_INFO, "none");
 	        				editor2.putString(SensorService.BED_MIN_INFO, "none");
 	        				editor2.commit();
 	        				
 	        				setHints();
 	        				MainActivity.mIsRunning = false;     	
 	        		    	ctx.stopService(new Intent(AdminManageActivity.this,SensorService.class)); 
 	        				setResult(9);
 	        				finish();
 	                    } 
 	                })
 	                . setNegativeButton("Cancel", new DialogInterface.OnClickListener() { 
 	                    
 	                   @Override 
 	                   public void onClick(DialogInterface dialog, int which) { 
 	                       // TODO Auto-generated method stub  
 	                   
 	                   } 
 	                })
 					.create();
 					
 					alertDialog.show();
 				}
 			}
 		});
	}

    
    private Dialog buildDialog1(Context context, String str) {  
        AlertDialog.Builder builder = new  AlertDialog.Builder(context);  
        builder.setCancelable(false);
        builder.setTitle("Confirm");  
        builder.setMessage(str);
        builder.setPositiveButton("OK",  
                new DialogInterface.OnClickListener() {  
                    public void onClick(DialogInterface dialog, int whichButton) {  
                    	setHints();
                    }  
                });   
        return builder.create();  
    }  
    
    private Dialog buildDialog2(Context context, String str) {  
        AlertDialog.Builder builder = new  AlertDialog.Builder(context);  
        builder.setCancelable(false);
        builder.setTitle("Confirm");  
        builder.setMessage(str);
        builder.setPositiveButton("OK",  
                new DialogInterface.OnClickListener() {  
                    public void onClick(DialogInterface dialog, int whichButton) {  
                    	editor.putString(ASID, asID.getText().toString());
                    	Log.d("here!!!", "id is "+asID.getText().toString());
         				//format check
         				
         				editor.putString(ASPWD, "");
         				editor.commit();
         				setHints();
         				setResult(8);
                    }  
                });  
        builder.setNegativeButton("Cancel",
        		new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						setHints();
					}
				});
        return builder.create();  
    }  


	@Override
    protected void onDestroy() {
        super.onDestroy();

    }

}