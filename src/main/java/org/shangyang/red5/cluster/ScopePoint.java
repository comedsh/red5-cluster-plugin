package org.shangyang.red5.cluster;

/**
 * 
 * the unique information for the current live stram.
 * 
 * @author 商洋
 *
 */
public class ScopePoint {

	String server; // the server address. always be the intranet ip address..
	
	int port;
	
	String scope;
	
	String streamName;
	
	public ScopePoint(){
		
	}
	
	public ScopePoint(String server, int port, String scope, String streamName){
		
		this.server = server;
		
		this.port = port;
		
		this.scope = scope;
		
		this.streamName = streamName;		
		
	}
	
	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getStreamName() {
		return streamName;
	}

	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
}
