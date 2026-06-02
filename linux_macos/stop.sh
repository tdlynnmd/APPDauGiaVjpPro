#!/bin/bash
# Di chuyển về thư mục gốc của dự án
cd "$(dirname "$0")/.."

TARGET=$1

echo "=========================================="
echo "  Dừng hệ thống Auction System (Linux/macOS)"
echo "=========================================="

FOUND=0

# Lấy danh sách PID của ServerMain và Launcher bằng jps -l
while read -r line; do
    if [[ -n "$line" ]]; then
        pid=$(echo "$line" | awk '{print $1}')
        class=$(echo "$line" | awk '{print $2}')
        if [[ "$class" == "com.auction.ServerMain" ]]; then
            if [[ -z "$TARGET" || "$TARGET" == "server" ]]; then
                echo "Đang tắt Server (PID: $pid)..."
                kill -9 "$pid" > /dev/null 2>&1
                FOUND=1
            fi
        elif [[ "$class" == "com.auction.Launcher" ]]; then
            if [[ -z "$TARGET" || "$TARGET" == "client" ]]; then
                echo "Đang tắt Client (PID: $pid)..."
                kill -9 "$pid" > /dev/null 2>&1
                FOUND=1
            fi
        fi
    fi
done <<< "$(jps -l 2>/dev/null)"

if [ $FOUND -eq 0 ]; then
    if [ -z "$TARGET" ]; then
        echo "Không tìm thấy tiến trình ServerMain hoặc Launcher nào đang chạy."
    else
        echo "Không tìm thấy tiến trình $TARGET nào đang chạy."
    fi
else
    echo "Đã dừng các tiến trình được chọn thành công!"
fi
