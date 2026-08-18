- Bài 5: Thiết kế và hiện thực hóa Workflow sự cố khẩn cấp (Console Alert)

- 1. Sơ đồ ASCII mô tả chi tiết luồng xử lý End-to-End Workflow:
  - Sơ đồ kiến trúc 2 pha và cơ chế chịu lỗi cô lập ngoại lệ:
    [ Tin nhắn báo cáo từ tài xế ]
                 │
                 ▼
    ┌────────────────────────────────────────┐
    │ 1. IncidentETLService                  │
    │    - Gọi LLM trích xuất DTO            │
    │    - Parse & Validate thông tin        │
    └────────────────────────────────────────┘
                 │
                 ▼ [ PHA 1: LƯU DB AN TOÀN ]
    ┌────────────────────────────────────────┐
    │ 2. Lưu IncidentReport xuống Database   │
    │    - Trạng thái: PENDING hoặc          │
    │      NOT_REQUIRED (nếu không khẩn cấp) │
    └────────────────────────────────────────┘
                 │
                 ├─────────────────────────────────────────┐
                 │ (Mức độ LOW / MEDIUM)                   │ (Mức độ HIGH / CRITICAL)
                 ▼                                         ▼
    ┌─────────────────────────┐               ┌────────────────────────────────────────┐
    │ Kết thúc workflow       │               │ 3. Kiểm tra điều kiện khẩn cấp         │
    │ NotificationStatus:     │               │    Kích hoạt ConsoleAlertService       │
    │ NOT_REQUIRED            │               │    (Bọc trong Try-Catch cô lập lỗi)    │
    └─────────────────────────┘               └────────────────────────────────────────┘
                                                           │
                                        ┌──────────────────┴──────────────────┐
                                        │ [ Thành công ]                      │ [ Ngoại lệ / Lỗi ]
                                        ▼                                     ▼
                          ┌───────────────────────────┐         ┌───────────────────────────┐
                          │ 4a. Cập nhật trạng thái   │         │ 4b. Ghi Log lỗi chi tiết  │
                          │     NotificationStatus:   │         │     Cập nhật trạng thái   │
                          │     SUCCESS               │         │     NotificationStatus:   │
                          │     (Sự cố đã lưu an toàn)│         │     FAILED                │
                          └───────────────────────────┘         │     (Sự cố vẫn lưu an toàn│
                                                                │      trong DB)            │
                                                                └───────────────────────────┘

- 2. Bản thuyết minh thiết kế giải pháp chịu lỗi (Fault Tolerance Architecture):
  - Phân tách giao dịch thành 2 pha độc lập (2-Phase Isolation):
    - Pha 1 (Save Data): Ghi nhận toàn bộ thông tin sự cố vào Database trước khi thực hiện bất kỳ hành động phụ nào. Đảm bảo dữ liệu sự cố không bao giờ bị mất mát.
    - Pha 2 (Dispatch Notification): Tiến trình gửi cảnh báo ra bên ngoài (Console / Webhook / SMS) được thực hiện sau khi Pha 1 đã commit thành công.
  - Cô lập ngoại lệ (Exception Isolation): Tiến trình phát cảnh báo ở Pha 2 được bao bọc trong khối try-catch riêng biệt. Nếu kênh cảnh báo bị lỗi (quá tải, mất mạng, thiết bị bận), ngoại lệ không làm rollback dữ liệu ở Pha 1 mà chỉ kích hoạt cập nhật trạng thái NotificationStatus = FAILED, phục vụ tra cứu và xử lý bù thủ công.
  - Mã nguồn Java hoàn chỉnh đã được triển khai tại các file ConsoleAlertService.java, IncidentReport.java, NotificationStatus.java và IncidentETLService.java trong project Session04/Bai5.

- 3. Minh chứng log console chạy ứng dụng trong 2 trường hợp:
  - Trường hợp 1: Phát cảnh báo đỏ thành công (Mức độ CRITICAL)
    - Log console:
      INFO  c.r.a.s.IncidentETLService - Pha 1: Lưu thành công sự cố vào DB với ID: 1, Trạng thái thông báo ban đầu: PENDING
      WARN  c.r.a.s.ConsoleAlertService - 
      ========================================================================
      🔴 [CẢNH BÁO ĐỎ KHẨN CẤP - LOGISTICS EMERGENCY ALERT] 🔴
      Mã sự cố ID: 1
      Mã đơn hàng : ORD-9999
      Biển số xe  : 51F-99999
      Loại sự cố  : Cháy kho hàng
      Mức độ      : CRITICAL (YÊU CẦU ĐIỀU PHỐI CỨU HỘ TỨC THỜI!)
      ========================================================================
      INFO  c.r.a.s.IncidentETLService - Pha 2: Phát cảnh báo đỏ thành công. Cập nhật trạng thái SUCCESS cho sự cố ID: 1
  - Trường hợp 2: Phát cảnh báo thất bại (Giả lập lỗi) nhưng dữ liệu DB vẫn được bảo toàn với trạng thái FAILED
    - Log console:
      INFO  c.r.a.s.IncidentETLService - Pha 1: Lưu thành công sự cố vào DB với ID: 2, Trạng thái thông báo ban đầu: PENDING
      ERROR c.r.a.s.ConsoleAlertService - [LỖI HỆ THỐNG PHÁT CẢNH BÁO]: Kênh phát tín hiệu Console Alert đang bận hoặc gặp sự cố phần cứng.
      ERROR c.r.a.s.IncidentETLService - Pha 2 [CÔ LẬP NGOẠI LỆ]: Tiến trình phát cảnh báo thất bại nhưng sự cố ID 2 vẫn được bảo toàn trong DB. Chi tiết: Lỗi kết nối thiết bị phát cảnh báo khẩn cấp (Simulated Failure).
      INFO  c.r.a.s.IncidentETLService - Cập nhật trạng thái FAILED thành công cho sự cố ID: 2
