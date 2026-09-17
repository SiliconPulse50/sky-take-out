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
        //设置key的序列化器（按课程原版:只设 key;value 走默认序列化器,才能存 Integer/对象等任意类型）
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        //key只收String
        return redisTemplate;
    }
}

