package edu.missouri.bas.service.modules.sensors;

public class AccelerometerReading {
	private float x;
	private float y;
	private float z;
	private String orientation;
	public AccelerometerReading(float x, float y, float z){
		this.x = x;
		this.y = y;
		this.z = z;
		this.orientation = "";
	}
	
	public AccelerometerReading(float x, float y, float z, String orientation){
		this(x, y, z);
		this.orientation = orientation;
	}
	
	public float getX(){
		return x;
	}
	
	public float getY(){
		return y;
	}
	
	public float getZ(){
		return z;
	}
	
	public String getOrientation(){
		return orientation;
	}
	
	public void setOrientation(String orientation){
		this.orientation = orientation;
	}
	
	@Override
	public String toString(){
		return "x: "+x+" y: "+y+" z: "+z+" "+orientation;
	}

	public Object toCSV() {
		return z+","+y+","+x;
	}
}
