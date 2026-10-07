package vn.aeoc.crm.service.other;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.repository.sale.CampaignSnapshotRepository;
import vn.aeoc.crm.service.RestService;

@Service
public class RagService {
    private final RestService restService;
    private final String syncWebhookUrl;
    private final String generateWebhookUrl;
    private final CampaignSnapshotRepository campaignSnapshotRepository;

    public RagService(
            RestService restService,
            @Value("${rag.webhook.sync}") String syncWebhookUrl,
            @Value("${rag.webhook.generate}") String generateWebhookUrl,
            CampaignSnapshotRepository campaignSnapshotRepository
    ) {
        this.restService = restService;
        this.syncWebhookUrl = syncWebhookUrl;
        this.generateWebhookUrl = generateWebhookUrl;
        this.campaignSnapshotRepository = campaignSnapshotRepository;
    }


    public void delete(Integer id, String type) {
        delete(id, type, null);
    }

    public void delete(Integer id, String type, Integer branchId) {
        JSONObject body = new JSONObject();
        body.put("type", type);
        body.put("id", id);
        body.put("action", "DELETE");
        if (branchId != null && branchId > 0) {
            body.put("branchId", branchId);
        }

        restService.callApiFromPartner(
                syncWebhookUrl,
                "POST",
                null,
                null,
                null,
                body
        );
    }

    public void upsert(Integer id, String type) {
        upsert(id, type, null);
    }

    public void upsert(Integer id, String type, Integer branchId) {
        JSONObject body = new JSONObject();
        body.put("type", type);
        body.put("id", id);
        body.put("action", "UPSERT");
        if (branchId != null && branchId > 0) {
            body.put("branchId", branchId);
        }

        restService.callApiFromPartner(
                syncWebhookUrl,
                "POST",
                null,
                null,
                null,
                body
        );
    }

    public void generate(Integer customerId) {
        JSONObject body = new JSONObject();
        body.put("id", customerId);

        if (campaignSnapshotRepository.existLoading(customerId)) {
            throw new AppException(ErrorCode.BAD_REQUEST_AI);
        }

        restService.callApiFromPartner(
                generateWebhookUrl,
                "POST",
                null,
                null,
                null,
                body
        );
    }
}
