package org.shangyang.redis;

import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.util.Pool;

/**
 * 
 * 处理 redis 的三种连接方式 1. Single Master / Master-Slave 2. Sentinel 3. Cluster
 * 
 * @author 商洋
 *
 */
public class RedisConnectionManager {

	protected String sentinelMaster = null;

	Set<String> sentinelSet = null;

	protected Pool<Jedis> connectionPool;

	protected JedisPoolConfig connectionPoolConfig = new JedisPoolConfig();

	protected int database = 0; // default as 0

	protected JedisCluster jedisCluster;

	protected Set<String> cluster;

	String host = "10.211.55.8"; // FIXME: hard set just for test

	int port = redis.clients.jedis.Protocol.DEFAULT_PORT;

	String password = "fhredispass007"; // FIXME: for test.

	boolean isRedisInitialized = false;
	
	static RedisConnectionManager redisConectionManager;
	
	/**
	 * FIXME: should get performance tuning, using container to initialize it at the init step of the container.
	 * 
	 * @return
	 */
	public static synchronized RedisConnectionManager getInstance(){
		
		if( redisConectionManager == null ){
			
			redisConectionManager = new RedisConnectionManager();
			
			redisConectionManager.initializeRedisConnection();
			
		}
		
		return redisConectionManager;
		
	}
	
	public JedisAdapter getConenction() {
		
		synchronized (this) {

			if (isRedisInitialized == false) {

				initializeRedisConnection();

				isRedisInitialized = true;

			}
		}
		
		if (connectionPool != null) {

			Jedis jedis = connectionPool.getResource();

			if (getDatabase() != 0) {
				
				jedis.select(getDatabase());
				
			}

			return new JedisAdapter(jedis);
		}

		if (jedisCluster != null) {
			
			return new JedisAdapter(jedisCluster);
			
		}

		throw new JedisRuntimeException("no expected connection retrived");
	}

	void initializeRedisConnection() throws JedisRuntimeException {

		try {

			// Way 1: Master-Slave with Sentinel
			if (getSentinelMaster() != null) {

				Set<String> sentinelSet = getSentinelSet();

				if (sentinelSet != null && sentinelSet.size() > 0) {

					connectionPool = new JedisSentinelPool(getSentinelMaster(), sentinelSet, this.connectionPoolConfig, redis.clients.jedis.Protocol.DEFAULT_TIMEOUT, getPassword());

				} else {

					throw new JedisRuntimeException(
							"Error configuring Redis Sentinel connection pool: expected both `sentinelMaster` and `sentiels` to be configured");

				}

			// Way 2: Clusters
			} else if (this.cluster != null) {

				Set<HostAndPort> connectionPoints = new HashSet<HostAndPort>();

				for (String s : cluster) {

					String[] ss = s.split(":");

					connectionPoints.add(new HostAndPort(ss[0], Integer.parseInt(ss[1])));

				}

				jedisCluster = new JedisCluster(connectionPoints);

			// Way 3: Master-Slave
			} else {

				connectionPool = new JedisPool(this.connectionPoolConfig, getHost(), getPort(), redis.clients.jedis.Protocol.DEFAULT_TIMEOUT, getPassword());

			}

		} catch (Exception e) {

			e.printStackTrace();

			throw new JedisRuntimeException("Error connecting to Redis", e);

		}

	}

	public String getSentinelMaster() {
		return sentinelMaster;
	}

	public void setSentinelMaster(String sentinelMaster) {
		this.sentinelMaster = sentinelMaster;
	}

	public Set<String> getSentinelSet() {
		return sentinelSet;
	}

	public void setSentinelSet(Set<String> sentinelSet) {
		this.sentinelSet = sentinelSet;
	}

	public int getDatabase() {
		return database;
	}

	public void setDatabase(int database) {
		this.database = database;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getConnectionPoolMaxTotal() {
		return this.connectionPoolConfig.getMaxTotal();
	}

	public void setConnectionPoolMaxTotal(int connectionPoolMaxTotal) {
		this.connectionPoolConfig.setMaxTotal(connectionPoolMaxTotal);
	}

	public int getConnectionPoolMaxIdle() {
		return this.connectionPoolConfig.getMaxIdle();
	}

	public void setConnectionPoolMaxIdle(int connectionPoolMaxIdle) {
		this.connectionPoolConfig.setMaxIdle(connectionPoolMaxIdle);
	}

	public int getConnectionPoolMinIdle() {
		return this.connectionPoolConfig.getMinIdle();
	}

	public void setConnectionPoolMinIdle(int connectionPoolMinIdle) {
		this.connectionPoolConfig.setMinIdle(connectionPoolMinIdle);
	}

}
