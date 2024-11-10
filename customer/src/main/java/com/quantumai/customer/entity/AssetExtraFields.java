package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class AssetExtraFields {
	@Id
	private String id;
	private String email;
	private String name;
	private String value;
	private String assetId;
	private String type;
	private String companyId;
}
