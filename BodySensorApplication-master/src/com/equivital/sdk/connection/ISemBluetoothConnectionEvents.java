package com.equivital.sdk.connection;

/**
 * Bluetooth connection events used to provide notification of when a connection succeeds or fails.
 * 
 * @author Steve Riggall, Hidalgo Limited.
 *
 */
public interface ISemBluetoothConnectionEvents
{
	/**
	 * Called when the connection to a bluetooth device fails.
	 * @param message The reported message from the bluetooth stack.
	 */
	public void connectionFailed(String message);
	/**
	 * Called when the connection to a bluetooth device succeeds.
	 */
	public void connectionSucceeded();
}
