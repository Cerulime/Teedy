package com.sismics.docs.rest.resource;

import com.sismics.docs.core.dao.UserActivityDao;
import com.sismics.docs.core.dao.criteria.UserActivityCriteria;
import com.sismics.docs.core.dao.dto.UserActivityDto;
import com.sismics.docs.core.model.jpa.UserActivity;
import com.sismics.docs.core.util.jpa.PaginatedList;
import com.sismics.docs.core.util.jpa.PaginatedLists;
import com.sismics.docs.core.util.jpa.SortCriteria;
import com.sismics.docs.rest.constant.BaseFunction;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.util.ValidationUtil;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * REST endpoints for user activity management.
 * 
 * @author Deepseek-0325
 */
@Path("/useractivity")
public class UserActivityResource extends BaseResource {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    /**
     * Get all user activities (admin only).
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll(
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset,
            @QueryParam("sort_column") Integer sortColumn,
            @QueryParam("asc") Boolean asc,
            @QueryParam("activity_type") String activityType,
            @QueryParam("user_id") String userId) {
        
        if (!authenticate() || !hasBaseFunction(BaseFunction.ADMIN)) {
            throw new ForbiddenClientException();
        }
        
        UserActivityCriteria criteria = new UserActivityCriteria()
                .setActivityType(activityType)
                .setUserId(userId);
        
        PaginatedList<UserActivityDto> paginatedList = getActivities(limit, offset, sortColumn, asc, criteria);
        
        return buildActivityResponse(paginatedList);
    }
    
    /**
     * Get current user's activities.
     */
    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUserActivities(
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset,
            @QueryParam("sort_column") Integer sortColumn,
            @QueryParam("asc") Boolean asc,
            @QueryParam("activity_type") String activityType,
            @QueryParam("entity_id") String entityId) {
        
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        
        UserActivityCriteria criteria = new UserActivityCriteria()
                .setUserId(principal.getId())
                .setActivityType(activityType)
                .setEntityId(entityId);
        
        PaginatedList<UserActivityDto> paginatedList = getActivities(limit, offset, sortColumn, asc, criteria);
        
        return buildActivityResponse(paginatedList);
    }
    
    /**
     * Create or update a user activity.
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrUpdate(
            @FormParam("id") String id,
            @FormParam("activity_type") String activityType,
            @FormParam("entity_id") String entityId,
            @FormParam("planned_date") String plannedDate,
            @FormParam("progress") Integer progress) {
        
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        
        ValidationUtil.validateRequired(activityType, "activity_type");
        ValidationUtil.validateRequired(progress, "progress");
        
        UserActivityDao userActivityDao = new UserActivityDao();
        
        try {
            if (id != null) {
                updateExistingActivity(userActivityDao, id, progress, plannedDate);
            } else {
                id = createNewActivity(userActivityDao, activityType, entityId, progress, plannedDate);
            }
        } catch (Exception e) {
            throw new ClientException("ValidationError", e.getMessage());
        }
        
        return Response.ok(Json.createObjectBuilder().add("id", id).build()).build();
    }
    
    /**
     * Delete a user activity.
     */
    @DELETE
    @Path("{id: [a-z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") String id) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        
        UserActivityDao userActivityDao = new UserActivityDao();
        Optional<UserActivity> userActivity = Optional.ofNullable(userActivityDao.getById(id))
                .orElseThrow(() -> new ClientException("NotFound", "Activity not found"));
        
        if (!principal.getId().equals(userActivity.get().getUserId()) && !hasBaseFunction(BaseFunction.ADMIN)) {
            throw new ForbiddenClientException();
        }
        
        userActivityDao.delete(id);
        
        return Response.ok(Json.createObjectBuilder().add("status", "ok").build()).build();
    }
    
    // Helper methods
    
    private PaginatedList<UserActivityDto> getActivities(Integer limit, Integer offset, 
            Integer sortColumn, Boolean asc, UserActivityCriteria criteria) {
        
        PaginatedList<UserActivityDto> paginatedList = PaginatedLists.create(limit, offset);
        new UserActivityDao().findByCriteria(paginatedList, criteria, new SortCriteria(sortColumn, asc));
        return paginatedList;
    }
    
    private Response buildActivityResponse(PaginatedList<UserActivityDto> paginatedList) {
        JsonArrayBuilder activities = Json.createArrayBuilder();
        
        paginatedList.getResultList().forEach(dto -> {
            JsonObjectBuilder activity = Json.createObjectBuilder()
                    .add("id", dto.getId())
                    .add("user_id", dto.getUserId())
                    .add("username", dto.getUsername())
                    .add("activity_type", dto.getActivityType())
                    .add("progress", dto.getProgress());
            
            Optional.ofNullable(dto.getEntityId()).ifPresent(v -> activity.add("entity_id", v));
            Optional.ofNullable(dto.getEntityName()).ifPresent(v -> activity.add("entity_name", v));
            Optional.ofNullable(dto.getPlannedDateTimestamp()).ifPresent(v -> activity.add("planned_date_timestamp", v));
            Optional.ofNullable(dto.getCompletedDateTimestamp()).ifPresent(v -> activity.add("completed_date_timestamp", v));
            Optional.ofNullable(dto.getCreateTimestamp()).ifPresent(v -> activity.add("create_timestamp", v));
            
            activities.add(activity);
        });
        
        return Response.ok(Json.createObjectBuilder()
                .add("activities", activities)
                .add("total", paginatedList.getResultCount())
                .build()).build();
    }
    
    private void updateExistingActivity(UserActivityDao dao, String id, int progress, String plannedDate) 
            throws Exception {
        
        Optional<UserActivity> optActivity = Optional.ofNullable(dao.getById(id))
                .orElseThrow(() -> new ClientException("ActivityNotFound", "Activity not found"));
        UserActivity activity = optActivity.get();

        if (!activity.getUserId().equals(principal.getId())) {
            throw new ForbiddenClientException();
        }
        
        activity.setProgress(progress);
        Optional.ofNullable(plannedDate).ifPresent(pd -> {
            try {
                activity.setPlannedDate(DATE_FORMAT.parse(pd));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        if (progress == 100) {
            activity.setCompletedDate(new Date());
        } else {
            activity.setCompletedDate(null);
        }
        
        dao.update(activity);
    }
    
    private String createNewActivity(UserActivityDao dao, String activityType, 
            String entityId, int progress, String plannedDate) throws Exception {
        
        UserActivity activity = new UserActivity();

                activity.setUserId(principal.getId());
                activity.setActivityType(activityType);
                activity.setEntityId(entityId);
                activity.setProgress(progress);
        
        Optional.ofNullable(plannedDate).ifPresent(pd -> {
            try {
                activity.setPlannedDate(DATE_FORMAT.parse(pd));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        if (progress == 100) {
            activity.setCompletedDate(new Date());
        }
        
        return dao.create(activity);
    }
}
