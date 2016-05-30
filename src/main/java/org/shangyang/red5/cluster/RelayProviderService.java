package org.shangyang.red5.cluster;

import java.util.LinkedHashSet;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.red5.server.api.scope.IScope;
import org.red5.server.stream.IProviderService;
import org.red5.server.stream.ProviderService;

public class RelayProviderService extends ProviderService{
	
	// 缓存已经成功做过 relay 的 stream. 如果已经缓存了，那就不要再 relay 了.. 简而言之，就是该流当前拉过了就别再拉了
	// TODO 几个事情要考虑，如果当时成功了，并缓存了；但是直播流 stream 断开，这个时候，也得要清空这个 cache 对应的 stream... 不然，就不能再对该 stream 创建 relay 了
	// 重点：一旦该直播流停止，那么相应的 cache 必须清空.. 不然，可能会导致，该流永远不会再拉.. 	
	LinkedHashSet<Node> nodeCache = new LinkedHashSet<Node>(20);
	
	@Override
	public INPUT_TYPE lookupProviderInput(IScope scope, String name, int type) {

		
		IProviderService.INPUT_TYPE sourceType = super.lookupProviderInput(scope, name, type);
		
		/**
		 * 
		 * 如果返回 LIVE_WAIT，两种可能
		 * 1. 正在别的 server 上推着流
		 * 2. 没有任何推流，让用户等待 
		 *    2.1 若是边服务器，需要有一个 job，定时去判断是否有流产生，
		 *        2.1.1 如果有流产生
		 *    	  		a. 该流不是本机产生，那么进行拉流处理，并且销毁该 job
		 *        		b. 该流是由本机产生，那么就什么都不做，并且销毁该 job
		 *        2.2.2 客户端断开连接，记得要销毁 job.
		 *              
		 *    	  
		 * 
		 */
		// 第二个条件，我认为是个 bug, super.lookupProviderInput(scope, name, type) 的 bug, 应该就是 LIVE_WAIT
		// 这里两个条件对应的 play decision 都是 2，注意，其实是不是等待直播，不是由 INPUT_TYPE 来决定，而是由 play decision 来决定的..
		if( IProviderService.INPUT_TYPE.LIVE_WAIT == sourceType || ( IProviderService.INPUT_TYPE.VOD == sourceType && type == -1 ) ){
			
			/*
			 *  protected the stream marked by the key group [appname, streamname] has been pull twice.
			 *  1. if the live stream existed, protected the live stream has been pull twice. -> need the event to remove from nodeCache
			 *  2. if the live stream not existed, protected the scheduler been created twice. -> need the event to remove from nodeCache
			 */
			
			if( nodeCache.add(new Node( scope.getName(), name ) ) == true ){

				RelayRegisterService service = RelayRegisterService.getInstance();
				
				ScopePoint sp = service.find( scope.getName(), name );
				
				if( sp == null ){
					
					// TODO implement the scheduler job to pull the stream periodically.
					
				}else{
				
					RelayService.getInstance().startRelay( sp.getServer(), sp.getPort(), sp.getScope(), sp.getStreamName() );
					
				}
			}
			
			// FIXME: if the live stoped, should remove the key group [appname, streamname] out from cache. 
			
		};
		
		return sourceType;
		
	}
	
}

/**
 * 
 * @author 商洋
 * 
 * @deprecated, uses the {@link RelayRegisterService#generateUniqueCode(String, String) instead} 
 */
class Node{
	
	String appname;
	
	String streamname;
	
	public Node(String appname, String streamname){
		
		this.appname = appname;
		
		this.streamname = streamname;
		
	}

	public String getAppname() {
		return appname;
	}

	public void setAppname(String appname) {
		this.appname = appname;
	}

	public String getStreamname() {
		return streamname;
	}

	public void setStreamname(String streamname) {
		this.streamname = streamname;
	}

	@Override
	public int hashCode() {
		
		return new HashCodeBuilder().append(appname).append(streamname).toHashCode();

	}

	@Override
	public boolean equals(Object obj) {
		
		Node target = (Node) obj;
		
		Node source = this;
		
		return new EqualsBuilder().append( source.getAppname(), target.getAppname() )
								  .append(source.getStreamname(), target.getStreamname())
								  .isEquals();
	}
	
	
	
}


