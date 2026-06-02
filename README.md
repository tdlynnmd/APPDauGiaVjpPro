# 🏗️ HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN THỜI GIAN THỰC (REAL-TIME ONLINE AUCTION SYSTEM)

[![Java Version](https://img.shields.io/badge/Java-21%2B-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![UI Framework](https://img.shields.io/badge/UI-JavaFX%2021-blue?style=flat-square&logo=java)](https://openjfx.io/)
[![Build Tool](https://img.shields.io/badge/Build-Maven-C71A36?style=flat-square&logo=apache-maven)](https://maven.apache.org/)
[![Database](https://img.shields.io/badge/Database-MySQL%208.x-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Java Enterprise CI Pipeline](https://github.com/tdlynnmd/APPDauGiaVjpPro/actions/workflows/ci.yaml/badge.svg)](https://github.com/tdlynnmd/APPDauGiaVjpPro/actions/workflows/ci.yaml)

Ứng dụng Client-Server phân tán trên nền tảng Java, mô phỏng sàn đấu giá tài sản trực tuyến thời gian thực. Người dùng có thể đăng bán tài sản đa hình (Điện tử, Mỹ thuật, Phương tiện), đặt giá thầu cạnh tranh trong phòng Live, và quản trị hệ thống — tất cả giao tiếp qua kết nối Socket TCP/IP với giao thức JSON tùy chỉnh.

---

## 1. 📋 Bài Toán & Phạm Vi Hệ Thống

Trong đấu giá trực tuyến, thách thức lớn nhất là đảm bảo **tính đồng bộ**, **tính thời gian thực** và **an toàn giao dịch ví tiền**. Hệ thống giải quyết 4 vấn đề cốt lõi:

| Thách thức | Giải pháp |
|:---|:---|
| 🔒 Đồng bộ đặt giá đa luồng | `synchronized` + Database Transaction — đảm bảo chỉ 1 mức giá hợp lệ được chấp nhận, ví tiền đóng băng/hoàn trả nguyên tử |
| ⚙️ Tự động hóa vòng đời đấu giá | Daemon Scheduler quét RAM mỗi 1 giây — tự động chuyển trạng thái, chốt phiên, chuyển tiền cho Seller |
| 📡 Phát sóng phòng Live real-time | Observer Pattern (AuctionEventBus → LiveRoomManage) — broadcast giá thầu, đếm ngược, viewer count tức thời |
| 🛡️ Chống bắn tỉa (Anti-sniping) | Tự động gia hạn 60 giây khi có lượt bid trong 30 giây cuối phiên |

### 🎯 Phạm vi hệ thống

| Vai trò | Phạm vi chức năng |
|:---|:---|
| **Bidder** | Đăng ký/Đăng nhập · Nạp/Rút tiền ví · Vào phòng Live · Đặt giá thầu · Xem lịch sử bid |
| **Seller** | Tạo/Sửa/Xóa vật phẩm đa hình (3 danh mục) · Tạo phiên đấu giá · Hủy phiên (khi chưa có thầu) |
| **Admin** | Xem danh sách người dùng · Khóa tài khoản (auto ngắt kết nối) · Hủy phiên bất hợp pháp (auto hoàn tiền) · Gỡ vật phẩm · Audit Logs |

---

## 2. 🛠️ Công Nghệ Sử Dụng & Yêu Cầu Cài Đặt

### ⚡ Công nghệ

| Hạng mục | Chi tiết |
|:---|:---|
| Nền tảng | Java 21+ (Virtual Threads) |
| Giao diện | JavaFX 21 · ControlsFX · CSS Dark/Light Theme |
| Database | MySQL 8.x · HikariCP Connection Pool · Hỗ trợ SSL Cloud (Azure) |
| Giao tiếp | Custom JSON-over-TCP Socket (Gson) · Port 5555 |
| Bảo mật | BCrypt (Favre) · Phân quyền Role-based |
| Build & CI | Maven Multi-module · GitHub Actions CI (ubuntu/windows/macos) |
| Testing | JUnit 5 · Mockito 5 (692 test cases) |

### 📌 Yêu cầu cài đặt

| Cách chạy | Yêu cầu |
|:---|:---|
| Bản Build Sẵn | Java (JRE/JDK 21+), MySQL |
| Script tự động | JDK 21+, Maven 3.8+, MySQL |
| Lệnh thủ công | JDK 21+, Maven 3.8+, MySQL |

---

## 3. 📁 Cấu Trúc Thư Mục & Các Module Chính

```text
APPDauGiaVjpPro/
├── common/                     # Module DTO & Enum dùng chung (Client ↔ Server)
│   └── src/.../auction/
│       ├── dto/                # 38 lớp truyền tải dữ liệu (SocketRequest, SocketResponse...)
│       ├── enums/              # ActionType, AuctionStatus, UserRole, ItemType...
│       └── utils/              # GsonProvider (cấu hình LocalDateTime adapter)
│
├── Server/                     # Module Backend
│   └── src/.../auction/
│       ├── config/             # DatabaseConnection (HikariCP, SSL Azure)
│       ├── controller/         # 5 Controller (Auth, Auction, Item, User, Admin)
│       ├── dao/                # 6 DAO interface + JDBC implementation
│       ├── event/              # Observer Pattern (AuctionEventBus, AuctionObserver)
│       ├── exception/          # 11 Custom Exception (Auth, Wallet, Validation...)
│       ├── manage/             # 5 In-memory Cache Manager (User, Auction, LiveRoom, Product, Connection)
│       ├── models/             # OOP đa hình — Factory Pattern (User, Item, Auction)
│       ├── network/            # SocketServer (Virtual Threads), ClientHandler, RequestDispatcher
│       └── service/            # 7 Service nghiệp vụ cốt lõi
│
├── Client/                     # Module Frontend (JavaFX)
│   └── src/.../auction/
│       ├── controller/         # 12 FXML Controller (Login, Dashboard, LiveBidding, Admin...)
│       ├── network/            # 7 API Client (Auth, Item, Auction, User, Admin, BidHistory...)
│       └── service/            # Socket reader thread, SessionManager
│   └── src/main/resources/
│       └── view/               # 10 FXML + dark.css / light.css (Dark & Light Theme)
│
├── windows/                    # Script khởi chạy nhanh (.bat)
├── linux_macos/                # Script khởi chạy nhanh (.sh)
├── schema.sql                  # Script khởi tạo database (7 bảng)
├── .env.example                # Cấu hình môi trường mẫu
├── build-release.bat / .sh     # Đóng gói bản phát hành (Fat JAR)
└── pom.xml                     # Parent POM quản lý dependencies chung
```

---

## 4. 🚀 Hướng Dẫn Khởi Chạy (How to Run)

⚠️ Đảm bảo đã bật **MySQL** (cổng `3306`) và import tệp **`schema.sql`** vào database.

### 📦 Cách 1: Chạy bản Build Sẵn (Dành cho Người Dùng Cuối)
*Yêu cầu: Cài **JDK 21+**, **Maven** và **MySQL**.*

1. Đóng gói bản phát hành (chỉ cần chạy **1 lần**):
   * **Windows**: Nhấp đúp **`build-release.bat`** → tự động tạo thư mục `release/`.
   * **Linux/macOS**: Chạy **`./build-release.sh`** → tự động tạo thư mục `release/`.
2. Vào thư mục `release/`, sửa cấu hình MySQL của bạn vào file `.env`.
3. Khởi chạy:
   * **Windows**: Nhấp đúp **`release/windows/run-server.bat`**, rồi nhấp đúp **`release/windows/run-client.bat`**.
   * **Linux/macOS**: Chạy **`release/linux_macos/run-server.sh`**, rồi chạy **`release/linux_macos/run-client.sh`**.
4. 🛑 Dừng ứng dụng: Chạy file `stop.bat` (hoặc `stop.sh`) trong thư mục `release/`.

### 🚀 Cách 2: Chạy tự động bằng Script (Từ Mã Nguồn — Dành cho DEV)
*Yêu cầu: Cài **JDK 21+**, **Maven** và **MySQL**.*

1. Khởi chạy:
   * **Windows**: Nhấp đúp **`windows/run-server.bat`** (tự động check database và tạo `.env`), rồi nhấp đúp **`windows/run-client.bat`**.
   * **Linux/macOS**: Chạy **`linux_macos/run-server.sh`**, rồi chạy **`linux_macos/run-client.sh`**.
2. 🛑 Dừng ứng dụng: `stop.bat server` để chỉ dừng Server, hoặc `stop.bat` để dừng cả hai.

### 💻 Cách 3: Chạy thủ công bằng Dòng Lệnh
*Yêu cầu: Cài **JDK 21+**, **Maven** và **MySQL**.*

1. Tạo file `.env` ở thư mục gốc từ `.env.example` và điền cấu hình MySQL.
2. Biên dịch: `mvn clean compile`
3. Khởi chạy ở 2 terminal riêng biệt:
   * **Server**: `mvn compile exec:java -pl Server -Dfile.encoding=UTF-8`
   * **Client**: `mvn compile exec:java -pl Client -Dfile.encoding=UTF-8`
4. 🛑 Dừng ứng dụng: Nhấn **`Ctrl + C`** tại terminal tương ứng.


---

## 5. ✅ Danh Sách Chức Năng Đã Hoàn Thành

### 🔥 Nhóm chức năng nâng cao
- [x] **Socket Server Virtual Threads (Java 21)** — chịu tải đồng thời 300 client
- [x] **Kiến trúc Event-Driven** — Observer Pattern: AuctionEventBus → LiveRoomManage broadcast
- [x] **Phòng Live thời gian thực** — đếm ngược từng giây, viewer count, broadcast biến động giá
- [x] **Đặt giá an toàn đa luồng** — `synchronized` + DB Transaction, đóng băng/hoàn tiền tự động
- [x] **Chống bắn tỉa (Anti-sniping)** — gia hạn 60s khi bid trong 30s cuối
- [x] **Daemon Scheduler** — quét RAM mỗi 1 giây: chốt phiên, chuyển tiền, đánh SOLD
- [x] **Vật phẩm đa hình OOP** — Factory Pattern: 3 danh mục (Electronics, Art, Vehicles)
- [x] **Cưỡng chế đăng xuất (Force Logout)** — khi bị Admin ban hoặc đổi mật khẩu
- [x] **Dark/Light Theme** — 2 bộ CSS hoàn chỉnh (25KB mỗi theme)
- [x] **CI/CD Pipeline** — GitHub Actions build matrix trên ubuntu/windows/macos

### 📝 Nhóm chức năng cơ bản
- [x] Đăng ký tài khoản (BCrypt) & Đăng nhập phân quyền (Admin/Seller/Bidder)
- [x] Cập nhật thông tin cá nhân & Đổi mật khẩu (xác thực mật khẩu cũ)
- [x] Ví điện tử: Nạp/Rút tiền, quản lý số dư khả dụng + số dư đóng băng
- [x] CRUD vật phẩm đa hình (Điện tử, Mỹ thuật, Phương tiện)
- [x] Tạo/Hủy phiên đấu giá, giám sát trạng thái tự động (Open → Running → Finished)
- [x] Đặt giá thầu trong phòng Live
- [x] Lịch sử đặt giá chi tiết
- [x] Admin: Khóa tài khoản, hủy phiên, gỡ vật phẩm, xem Audit Logs
- [x] Seeding Data tự động khi Server khởi động

---

## 6. 📄 Báo Cáo & Video Demo

- 📑 **Báo cáo Đồ án (PDF):** [Xem tại đây](#) *(Cập nhật link)*
- 🎬 **Video Demo hệ thống:** [Xem tại đây](#) *(Cập nhật link)*
