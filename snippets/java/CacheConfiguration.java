@EnableCaching
@Configuration
public class CacheConfiguration {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("sensitiveDataCache");
    }
}