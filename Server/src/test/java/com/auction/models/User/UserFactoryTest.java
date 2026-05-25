package com.auction.models.User;

import com.auction.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserFactoryTest {

    /**
     * Hàm này chạy trước mỗi test.
     * UserFactory của project dùng registry:
     * Map<UserRole, UserFactory>
     * Muốn createUser() hoạt động,
     * ta phải đăng ký factory cho từng role trước.
     * Trong Main.java server cũng làm tương tự:
     * BIDDER -> BidderFactory
     * SELLER -> SellerFactory
     * ADMIN -> AdminFactory
     */
    @BeforeEach
    void setUp() {
        UserFactory.setRegistry(UserRole.BIDDER, new BidderFactory());
        UserFactory.setRegistry(UserRole.SELLER, new SellerFactory());
        UserFactory.setRegistry(UserRole.ADMIN, new AdminFactory());
    }

    /**
     * Test UserFactory tạo Bidder.
     * Khi gọi:
     * UserFactory.createUser(UserRole.BIDDER, ...)
     * Expected:
     * object tạo ra phải là Bidder
     * username/email đúng
     * role đúng là BIDDER
     */
    @Test
    void createUserShouldCreateBidderWhenRoleIsBidder() {
        Bidder bidder = UserFactory.createUser(
                UserRole.BIDDER,
                "bidder1",
                "bidder1@example.com",
                "Bidder@123"
        );

        // Object không được null
        assertNotNull(bidder);

        // Object phải đúng kiểu Bidder
        assertInstanceOf(Bidder.class, bidder);

        // Kiểm tra dữ liệu cơ bản
        assertEquals("bidder1", bidder.getUsername());
        assertEquals("bidder1@example.com", bidder.getEmail());

        // Kiểm tra role
        assertEquals(UserRole.BIDDER, bidder.getUserRole());
        assertEquals("BIDDER", bidder.getRole());
    }

    /**
     * Test UserFactory tạo Seller.
     * Khi role là SELLER,
     * factory phải trả về object Seller.
     */
    @Test
    void createUserShouldCreateSellerWhenRoleIsSeller() {
        Seller seller = UserFactory.createUser(
                UserRole.SELLER,
                "seller1",
                "seller1@example.com",
                "Seller@123"
        );

        // Object không được null
        assertNotNull(seller);

        // Object phải đúng kiểu Seller
        assertInstanceOf(Seller.class, seller);

        // Kiểm tra dữ liệu cơ bản
        assertEquals("seller1", seller.getUsername());
        assertEquals("seller1@example.com", seller.getEmail());

        // Kiểm tra role
        assertEquals(UserRole.SELLER, seller.getUserRole());
        assertEquals("SELLER", seller.getRole());
    }

    /**
     * Test UserFactory tạo Admin.
     * Khi role là ADMIN,
     * factory phải trả về object Admin.
     */
    @Test
    void createUserShouldCreateAdminWhenRoleIsAdmin() {
        Admin admin = UserFactory.createUser(
                UserRole.ADMIN,
                "admin1",
                "admin1@example.com",
                "Admin@123"
        );

        // Object không được null
        assertNotNull(admin);

        // Object phải đúng kiểu Admin
        assertInstanceOf(Admin.class, admin);

        // Kiểm tra dữ liệu cơ bản
        assertEquals("admin1", admin.getUsername());
        assertEquals("admin1@example.com", admin.getEmail());

        // Kiểm tra role
        assertEquals(UserRole.ADMIN, admin.getUserRole());
        assertEquals("ADMIN", admin.getRole());
    }

    /**
     * Test password có được hash hay không.
     * Trong UserFactory.createUser(),
     * password plain text được hash bằng BCrypt trước khi tạo user.
     * Vì vậy password lưu trong user không được bằng password gốc.
     */
    @Test
    void createUserShouldHashPassword() {
        Bidder bidder = UserFactory.createUser(
                UserRole.BIDDER,
                "bidder1",
                "bidder1@example.com",
                "Bidder@123"
        );

        // Password lưu trong object không được là plain text
        assertNotEquals("Bidder@123", bidder.getPassword());

        // BCrypt hash thường bắt đầu bằng $2a$, $2b$ hoặc $2y$
        assertTrue(
                bidder.getPassword().startsWith("$2a$")
                        || bidder.getPassword().startsWith("$2b$")
                        || bidder.getPassword().startsWith("$2y$")
        );
    }

    /**
     * Test checkPassword() với password đúng.
     * UserFactory hash password khi tạo user.
     * User.checkPassword() dùng BCrypt để verify plain password với hash.
     * Nếu nhập đúng password gốc,
     * checkPassword() phải trả về true.
     */
    @Test
    void createdUserShouldVerifyCorrectPassword() {
        Bidder bidder = UserFactory.createUser(
                UserRole.BIDDER,
                "bidder1",
                "bidder1@example.com",
                "Bidder@123"
        );

        // Password đúng phải verify thành công
        assertTrue(bidder.checkPassword("Bidder@123"));
    }

    /**
     * Test checkPassword() với password sai.
     * Nếu nhập sai password,
     * BCrypt verify phải thất bại và trả về false.
     */
    @Test
    void createdUserShouldRejectWrongPassword() {
        Bidder bidder = UserFactory.createUser(
                UserRole.BIDDER,
                "bidder1",
                "bidder1@example.com",
                "Bidder@123"
        );

        // Password sai phải bị từ chối
        assertFalse(bidder.checkPassword("WrongPassword"));
    }

    /**
     * Test createUser() với role null.
     * Khi role null, registry.get(null) sẽ không tìm được factory.
     * Code hiện tại sẽ throw IllegalArgumentException.
     * Test này đảm bảo hệ thống không tạo user với role không hợp lệ.
     */
    @Test
    void createUserShouldThrowExceptionWhenRoleIsNull() {
        assertThrows(IllegalArgumentException.class, () -> UserFactory.createUser(
                null,
                "user1",
                "user1@example.com",
                "Password@123"
        ));
    }
}