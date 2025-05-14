package com.sismics.docs.core.dao;

import com.sismics.docs.core.dao.criteria.UserActivityCriteria;
import com.sismics.docs.core.dao.dto.UserActivityDto;
import com.sismics.docs.core.model.jpa.UserActivity;
import com.sismics.docs.core.util.jpa.PaginatedList;
import com.sismics.docs.core.util.jpa.PaginatedLists;
import com.sismics.docs.core.util.jpa.QueryParam;
import com.sismics.docs.core.util.jpa.SortCriteria;
import com.sismics.util.context.ThreadLocalContext;

import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User activity DAO.
 * 
 * @author Deepseek-0325
 */
public class UserActivityDao {

    /**
     * Creates a new user activity.
     * 
     * @param userActivity User activity to create
     * @return New ID
     */
    public String create(UserActivity userActivity) {
        Objects.requireNonNull(userActivity, "User activity cannot be null");
        
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        userActivity.setId(UUID.randomUUID().toString());
        userActivity.setCreateDate(Date.from(Instant.now()));
        em.persist(userActivity);
        
        return userActivity.getId();
    }
    
    /**
     * Updates a user activity.
     * 
     * @param userActivity User activity to update
     * @return Updated user activity or empty if not found
     */
    public Optional<UserActivity> update(UserActivity userActivity) {
        Objects.requireNonNull(userActivity, "User activity cannot be null");
        
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        return Optional.ofNullable(em.find(UserActivity.class, userActivity.getId()))
            .map(userActivityDb -> {
                userActivityDb.setProgress(userActivity.getProgress());
                Optional.ofNullable(userActivity.getPlannedDate()).ifPresent(userActivityDb::setPlannedDate);
                Optional.ofNullable(userActivity.getCompletedDate()).ifPresent(userActivityDb::setCompletedDate);
                return userActivityDb;
            });
    }
    
    /**
     * Gets a user activity by ID.
     * 
     * @param id User activity ID
     * @return User activity or empty if not found
     */
    public Optional<UserActivity> getById(String id) {
        Objects.requireNonNull(id, "ID cannot be null");
        
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        try {
            return Optional.ofNullable(em.find(UserActivity.class, id));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Soft deletes a user activity.
     * 
     * @param id User activity ID
     * @return true if the activity was found and deleted, false otherwise
     */
    public boolean delete(String id) {
        Objects.requireNonNull(id, "ID cannot be null");
        
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        return Optional.ofNullable(em.find(UserActivity.class, id))
            .map(userActivity -> {
                userActivity.setDeleteDate(Date.from(Instant.now()));
                return true;
            })
            .orElse(false);
    }
    
    /**
     * Searches user activities by criteria.
     * 
     * @param paginatedList List of user activities (updated by side effects)
     * @param criteria Search criteria
     * @param sortCriteria Sort criteria
     */
    public void findByCriteria(PaginatedList<UserActivityDto> paginatedList, 
                             UserActivityCriteria criteria, 
                             SortCriteria sortCriteria) {
        Objects.requireNonNull(paginatedList, "PaginatedList cannot be null");
        Objects.requireNonNull(criteria, "Criteria cannot be null");
        
        Map<String, Object> parameterMap = new HashMap<>();
        
        String query = """
            select ua.UTA_ID_C, ua.UTA_IDUSER_C, u.USE_USERNAME_C, ua.UTA_ACTIVITY_TYPE_C, 
                   ua.UTA_ENTITY_ID_C, d.DOC_TITLE_C, ua.UTA_PROGRESS_N, 
                   ua.UTA_PLANNED_DATE_D, ua.UTA_COMPLETED_DATE_D, ua.UTA_CREATEDATE_D 
            from T_USER_ACTIVITY ua 
            join T_USER u on ua.UTA_IDUSER_C = u.USE_ID_C 
            left join T_DOCUMENT d on ua.UTA_ENTITY_ID_C = d.DOC_ID_C 
            """;
        
        // Build WHERE clause
        List<String> criteriaList = new ArrayList<>();
        criteriaList.add("ua.UTA_DELETEDATE_D is null");
        
        Optional.ofNullable(criteria.getUserId())
            .ifPresent(userId -> {
                criteriaList.add("ua.UTA_IDUSER_C = :userId");
                parameterMap.put("userId", userId);
            });
            
        Optional.ofNullable(criteria.getActivityType())
            .ifPresent(activityType -> {
                criteriaList.add("ua.UTA_ACTIVITY_TYPE_C = :activityType");
                parameterMap.put("activityType", activityType);
            });
            
        Optional.ofNullable(criteria.getEntityId())
            .ifPresent(entityId -> {
                criteriaList.add("ua.UTA_ENTITY_ID_C = :entityId");
                parameterMap.put("entityId", entityId);
            });
        
        if (!criteriaList.isEmpty()) {
            query += " where " + String.join(" and ", criteriaList);
        }
        
        // Perform the search
        QueryParam queryParam = new QueryParam(query, parameterMap);
        List<Object[]> results = PaginatedLists.executePaginatedQuery(paginatedList, queryParam, sortCriteria);
        
        // Transform results using Stream API
        List<UserActivityDto> userActivityDtoList = results.stream()
            .map(this::mapToUserActivityDto)
            .collect(Collectors.toList());
        
        paginatedList.setResultList(userActivityDtoList);
    }
    
    private UserActivityDto mapToUserActivityDto(Object[] row) {
        int i = 0;
        UserActivityDto dto = new UserActivityDto();
        dto.setId((String) row[i++]);
        dto.setUserId((String) row[i++]);
        dto.setUsername((String) row[i++]);
        dto.setActivityType((String) row[i++]);
        dto.setEntityId((String) row[i++]);
        dto.setEntityName((String) row[i++]);
        dto.setProgress((Integer) row[i++]);
        
        Optional.ofNullable((Timestamp) row[i++])
            .ifPresent(timestamp -> dto.setPlannedDateTimestamp(timestamp.getTime()));
            
        Optional.ofNullable((Timestamp) row[i++])
            .ifPresent(timestamp -> dto.setCompletedDateTimestamp(timestamp.getTime()));
            
        Optional.ofNullable((Timestamp) row[i++])
            .ifPresent(timestamp -> dto.setCreateTimestamp(timestamp.getTime()));
            
        return dto;
    }
}
