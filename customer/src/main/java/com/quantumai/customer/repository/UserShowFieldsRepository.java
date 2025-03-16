package com.quantumai.customer.repository;

import com.quantumai.customer.entity.UserShowFields;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserShowFieldsRepository extends MongoRepository<UserShowFields, String> {
    public Optional<UserShowFields> findByNameIgnoreCaseAndCompanyId(String name, String companyId);

    public List<UserShowFields> findByCompanyId(String companyId);
}
