package edu.missouri.bas.survey;

import java.io.IOException;
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.util.Log;
import edu.missouri.bas.survey.answer.Answer;
import edu.missouri.bas.survey.answer.SurveyAnswer;
import edu.missouri.bas.survey.category.Category;
import edu.missouri.bas.survey.category.RandomCategory;
import edu.missouri.bas.survey.category.SurveyCategory;
import edu.missouri.bas.survey.question.CheckQuestion;
import edu.missouri.bas.survey.question.CoffeeQuestion;
import edu.missouri.bas.survey.question.HourQuestion;
import edu.missouri.bas.survey.question.NumberQuestion;
import edu.missouri.bas.survey.question.QuestionType;
import edu.missouri.bas.survey.question.RadioInputQuestion;
import edu.missouri.bas.survey.question.RadioQuestion;
import edu.missouri.bas.survey.question.SurveyQuestion;
import edu.missouri.bas.survey.question.TextQuestion;

/* Author: Paul Baskett
 * Last Update: 9/25/2012
 * Comments Added
 * 
 * Handler for parsing survey configuration files.
 * Using SAX parser for XML parsing.
 */

public class XMLHandler extends DefaultHandler {

	/*
	 * String buffer is used to read text in tags
	 * for example: <tag>text</tag>, "text" will be read
	 * into the buffer.
	 */
	StringBuffer buffer = new StringBuffer();

	/*
	 * Array list that will be passed to activity to be displayed.
	 * Each question from each category will be displayed one
	 * at a time.
	 */
	ArrayList<SurveyCategory> categories = new ArrayList<SurveyCategory>();
	
	/*
	 * Object for the category questions are currently being
	 * added to.
	 */ 
	SurveyCategory category;
	
	/*
	 * Object for the question currently being read.
	 * Question will be added to category once all the
	 * answers for the question have been read.
	 */
	SurveyQuestion question;
	
	/*
	 * Object for the answer currently being read.
	 * Answer will be added to question.
	 */ 
	SurveyAnswer answer;

	/*
	 * Block questions allow one set of answers to 
	 * be applied to every question in the block.
	 * This list will be applied to each question.
	 */ 
	ArrayList<SurveyAnswer> blockAnswerList;
	
	/*
	 * Identifies the type of question being read
	 */
	QuestionType questionType;
	
	/*
	 * baseId is appended to the begining of the
	 * questionId for each question read.  This is
	 * used for externalsource tags where external 
	 * XML files are read.
	 */
	String baseId;
	
	/*
	 * Tag used for LogCat logging.
	 */
	final String TAG = "Question handler";
	
	/*
	 * Context for the application
	 */
	Context appContext;
	
	/*
	 * If true, externalsource tags will be followed.
	 * Currently externalsource tags will only be followed
	 * one level away from the first XML file that was read
	 * to prevent infinite loops.
	 */
	boolean external;
	
	/*
	 * Setup the XML handler.
	 * Takes a context, a boolean to determine if external xml 
	 * tags should be followed, and a baseId that will be added
	 * to the begining of question Ids.  null or "" will cause 
	 * no baseId to be added.
	 */
	public XMLHandler(Context c, boolean allowExternalXML, String baseId){
		
		this.appContext = c;
		this.external = allowExternalXML;
		
		if(baseId != null)
			this.baseId = baseId;
		else 
			this.baseId = "";
	}
	
	/*
	 * Parses opening tags
	 * (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes attr){
		
		buffer.setLength(0);
		
		//Place holder, currently root is not being used.
		if(localName.equals("root")){
			//Do nothing
		}
		
		//Read a category
		else if(localName.equals("category")){
			/*
			 * Category supports two types.
			 * Regular categories where questions are 
			 * 		put in the same order they are read.
			 * Random categories where the order is
			 * 		randomized as questions are added
			 * 		until questions start being read from
			 * 		the category.
			 */
			String s = attr.getValue("type");
			
