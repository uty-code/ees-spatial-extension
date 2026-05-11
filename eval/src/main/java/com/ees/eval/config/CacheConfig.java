package com.ees.eval.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
            // 부서 트리 데이터: 변경 빈도 낮음, 1시간 유지
            new CaffeineCache("departments", Caffeine.newBuilder()
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    .maximumSize(200)
                    .build()),
            
            // 단순 부서 목록(드롭다운용): 변경 빈도 낮음, 1시간 유지
            new CaffeineCache("departments-simple", Caffeine.newBuilder()
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    .maximumSize(200)
                    .build()),

            // 직급 목록: 변경 빈도 매우 낮음, 1시간 유지
            new CaffeineCache("positions", Caffeine.newBuilder()
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    .maximumSize(100)
                    .build()),

            // 평가 차수: 가끔 변경됨 (상태 전이 등), 10분 유지 및 명시적 Evict
            new CaffeineCache("eval-periods", Caffeine.newBuilder()
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .maximumSize(50)
                    .build()),

            // 사원 선택기 전체 사원 목록: 100건, 5분 유지
            new CaffeineCache("employee-selector", Caffeine.newBuilder()
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .maximumSize(50)
                    .build()),

            // 대시보드 통계용 (사원수, 잠금 계정 수 등): 실시간성 낮음, 5분 유지
            new CaffeineCache("dashboard-counts", Caffeine.newBuilder()
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .maximumSize(20)
                    .build())
        ));

        return cacheManager;
    }
}
