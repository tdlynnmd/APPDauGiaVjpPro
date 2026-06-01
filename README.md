~~~~~~~~~~~~~~~~~~~~~~~~# 🏗️ HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN THỜI GIAN THỰC (REAL-TIME ONLINE AUCTION SYSTEM)

[![Java Enterprise CI Pipeline](https://github.com/tdlynnmd/APPDauGiaVjpPro/actions/workflows/ci.yaml/badge.svg)](https://github.com/tdlynnmd/APPDauGiaVjpPro/actions/workflows/ci.yaml)

Hệ thống Đấu giá Trực tuyến Thời gian thực là một ứng dụng Client-Server phân tán được phát triển trên nền tảng Java, mô phỏng sàn giao dịch đấu giá tài sản trực tuyến hiệu năng cao. Hệ thống cho phép người dùng đăng ký làm Người bán (Seller) để đăng tải tài sản đa hình, hoặc Người đấu giá (Bidder) để tham gia đặt giá và tương tác thời gian thực trong các phòng đấu giá trực tiếp thông qua kết nối Socket (TCP/IP).

---

## 📖 1. Mô tả Bài Toán & Phạm Vi Hệ Thống

### Bài toán nghiệp vụ
Trong đấu giá trực tuyến, thách thức lớn nhất là đảm bảo **tính đồng bộ**, **tính thời gian thực** và **an toàn giao dịch ví tiền**. Hệ thống giải quyết các vấn đề cốt lõi:
- **Đồng bộ đặt giá (Concurrency Control)**: Đảm bảo khi hàng trăm Bidder cùng đặt giá (bid) tại một mili-giây cuối cùng, chỉ có một mức giá cao nhất hợp lệ được chấp nhận, các giao dịch ví tiền (khóa/mở số dư đóng băng) được xử lý nguyên tử (Atomic Transactions) dưới Database.
- **Tự động hóa vòng đời đấu giá (State Machine)**: Quản lý tự động các phiên đấu giá từ khi mở (OPEN), đang chạy (RUNNING), kết thúc (FINISHED), đến khi bị hủy (CANCELED) thông qua daemon quét thời gian thực trên RAM.
- **Livestream phòng đấu giá (Live Room Real-time Broadcast)**: Cập nhật biến động giá thầu, thời gian đếm ngược từng giây, số lượng người xem trực tiếp đến tất cả client trong phòng ngay lập tức mà không cần tải lại trang (nạp lại UI).
- **Cơ chế chống bắn tỉa (Anti-Sniping/Anti-Bot)**: Tự động gia hạn thời gian kết thúc thêm 60 giây nếu có người đặt giá trong 30 giây cuối cùng, đồng thời lũy tiến bước giá để tránh spam và bot phá hoại.

### Phạm vi hệ thống
Hệ thống hỗ trợ 3 nhóm tác nhân chính:
1. **Người đấu giá (Bidder)**: Quản lý thông tin cá nhân, nạp/rút tiền ảo vào ví khả dụng, đăng ký theo dõi phiên đấu giá, vào phòng xem live, đặt giá thầu, nhận lại tiền đóng băng khi bị outbid, và nhận thông báo thắng cuộc.
2. **Người bán (Seller)**: Tạo và cập nhật vật phẩm đa hình (Điện tử, Mỹ thuật, Phương tiện), thiết lập và kích hoạt phiên đấu giá, tự hủy phòng đấu giá (khi chưa có thầu).
3. **Quản trị viên (Admin)**: Quản lý danh sách người dùng, cưỡng chế khóa/mở khóa tài khoản vi phạm, cưỡng chế hủy phiên đấu giá bất hợp pháp (hoàn tiền tự động cho Bidder dẫn đầu), gỡ bỏ vật phẩm vi phạm, xem nhật ký kiểm toán hệ thống (Audit Logs).

---

## 🛠️ 2. Công Nghệ Sử Dụng & Yêu Cầu Cài Đặt

### Công nghệ sử dụng
- **Core Platform**: Java 21 / Java 25.
- **UI Framework (Client)**: JavaFX 21 (giao diện đồ họa đa nền tảng).
- **Database**: MySQL 8.x / Azure Database for MySQL (sử dụng HikariCP làm Connection Pool hiệu năng cao).
- **Communication Protocol**: Custom Stateless & Stateful Custom Socket TCP/IP (truyền nhận gói tin dưới dạng JSON).
- **JSON Parser**: Google GSON (đã cấu hình Adapter xử lý chuẩn hóa `LocalDateTime` tránh lỗi Reflection trên Java mới).
- **Build Tool**: Maven (Multi-module structure).
- **Security**: Mã hóa mật khẩu một chiều BCrypt (Favre-Bcrypt).

### Yêu cầu cài đặt môi trường
Trước khi chạy ứng dụng, hãy đảm bảo hệ thống của bạn đã cài đặt:
1. **Java Development Kit (JDK)**: Phiên bản từ **21 trở lên**.
2. **Apache Maven**: Phiên bản từ **3.8.x trở lên**.
3. **MySQL Server**: Phiên bản **8.x trở lên** (nếu chạy Database cục bộ dưới localhost).

---

## 📂 3. Cấu Trúc Thư Mục & Các Module Chính

Dự án được thiết kế theo kiến trúc **Multi-Module Maven** nhằm tối ưu hóa tính độc lập và khả năng tái sử dụng mã nguồn:

```text
APPDauGiaVjpPro/
├── common/                  # Module DTO và Enum dùng chung giữa Client & Server
│   ├── src/main/java/com/auction/
│   │   ├── dto/             # Các lớp truyền tải dữ liệu (SocketRequest, SocketResponse, DTOs...)
│   │   ├── enums/           # Định nghĩa các trạng thái (ActionType, ItemStatus, AuctionStatus...)
│   │   └── utils/           # Tiện ích dùng chung (GsonProvider cấu hình LocalDateTime adapter)
│   └── pom.xml
│
├── Server/                  # Module Server (Logic nghiệp vụ, Socket Server, CSDL)
│   ├── src/main/java/com/auction/
│   │   ├── config/          # Cấu hình DatabaseConnection (HikariCP, hỗ trợ SSL Cloud Azure)
│   │   ├── controller/      # Tầng tiếp nhận requests & định cấu trúc responses
│   │   ├── dao/             # Data Access Object (DAO interfaces & implementation JDBC)
│   │   ├── event/           # Hệ thống phân phối sự kiện Observer (AuctionEventBus, Observers)
│   │   ├── manage/          # Bộ quản lý in-memory cache trên RAM (User, Auction, LiveRoom, Product)
│   │   ├── models/          # Các thực thể nghiệp vụ hướng đối tượng (Polymorphic Users, Items...)
│   │   ├── network/         # Hạ tầng SocketServer (sử dụng Virtual Threads), ClientHandler, ClientSession
│   │   └── service/         # Tầng chứa logic nghiệp vụ cốt lõi (Auth, Auction, Item, User)
│   └── pom.xml
│
├── Client/                  # Module Client (JavaFX UI, Socket Client)
│   ├── src/main/java/com/auction/
│   │   ├── controller/      # JavaFX Controller điều khiển giao diện UI (.fxml)
│   │   ├── network/         # API mạng đóng gói socket requests (ClientAuthApi, ClientItemApi...)
│   │   └── service/         # Socket reader thread, SessionManager
│   ├── src/main/resources/  # Tài nguyên giao diện (.fxml, CSS, hình ảnh)
│   └── pom.xml
│
└── pom.xml                  # Parent POM quản lý dependencies chung của toàn dự án
```

---

## 💻 4. Hướng Dẫn Biên Dịch & Chạy Chương Trình (Cross-Platform)

Ứng dụng hỗ trợ chạy trên cả **Windows**, **Linux**, và **macOS** bằng các dòng lệnh chuẩn.

### 🏁 Bước 1: Thiết lập Cơ sở dữ liệu

Hệ thống hỗ trợ hai phương án cấu hình cơ sở dữ liệu tùy thuộc vào môi trường chạy của người dùng:

#### 🔹 Phương án A: Chạy Database Local (Zero Configuration - Khuyên dùng)
Hạ tầng Server đã được lập trình sẵn cơ chế **Zero Configuration**. Nếu không tìm thấy file `.env` trong thư mục gốc, hệ thống sẽ tự động sử dụng kết nối mặc định cho Local MySQL:
* **Host/Port**: `localhost:3306`
* **Username**: `root`
* **Password**: *Trống* (`""`)
* **SSL**: *Tắt* (Tránh lỗi kết nối không được mã hóa trên local)

**Các bước thực hiện:**
1. Khởi động dịch vụ MySQL cục bộ trên máy tính của bạn (đảm bảo chạy ở cổng mặc định `3306`).
2. Sử dụng công cụ quản trị MySQL (như Command Line, DBeaver, Navicat, MySQL Workbench) để chạy toàn bộ tệp SQL **[schema.sql](file:///c:/Users/Admin/IdeaProjects/Project/APPDauGiaVjpPro/schema.sql)** nằm ở thư mục gốc của dự án. Tệp này sẽ tự động tạo cơ sở dữ liệu `vnu_auction_system` và toàn bộ cấu trúc bảng tối ưu hóa.

#### 🔹 Phương án B: Sử dụng Database Tùy chỉnh (Local có mật khẩu hoặc Online Cloud Azure/AWS)
Nếu MySQL Local của bạn sử dụng tài khoản/mật khẩu khác hoặc bạn muốn kết nối tới cơ sở dữ liệu cloud hoạt động trực tuyến:
1. Sao chép tệp mẫu **[.env.example](file:///c:/Users/Admin/IdeaProjects/Project/APPDauGiaVjpPro/.env.example)** ở thư mục gốc và đổi tên thành tệp **`.env`** (Tệp này đã được cấu hình trong `.gitignore` để tránh đẩy mật khẩu cá nhân lên GitHub).
2. Mở tệp `.env` vừa tạo và điền cấu hình tương ứng (ví dụ: `DB_HOST`, `DB_USER`, `DB_PASSWORD`, cổng dịch vụ...).
3. Nếu sử dụng CSDL đám mây yêu cầu kết nối bảo mật nghiêm ngặt, hãy đặt thuộc tính `DB_USE_SSL=true`.
4. Import tệp SQL **[schema.sql](file:///c:/Users/Admin/IdeaProjects/Project/APPDauGiaVjpPro/schema.sql)** vào database đích của bạn để khởi tạo cấu trúc bảng.

---

### 🛠️ Bước 2: Biên dịch và chạy trên các Hệ điều hành

Mở terminal tại thư mục gốc của dự án `APPDauGiaVjpPro` và thực hiện:

#### Lệnh biên dịch dự án (Chung cho mọi hệ điều hành)
```bash
mvn clean compile
```

#### 🚀 Cách 1: Khởi chạy trên hệ điều hành Windows (CMD / PowerShell)
- **Khởi động Server**:
  ```cmd
  mvn -pl Server exec:java
  ```
- **Khởi động Client** (Chạy lệnh này ở một cửa sổ CMD/PowerShell mới):
  ```cmd
  mvn -pl Client javafx:run
  ```

#### 🚀 Cách 2: Khởi chạy trên hệ điều hành Linux / macOS (Terminal Bash/Zsh)
Trước khi chạy, đảm bảo bạn đã cấp quyền thực thi cho các script (nếu có) hoặc gọi trực tiếp qua Maven:
- **Khởi động Server**:
  ```bash
  mvn -pl Server exec:java
  ```
- **Khởi động Client** (Chạy lệnh này ở một cửa sổ Terminal mới):
  ```bash
  mvn -pl Client javafx:run
  ```

---

## 👤 5. Thứ Tự Chạy Thử Nghiệm & Tài Khoản Test

Hệ thống đã tích hợp sẵn cơ chế **Seeding Data** tự động nạp các tài khoản thử nghiệm và dữ liệu mẫu (6 vật phẩm đa hình, 6 phiên đấu giá đang mở) ngay khi Server khởi động để bạn dễ dàng chấm điểm/kiểm thử.

### Thứ tự thực hiện:
1. **Khởi động Server trước**: Đợi cho log terminal báo hạ tầng DB sẵn sàng và in danh sách các phòng đấu giá đã được hydrate thành công lên RAM.
2. **Khởi động Client sau**: Mở màn hình Login.

### Danh sách tài khoản thử nghiệm có sẵn:
| Vai trò | Username | Password | Email | Số dư mặc định |
| :--- | :--- | :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin1` | `Admin@123` | `admin1@auction.com` | N/A |
| **Người bán (Seller)** | `seller1` | `Seller@123` | `seller1@auction.com` | 0.00 VND |
| **Người mua (Bidder)** | `bidder1` | `Bidder@123` | `bidder1@auction.com` | 100,000,000.00 VND |

*(Lưu ý: Mật khẩu test đều tuân thủ chính sách mật khẩu mạnh: ít nhất 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt).*

---

## 🎯 6. Danh Sách Chức Năng Đã Hoàn Thành

### Phân hệ Mạng & Lõi Hệ thống (Server Core)
- [x] **High-Concurrency Socket Server**: Khởi tạo Server Socket sử dụng **Luồng Ảo (Virtual Threads)** giúp chịu tải đồng thời hàng trăm client kết nối mà không bị nghẽn (Thread Pool Starvation).
- [x] **Stateless Request Dispatching**: Bộ điều hướng gói tin tập trung phân tích DTO JSON, kiểm tra an toàn session, tự ngắt các kết nối lỗi (Connection Ma) để giải phóng RAM.
- [x] **Event-Driven Architecture (Observer Pattern)**: Tách biệt hoàn toàn tầng logic nghiệp vụ (`AuctionService`) khỏi giao thức mạng bằng cách đẩy sự kiện lên `AuctionEventBus` và để `LiveRoomManage` thu nhận phát sóng real-time.
- [x] **Safe Database Connection Pool**: Tích hợp HikariCP quản lý pool kết nối, hỗ trợ tự động bật mã hóa SSL mã nguồn khi kết nối Azure Cloud MySQL.

### Phân hệ Người Dùng & Tài Chính (User & Wallet)
- [x] Đăng ký tài khoản (mã hóa BCrypt mật khẩu) & Đăng nhập phân quyền bảo mật (bốc UserId trực tiếp từ Session đầu mạng).
- [x] Đăng xuất an toàn (Giật phích cắm socket vật lý và xóa cache).
- [x] Xem thông tin cá nhân (Profile Dashboard).
- [x] Nạp tiền / Rút tiền ảo (Quản lý số dư khả dụng và số dư đóng băng an toàn giao dịch).

### Phân hệ Vật Phẩm Đa Hình (Polymorphic Items)
- [x] Đăng tải sản phẩm mới (Hỗ trợ 3 nhóm sản phẩm kế thừa đa hình: Điện tử, Mỹ thuật, Phương tiện).
- [x] Xem danh sách sản phẩm cá nhân của Seller.
- [x] Chỉnh sửa thông tin sản phẩm và ẩn sản phẩm (Item deletion).

### Phân hệ Đấu Giá Thời Gian Thực (Real-time Auction & Live Room)
- [x] Tạo phiên đấu giá mới kèm bước giá, thời gian bắt đầu và kết thúc.
- [x] Xem danh sách các phiên đấu giá đang mở/đang chạy.
- [x] Xem chi tiết phiên đấu giá (thông tin vật phẩm, lịch sử 15 lượt thầu gần nhất).
- [x] Vào phòng xem livestream thời gian thực (nhận thông báo người vào/ra, tổng kết đếm ngược giây, biến động giá thầu).
- [x] Đặt giá thầu thắt chặt (Race-condition proof qua `synchronized` và Database Transaction): tự động đóng băng tiền thầu mới, unfreeze trả lại tiền cho người dẫn đầu cũ ngay lập tức.
- [x] Cơ chế chống bắn tỉa (Anti-sniping): tự động gia hạn thời gian kết thúc thầu khi có lượt đặt phút cuối.
- [x] Tự động chốt phiên (Daemon Scheduler quét RAM mỗi 1 giây): khấu trừ tiền đóng băng của người thắng chuyển cho người bán, chuyển trạng thái vật phẩm thành SOLD và giải phóng bộ nhớ.

### Phân hệ Quản Trị Hệ Thống (Admin Panel)
- [x] Tải danh sách người dùng phân trang toàn hệ thống.
- [x] Cưỡng chế khóa tài khoản vi phạm (Ban User) và tự động kích hoạt ngắt kết nối an toàn (Polite Close) sau 300ms.
- [x] Cưỡng chế hủy phiên đấu giá bất hợp pháp (hoàn tiền đặt cọc tự động cho bidder dẫn đầu).
- [x] Cưỡng chế hạ tải vật phẩm vi phạm.
- [x] Xem nhật ký kiểm toán (Audit Logs) lưu vết hành động của Admin.

---

## 📈 7. Báo Cáo & Video Demo Chương Trình

- **Báo cáo Đồ án định dạng PDF**: [Xem Báo cáo PDF tại đây](https://github.com/tdlynnmd/APPDauGiaVjpPro/raw/main/reports/Project_Report.pdf) *(Vui lòng cập nhật đường dẫn chính xác của bạn)*
- **Video Demo vận hành hệ thống**: [Xem Video Demo trên YouTube tại đây](https://youtu.be/your-video-id) *(Vui lòng cập nhật link video thực tế)*
