package com.heterodain.lightsensor.monitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * デバイスの設定
 */
@Component
@ConfigurationProperties("device")
@Data
public class DeviceConfig {
    /* 照度センサーの設定 */
    private LightSensor lightSensor;

    /**
     * 照度センサーの設定情報
     */
    @Data
    public static class LightSensor {
        /* シリアル通信ポート名 */
        private String comPort;
    }

}