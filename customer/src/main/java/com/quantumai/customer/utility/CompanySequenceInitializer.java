package com.quantumai.customer.utility;

import com.quantumai.customer.entity.CompanyPrimaryKeyTable;
import com.quantumai.customer.repository.CompanyPrimaryKeyTableRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CompanySequenceInitializer {

  private static final String SEQ_ID = "company_sequence";
  private static final long INITIAL_VALUE = 100001;

  @Autowired private CompanyPrimaryKeyTableRepository sequenceRepository;

  @PostConstruct
  public void initCompanySequence() {
    // Check if the sequence already exists
    if (!sequenceRepository.existsById(SEQ_ID)) {
      CompanyPrimaryKeyTable sequence = new CompanyPrimaryKeyTable();
      sequence.setId(SEQ_ID);
      sequence.setSeq(INITIAL_VALUE);
      sequenceRepository.save(sequence);
      log.info("✅ Initialized company_sequence with {}", INITIAL_VALUE);
    } else {
      log.info("ℹ️ Sequence already exists, skipping initialization.");
    }
  }
}
