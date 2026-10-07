package vn.aeoc.crm.service.segment;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.crm.config.property.CustomerSegmentProperties;
import vn.aeoc.crm.constant.CustomerSegmentConstant;
import vn.aeoc.crm.data.segment.*;
import vn.aeoc.crm.repository.segment.CustomerSegmentHistoryRepository;
import vn.aeoc.crm.repository.segment.CustomerSegmentRepository;
import vn.aeoc.crm.repository.segment.SegmentJobRunRepository;
import vn.aeoc.crm.service.cache.CacheKeyFactory;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerSegmentService {

    private static final Duration SUMMARY_CACHE_TTL = Duration.ofSeconds(60);
    private static final int CHECKPOINT_UPDATE_EVERY_CHUNK = 1;

    private final CustomerSegmentRepository segmentRepository;
    private final CustomerSegmentHistoryRepository historyRepository;
    private final SegmentJobRunRepository jobRunRepository;
    private final ThreeLayerCacheTemplate cacheTemplate;
    private final CustomerSegmentProperties properties;

    private final ConcurrentHashMap<String, SegmentRecomputeProgress> progressMap = new ConcurrentHashMap<>();
    @Resource(name = "dbExecutor")
    private Executor dbExecutor;

    @Scheduled(cron = "${app.segment.full-cron:0 0 1 * * ?}")
    public void scheduledFullRecompute() {
        if (!properties.isEnabled()) return;
        recomputeAll(null, "SCHEDULED");
    }

    public String recomputeAll(Integer branchId, String triggerSource) {
        SegmentJobRunRow latest = jobRunRepository.getLatestByBranch(branchId);
        if (latest != null && CustomerSegmentConstant.JOB_STATUS_PAUSED.equalsIgnoreCase(latest.getStatus())) {
            resumeRecompute(latest.getRunId(), branchId, triggerSource);
            return latest.getRunId();
        }

        String runId = UUID.randomUUID().toString();
        SegmentJobRunRow job = new SegmentJobRunRow();
        job.setRunId(runId);
        job.setBranchId(branchId);
        job.setMode(CustomerSegmentConstant.JOB_MODE_FULL);
        job.setStatus(CustomerSegmentConstant.JOB_STATUS_RUNNING);
        job.setTriggerSource(triggerSource == null ? "MANUAL" : triggerSource);
        job.setStartedTime(LocalDateTime.now());
        job.setMessage("Job queued in background");
        jobRunRepository.insert(job);

        SegmentRecomputeProgress progress = createProgress(runId, branchId, 0, 0, 0, 0, true, 0, "Job queued in background");
        progressMap.put(runId, progress);
        CompletableFuture.runAsync(() -> recomputeAllSync(runId, branchId, 0), dbExecutor);
        return runId;
    }

    public String resumeRecompute(String runId, Integer branchId, String triggerSource) {
        SegmentRecomputeProgress current = getRecomputeProgress(runId);
        if (current == null) current = mapJobRunToProgress(jobRunRepository.getByRunId(runId));
        if (current == null) throw new IllegalArgumentException("Unknown recompute runId: " + runId);
        if (!CustomerSegmentConstant.JOB_STATUS_PAUSED.equalsIgnoreCase(current.getStatus())) return runId;

        int resumeFrom = current.getNextChunkIndex() == null ? 0 : current.getNextChunkIndex();
        current.setStatus(CustomerSegmentConstant.JOB_STATUS_RUNNING);
        current.setResumable(true);
        current.setMessage("Resumed recompute");
        current.setLastUpdatedAtEpochMs(System.currentTimeMillis());
        progressMap.put(runId, current);
        CompletableFuture.runAsync(() -> recomputeAllSync(runId, branchId, resumeFrom), dbExecutor);
        return runId;
    }

    public void stopRecompute(String runId) {
        SegmentRecomputeProgress progress = getRecomputeProgress(runId);
        if (progress == null) {
            return;
        }
        progress.setStatus(CustomerSegmentConstant.JOB_STATUS_PAUSED);
        progress.setResumable(true);
        progress.setMessage("Paused by user");
        progress.setLastUpdatedAtEpochMs(System.currentTimeMillis());
        jobRunRepository.markPaused(runId, progress.getTotalCustomers(), progress.getProcessedCustomers(), progress.getErrorCount(), progress.getMessage());
    }

    private void recomputeAllSync(String runId, Integer branchId, int startChunkIndex) {
        SegmentRecomputeProgress progress = progressMap.get(runId);
        int totalCustomers = segmentRepository.countCustomersByBranch(branchId);
        int chunkSize = Math.max(100, properties.getRecomputeChunkSize());
        int totalChunks = (int) Math.ceil(totalCustomers * 1.0 / chunkSize);
        int effectiveStartChunk = Math.max(0, startChunkIndex);

        AtomicInteger processedCustomers = new AtomicInteger(progress != null && progress.getProcessedCustomers() != null ? progress.getProcessedCustomers() : 0);
        AtomicInteger processedChunks = new AtomicInteger(progress != null && progress.getProcessedChunks() != null ? progress.getProcessedChunks() : 0);
        AtomicInteger errorCount = new AtomicInteger(progress != null && progress.getErrorCount() != null ? progress.getErrorCount() : 0);
        AtomicInteger lastCheckpointCustomerId = new AtomicInteger(progress != null && progress.getCheckpointCustomerId() != null ? progress.getCheckpointCustomerId() : 0);

        try {
            initializeProgress(progress, totalCustomers, chunkSize, totalChunks, processedChunks.get(), processedCustomers.get(), errorCount.get(), effectiveStartChunk, lastCheckpointCustomerId.get());
            LocalDateTime since = LocalDateTime.now().minusDays(90);
            int currentChunkIndex = effectiveStartChunk;

            while (true) {
                List<Integer> customerIds = segmentRepository.getCustomerIdsByBranchAfterId(branchId, lastCheckpointCustomerId.get(), chunkSize);
                if (customerIds.isEmpty()) {
                    break;
                }

                processChunk(runId, branchId, progress, since, totalCustomers, totalChunks, currentChunkIndex,
                        customerIds, processedCustomers, processedChunks, errorCount, lastCheckpointCustomerId);

                currentChunkIndex++;
                if (progress != null && CustomerSegmentConstant.JOB_STATUS_PAUSED.equalsIgnoreCase(progress.getStatus())) {
                    break;
                }
            }

            if (progress == null || !CustomerSegmentConstant.JOB_STATUS_PAUSED.equalsIgnoreCase(progress.getStatus())) {
                jobRunRepository.markSuccess(runId, totalCustomers, processedCustomers.get(), errorCount.get(), "Completed full recompute");
                evictSummaryCache(branchId);
                if (progress != null) {
                    progress.setStatus(CustomerSegmentConstant.JOB_STATUS_SUCCESS);
                    progress.setResumable(false);
                    progress.setNextChunkIndex(totalChunks);
                    progress.setProcessedChunks(totalChunks);
                    progress.setProcessedCustomers(processedCustomers.get());
                    progress.setCheckpointCustomerId(lastCheckpointCustomerId.get());
                    progress.setMessage("Completed full recompute");
                    progress.setLastUpdatedAtEpochMs(System.currentTimeMillis());
                }
            }
        } catch (Exception ex) {
            errorCount.incrementAndGet();
            log.error("Segment recompute failed runId={}", runId, ex);
            if (progress != null) {
                progress.setStatus(CustomerSegmentConstant.JOB_STATUS_PAUSED);
                progress.setResumable(true);
                progress.setMessage("Paused at checkpoint customerId " + progress.getCheckpointCustomerId() + ": " + ex.getMessage());
                progress.setErrorCount(errorCount.get());
                progress.setLastUpdatedAtEpochMs(System.currentTimeMillis());
                jobRunRepository.markPaused(runId, totalCustomers, processedCustomers.get(), errorCount.get(), progress.getMessage());
            } else {
                jobRunRepository.markFailed(runId, totalCustomers, processedCustomers.get(), errorCount.get(), ex.getMessage());
            }
        }
    }

    private void initializeProgress(SegmentRecomputeProgress progress, int totalCustomers, int chunkSize, int totalChunks,
                                    int processedChunks, int processedCustomers, int errorCount, int nextChunkIndex, int checkpointCustomerId) {
        if (progress == null) return;
        progress.setTotalCustomers(totalCustomers);
        progress.setChunkSize(chunkSize);
        progress.setTotalChunks(totalChunks);
        progress.setProcessedChunks(processedChunks);
        progress.setProcessedCustomers(processedCustomers);
        progress.setErrorCount(errorCount);
        progress.setNextChunkIndex(nextChunkIndex);
        progress.setResumeFromChunkIndex(nextChunkIndex);
        progress.setCheckpointCustomerId(checkpointCustomerId);
        progress.setResumable(true);
        progress.setMessage(nextChunkIndex > 0 ? "Resuming from checkpoint chunk " + nextChunkIndex : "Start processing chunks");
        progress.setLastUpdatedAtEpochMs(System.currentTimeMillis());
    }

    private void processChunk(String runId, Integer branchId, SegmentRecomputeProgress progress, LocalDateTime since,
                              int totalCustomers, int totalChunks, int chunkIndex, List<Integer> customerIds,
                              AtomicInteger processedCustomers, AtomicInteger processedChunks, AtomicInteger errorCount,
                              AtomicInteger lastCheckpointCustomerId) {
        int checkpointId = customerIds.get(customerIds.size() - 1);
        try {
            List<CustomerInvoiceAggregate> aggregates = segmentRepository.aggregateInvoice90dByCustomerIds(branchId, since, customerIds);
            Map<Integer, CustomerInvoiceAggregate> aggregateMap = aggregates.stream().collect(Collectors.toMap(CustomerInvoiceAggregate::getCustomerId, a -> a, (a, b) -> a));
            List<CustomerSegmentSnapshotRow> previous = segmentRepository.getSnapshotByCustomerIds(branchId, customerIds);
            Map<Integer, CustomerSegmentSnapshotRow> previousMap = previous.stream().collect(Collectors.toMap(CustomerSegmentSnapshotRow::getCustomerId, p -> p, (a, b) -> a));

            List<CustomerSegmentSnapshotRow> snapshots = new ArrayList<>(customerIds.size());
            List<CustomerSegmentHistoryRow> histories = new ArrayList<>();
            int defaultRecencyDays = properties.getChurnRecencyDays() + 1;
            LocalDate currentDate = LocalDate.now();

            for (Integer customerId : customerIds) {
                CustomerInvoiceAggregate aggregate = aggregateMap.get(customerId);
                if (aggregate == null) {
                    aggregate = new CustomerInvoiceAggregate();
                    aggregate.setCustomerId(customerId);
                    aggregate.setBranchId(branchId);
                    aggregate.setFrequency90d(0);
                    aggregate.setMonetary90d(0.0);
                    aggregate.setRecencyDays(defaultRecencyDays);
                }
                CustomerSegmentSnapshotRow snapshot = buildSnapshot(branchId, aggregate, currentDate, defaultRecencyDays);
                snapshots.add(snapshot);
                CustomerSegmentSnapshotRow before = previousMap.get(customerId);
                if (before == null || !before.getSegmentCode().equals(snapshot.getSegmentCode())) {
                    histories.add(buildHistory(runId, snapshot, before));
                }
            }

            segmentRepository.upsertSnapshots(snapshots);
            if (!histories.isEmpty()) historyRepository.insertBatchHistory(histories);

            processedCustomers.addAndGet(customerIds.size());
            processedChunks.incrementAndGet();
            lastCheckpointCustomerId.set(checkpointId);

            if (progress != null) {
                progress.setProcessedChunks(processedChunks.get());
                progress.setProcessedCustomers(processedCustomers.get());
                progress.setErrorCount(errorCount.get());
                progress.setNextChunkIndex(chunkIndex + 1);
                progress.setCheckpointCustomerId(checkpointId);
                progress.setMessage("Processed chunk " + (chunkIndex + 1) + "/" + totalChunks);
                progress.setLastUpdatedAtEpochMs(System.currentTimeMillis());
            }

            if (CHECKPOINT_UPDATE_EVERY_CHUNK == 1) {
                jobRunRepository.markRunning(runId, totalCustomers, processedCustomers.get(), errorCount.get(), checkpointId, chunkIndex + 1, progress != null ? progress.getMessage() : null);
            }
        } catch (Exception ex) {
            errorCount.incrementAndGet();
            throw ex;
        }
    }

    public Page<CustomerSegmentSnapshotRow> getSnapshotPage(Integer branchId, String segmentCode, Pageable pageable) {
        List<CustomerSegmentSnapshotRow> list = segmentRepository.getSnapshotByCriteria(branchId, segmentCode, pageable);
        Long total = segmentRepository.countSnapshotByCriteria(branchId, segmentCode);
        pageable.setTotal(total);
        return new Page<>(pageable, list);
    }

    public CustomerSegmentSnapshotRow getSnapshot(Integer branchId, Integer customerId) {
        List<CustomerSegmentSnapshotRow> rows = segmentRepository.getSnapshotByCustomerIds(branchId, List.of(customerId));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<SegmentSummaryItem> getSummary(Integer branchId) {
        String key = CacheKeyFactory.segmentSummary(branchId);
        return cacheTemplate.getOrLoad(key, SUMMARY_CACHE_TTL, () -> segmentRepository.getSummary(branchId));
    }

    public List<SegmentTrendItem> getTrend(Integer branchId, Integer monthsBack) {
        return historyRepository.getTrendByMonth(branchId, monthsBack);
    }

    public SegmentRecomputeProgress getRecomputeProgress(String runId) {
        SegmentRecomputeProgress progress = progressMap.get(runId);
        if (progress != null) return progress;
        return mapJobRunToProgress(jobRunRepository.getByRunId(runId));
    }

    public SegmentRecomputeProgress getLatestRecomputeProgress(Integer branchId) {
        SegmentJobRunRow row = jobRunRepository.getLatestByBranch(branchId);
        if (row == null) return null;
        SegmentRecomputeProgress inMemory = progressMap.get(row.getRunId());
        return inMemory != null ? inMemory : mapJobRunToProgress(row);
    }

    private SegmentRecomputeProgress mapJobRunToProgress(SegmentJobRunRow row) {
        if (row == null) return null;
        SegmentRecomputeProgress dto = new SegmentRecomputeProgress();
        dto.setRunId(row.getRunId());
        dto.setBranchId(row.getBranchId());
        dto.setStatus(row.getStatus());
        dto.setTotalCustomers(row.getTotalCustomers());
        dto.setProcessedCustomers(row.getProcessedCustomers());
        dto.setCheckpointCustomerId(row.getCheckpointCustomerId());
        dto.setErrorCount(row.getErrorCount());
        dto.setMessage(row.getMessage());
        dto.setResumable(CustomerSegmentConstant.JOB_STATUS_PAUSED.equalsIgnoreCase(row.getStatus()));
        dto.setNextChunkIndex(row.getCheckpointChunkIndex());
        dto.setResumeFromChunkIndex(row.getCheckpointChunkIndex());
        if (row.getStartedTime() != null) {
            dto.setStartedAtEpochMs(row.getStartedTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (row.getFinishedTime() != null) {
            dto.setLastUpdatedAtEpochMs(row.getFinishedTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        } else {
            dto.setLastUpdatedAtEpochMs(dto.getStartedAtEpochMs());
        }
        return dto;
    }

    private CustomerSegmentSnapshotRow buildSnapshot(Integer branchId, CustomerInvoiceAggregate aggregate, LocalDate currentDate, int defaultRecencyDays) {
        CustomerSegmentSnapshotRow row = new CustomerSegmentSnapshotRow();
        row.setBranchId(branchId);
        row.setCustomerId(aggregate.getCustomerId());
        int recencyDays = defaultRecencyDays;
        if (aggregate.getLastReceiptTime() != null) {
            recencyDays = (int) java.time.temporal.ChronoUnit.DAYS.between(aggregate.getLastReceiptTime().toLocalDate(), currentDate);
        }
        row.setRecencyDays(recencyDays);
        row.setFrequency90d(aggregate.getFrequency90d() == null ? 0 : aggregate.getFrequency90d());
        row.setMonetary90d(aggregate.getMonetary90d() == null ? 0.0 : aggregate.getMonetary90d());
        double totalScore = calcRecencyScore(row.getRecencyDays()) * properties.getRecencyWeight()
                + calcFrequencyScore(row.getFrequency90d()) * properties.getFrequencyWeight()
                + calcMonetaryScore(row.getMonetary90d()) * properties.getMonetaryWeight();
        row.setTotalScore(totalScore);
        row.setSegmentCode(resolveSegmentCode(row.getRecencyDays(), totalScore));
        row.setRuleVersion(CustomerSegmentConstant.RULE_VERSION_RFM_V1);
        row.setActive(1);
        row.setUpdatedTime(LocalDateTime.now());
        row.setCreatedTime(LocalDateTime.now());
        return row;
    }

    private CustomerSegmentHistoryRow buildHistory(String runId, CustomerSegmentSnapshotRow current, CustomerSegmentSnapshotRow previous) {
        CustomerSegmentHistoryRow history = new CustomerSegmentHistoryRow();
        history.setRunId(runId);
        history.setBranchId(current.getBranchId());
        history.setCustomerId(current.getCustomerId());
        history.setFromSegment(previous == null ? null : previous.getSegmentCode());
        history.setToSegment(current.getSegmentCode());
        history.setRecencyDays(current.getRecencyDays());
        history.setFrequency90d(current.getFrequency90d());
        history.setMonetary90d(current.getMonetary90d());
        history.setTotalScore(current.getTotalScore());
        history.setRuleVersion(CustomerSegmentConstant.RULE_VERSION_RFM_V1);
        history.setCreatedTime(LocalDateTime.now());
        return history;
    }

    private double calcRecencyScore(Integer recencyDays) {
        if (recencyDays == null) return 1;
        if (recencyDays <= 7) return 5;
        if (recencyDays <= 30) return 4;
        if (recencyDays <= 60) return 3;
        if (recencyDays <= 120) return 2;
        return 1;
    }

    private double calcFrequencyScore(Integer frequency90d) {
        if (frequency90d == null) return 1;
        if (frequency90d >= 10) return 5;
        if (frequency90d >= 6) return 4;
        if (frequency90d >= 3) return 3;
        if (frequency90d >= 1) return 2;
        return 1;
    }

    private double calcMonetaryScore(Double monetary90d) {
        double total = monetary90d == null ? 0.0 : monetary90d;
        if (total >= properties.getMonetaryTier5()) return 5;
        if (total >= properties.getMonetaryTier4()) return 4;
        if (total >= properties.getMonetaryTier3()) return 3;
        if (total >= properties.getMonetaryTier2()) return 2;
        return 1;
    }

    private String resolveSegmentCode(Integer recencyDays, double totalScore) {
        if (recencyDays != null && recencyDays > properties.getChurnRecencyDays()) return CustomerSegmentConstant.SEGMENT_CHURNED;
        if (totalScore >= properties.getVipScoreThreshold()) return CustomerSegmentConstant.SEGMENT_VIP;
        if (totalScore >= properties.getLoyalScoreThreshold()) return CustomerSegmentConstant.SEGMENT_LOYAL;
        if (totalScore >= properties.getPotentialScoreThreshold()) return CustomerSegmentConstant.SEGMENT_POTENTIAL;
        return CustomerSegmentConstant.SEGMENT_AT_RISK;
    }

    private void evictSummaryCache(Integer branchId) {
        cacheTemplate.evict(CacheKeyFactory.segmentSummary(branchId));
    }

    private SegmentRecomputeProgress createProgress(String runId, Integer branchId, int totalCustomers, int chunkSize,
                                                    int totalChunks, int processedCustomers, boolean resumable,
                                                    int nextChunkIndex, String message) {
        SegmentRecomputeProgress progress = new SegmentRecomputeProgress();
        progress.setRunId(runId);
        progress.setBranchId(branchId);
        progress.setStatus(CustomerSegmentConstant.JOB_STATUS_RUNNING);
        progress.setTotalCustomers(totalCustomers);
        progress.setChunkSize(chunkSize);
        progress.setTotalChunks(totalChunks);
        progress.setProcessedChunks(0);
        progress.setProcessedCustomers(processedCustomers);
        progress.setErrorCount(0);
        progress.setCheckpointCustomerId(0);
        progress.setResumable(resumable);
        progress.setNextChunkIndex(nextChunkIndex);
        progress.setResumeFromChunkIndex(nextChunkIndex);
        progress.setMessage(message);
        progress.setStartedAtEpochMs(System.currentTimeMillis());
        progress.setLastUpdatedAtEpochMs(System.currentTimeMillis());
        return progress;
    }
}
