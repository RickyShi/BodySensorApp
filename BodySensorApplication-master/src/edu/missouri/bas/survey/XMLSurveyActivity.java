package edu.missouri.bas.survey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xml.sax.InputSource;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import edu.missouri.bas.R;
import edu.missouri.bas.activities.AdminManageActivity;
import edu.missouri.bas.service.SensorService;
import edu.missouri.bas.survey.category.RandomCategory;
import edu.missouri.bas.survey.category.SurveyCategory;
import edu.missouri.bas.survey.question.SurveyQuestion;

/* Author: Ricky Shi
 * Last Update: 12/14/2013
 * Comments Added
 * 
 * Generic activity used for displaying surveys.
 * Information about the survey to be displayed
 * is passed in using the creating intent with 
 * extras ("survey_name", "survey_file"). 
 */
//TODO: Add previous question button to go back
//TODO: Allow multiple questions to be displayed at a once


public class XMLSurveyActivity extends Activity {
	
	//Constants
	//Used to pass survey results back 
	public static final String INTENT_ACTION_SURVEY_RESULTS =
			"action_survey_results";

	public static final String INTENT_EXTRA_SURVEY_NAME =
			"survey_name";

	public static final String INTENT_EXTRA_SURVEY_RESULTS =
			"survey_results";

	public static final String INTENT_EXTRA_COMPLETION_TIME =
			"survey_completion_time";
	
	private static final String LAST_QUESTION_TEXT =
			"In the past 15 minutes, WHERE is your location: (Check all that apply)";
	
	private boolean modifyBackButtonFlag = false;
	
	public static Context SurveyActivityContext;

	//List of read categories
	ArrayList<SurveyCategory> cats = null;

	//Current question
	SurveyQuestion currentQuestion;
	
	//Current category
	SurveyCategory currentCategory;
	
	//Category position in arraylist
	int categoryNum;
	
	//Layout question will be displayed on
    LinearLayout surveyLayout;
    
    //Button used to submit each question
    Button submitButton;
    Button backButton;
    
    //Will be set if a question needs to skip others
    boolean skipTo = false;
    String skipFrom = null;
        
    String surveyName;
    String surveyFile;
  //Ricky 4/1/14
  	private int randomSeq;
  	
  	private String ID;
  	
    private static final String TAG = "XMLSurveyActivity";
    String errMSG ="Please check your wifi or dataplan.\r\nThe phone is offline now.";
    /*
     * Putting a serializable in an intent seems to default to the class
     * that implements serializable, so LinkedHashMap or TreeMap are treated
     * as a hashmap when received.
     * 
     * TODO: Maybe make a private class with LinkedHash/Tree Map + parcelable
     */
    LinkedHashMap<String, List<String>> answerMap;
    MediaPlayer mp;
    
    

    public class StartSound extends TimerTask {
    	@Override    	
    	public void run(){ 
    	Log.d(TAG,"Sound ALarm");
        mp = MediaPlayer.create(getApplicationContext(), R.raw.bodysensor_alarm);
    	mp.start();
    	}
    }
    
