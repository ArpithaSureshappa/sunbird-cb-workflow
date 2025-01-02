package org.sunbird.workflow.sunbird.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.sunbird.workflow.sunbird.entity.WfStatusV2Entity;

import java.util.List;

@Repository
public interface WfStatusV2Repo extends JpaRepository<WfStatusV2Entity, String> {
    List<WfStatusV2Entity> findByServiceName(String serviceName);
}
