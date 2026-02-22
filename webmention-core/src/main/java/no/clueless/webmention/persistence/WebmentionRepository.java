package no.clueless.webmention.persistence;

import java.util.List;

public interface WebmentionRepository extends Repository<Webmention, Integer> {
    Webmention getWebmentionBySourceUrl(String sourceUrl);

    void updateApproval(Webmention webmention, boolean isApproved);

    void deleteWebmention(int id);

    List<Webmention> getWebmentionsByIsApproved(int pageNumber, int pageSize, String orderByColumn, String orderDirection, Boolean isApproved);
}
