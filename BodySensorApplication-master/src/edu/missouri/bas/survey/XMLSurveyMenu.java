package edu.missouri.bas.survey;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xml.sax.InputSource;

import edu.missouri.bas.service.SensorService;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class XMLSurveyMenu extends Activity{
	
	Button morningButton;
	List<SurveyInfo> surveys;
	HashMap<View, SurveyInfo> buttonMap;
	//SurveyInfo currentSurvey;
	
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
							Intent drinkFollowUpScheduler = new Intent(SensorService.ACTION_DRINK_FOLLOWUP);
							getApplicationContext().sendBroadcast(drinkFollowUpScheduler);
						}
						Intent launchSurvey = 
								new Intent(getApplicationContext(), XMLSurveyActivity.class);
						launchSurvey.putExtra("survey_file", temp.getFileName());
						launchSurvey.putExtra("survey_name", temp.getName());
						startActivity(launchSurvey);
					}
				});
				buttonMap.put(b, survey);
			}
		}
		setContentView(scrollView);
		/*morningButton = (Button) findViewById(R.id.morningSurvey);
		
		morningButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getApplicationContext(), XMLSurveyActivity.class);
				i.putExtra("file_name", "morningReport");
				startActivity(i);	
			}
		});*/
	}
}
