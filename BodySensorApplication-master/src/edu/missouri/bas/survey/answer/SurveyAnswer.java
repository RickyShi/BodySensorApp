package edu.missouri.bas.survey.answer;

public interface SurveyAnswer {
	
	public static final String TRIGGER_NAME = "TRIGGER_NAME";
	public static final String TRIGGER_FILE = "TRIGGER_FILE";
	public static final String TRIGGER_TIME = "TRIGGER_TIME";
	
	public String getId();
	
	public String getValue();
	
	public String getAnswerText();
	
	public void setAnswer(String answer);
	
	public void setSelected(boolean selected);
	
	public boolean isSelected();
	
	public void setClear(boolean clear);
	
	public boolean checkClear();
	
	public void setSkip(String id);
	
	public String getSkip();
	
	public void setExtraInput(boolean extraInput);
	
	public boolean getExtraInput();
	
	public void setSurveyTrigger(String name, String location, String times);
	
	public boolean hasSurveyTrigger();
	
	public String getTriggerName();
	
	public String getTriggerFile();
	
	public long[] getTriggerTimes();
	
	public boolean equals(SurveyAnswer ans);
}
