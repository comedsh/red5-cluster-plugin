package org.shangyang.red5.cluster;

import java.util.LinkedHashSet;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.red5.server.api.scope.IScope;
import org.red5.server.stream.IProviderService;
import org.red5.server.stream.PlayEngine;
import org.red5.server.stream.ProviderService;

/**
 * Checks the status of consumer connection, to determine if we need to relay or not.
 * 
 * TODO, v2.0 -> do not use pusher.. direct push from puller
 * 
 * 
 * @author 商洋
 *
 */
public class RelayProviderService extends ProviderService{
	
	// 缓存已经成功做过 relay 的 stream. 如果已经缓存了，那就不要再 relay 了.. 简而言之，就是该流当前拉过了就别再拉了
	// TODO 几个事情要考虑，如果当时成功了，并缓存了；但是直播流 stream 断开，这个时候，也得要清空这个 cache 对应的 stream... 不然，就不能再对该 stream 创建 relay 了
	// 重点：一旦该直播流停止，那么相应的 cache 必须清空.. 不然，可能会导致，该流永远不会再拉.. 	
	LinkedHashSet<Node> replayedCache = new LinkedHashSet<Node>(20);
	
	// TODO Marked as v2.0 to implement, 
	// static ThreadLocal<RelayPuller> pullerThreadLocal = new ThreadLocal<RelayPuller>();
	
	/**
	 * 
	 * The invoke logic checks from {@link PlayEngine#play(org.red5.server.api.stream.IPlayItem, boolean) } <br>
	 * 
	 * Remember that, the LIVE, LIVE_WAIT, VOD mode is not determine by the @see IProviderService.INPUT_TYPE, but determined by playDecision
	 * 
	 * play decision = 0 -> live
	 * play decision = 2 -> live wait
	 * play decision = 1 -> VOD
	 * 
	 */
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
			// handler the pull scenario.
			if( replayedCache.add(new Node( scope.getName(), name ) ) == true ){
				
				// init the puller means we need get pulled from the source live server.
				
				// TODO Marked as v2.0 to implement, 
				// pullerThreadLocal.set( new RelayPuller( "10.211.55.8", 1935, "my-first-red5-example", "mystream" ) );
				
				RelayService.getInstance().startRelay( "10.211.55.8", 1935, "my-first-red5-example", "mystream" );
				
//				RelayRegisterService service = RelayRegisterService.getInstance();
//				
//				ScopePoint sp = service.find( scope.getName(), name );
//				
//				if( sp == null ){
//					
//					// TODO implement the scheduler job to pull the stream periodically. and how to destroy the scheduler
//					
//				}else{
//				
//					RelayService.getInstance().startRelay( sp.getServer(), sp.getPort(), sp.getScope(), sp.getStreamName() );
//					
//				}
			}
			
			// FIXME: if the live stoped, should remove the key group [appname, streamname] out from cache. 
			
		};
		
		return sourceType;
		
	}
	
	/**
	 * 
	 * The invoke logic checks from {@link PlayEngine#play(org.red5.server.api.stream.IPlayItem, boolean) } <br>
	 * 
	 * TODO marked as v2.0 to be implemented
	 */
//	@Override
//	public IMessageInput getLiveProviderInput(IScope scope, String name, boolean needCreate) {
//		
//		// 这里返回的就是 BroadcastScope
//		IBroadcastScope broadcastScope = (IBroadcastScope) super.getLiveProviderInput(scope, name, needCreate);
//		
//		/*
//		 *  TODO, the best way is to retrieve the InMemoryPushPushPipe, but the scope not provided, uses the InMemoryPushPushPipe#pushMessage is the best way for not duplicate
//		 *  the code.
//		 * 
//		 */
//		
//		RelayPuller puller = pullerThreadLocal.get();
//		
//		if( puller != null ){
//			
//			// FIXME: broadcastScope.getConsumers() 得不到.. 因为 PlayEngine#play 方法执行到这里，还没有进行 subscribe.. 
//			// TODO: 想过去想过来，觉得最好还是得到 Pipe 是最好的。看来只有通过反射获取了... 如果通过反射获取，是否是获得原有成员变量的引用还是一份新的拷贝.. 如果是新的拷贝，则还是不得行..
//			// TODO: 是否需要注册 IPipeConnectionListener @see InMemoryPushPushPipe#subscribe(IConsumer consumer, Map<String, Object> paramMap) 以获得变换中的 customers?
//			
//			puller.setPushPushPipe(pipe); // TODO: retrieve it by Java Reflection...
//			
//			puller.setConnectionConsumers( broadcastScope.getConsumers() ); // 得到正在 waiting 该 broadcast living stream 的 consumers. -> 注意是 reference，新的也会被 referenced.
//			
//			puller.startRelay(); // 开始从 source server 上拉流罗。
//		
//		}
//		
//		
//		return broadcastScope;
//		
//	}
	
	
	
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


