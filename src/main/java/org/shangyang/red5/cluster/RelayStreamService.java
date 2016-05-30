package org.shangyang.red5.cluster;

import org.red5.server.stream.StreamService;

/**
 * 
 * 接口，当有客户端发送流数据的时候，会提前调用 publish 方法，那么僵直播流的信息保存到 redis 中，便于集群众其它 server 获取信息
 * 
 * @author 商洋
 *
 */
public class RelayStreamService extends StreamService{
	
	
	/**
	 * TODO, think that, is necessary to check the mode? LIVE、LIVE_RECORD、LIVE_APPEND
	 * 		 check IClientStream.MODE_RECORD、IClientStream.MODE_APPEND、IClientStream.MODE_LIVE.
	 * 	     
	 * 		 my first thinking that is , do not need, because the three of these are all live stream...
	 */
	@Override
	public void publish(String name, String mode) {
		
		super.publish(name, mode);
		
		// todo, save those information into redis.
		
	}
	
	

}
