package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Bin;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.quantumai.customer.entity.Location;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BinRepository extends MongoRepository<Bin, String> , CompanyScopedRepository {

  List<Bin> findByCompanyId(Long id);
  Optional<Bin> findByCompanyIdAndBinNumberIgnoreCase(Long companyId, String binNumber);
  List<Bin> findByCompanyIdAndBinNumberIgnoreCaseAndLocationId(Long companyId, String binNumber, Location locationId);

  public void deleteByCompanyId(Long companyId);
}
