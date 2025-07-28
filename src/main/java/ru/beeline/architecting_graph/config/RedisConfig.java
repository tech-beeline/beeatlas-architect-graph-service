package ru.beeline.architecting_graph.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.beeline.architecting_graph.dto.TaskCacheDTO;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, TaskCacheDTO> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, TaskCacheDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}