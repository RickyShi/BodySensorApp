package edu.missouri.bas.service;

import java.io.File;

public class FileDataWrapper {
	
	private File destinationFile;
	private String dataToWrite;
	
	public FileDataWrapper(File destinationFile, String dataToWrite){
		this.destinationFile = destinationFile;
		this.dataToWrite = dataToWrite;
	}
	
	public File getFile(){
		return destinationFile;
	}
	
	public String getData(){
		return dataToWrite;
	}
}
