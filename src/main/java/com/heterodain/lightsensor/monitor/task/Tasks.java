package com.heterodain.lightsensor.monitor.task;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.heterodain.lightsensor.monitor.config.DeviceConfig;
import com.heterodain.lightsensor.monitor.config.ServiceConfig;
import com.heterodain.lightsensor.monitor.device.LightSensorDevice;
import com.heterodain.lightsensor.monitor.service.AmbientService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 非同期タスク
 */
@Component
@Slf4j
public class Tasks {
    @Autowired
    private DeviceConfig deviceConfig;
    @Autowired
    private ServiceConfig serviceConfig;

    @Autowired
    private LightSensorDevice lightSensorDevice;
    @Autowired
    private AmbientService ambientService;

    /** 計測データ(3秒値) */
    private List<Integer> threeSecDatas = new ArrayList<>();

    /**
     * 初期化処理
     */
    @PostConstruct
    public void init() throws IOException {
        // 照度センサーに接続
        lightSensorDevice.connect(deviceConfig.getLightSensor());
    }

    /**
     * 3秒毎に照度センサーからデータ取得
     */
    @Scheduled(initialDelay = 3 * 1000, fixedDelay = 3 * 1000)
    public void realtime() {
        try {
            var current = lightSensorDevice.readLux();
            log.debug("current={} lux", current);
            synchronized (threeSecDatas) {
                threeSecDatas.add(current);
            }

        } catch (Exception e) {
            log.error("照度センサーへのアクセスに失敗しました。", e);
        }
    }

    /**
     * 3分毎にAmbientにデータ送信
     */
    @Scheduled(cron = "0 */3 * * * *")
    public void sendAmbient() throws Exception {
        if (threeSecDatas.isEmpty()) {
            return;
        }

        // 平均値算出
        Double average;
        synchronized (threeSecDatas) {
            average = threeSecDatas.stream().mapToDouble(d -> (double) d).average().orElse(0D);
            threeSecDatas.clear();
        }

        // Ambient送信
        var ambientConfig = serviceConfig.getAmbient();
        if (ambientConfig != null) {
            try {
                var sendDatas = new Double[] { average };
                log.debug(
                        "Ambientに3分値を送信します。current={} lx",
                        sendDatas[0]);

                ambientService.send(ambientConfig, ZonedDateTime.now(), null, sendDatas);
            } catch (Exception e) {
                log.error("Ambientへのデータ送信に失敗しました。", e);
            }
        }
    }

    /**
     * 終了処理
     */
    @PreDestroy
    public void destroy() throws IOException {
        // 照度センサーの接続解除
        lightSensorDevice.close();
    }

}
