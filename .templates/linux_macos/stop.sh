#!/bin/bash
# Di chuyển về thư mục chứa file jar (thư mục release)
cd "$(dirname "$0")/.."

TARGET=$1

echo "=========================================="
echo "  Dừng hệ thống (Bản Build Sẵn)"
echo "=========================================="

FOUND=0

# Tìm kiếm PID của Server.jar hoặc Client.jar bằng jps -l
while read -r line; do
    if [[ -n "$line" ]]; then
        pid=$(echo "$line" | awk '{print $1}')
        class=$(echo "$line" | awk '{print $2}')
        if [[ "$class" == "Server.jar" ]]; then
            if [[ -z "$TARGET" || "$TARGET" == "server" ]]; then
                echo "Đang tắt Server (PID: $pid)..."
                kill -9 "$pid" > /dev/null 2>&1
                FOUND=1
            fi
        elif [[ "$class" == "Client.jar" ]]; then
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
        echo "Không tìm thấy tiến trình Server.jar hoặc Client.jar nào đang chạy."
    else
        echo "Không tìm thấy tiến trình $TARGET nào đang chạy."
    fi
else
    echo "Đã dừng các tiến trình được chọn thành công!"
fi
