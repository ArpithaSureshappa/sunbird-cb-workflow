package org.sunbird.workflow.sunbird.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "wf_status_v2", schema = "public")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class WfStatusV2Entity {
    @Id
    @Column(name = "wf_id", length = 64)
    private String wfId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "service_name", length = 128)
    private String serviceName;

    @Column(name = "request_type", length = 128)
    private String requestType;

    @Column(name = "from_value", length = 256)
    private String fromValue;

    @Column(name = "to_value", length = 256)
    private String toValue;

    @Column(name = "dept_id", length = 64)
    private String deptId;

    @Column(name = "dept_name", length = 256)
    private String deptName;

    @Column(name = "current_status", length = 128)
    private String currentStatus;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;
}

