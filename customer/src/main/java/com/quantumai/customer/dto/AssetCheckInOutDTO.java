package com.quantumai.customer.dto;


import lombok.Data;

import java.util.List;

@Data
public class AssetCheckInOutDTO {

	private String id;
	private String assetId;
	private String status;
	private List<AssetCheckInOutDetailsDTO> detailsList;
	private String companyId;
}
