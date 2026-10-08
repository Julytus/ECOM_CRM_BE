package vn.aeoc.crm.repository.auth;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.auth.User;
import vn.entity.backend.tables.records.UserRecord;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static vn.entity.backend.tables.Branch.BRANCH;
import static vn.entity.backend.tables.Role.ROLE;
import static vn.entity.backend.tables.User.USER;

@Repository
public class UserRepository extends AbsMysqlRepository<UserRecord, User, Integer> {
    @Override
    protected TableImpl<UserRecord> getTable() {
        return USER;
    }

    private Table<?> getJoinTable() {
        return USER
                .leftJoin(ROLE).on(ROLE.ID.eq(USER.ROLE_ID))
                .leftJoin(BRANCH).on(BRANCH.ID.eq(USER.BRANCH_ID));
    }

    private static List<Field<?>> getUserFields() {
        return asList(
                USER.ID,
                USER.NAME,
                USER.AVATAR,
                USER.PHONE,
                USER.PHONE_VERIFIED,
                USER.EMAIL,
                USER.FACEBOOK_ID,
                USER.GPLUS_ID,
                USER.REGIS_DATE,
                USER.FIRST_TIME,
                USER.ROLE_ID,
                USER.BRANCH_ID,
                USER.ACTIVE,
                USER.ALIAS,
                USER.CITY_NAME,
                USER.SUBDISTRICT_NAME,
                USER.GENDER
                );
    }

    /**
     * Lấy thông tin người dùng theo số điện thoại (Đăng nhập trực tiếp)
     *
     * @param phone
     * @return
     */
    public User getUserByPhone(String phone) {
        if (StringUtils.isBlank(phone)) {
            return new User();
        }

        List<Field<?>>  fields = new ArrayList<>(getUserFields());
        fields.add(USER.PASSWORD);
        fields.add(USER.PLAIN_PASSWORD);

        return  dslContext
                .select(fields)
                .from(USER)
                .where(USER.PHONE.eq(phone))
                .limit(1) //Sau trả về nhiều đê lựa chọn 1 tài khoản cụ thể
                .fetchOptionalInto(User.class)
                .orElse(new User());
    }

    public Integer deleteUser(Integer id) {
        return dslContext.deleteFrom(USER)
                .where(USER.ID.eq(id))
                .execute();
    }

    public String getUserPasswordById(Integer id) {
        return dslContext.select(USER.PASSWORD).from(USER).where(USER.ID.eq(id))
                .fetchOptionalInto(String.class)
                .orElse(null);
    }

    /**
     * Lấy thông tin người dùng theo số điện thoại (Đăng nhập trực tiếp)
     *
     * @param id
     * @return
     */
    public User getUserById(Integer id) {
        List<Field<?>> selectedFields = new ArrayList<>(getUserFields());

        selectedFields.add(ROLE.NAME.as("role_name"));
        selectedFields.add(BRANCH.NAME.as("branch_name"));
        selectedFields.add(ROLE.IS_OPERATOR.as("is_operator"));

        return  dslContext
                .select(selectedFields)
                .from(getJoinTable())
                .where(USER.ID.eq(id))
                .fetchOptionalInto(User.class)
                .orElse(new User());
    }

    /**
     * Lấy thông tin người dùng theo email (Đăng nhập trực tiếp)
     *
     * @param email
     * @return
     */
    public User getUserByEmail(String email) {
        if (StringUtils.isBlank(email)) {
            new User();
        }
        List<Field<?>>  fields = new ArrayList<>(getUserFields());
        fields.add(USER.PASSWORD);
        fields.add(USER.PLAIN_PASSWORD);

        return dslContext
                .select(fields)
                .from(USER)
                .where(USER.EMAIL.eq(email))
                .limit(1) //Sau trả về nhiều để lựa chọn lấy 1
                .fetchOptionalInto(User.class)
                .orElse(new User());
    }

