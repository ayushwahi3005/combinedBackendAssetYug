package com.quantumai.customer.repository;

import com.quantumai.customer.entity.UserExtraFieldName;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface UserExtraFieldNameRepository extends MongoRepository<UserExtraFieldName, String> {
    public UserExtraFieldName findByNameIgnoreCaseAndCompanyId(String name, String companyId);

    public List<UserExtraFieldName> findByCompanyId(String companyId);
}
