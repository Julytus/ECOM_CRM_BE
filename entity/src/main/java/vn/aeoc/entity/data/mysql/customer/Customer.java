package vn.aeoc.entity.data.mysql.customer;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class Customer {
    private Integer id;
    private String name;
    private String avatar;
    private Integer gender; // 1 - Nữ, 2 - Nam
    private Integer age;
    private Integer sourceId;  // Nguồn khách hàng
    private String sourceName;
    private String address;
    private String phone;
    private String email;
    private LocalDate birthday;
    private Integer height;
    private Double weight;
    private Integer userId;      // Người phụ trách khách hàng
    private String userName;
    private Integer opportunityCount;

    private Integer creatorId;   // Người tạo
    private String creatorName;

    private String note;
    private LocalDateTime createdTime = LocalDateTime.now();
    private LocalDateTime updatedTime = LocalDateTime.now();
    private LocalDateTime latestContact;

    private Integer branchId;

    private String segmentCode;

    //Dữ liệu submit trên form xuống
    private List<CustomerExtraInfo> customerExtraInfos;

    //Chứa thông tin bổ trợ (thuộc tính bổ sung khách hàng)
    private Map<Integer, List<CustomerAttribute>> mapCustomerAttribute;

}