    private class SurveyNotCompletedAlarm extends TimerTask{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d(TAG,"Final ALarm");
			SensorService.drinkUpFlag = false;
			finish();
			this.cancel();
			
		}
    	
    }
	
    /** Called when the activity is first created. */
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setVolumeControlStream(AudioManager.STREAM_ALARM);
        setContentView(R.layout.survey_layout);
        
        //Initialize map that will pass questions and answers to service
        answerMap = new LinkedHashMap<String, List<String>>();
        
        /*
         * The same submit button is used for every question.
         * New buttons could be made for each question if
         * additional specific functionality is needed/
         */
        submitButton = new Button(this);
        backButton = new Button(this);
        submitButton.setText("Submit");
        backButton.setText("Previous Question");
        
        //First cancel the all the alarms set in the SurveyPinCheck Activity
        cancelAllTimerTask();
        /*
         * Here to alarm the user to complete the survey
         * Ricky 2013/12/14
         */
        Date aTime1 = new Date();
        Date aTime2 = new Date();
        Date aTime3 = new Date();
        Date aTime4 = new Date();
        aTime1.setMinutes(aTime1.getMinutes()+5);
        aTime2.setMinutes(aTime2.getMinutes()+10);
        aTime3.setMinutes(aTime3.getMinutes()+15);
        aTime4.setMinutes(aTime4.getMinutes()+15);
        aTime4.setSeconds(aTime4.getSeconds()+20);
        SensorService.alarmTask1 = new StartSound();
        SensorService.alarmTask2 = new StartSound();
        SensorService.alarmTask3 = new StartSound();
        SensorService.alarmTask4 = new SurveyNotCompletedAlarm();
        SensorService.alarmTimer.schedule(SensorService.alarmTask1, aTime1);
        SensorService.alarmTimer.schedule(SensorService.alarmTask2, aTime2);
        SensorService.alarmTimer.schedule(SensorService.alarmTask3, aTime3);
        SensorService.alarmTimer.schedule(SensorService.alarmTask4, aTime4);
        
        SurveyActivityContext = this;
        
        submitButton.setOnClickListener(new OnClickListener(){
			
			public void onClick(View v) {
				if(currentQuestion.validateSubmit()){
					ViewGroup vg = setupLayout(nextQuestion());
					if(vg != null){
						setContentView(vg);
					}
				}
			}
        });
        
        backButton.setOnClickListener(new OnClickListener(){
			
			public void onClick(View v) {
				Log.d(TAG,"Going back a question");
				ViewGroup vg = setupLayout(previousQuestion());
				if(vg != null)
					setContentView(vg);
			}
        });
        
        
        
        //Setup XML parser
		XMLParser parser = new XMLParser();
		
		//Tell the parser which survey to use		
		surveyName = getIntent().getStringExtra("survey_name");
		surveyFile = getIntent().getStringExtra("survey_file");
		
		//Ricky 2/4/2014
		//The following alarms are used in SurvetPinCheck now.
		/*
		if(surveyName.equalsIgnoreCase("RANDOM_ASSESSMENT") && surveyFile.equalsIgnoreCase("RandomAssessmentParcel.xml"))
		{
			Timer t=new Timer();
			t.schedule(new  StartSound(),1000*5);			
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	        v.vibrate(1000);
		}
		//ADD VOICE AND VIBRATE CONTROL TO THE DRINKFOLLOWUP
		if(surveyName.equalsIgnoreCase("DRINKING_FOLLOWUP") && surveyFile.equalsIgnoreCase("DrinkingFollowup.xml"))
		{	
			SensorService.drinkUpFlag =true;
			Timer t=new Timer();
			t.schedule(new  StartSound(),1000*5);			
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	        v.vibrate(1000);
		}
		*/
		Log.d("XMLSurvey","File Name: "+surveyFile);
		
		//Open the specified survey
		try {
			/*
			 * .parseQuestion takes an input source to the assets file,
			 * a context in case there are external files, a boolean for
			 * allowing external files, and a baseid that will be appended
			 * to question ids.  If boolean is false, no context is needed.
			 */
			cats = parser.parseQuestion(new InputSource(getAssets().open(surveyFile)),
					this,true,"");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Survey didn't contain any categories
		if(cats == null){
			surveyComplete();
		}
		//Survey did contain categories
		else{
			//Set current category to the first category
			currentCategory = cats.get(0);
			//Setup the layout
			ViewGroup vg = setupLayout(nextQuestion());
			if(vg != null)
				setContentView(vg);
		}
		SharedPreferences shp = getSharedPreferences("PINLOGIN", Context.MODE_PRIVATE);
	    ID = shp.getString(AdminManageActivity.ASID, "");
		String startLog = Calendar.getInstance().getTime().toString()+", "+surveyName+" survey is started.";
		TransmitData transmitData=new TransmitData();
		transmitData.execute("EventSurvey."+ID,startLog);
    }
    
    
    
    @Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		//mp.release();
	}


    protected void onDestroy(){
    	if(surveyName.equalsIgnoreCase("DRINKING_FOLLOWUP") && surveyFile.equalsIgnoreCase("DrinkingFollowup.xml")){
    		SensorService.drinkUpFlag = false;
    	}
    	cancelAllTimerTask();
    	super.onDestroy();
    }
	protected LinearLayout setupLayout(LinearLayout layout){
    	/* Didn't get a layout from nextQuestion(),
    	 * error (shouldn't be possible) or survey complete,
    	 * either way finish safely.
    	 */
    	if(layout == null){
    		surveyComplete();
    		return null;
    	}
    	else{
			//Setup ScrollView
    		LinearLayout sv = new LinearLayout(getApplicationContext());
			//Remove submit button from its parent so we can reuse it
			if(submitButton.getParent() != null){
				((ViewGroup)submitButton.getParent()).removeView(submitButton);
			}
			if(backButton.getParent() != null){
				((ViewGroup)backButton.getParent()).removeView(backButton);
			}
			//Add submit button to layout
			
			//LinearLayout.LayoutParams keepFull = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT);
			
			RelativeLayout.LayoutParams keepBTTM = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
			keepBTTM.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			
			//sv.setLayoutParams(keepFull);
			//layout.setLayoutParams(keepFull);
			
			RelativeLayout rela = new RelativeLayout(getApplicationContext());
			//rela.setLayoutParams(keepFull);
						
			LinearLayout buttonCTN = new LinearLayout(getApplicationContext());
			buttonCTN.setOrientation(LinearLayout.VERTICAL);
			buttonCTN.setLayoutParams(keepBTTM);
			
			buttonCTN.addView(submitButton);
			buttonCTN.addView(backButton);

			rela.addView(buttonCTN);
			layout.addView(rela);
			
			//layout.addView(submitButton);
			//layout.addView(backButton);
			//Add layout to scroll view in case it's too long
			sv.addView(layout);
			//Display scroll view
			setContentView(sv);
			return sv;
    	}
    }
    
    protected void surveyComplete(){
    	
    	//Fill answer map for when it is passed to service
    	for(SurveyCategory cat: cats){
    		for(SurveyQuestion question: cat.getQuestions()){
    			answerMap.put(question.getId(), question.getSelectedAnswers());
    			//Here to target the first question of Drinking Follow-up
    			if (cat.getCurrentQuestionText().equals("Drinking Follow-up")&&question.getId().equals("q611")){
    				//dAns is the answer of drink numbers user reported
    				String dAns = question.getSelectedAnswers().get(0);
    				if (!dAns.equals("0")){
    					Intent drinkFollowUpScheduler = new Intent(SensorService.ACTION_DRINK_FOLLOWUP);
    					getApplicationContext().sendBroadcast(drinkFollowUpScheduler);
    				}
    				//Log.d(TAG,dAns);
    				
    			}
    		}
    	}
		//answerMap.put(currentQuestion.getId(), currentQuestion.getSelectedAnswers());

    	
    	//Send to service
    	Intent surveyResultsIntent = new Intent();
    	surveyResultsIntent.setAction(INTENT_ACTION_SURVEY_RESULTS);
    	surveyResultsIntent.putExtra(INTENT_EXTRA_SURVEY_NAME, surveyName);
    	surveyResultsIntent.putExtra(INTENT_EXTRA_SURVEY_RESULTS, answerMap);
    	surveyResultsIntent.putExtra(INTENT_EXTRA_COMPLETION_TIME, System.currentTimeMillis());
    	if (surveyName.equalsIgnoreCase("RANDOM_ASSESSMENT")){
    		randomSeq = getIntent().getIntExtra("random_sequence", 0);
    		surveyResultsIntent.putExtra("random_sequence",randomSeq);
    		Log.d("wtest","random's seq in SurveyActivity: "+randomSeq);
    	}
    	this.sendBroadcast(surveyResultsIntent);    	
    	//Alert user
    	Toast.makeText(this, "Survey Complete.", Toast.LENGTH_LONG).show();
    	
    	if(surveyName.equalsIgnoreCase("DRINKING_FOLLOWUP") && surveyFile.equalsIgnoreCase("DrinkingFollowup.xml")){
    		SensorService.drinkUpFlag = false;
    	}
    	cancelAllTimerTask();
    	String EndLog = Calendar.getInstance().getTime().toString()+", "+surveyName+" survey is completed.";
		TransmitData completeSurveyData=new TransmitData();
		completeSurveyData.execute("EventSurvey."+String.valueOf(ID),EndLog);
    	/* Finish, this call is asynchronous, so handle that when
    	 * views need to be changed...
    	 */
    	finish();
    }
    
    public  void cancelAllTimerTask()
	{	
    	if(SensorService.alarmTimer!=null)
		{
	    	CancelTask(SensorService.alarmTask1);
			CancelTask(SensorService.alarmTask2);
			CancelTask(SensorService.alarmTask3);
			CancelTask(SensorService.alarmTask4);
			PurgeTimers(SensorService.alarmTimer);
		}
	}	
    public void CancelTask(TimerTask tTask){
		if  (tTask!=null)
		tTask.cancel();
	}
	public void PurgeTimers(Timer t)
	{
		if(t!=null)
		{
		t.purge();	
		}
	}
    //Get the next question to be displayed
    protected LinearLayout nextQuestion(){
    	SurveyQuestion temp = null;
    	boolean done = false;
    	boolean allowSkip = false;
    	if(currentQuestion != null && !skipTo)
    		skipFrom = currentQuestion.getId();
    	do{
    		if(temp != null)
    			answerMap.put(temp.getId(), null);
    		//Simplest case: category has the next question
    		temp = currentCategory.nextQuestion();
    		//Ricky 2/7
    		//To check whether it is the last Question
    		if(temp!=null){
        		if (temp.getQuestion().equals(LAST_QUESTION_TEXT)){
        			backButton.setText("Cancel");
        			modifyBackButtonFlag = true;
        		}
    		}
    		//Category is out of questions, try to move to next category
    		if(temp == null && (++categoryNum < cats.size())){
    			/* Advance the category.  Loop will get the question
    			 * on next iteration.
    			 */
    			currentCategory = cats.get(categoryNum);
    			if(currentCategory instanceof RandomCategory &&
    					currentQuestion.getSkip() != null){
    				//Check if skip is in category
    				RandomCategory tempCat = (RandomCategory) currentCategory;
    				if(tempCat.containsQuestion(currentQuestion.getSkip())){
    					allowSkip = skipTo = true;
    				}
    				
    			}
    		}
    		//Out of categories, survey must be done
    		else if(temp == null){
    			Log.d("XMLActivity","Should be done...");
    			done = true;
    			break;
    			//surveyComplete();
    		}
    	}while(temp == null ||
    			(currentQuestion != null && currentQuestion.getSkip() != null &&
    			!(currentQuestion.getSkip().equals(temp.getId()) || allowSkip))	);
		/*if(currentQuestion != null){
			answerMap.put(currentQuestion.getId(), currentQuestion.getSelectedAnswers());
		}*/
    	if(done){
    		//surveyComplete();
    		return null;
    	}
    	else{

    		currentQuestion = temp;
    		return currentQuestion.prepareLayout(this);
    	}
    }
    
    protected LinearLayout previousQuestion(){
    	SurveyQuestion temp = null;
    	//Ricky 2/7 restore text of back-button
    	if (modifyBackButtonFlag){
    		backButton.setText("Previous Question");
    		modifyBackButtonFlag = false;
    	}
    	while(temp == null){
    		temp = currentCategory.previousQuestion();
    		Log.d(TAG,"Trying to get previous question");
    		/*
    		 * If temp is null, this category is out of questions,
    		 * we need to go back to the previous category if it exists.
    		 */
    		if(temp == null){
    			Log.d(TAG,"Temp is null, probably at begining of category");
    			/* Try to go back a category, get the question on
    			 * the next iteration.
    			 */
    			if(categoryNum - 1 >= 0){
    				Log.d(TAG,"Moving to previous category");
    				categoryNum--;
    				currentCategory = cats.get(categoryNum);
    			}
    			//First question in first category, return currentQuestion
    			else{
    				Log.d(TAG,"No previous category, staying at current question");
    				temp = currentQuestion;
    			}
    		}
    		/* A question with no answer must have been skipped,
    		 * skip it again.
    		 */
    		else if(temp != null && !temp.validateSubmit()){
    			Log.d(TAG, "No answer, skipping question");
    			temp = null;
    		}
    		
    		if(temp != null && skipTo && !temp.getId().equals(skipFrom)){
    			temp = null;
    		}
    		else if(temp != null && skipTo){
    			skipTo = false;
    			skipFrom = null;
    		}
    		//Else: valid question, it will be returned.
    	}
    	currentQuestion = temp;
    	return currentQuestion.prepareLayout(this);
    }
    
    /* 2014/1/8
     * onBackPressed deal with the condition when back button is pressed
     * android.view.View.OnClickListener() is conflicted with content.DialogInterface.OnClickListener()
     * so here need to write the full path of android.content.DialogInterface.OnClickListener()
     */
	
 	public void onBackPressed(){
 		    new AlertDialog.Builder(this)
 		        .setTitle("Are you sure you want to exit?")
 		        .setMessage("This action will erase the current survey.\r\nYou should complete the survey.")
 		        .setCancelable(false)
 		        .setNegativeButton(android.R.string.no, null)
 		        .setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {

 		            public void onClick(DialogInterface arg0, int arg1) {
 		            	XMLSurveyActivity.super.onBackPressed();
 		            }
 		        }).create().show();
 		
 		return;
 	}
 	
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
	     	Toast.makeText(SurveyActivityContext, errMSG, Toast.LENGTH_LONG).show();
	     	return false;
	      } 
		    
		}
		
	}
	 public static boolean checkDataConnectivity() {
	    	ConnectivityManager connectivity = (ConnectivityManager) SurveyActivityContext
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connectivity != null) {
				NetworkInfo[] info = connectivity.getAllNetworkInfo();
				if (info != null) {
					for (int i = 0; i < info.length; i++) {
						if (info[i].getState() == NetworkInfo.State.CONNECTED) {
							return true;
						}
					}
				}
			}
			return false;
	}
}