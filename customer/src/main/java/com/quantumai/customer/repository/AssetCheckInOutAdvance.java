package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CheckInOutStatus;

import java.util.List;

public interface AssetCheckInOutAdvance {

    List<CheckInOutStatus> getCheckInOutStatusByAssetIds(List<String> assetIds);
}