    public User getUserByEmailAndOtp(String email, String otp) {
        if (StringUtils.isBlank(email)) {
            new User();
        }
        List<Field<?>>  fields = new ArrayList<>(getUserFields());

        return dslContext
                .select(fields)
                .from(USER)
                .where(USER.EMAIL.eq(email).and(USER.OTP.eq(otp)))
                .fetchOptionalInto(User.class)
                .orElse(new User());
    }

    public boolean existByEmailExceptId(String email, Integer userId) {
        return exists(USER.EMAIL.eq(email).and(USER.ID.ne(userId)));
    }

    public boolean existByEmail(String email) {
        return exists(USER.EMAIL.eq(email));
    }

    public boolean existByPhoneExceptId(String phone, Integer userId) {
        if (userId == null) {
            return exists(USER.PHONE.eq(phone));
        }
        return exists(USER.PHONE.eq(phone).and(USER.ID.ne(userId)));
    }

    public User insertUser(User user) {
        List<Field<?>> selectedFields = new ArrayList<>(getUserFields());
        selectedFields.add(
                USER.PASSWORD
        );
        Timestamp ts = new Timestamp(new Date().getTime());
        dslContext.insertInto(USER, selectedFields)
                .values(
                        null,
                        DSL.value(user.getName()),
                        DSL.value(user.getAvatar()),
                        DSL.value(user.getPhone()),
                        DSL.value(user.getPhoneVerified()),
                        DSL.value(user.getEmail()),
                        DSL.value(user.getFacebookId()),
                        DSL.value(user.getGplusId()),
                        ts,
                        DSL.value(user.getFirstTime()),
                        DSL.value(user.getRoleId()),
                        DSL.value(user.getBranchId()),
                        1,
                        DSL.value(user.getAlias()),
                        DSL.value(user.getCityName()),
                        DSL.value(user.getSubdistrictName()),
                        DSL.value(user.getGender()),
                        DSL.value(user.getPassword())
                )
                .execute();
        user.setId(dslContext.lastID().intValue());
        return user;
    }

    public User register(User user) {
        List<Field<?>>  fields = new ArrayList<>(asList(USER.ID, USER.NAME, USER.PASSWORD, USER.PHONE, USER.EMAIL, USER.ACTIVE, USER.ROLE_ID));
        Timestamp ts = new Timestamp(new Date().getTime());
        dslContext.insertInto(USER, fields)
                .values(
                        null,
                        DSL.value(user.getName()),
                        DSL.value(user.getPassword()),
                        DSL.value(user.getPhone()),
                        DSL.value(user.getEmail()),
                        1,
                        DSL.value(user.getRoleId())
                )
                .execute();
        user.setId(dslContext.lastID().intValue());
        return user;
    }

    /**
     * Cập nhật thông tin người dùng (Admin thực hiện)
     *
     * @param user
     * @return
     */
    public User updateUser(User user) {
        updateIgnoreNull(user.getId(), user);
        return user;
    }

    public User updatePass(User user) {
        dslContext.update(USER)
                .set(USER.PASSWORD, user.getPassword())
                .where(USER.ID.eq(user.getId()))
                .execute();
        return user;
    }

    /**
     * Cập nhật thông tin người dùng (Người dùng tự thực hiện)
     *
     * @param user
     * @return
     */
    public User updateInfo(User user) {
        LocalDateTime ts = LocalDateTime.now();

        dslContext.update(USER)
                .set(USER.NAME, user.getName())
                .set(USER.AVATAR, user.getAvatar())
                .set(USER.PHONE, user.getPhone())
                .set(USER.EMAIL, user.getEmail())
                .set(USER.GENDER, user.getGender())
                .set(USER.ALIAS, user.getAlias())
                .set(USER.SUBDISTRICT_NAME, user.getSubdistrictName())
                .set(USER.CITY_NAME, user.getCityName())
                .set(USER.UPDATED_AT, ts)
                .where(USER.ID.eq(user.getId()))
                .execute();
        return user;
    }

