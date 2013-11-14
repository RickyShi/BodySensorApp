package edu.missouri.bas.survey;

import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;


/* Author: Paul Baskett
 * Last Update: 9/25/2012
 * Comments Added
 * 
 * Sets up XML reader and parser, and gives XML
 * file to parser for handling.  Returns resulting
 * list of surveys when finished.
 */
public class XMLConfigParser {
	
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
	public List<SurveyInfo> parseQuestion(InputSource XML){
		try{
			XMLReader reader = initializeReader();
			
			XMLConfigHandler xmlHandler = new XMLConfigHandler();
			
			reader.setContentHandler(xmlHandler);
			reader.parse(XML);		
			
			return xmlHandler.getSurveyList();
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
}
