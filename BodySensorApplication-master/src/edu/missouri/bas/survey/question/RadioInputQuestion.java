package edu.missouri.bas.survey.question;

import java.util.ArrayList;

import edu.missouri.bas.survey.answer.SurveyAnswer;

import android.R;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class RadioInputQuestion extends Question{
	
	String skipTo;
	String selectedText = "";
	SurveyAnswer selectedAnswer;
	EditText editText;
	
	public RadioInputQuestion(String id){
		this.questionId = id;
		this.questionType = QuestionType.RADIOINPUT;
	}
	
	
	public ArrayList<String> getSelectedAnswers() {
		ArrayList<String> temp = new ArrayList<String>();
		temp.add(selectedAnswer.getId()+"/"+selectedText);
		return temp;
	}

	
	public LinearLayout prepareLayout(Context c) {
		
		LinearLayout layout = new LinearLayout(c);
		layout.setOrientation(LinearLayout.VERTICAL);
		
		TextView questionText = new TextView(c);
		questionText.setText(getQuestion());
		//questionText.setTextAppearance(c, R.attr.textAppearanceLarge);
		questionText.setTextSize(TypedValue.COMPLEX_UNIT_DIP,20);

		
		RadioGroup radioGroup = new RadioGroup(c);
		radioGroup.setOrientation(RadioGroup.VERTICAL);
		
		//RadioButton[] buttons = new RadioButton[this.answers.size()];
		
		editText = new EditText(c);
		editText.setEnabled(false);
		editText.addTextChangedListener(new TextWatcher(){
			
			public void afterTextChanged(Editable arg0) {
				selectedText = arg0.toString();
			}
			
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }	
		});
		
		for(SurveyAnswer ans: this.answers){
			RadioButton temp = new RadioButton(c);
			temp.setText(ans.getValue());
			temp.setTextSize(TypedValue.COMPLEX_UNIT_DIP,15);

			radioGroup.addView(temp);
			//temp.setText(ans.getAnswerText());

			answerViews.put(temp, ans);
			
			temp.setOnCheckedChangeListener(new OnCheckedChangeListener(){
				
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					if(isChecked){
						if(selectedAnswer != null) selectedAnswer.setSelected(false);
						SurveyAnswer selected = answerViews.get(buttonView);
						selected.setSelected(true);
						selectedAnswer = selected;
						skipTo = selected.getSkip();
						if(selectedAnswer.getExtraInput()){
							editText.setEnabled(true);
						}
						else{
							editText.setEnabled(false);
						}
					}
				}
			});
		}
		layout.addView(questionText);
		layout.addView(radioGroup);
		layout.addView(editText);
		return layout;
		
		
	}

	
	public boolean validateSubmit() {
		if(selectedAnswer == null) return false;
		boolean extraInput = selectedAnswer.getExtraInput();
		if((!extraInput) || (extraInput && selectedText.length() > 0)){
			return true;
		}
		return false;
	}

	
	public String getSkip() {
		return skipTo;
	}

}
