package com.agentguard.alert.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * 通知渠道抽象基类
 * 提供重试机制的通用实现
 *
 * @author zhuhx
 */
@Slf4j
public abstract class AbstractNotificationChannel implements NotificationChannel {

    /** 最大重试次数 */
    @Value("${alert.notification.max-retry:3}")
    protected int maxRetry;

    /** 重试间隔（毫秒） */
    @Value("${alert.notification.retry-interval:1000}")
    protected long retryInterval;

    @Override
    public boolean send(String recipient, String subject, String content) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetry) {
            attempt++;
            try {
                log.debug("尝试发送通知: type={}, recipient={}, attempt={}/{}",
                        getType(), recipient, attempt, maxRetry);

                boolean success = doSend(recipient, subject, content);

                if (success) {
                    if (attempt > 1) {
                        log.info("通知发送成功（重试后）: type={}, recipient={}, attempt={}",
                                getType(), recipient, attempt);
                    }
                    return true;
                }

                // 发送返回 false，记录并准备重试
                log.warn("通知发送失败: type={}, recipient={}, attempt={}/{}",
                        getType(), recipient, attempt, maxRetry);

            } catch (Exception e) {
                lastException = e;
                log.warn("通知发送异常: type={}, recipient={}, attempt={}/{}, error={}",
                        getType(), recipient, attempt, maxRetry, e.getMessage());
            }

            // 如果不是最后一次尝试，等待后重试
            if (attempt < maxRetry) {
                try {
                    log.debug("等待{}ms后重试...", retryInterval);
                    Thread.sleep(retryInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("重试等待被中断");
                    break;
                }
            }
        }

        // 所有重试都失败
        log.error("通知发送最终失败: type={}, recipient={}, maxRetry={}, lastError={}",
                getType(), recipient, maxRetry,
                lastException != null ? lastException.getMessage() : "发送返回false");

        return false;
    }

    /**
     * 实际发送通知的方法，由子类实现
     *
     * @param recipient 接收人
     * @param subject   主题
     * @param content   内容
     * @return 是否发送成功
     * @throws Exception 发送过程中的异常
     */
    protected abstract boolean doSend(String recipient, String subject, String content) throws Exception;
}
