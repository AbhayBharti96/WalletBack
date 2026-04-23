package com.loyaltyService.user_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    private final RedisConfig redisConfig = new RedisConfig();
    private final RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

    @Test
    void redisTemplateUsesExpectedSerializers() {
        RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory);

        assertNotNull(template);
        assertNotNull(template.getConnectionFactory());
        assertInstanceOf(StringRedisSerializer.class, template.getKeySerializer());
        assertInstanceOf(StringRedisSerializer.class, template.getHashKeySerializer());
        assertInstanceOf(GenericJackson2JsonRedisSerializer.class, template.getValueSerializer());
        assertInstanceOf(GenericJackson2JsonRedisSerializer.class, template.getHashValueSerializer());
    }

    @Test
    void cacheManagerBuildsNamedAndDefaultCaches() {
        RedisCacheManager cacheManager = redisConfig.cacheManager(connectionFactory);

        assertNotNull(cacheManager);
        assertNotNull(cacheManager.getCache("user-profile"));
        assertNotNull(cacheManager.getCache("another-cache"));
    }
}
