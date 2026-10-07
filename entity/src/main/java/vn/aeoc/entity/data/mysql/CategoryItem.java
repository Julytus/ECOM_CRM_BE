package vn.aeoc.entity.data.mysql;

import lombok.Data;

import java.io.Serializable;

@Data
public class CategoryItem implements Serializable {

    private Integer id;

    private String avatar;

    private String name;

    /**
     * 1 - dịch vụ
     * 2 - sản phẩm
     */
    private Integer type;

    private Integer parentId;

    private String featured;

    private Integer position;

    /**
     * 1 - hoạt động
     * 0 - ngừng hoạt động
     */
    private Integer active;

    private Integer branchId;
}
