package com.quantumai.customer.repository;


import com.quantumai.customer.entity.UserMandatoryFields;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserMandatoryFieldsRepository   extends MongoRepository<UserMandatoryFields, String> {
    public Optional<UserMandatoryFields> findByNameIgnoreCaseAndCompanyId(String name, String companyId);

    public List<UserMandatoryFields> findByCompanyId(String companyId);
}
