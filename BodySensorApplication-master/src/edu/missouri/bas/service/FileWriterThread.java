package edu.missouri.bas.service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class FileWriterThread implements Runnable{

	LinkedBlockingQueue<FileDataWrapper> writeQueue;
	private volatile boolean running = false;
	
	
	public void run() {
		FileDataWrapper next;
		while(running){
			next = writeQueue.poll();
			if(next != null){
				FileWriter fw;
				try {
					fw = new FileWriter(next.getFile());
					fw.write(next.getData());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else{
				try {
					Thread.sleep(10 * 1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void start(){
		running = true;
	}
	public void stop(){
		running = false;
	}
	public void write(FileDataWrapper d){
		writeQueue.add(d);
	}
}
