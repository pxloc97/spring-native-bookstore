# Feature Specification: Inventory Service — Stock Management & Reservation Lifecycle

**Feature Branch**: `001-inventory-service`  
**Created**: 2026-04-18  
**Status**: Draft  
**Bounded Context**: Inventory — source of truth for sellable stock and reservation state

---

## Overview

`inventory-service` tồn tại để **ngăn oversell và quản lý reservation lifecycle** trong hệ thống bookstore event-driven.

Nó là owner duy nhất của stock sellable. `catalog-service` và `order-service` không tự tính hay giữ stock.

Nó phải trả lời đúng 4 câu hỏi:
1. Còn bao nhiêu hàng có thể bán (theo ISBN)?
2. Đơn hàng này có giữ chỗ được không (all-or-nothing)?
3. Một order đang giữ những gì và bao nhiêu?
4. Khi hủy/timeout thì trả hàng về thế nào (idempotent)?

---

## Business Invariants

Đây là các ràng buộc không được phá vỡ trong bất kỳ trường hợp nào:

| # | Invariant |
|---|-----------|
| I1 | `available` không bao giờ âm |
| I2 | `reserved` không bao giờ âm |
| I3 | Một order không được reserve cùng một line item nhiều lần |
| I4 | Reserve là all-or-nothing: hoặc tất cả line items thành công, hoặc fail toàn bộ |
| I5 | Release chỉ áp dụng cho reservation đang `RESERVED` |
| I6 | Cùng một event đến nhiều lần → kết quả business phải giống như một lần (idempotency ở mức business) |

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Manage Stock (Priority: P1)

Là admin/operator, tôi cần khởi tạo và điều chỉnh số lượng tồn kho cho từng ISBN, để hệ thống có dữ liệu đúng về hàng có thể bán.

**Why this priority**: Đây là foundation. Không có stock data thì không có reservation hay query nào có nghĩa.

**Independent Test**: Có thể test độc lập bằng cách seed stock cho một ISBN, query lại và verify `available` đúng.

**Acceptance Scenarios**:

1. **Given** ISBN chưa có trong hệ thống, **When** admin add stock với delta = +50, **Then** `available = 50`, `reserved = 0`
2. **Given** ISBN có `available = 10`, `reserved = 3`, **When** admin adjust delta = +5, **Then** `available = 15`, `reserved = 3`
3. **Given** ISBN có `available = 5`, `reserved = 3`, **When** admin adjust delta = -6, **Then** request bị reject vì `available` sẽ âm
4. **Given** bất kỳ ISBN nào, **When** query stock, **Then** trả về `{ isbn, available, reserved }`

---

### User Story 2 — Reserve Stock for Order (Priority: P1)

Là hệ thống order, khi một order mới được tạo, tôi cần inventory kiểm tra và giữ hàng cho toàn bộ order, để ngăn oversell.

**Why this priority**: Đây là core use case. Không có reserve thì inventory service không có giá trị.

**Independent Test**: Có thể test bằng cách gửi `order.created` event với list items, verify stock thay đổi và `inventory.reserved` hoặc `inventory.rejected` được phát ra.

**Acceptance Scenarios**:

1. **Given** ISBN A có `available = 5`, ISBN B có `available = 3`, **When** order yêu cầu A:2, B:1, **Then** `A.available = 3, A.reserved = 2`, `B.available = 2, B.reserved = 1`, phát `inventory.reserved`
2. **Given** ISBN A có `available = 5`, ISBN B có `available = 0`, **When** order yêu cầu A:2, B:1, **Then** không có stock nào thay đổi, phát `inventory.rejected` với reason `INSUFFICIENT_STOCK` cho ISBN B
3. **Given** ISBN X chưa tồn tại trong hệ thống, **When** order yêu cầu X:1, **Then** reject toàn order, phát `inventory.rejected` với reason `INVENTORY_NOT_FOUND`
4. **Given** `order.created` event cho orderId đã được xử lý trước đó, **When** event đến lần 2, **Then** không thay đổi stock, trả về kết quả giống lần đầu (idempotent)
5. **Given** order yêu cầu nhiều ISBNs, **When** chỉ một line thiếu hàng, **Then** không có line nào được reserve (all-or-nothing)

