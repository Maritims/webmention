package no.clueless.webmention.persistence;

import java.util.List;

public interface WebmentionRepository<IdType> {
    WebmentionRepository<IdType> initialize();

    Webmention getWebmentionById(IdType id);

    Webmention getWebmentionBySourceUrl(String sourceUrl);

    long getApprovedCount();

    default String getOrderByColumn() {
        return "id";
    }

    default String getOrderByDirection() {
        return "desc";
    }

    List<Webmention> getApprovedWebmentions(int pageNumber, int pageSize, String orderByColumn, String orderByDirection);

    default List<Webmention> getApprovedWebmentions(int pageNumber, int pageSize) {
        return getApprovedWebmentions(pageNumber, pageSize, getOrderByColumn(), getOrderByDirection());
    }

    Webmention createWebmention(Webmention webmention);

    Webmention updateWebmention(Webmention webmention);

    Webmention upsertWebmention(Webmention webmention);
}
