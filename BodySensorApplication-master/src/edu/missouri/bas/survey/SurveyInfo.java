package edu.missouri.bas.survey;

/* Author: Paul Baskett
 * Last Update: 9/25/2012
 * Comments Added
 *
 * This class is used to wrap information about surveys that
 * will be displayed when selected from the menu.  Data is 
 * read from an xml configuration file (config.xml currently)
 * and wrapped in one of these objects.  Accessor methods are
 * provided to get all of the stored data, and a setter is
 * provided to set the display name because of the XML format
 * and the way the XML parser reads data from tags.
 */
public class SurveyInfo {

	//Instance variables
	protected String surveyName;
	protected String surveyFile;
	protected String surveyType;
	protected String surveyDisplayName;
	
	/*
	 * Takes the survey location (in assets folder),
	 * survey type (not currently used),
	 * and survey name (used for storage, not displayed for user)
	 */
	public SurveyInfo(String surveyFile, String surveyType,
			String surveyName){
		this.surveyFile = surveyFile;
		this.surveyType = surveyType;
		this.surveyName = surveyName;
	}
	
	/*
	 * This method is provided to set the displayed name for
	 * the survey (for example, "Morning Report").  The XML
	 * parser reads the text in the tag after this object 
	 * has been created, so the name is set later.
	 */
	public void setDisplayName(String name){
		this.surveyDisplayName = name;
	}
	
	/*
	 * Returns the name that should be displayed to the user.
	 */
	public String getDisplayName(){
		return this.surveyDisplayName;
	}
	
	/*
	 * The internal name for the survey, the user doesn't see this.
	 */
	public String getName(){
		return this.surveyName;
	}
	
	/*
	 * The name of the file for the survey.  All surveys
	 * are stored in assets so no path is necessary currently.
	 */
	public String getFileName(){
		return this.surveyFile;
	}
}
