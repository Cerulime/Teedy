package com.sismics.docs.core.util.jpa;

import java.util.Map.Entry;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import com.sismics.util.context.ThreadLocalContext;

/**
 * Query utilities.
 *
 * @author jtremeaux 
 */
public class QueryUtil {

    /**
     * Creates a native query from the query parameters.
     * 
     * @param queryParam Query parameters
     * @return Native query
     */
    public static Query getNativeQuery(QueryParam queryParam) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Query query = em.createNativeQuery(queryParam.getQueryString());
        for (Entry<String, Object> entry : queryParam.getParameterMap().entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return query;
    }
    
    /**
     * Returns sorted query parameters.
     * 
     * @param queryParam Query parameters
     * @param sortCriteria Sort criteria
     * @return Sorted query parameters
     */
    public static QueryParam getSortedQueryParam(QueryParam queryParam, SortCriteria sortCriteria) {
        if (sortCriteria == null) {
            return queryParam;
        }

        String queryString = queryParam.getQueryString().toLowerCase();
        String orderColumn = determineOrderColumn(queryString, sortCriteria);
        
        String sortedQuery = buildSortedQuery(queryParam.getQueryString(), orderColumn, sortCriteria.isAsc());
        return new QueryParam(sortedQuery, queryParam.getParameterMap());
    }

    private static String determineOrderColumn(String queryString, SortCriteria sortCriteria) {
        if (queryString.contains("from t_user_activity ua")) {
            return getUserActivityOrderColumn(sortCriteria.getColumn());
        } else if (queryString.contains("from t_tag t")) {
            return getTagOrderColumn(sortCriteria.getColumn());
        } else if (queryString.contains("from t_document d")) {
            return getDocumentOrderColumn(sortCriteria.getColumn());
        }
        return "c" + sortCriteria.getColumn(); // Default legacy approach
    }

    private static String getUserActivityOrderColumn(int column) {
        switch (column) {
            case 0: return "ua.UTA_ID_C";
            case 1: return "ua.UTA_IDUSER_C";
            case 2: return "u.USE_USERNAME_C";
            case 3: return "ua.UTA_ACTIVITY_TYPE_C";
            case 4: return "ua.UTA_ENTITY_ID_C";
            case 5: return "d.DOC_TITLE_C";
            case 6: return "ua.UTA_PROGRESS_N";
            case 7: return "ua.UTA_PLANNED_DATE_D";
            case 8: return "ua.UTA_COMPLETED_DATE_D";
            case 9: return "ua.UTA_CREATEDATE_D";
            default: return "ua.UTA_CREATEDATE_D";
        }
    }

    private static String getTagOrderColumn(int column) {
        switch (column) {
            case 0: return "t.TAG_ID_C";
            case 1: return "t.TAG_NAME_C";
            case 2: return "t.TAG_COLOR_C";
            case 3: return "t.TAG_IDPARENT_C";
            case 4: return "u.USE_USERNAME_C";
            default: return "t.TAG_NAME_C";
        }
    }

    private static String getDocumentOrderColumn(int column) {
        switch (column) {
            case 0: return "d.DOC_ID_C";
            case 1: return "d.DOC_TITLE_C";
            case 2: return "d.DOC_DESCRIPTION_C";
            case 3: return "d.DOC_CREATEDATE_D";
            case 4: return "d.DOC_UPDATEDATE_D";
            case 5: return "u.USE_USERNAME_C";
            default: return "d.DOC_CREATEDATE_D";
        }
    }

    private static String buildSortedQuery(String originalQuery, String orderColumn, boolean isAsc) {
        return originalQuery + " order by " + orderColumn + (isAsc ? " asc" : " desc");
    }
}
