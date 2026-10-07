package vn.aeoc.crm.util;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.User;

/**
 * Lọc dữ liệu theo chi nhánh: operator ({@code isOperator == 1}) dùng 0 để xem mọi chi nhánh;
 * ngược lại dùng {@code branchId} của user đang đăng nhập.
 */
public final class BranchFilter {

    private BranchFilter() {
    }

    /**
     * @return 0 nếu không giới hạn chi nhánh; &gt; 0 là id chi nhánh cần lọc.
     */
    public static int listScopeBranchId(User login) {
        if (login == null || CommonUtils.NVL(login.getIsOperator()) == 1) {
            return 0;
        }
        return CommonUtils.NVL(login.getBranchId());
    }

    /**
     * Điều kiện jOOQ: khi {@code filterBranchId &gt; 0} thì {@code branchField} phải khớp.
     */
    public static Condition branchEq(Field<Integer> branchField, int filterBranchId) {
        if (filterBranchId <= 0) {
            return DSL.trueCondition();
        }
        return branchField.eq(filterBranchId);
    }
}
