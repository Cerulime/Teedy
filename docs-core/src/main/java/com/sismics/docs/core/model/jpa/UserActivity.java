package com.sismics.docs.core.model.jpa;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * User activity entity.
 * 
 * @author Deepseek-0325
 */
@Entity
@Table(name = "T_USER_ACTIVITY")
public class UserActivity {
    @Id
    @Column(name = "UTA_ID_C", length = 36)
    private String id;
    
    @Column(name = "UTA_IDUSER_C", length = 36, nullable = false)
    private String userId;
    
    @Column(name = "UTA_ACTIVITY_TYPE_C", length = 50, nullable = false)
    private String activityType;
    
    @Column(name = "UTA_ENTITY_ID_C", length = 36)
    private String entityId;
    
    @Column(name = "UTA_PROGRESS_N", nullable = false)
    private Integer progress;
    
    @Column(name = "UTA_PLANNED_DATE_D")
    private Date plannedDate;
    
    @Column(name = "UTA_COMPLETED_DATE_D")
    private Date completedDate;
    
    @Column(name = "UTA_CREATEDATE_D", nullable = false)
    private Date createDate;
    
    @Column(name = "UTA_DELETEDATE_D")
    private Date deleteDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Date getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(Date plannedDate) {
        this.plannedDate = plannedDate;
    }

    public Date getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Date completedDate) {
        this.completedDate = completedDate;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getDeleteDate() {
        return deleteDate;
    }

    public void setDeleteDate(Date deleteDate) {
        this.deleteDate = deleteDate;
    }
} 