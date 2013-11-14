package edu.missouri.bas.survey.question;

import java.util.ArrayList;

import android.R;
import android.content.Context;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class CoffeeQuestion extends Question {
	TextView counterText;
	boolean answered = false;
	int result = 0;
	
	public CoffeeQuestion(String id){
		this.questionId = id;
		this.questionType = QuestionType.NUMBER;
	}
	
	
	public LinearLayout prepareLayout(Context c) {
		LinearLayout layout = new LinearLayout(c);
		layout.setOrientation(LinearLayout.VERTICAL);
		TextView questionText = new TextView(c);
		questionText.setText(getQuestion());
		questionText.setTextAppearance(c, R.attr.textAppearanceLarge);
		counterText = new TextView(c);
		counterText.setText("How many cups\n"+result + " cup(s)");
		counterText.setTextSize(TypedValue.COMPLEX_UNIT_DIP,18);

		
		SeekBar sb = new SeekBar(c);
		sb.setMax(24);
		sb.setProgress(result);
		sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if(fromUser){
					result = progress;
					counterText.setText("How many cups ?\n"+progress + " cup(s)");
					answered = true;
				}
			}
			public void onStartTrackingTouch(SeekBar seekBar) {		}
			public void onStopTrackingTouch(SeekBar seekBar)  {		}
		});
		
		layout.addView(counterText);
		layout.addView(sb);
		
		return layout;
	}

	
	public boolean validateSubmit() {
		if(answered && result > 0)
			return true;
		return false;
	}
	
	public String getSkip(){
		return null;
	}
	
	
	public ArrayList<String> getSelectedAnswers(){
		ArrayList<String> temp = new ArrayList<String>();
		temp.add(new Integer(result).toString());
		return temp;
	}
	
	
}
