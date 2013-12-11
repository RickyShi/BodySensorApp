package edu.missouri.bas.survey.category;

import java.util.ArrayList;
import java.util.List;

import edu.missouri.bas.survey.question.SurveyQuestion;


public class Category implements SurveyCategory{
	
	protected ArrayList<SurveyQuestion> questions;
	protected int nextQuestionNumber = 0;
	protected String questionText;
	
	public Category(){
		questions = new ArrayList<SurveyQuestion>();
	}
	
	public Category(String questionText){
		this.questionText = questionText;
		questions = new ArrayList<SurveyQuestion>();
	}
	
	public Category(String questionText, ArrayList<SurveyQuestion> questions){
		this.questionText = questionText;
		this.questions = new ArrayList<SurveyQuestion>();
		addQuestions(questions);
	}
	
	public Category(String questionText, SurveyQuestion[] questions){
		this.questionText = questionText;
		this.questions = new ArrayList<SurveyQuestion>();
		addQuestions(questions);
	}	
	
	
	public SurveyQuestion nextQuestion(){
		if((nextQuestionNumber) >= questions.size()){
			return null;
		}
		return questions.get(nextQuestionNumber++);
	}
	
	
	public SurveyQuestion previousQuestion(){
		if(nextQuestionNumber == 0)
			return null;
		else
			return questions.get(--nextQuestionNumber);
	}
	
	
	public SurveyQuestion getQuestion(int index){
		if(index >= questions.size()){
			return null;
		}
		return questions.get(index);
	}
	
	
	public void addQuestion(SurveyQuestion question){
		questions.add(question);
	}
	
	
	public void addQuestions(ArrayList<SurveyQuestion> newQuestions){
		questions.addAll(newQuestions);
	}
	
	
	public void addQuestions(SurveyQuestion[] newQuestions){
		for(SurveyQuestion q: newQuestions){
			questions.add(q);
		}
	}
	
	//Ricky 2013/12/10 Add
	public String getCurrentQuestionText(){
		return questionText;
	}
	
	public String getQuestionText(String question){
	
		return questionText;
	}
	
	
	public void setQuestionText(String question){
		this.questionText = question;
	}

	
	public int totalQuestions() {
		return questions.size();
	}

	
	public int currentQuestion() {
		return nextQuestionNumber;
	}
	
	
	public List<SurveyQuestion> getQuestions(){
		return questions;
	}
	
}
