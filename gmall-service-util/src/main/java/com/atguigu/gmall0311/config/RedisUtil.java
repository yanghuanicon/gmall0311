package com.atguigu.gmall0311.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {
    private JedisPool jedisPool;

    public void initJedisPool(String host,int port,int database){
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //总数
        jedisPoolConfig.setMaxTotal(200);
        //获取连接时等待的最大毫秒
        jedisPoolConfig.setMaxWaitMillis(10*1000);
        //最小剩余数
        jedisPoolConfig.setMinIdle(10);
        //如果最大数设置等待
        jedisPoolConfig.setBlockWhenExhausted(true);
        //在获取连接时检查是否有效
        jedisPoolConfig.setTestOnBorrow(true);
        //创建连接池
        jedisPool = new JedisPool(jedisPoolConfig,host,port,20*1000);

    }
    public Jedis getJedis(){
        Jedis jedis =jedisPool.getResource();
        return jedis;
    }
}
