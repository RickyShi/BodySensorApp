package edu.missouri.bas.survey;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

/* Author: Paul Baskett
 * Last Update: 9/25/2012
 * Comments Added
 * 
 * Handler for parsing survey configuration files.
 * Using SAX parser for XML parsing.
 */

public class XMLConfigHandler extends DefaultHandler {

	/*
	 * String buffer is used to read text in tags
	 * for example: <tag>text</tag>, "text" will be read
	 * into the buffer.
	 */
	StringBuffer buffer = new StringBuffer();

	/*
	 * Array list that will be passed to activity to be displayed
	 */
	List<SurveyInfo> surveys = new ArrayList<SurveyInfo>();
	
	/*
	 * The wrapper object for the survey that is 
	 * currently being read from the XML file.
	 */
	SurveyInfo currentSurvey;

	/*
	 * TAG used for logging to Android's LogCat.
	 */
	final String TAG = "Survey handler";
	
	
	/*
	 * Reads open tags. Also provides attributes from those tags.
	 * (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes attr){
		
		buffer.setLength(0);
		
		if(localName.equals("item")){
			String file = attr.getValue("file");
			String type = attr.getValue("type");
			String name = attr.getValue("name");
			currentSurvey = new SurveyInfo(file, type, name);
		}
	
	}
	
	/*
	 * Reads closing tags.  At this point, text from between
	 * open and closing tags will be available in buffer.
	 * (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override 
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if(localName.equals("item")){
			currentSurvey.setDisplayName(buffer.toString());
			surveys.add(currentSurvey);
		}
	}
	
	/*
	 * This method actually gets the text from between
	 * the tags.  If you want to use another method for
	 * storing it, do it here.
	 * (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char[] ch, int start, int length){
		buffer.append(ch,start,length);
		//Log.d(TAG,"Got some characters");
	}
	
	/*
	 * Returns the survey list
	 */
	public List<SurveyInfo> getSurveyList() {
		return surveys;
	}
}
