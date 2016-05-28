package org.shangyang.red5.cluster;

import java.util.ArrayList;

import org.red5.client.net.rtmp.INetStreamEventHandler;
import org.red5.client.net.rtmp.RTMPClient;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.messaging.IMessage;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.stream.message.RTMPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * 
 * 
 * 
 * @author shangyang
 *
 */
public class RelayPusher extends RTMPClient implements INetStreamEventHandler, IPendingServiceCallback, IEventDispatcher {  
	
	static final Logger logger = LoggerFactory.getLogger(RelayPusher.class); 
	
	final String server = "127.0.0.1"; // hard code this 我的想法是，只让 relay 发生在同一台服务器.. TODO，想一想有必要放开吗？
	
	String appname;
	
	String streamname;
	
	final String LIVE_MODE = "live"; // 制作 live 的 relay
	
	int port = 1935;  // the RTMP port, make it as default.
	
	boolean readyForPush = false;
	
	// 服务器会为每一个 Stream (流媒体产生的流) 分配一个唯一的 ID
	Number streamId = 0;
	
	public RelayPusher(String appname, String streamname ){
		
		super();
		
		this.appname = appname;
		
		this.streamname = streamname;
		
		connect();
		
	}
	
	public RelayPusher(String appname, String streamname, int port ) {
		
	    super();
	    
	    this.appname = appname;
	    
	    this.streamname = streamname;
	    
	    this.port = port;
	    
	    connect();
	    
	}  
	
	void connect(){
		
		super.connect(server, port, makeDefaultConnectionParams( server, port, appname ), this);
		
	}
	
	/**
	 * any results returned from the server, handle those.
	 */
	@SuppressWarnings("unchecked")
	public void resultReceived( IPendingServiceCall call ) { 
		
	    Object result = call.getResult();
	    
	    if (result instanceof ObjectMap) {
	    	
	        if ("connect".equals(call.getServiceMethodName())) {  
	        	
	        	System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> connect success, send create steam command to server");
	        	
	            createStream(this);
	            
	        }  
	        
	    } else {
	    	
	        if ("createStream".equals( call.getServiceMethodName()) ) {
	        	
	        	System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> create stream success, send the publish command to server");
	        	
	        	// 如果有返回值，返回值一定是一个 @See Number 类型
	            if ( result instanceof Number ) {
	            	
	            	/*
	            	 * 获取服务器端分配的 stream id @TODO 了解 stream id 的创建过程
	            	 * 发送 publish 指令给服务器端，告诉服务器，我准备要 publish 了
	            	 */
	                streamId = (Number) result;
	                
	                // 告诉服务器，我准备要 push live stream 了。
	                publish( streamId, streamname, LIVE_MODE, this );
	                
//	                invoke( "getRoomsInfo", this ); // TODO, relay for the room, but so far, I'm not quite understand what is means of room
	                
	            } else {
	            	
	                disconnect();
	                
	            }  
	            
	        } else if ("getRoomsInfo".equals(call.getServiceMethodName())) {
	        	
	            ArrayList<String> list = (ArrayList<String>) result;
	            
	            for (int i = 0; i < list.size(); i++) {
	            	
	                System.out.println(list.get(i));
	                
	            }  
	            
	        }  
	    }  
	}  

    @Override 
	public void connectionOpened(RTMPConnection conn) { 
	  
    	super.connectionOpened(conn);
	  
	} 
	
	public void onStreamEvent( Notify notify ) {   
			
		logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ready, pulish the stream data from frameBuffer ");
		
	    ObjectMap<?, ?> map = (ObjectMap<?, ?>) notify.getCall().getArguments()[0];
	    
	    String code = (String) map.get("code");
	    
	    logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  the code is  " + code );
	    
	    if (StatusCodes.NS_PUBLISH_START.equals(code)) {
	    	
	    	readyForPush = true; // just mark that, now the pusher is ready for push.. let's the puller to invoke the actual push 
	    	
	    }
		    
	}  
	
	// Must wait ready for push before push the message
	// really start push the message to the server.
	public void pushMessage( IMessage message ){
	
		if( readyForPush ){
			// getChannelForStreamId(streamId) 这个方法比较有趣，原本以为，程序里面会保存一个 Map 之类的对象，以用来记录 stream id 和 channel id 之间的对应关系
			// 殊不知，作者是写了一个固定的算法，一个唯一的 stream id 只会导出一个唯一的 channel id 
			int channelId = super.getChannelForStreamId( streamId );
			
			org.red5.server.net.rtmp.Channel channel = super.getConnection().getChannel(channelId);
			
			channel.write(  ( ( RTMPMessage ) message ).getBody() );
			
			logger.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  write the message to server successfull <<<<<<<<<<<<<<<<<<<<<<<< ");
			
		}else{
			
			// do nothing
			
		}
	}
	
	// 实现接口方法
	public void dispatchEvent(IEvent event) {
		
		System.out.println(" dispatch the events ");
		
	}
	
	//TODO allow self define the port
	public void setPort(int port){
		
		this.port = port;
		
	}
	
}