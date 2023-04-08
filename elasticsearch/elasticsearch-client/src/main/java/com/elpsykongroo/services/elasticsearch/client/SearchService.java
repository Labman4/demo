package com.elpsykongroo.services.elasticsearch.client;

import com.elpsykongroo.services.elasticsearch.client.domain.AccessRecord;
import com.elpsykongroo.services.elasticsearch.client.domain.IPManage;

import java.util.List;

public interface SearchService {
    List<IPManage> findByIsBlackTrue();

    List<IPManage>  findByIsBlackFalse();

    List<IPManage> findByIsBlackTrue(String pageNumber, String pageSize);

    List<IPManage>  findByIsBlackFalse(String pageNumber, String pageSize);

    Long countByAddressAndIsBlackTrue(String address);

    Long countByAddressAndIsBlackFalse(String address);

    void deleteByAddressAndIsBlackTrue(String address);

    void deleteByAddressAndIsBlackFalse(String address);

    String findAllIP(String pageNumber, String pageSize);

    String findAllRecord(String pageNumber, String pageSize, String order);

    void saveRecord(AccessRecord AccessRecord);

    String saveIP(IPManage IPManage);

    List<AccessRecord> findByAccessPathLike(String path);

    List<AccessRecord> findBySourceIP(String sourceip);

    List<AccessRecord> findByUserAgentLike(String agent);

    List<AccessRecord> findByRequestHeaderLike(String header);

    void deleteAllRecordById(List<String> ids);

    void deleteIPById(String id);

}
