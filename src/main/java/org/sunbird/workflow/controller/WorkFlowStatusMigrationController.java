package org.sunbird.workflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.service.WorkflowMigration;

@RestController
@RequestMapping("/migration")
public class WorkFlowStatusMigrationController {

    @Autowired
    private WorkflowMigration workflowMigration;

    @GetMapping("/workflowStatus/{servicename}")
    public ResponseEntity<Response> migrateWfStatusToNewFormat(@PathVariable String servicename) {
        Response response = workflowMigration.migrateWfStatusToNewFormat(servicename);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
