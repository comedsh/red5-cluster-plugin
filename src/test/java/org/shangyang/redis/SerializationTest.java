package org.shangyang.redis;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.shangyang.red5.cluster.ScopePoint;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class SerializationTest {

	@Test
	public void testJaksonDerialization() throws JsonParseException, JsonMappingException, IOException{
		
		String serial = "{\"server\":\"10.211.55.8\",\"port\":6379,\"scope\":\"my-first-red5-example\",\"streamName\":\"mystream\"}";
		
		ObjectMapper mapper = new ObjectMapper();
		
		// 注意，使用这个方式来 deserialize, 一定要给 ScopePoint 实现一个默认的 public 的构造函数
		ScopePoint sp = mapper.readValue(serial, ScopePoint.class ); 
		
		assertEquals( sp.getServer(), "10.211.55.8");
		
		assertEquals( sp.getScope(), "my-first-red5-example");
		
		
	}
	
}
