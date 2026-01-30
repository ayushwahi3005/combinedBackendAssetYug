package com.quantumai.customer.service;

import com.quantumai.customer.dto.UserInspectionAnalyticsDTO;
import com.quantumai.customer.entity.InspectionTemplateResult;
import com.quantumai.customer.entity.AssetCategoryInspectionInstance;
import com.quantumai.customer.entity.enums.InspectionInstanceStatus;
import com.quantumai.customer.repository.AssetCategoryInspectionInstanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssetInspectionServiceImpl implements AssetInspectionService {

    @Autowired
    private AssetCategoryInspectionInstanceRepository assetCategoryInspectionInstanceRepository;
    @Override
    public List<UserInspectionAnalyticsDTO> getUserInspectionAnalytics(Long companyId, LocalDate startDate, LocalDate endDate) {
        List<AssetCategoryInspectionInstance> assetCategoryInspectionInstanceList =assetCategoryInspectionInstanceRepository.findByCompanyId(companyId);
        List<UserInspectionAnalyticsDTO> userInspectionAnalyticsDTOList = new ArrayList<>();
        Map<String, Long> countByActionPerformedBy =
                assetCategoryInspectionInstanceList.stream()
                        .filter(data -> data.getStatus() == InspectionInstanceStatus.COMPLETED)
                        .collect(Collectors.groupingBy(
                                AssetCategoryInspectionInstance::getActionPerformedBy,
                                Collectors.counting()
                        ))
                        .entrySet()
                        .stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (a, b) -> a,
                                LinkedHashMap::new
                        ));

        countByActionPerformedBy.forEach((userName, count) -> {
            UserInspectionAnalyticsDTO userInspectionAnalyticsDTO = new UserInspectionAnalyticsDTO();
            userInspectionAnalyticsDTO.setUserName(userName);
            userInspectionAnalyticsDTO.setTotalCompletedInspections(count);
            // In a real scenario, you would fetch the userName from a User service or repository
//            userInspectionAnalyticsDTO.setUserName("User_" + userId); // Placeholder for user name
            userInspectionAnalyticsDTOList.add(userInspectionAnalyticsDTO);
        });
        return userInspectionAnalyticsDTOList;
    }

    @Override
    public Map<InspectionInstanceStatus, Long> getStatusDistribution(Long companyId, LocalDate startDate, LocalDate endDate) {
        List<AssetCategoryInspectionInstance> assetCategoryInspectionInstanceList =assetCategoryInspectionInstanceRepository.findByCompanyId(companyId);

        Map<InspectionInstanceStatus, Long> countByStatus =
                assetCategoryInspectionInstanceList.stream()
                        .collect(Collectors.groupingBy(
                                AssetCategoryInspectionInstance::getStatus,
                                Collectors.counting()
                        ));
        return countByStatus;

    }

    @Override
    public Map<String, Long> getInspectionTypeCompletion(Long companyId, LocalDate startDate, LocalDate endDate) {
        List<AssetCategoryInspectionInstance> assetCategoryInspectionInstanceList =assetCategoryInspectionInstanceRepository.findByCompanyId(companyId);
        Map<String, Long> completedInspectionTypeCount =
                assetCategoryInspectionInstanceList.stream()
                        .filter(i -> i.getStatus() == InspectionInstanceStatus.COMPLETED)
                        .flatMap(i -> Optional.ofNullable(i.getInspectionTemplates())
                                .orElse(Collections.emptyList())
                                .stream())
                        .collect(Collectors.groupingBy(
                                InspectionTemplateResult::getInspectionName,
                                Collectors.counting()
                        ));


        return completedInspectionTypeCount;
    }

    @Override
    public Map<String, Long> getLeadInspector(Long companyId,
                                              LocalDate startDate,
                                              LocalDate endDate) {

        List<AssetCategoryInspectionInstance> assetCategoryInspectionInstanceList =
                assetCategoryInspectionInstanceRepository.findByCompanyId(companyId);

        return assetCategoryInspectionInstanceList.stream()
                .filter(i -> i.getStatus() == InspectionInstanceStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        AssetCategoryInspectionInstance::getActionPerformedBy,
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue
                ))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByKey()) // max count
                .map(maxEntry ->
                        maxEntry.getValue().stream()
                                .sorted(Map.Entry.comparingByKey()) // alphabetical
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (a, b) -> a,
                                        LinkedHashMap::new
                                ))
                )
                .orElse(new LinkedHashMap<>());
    }

    @Override
    public Map<String, Long> getAssetInspectionDetails(Long companyId) {
        List<AssetCategoryInspectionInstance> assetCategoryInspectionInstanceList =
                assetCategoryInspectionInstanceRepository.findByCompanyId(companyId);
        long totalAsset=assetCategoryInspectionInstanceList.stream()
                .map(AssetCategoryInspectionInstance::getAssetId).distinct().count();
        long completedInspections=
        assetCategoryInspectionInstanceList.stream().filter(i->i.getStatus()==InspectionInstanceStatus.COMPLETED).count();

        long totalInspections=assetCategoryInspectionInstanceList.size();

        Map<String, Long> inspectionDetails = new HashMap<>();
        inspectionDetails.put("totalAssetsInspected", totalAsset);
        inspectionDetails.put("completedInspections", completedInspections);
        inspectionDetails.put("totalInspections", totalInspections);
        return inspectionDetails;


    }

}