---

### User Story 3 — Release Reserved Stock (Priority: P2)

Là hệ thống order, khi một order bị cancel hoặc timeout, tôi cần inventory trả lại hàng đã giữ, để stock được giải phóng cho các order khác.

**Why this priority**: Cần thiết cho compensation flow. Không có release thì reserved stock bị lock mãi mãi.

**Independent Test**: Có thể test bằng cách reserve trước, sau đó gửi release request, verify stock trở về trạng thái ban đầu.

**Acceptance Scenarios**:

1. **Given** order đã reserve A:2, B:1, **When** release được gọi cho orderId đó, **Then** `A.available += 2, A.reserved -= 2`, `B.available += 1, B.reserved -= 1`, cả hai reservation chuyển sang `RELEASED`
2. **Given** release đã được gọi lần đầu thành công, **When** release được gọi lần 2 với cùng orderId, **Then** không có stock nào thay đổi (no-op idempotent)
3. **Given** orderId không có reservation nào đang `RESERVED`, **When** release được gọi, **Then** no-op, không có lỗi

---

### User Story 4 — Query Reservation State (Priority: P3)

Là operator/admin, tôi cần xem trạng thái reservation của một order hoặc tồn kho hiện tại của một ISBN, để debug và vận hành.

**Why this priority**: Read-only, không ảnh hưởng business flow. Cần thiết cho observability nhưng không blocking cho Epic 1.

**Independent Test**: Có thể test bằng cách query sau khi reserve/release.

**Acceptance Scenarios**:

1. **Given** order đã reserve, **When** query reservation theo orderId, **Then** trả về list `{ isbn, quantity, status }` cho từng line
2. **Given** order đã release, **When** query reservation theo orderId, **Then** trả về list với `status = RELEASED`

---

### Edge Cases

- **Duplicate event**: Cùng `order.created` eventId đến lần 2 → idempotent, không reserve lại
- **Partial stock**: Một trong nhiều ISBNs thiếu hàng → reject toàn bộ order, không giữ partial
- **ISBN không tồn tại**: Order yêu cầu ISBN chưa có inventory record → reject toàn order
- **Adjust âm quá**: delta khiến `available < 0` → reject, không thay đổi stock
- **Release không có reservation**: orderId chưa từng reserve → no-op, không lỗi
- **Concurrent reserve**: Hai order cùng tranh giành hàng cuối cùng → chỉ một thành công, một reject

---

## Requirements *(mandatory)*

### Functional Requirements

**Nhóm A — Stock Management**

- **FR-001**: Hệ thống PHẢI cho phép thêm stock cho một ISBN bằng delta dương (nhập hàng)
- **FR-002**: Hệ thống PHẢI cho phép giảm stock bằng delta âm (hao hụt/correction), với điều kiện `available + delta >= 0`
- **FR-003**: Hệ thống PHẢI từ chối adjust nếu kết quả vi phạm invariant I1 hoặc I2
- **FR-004**: Hệ thống PHẢI trả về trạng thái `{ isbn, available, reserved }` khi được query theo ISBN
- **FR-005**: Adjust PHẢI nhận `reason` để audit trail

**Nhóm B — Reserve**

- **FR-006**: Hệ thống PHẢI xử lý `order.created` event và kiểm tra tất cả line items
- **FR-007**: Hệ thống PHẢI thực hiện reserve theo all-or-nothing: hoặc tất cả line items được reserve, hoặc không có gì
- **FR-008**: Khi reserve thành công: giảm `available`, tăng `reserved`, tạo reservation records trạng thái `RESERVED`, phát `inventory.reserved`
- **FR-009**: Khi thiếu bất kỳ line nào: không thay đổi stock, phát `inventory.rejected` kèm `rejectReason` (`INSUFFICIENT_STOCK` hoặc `INVENTORY_NOT_FOUND`)
- **FR-010**: Hệ thống PHẢI xử lý duplicate `order.created` event idempotent theo orderId/eventId

