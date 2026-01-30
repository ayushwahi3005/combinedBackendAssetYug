package com.quantumai.customer.repository;

public interface CompanyScopedRepository {
    void deleteByCompanyId(Long companyId);
}
