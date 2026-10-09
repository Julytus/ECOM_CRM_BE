# CRUD Generated Summary - Product Module

## Tổng quan

Đã tạo thành công CRUD cho 7 bảng trong module Product theo template chuẩn của dự án.

## Danh sách Entity & Files đã tạo

### 1. Product
- **Entity**: `vn.aeoc.entity.data.mysql.product.Product` (đã có)
- **Repository**: `ProductRepository.java` (đã cập nhật)
- **Service**: `ProductService.java` ✅
- **Controller**: `ProductController.java` ✅

**API Endpoints:**
- `GET /product/list` - Danh sách sản phẩm (có phân trang, lọc theo id, name, status, categoryId, branchId)
- `GET /product/get?id={id}` - Chi tiết sản phẩm
- `POST /product/update` - Tạo/Cập nhật sản phẩm
- `DELETE /product/delete/{id}` - Xóa sản phẩm

**Features:**
- Join với `category_item` để lấy `category_name`
- Hỗ trợ `BranchFilter` để lọc theo chi nhánh

---

### 2. ProductAttribute
- **Entity**: `vn.aeoc.entity.data.mysql.product.ProductAttribute` (đã có)
- **Repository**: `ProductAttributeRepository.java` ✅
- **Service**: `ProductAttributeService.java` ✅
- **Controller**: `ProductAttributeController.java` ✅

**API Endpoints:**
- `GET /product-attribute/list` - Danh sách thuộc tính (lọc theo fieldName, name, productId)
- `GET /product-attribute/get?id={id}` - Chi tiết thuộc tính
- `POST /product-attribute/update` - Tạo/Cập nhật thuộc tính
- `DELETE /product-attribute/delete/{id}` - Xóa thuộc tính

**Schema:**
```sql
create table product_attribute
(
    id         int auto_increment primary key,
    field_name varchar(100) not null,
    name       varchar(100) not null,
    product_id int          not null,
    constraint uk_attribute_field_name unique (field_name, product_id)
);
```

---

### 3. ProductReview
- **Entity**: `vn.aeoc.entity.data.mysql.product.ProductReview` (đã có)
- **Repository**: `ProductReviewRepository.java` ✅
- **Service**: `ProductReviewService.java` ✅
- **Controller**: `ProductReviewController.java` ✅

**API Endpoints:**
- `GET /product-review/list` - Danh sách đánh giá (lọc theo productId, productVariantId, userId, rating, orderId)
- `GET /product-review/get?id={id}` - Chi tiết đánh giá
- `POST /product-review/update` - Tạo/Cập nhật đánh giá
- `DELETE /product-review/delete/{id}` - Xóa đánh giá

**Features:**
- Sắp xếp theo `created_at` giảm dần (đánh giá mới nhất trước)
- Constraint: unique trên `order_item_id`

**Schema:**
```sql
create table product_review
(
    id                 int auto_increment primary key,
    order_id           int                                 not null,
    order_item_id      int                                 not null,
    product_id         int                                 not null,
    product_variant_id int                                 not null,
    user_id            int                                 not null,
    rating             int                                 not null,
    comment            text                                null,
    files              text                                null,
    created_at         timestamp default CURRENT_TIMESTAMP null,
    constraint uk_review_order_item unique (order_item_id)
);
```

---

### 4. ProductVariant
- **Entity**: `vn.aeoc.entity.data.mysql.product.ProductVariant` (đã có)
- **Repository**: `ProductVariantRepository.java` ✅
- **Service**: `ProductVariantService.java` ✅
- **Controller**: `ProductVariantController.java` ✅

**API Endpoints:**
- `GET /product-variant/list` - Danh sách biến thể (lọc theo productId, sku, minStock, maxStock)
- `GET /product-variant/get?id={id}` - Chi tiết biến thể
- `POST /product-variant/update` - Tạo/Cập nhật biến thể
- `DELETE /product-variant/delete/{id}` - Xóa biến thể

**Features:**
- Join với `product` để lấy `product_name`
- Hỗ trợ lọc theo khoảng stock (minStock, maxStock)
- Constraint: unique trên `sku`

**Schema:**
```sql
create table product_variant
(
    id             int auto_increment primary key,
    product_id     int                                 not null,
    sku            varchar(100)                        null,
    price          double                              null,
    stock          int       default 0                 null,
    created_at     timestamp default CURRENT_TIMESTAMP null,
    image          text                                null,
    original_price double                              null,
    constraint sku unique (sku)
);
```

---

### 5. ProductVariantAttribute (Bảng liên kết)
- **Entity**: `vn.aeoc.entity.data.mysql.product.ProductVariantAttribute` (đã có)
- **Repository**: `ProductVariantAttributeRepository.java` ✅
- **Service**: `ProductVariantAttributeService.java` ✅
- **Controller**: `ProductVariantAttributeController.java` ✅

**API Endpoints:**
- `GET /product-variant-attribute/list` - Danh sách liên kết (lọc theo variantId, attributeValueId)
- `POST /product-variant-attribute/update` - Tạo liên kết
- `DELETE /product-variant-attribute/delete?variantId={variantId}&attributeValueId={attributeValueId}` - Xóa liên kết

**Features:**
- Composite key (variantId, attributeValueId)
- Custom delete method: `deleteByVariantIdAndAttributeValueId`

**Schema:**
```sql
create table product_variant_attribute
(
    variant_id         int not null,
    attribute_value_id int not null
);
```

---

