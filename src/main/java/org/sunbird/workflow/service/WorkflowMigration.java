package org.sunbird.workflow.service;

import org.springframework.stereotype.Service;
import org.sunbird.workflow.models.Response;

@Service
public interface WorkflowMigration {

    Response migrateWfStatusToNewFormat(String serviceName);
}
