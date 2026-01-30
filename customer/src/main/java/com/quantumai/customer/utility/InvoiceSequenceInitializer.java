package com.quantumai.customer.utility;

import com.quantumai.customer.entity.IdGenerator.CompanyPrimaryKeyTable;
import com.quantumai.customer.entity.IdGenerator.InvoicePrimaryKeyTable;
import com.quantumai.customer.repository.InvoicePrimaryKeyTableRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class InvoiceSequenceInitializer {

    private static final String SEQ_ID = "invoice_sequence";
    private static final long INITIAL_VALUE = 1001;

    @Autowired
    private InvoicePrimaryKeyTableRepository invoicePrimaryKeyTableRepository;

    @PostConstruct
    public void initCompanySequence() {
        // Check if the sequence already exists
        if (!invoicePrimaryKeyTableRepository.existsById(SEQ_ID)) {
            InvoicePrimaryKeyTable sequence = new InvoicePrimaryKeyTable();
            sequence.setId(SEQ_ID);
            sequence.setSeq(INITIAL_VALUE);
            invoicePrimaryKeyTableRepository.save(sequence);
            log.info("✅ Initialized invoice_sequence with {}", INITIAL_VALUE);
        } else {
            log.info("ℹ️ Invoice Sequence already exists, skipping initialization.");
        }
    }
}
