package com.quantumai.customer.service;

import com.quantumai.customer.entity.IdGenerator.InvoicePrimaryKeyTable;
import com.quantumai.customer.repository.InvoicePrimaryKeyTableRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class InvoiceService {

    @Autowired
    private InvoicePrimaryKeyTableRepository invoicePrimaryKeyTableRepository;

    public Long getNextInvoiceSequence() {
        Optional<InvoicePrimaryKeyTable> invoicePrimaryKeyTableOptional = invoicePrimaryKeyTableRepository.findById("invoice_sequence");

        if (invoicePrimaryKeyTableOptional.isEmpty()) {
            log.error("Invoice sequence not initialized.");
            throw new IllegalStateException("Invoice sequence not initialized.");
        }
        InvoicePrimaryKeyTable invoicePrimaryKeyTable = invoicePrimaryKeyTableOptional.get();
        Long seq = invoicePrimaryKeyTable.getSeq();
        invoicePrimaryKeyTable.setSeq(seq + 1);
        invoicePrimaryKeyTableRepository.save(invoicePrimaryKeyTable);
        log.info("Invoice sequence Number : {}", seq);
        return seq;
    }
}
