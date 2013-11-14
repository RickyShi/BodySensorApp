package edu.missouri.bas.survey.question;

import java.util.ArrayList;

import edu.missouri.bas.survey.answer.SurveyAnswer;

import android.content.Context;
import android.widget.LinearLayout;

public interface SurveyQuestion {
	
	public String getQuestion();
	
	public void setQuestion(String questionText);
	
	public QuestionType getQuestionType();
	
	public void setQuestionType(QuestionType type);
	
	public void addAnswer(SurveyAnswer answer);
	
	public void addAnswers(ArrayList<SurveyAnswer> answers);
	
	public void addAnswers(SurveyAnswer[] answers);
	
	public ArrayList<SurveyAnswer> getAnswers();
	
	public LinearLayout prepareLayout(Context c);
	
	public boolean validateSubmit();
	
	public String getSkip();
	
	public String getId();
	
	public ArrayList<String> getSelectedAnswers();
	
}
