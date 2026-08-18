package com.rikkei.ai.service;

import com.rikkei.ai.dto.IncidentExtraction;
import com.rikkei.ai.entity.IncidentReport;
import com.rikkei.ai.entity.NotificationStatus;
import com.rikkei.ai.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentETLService {

    private static final Logger log = LoggerFactory.getLogger(IncidentETLService.class);

    private final ChatModel chatModel;
    private final IncidentRepository repository;
    private final ConsoleAlertService alertService;

    public IncidentETLService(ChatModel chatModel, IncidentRepository repository, ConsoleAlertService alertService) {
        this.chatModel = chatModel;
        this.repository = repository;
        this.alertService = alertService;
    }

    public IncidentReport processAndDispatch(String rawMessage, boolean simulateAlertFailure) {
        log.info("Bắt đầu xử lý tin nhắn sự cố: {}", rawMessage);

        // 1. EXTRACT & TRANSFORM: Gọi LLM trích xuất DTO
        BeanOutputConverter<IncidentExtraction> converter = new BeanOutputConverter<>(IncidentExtraction.class);
        String promptString = """
            Bạn là hệ thống AI phân tích sự cố vận tải. Hãy trích xuất tin nhắn sau sang JSON:
            --- TIN NHẮN ---
            %s
            --- QUY TẮC ---
            Chỉ trả về JSON thuần, không bọc markdown block (```json).
            %s
            """.formatted(rawMessage, converter.getFormatInstructions());

        String rawResponse = chatModel.call(new Prompt(promptString)).getResult().getOutput().getText();
        String cleanedJson = cleanMarkdown(rawResponse);
        IncidentExtraction dto = converter.convert(cleanedJson);

        boolean isUrgent = "HIGH".equalsIgnoreCase(dto.urgency()) || "CRITICAL".equalsIgnoreCase(dto.urgency());
        NotificationStatus initialStatus = isUrgent ? NotificationStatus.PENDING : NotificationStatus.NOT_REQUIRED;

        // 2. PHASE 1: Lưu sự cố vào DB an toàn trước khi kích hoạt cảnh báo
        IncidentReport incident = savePhase1(dto, initialStatus);
        log.info("Pha 1: Lưu thành công sự cố vào DB với ID: {}, Trạng thái thông báo ban đầu: {}", incident.getId(), initialStatus);

        // 3. PHASE 2: Nếu là sự cố khẩn cấp, phát cảnh báo với cơ chế cô lập lỗi (Fault Tolerance)
        if (isUrgent) {
            try {
                alertService.dispatchEmergencyAlert(incident, simulateAlertFailure);
                incident = updateNotificationStatus(incident.getId(), NotificationStatus.SUCCESS);
                log.info("Pha 2: Phát cảnh báo đỏ thành công. Cập nhật trạng thái SUCCESS cho sự cố ID: {}", incident.getId());
            } catch (Exception e) {
                log.error("Pha 2 [CÔ LẬP NGOẠI LỆ]: Tiến trình phát cảnh báo thất bại nhưng sự cố ID {} vẫn được bảo toàn trong DB. Chi tiết: {}",
                        incident.getId(), e.getMessage());
                incident = updateNotificationStatus(incident.getId(), NotificationStatus.FAILED);
            }
        }

        return incident;
    }

    @Transactional
    public IncidentReport savePhase1(IncidentExtraction dto, NotificationStatus status) {
        IncidentReport entity = new IncidentReport(
            dto.orderCode(),
            dto.licensePlate(),
            dto.incidentType(),
            dto.urgency(),
            dto.description(),
            status
        );
        return repository.save(entity);
    }

    @Transactional
    public IncidentReport updateNotificationStatus(Long id, NotificationStatus status) {
        IncidentReport entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự cố với ID: " + id));
        entity.setNotificationStatus(status);
        return repository.save(entity);
    }

    private String cleanMarkdown(String text) {
        if (text == null) return "";
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        return cleaned.trim();
    }
}
