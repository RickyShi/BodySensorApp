package edu.missouri.bas.survey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.xml.sax.InputSource;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import edu.missouri.bas.R;
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
    
    private static final String TAG = "XMLSurveyActivity";
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
        
        
        submitButton.setOnClickListener(new OnClickListener(){
			
			public void onClick(View v) {
				if(currentQuestion.validateSubmit()){
					ViewGroup vg = setupLayout(nextQuestion());
					if(vg != null)
						setContentView(vg);
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
	protected ScrollView setupLayout(LinearLayout layout){
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
			ScrollView sv = new ScrollView(getApplicationContext());
			//Remove submit button from its parent so we can reuse it
			if(submitButton.getParent() != null){
				((ViewGroup)submitButton.getParent()).removeView(submitButton);
			}
			if(backButton.getParent() != null){
				((ViewGroup)backButton.getParent()).removeView(backButton);
			}
			//Add submit button to layout
			layout.addView(submitButton);
			layout.addView(backButton);
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
    	this.sendBroadcast(surveyResultsIntent);    	
    	//Alert user
    	Toast.makeText(this, "Survey Complete.", Toast.LENGTH_LONG).show();
    	
    	if(surveyName.equalsIgnoreCase("DRINKING_FOLLOWUP") && surveyFile.equalsIgnoreCase("DrinkingFollowup.xml")){
    		SensorService.drinkUpFlag = false;
    	}
    	cancelAllTimerTask();
    	
    	/* Finish, this call is asynchronous, so handle that when
    	 * views need to be changed...
    	 */
    	finish();
    }
    
    public static void cancelAllTimerTask()
	{
		if(SensorService.alarmTimer!=null)
		{	
			//Boolean rickytest = alarmTask1.cancel();
			SensorService.alarmTask1.cancel();
			SensorService.alarmTask2.cancel();
			SensorService.alarmTask3.cancel();
			SensorService.alarmTask4.cancel();
			SensorService.alarmTimer.purge();
			//Log.d(TAG, rickytest.toString());
			//alarmTimer.cancel();
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
 		        .setMessage("This action will destroy the current survey.\r\nYou should complete the survey.")
 		        .setNegativeButton(android.R.string.no, null)
 		        .setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {

 		            public void onClick(DialogInterface arg0, int arg1) {
 		            	XMLSurveyActivity.super.onBackPressed();
 		            }
 		        }).create().show();
 		
 		return;
 	}
}