**Nhóm C — Release**

- **FR-011**: Hệ thống PHẢI xử lý release request theo orderId
- **FR-012**: Khi release: tăng `available`, giảm `reserved`, chuyển reservation sang `RELEASED`, chỉ áp dụng cho reservation đang `RESERVED`
- **FR-013**: Release lặp lại với cùng orderId PHẢI là no-op (idempotent)
- **FR-014**: Release orderId không có reservation đang `RESERVED` PHẢI là no-op, không trả lỗi

**Nhóm D — Query**

- **FR-015**: Hệ thống PHẢI cho phép query reservation records theo orderId
- **FR-016**: Kết quả query PHẢI bao gồm isbn, quantity, và reservation status

### Key Entities

- **InventoryItem**: Tồn kho cho một ISBN. Giữ `isbn`, `available`, `reserved`. Là aggregate root của stock state.
- **Reservation**: Cam kết tạm thời gắn với `orderId` và `isbn`. Giữ `orderId`, `isbn`, `quantity`, `status` (`RESERVED` / `REJECTED` / `RELEASED`).
- **InventoryDecision**: Kết quả xử lý một order. Giá trị: `RESERVED` hoặc `REJECTED`.
- **RejectReason**: Lý do reject. Giá trị: `INSUFFICIENT_STOCK`, `INVENTORY_NOT_FOUND`.

### Decisions đã chốt

| Quyết định | Lựa chọn | Lý do |
|------------|----------|-------|
| Partial fulfillment | Không — reject toàn order | Tránh trạng thái "partially reserved" phức tạp |
| Reservation granularity | Lưu theo từng line (isbn), quyết định ở mức order | Dễ release từng phần sau này |
| Adjust mode | Delta (không dùng set absolute) | Dễ audit, ít gây sai |
| Idempotency key | orderId cho reserve; orderId cho release | Đủ để ngăn duplicate |
| Timeout handling | Epic 2 — không cần ở Phase 1 | Tránh phức tạp sớm |

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Stock không bao giờ âm sau bất kỳ thao tác nào (invariant I1, I2 luôn đúng)
- **SC-002**: Không có oversell: tổng `reserved` của tất cả orders không vượt quá stock ban đầu
- **SC-003**: Reserve all-or-nothing: không tồn tại trạng thái "partially reserved" cho bất kỳ order nào
- **SC-004**: Duplicate event: gửi cùng `order.created` 10 lần, stock chỉ thay đổi đúng 1 lần
- **SC-005**: Release idempotency: gọi release 5 lần cho cùng orderId, stock chỉ được trả về 1 lần
- **SC-006**: Sau khi reserve rồi release đầy đủ, `available` và `reserved` trở về đúng giá trị trước khi reserve
- **SC-007**: Tất cả reject đều kèm `rejectReason` rõ ràng (`INSUFFICIENT_STOCK` hoặc `INVENTORY_NOT_FOUND`)

---

## Assumptions

- `catalog-service` và `order-service` không giữ hay tính stock sellable — inventory là source of truth duy nhất
- Phase 1 không cần reservation timeout tự động; timeout job sẽ làm ở Epic 2
- Adjust stock chỉ dành cho admin/internal flows, không phải end-user action
- Phase 1 không yêu cầu partial fulfillment; all-or-nothing là đủ cho Epic 1
- Event contract với `order-service` dùng Kafka (phù hợp với kiến trúc hiện tại của bookstore)
- Idempotency key cho reserve là `orderId`; có thể mở rộng sang `eventId` ở Epic 2 nếu cần
- `onHand` không cần lưu riêng ở Phase 1; có thể tính từ `available + reserved`
