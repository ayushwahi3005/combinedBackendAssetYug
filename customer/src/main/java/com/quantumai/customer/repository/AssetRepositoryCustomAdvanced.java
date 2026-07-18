package com.quantumai.customer.repository;

import com.quantumai.customer.dto.AssetAdvancedFilterDTO;
import com.quantumai.customer.dto.AssetWithCustomFieldsDTO;
import com.quantumai.customer.dto.PaginatedAssetResponseDTO;
import com.quantumai.customer.entity.Assets;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AssetRepositoryCustomAdvanced {

    PaginatedAssetResponseDTO getAssetsWithAdvancedFilter(AssetAdvancedFilterDTO filter);

    List<AssetWithCustomFieldsDTO> findAllWithAdvancedFilter(AssetAdvancedFilterDTO filter);

    long countAssetsWithAdvancedFilter(AssetAdvancedFilterDTO filter);

    Page<Assets> findByCompanyIdWithSort(Long companyId, String sortField, String sortDirection, Pageable pageable);
}

