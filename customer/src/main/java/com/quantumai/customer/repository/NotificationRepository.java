package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification,String> , CompanyScopedRepository{
    List<Notification> findByCompanyId(Long companyId);

    public void deleteByCompanyId(Long companyId);
//    List<Notification> findByRead(Boolean read);

}
