package backend.pineapple_ecommerce.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Cấu hình Async Executor cho hệ thống email.
 *
 * <p>Tại sao không dùng SimpleAsyncTaskExecutor (default)?
 * SimpleAsyncTaskExecutor tạo một thread mới cho mỗi lần gọi @Async,
 * không có pool, không có queue — dễ gây resource exhaustion khi nhiều user
 * đặt hàng/đăng ký cùng lúc.
 *
 * <p>ThreadPoolTaskExecutor giải quyết vấn đề trên:
 * - corePoolSize: số thread luôn sẵn sàng trong pool
 * - maxPoolSize: số thread tối đa khi queue đầy
 * - queueCapacity: số task xếp hàng chờ trước khi spawn thêm thread
 * - threadNamePrefix: dễ trace trong log
 *
 * <p>Sizing guide (điều chỉnh theo môi trường):
 * - Dev/staging: core=2, max=5, queue=50
 * - Production: core=5, max=20, queue=200
 *
 * <p>@EnableAsync đặt tại đây — không cần thêm vào main class.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Executor dành riêng cho email — tách biệt với Executor mặc định
     * để tránh email blocking các async task khác trong tương lai.
     */
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("email-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);   // drain queue khi shutdown
        executor.setAwaitTerminationSeconds(30);              // tối đa 30s chờ
        executor.initialize();
        return executor;
    }

    /**
     * Xử lý exception không được catch trong @Async method.
     * Nếu không override, exception sẽ bị nuốt hoàn toàn.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                log.error("[Async] Uncaught exception in method '{}' with params {}: {}",
                        method.getName(), Arrays.toString(params), ex.getMessage(), ex);
    }
}
