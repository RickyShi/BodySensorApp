package edu.missouri.bas.survey.question;

import java.util.ArrayList;

import edu.missouri.bas.service.SensorService;
import edu.missouri.bas.survey.answer.SurveyAnswer;

import android.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;


public class RadioQuestion extends Question {
	
	boolean answered;
	String skipTo;
	SurveyAnswer selectedAnswer;
	Context broadcastContext;
	
	public RadioQuestion(String id){
		this.questionId = id;
		this.questionType = QuestionType.RADIO;
	}
	
	
	public LinearLayout prepareLayout(Context c) {
		broadcastContext = c;
		LinearLayout layout = new LinearLayout(c);
		layout.setOrientation(LinearLayout.VERTICAL);
		
		TextView questionText = new TextView(c);
		questionText.setText(getQuestion());
		//questionText.setTextAppearance(c, R.attr.textAppearanceLarge);
		questionText.setTextSize(TypedValue.COMPLEX_UNIT_DIP,20);

		
		RadioGroup radioGroup = new RadioGroup(c);
		radioGroup.setOrientation(RadioGroup.VERTICAL);
				
		for(SurveyAnswer ans: this.answers){
			RadioButton temp = new RadioButton(c);
			temp.setText(ans.getValue());
			temp.setTextSize(TypedValue.COMPLEX_UNIT_DIP,18);
			ans.checkClear();
			
			radioGroup.addView(temp);
			
			if(ans.isSelected()){
				temp.setChecked(true);
				temp.setSelected(true);
			}

			answerViews.put(temp, ans);
			
			
			temp.setOnCheckedChangeListener(new OnCheckedChangeListener(){
				
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					if(isChecked){
						if(selectedAnswer != null) selectedAnswer.setSelected(false);
						SurveyAnswer selected = answerViews.get(buttonView);
						//selected.setSelected(true);
						selectedAnswer = selected;
						skipTo = selected.getSkip();
						answered = true;
					}
				}
			});
		}
		layout.addView(questionText);
		layout.addView(radioGroup);
		return layout;
	}

	
	public boolean validateSubmit() {
		return answered;
	}

	public String getSkip(){
		return skipTo;
	}
	
	
	public ArrayList<String> getSelectedAnswers(){
		ArrayList<String> temp = new ArrayList<String>();
		if(selectedAnswer != null){
			temp.add(selectedAnswer.getId());
			if(selectedAnswer.hasSurveyTrigger()){
				long[] times = selectedAnswer.getTriggerTimes();
				String triggerName = selectedAnswer.getTriggerName();
				String triggerFile = selectedAnswer.getTriggerFile();
				Log.d("RADIO QUESTION","Times: "+times.length);
				int counter = 0;
				for(long time: times){
					Log.d("RadioQuestion","Time: "+time);
					triggerSurvey(time, triggerName, triggerFile, counter++);
				}
			}
			broadcastContext = null;
			return temp;
		}
		return null;
	}
	
	private void triggerSurvey(long time, String triggerFile, 
			String triggerName, int counter){
		Log.d("RADIO QUESTION","Triggering survey number: "+counter);
		AlarmManager manager = 
				(AlarmManager) broadcastContext.getSystemService(Context.ALARM_SERVICE);
		Intent broadcast = new Intent(SensorService.ACTION_SCHEDULE_SURVEY);
		broadcast.putExtra("doing", "trigger");
		broadcast.putExtra(SurveyAnswer.TRIGGER_NAME, triggerName);
		broadcast.putExtra(SurveyAnswer.TRIGGER_FILE, triggerFile);
		broadcast.putExtra(SurveyAnswer.TRIGGER_TIME, time);
		broadcast.putExtra("id"+counter, counter);
		PendingIntent temp = PendingIntent.getBroadcast(
				broadcastContext, counter, broadcast, PendingIntent.FLAG_ONE_SHOT);
		manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime()+time, temp);

	}
}
