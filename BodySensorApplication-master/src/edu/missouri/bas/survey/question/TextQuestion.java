package edu.missouri.bas.survey.question;

import java.util.ArrayList;

import android.R;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TextQuestion extends Question{

	String selectedText = "";
	
	public TextQuestion(String id){
		this.questionId = id;
		this.questionType = QuestionType.TEXT;
	}
	
	
	public LinearLayout prepareLayout(Context c) {
		LinearLayout layout = new LinearLayout(c);
		layout.setOrientation(LinearLayout.VERTICAL);
		
		TextView questionText = new TextView(c);
		questionText.setText(this.getQuestion());
		//questionText.setTextAppearance(c, R.attr.textAppearanceLarge);
		questionText.setTextSize(TypedValue.COMPLEX_UNIT_DIP,18);

		
		EditText editText = new EditText(c);
		editText.addTextChangedListener(new TextWatcher(){

			
			public void afterTextChanged(Editable arg0) {
				selectedText = arg0.toString();
			}
			
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
			
		});
		
		layout.addView(questionText);
		layout.addView(editText);
		
		return layout;
	}

	
	public boolean validateSubmit() {
		if(selectedText.length() == 0)
			return false;
		return true;
	}

	
	public String getSkip() {
		return null;
	}
	
	public ArrayList<String> getSelectedAnswers(){
		ArrayList<String> temp = new ArrayList<String>();
		temp.add(selectedText);
		return temp;
	}
}
