# Hệ Thống Đấu Giá Trực Tuyến Thời Gian Thực

[![CI Pipeline](https://github.com/tdlynnmd/APPDauGiaVjpPro/actions/workflows/ci.yaml/badge.svg)](https://github.com/tdlynnmd/APPDauGiaVjpPro/actions/workflows/ci.yaml)
[![Java](https://img.shields.io/badge/Java-21+-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)

Ứng dụng Client-Server phân tán trên nền tảng Java, mô phỏng sàn đấu giá tài sản trực tuyến thời gian thực. Người dùng có thể đăng bán tài sản đa hình (Điện tử, Mỹ thuật, Phương tiện), đặt giá thầu cạnh tranh trong phòng Live, và quản trị hệ thống — tất cả giao tiếp qua kết nối Socket TCP/IP với giao thức JSON tùy chỉnh.


---

## Mục Lục

- [Bài Toán & Phạm Vi](#bài-toán--phạm-vi)
- [Kiến Trúc Hệ Thống](#kiến-trúc-hệ-thống)
- [Công Nghệ Sử Dụng](#công-nghệ-sử-dụng)
- [Cấu Trúc Thư Mục](#cấu-trúc-thư-mục)
- [Hướng Dẫn Khởi Chạy](#hướng-dẫn-khởi-chạy)
- [Tính Năng](#tính-năng)
- [Báo Cáo & Demo](#báo-cáo--demo)

---

## Bài Toán & Phạm Vi

Trong đấu giá trực tuyến, thách thức lớn nhất là đảm bảo **tính đồng bộ**, **tính thời gian thực** và **an toàn giao dịch ví tiền**. Hệ thống giải quyết 4 vấn đề cốt lõi:

| Thách thức                     | Giải pháp                                                                                                                 |
|:-------------------------------|:--------------------------------------------------------------------------------------------------------------------------|
| Đồng bộ đặt giá đa luồng       | `synchronized` + Database Transaction — đảm bảo chỉ 1 mức giá hợp lệ được chấp nhận, ví tiền đóng băng/hoàn trả nguyên tử |
| Tự động hóa vòng đời đấu giá   | Daemon Scheduler quét RAM mỗi 1 giây — tự động chuyển trạng thái, chốt phiên, chuyển tiền cho Seller                      |
| Phát sóng phòng Live real-time | Observer Pattern (AuctionEventBus → LiveRoomManage) — broadcast giá thầu, đếm ngược, viewer count tức thời                |
| Chống bắn tỉa (Anti-sniping)   | Tự động gia hạn 60 giây khi có lượt bid trong 30 giây cuối phiên                                                          |

### Phạm vi theo vai trò

| Vai trò    | Chức năng                                                                                                                          |
|:-----------|:-----------------------------------------------------------------------------------------------------------------------------------|
| **Bidder** | Đăng ký/Đăng nhập · Nạp/Rút tiền ví · Vào phòng Live · Đặt giá thầu · Xem lịch sử bid                                              |
| **Seller** | Tạo/Sửa/Xóa vật phẩm đa hình (3 danh mục) · Tạo phiên đấu giá · Hủy phiên (khi chưa có thầu)                                       |
| **Admin**  | Xem danh sách người dùng · Khóa tài khoản (auto ngắt kết nối) · Hủy phiên bất hợp pháp (auto hoàn tiền) · Gỡ vật phẩm · Audit Logs |

---

## Kiến Trúc Hệ Thống

```text
┌───────────────────────────────────────────────────────────────────────────────────────────┐
│                                       CLIENT (JavaFX)                                     │
│                                                                                           │
│       [JavaFX UI Thread (Main Thread)]                                                    │
│         ┌─────────────────────────────────────────────────────────────────────────┐       │
│         │        UI Views & Controllers (Dashboard, LiveBidding, Wallet...)       │       │
│         └───────┬─────────────────────────────────────────────────────────▲───────┘       │
│                 │                                                         │               │
│                 │ 1. Sends Request (API Call)                             │ 4B. Returns   │
│                 ▼                                                         │     Result    │
│          ┌──────────────┐                                          ┌──────┴──────┐        │
│          │ API Clients  │                                          │   Pending   │        │
│          │ (Auth, Bid)  │                                          │   Futures   │        │
│          └──────┬───────┘                                          └──────▲──────┘        │
│                 │                                                         │               │
│                 │ 2. Writes Request                                       │ 3B. Resolves  │
│                 │    (with requestId)                                     │     Future    │
│                 ▼                                                         │               │
│          ┌────────────────────────────────────────────────────────────────┴─────────────┐ │
│          │             ClientSocketService (Core Communication Service)                 │ │
│          │                                                                              │ │
│          │   [Socket Reader Thread (Background Thread)]                                 │ │
│          │     Reads JSON Line ──► JSON parsed to SocketResponse                        │ │
│          │                                   │                                          │ │
│          │                                   ├────────────────────────────┐             │ │
│          │                                   ▼ (If Event)                 ▼ (If Response) │
│          │                         [Listener Registry]            [Resolve Future]      │ │
│          └───────────────────────────────────┬──────────────────────────────────────────┘ │
│                                              │                                            │
│                                              │ 3A. Dispatches Event                       │
│                                              ▼                                            │
│                                    ┌───────────────────┐                                  │
│                                    │ BalanceListener   │                                  │
│                                    │ RealtimeListener  │                                  │
│                                    └─────────┬─────────┘                                  │
│                                              │                                            │
│                                              │ 4A. Platform.runLater()                    │
│                                              ▼                                            │
│                                      [Update UI Elements]                                 │
│                                              │                                            │
│                                              └────────────────────────────► [UI Views]    │
└──────────────────────────────────────────────┬────────────────────────────────────────────┘
                                               │ JSON over TCP (Port 5555)
                                               │ (SocketRequest / SocketResponse)
┌──────────────────────────────────────────────▼────────────────────────────────────────────┐
│                                       SERVER (Java 21)                                    │
│                                                                                           │
│   [Network Layer]                                                                         │
│     SocketServer (Java 21 Virtual Threads) ──► ClientHandler                              │
│                                                     │                                     │
│   [Routing Layer]                                   ▼                                     │
│                                             RequestDispatcher                             │
│                                                     │                                     │
│   [Controller & Service Layer]                      ▼                                     │
│                                             AuctionService.processBid()                   │
│                                             ╱               ╲                             │
│            (Event-Driven Broadcast Path)   ╱                 ╲ (Hot Path)                 │
│                                           ▼                   ▼                           │
│                                    AuctionEventBus     In-Memory RAM Cache                │
│                                           │            (User & Auction)                   │
│                                           ▼                   │                           │
│                                    LiveRoomManage             ▼                           │
│                                  (Observer Pattern)    [Async DB Queue]                   │
│                                           │               (dbQueue)                       │
│                                           ▼                   │                           │
│                                     ClientSession             ▼                           │
│                                  (TCP Socket Session)  Single-Thread Writer               │
│                                           │                   │                           │
│                                           ▼                   ▼                           │
│                                     [Socket Send]         DAO Layer                       │
│                                                           (HikariCP Pool)                 │
│                                                               │                           │
│                                                               ▼                           │
│                                                           MySQL DB                        │
└───────────────────────────────────────────────────────────────────────────────────────────┘
```

Hệ thống được thiết kế theo mô hình kiến trúc phân lớp hướng hiệu năng cao (High Performance Layered Architecture), phân tách luồng nghiệp vụ nóng (Hot-path) và luồng lưu trữ bền vững (Cold-path):

### 1. Phân Tách Đa Luồng Phía Client (JavaFX Threading Model)
Để đảm bảo giao diện luôn mượt mà và phản hồi tức thời dưới 60 FPS, Client phân chia cấu trúc thành 2 môi trường luồng tách biệt:
* **JavaFX UI Thread (Main Thread):** Chịu trách nhiệm render màn hình, vẽ biểu đồ và xử lý các sự kiện click chuột của người dùng. Luồng này **không bao giờ** được phép thực hiện các thao tác I/O mạng hoặc chờ đợi phản hồi Socket đồng bộ.
* **Socket Reader Thread (Background Thread):** Là một luồng ngầm chạy vô hạn (`SocketService`) lắng nghe kênh nhận tin từ Socket InputStream. Khi nhận được `SocketResponse` dưới dạng JSON (ví dụ như cập nhật số dư ví hoặc có giá thầu mới), luồng này sẽ parse dữ liệu và tìm kiếm các Callback đã đăng ký trong **Listener Registry** (chẳng hạn như `BalanceListener`, `RealtimeUpdateListener`).
* **Đồng bộ luồng UI (Platform.runLater):** Do các thay đổi giao diện JavaFX bắt buộc phải diễn ra trên luồng UI chính, Socket Reader Thread sau khi dispatch callback sẽ bọc các tác vụ cập nhật giao diện trong `Platform.runLater()`. Việc này đảm bảo luồng ngầm giao tiếp mạng an toàn mà không gây ra lỗi xung đột luồng giao diện (`IllegalStateException`).

### 2. Luồng Xử Lý Phía Server (Event-Driven & Write-behind DB Queue)
* **Virtual Threads (Java 21):** Mỗi khi có Client kết nối, `SocketServer` sinh ra một luồng ảo để quản lý kết nối đó thông qua `ClientHandler`. Nhờ tính chất cực nhẹ của luồng ảo Java 21, Server có thể chịu tải hàng ngàn kết nối đồng thời mà không bị giới hạn bộ nhớ như luồng vật lý OS.
* **Request Routing:** `RequestDispatcher` tiếp nhận gói tin JSON, bóc tách và định tuyến nghiệp vụ tới Controller tương ứng.
* **Hot-path cập nhật RAM cực nhanh:** Khi xử lý lượt thầu trong `AuctionService.processBid()`, thay vì block luồng để chờ MySQL ghi đĩa, hệ thống thực hiện trừ/đóng băng tiền và đặt giá trực tiếp trên các cấu trúc dữ liệu in-memory cache có khóa bảo vệ (`UserManage`, `AuctionManage`).
* **Hàng Đợi Bất Đồng Bộ (Async DB Queue):** Sau khi trạng thái RAM được xác nhận hợp lệ, Server gói dữ liệu trạng thái thầu thành một `BidTask` và ném vào hàng đợi bất đồng bộ `dbQueue` (ConcurrentLinkedQueue). Một luồng ngầm đơn nhiệm (`Single-Thread DB Writer`) sẽ tiêu thụ tuần tự từ hàng đợi này để cập nhật xuống MySQL. Việc này giúp luồng xử lý WebSocket realtime không bị block bởi Database I/O, đồng thời khống chế số lượng kết nối ghi xuống MySQL luôn là duy nhất, loại bỏ lỗi cạn kiệt Connection Pool hay xung đột lock DB (Deadlocks).
* **Phát sự kiện thời gian thực (Observer Pattern):** Song song với hàng đợi ghi đĩa, một sự kiện `NEW_BID` được gửi qua `AuctionEventBus`. Lớp `LiveRoomManage` (Observer) bắt sự kiện này và ngay lập tức gửi thông điệp cập nhật cho tất cả client trong phòng thông qua các `ClientSession` tương ứng, đảm bảo độ trễ cập nhật trên màn hình người dùng là dưới 10ms.

---

## Công Nghệ Sử Dụng

| Hạng mục       | Chi tiết                                                        |
|:---------------|:----------------------------------------------------------------|
| Nền tảng       | Java 21+ (Virtual Threads)                                      |
| Giao diện      | JavaFX 21 · ControlsFX · CSS Dark/Light Theme                   |
| Cơ sở dữ liệu  | MySQL 8.x · HikariCP Connection Pool · Hỗ trợ SSL Cloud (Azure) |
| Giao tiếp mạng | Custom JSON-over-TCP Socket (Gson) · Port 5555                  |
| Bảo mật        | BCrypt (Favre) · Phân quyền Role-based                          |
| Kiểm thử       | JUnit 5 · Mockito 5 · 694+ test cases                           |
| Build & CI     | Maven Multi-module · GitHub Actions (ubuntu / windows / macos)  |

### Yêu cầu cài đặt

| Cách chạy      | Yêu cầu tối thiểu          |
|:---------------|:---------------------------|
| Bản Build Sẵn  | JRE/JDK 21+, MySQL         |
| Script tự động | JDK 21+, Maven 3.8+, MySQL |
| Lệnh thủ công  | JDK 21+, Maven 3.8+, MySQL |

---

## Cấu Trúc Thư Mục

```
APPDauGiaVjpPro/
├── common/                 # DTO & Enum dùng chung (Client ↔ Server)
├── Server/                 # Module Backend
├── Client/                 # Module Frontend (JavaFX)
├── docs/
│   └── screenshots/        # Ảnh giao diện cho README
├── windows/                # Script khởi chạy (.bat)
├── linux_macos/            # Script khởi chạy (.sh)
├── schema.sql              # Script khởi tạo database (7 bảng)
├── .env.example            # Cấu hình môi trường mẫu
├── build-release.bat/.sh   # Đóng gói bản phát hành (Fat JAR)
└── pom.xml                 # Parent POM
```

<details>
<summary>Xem chi tiết cấu trúc từng module</summary>

```
common/
└── src/.../auction/
    ├── dto/            # 38 lớp DTO (SocketRequest, SocketResponse...)
    ├── enums/          # ActionType, AuctionStatus, UserRole, ItemType...
    └── utils/          # GsonProvider (LocalDateTime adapter)

Server/
└── src/.../auction/
    ├── config/         # DatabaseConnection (HikariCP, SSL Azure)
    ├── controller/     # 5 Controller (Auth, Auction, Item, User, Admin)
    ├── dao/            # 6 DAO interface + JDBC implementation
    ├── event/          # Observer Pattern (AuctionEventBus, AuctionObserver)
    ├── exception/      # 11 Custom Exception (Auth, Wallet, Validation...)
    ├── manage/         # 5 In-memory Cache Manager
    ├── models/         # OOP đa hình — Factory Pattern (User, Item, Auction)
    ├── network/        # SocketServer, ClientHandler, RequestDispatcher
    └── service/        # 8 Service nghiệp vụ cốt lõi

Client/
└── src/.../auction/
    ├── controller/     # 15 FXML Controller
    ├── network/        # 7 API Client
    └── service/        # Socket reader thread, SessionManager
└── src/main/resources/com/auction/client/
    └── view/           # 15 FXML + dark.css / light.css
```

</details>

---

## Hướng Dẫn Khởi Chạy

> **Lưu ý:** Đảm bảo đã bật **MySQL** (cổng `3306`) và import tệp **`schema.sql`** vào database trước khi khởi chạy.

### Cách 1 — Bản Build Sẵn *(dành cho người dùng cuối)*

> Yêu cầu: JDK 21+, Maven, MySQL

1. Đóng gói bản phát hành — chỉ cần chạy **1 lần**:

   ```bash
   # Windows
   build-release.bat

   # Linux / macOS
   ./build-release.sh
   ```

   Lệnh trên tự động tạo thư mục `release/`.

2. Vào thư mục `release/`, điền cấu hình MySQL vào file `.env`.

3. Khởi chạy:

   ```bash
   # Windows
   release/windows/run-server.bat
   release/windows/run-client.bat

   # Linux / macOS
   release/linux_macos/run-server.sh
   release/linux_macos/run-client.sh
   ```

4. Dừng ứng dụng: chạy `stop.bat` hoặc `stop.sh` trong thư mục `release/`.

---

### Cách 2 — Script Tự Động *(dành cho developer)*

> Yêu cầu: JDK 21+, Maven 3.8+, MySQL

```bash
# Windows — tự động kiểm tra database và tạo .env
windows/run-server.bat
windows/run-client.bat

# Linux / macOS
linux_macos/run-server.sh
linux_macos/run-client.sh
```

Dừng ứng dụng:

```bash
stop.bat server   # chỉ dừng Server
stop.bat          # dừng cả Server và Client
```

---

### Cách 3 — Dòng Lệnh Thủ Công

> Yêu cầu: JDK 21+, Maven 3.8+, MySQL

```bash
# 1. Tạo file cấu hình
cp .env.example .env
# Điền thông tin MySQL vào .env

# 2. Biên dịch
mvn clean compile

# 3. Khởi chạy ở 2 terminal riêng biệt
mvn compile exec:java -pl Server -Dfile.encoding=UTF-8
mvn compile exec:java -pl Client -Dfile.encoding=UTF-8
```

Dừng ứng dụng: nhấn **`Ctrl + C`** tại terminal tương ứng.

---

## Tính Năng

### Nhóm chức năng nâng cao

- [x] **Socket Server Virtual Threads (Java 21)** — chịu tải đồng thời 300 client.
- [x] **Kiến trúc Event-Driven** — Observer Pattern: AuctionEventBus phát sự kiện, LiveRoomManage broadcast đến tất cả client trong phòng.
- [x] **Phòng Live thời gian thực** — đếm ngược từng giây, viewer count, broadcast biến động giá tức thì.
- [x] **Đặt giá thầu an toàn đa luồng** — `synchronized` + DB Transaction, đóng băng và hoàn tiền tự động theo từng trạng thái.
- [x] **Chống bắn tỉa (Anti-sniping)** — tự động gia hạn 60 giây khi có bid trong 30 giây cuối phiên.
- [x] **Daemon Scheduler** — quét RAM mỗi 1 giây: chốt phiên, chuyển tiền cho Seller, đánh trạng thái SOLD.
- [x] **Vật phẩm đa hình OOP** — Factory Pattern: 3 danh mục (Electronics, Art, Vehicles).
- [x] **Cưỡng chế đăng xuất (Force Logout)** — tự động ngắt kết nối khi Admin ban tài khoản hoặc người dùng đổi mật khẩu.
- [x] **Dark / Light Theme** — 2 bộ CSS hoàn chỉnh (25KB mỗi theme).
- [x] **CI/CD Pipeline** — GitHub Actions build matrix trên ubuntu / windows / macOS.

### Nhóm chức năng cơ bản

- [x] Đăng ký tài khoản (BCrypt) và đăng nhập phân quyền (Admin / Seller / Bidder).
- [x] Cập nhật thông tin cá nhân và đổi mật khẩu (xác thực mật khẩu cũ).
- [x] Ví điện tử: Nạp/Rút tiền, quản lý số dư khả dụng và số dư đóng băng.
- [x] CRUD vật phẩm đa hình (Điện tử, Mỹ thuật, Phương tiện).
- [x] Tạo/Hủy phiên đấu giá, giám sát trạng thái tự động (Open → Running → Finished).
- [x] Đặt giá thầu trong phòng Live và xem lịch sử đặt giá chi tiết.
- [x] Admin: Khóa tài khoản, hủy phiên, gỡ vật phẩm, xem Audit Logs.
- [x] Seeding Data tự động khi Server khởi động.

---

## Báo Cáo & Demo

| Tài liệu            | Link            |
|:--------------------|:----------------|
| Báo cáo đồ án (PDF) | [Google Drive](https://drive.google.com/drive/folders/1aNVkxSMh7OzZP0KNJxeiIXqfowq9UZsB) |
| Video demo hệ thống | [Google Drive](https://drive.google.com/file/d/18xcNZg3Z8ir9OedHi31D7lqgKrCIY_nA/view) |

