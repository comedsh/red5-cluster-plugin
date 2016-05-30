package org.shangyang.red5.cluster;

public class RelayException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 520506927468035943L;

    public RelayException(String message) {
    	
        super(message);
        
    }

    public RelayException(String message, Throwable cause) {
        super(message, cause);
    }
}
