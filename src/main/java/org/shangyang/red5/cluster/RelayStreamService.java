package org.shangyang.red5.cluster;

import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.stream.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 接口，当有客户端发送流数据的时候，会提前调用 publish 方法，那么僵直播流的信息保存到 redis 中，便于集群众其它 server 获取信息
 * 
 * handles push scenario, tell the clusters that there is an stream current publishing
 * 
 * @author 商洋
 *
 */
public class RelayStreamService extends StreamService{
	
	private static Logger log = LoggerFactory.getLogger(RelayStreamService.class);
	
	/**
	 * handles push scenario, tell the clusters that there is an stream current publishing
	 * 
	 * TODO, think that, is necessary to check the mode? LIVE、LIVE_RECORD、LIVE_APPEND
	 * 		 check IClientStream.MODE_RECORD、IClientStream.MODE_APPEND、IClientStream.MODE_LIVE.
	 * 	     
	 * 		 my first thinking that is , do not need, because the three of these are all live stream...
	 * 
	 * bad name
	 * 
	 */
	@Override
	public void publish(String name, String mode) {
		
		RelayRegisterService service = RelayRegisterService.getInstance(); 
		
		IConnection conn = Red5.getConnectionLocal(); 
		
		IScope scope = conn.getScope();
		
		// bad name 逻辑，原本的逻辑是，如果当前 Server，已经有一个同名且同 Scope 的的 publishing stream.. 那么会抛出这样的错误
		// 现在的逻辑是，因为我们扩展到了集群，那么集群中只能有一个同名且同 Scope 的 publising stream 在推流，所以，新的逻辑是，在集群中判断是否有 bad name..
		// 从 redis 中取寻找是否有同名且同 scope 的直播流 - 也就是在集群中去找。
		// TODO, need to resolve the security code of the super ?
		if( service.find( scope.getName(), name ) != null ){
			
			StreamService.sendNetStreamStatus( (IStreamCapableConnection) conn, StatusCodes.NS_PUBLISH_BADNAME, name, name, Status.ERROR, conn.getStreamId());
			
            log.error("Bad name {}", name);
            
            return;
		}
		
		super.publish( name, mode );
		
		// todo, save those information into redis. -> handles push scenario, tell the clusters that there is an stream current publishing
		
		// FIXME: makes the real server address and real port
		// server -> 这里必须是本机 IP（一般而言是内网)，这里通过配置文件说明。能否通过自动检测的方式... ？ 比较麻烦..
		
		try {
						
			service.save( "10.211.55.8", 1935, scope.getName(), name );
			
		} catch (RelayException e) {
			
			e.printStackTrace();
		}
		
		
	}
	
	

}
