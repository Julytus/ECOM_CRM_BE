package vn.aeoc.crm.service.other;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.crm.constant.EmailContent;
import vn.aeoc.crm.repository.other.ScheduleRepository;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.entity.data.mysql.Notification;
import vn.aeoc.entity.data.mysql.Schedule;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ScheduleService {
    private final JavaMailSender mailSender;
    private final ScheduleRepository scheduleRepository;
    private final NotificationService notificationService;
    private final UserService userService;

    public Page<Schedule> getList(String keyword, Integer customerId, Integer type, Integer branchId, Pageable pageable) {
        List<Schedule> list = scheduleRepository.getByCriteria(keyword, customerId, type, branchId, pageable);
        Long total = scheduleRepository.countByCriteria(keyword, customerId, type, branchId);

        pageable.setTotal(total);
        return new Page<>(pageable, list);
    }

    public Schedule getById(Integer id) {
        return scheduleRepository.getById(id);
    }

    public Schedule insert(Schedule item) {
        Schedule saved = scheduleRepository.insert(item);
        sendCreateNotification(saved);
        return saved;
    }

    public Schedule update(Schedule item) {
        Schedule updated = scheduleRepository.update(item);
        sendUpdateNotification(updated);
        return updated;
    }

    public Integer delete(Integer id) {
        Schedule existing = scheduleRepository.getById(id);
        Integer deleted = scheduleRepository.delete(id);
        if (deleted != null && deleted > 0) {
            sendDeleteNotification(existing);
        }
        return deleted;
    }

    private void sendCreateNotification(Schedule saved) {
        sendNotification(saved, "Lịch mới được tạo", "Bạn được gán", "một lịch mới");
    }

    private void sendUpdateNotification(Schedule updated) {
        sendNotification(updated, "Lịch hẹn đã được cập nhật", "Lịch hẹn của bạn đã được cập nhật", "");
    }

    private void sendDeleteNotification(Schedule existing) {
        if (existing == null || existing.getUserId() == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(existing.getUserId());
        notification.setType(1);
        notification.setTitle("Lịch hẹn đã bị xoá");
        notification.setContent(buildScheduleDeletionContent(existing));
        notification.setRefType("SCHEDULE");
        notification.setRefId(existing.getId());
        notification.setIsRead(0);
        notificationService.saveAndNotify(notification);
    }

    private void sendNotification(Schedule schedule, String title, String contentPrefix, String fallbackTitle) {
        if (schedule == null || schedule.getUserId() == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(schedule.getUserId());
        notification.setType(1);
        notification.setTitle(title);
        notification.setContent(buildScheduleNotificationContent(schedule, contentPrefix, fallbackTitle));
        notification.setRefType("SCHEDULE");
        notification.setRefId(schedule.getId());
        notification.setIsRead(0);
        notificationService.saveAndNotify(notification);
    }

    private String buildScheduleNotificationContent(Schedule s, String prefix, String fallbackTitle) {
        String title = s.getTitle() != null && !s.getTitle().isBlank()
                ? s.getTitle()
                : fallbackTitle;
        String customer = s.getCustomerName() != null && !s.getCustomerName().isBlank()
                ? " cho khách hàng " + s.getCustomerName()
                : "";
        String time = s.getStartTime() != null
                ? " vào lúc " + s.getStartTime()
                : "";

        return prefix + " " + title + customer + time;
    }

    private String buildScheduleDeletionContent(Schedule s) {
        String title = s.getTitle() != null && !s.getTitle().isBlank()
                ? s.getTitle()
                : "một lịch hẹn";
        String customer = s.getCustomerName() != null && !s.getCustomerName().isBlank()
                ? " của khách hàng " + s.getCustomerName()
                : "";
        String time = s.getStartTime() != null
                ? " vào lúc " + s.getStartTime()
                : "";

        return "Lịch hẹn " + title + customer + time + " đã bị xoá";
    }

    /**
     * Quét & gửi email nhắc lịch
     */
    public void processEmailReminder() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fromTime = now.minusHours(1);

        List<Schedule> schedules =
                scheduleRepository.getSchedulesAfter(fromTime);

        if (schedules.isEmpty()) {
            log.info("No schedules found for email reminder.");
            return;
        }

        for (Schedule s : schedules) {
            try {
                sendEmail(s);
                scheduleRepository.markEmailSent(s.getId());
            } catch (Exception e) {
                log.error("Send email failed for schedule {}", s.getId(), e);
            }
        }
    }
    @Async
    public void sendEmail(Schedule s) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(s.getUserEmail());
        message.setSubject("🔔 Nhắc lịch hẹn sắp tới");
        message.setText(EmailContent.buildEmailContent(s));

        mailSender.send(message);
    }

    /**
     * Chạy mỗi 1 phút
     */
    @Scheduled(cron = "0 */1 * * * ?")
//    @Scheduled(cron = "*/30 * * * * ?")
//    @PostConstruct
    public void run() {
        log.info("Running schedule email reminder task...");
        processEmailReminder();
    }
}
