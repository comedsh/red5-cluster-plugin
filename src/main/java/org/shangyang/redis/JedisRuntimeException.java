package org.shangyang.redis;

/**
 * Redis exception handlers for red5 only.
 * 
 * @author 商洋
 *
 */
@SuppressWarnings("serial")
public class JedisRuntimeException extends RuntimeException {

	
	public JedisRuntimeException(String message){
		
		super(message);
		
	}	
	
	public JedisRuntimeException(String message, Throwable throwable){
		
		super(message, throwable);
		
	}
	
	
}
