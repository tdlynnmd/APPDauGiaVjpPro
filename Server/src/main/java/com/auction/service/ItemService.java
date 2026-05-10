package com.auction.service;

public class ItemService {
    //private ItemDAO itemDAO;

    // 1. Tạo vật phẩm mới (Gọi ItemFactory kiểm tra dữ liệu, ném lỗi nếu sai)
    //public boolean createItem(String type, Map<String, Object> data) { ... }

    // 2. [HÀM CỦA SELLER] - Lấy danh sách vật phẩm để hiện lên bảng JavaFX
    /*public List<ItemSummaryDTO> getSellerItems(String sellerId, String statusFilter) {
        // 1. Dùng ItemDAO tìm tất cả Item có ownerId = sellerId
        List<Item> items = itemDAO.findBySellerIdAndStatus(sellerId, statusFilter);

        // 2. Chuyển đổi sang DTO siêu nhẹ
        List<ItemSummaryDTO> result = new ArrayList<>();
        for (Item item : items) {
            result.add(new ItemSummaryDTO(
                    item.getId(),
                    item.getName(),
                    item.getStartingPrice(),
                    item.getItemType().toString() // "ART", "ELECTRONICS"...
            ));
        }
        return result;
    }*/

    // 3. Lấy chi tiết 1 vật phẩm (Khi click đúp vào bảng)
    //public Item getDetailedItem(String itemId) { ... }

}
