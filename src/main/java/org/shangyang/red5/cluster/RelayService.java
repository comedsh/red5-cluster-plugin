package org.shangyang.red5.cluster;

/**
 * 
 * 注意，不能用单例模式，因为会 relay 多个流数据。
 * 
 * @author 商洋
 *
 */
public class RelayService {
	
	private static RelayService service;
	
	public static synchronized RelayService getInstance(){
		
		if( service == null ){
			service = new RelayService();
		}
		
		return service;
		
	}
	
	/**
	 * 
	 * @param scope
	 * @param port
	 * @param appName
	 * @param streamName
	 */
	public void startRelay(String server, int port, String appname, String streamname){
		
		RelayPuller puller = new RelayPuller( server, port, appname, streamname );
		
		puller.startRelay();
		
	};
	
	
	
	
	
}
