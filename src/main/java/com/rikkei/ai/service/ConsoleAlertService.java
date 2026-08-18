package com.rikkei.ai.service;

import com.rikkei.ai.entity.IncidentReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleAlertService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleAlertService.class);

    public void dispatchEmergencyAlert(IncidentReport incident, boolean simulateFailure) {
        if (simulateFailure) {
            log.error("[LỖI HỆ THỐNG PHÁT CẢNH BÁO]: Kênh phát tín hiệu Console Alert đang bận hoặc gặp sự cố phần cứng.");
            throw new RuntimeException("Lỗi kết nối thiết bị phát cảnh báo khẩn cấp (Simulated Failure).");
        }

        String alertBox = String.format("""
            \n========================================================================
            🔴 [CẢNH BÁO ĐỎ KHẨN CẤP - LOGISTICS EMERGENCY ALERT] 🔴
            Mã sự cố ID: %d
            Mã đơn hàng : %s
            Biển số xe  : %s
            Loại sự cố  : %s
            Mức độ      : %s (YÊU CẦU ĐIỀU PHỐI CỨU HỘ TỨC THỜI!)
            Mô tả       : %s
            Thời gian   : %s
            ========================================================================",
            incident.getId(),
            incident.getOrderCode(),
            incident.getLicensePlate(),
            incident.getIncidentType(),
            incident.getUrgency(),
            incident.getDescription(),
            incident.getCreatedAt()
        );

        log.warn(alertBox);
    }
}
