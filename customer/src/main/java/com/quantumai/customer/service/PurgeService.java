package com.quantumai.customer.service;

import com.quantumai.customer.entity.AssetCategory;
import com.quantumai.customer.entity.AssetCategoryInspection;
import com.quantumai.customer.entity.Bin;
import com.quantumai.customer.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PurgeService {

    @Autowired
    private AssetCategoryInspectionInstanceRepository assetCategoryInspectionInstanceRepository;

    @Autowired
    private AssetCategoryInspectionRepository assetCategoryInspectionRepository;



    @Autowired
    private AssetCategoryRepository assetCategoryRepository;

    @Autowired
    private AssetCheckInOutRepository assetCheckInOutRepository;

    @Autowired
    private AssetExtraFieldNameRepository assetExtraFieldNameRepository;

    @Autowired
    private AssetExtraFieldsRepository assetExtraFieldsRepository;

    @Autowired
    private AssetFileRepository assetFileRepository;

    @Autowired
    private AssetMandatoryFieldsRepository assetMandatoryFieldsRepository;

    @Autowired
    private AssetShowFieldsRepository assetShowFieldsRepository;

    @Autowired
    private AssetsRepository assetsRepository;

    @Autowired
    private BinRepository binRepository;

    @Autowired
    private CompanyCustomerCategoryRepository companyCustomerCategoryRepository;

    @Autowired
    private CompanyCustomerExtraFieldNameRepository companyCustomerExtraFieldNameRepository;

    @Autowired
    private CompanyCustomerExtraFieldsRepository companyCustomerExtraFieldsRepository;

    @Autowired
    private CompanyCustomerFileRepository companyCustomerFileRepository;

    @Autowired
    private CompanyCustomerMandatoryFieldsRepository companyCustomerMandatoryFieldsRepository;

    @Autowired
    private CompanyCustomerShowFieldsRepository companyCustomerShowFieldsRepository;

    @Autowired
    private CompanyCustomerRepository companyCustomerRepository;

    @Autowired
    private CustomRoleRepository customRoleRepository;

    @Autowired
    private ImportHistoryRepository importHistoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserExtraFieldNameRepository userExtraFieldNameRepository;

    @Autowired
    private UserExtraFieldsRepository userExtraFieldsRepository;

    @Autowired
    private UserMandatoryFieldsRepository userMandatoryFieldsRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private UserShowFieldsRepository userShowFieldsRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TrialStatusRepository trialStatusRepository;

    @Value("${purge.expiry.days}")
    private int purgeExpiryDays;


    public void purgeOldData() {
        // Implementation for purging old data goes here
        log.info("Purge old data process started.");
        // delete old records from database based on companyId from all repositry table except user table and customer table



        trialStatusRepository.findByTrialEndDateBefore(java.time.LocalDateTime.now().minusDays(purgeExpiryDays))
                .forEach(trialStatus -> {
                    Long companyId = trialStatus.getCompanyId();
                    deleteOldDataFromAllTables(companyId);
                    log.info("Purged data for companyId: {}", companyId);
                });




    }
    void deleteOldDataFromAllTables(Long companyId) {
        // Default implementation (can be overridden by specific repositories)
        List<String> repoList = List.of(
                "assetCategoryInspectionInstanceRepository",
                "assetCategoryInspectionRepository",
                "assetCategoryRepository",
                "assetCheckInOutRepository",
                "assetExtraFieldNameRepository",
                "assetExtraFieldsRepository",
                "assetFileRepository",
                "assetMandatoryFieldsRepository",
                "assetShowFieldsRepository",
                "assetsRepository",
                "binRepository",
                "companyCustomerCategoryRepository",
                "companyCustomerExtraFieldNameRepository",
                "companyCustomerExtraFieldsRepository",
                "companyCustomerFileRepository",
                "companyCustomerMandatoryFieldsRepository",
                "companyCustomerShowFieldsRepository",
                "companyCustomerRepository",
                "customRoleRepository",
                "importHistoryRepository",
                "locationRepository",
                "notificationRepository",
                "userExtraFieldNameRepository",
                "userExtraFieldsRepository",
                "userMandatoryFieldsRepository",
                "userNotificationRepository",
//                "usersRepository",
                "userShowFieldsRepository"
        );

        for (String repoName : repoList) {
            Object bean = applicationContext.getBean(repoName);


            if (bean instanceof CompanyScopedRepository repository) {
                repository.deleteByCompanyId(companyId);
                log.info("Purged data from {}", repoName);
            } else {
                log.warn("{} does not support company purge", repoName);
            }
        }


    }
}
