package edu.missouri.bas.survey.question;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import edu.missouri.bas.survey.answer.SurveyAnswer;


public abstract class Question implements SurveyQuestion {

	protected ArrayList<SurveyAnswer> answers = new ArrayList<SurveyAnswer>();
	protected HashMap<View, SurveyAnswer> answerViews = new HashMap<View, SurveyAnswer>();
	protected String questionText;
	protected String questionId;
	protected QuestionType questionType;
	
	
	public String getQuestion() {
		return questionText;
	}

	
	public void setQuestion(String questionText) {
		this.questionText = questionText;
		
	}

	
	public void addAnswer(SurveyAnswer answer) {
		this.answers.add(answer);
	}

	
	public void addAnswers(ArrayList<SurveyAnswer> answers) {
		this.answers.addAll(answers);
	}

	
	public void addAnswers(SurveyAnswer[] answers) {
		for(SurveyAnswer a: answers){
			this.answers.add(a);
		}
	}

	
	public ArrayList<SurveyAnswer> getAnswers() {
		return answers;
	}

	
	public void setQuestionType(QuestionType type) {
		this.questionType = type;
	}

	
	public QuestionType getQuestionType() {
		return questionType;
	}

	
	public abstract LinearLayout prepareLayout(Context c);

	
	public abstract boolean validateSubmit();
	
	
	public abstract String getSkip();
	
	
	public String getId(){
		return questionId;
	}

}
