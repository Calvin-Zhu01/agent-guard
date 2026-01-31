package com.agentguard.approval.util;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 审批ID生成器
 * <p>
 * 生成格式：YYYYMMDDHHMMSSXXX
 * 示例：20260131143022001
 * <p>
 * - YYYYMMDD: 年月日
 * - HHMMSS: 时分秒
 * - XXX: 序号（同一秒内的第N个审批，支持1-999）
 *
 * @author zhuhx
 */
@Component
public class ApprovalIdGenerator implements IdentifierGenerator {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // 当前秒的时间戳
    private volatile long currentSecond = 0;
    // 当前秒内的序号
    private final AtomicInteger sequence = new AtomicInteger(0);

    @Override
    public Number nextId(Object entity) {
        // 不使用Number类型，返回null让nextUUID处理
        return null;
    }

    @Override
    public String nextUUID(Object entity) {
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = System.currentTimeMillis() / 1000;

        // 如果是新的一秒，重置序号
        if (nowSecond != currentSecond) {
            synchronized (this) {
                if (nowSecond != currentSecond) {
                    currentSecond = nowSecond;
                    sequence.set(0);
                }
            }
        }

        // 获取序号（1-999）
        int seq = sequence.incrementAndGet();
        if (seq > 999) {
            // 如果超过999，等待下一秒
            try {
                Thread.sleep(1000);
                return nextUUID(entity);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("生成审批ID失败", e);
            }
        }

        // 格式化日期时间
        String datetime = now.format(DATETIME_FORMATTER);

        // 生成ID：20260131143022001
        return String.format("%s%03d", datetime, seq);
    }
}
