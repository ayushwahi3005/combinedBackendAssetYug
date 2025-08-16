package com.quantumai.customer.utility;

import com.quantumai.customer.entity.IdGenerator.AssetCategoryIdGenerator;
import com.quantumai.customer.entity.IdGenerator.CompanyCustomerCategoryIdGenerator;
import com.quantumai.customer.entity.IdGenerator.CompanyPrimaryKeyTable;
import com.quantumai.customer.repository.AssetCategoryIdGeneratorRepository;
import com.quantumai.customer.repository.CompanyCustomerCategoryIdGeneratorRepository;
import com.quantumai.customer.repository.CompanyPrimaryKeyTableRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SequenceInitializer {

  private static final String SEQ_ID_COMPANY = "company_sequence";
//  private static final String SEQ_ASSET_CATEGORY="asset_category_sequence";
//  private static final String SEQ_COMPANY_CUSTOMER_CATEGORY="company_customer_category_sequence";
  private static final long INITIAL_VALUE = 100001;

  @Autowired private CompanyPrimaryKeyTableRepository CompanySequenceRepository;
  @Autowired private AssetCategoryIdGeneratorRepository assetCategoryIdGeneratorRepository;

  @Autowired private CompanyCustomerCategoryIdGeneratorRepository companyCustomerCategoryIdGeneratorRepository;

  @PostConstruct
  public void initSequence() {
    // Check if the sequence already exists
//    if (!CompanySequenceRepository.existsById(SEQ_ID_COMPANY)) {
//      CompanyPrimaryKeyTable companyPrimaryKeyTableSequence = new CompanyPrimaryKeyTable();
//      companyPrimaryKeyTableSequence.setId(SEQ_ID_COMPANY);
//      companyPrimaryKeyTableSequence.setSeq(INITIAL_VALUE);
//      CompanySequenceRepository.save(companyPrimaryKeyTableSequence);
//      log.info("✅ Initialized Company_Sequence with {}", INITIAL_VALUE);
//    } else {
//      log.info("ℹ️ Company_Sequence already exists, skipping initialization.");
//    }

//    if (!assetCategoryIdGeneratorRepository.existsById(SEQ_ASSET_CATEGORY)) {
//      AssetCategoryIdGenerator assetCategoryIdGeneratorSequence = new AssetCategoryIdGenerator();
//      assetCategoryIdGeneratorSequence.setId(SEQ_ASSET_CATEGORY);
//      assetCategoryIdGeneratorSequence.setSeq(1L);
//      assetCategoryIdGeneratorRepository.save(assetCategoryIdGeneratorSequence);
//      log.info("✅ Initialized asset_category_sequence with {}", 1);
//    } else {
//      log.info("ℹ️ Asset_Category_Sequence already exists, skipping initialization.");
//    }
//
//    if (!companyCustomerCategoryIdGeneratorRepository.existsById(SEQ_COMPANY_CUSTOMER_CATEGORY)) {
//      CompanyCustomerCategoryIdGenerator companyCustomerCategoryIdGeneratorSequence = new CompanyCustomerCategoryIdGenerator();
//      companyCustomerCategoryIdGeneratorSequence.setId(SEQ_COMPANY_CUSTOMER_CATEGORY);
//      companyCustomerCategoryIdGeneratorSequence.setSeq(1L);
//      companyCustomerCategoryIdGeneratorRepository.save(companyCustomerCategoryIdGeneratorSequence);
//      log.info("✅ Initialized asset_category_sequence with {}", 1);
//    } else {
//      log.info("ℹ️ Company_Customer_Category_Sequence already exists, skipping initialization.");
//    }



  }
}
