package org.shangyang.red5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.shangyang.red5.cluster.RelayException;
import org.shangyang.red5.cluster.RelayRegisterService;
import org.shangyang.red5.cluster.ScopePoint;
import org.shangyang.redis.JedisAdapter;
import org.shangyang.redis.RedisConnectionManager;

public class RelayRegisterServiceTest {
	
	/**
	 * 这个测试用例，用 HashCodeBuilder 测试没有通过。<br>
	 * 
	 * 用以下的方式.. 
	 * 
	 * new HashCodeBuilder().append( scope.toCharArray() ).append( streamName.toCharArray() ).toHashCode();
	 * 
	 * 解决办法， <br>
	 * 
	 * 在 scope 和 stream name 之间要加上一个分隔符，加上分隔符以后，测试通过，这里我用的分隔符为 ~
	 * 
	 */
	@Test
	public void testGenerateUniqueCode(){
		
		RelayRegisterService service = RelayRegisterService.getInstance();
		
		String scope = "hello";
		
		String streamName  = "world";
		
		String code1 = service.generateUniqueCode(scope, streamName);
		
		String code2 = service.generateUniqueCode(scope, streamName);
		
		assertEquals("两次生成的code应该相等", code1, code2 ); 
		
		System.out.println("code1 is: " + code1 );
		
		scope = "hell";
		
		streamName = "oworld";
		
		String code3 = service.generateUniqueCode(scope, streamName);
		
		System.out.println("code3 is: " + code3 );
		
		assertNotEquals("两次生成的code应该不相等", code1, code3);
		
	}
	
	@Test
	public void testSerialize() throws RelayException{
		
		JedisAdapter jedisAdapter = RedisConnectionManager.getInstance().getConenction();
		
		String server = "10.211.55.8";
		
		int port = redis.clients.jedis.Protocol.DEFAULT_PORT;
		
		String scope = "myscope";
		
		String streamName = "mystream";
		
		RelayRegisterService service = RelayRegisterService.getInstance();
		
		String key = service.generateUniqueCode(scope, streamName);
		
		System.out.println("the generate key:"+ key);
				
		// first clean up the key from redis
		jedisAdapter.del(key);
		
		ScopePoint sp = new ScopePoint( server, port, scope, streamName );
		
		String serial = service.serialize(sp);
		
		System.out.println("json serial:"+ serial);
		
		jedisAdapter.set(key, serial);
		
	}
	
}
