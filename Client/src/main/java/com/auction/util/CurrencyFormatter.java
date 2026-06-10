package com.auction.util;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurrencyFormatter {

    private static final String[] UNITS = {"", "nghìn", "triệu", "tỷ", "nghìn tỷ", "triệu tỷ"};
    private static final String[] DIGITS = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};

    /**
     * Định dạng số tiền thành chuỗi hiển thị dạng: 15.000.000 VNĐ
     */
    public static String formatCurrency(double amount) {
        DecimalFormat df = new DecimalFormat("#,###");
        return df.format(amount) + " VNĐ";
    }

    /**
     * Định dạng số tiền và đọc thành chữ: 15.000.000 VNĐ (Mười lăm triệu đồng)
     */
    public static String formatCurrencyWithWords(double amount) {
        if (amount < 0) {
            return formatCurrency(amount);
        }
        return formatCurrency(amount) + " (" + numberToVietnameseWords((long) amount) + ")";
    }

    /**
     * Phân tích viết tắt tiền tệ từ người dùng gõ:
     * - "150k" -> "150000"
     * - "2.5m" -> "2500000"
     * - "3tr" -> "3000000"
     * - "1.2t" -> "1200000000"
     */
    public static String parseMoneyShortcut(String input) {
        if (input == null) return null;
        String text = input.trim().toLowerCase().replaceAll(",", "");
        if (text.isEmpty()) return "";

        Pattern pattern = Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)\\s*(k|m|tr|t|tỷ)$");
        Matcher matcher = pattern.matcher(text);
        if (matcher.matches()) {
            try {
                double value = Double.parseDouble(matcher.group(1));
                String suffix = matcher.group(2);
                double multiplier = 1;
                switch (suffix) {
                    case "k":
                        multiplier = 1000;
                        break;
                    case "m":
                    case "tr":
                        multiplier = 1000000;
                        break;
                    case "t":
                    case "tỷ":
                        multiplier = 1000000000;
                        break;
                }
                long finalValue = (long) (value * multiplier);
                return String.valueOf(finalValue);
            } catch (NumberFormatException e) {
                return text;
            }
        }
        return text;
    }

    /**
     * Chuyển đổi một số nguyên thành cách đọc bằng tiếng Việt
     */
    public static String numberToVietnameseWords(long number) {
        if (number == 0) return "Không đồng";
        if (number < 0) return "Âm " + numberToVietnameseWords(-number);

        String words = "";
        int unitIndex = 0;
        long temp = number;

        while (temp > 0) {
            int group = (int) (temp % 1000);
            if (group > 0 || (temp / 1000 > 0 && unitIndex > 0)) {
                // Chỉ hiển thị nhóm 000 nếu có các nhóm cao hơn phía trước cần đọc liên kết
                if (group > 0) {
                    String groupWords = readGroupOfThree(group, temp / 1000 > 0);
                    words = groupWords + (UNITS[unitIndex].isEmpty() ? "" : " " + UNITS[unitIndex]) + (words.isEmpty() ? "" : " " + words);
                }
            }
            temp /= 1000;
            unitIndex++;
        }

        words = words.trim().replaceAll("\\s+", " ") + " đồng";
        if (!words.isEmpty()) {
            words = Character.toUpperCase(words.charAt(0)) + words.substring(1);
        }
        return words;
    }

    private static String readGroupOfThree(int number, boolean hasHigherGroups) {
        int hundreds = number / 100;
        int tens = (number % 100) / 10;
        int ones = number % 10;

        StringBuilder sb = new StringBuilder();

        if (hundreds > 0 || hasHigherGroups) {
            sb.append(DIGITS[hundreds]).append(" trăm ");
        }

        if (tens > 0) {
            if (tens == 1) {
                sb.append("mười ");
            } else {
                sb.append(DIGITS[tens]).append(" mươi ");
            }
        } else if (hundreds > 0 && ones > 0) {
            sb.append("lẻ ");
        }

        if (ones > 0) {
            if (ones == 1 && tens > 1) {
                sb.append("mốt");
            } else if (ones == 5 && tens > 0) {
                sb.append("lăm");
            } else {
                sb.append(DIGITS[ones]);
            }
        }

        return sb.toString().trim();
    }
}
