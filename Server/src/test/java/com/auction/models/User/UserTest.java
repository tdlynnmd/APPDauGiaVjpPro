package com.auction.models.User;

import com.auction.enums.UserRole;
import com.auction.enums.UserStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    /**
     * Test constructor của User thông qua class con Bidder.
     * Vì User là abstract class nên không thể new User() trực tiếp.
     * Ta tạo Bidder để kiểm tra các thông tin chung của User:
     * - username
     * - email
     * - password
     * - role dạng String
     * - role dạng enum UserRole
     */
    @Test
    void newUserShouldStoreBasicInformation() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "hashed-password"
        );

        // Kiểm tra username truyền vào constructor có được lưu đúng không
        assertEquals("bidder1", bidder.getUsername());

        // Kiểm tra email truyền vào constructor có được lưu đúng không
        assertEquals("bidder1@example.com", bidder.getEmail());

        // Kiểm tra password truyền vào constructor có được lưu đúng không
        assertEquals("hashed-password", bidder.getPassword());

        // getRole() trong User đang trả về String, nên kỳ vọng là "BIDDER"
        assertEquals("BIDDER", bidder.getRole());

        // getUserRole() trả về enum UserRole thật, nên kỳ vọng là UserRole.BIDDER
        assertEquals(UserRole.BIDDER, bidder.getUserRole());
    }

    /**
     * Test số dư ban đầu của user mới.
     *
     * Theo constructor User hiện tại:
     * availableBalance = 0
     * frozenBalance = 0
     *
     * availableBalance: số tiền có thể dùng để đấu giá.
     * frozenBalance: số tiền đang bị khóa/đóng băng khi tham gia đấu giá.
     */
    @Test
    void newUserShouldHaveZeroBalances() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "hashed-password"
        );

        // User mới chưa nạp tiền nên số dư khả dụng phải bằng 0
        assertEquals(0.0, bidder.getAvailableBalance());

        // User mới chưa bid nên số tiền bị đóng băng phải bằng 0
        assertEquals(0.0, bidder.getFrozenBalance());
    }

    /**
     * Test trạng thái mặc định của user mới.
     *
     * Trong constructor User hiện tại, status được set là UserStatus.ACTIVE.
     * Nghĩa là user mới tạo ra mặc định đang hoạt động.
     */
    @Test
    void newUserShouldBeActiveByDefault() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "hashed-password"
        );

        // Kiểm tra user mới có trạng thái ACTIVE không
        assertEquals(UserStatus.ACTIVE, bidder.getStatus());
    }

    /**
     * Test method setStatus().
     *
     * Method này dùng để đổi trạng thái tài khoản,
     * ví dụ từ ACTIVE sang BANNED/LOCKED/INACTIVE tùy enum của project.
     *
     * Lưu ý:
     * Nếu enum UserStatus của bạn không có BANNED,
     * hãy đổi UserStatus.BANNED thành trạng thái thật đang có trong project.
     */
    @Test
    void setStatusShouldUpdateUserStatus() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "hashed-password"
        );

        // Đổi trạng thái user sang BANNED
        bidder.setStatus(UserStatus.BANNED);

        // Kiểm tra getStatus() có trả về trạng thái mới không
        assertEquals(UserStatus.BANNED, bidder.getStatus());
    }

    /**
     * Test addAvailableBalance() với số tiền hợp lệ.
     *
     * Method addAvailableBalance(amount) chỉ cộng tiền nếu amount > 0.
     *
     * Case này:
     * ban đầu availableBalance = 0
     * cộng thêm 100
     * expected availableBalance = 100
     * frozenBalance vẫn = 0
     */
    @Test
    void addAvailableBalanceShouldIncreaseBalanceWhenAmountIsPositive() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "hashed-password"
        );

        // Nạp thêm 100 vào số dư khả dụng
        bidder.addAvailableBalance(100.0);

        // Số dư khả dụng phải tăng lên 100
        assertEquals(100.0, bidder.getAvailableBalance());

        // Nạp tiền không được làm thay đổi số dư đóng băng
        assertEquals(0.0, bidder.getFrozenBalance());
    }

    /**
     * Test addAvailableBalance() với amount = 0.
     *
     * Theo code User:
     * if (amount > 0) this.availableBalance += amount;
     *
     * Vì 0 không lớn hơn 0 nên method phải bỏ qua,
     * số dư không được thay đổi.
     */
    @Test
    void addAvailableBalanceShouldIgnoreZeroAmount() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "hashed-password"
        );

        // Thử cộng 0 vào số dư
        bidder.addAvailableBalance(0.0);

        // Số dư vẫn phải là 0
        assertEquals(0.0, bidder.getAvailableBalance());
    }

    /**
     * Test addAvailableBalance() với số âm.
     *
     * Người dùng không được nạp số tiền âm,
     * vì điều đó có thể làm sai số dư tài khoản.
     *
     * Code hiện tại bỏ qua amount <= 0,
     * nên availableBalance vẫn phải giữ nguyên.
     */
    @Test
    void addAvailableBalanceShouldIgnoreNegativeAmount() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "hashed-password"
        );

        // Thử cộng số âm vào số dư
        bidder.addAvailableBalance(-50.0);

        // Số dư vẫn phải là 0 vì amount âm bị bỏ qua
        assertEquals(0.0, bidder.getAvailableBalance());
    }

    /**
     * Test freeze() khi user có đủ tiền.
     *
     * freeze(amount) dùng khi bidder đặt giá:
     * hệ thống chuyển một phần tiền từ availableBalance sang frozenBalance.
     *
     * Case này:
     * availableBalance ban đầu = 200
     * freeze 80
     *
     * Expected:
     * availableBalance = 120
     * frozenBalance = 80
     * method trả về true
     */
    @Test
    void freezeShouldMoveMoneyFromAvailableToFrozenWhenEnoughBalance() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "hashed-password"
        );

        // Chuẩn bị số dư khả dụng là 200
        bidder.addAvailableBalance(200.0);

        // Đóng băng 80
        boolean result = bidder.freeze(80.0);

        // Vì đủ tiền nên freeze phải thành công
        assertTrue(result);

        // availableBalance giảm 80: 200 - 80 = 120
        assertEquals(120.0, bidder.getAvailableBalance());

        // frozenBalance tăng 80
        assertEquals(80.0, bidder.getFrozenBalance());
    }

    /**
     * Test freeze() khi user không đủ tiền.
     *
     * Case này:
     * availableBalance = 50
     * muốn freeze 80
     *
     * Vì không đủ tiền nên:
     * - method trả về false
     * - availableBalance giữ nguyên
     * - frozenBalance giữ nguyên
     */
    @Test
    void freezeShouldReturnFalseWhenBalanceIsNotEnough() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "hashed-password"
        );

        // Chỉ có 50 trong ví khả dụng
        bidder.addAvailableBalance(50.0);

        // Thử đóng băng 80, lớn hơn số dư hiện có
        boolean result = bidder.freeze(80.0);

        // Không đủ tiền nên freeze thất bại
        assertFalse(result);

        // availableBalance không được thay đổi
        assertEquals(50.0, bidder.getAvailableBalance());

        // frozenBalance vẫn là 0
        assertEquals(0.0, bidder.getFrozenBalance());
    }

    /**
     * Test unfreeze() khi frozenBalance đủ tiền.
     *
     * unfreeze(amount) dùng để giải phóng tiền đã bị đóng băng,
     * ví dụ khi bidder thua đấu giá hoặc bid bị thay thế.
     *
     * Case:
     * availableBalance = 200
     * freeze 80 => available = 120, frozen = 80
     * unfreeze 30
     *
     * Expected:
     * availableBalance = 150
     * frozenBalance = 50
     */
    @Test
    void unfreezeShouldMoveMoneyFromFrozenToAvailableWhenEnoughFrozenBalance() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "hashed-password"
        );

        // Nạp 200
        bidder.addAvailableBalance(200.0);

        // Đóng băng 80
        bidder.freeze(80.0);

        // Giải phóng 30 từ frozenBalance về availableBalance
        bidder.unfreeze(30.0);

        // availableBalance: 120 + 30 = 150
        assertEquals(150.0, bidder.getAvailableBalance());

        // frozenBalance: 80 - 30 = 50
        assertEquals(50.0, bidder.getFrozenBalance());
    }

    /**
     * Test unfreeze() khi frozenBalance không đủ.
     *
     * Case:
     * frozenBalance = 80
     * muốn unfreeze 100
     *
     * Vì frozenBalance không đủ nên method không làm gì.
     */
    @Test
    void unfreezeShouldDoNothingWhenFrozenBalanceIsNotEnough() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "hashed-password"
        );

        // Nạp 200 và đóng băng 80
        bidder.addAvailableBalance(200.0);
        bidder.freeze(80.0);

        // Thử giải phóng 100, lớn hơn frozenBalance hiện tại
        bidder.unfreeze(100.0);

        // Số dư không được thay đổi
        assertEquals(120.0, bidder.getAvailableBalance());
        assertEquals(80.0, bidder.getFrozenBalance());
    }

    /**
     * Test deductFrozen() khi frozenBalance đủ.
     *
     * deductFrozen(amount) dùng khi user thắng đấu giá:
     * tiền đã đóng băng sẽ bị trừ thật.
     *
     * Case:
     * available = 200
     * freeze 80 => available = 120, frozen = 80
     * deductFrozen 50
     *
     * Expected:
     * availableBalance vẫn = 120
     * frozenBalance = 30
     */
    @Test
    void deductFrozenShouldDecreaseFrozenBalanceWhenEnoughFrozenBalance() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "hashed-password"
        );

        // Nạp 200 và đóng băng 80
        bidder.addAvailableBalance(200.0);
        bidder.freeze(80.0);

        // Trừ 50 từ số tiền đang bị đóng băng
        bidder.deductFrozen(50.0);

        // availableBalance không thay đổi vì tiền bị trừ từ frozenBalance
        assertEquals(120.0, bidder.getAvailableBalance());

        // frozenBalance giảm từ 80 xuống 30
        assertEquals(30.0, bidder.getFrozenBalance());
    }

    /**
     * Test deductFrozen() khi frozenBalance không đủ.
     *
     * Case:
     * frozenBalance = 80
     * muốn deductFrozen 100
     *
     * Vì không đủ frozenBalance nên method không làm gì.
     */
    @Test
    void deductFrozenShouldDoNothingWhenFrozenBalanceIsNotEnough() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "hashed-password"
        );

        // Nạp 200 và đóng băng 80
        bidder.addAvailableBalance(200.0);
        bidder.freeze(80.0);

        // Thử trừ 100 từ frozenBalance
        bidder.deductFrozen(100.0);

        // Không đủ frozenBalance nên cả hai số dư giữ nguyên
        assertEquals(120.0, bidder.getAvailableBalance());
        assertEquals(80.0, bidder.getFrozenBalance());
    }

    /**
     * Test setAvailableBalance().
     * <p>
     * Method này thay thế trực tiếp availableBalance.
     * Thường dùng khi DAO load dữ liệu từ database hoặc cập nhật số dư.
     */
    @Test
    void setAvailableBalanceShouldReplaceAvailableBalance() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "hashed-password"
        );

        // Set trực tiếp availableBalance thành 999
        bidder.setAvailableBalance(999.0);

        // Kiểm tra số dư đã được thay thế đúng chưa
        assertEquals(999.0, bidder.getAvailableBalance());
    }

    /**
     * Test setPassword().
     *
     * Method này dùng để cập nhật password.
     * Trong thực tế password nên là chuỗi đã hash,
     * không nên lưu plain text.
     */
    @Test
    void setPasswordShouldUpdatePassword() {
        Bidder bidder = new Bidder(
                "bidder1",
                "bidder1@example.com",
                "old-password"
        );

        // Cập nhật password mới
        bidder.setPassword("new-password");

        // Kiểm tra password đã đổi chưa
        assertEquals("new-password", bidder.getPassword());
    }

    /**
     * Test setEmail().
     *
     * Expected đúng:
     * email cũ là old@example.com
     * gọi setEmail("new@example.com")
     * getEmail() phải trả về new@example.com
     *
     * Lưu ý:
     * Code User.java hiện tại đang có bug:
     * public void setEmail(String newEmail) {
     *     this.email = email;
     * }
     *
     * Đúng phải là:
     * this.email = newEmail;
     *
     * Vì vậy test này có thể fail cho đến khi bạn sửa User.java.
     */
    @Test
    void setEmailShouldUpdateEmail_ButCurrentlyThisTestWillExposeBug() {
        Bidder bidder = new Bidder(
                "bidder1",
                "old@example.com",
                "hashed-password"
        );

        // Thử đổi email
        bidder.setEmail("new@example.com");

        // Kỳ vọng email phải đổi sang new@example.com
        assertEquals("new@example.com", bidder.getEmail());
    }

    /**
     * Test setUsername().
     *
     * Expected đúng:
     * username cũ là oldUsername
     * gọi setUsername("newUsername")
     * getUsername() phải trả về newUsername
     *
     * Lưu ý:
     * Code User.java hiện tại đang có bug:
     * public void setUsername(String newUsername) {
     *     this.username = username;
     * }
     *
     * Đúng phải là:
     * this.username = newUsername;
     *
     * Vì vậy test này có thể fail cho đến khi bạn sửa User.java.
     */
    @Test
    void setUsernameShouldUpdateUsername_ButCurrentlyThisTestWillExposeBug() {
        Bidder bidder = new Bidder(
                "oldUsername",
                "bidder1@example.com",
                "hashed-password"
        );

        // Thử đổi username
        bidder.setUsername("newUsername");

        // Kỳ vọng username phải đổi sang newUsername
        assertEquals("newUsername", bidder.getUsername());
    }
}