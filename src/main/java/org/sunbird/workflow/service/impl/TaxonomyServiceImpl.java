package org.sunbird.workflow.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntityV2;
import org.sunbird.workflow.postgres.repo.WfStatusRepoV2;
import org.sunbird.workflow.service.WfServiceHandler;

import java.util.*;

@Service("taxonomyServiceImpl")
public class TaxonomyServiceImpl implements WfServiceHandler {

    Logger logger = LogManager.getLogger(TaxonomyServiceImpl.class);

    @Autowired
    private WfStatusRepoV2 wfStatusRepoV2;

    @Autowired
    private RequestServiceImpl requestService;

    @Autowired
    private ObjectMapper mapper;

    @Value("${lms.system.host}")
    private String host;

    @Value("${taxonomy.framework.id}")
    private String frameworkId;

    @Value("${taxonomy.channel.id}")
    private String channelId;

    @Value("${taxonomy.workflow.draft.status}")
    private String draft;

    @Value("${taxonomy.workflow.L1.status}")
    private String under_L1_Review;

    @Value("${taxonomy.workflow.L2.status}")
    private String under_L2_Review;

    @Value("${taxonomy.workflow.approved.status}")
    private String approved;

    @Value("${taxonomy.term.update.api}")
    private String TERM_UPDATE_URI;

    @Value("${taxonomy.framework.publish.api}")
    private String PUBLISH_FRAMEWORK_URI;

    @Value("${taxonomy.term.read.api}")
    private String termReadURI;

    @Value("${taxonomy.workflow.live}")
    private String live;

    @Value("${taxonomy.workflow.publish}")
    private String underPublish;

    @Override
    public void processMessage(WfRequest wfRequest) {
        if (Objects.nonNull(wfRequest) && !StringUtils.isEmpty(wfRequest.getWfId())) {
            WfStatusEntityV2 wfStatusEntityV2 = wfStatusRepoV2.findBywfId(wfRequest.getWfId());
            List<HashMap<String, Object>> updateFieldValues = getUpdateFieldValues(wfStatusEntityV2);
            if (Objects.nonNull(updateFieldValues) && !CollectionUtils.isEmpty(updateFieldValues)) {
                for (HashMap<String, Object> updateFieldValue : updateFieldValues) {
                    String identifier = (String) updateFieldValue.get(Constants.IDENTIFIER);
                    String id = (String) updateFieldValue.get(Constants.CODE);
                    String category = (String) updateFieldValue.get(Constants.CATEGORY);
                    changeStatus((String) updateFieldValue.get(Constants.APPROVAL_STATUS),wfStatusEntityV2.getCurrentStatus(),identifier,category,id);
                    List<HashMap<String, Object>> associationsList = (List<HashMap<String, Object>>) updateFieldValue.get(Constants.ASSOCIATIONS);
                     if (!CollectionUtils.isEmpty(associationsList)) {
                            for (Map association : associationsList) {
                                if (!CollectionUtils.isEmpty(association)) {
                                    changeStatus((String) association.get(Constants.APPROVAL_STATUS),wfStatusEntityV2.getCurrentStatus(),(String) association.get(Constants.IDENTIFIER),(String) association.get(Constants.CATEGORY),(String) association.get(Constants.CODE));
                                }
                            }
                     }
                }
                StringBuilder frameworkURI = constructPublishFrameworkURI();
                HashMap<String, String> headers = new HashMap<>();
                headers.put(Constants.XCHANNELID, channelId);
                Map<String, Object> publishApiResponse = (Map<String, Object>) requestService.fetchResultUsingPost(frameworkURI, null, Map.class, headers);
                String responseCode = (String) publishApiResponse.get(Constants.RESPONSE_CODE);
                if (!responseCode.equals(Constants.OK)) {
                    logger.error("Unable To Publish Your Framework");
                }
            }
        }

    }

    private String constructTermUpdateURI(String term, String category) {
        String uri = null;
        StringBuilder builder = null;
        if (!StringUtils.isEmpty(term) && !StringUtils.isEmpty(frameworkId) && !StringUtils.isEmpty(category)) {
            builder = new StringBuilder();
            builder = builder.append(host+TERM_UPDATE_URI.replace(Constants.ID, term))
                    .append(Constants.QUESTION_MARK).append(Constants.FRAMEWORK).append(Constants.EQUAL_TO).append(frameworkId)
                    .append(Constants.AND).append(Constants.CATEGORY).append(Constants.EQUAL_TO).append(category);
        }
        return builder.toString();
    }

    private StringBuilder constructPublishFrameworkURI() {
        StringBuilder builder = null;
        if (!StringUtils.isEmpty(frameworkId)) {
            builder = new StringBuilder();
            builder.append(host+PUBLISH_FRAMEWORK_URI.replace(Constants.ID, frameworkId));
        }
        return builder;
    }

    private List<HashMap<String, Object>> getUpdateFieldValues(WfStatusEntityV2 statusEntity) {
        List<HashMap<String, Object>> updateFieldValuesList = null;
        if (!ObjectUtils.isEmpty(statusEntity)) {
            if (!StringUtils.isEmpty(statusEntity.getUpdateFieldValues())) {
                try {
                    updateFieldValuesList = mapper.readValue(statusEntity.getUpdateFieldValues(), new TypeReference<List<HashMap<String, Object>>>() {
                    });
                } catch (Exception ex) {
                    logger.error("Exception occurred while parsing wf fields!");
                }
            }
        }
        return updateFieldValuesList;
    }
    private void changeStatus(String currentApprovalStatus, String currentStatus, String identifier, String category, String id) {
        if (StringUtils.isNotEmpty(currentStatus) && StringUtils.isNotEmpty(identifier) && StringUtils.isNotEmpty(category) && StringUtils.isNotEmpty(id)) {
        if (!currentApprovalStatus.equals(Constants.Live)) {
            HashMap<String, Object> request = new HashMap<>();
            HashMap<String, Object> term = new HashMap<>();
            HashMap<String, Object> requestMap = new HashMap<>();
            if (currentStatus.equals(Constants.SEND_FOR_REVIEW_LEVEL_1)) {
                requestMap.put(Constants.APPROVAL_STATUS, under_L1_Review);
            } else if (currentStatus.equals(Constants.SEND_FOR_REVIEW_LEVEL_2)) {
                requestMap.put(Constants.APPROVAL_STATUS, under_L2_Review);
            } else if (currentStatus.equals(Constants.SEND_FOR_PUBLISH)) {
                requestMap.put(Constants.APPROVAL_STATUS, underPublish);
            } else if (currentStatus.equals(Constants.APPROVED)) {
                requestMap.put(Constants.APPROVAL_STATUS, live);
            }
            requestMap.put(Constants.IDENTIFIER, identifier);
            term.put(Constants.TERM, requestMap);
            request.put(Constants.REQUEST, term);
            String URI = constructTermUpdateURI(id, category);
            logger.info("printing URI For Term Update {} ", URI);
            Map<String, Object> response = requestService.fetchResultUsingPatch(URI, request, null);
            String responseCode = (String) response.get(Constants.RESPONSE_CODE);
            if (!responseCode.equals(Constants.OK)) {
                logger.error("Unable To Update Term Status");
            }
        }
    }
    }

}
