package edu.missouri.bas.survey.category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import edu.missouri.bas.survey.question.SurveyQuestion;


public class RandomCategory extends Category {

	public void addQuestion(SurveyQuestion question){
		super.addQuestion(question);
		if(nextQuestionNumber == 0)
			Collections.shuffle(this.questions, new Random(System.currentTimeMillis()));
	}
	
	public void addQuestions(ArrayList<SurveyQuestion> newQuestions){
		super.addQuestions(newQuestions);
		if(nextQuestionNumber == 0)
			Collections.shuffle(this.questions, new Random(System.currentTimeMillis()));
	}
	
	public void addQuestions(SurveyQuestion[] newQuestions){
		super.addQuestions(newQuestions);
		if(nextQuestionNumber == 0)
			Collections.shuffle(this.questions, new Random(System.currentTimeMillis()));
	}
	
	public boolean containsQuestion(String questionId){
		for(SurveyQuestion question: this.questions){
			if(question.getId().equals(questionId))
				return true;
		}
		return false;
	}
}
