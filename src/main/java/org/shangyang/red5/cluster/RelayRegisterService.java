package org.shangyang.red5.cluster;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.shangyang.redis.JedisAdapter;
import org.shangyang.redis.RedisConnectionManager;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Relay 注册中心，使用 redis
 * 
 * TODO list as below
 * 1. When the source is disconnect, the register information should be removed from redis
 * 2. 直播的时候，Scope Point 的信息要保存到 redis 中
 * 
 * @author 商洋
 *
 */
public class RelayRegisterService {
	
	private static RelayRegisterService service;
	
	// the Jackson serialize base object.
	static final ObjectMapper mapper = new ObjectMapper();
	
	private RelayRegisterService(){
		
	}
	
	public static synchronized RelayRegisterService getInstance(){
		
		if( service == null ){
			
			service = new RelayRegisterService();
			
		}
		
		return service;
		
	}
	
	/**
	 * 
	 * 注意，直播源流在整个集群环境中应该是唯一的，其对应的 key 只由 scope + stream name 组成. <br> 
	 * 
	 * 添加 address 和 port 参数，是为了边服务器去拉流.
	 * 
	 * @param address 当前源主机的地址，一般为集群中的内网地址
	 * @param port 源主机的 rtmp port
	 * @param scope 源主机对应的 app name
	 * @param streamName 
	 * @throws JsonProcessingException 
	 */
	public void saveLiveInfo(String address, int port, String scope, String streamName) throws RelayException{
		
		String key = this.generateUniqueCode(scope, streamName);
		
		JedisAdapter jedisAdapter = RedisConnectionManager.getInstance().getConenction();
		
		// only when the key is empty, then set.
		if( StringUtils.isEmpty( jedisAdapter.get( key ) ) ){
			
			String serial = this.serialize( new ScopePoint( address, port, scope, streamName ) );
			
			jedisAdapter.set( key, serial );			
			
		}
		
	}

    /** 
     * ObjectMapper 是 JSON 操作的核心，Jackson 的所有 JSON 操作都是在 ObjectMapper 中实现。 
     * ObjectMapper 有多个 JSON 序列化的方法，可以把 JSON 字符串保存 File、OutputStream 等不同的介质中。 
     * 		writeValue( File arg0, Object arg1 ) 把 arg1 转成 json 序列，并保存到 arg0 文件中。 
     * 		writeValue( OutputStream arg0, Object arg1 ) 把 arg1 转成 json 序列，并保存到 arg0 输出流中。 
     * 		writeValueAsBytes( Object arg0 ) 把 arg0 转成 json 序列，并把结果输出成字节数组。 
     * 		writeValueAsString( Object arg0 ) 把 arg0 转成 json 序列，并把结果输出成字符串。
     * 
     * @param 
     * @return the serialized String.. 
     * @throws JsonProcessingException 
     */  
	public String serialize( ScopePoint sp ) throws RelayException{
	    
		String serial = null;
		
		try{

			  
			
			serial = mapper.writeValueAsString(sp);
			
		}catch( JsonProcessingException e){
			
			throw new RelayException(e.getMessage(), e.getCause() );
			
		}
		
		return serial;
				
	}
	
	/**
	 * Uses the return value as the key.
	 * 
	 * @param scope
	 * @param streanName
	 * @return
	 */
	public String generateUniqueCode(String scope, String streamName){
		
		int code = new HashCodeBuilder().append( scope.toCharArray() )
									.append("~".toCharArray()) // 分隔符
									.append( streamName.toCharArray() )
									.toHashCode();
		return code + "";
		
		
	}
	
	/**
	 * 
	 * find the current live stream information from redis. 
	 * 
	 * @param scope
	 * @param streamName
	 * @return
	 */
	public ScopePoint find(String scope, String streamName) {

		JedisAdapter jedisAdapter = RedisConnectionManager.getInstance().getConenction();
		
		String serial = jedisAdapter.get( this.generateUniqueCode(scope, streamName) );
		
		if( !StringUtils.isEmpty( serial ) ){
		
		 try {
			 
			return mapper.readValue(serial, ScopePoint.class);
			
		} catch (JsonParseException e) {
			
			new RelayException( e.getMessage(), e.getCause() );
			
		} catch (JsonMappingException e) {
			
			new RelayException( e.getMessage(), e.getCause() );
			
		} catch (IOException e) {
			
			new RelayException( e.getMessage(), e.getCause() );
		}
		 
		}
		
		return null;
	
	}
	
}


