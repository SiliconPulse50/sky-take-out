package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
public class RedisConfiguration {
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("开始创建Redis模版对象");
        RedisTemplate redisTemplate=new RedisTemplate() ;
        //可以设计Redis的连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //设置key的序列化器
        // RedisConfiguration 里补三行(否则在客户端看到的是二进制乱码)
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());      // ★
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());    // ★
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());  // ★
        return redisTemplate;
    }
}

