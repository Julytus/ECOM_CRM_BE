package vn.aeoc.crm.service.other;

import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.config.socket.ChatSocketHandler;
import vn.aeoc.crm.repository.other.NotificationRepository;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.cache.CacheKeyFactory;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.entity.data.mysql.Notification;
import vn.aeoc.entity.data.mysql.auth.User;

import java.time.Duration;
import java.util.List;

@Service
public class NotificationService extends BaseService<
        NotificationRepository,
        Notification,
        Integer> {

    private static final Duration UNREAD_TTL = Duration.ofSeconds(45);

    private final ThreeLayerCacheTemplate cacheTemplate;
    private final ChatSocketHandler socketHandler;
    private final UserService userService;

    public NotificationService(
            NotificationRepository repository,
            ThreeLayerCacheTemplate cacheTemplate,
            ChatSocketHandler socketHandler,
            UserService userService
    ) {
        super(repository);
        this.cacheTemplate = cacheTemplate;
        this.socketHandler = socketHandler;
        this.userService = userService;
    }

    public Page<Notification> getByCriteria(
            Integer userId,
            Integer isRead,
            Integer branchId,
            Pageable pageable
    ) {

        return pageQuery(
                pageable,
                () -> repository.getByCriteria(userId, isRead, branchId, pageable),
                () -> repository.countByCriteria(userId, isRead, branchId)
        );
    }

    public Long getUnreadCount(Integer userId, Integer branchId) {
        String key = CacheKeyFactory.notificationUnreadCount(userId, branchId);
        return cacheTemplate.getOrLoad(key, UNREAD_TTL, () -> repository.countUnread(userId, branchId));
    }

    public Integer markRead(Integer id, Integer userId, Integer branchId) {
        Integer updated = repository.markRead(id);
        evictUnreadCount(userId, branchId);
        return updated;
    }

    public Integer markAllRead(Integer userId, Integer branchId) {
        Integer updated = repository.markAllRead(userId);
        evictUnreadCount(userId, branchId);
        pushUnreadCount(userId, branchId);
        return updated;
    }

    public Notification saveAndNotify(Notification notification) {
        Notification saved = notification.getId() == null || notification.getId() <= 0
                ? repository.insertReturning(notification)
                : repository.update(notification.getId(), notification);

        Integer targetUserId = saved.getUserId();
        if (targetUserId != null) {
            User login = userService.getUserById(targetUserId);
            Integer branchId = login != null ? BranchFilter.listScopeBranchId(login) : null;
            evictUnreadCount(targetUserId, branchId);
            pushUnreadCount(targetUserId, branchId);
            socketHandler.emitNotification(targetUserId, saved);
        }

        return saved;
    }

    public Integer deleteNotification(Integer id, Integer userId, Integer branchId) {
        Notification existing = repository.getById(id);
        if (existing == null) {
            return 0;
        }

        Integer deleted = repository.delete(id);
        Integer targetUserId = existing.getUserId() != null ? existing.getUserId() : userId;
        Integer targetBranchId = branchId;
        if (targetUserId != null && targetBranchId == null) {
            User login = userService.getUserById(targetUserId);
            targetBranchId = login != null ? BranchFilter.listScopeBranchId(login) : null;
        }

        evictUnreadCount(targetUserId, targetBranchId);
        pushUnreadCount(targetUserId, targetBranchId);
        return deleted;
    }

    public void notifyUnreadCount(Integer userId, Integer branchId) {
        pushUnreadCount(userId, branchId);
    }

    private void pushUnreadCount(Integer userId, Integer branchId) {
        Long unread = getUnreadCount(userId, branchId);
        socketHandler.emitNotification(userId, java.util.Map.of(
                "type", "unread_count",
                "unreadCount", unread,
                "branchId", branchId
        ));
    }

    private void evictUnreadCount(Integer userId, Integer branchId) {
        cacheTemplate.evictAll(List.of(
                CacheKeyFactory.notificationUnreadCount(userId, branchId),
                CacheKeyFactory.notificationUnreadCount(userId, 0)
        ));
    }
}
