package org.shangyang.red5.cluster;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.red5.client.net.rtmp.ClientExceptionHandler;
import org.red5.client.net.rtmp.RTMPClient;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.stream.message.RTMPMessage;

/**
 * 
 * Pull the stream from the source server
 * 
 * 只支持 live 的 relay..
 * 
 * @author 商洋
 *
 */
public class RelayPuller extends RTMPClient{

	RelayPusher pusher;
	
	boolean is_pusher_initialized = false;
	
    private String server; 
    
    private int port = 1935; // default.

    private String appname;

    private String streamname;
    
    private final boolean live = true;

    
    public RelayPuller( String server, String appname, String streamname ){
    	
    	this.server = server;
    	
    	this.appname = appname;
    	
    	this.streamname = streamname;
    	
    }
    
    public RelayPuller( String server, int port, String appname, String streamname ){
    	
    	this.server = server;
    	
    	this.appname = appname;
    	
    	this.port = port;
    	
    	this.streamname = streamname;
    	
    }
    
    /**
     * 开始执行 relay，既从 source server 开始 pull 数据并转发给边服务器
     */
    public void startRelay(){
    	
    	this.setConnectionClosedHandler( connectionClosedHandler );
    	
    	setExceptionHandler( exceptionHandler );
        
        setStreamEventDispatcher( streamEventDispatcher );
        
        connect( server, port, appname, connectCallback );   	
        
        // 后续的方法调用，由一系列的回调方法，由 mina 控制.. 具体看上面的几个 handler 回调方法。
        
    }
    
    /**
     * 处理关闭事件
     */
    Runnable connectionClosedHandler = new Runnable(){
    	
    	public void run(){
    		
    		// FIXME: use log4j instead
            System.out.println("Connection closed");
            
    	}
    	
    };
    
    /**
     * 处理异常事件
     */
    ClientExceptionHandler exceptionHandler = new ClientExceptionHandler() {
    	
        public void handleException(Throwable throwable) {
            
        	throwable.printStackTrace();
            
        }
        
    };
    
    /**
     * 当客户端成功获取流数据后，会回调这个方法，获取服务器上的流数据 (RTMPEvent)，然后定义这个方法的目的是用来如何处理这些数据。
     * 
     * 
     */
    private IEventDispatcher streamEventDispatcher = new IEventDispatcher() {
    	
        public void dispatchEvent(IEvent e) {
        	
        	IRTMPEvent event = (IRTMPEvent) e;
        	
        	// final byte headerDataType = event.getHeader().getDataType();
        	
        	// FIXME: uses log4j instead
            System.out.println(" ============================================ ClientStream.dispachEvent()" + event.toString() );
            
            // not sure, perhaps we don't need the following checks, because some metadata also need to be send. 
            // if( headerDataType == Constants.TYPE_VIDEO_DATA || headerDataType == Constants.TYPE_AUDIO_DATA ){

                RTMPMessage message = RTMPMessage.build( event );
                
                // most important, push this message to the native server.. 
                pusher.pushMessage( message );

            // }
            
            /*
             * 问题描述，
             * 	  1. 因为 pusher.pushMessage 是通过 Mina 发送，而 mina 是异步的，就是说，是新开启的一个线程在处理
             *    2. 而，diapachEvent 是在 BaseRTMPHanlder#messageRecieved() 方法中调用的，而，这个方法的最后一步，要执行 message.release()，要把流数据清空。
             *    
             *    正是因为 #1 和 #2 是异步执行的，所以可能会导致在 #1 最终发送数据之前，而数据被#2清空了
             */
            // 用摄像头作为源，播放直播的时候，relay message 有可能因为现成的原因丢失掉.. 设置一个时间点，让 message 能够被顺利的 push 出去
            // 下面只是一个临时的解决方案，完整的解决方案，必须是，clone, 可以试试 BeanUtils.    
            try {
				TimeUnit.MILLISECONDS.sleep(100);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
        
    };

    private IPendingServiceCallback methodCallCallback = new IPendingServiceCallback() {
    	
        public void resultReceived(IPendingServiceCall call) {
        	
            System.out.println("methodCallCallback");
            
            Map<?, ?> map = (Map<?, ?>) call.getResult();
            
            System.out.printf("Response %s\n", map);
            
        }
        
    };
    
    /**
     * 
     */
    private IPendingServiceCallback connectCallback = new IPendingServiceCallback() {
    	
        public void resultReceived(IPendingServiceCall call) {
        	
            System.out.println("connectCallback");
            
            ObjectMap<?, ?> map = (ObjectMap<?, ?>) call.getResult();
            
            String code = (String) map.get("code");
            
            System.out.printf("Response code: %s\n", code);
            
            if ("NetConnection.Connect.Rejected".equals(code)) {
            	
                System.out.printf("Rejected: %s\n", map.get("description"));
                
                disconnect();
                
            } else if ("NetConnection.Connect.Success".equals(code)) {
            	
                invoke("demoService.getListOfAvailableFLVs", new Object[] {}, methodCallCallback);
                
                createStream( createStreamCallback );
                
            }
            
        }
    };

    private IPendingServiceCallback createStreamCallback = new IPendingServiceCallback() {
    	
        public void resultReceived(IPendingServiceCall call) {
        	
            Number streamId = (Number) call.getResult();
            
            // the live is hard code, only supports for the live relay
            if ( live ) {
            	
                conn.ping(new Ping(Ping.CLIENT_BUFFER, streamId, 500));
                
                // 这里是发送 play 指令给服务器，要求服务器开始返回流数据，表示客户端准备开始拉流了哦...
                play( streamId, streamname, -1, -1 );
                
                // 在确认要开始从服务器上拉流后，开始初始化 relay pusher.
                initRelayPusher();
                
                
            } else {
            	
                conn.ping(new Ping(Ping.CLIENT_BUFFER, streamId, 4000));
                
                play( streamId, streamname, 0, -1 );
                
            }
        }

    };
    
    void initRelayPusher(){
    	
    	// in case of double time intialized.. 
    	if( is_pusher_initialized == false ){
    		
    		// make the pusher connecting with server ready. 
    		
    		pusher = new RelayPusher( this.appname, this.streamname );
    		
    		is_pusher_initialized = true;
    		
    	}
    	
    };
    
    @SuppressWarnings("unchecked")
    protected void onCommand(RTMPConnection conn, Channel channel, Header header, Notify notify) {
    	
        super.onCommand(conn, channel, header, notify);
        
        System.out.println("onInvoke - header: " + header.toString() + " notify: " + notify.toString());
        
        Object obj = notify.getCall().getArguments().length > 0 ? notify.getCall().getArguments()[0] : null;
        
        if (obj instanceof Map) {
        	
            Map<String, String> map = (Map<String, String>) obj;
            
            String code = map.get("code");
            
            if (StatusCodes.NS_PLAY_STOP.equals(code)) {
            	
                disconnect();
                
                System.out.println("Disconnected");
                
            }
        }

    }

}