    private Condition getWhereCondition(List<Integer> lstUserId, Integer branchId, String query) {
        Condition condition = DSL.trueCondition();

        if (lstUserId != null && !lstUserId.isEmpty()) {
            condition = condition.and(USER.ID.in(lstUserId));
        }

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(USER.BRANCH_ID.eq(branchId));
        }

        if (isNotEmpty(query)) {
            query = query.trim().toLowerCase();
            condition = condition.and(
                    DSL.lower(USER.NAME) .like(DSL.val("%" + query.trim().toLowerCase() + "%"))
                            .or(USER.PHONE.likeRegex(query))
                            .or(DSL.lower(USER.EMAIL) .like(DSL.val("%" + query.trim().toLowerCase() + "%")))
            );
        }

        return condition;
    }

    /**
     * Tìm kiếm người dùng trong select box
     *
     * @return
     */
    public List<User> getUserByCriteria(List<Integer> lstUserId, String query,
                                        Pageable pageable) {
        Condition condition = this.getWhereCondition(lstUserId, null, query);

        List<Field<?>> lstFields = new ArrayList<>(getUserFields());

        lstFields.addAll(
                asList(
                        ROLE.NAME.as("role_name"),
                        BRANCH.NAME.as("branch_name"))
        );

        return getListByCriteriaWithJoin(lstFields, getJoinTable(), condition, USER.ID.desc(), pageable);
    }

    public List<User> getUserByCriteria(List<Integer> lstUserId,
                                        Integer branchId,
                                        String query,
                                        Pageable pageable, Integer excludeUserId) {
        Condition condition = this.getWhereCondition(lstUserId, branchId, query);

        if (excludeUserId != null) {
            condition = condition.and(USER.ID.ne(excludeUserId));
        }

        List<Field<?>> lstFields = new ArrayList<>(getUserFields());

        lstFields.addAll(
                asList(
                        ROLE.NAME.as("role_name"),
                        BRANCH.NAME.as("branch_name"))
        );

        return getListByCriteriaWithJoin(lstFields, getJoinTable(), condition, USER.ID.desc(), pageable);
    }

    public Long countUserByCriteria(List<Integer> lstUserId, String query) {
        Condition condition = this.getWhereCondition(lstUserId, null, query);
        return getCountWithCriteria(condition);
    }

    public Long countUserByCriteria(List<Integer> lstUserId,
                                    Integer branchId,
                                    String query, Integer excludeUserId) {
        Condition condition = this.getWhereCondition(lstUserId, branchId, query);
        if (excludeUserId != null) {
            condition = condition.and(USER.ID.ne(excludeUserId));
        }
        return getCountWithCriteria(condition);
    }

    public Integer updateUserStatus(Integer id, Integer active) {
        return updateField(id, USER.ACTIVE, CommonUtils.NVL(active));
    }

    public Integer updateOtp(Integer userId, String otp) {
        return updateField(userId, USER.OTP, otp);
    }

    public Integer clearOtp(Integer userId) {
        return updateField(userId, USER.OTP, null);
    }

    public List<User> getUserCursor(Integer beforeUserId, String keyword, int limit, Integer excludeUserId) {
        Condition condition = DSL.trueCondition();

        if (beforeUserId != null) {
            condition = condition.and(USER.ID.lt(beforeUserId));
        }

        if (excludeUserId != null) {
            condition = condition.and(USER.ID.ne(excludeUserId));
        }

        if (StringUtils.isNotBlank(keyword)) {
            String like = "%" + keyword.trim().toLowerCase() + "%";
            condition = condition.and(
                    DSL.lower(USER.NAME).like(like)
                            .or(USER.PHONE.like(like))
                            .or(DSL.lower(USER.EMAIL).like(like))
            );
        }

        return dslContext
                .select(getUserFields())
                .from(USER)
                .where(condition)
                .orderBy(USER.ID.desc())
                .limit(limit)
                .fetchInto(User.class);
    }
}
