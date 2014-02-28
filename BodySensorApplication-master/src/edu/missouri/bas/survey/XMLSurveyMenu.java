package edu.missouri.bas.survey;


import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.xml.sax.InputSource;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import edu.missouri.bas.R;
import edu.missouri.bas.service.SensorService;

/**
 * @author Ricky 2013/12/14
 * Here Change the Activity to FragmentActivity to enable the use of AlertDialog 
 */
public class XMLSurveyMenu extends FragmentActivity{
	
	Button morningButton;
	List<SurveyInfo> surveys;
	HashMap<View, SurveyInfo> buttonMap;
	//SurveyInfo currentSurvey;
	
	private static final String INITIAL_DRINK_FILE = "InitialDrinkingParcel.xml";
	private static final String INITIAL_DRINK_NAME = "INITIAL_DRINKING";
	
	private class FireMissilesDialogFragment extends DialogFragment {
	    @Override
	    public Dialog onCreateDialog(Bundle s) {
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setMessage(R.string.first_drink_check)
	        	   .setTitle(R.string.first_drink_title)
	               .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   Intent drinkFollowUpScheduler = new Intent(SensorService.ACTION_DRINK_FOLLOWUP);
	                	   XMLSurveyMenu.this.getApplicationContext().sendBroadcast(drinkFollowUpScheduler);
	                	   dialog.dismiss();	                	   
	                	   //Intent launchInitalDrink = 
						   //		new Intent(getApplicationContext(), XMLSurveyActivity.class);
	                	   Intent launchInitalDrink =new Intent(XMLSurveyMenu.this ,SurveyPinCheck.class);
	                	   launchInitalDrink.putExtra("survey_file", INITIAL_DRINK_FILE);
	                	   launchInitalDrink.putExtra("survey_name", INITIAL_DRINK_NAME);
	                	   launchInitalDrink.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	                	   XMLSurveyMenu.this.startActivity(launchInitalDrink);
	                	    
	                	   //Toast.makeText(getApplicationContext(), "Fire",Toast.LENGTH_LONG).show();
	                   }
	               })
	               .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       // User cancelled the dialog
	                	   dialog.cancel();
	                   }
	               });
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	} 
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.survey_menu);
		
		ScrollView scrollView = new ScrollView(this);
		LinearLayout linearLayout = new LinearLayout(this);
		//linearLayout.addView(new Button(this));
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		scrollView.addView(linearLayout);
		
		//surveys = new ArrayList<SurveyInfo>();
		buttonMap = new HashMap<View, SurveyInfo>();
		
		XMLConfigParser configParser = new XMLConfigParser();
		
		//Try to read surveys from give file
		try {
			surveys = configParser.parseQuestion(new InputSource(getAssets().open("config.xml")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(surveys == null){
			Toast.makeText(this, "Invalid configuration file", Toast.LENGTH_LONG).show();
			Log.e("XMLSurveyMenu","No surveys in config.xml");
			finish();
		}
		else{
			setTitle("Self-Assessment Survey Menu");
			TextView tv = new TextView(this);
			tv.setText("Select a survey");
			linearLayout.addView(tv);
			for(SurveyInfo survey: surveys){
				Button b = new Button(this);
				b.setText(survey.getDisplayName());
				linearLayout.addView(b);
				b.setOnClickListener(new OnClickListener(){
					
					public void onClick(View v) {
						SurveyInfo temp = buttonMap.get(v);
						//once click the initial drinking survey, the drinking follow MSG will be broadcast.
						if (temp.getDisplayName().equals("Initial Drinking")){
							FireMissilesDialogFragment TestDialog = new FireMissilesDialogFragment();
							TestDialog.setCancelable(false);
							TestDialog.show(getSupportFragmentManager(), "drink_check");
						} else if(temp.getDisplayName().equals("Morning Report")) {
							Calendar mT = Calendar.getInstance();
							Calendar noonT = Calendar.getInstance();
							noonT.set(Calendar.HOUR_OF_DAY, 12);
							noonT.set(Calendar.MINUTE, 0);
							noonT.set(Calendar.SECOND, 0);
							if (mT.after(noonT)){
								moringCheck();
							}
							else {
								launchSurvey(temp.getFileName(),temp.getName());
							}
						}
						else {
							launchSurvey(temp.getFileName(),temp.getName());
						}
					}
				});
				buttonMap.put(b, survey);
			}
		}
		setContentView(scrollView);				
	}
	private void moringCheck(){
	    new AlertDialog.Builder(this)
	        .setTitle("It's too late to do the Morning Survey.")
	        .setMessage("You should do the morning survey before 12:00 P.M.")
	        .setCancelable(false)
	        .setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {

	            public void onClick(DialogInterface arg0, int arg1) {
	            	arg0.cancel();
	            }
	        }).create().show();
	
	return;
	}
	private void launchSurvey(String FileName, String Name){
		Intent launchSurvey = 
				new Intent(getApplicationContext(), SurveyPinCheck.class);
		launchSurvey.putExtra("survey_file", FileName);
		launchSurvey.putExtra("survey_name", Name);
		startActivity(launchSurvey);
	}
}