### 6. ProductWishlist (Bảng liên kết)
- **Entity**: `vn.aeoc.entity.data.mysql.product.ProductWishlist` ✅ (mới tạo)
- **Repository**: `ProductWishlistRepository.java` ✅
- **Service**: `ProductWishlistService.java` ✅
- **Controller**: `ProductWishlistController.java` ✅

**API Endpoints:**
- `GET /product-wishlist/list` - Danh sách wishlist (lọc theo userId, productId)
- `POST /product-wishlist/update` - Thêm vào wishlist
- `DELETE /product-wishlist/delete?userId={userId}&productId={productId}` - Xóa khỏi wishlist

**Features:**
- Primary key composite (userId, productId)
- Join với `product` để lấy `product_name`
- Sắp xếp theo `created_at` giảm dần
- Custom delete method: `deleteByUserIdAndProductId`

**Schema:**
```sql
create table product_wishlist
(
    user_id    int                                 not null,
    product_id int                                 not null,
    created_at timestamp default CURRENT_TIMESTAMP null,
    primary key (user_id, product_id)
);
```

---

### 7. AttributeValue
- **Entity**: `vn.aeoc.entity.data.mysql.product.AttributeValue` (đã có)
- **Repository**: `AttributeValueRepository.java` ✅
- **Service**: `AttributeValueService.java` ✅
- **Controller**: `AttributeValueController.java` ✅

**API Endpoints:**
- `GET /attribute-value/list` - Danh sách giá trị thuộc tính (lọc theo attributeId, value)
- `GET /attribute-value/get?id={id}` - Chi tiết giá trị thuộc tính
- `POST /attribute-value/update` - Tạo/Cập nhật giá trị thuộc tính
- `DELETE /attribute-value/delete/{id}` - Xóa giá trị thuộc tính

**Schema:**
```sql
create table attribute_value
(
    id           int auto_increment primary key,
    attribute_id int          not null,
    value        varchar(255) not null
);
```

---

## Cấu trúc thư mục

```
crm/src/main/java/vn/aeoc/crm/
├── controller/product/
│   ├── ProductController.java
│   ├── ProductAttributeController.java
│   ├── ProductReviewController.java
│   ├── ProductVariantController.java
│   ├── ProductVariantAttributeController.java
│   ├── ProductWishlistController.java
│   └── AttributeValueController.java
├── service/product/
│   ├── ProductService.java
│   ├── ProductAttributeService.java
│   ├── ProductReviewService.java
│   ├── ProductVariantService.java
│   ├── ProductVariantAttributeService.java
│   ├── ProductWishlistService.java
│   └── AttributeValueService.java
└── repository/product/
    ├── ProductRepository.java (updated)
    ├── ProductAttributeRepository.java
    ├── ProductReviewRepository.java
    ├── ProductVariantRepository.java
    ├── ProductVariantAttributeRepository.java
    ├── ProductWishlistRepository.java
    └── AttributeValueRepository.java

entity/src/main/java/vn/aeoc/entity/data/mysql/product/
├── Product.java
├── ProductAttribute.java
├── ProductReview.java
├── ProductVariant.java
├── ProductVariantAttribute.java
├── ProductWishlist.java ✅ (mới tạo)
└── AttributeValue.java
```

---

## Convention tuân thủ

✅ **Service**: Kế thừa `BaseService`, dùng `pageQuery()` cho phân trang
✅ **Repository**: Kế thừa `AbsMysqlRepository`, tách `getWhereCondition()` dùng chung cho list/count
✅ **Controller**: Kế thừa `BaseController`, kiểm tra quyền với `checkPermission()`
✅ **API Pattern**: `/resource/list`, `/resource/get`, `/resource/update`, `/resource/delete`
✅ **Permission**: `VIEW`, `ADD`, `UPDATE`, `DELETE`
✅ **Branch Filter**: Áp dụng cho bảng có `branch_id` (Product)
✅ **JOIN**: Chỉ sử dụng khi cần lấy dữ liệu bổ sung (category_name, product_name)
✅ **Composite Key**: Custom delete method cho bảng liên kết

---

## Các bước tiếp theo

1. **Chạy jOOQ code generation** để tạo các bảng jOOQ tương ứng:
   ```bash
   mvn clean compile
   ```

2. **Kiểm tra import** và đảm bảo tất cả các class đều compile thành công

3. **Tạo Resource & Permission** trong database để phân quyền:
   - `/product/` - VIEW, ADD, UPDATE, DELETE
   - `/product-attribute/` - VIEW, ADD, UPDATE, DELETE
   - `/product-review/` - VIEW, ADD, UPDATE, DELETE
   - `/product-variant/` - VIEW, ADD, UPDATE, DELETE
   - `/product-variant-attribute/` - VIEW, ADD, DELETE
   - `/product-wishlist/` - VIEW, ADD, DELETE
   - `/attribute-value/` - VIEW, ADD, UPDATE, DELETE

4. **Test API** với Postman hoặc công cụ tương tự

---

## Lưu ý đặc biệt

- **ProductVariantAttribute** và **ProductWishlist** là bảng liên kết với composite key, không có ID tự tăng
- **ProductReview** có constraint unique trên `order_item_id` - mỗi order item chỉ có 1 review
- **ProductVariant** có constraint unique trên `sku`
- **Product** hỗ trợ branch filter - dữ liệu được lọc theo chi nhánh của user
- Tất cả các bảng đều hỗ trợ phân trang thông qua `Pageable`

---

**Tổng số file đã tạo/cập nhật: 22 files**
- 1 Entity mới (ProductWishlist)
- 7 Repository (6 mới + 1 cập nhật)
- 7 Service mới
- 7 Controller mới
