package no.clueless.webmention.persistence;

import java.util.List;

public interface WebmentionRepository extends Repository<Webmention, Integer> {
    Webmention getWebmentionBySourceUrl(String sourceUrl);

    long getApprovedCount();

    List<Webmention> getApprovedWebmentions(int pageNumber, int pageSize, String orderByColumn, String orderByDirection);

    default List<Webmention> getApprovedWebmentions(int pageNumber, int pageSize) {
        return getApprovedWebmentions(pageNumber, pageSize, getOrderByColumn(), getOrderByDirection());
    }
}
