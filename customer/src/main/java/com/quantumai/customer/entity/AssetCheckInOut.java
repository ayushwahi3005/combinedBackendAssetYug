package com.quantumai.customer.entity;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document
public class AssetCheckInOut {

	@Id
	private String id;
	private String assetId;
	private String status;
	private String companyId;
	private List<AssetCheckInOutDetails> detailsList;
}
