package edu.missouri.bas.survey.category;

import java.util.ArrayList;
import java.util.List;

import edu.missouri.bas.survey.question.SurveyQuestion;



public interface SurveyCategory {
	
	public SurveyQuestion nextQuestion();
	
	public SurveyQuestion previousQuestion();

	public SurveyQuestion getQuestion(int index);
	
	public void addQuestion(SurveyQuestion question);

	public void addQuestions(ArrayList<SurveyQuestion> newQuestions);
	
	public void addQuestions(SurveyQuestion[] newQuestions);
	
	public void setQuestionText(String question);
	
	public String getQuestionText(String question);
	
	public int totalQuestions();
	
	public int currentQuestion();
	
	public List<SurveyQuestion> getQuestions();
}
