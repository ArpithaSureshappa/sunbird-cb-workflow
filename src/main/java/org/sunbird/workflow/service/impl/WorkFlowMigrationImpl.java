package org.sunbird.workflow.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.sunbird.entity.WfStatusV2Entity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.sunbird.repo.WfStatusV2Repo;
import org.sunbird.workflow.service.WorkflowMigration;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@Slf4j
public class WorkFlowMigrationImpl implements WorkflowMigration {

    @Autowired
    private WfStatusV2Repo wfStatusV2Repo;

    @Autowired
    private WfStatusRepo wfStatusRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Response migrateWfStatusToNewFormat(String serviceName) {
        Response response = new Response();
        try {
            if (serviceName == null || serviceName.isEmpty()) {
                log.error("Service name is null or empty.");
                response.put(Constants.MESSAGE, "Service name is null or empty");
                response.put(Constants.STATUS, HttpStatus.BAD_REQUEST);
                return response;
            }
            int pageSize = 1000;
            int pageNumber = 0;
            Page<WfStatusEntity> page;
            do {
                page = wfStatusRepo.findByServiceName(serviceName, PageRequest.of(pageNumber, pageSize));
                List<WfStatusV2Entity> wfStatusV2EntityList = new ArrayList<>();
                List<WfStatusEntity> wfStatusEntities = page.getContent();
                if (wfStatusEntities.isEmpty()) {
                    log.info("No data found for serviceName: {}", serviceName);
                    break;
                }
                for (WfStatusEntity wfStatusEntity : wfStatusEntities) {
                    try {
                        if (wfStatusEntity.getUpdateFieldValues() == null) {
                            log.warn("No update field values for WfStatusEntity with ID: {}", wfStatusEntity.getWfId());
                            continue;
                        }
                        List<Map<String, Object>> updatedFieldValues = objectMapper.readValue(wfStatusEntity.getUpdateFieldValues(), new TypeReference<List<Map<String, Object>>>() {});
                        if (!updatedFieldValues.isEmpty()) {
                            Map<String, Object> fieldValue = updatedFieldValues.get(0);
                            Map<String, Object> fromValueMap = (Map<String, Object>) fieldValue.get("fromValue");
                            Map<String, Object> toValueMap = (Map<String, Object>) fieldValue.get("toValue");
                            if (fromValueMap == null || toValueMap == null) {
                                log.warn("Missing 'fromValue' or 'toValue' for WfStatusEntity with ID: {}", wfStatusEntity.getWfId());
                                continue;
                            }
                            String fromValue = objectMapper.writeValueAsString(fromValueMap);
                            String toValue = objectMapper.writeValueAsString(toValueMap);
                            WfStatusV2Entity wfStatusV2Entity = mapToWfStatusV2Entity(wfStatusEntity, fromValue, toValue);
                            wfStatusV2EntityList.add(wfStatusV2Entity);
                        }else {
                            log.warn("Empty updatedFieldValues for WfStatusEntity with ID: {}", wfStatusEntity.getWfId());
                        }
                    } catch (Exception e) {
                        log.error("Error processing WfStatusEntity with ID: {}", wfStatusEntity.getWfId(), e);
                    }
                }
                if (!wfStatusV2EntityList.isEmpty()) {
                    bulkSave(wfStatusV2EntityList);
                    log.info("Successfully saved {} records for serviceName: {}", wfStatusV2EntityList.size(), serviceName);
                }
                pageNumber++;
            } while (page.hasNext());

            response.put(Constants.MESSAGE, Constants.DATA_MIGRATED_SUCCESSFULLY);
            response.put(Constants.STATUS, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error during migration for serviceName: {}", serviceName, e);
            response.put(Constants.MESSAGE, e.getMessage());
            response.put(Constants.STATUS, Constants.FAILED);
        }
        return response;
    }

    private WfStatusV2Entity mapToWfStatusV2Entity(WfStatusEntity wfStatusEntity, String fromValue, String toValue) {
        WfStatusV2Entity wfStatusV2Entity = new WfStatusV2Entity();
        wfStatusV2Entity.setWfId(wfStatusEntity.getWfId());
        wfStatusV2Entity.setUserId(wfStatusEntity.getUserId());
        wfStatusV2Entity.setRequestType(wfStatusEntity.getRequestType());
        wfStatusV2Entity.setDeptName(wfStatusEntity.getDeptName());
        wfStatusV2Entity.setServiceName(wfStatusEntity.getServiceName());
        wfStatusV2Entity.setCurrentStatus(wfStatusEntity.getCurrentStatus());
        wfStatusV2Entity.setFromValue(fromValue);
        wfStatusV2Entity.setToValue(toValue);
        wfStatusV2Entity.setCreatedAt(wfStatusEntity.getCreatedOn());
        wfStatusV2Entity.setCreatedBy(wfStatusEntity.getUserId());
        wfStatusV2Entity.setUpdatedAt(wfStatusEntity.getLastUpdatedOn());
        wfStatusV2Entity.setUpdatedBy(wfStatusEntity.getUserId());
        return wfStatusV2Entity;
    }

    @Transactional
    private void bulkSave(List<WfStatusV2Entity> wfStatusV2EntityList) {
        if (wfStatusV2EntityList == null || wfStatusV2EntityList.isEmpty()) {
            log.warn("Attempting to save an empty list of WfStatusV2Entities.");
            return;
        }
        wfStatusV2Repo.saveAll(wfStatusV2EntityList);
    }
}
