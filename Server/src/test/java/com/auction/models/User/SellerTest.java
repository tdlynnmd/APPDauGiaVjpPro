package com.auction.models.User;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SellerTest {

    /**
     * Test constructor đăng ký mới của Seller.
     * Trong Seller.java, constructor:
     * public Seller(String username, String email, String password)
     * gọi:
     * super(username, email, password, UserRole.SELLER);
     * Vì vậy Seller mới tạo phải có role SELLER.
     */
    @Test
    void newSellerShouldHaveSellerRole() {
        Seller seller = new Seller(
                "seller1",
                "seller1@example.com",
                "hashed-password"
        );

        // getUserRole() trả về enum UserRole
        assertEquals(UserRole.SELLER, seller.getUserRole());

        // getRole() trả về String
        assertEquals("SELLER", seller.getRole());
    }

    /**
     * Test rating ban đầu của Seller.
     * Trong constructor Seller mới, code có:
     * this.rating = 0;
     * Vì vậy seller mới tạo phải có rating bằng 0.
     */
    @Test
    void newSellerShouldHaveZeroRating() {
        Seller seller = new Seller(
                "seller1",
                "seller1@example.com",
                "hashed-password"
        );

        // Seller mới chưa có đánh giá nên rating phải bằng 0
        assertEquals(0.0, seller.getRating());
    }

    /**
     * Test số dư ban đầu của Seller.
     * Vì Seller kế thừa User,
     * constructor User sẽ set:
     * availableBalance = 0
     * frozenBalance = 0
     */
    @Test
    void newSellerShouldHaveZeroBalances() {
        Seller seller = new Seller(
                "seller1",
                "seller1@example.com",
                "hashed-password"
        );

        // Seller mới chưa có tiền khả dụng
        assertEquals(0.0, seller.getAvailableBalance());

        // Seller mới chưa có tiền bị đóng băng
        assertEquals(0.0, seller.getFrozenBalance());
    }

    /**
     * Test status mặc định của Seller mới.
     * Vì Seller gọi constructor User,
     * mà User set status mặc định là ACTIVE.
     */
    @Test
    void newSellerShouldBeActiveByDefault() {
        Seller seller = new Seller(
                "seller1",
                "seller1@example.com",
                "hashed-password"
        );

        // Seller mới tạo phải ở trạng thái ACTIVE
        assertEquals(UserStatus.ACTIVE, seller.getStatus());
    }

    /**
     * Test Seller có thể dùng các method ví tiền kế thừa từ User.
     * Seller cũng là User, nên có:
     * addAvailableBalance()
     * freeze()
     * unfreeze()
     * deductFrozen()
     * Ở đây test nhanh một flow:
     * nạp 300
     * freeze 100
     * expected available = 200, frozen = 100
     */
    @Test
    void sellerShouldUseBalanceMethodsInheritedFromUser() {
        Seller seller = new Seller(
                "seller1",
                "seller1@example.com",
                "hashed-password"
        );

        // Nạp tiền cho Seller
        seller.addAvailableBalance(300.0);

        // Đóng băng 100
        boolean result = seller.freeze(100.0);

        // Freeze phải thành công
        assertTrue(result);

        // availableBalance giảm từ 300 xuống 200
        assertEquals(200.0, seller.getAvailableBalance());

        // frozenBalance tăng lên 100
        assertEquals(100.0, seller.getFrozenBalance());
    }

    /**
     * Test constructor thứ 2 của Seller: constructor load từ DB.
     * Constructor này nhận đủ dữ liệu từ database,
     * gồm cả rating.
     * Mục tiêu:
     * đảm bảo Seller load từ DB giữ đúng username, email, password,
     * role, balance, status và rating.
     */
    @Test
    void dbConstructorShouldRestoreSellerData() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        Seller seller = new Seller(
                "seller-id-1",
                "seller1",
                "seller1@example.com",
                "hashed-password",
                UserRole.SELLER,
                500.0,
                100.0,
                UserStatus.ACTIVE,
                createdAt,
                updatedAt,
                4.5
        );

        // Kiểm tra thông tin cơ bản
        assertEquals("seller1", seller.getUsername());
        assertEquals("seller1@example.com", seller.getEmail());
        assertEquals("hashed-password", seller.getPassword());

        // Kiểm tra role
        assertEquals(UserRole.SELLER, seller.getUserRole());
        assertEquals("SELLER", seller.getRole());

        // Kiểm tra balance được load từ DB
        assertEquals(500.0, seller.getAvailableBalance());
        assertEquals(100.0, seller.getFrozenBalance());

        // Kiểm tra status
        assertEquals(UserStatus.ACTIVE, seller.getStatus());

        // Kiểm tra rating được load từ DB
        assertEquals(4.5, seller.getRating());
    }
}