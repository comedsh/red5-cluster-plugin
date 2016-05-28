package org.shangyang.red5.cluster;

/**
 * 
 * 注意，不能用单例模式，因为会 relay 多个流数据。
 * 
 * @author 商洋
 *
 */
public class RelayService {

	// Q: 为什么不定义 pusher 作为成员变量？因为 puhser 直接被 puller 的回调方法所调用
	
	RelayPuller puller;
	
	/**
	 * 
	 * @param scope
	 * @param port
	 * @param appName
	 * @param streamName
	 */
	public void initRelayPuller(String server, int port, String appname, String streamname){
		
		puller = new RelayPuller( server, port, appname, streamname );
		
		puller.startRelay();
		
	};
	
	
	
	
	
}
