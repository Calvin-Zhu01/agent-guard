package com.agentguard.alert.channel;

import com.agentguard.alert.enums.NotificationChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知渠道工厂
 * 根据渠道类型获取对应的通知渠道实现
 *
 * @author zhuhx
 */
@Slf4j
@Component
public class NotificationChannelFactory {

    private final Map<NotificationChannelType, NotificationChannel> channelMap;

    public NotificationChannelFactory(List<NotificationChannel> channels) {
        this.channelMap = channels.stream()
                .collect(Collectors.toMap(NotificationChannel::getType, Function.identity()));
        log.info("已注册通知渠道: {}", channelMap.keySet());
    }

    /**
     * 根据渠道类型获取通知渠道
     *
     * @param type 渠道类型
     * @return 通知渠道实现
     */
    public NotificationChannel getChannel(NotificationChannelType type) {
        NotificationChannel channel = channelMap.get(type);
        if (channel == null) {
            log.error("未找到通知渠道实现: type={}", type);
            throw new IllegalArgumentException("不支持的通知渠道类型: " + type);
        }
        return channel;
    }

    /**
     * 检查渠道类型是否支持
     *
     * @param type 渠道类型
     * @return 是否支持
     */
    public boolean isSupported(NotificationChannelType type) {
        return channelMap.containsKey(type);
    }
}
