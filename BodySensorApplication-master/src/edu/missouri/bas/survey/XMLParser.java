package edu.missouri.bas.survey;

import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.Context;
import edu.missouri.bas.survey.category.SurveyCategory;

/* Author: Paul Baskett
 * Last Update: 9/25/2012
 * Comments Added
 * 
 * Sets up XML reader and parser, and gives XML
 * file to parser for handling.  Returns resulting
 * list of survey categories when finished.
 */
public class XMLParser {
	
	/*
	 * Setup SAX Parser
	 */
	private XMLReader initializeReader() throws SAXException, ParserConfigurationException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XMLReader reader = parser.getXMLReader();
		return reader;
	}
	
	/*
	 * Parse InputSource xml file to list of SurveyInfo
	 * wrapper objects.
	 */
	public ArrayList<SurveyCategory> parseQuestion(InputSource XML,
			Context c, boolean allowExternalXML, String baseId){
		try{
			XMLReader reader = initializeReader();
			
			XMLHandler questionHandler = new XMLHandler(c, allowExternalXML, baseId);
			
			reader.setContentHandler(questionHandler);
			reader.parse(XML);		
			
			return questionHandler.getCategoryList();
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
}