			if(s != null && s.equals("random")){
				category = new RandomCategory();
			}
			else{
				category = new Category();
			}
		}
		
		//Placeholder
		else if(localName.equals("description")){
			//Do nothing
		}
		
		//Read a question
		else if(localName.equals("question")){
			//Check question type
			String s = attr.getValue("type");
			
			/* No type was specified, set to value
			 * so null strings don't cause null
			 * pointer exceptions when used.
			 */
			if(s == null) s = "invalid";
			
			//Get baseId
			String id = baseId;
			
			//Make sure it's valid for operations
			if(attr.getValue("id") != null)
				id += attr.getValue("id");
			
			/*
			 * Create appropriate question type, default
			 * to checkbox.
			 */
			if(s.equals("radio")){
				question = new RadioQuestion(id);
			}
			else if(s.equals("check")){
				question = new CheckQuestion(id);
			}
			else if(s.equals("number")){
				question = new NumberQuestion(id);
			}
			else if(s.equals("hour")){
				question = new HourQuestion(id);
			}
			else if(s.equals("coffee")){
				question = new CoffeeQuestion(id);
			}
			else if(s.equals("text")){
				question = new TextQuestion(id);
			}
			else if(s.equals("radioinput")){
				question = new RadioInputQuestion(id);
			}
			else{
				question = new CheckQuestion(id);
			}
		}
		
		/*
		 * The text tag is used for question text in both
		 * single question <question> tags and block
		 * <questionblock> tags.  Depending on the usage
		 * it may have an ID attribute.
		 */
		else if(localName.equals("text")){
			//In a question block
			if(blockAnswerList != null){
				
				String id = baseId + attr.getValue("id");
				
				if(questionType.equals(QuestionType.RADIO)){
					question = new RadioQuestion(id);
				}
				else if(questionType.equals(QuestionType.CHECKBOX)){
					question = new CheckQuestion(id);
				}
				else if(questionType.equals(QuestionType.NUMBER)){
					question = new NumberQuestion(id);
				}
				else if(questionType.equals(QuestionType.TEXT)){
					question = new TextQuestion(id);
				}
				else if(questionType.equals(QuestionType.RADIOINPUT)){
					question = new RadioInputQuestion(id);
				}
				else{
					question = new CheckQuestion(id);
				}
			}
		}
		
		//Read an answer
		else if(localName.equals("answer")){
			//Read id
			String id = attr.getValue("id");
			/* Skip is used to skip following questions
			 * until a certain question Id is reached.
			 */
			String skip = attr.getValue("skip");
			
			/*
			 * In check questions, some questions will
			 * uncheck/disable other (i.e. none of the above)
			 * 
			 * In radio button + text input questions,
			 * text input is enabled if certain radio
			 * buttons are selected.
			 * 
			 * Action is used for both of these.
			 */
			String action = attr.getValue("action");
			
			/*
			 * Some questions (currently only radio buttons)
			 * can be used to trigger other surveys at a 
			 * later time.
			 */
			String triggerFile = attr.getValue("triggerFile");
			String triggerName = attr.getValue("triggerName");
			String triggerTimes = attr.getValue("triggerTimes");

			//If answer will trigger another survey, add info
			if(triggerFile != null &&
					triggerName != null &&
					triggerTimes != null){
				
				answer = new Answer(id, triggerName, triggerFile, triggerTimes);
			}
			//Otherwise create a standard answer object
			else{
				answer = new Answer(id);
			}
			
			//Set the answer to skip to a given question
			answer.setSkip(skip);
			
			//Answer has an extra, set flags
			if(action != null){
				if(action.equals("uncheck")){
					answer.setClear(true);
				}
				else if(action.equals("extrainput")){
					answer.setExtraInput(true);
				}
			}
		}
		
		/*
		 * Read a question block.  Works like question
		 * but is more efficient for multiple questions
		 * with the same set of answers.
		 */
		else if(localName.equals("questionblock")){
			blockAnswerList = new ArrayList<SurveyAnswer>();
			String type = attr.getValue("type");
			if(type.equals("radio")){
				questionType = QuestionType.RADIO;
			}
			else if(type.equals("check")){
				questionType = QuestionType.CHECKBOX;
			}
			else if(type.equals("number")){
				questionType = QuestionType.NUMBER;
			}
			else if(type.equals("text")){
				questionType = QuestionType.TEXT;
			}
			else if(type.equals("radioinput")){
				questionType = QuestionType.RADIOINPUT;
			}
			else{
				questionType = QuestionType.CHECKBOX;
			}
		}
		
		//External source tag
		else if(external && localName.equals("externalsource")){
			String fileName = attr.getValue("filename");
			String baseId = attr.getValue("baseid");
			
			//Setup XML parser and handler for new file
			if(fileName != null){
				XMLParser externalXML = new XMLParser();
				try {
					//Add categories from external source
					this.categories.addAll(externalXML.parseQuestion(
							new InputSource(appContext.getAssets().open(fileName)),
							appContext, false, baseId));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	
	}
	
	/*
	 * Read closing tags.
	 * (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override 
	public void endElement(String uri, String localName, String qName) throws SAXException {
	
		//Placeholder
		if(localName.equals("root")){		}
		
		//Category
		else if(localName.equals("category")){
			categories.add(category);
			category = null;
		}
		
		//Description (not shown to user currently)
		else if(localName.equals("description")){
			category.setQuestionText(buffer.toString());
		}
		
		//Question can be added to category
		else if(localName.equals("question")){ 
			category.addQuestion(question);
			question = null;
		}
		
		//Used for question text that user will see
		else if(localName.equals("text")){
			question.setQuestion(buffer.toString());
			if(blockAnswerList != null){
				question.addAnswers(blockAnswerList);
				category.addQuestion(question);
				question = null;
			}
		}
		
		//Finished with an answer
		else if(localName.equals("answer")){
			answer.setAnswer(buffer.toString());
			if(blockAnswerList == null)
				question.addAnswer(answer);
			else{
				//Log.d(TAG,"Finished question block answer list, length: "+blockAnswerList.size());
				blockAnswerList.add(answer);
			}
		}
		else if(localName.equals("questionblock")){
			blockAnswerList = null;
		}
	}
	
	/*
	 * Called for text between open and closing tags
	 * (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char[] ch, int start, int length){
		buffer.append(ch,start,length);
		//Log.d(TAG,"Got some characters");
	}
	
	//Return list to parser
	public ArrayList<SurveyCategory> getCategoryList() {
		return categories;
	}
}
