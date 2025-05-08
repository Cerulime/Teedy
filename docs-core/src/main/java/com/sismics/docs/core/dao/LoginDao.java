package com.sismics.docs.core.dao;

import com.sismics.docs.core.model.jpa.LoginRequest;
import com.sismics.util.context.ThreadLocalContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * DAO for {@link LoginRequest} operations.
 */
public class LoginDao {

    /**
     * Creates a new login request.
     * 
     * @param request the login request to create
     * @return the generated ID of the request
     * @throws IllegalArgumentException if request is null
     */
    public String create(LoginRequest request) {
        Objects.requireNonNull(request, "LoginRequest cannot be null");
        
        EntityManager em = getEntityManager();
        em.persist(request);
        return request.getId();
    }

    /**
     * Retrieves all login requests ordered by timestamp (newest first).
     * 
     * @return immutable list of login requests, never null
     */
    public List<LoginRequest> findAll() {
        EntityManager em = getEntityManager();
        TypedQuery<LoginRequest> query = em.createQuery(
            "SELECT lr FROM LoginRequest lr ORDER BY lr.timestamp DESC", 
            LoginRequest.class);
        return List.copyOf(query.getResultList());
    }

    /**
     * Updates the status of a login request.
     * 
     * @param id the ID of the request to update
     * @param status the new status
     * @throws IllegalArgumentException if id or status is null
     * @throws jakarta.persistence.EntityNotFoundException if request not found
     */
    public void updateStatus(String id, String status) {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        
        EntityManager em = getEntityManager();
        LoginRequest request = em.find(LoginRequest.class, id);
        request.setStatus(status);
    }

    /**
     * Finds a login request by its token.
     * 
     * @param token the token to search for
     * @return an Optional containing the request if found, empty otherwise
     * @throws IllegalArgumentException if token is null
     * @throws IllegalStateException if multiple requests found with same token
     */
    public Optional<LoginRequest> findByToken(String token) {
        Objects.requireNonNull(token, "Token cannot be null");
        
        EntityManager em = getEntityManager();
        TypedQuery<LoginRequest> query = em.createQuery(
            "SELECT lr FROM LoginRequest lr WHERE lr.token = :token", 
            LoginRequest.class);
        query.setParameter("token", token);
        
        try {
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (NonUniqueResultException e) {
            throw new IllegalStateException("Multiple login requests found with the same token", e);
        }
    }

    /**
     * Finds a login request by its ID.
     * 
     * @param id the ID to search for
     * @return an Optional containing the request if found, empty otherwise
     * @throws IllegalArgumentException if id is null
     */
    public Optional<LoginRequest> findById(String id) {
        Objects.requireNonNull(id, "ID cannot be null");
        
        EntityManager em = getEntityManager();
        return Optional.ofNullable(em.find(LoginRequest.class, id));
    }

    /**
     * Helper method to get the EntityManager from the context.
     * 
     * @return the EntityManager
     * @throws IllegalStateException if EntityManager is not available
     */
    private EntityManager getEntityManager() {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        if (em == null) {
            throw new IllegalStateException("EntityManager not available in current context");
        }
        return em;
    }
}
