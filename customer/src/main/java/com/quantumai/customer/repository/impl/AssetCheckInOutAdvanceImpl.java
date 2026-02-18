package com.quantumai.customer.repository.impl;

import com.quantumai.customer.entity.AssetCheckInOut;
import com.quantumai.customer.entity.CheckInOutStatus;
import com.quantumai.customer.repository.AssetCheckInOutAdvance;
import com.quantumai.customer.repository.AssetCheckInOutRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;


@Repository
@Slf4j
public class AssetCheckInOutAdvanceImpl implements AssetCheckInOutAdvance {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AssetCheckInOutRepository assetCheckInOutRepository;

    @Override
    public List<CheckInOutStatus> getCheckInOutStatusByAssetIds(List<String> assetIds) {
        Query query = new Query(Criteria.where("assetId").in(assetIds));
        List<AssetCheckInOut> checkInOuts = mongoTemplate.find(query, AssetCheckInOut.class);

        return checkInOuts.stream()
                .map(this::convertToCheckInOutStatus)
                .collect(Collectors.toList());

    }
    private CheckInOutStatus convertToCheckInOutStatus(AssetCheckInOut checkInOut) {
        CheckInOutStatus status = new CheckInOutStatus();
        status.setAssetId(checkInOut.getAssetId());
        status.setStatus(checkInOut.getStatus());
        // Map other fields as needed
        return status;
    }
}
