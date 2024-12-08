package com.quantumai.customer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.exception.CategoryException;
import com.quantumai.customer.exception.ExtraFieldAlreadyPresentException;
import com.quantumai.customer.repository.AssetFileRepository;
import com.quantumai.customer.repository.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class AssetsServiceImpl implements AssetsService {

	@Autowired
	private AssetFileRepository assetFileRepository;

	@Autowired
	private AssetCategoryRepository assetCategoryRepository;
	
	@Autowired
	private AssetsRepository assetsRepository;
	@Autowired
	private AssetExtraFieldsRepository extraFieldsRepository;
	
	@Autowired
	private AssetExtraFieldNameRepository extraFieldNameRepository;
	
	@Autowired
	private AssetCheckInOutRepository checkInOutRepository;
	
	@Autowired
	private AssetMandatoryFieldsRepository mandatoryFieldsRepository;
	@Autowired
	private AssetShowFieldsRepository showFieldsRepository;
	
	@Autowired
	private AssetIdTableRepository idTableRepository;
	
	@Autowired
	private AssetQRRepository qrRepository;

	LocalDateTime localDateTime;
	
	 private ModelMapper modelMapper=new ModelMapper();
	@Override
	public List<AssetsDTO> getAssetsDetails(String companyId) {
		// TODO Auto-generated method stub
		List<Assets> assetsList=assetsRepository.findByCompanyId(companyId);
		List<AssetsDTO> assetsDTOList=new ArrayList<AssetsDTO>();
		assetsList.stream().forEach(x->{
			AssetsDTO assetsDTO=modelMapper.map(x, AssetsDTO.class);
			assetsDTOList.add(assetsDTO);
		});
		return assetsDTOList;
	}
	@Override
	public AssetsDTO addAssets(AssetsDTO assetsDTO) {
		// TODO Auto-generated method stub
		Assets assets=modelMapper.map(assetsDTO, Assets.class);
		if(assets.getAssetId()==null) {
			Optional<AssetIdTable> optionalIdTable=idTableRepository.findByCompanyId(assetsDTO.getCompanyId());
			if(optionalIdTable.isEmpty()) {
				assets.setAssetId(1);
				AssetIdTable myidTable=new AssetIdTable();
				myidTable.setTableId(2);
				myidTable.setCompanyId(assetsDTO.getCompanyId());
//				//System.out.println("---------------------new---------->"+idTable.getTableId());
				idTableRepository.save(myidTable);
			}
			else {
//				List<AssetIdTable> idTableList=idTableRepository.findAll();
//				AssetIdTable idTable=idTableList.get(0);
				AssetIdTable idTable=optionalIdTable.get();
				assets.setAssetId(idTable.getTableId());
				idTable.updateId();
//				//System.out.println("---------------------already---------->"+idTable.getTableId()+" "+idTable.get);
				idTableRepository.save(idTable);
			}
		}

		assets.setUpdatedAt(LocalDateTime.now().toString());
		AssetsDTO myAssetsDTO=modelMapper.map(assetsRepository.save(assets),AssetsDTO.class);
		return myAssetsDTO;
		
	}
	@Override
	public void importExcel(List<AssetsDTO> assetsDTOList,Map<String,String> columnMap) {
		// TODO Auto-generated method stub
		
		if(assetsDTOList.isEmpty()) {
			return;
		}
		Optional<AssetIdTable> optionalIdTable=idTableRepository.findByCompanyId(assetsDTOList.get(0).getCompanyId());
		assetsDTOList.stream().forEach(x->{
			
			Assets assets=modelMapper.map(x, Assets.class);
			if(optionalIdTable.isEmpty()) {
				assets.setAssetId(1);
				AssetIdTable myidTable=new AssetIdTable();
				myidTable.setTableId(2);
				myidTable.setCompanyId(assetsDTOList.get(0).getCompanyId());
//				//System.out.println("---------------------new---------->"+idTable.getTableId());
				idTableRepository.save(myidTable);
			}
			else {
			
			AssetIdTable myidTable=optionalIdTable.get();
			assets.setAssetId(myidTable.getTableId());
			myidTable.updateId();
			idTableRepository.save(myidTable);
			
			}
			assets.setUpdatedAt(LocalDateTime.now().toString());
			assetsRepository.save(assets);
		});
		
		
	}
	@Override
	public void addImage(AssetImageDTO assetImageDTO) throws Exception {
		// TODO Auto-generated method stub
		Optional<Assets> optionalAssets=assetsRepository.findById(assetImageDTO.getId());
		Assets asset=optionalAssets.orElseThrow(()-> new Exception("No Such Asset"));
		asset.setImage(assetImageDTO.getImage());
		
		assetsRepository.save(asset);
		
	}
	@Override
	public void removeImage(String id) throws Exception {
		// TODO Auto-generated method stub
		Optional<Assets> optionalAssets=assetsRepository.findById(id);
		Assets asset=optionalAssets.orElseThrow(()-> new Exception("No Such Asset"));
		asset.setImage("");
		assetsRepository.save(asset);
		
	}
	@Override
	public void removeAsset(String id) throws Exception {
		// TODO Auto-generated method stub
		
		assetsRepository.deleteById(id);
		
	}
	@Override
	public AssetsDTO getAsset(String assetId) throws Exception {
		// TODO Auto-generated method stub
		Optional<Assets> optionalasset=assetsRepository.findById(assetId);
		Assets asset=optionalasset.orElseThrow(()-> new Exception("No Such Asset"));
		AssetsDTO assetDTO=modelMapper.map(asset, AssetsDTO.class);
		return assetDTO;
		
	}
	@Override
	public void addExtraFields(AssetExtraFieldsDTO extraFieldsDTO) throws Exception {
		// TODO Auto-generated method stub
		extraFieldsDTO.setName(extraFieldsDTO.getName().toLowerCase());

//		List<AssetExtraFields> extraFieldsList=extraFieldsRepository.findByName(extraFieldsDTO.getName().toLowerCase());
//		if(!extraFieldsList.isEmpty()) {
//			throw new Exception("Extra Field Already Present");
//		}
		AssetExtraFields extraFields=modelMapper.map(extraFieldsDTO, AssetExtraFields.class);
		extraFieldsRepository.save(extraFields);
	
		
		
	}
	@Override
	public List<AssetExtraFieldsDTO> getExtraFields(String id) {
		// TODO Auto-generated method stub
		List<AssetExtraFields> extraFieldsList=extraFieldsRepository.findByAssetId(id);
		if(extraFieldsList.isEmpty()) {
			return null;
		}
		List<AssetExtraFieldsDTO> extraFieldsDTOList=new ArrayList<>();
		extraFieldsList.stream().forEach((x)->{
			AssetExtraFieldsDTO extraFieldsDTO=modelMapper.map(x, AssetExtraFieldsDTO.class);
			extraFieldsDTOList.add(extraFieldsDTO);
		});
		return extraFieldsDTOList;
	}
	@Override
	public void deleteExtraFields(String id) throws Exception {
		// TODO Auto-generated method stub
		Optional<AssetExtraFields> extraFields=extraFieldsRepository.findById(id);
		if(extraFields.isEmpty()) {
			throw new Exception("No such extra Field");
		}
		extraFieldsRepository.delete(extraFields.get());
		
	}
	@Override
	public List<AssetExtraFieldNameDTO> getAssetExtraField(String companyId) {
		// TODO Auto-generated method stub
		List<AssetExtraFieldName> extraFieldNameList=extraFieldNameRepository.findByCompanyId(companyId);
		List<AssetExtraFieldNameDTO> extraFieldNameListDTO=new ArrayList<>();
		extraFieldNameList.stream().forEach((x)->{
			AssetExtraFieldNameDTO extraFieldNameDTO=modelMapper.map(x, AssetExtraFieldNameDTO.class);
			extraFieldNameListDTO.add(extraFieldNameDTO);
		});
		return extraFieldNameListDTO;
	}
	@Override
	public void addAssetExtraField(AssetExtraFieldNameDTO extraFieldNameDTO) throws ExtraFieldAlreadyPresentException {
		// TODO Auto-generated method stub
		AssetExtraFieldName extraFieldNameNew=extraFieldNameRepository.findByNameAndCompanyId(extraFieldNameDTO.getName().toLowerCase(),extraFieldNameDTO.getCompanyId());
		if(extraFieldNameNew!=null) {
			throw new ExtraFieldAlreadyPresentException("Extra Field Already Present");
		}
		extraFieldNameDTO.setName(extraFieldNameDTO.getName().toLowerCase());
		
		AssetExtraFieldName extraFieldName=modelMapper.map(extraFieldNameDTO, AssetExtraFieldName.class);
		extraFieldNameRepository.save(extraFieldName);
		
		
	}
	@Override
	public void deleteAssetExtraField(String id) {
		// TODO Auto-generated method stub
		
		Optional<AssetExtraFieldName> extraFieldNameOptional=extraFieldNameRepository.findById(id);
		extraFieldNameRepository.deleteById(id);
		AssetExtraFieldName extraFieldName=extraFieldNameOptional.get();
		List<AssetExtraFields> extraFieldsList=extraFieldsRepository.findByName(extraFieldName.getName().toLowerCase());
		extraFieldsList.stream().forEach((x)->{
			//System.out.println("-------------------------------------->"+x.getName());
			extraFieldsRepository.delete(x);
		});
		
		
		
	}
	@Override
	public Map<String, Map<String,String>> getextraFieldList(String companyId) {
		// TODO Auto-generated method stub
		List<AssetExtraFields> extraFieldNameList=extraFieldsRepository.findByCompanyId(companyId);
		List<Assets> assetList=assetsRepository.findByCompanyId(companyId);
		Map<String, Map<String,String>> fieldNameValueMap=new HashMap<>();
		
		assetList.stream().forEach((asset)->{
			Map<String,String> m=new HashMap<>();
			extraFieldNameList.stream().forEach((field)->{
				
				if(field.getAssetId().endsWith(asset.getId()) ) {
					m.put(field.getName(), field.getValue());
					
				}
			
				
				
		
			});
			fieldNameValueMap.put(asset.getId(), m);
		});
		return fieldNameValueMap;
	}
	@Override
	public void addCheckInOut(AssetCheckInDTO checkInDTO) {
		// TODO Auto-generated method stub
		
//		checkInOutDetailsDTO.stream().forEach((x)->{
//			//System.out.println("--------------------------------------------"+x.getDate());
//		});
//		//System.out.println("--------------------------------------------"+checkInOutDTO.getDetailsList().size());
		Optional<AssetCheckInOut> checkInOutList=checkInOutRepository.findByAssetId(checkInDTO.getAssetId());
	
		
		if(checkInOutList.isEmpty()) {
			AssetCheckInOut checkInOut=new AssetCheckInOut();
			checkInOut.setAssetId(checkInDTO.getAssetId());
			checkInOut.setStatus(checkInDTO.getStatus());
			checkInOut.setCompanyId(checkInDTO.getCompanyId());
			AssetCheckInOutDetails checkInOutDetails=new AssetCheckInOutDetails();
			checkInOutDetails.setStatus(checkInDTO.getStatus());
			checkInOutDetails.setDate(checkInDTO.getDate());
			checkInOutDetails.setEmployee(checkInDTO.getEmployee());
			checkInOutDetails.setLocation(checkInDTO.getLocation());
			checkInOutDetails.setNotes(checkInDTO.getNotes());
			List<AssetCheckInOutDetails> checkInOutDetailsList=new ArrayList<>();
			checkInOutDetailsList.add(checkInOutDetails);
			checkInOut.setDetailsList(checkInOutDetailsList);
			checkInOutRepository.save(checkInOut);
			
			
			
			
		}
		else {
			checkInOutList.stream().forEach((x)->{
				
				x.setAssetId(checkInDTO.getAssetId());
				x.setStatus(checkInDTO.getStatus());
				x.setCompanyId(checkInDTO.getCompanyId());
				
				List<AssetCheckInOutDetails> checkInOutDetailsList=x.getDetailsList();
				AssetCheckInOutDetails checkInOutDetails=new AssetCheckInOutDetails();
				checkInOutDetails.setStatus(checkInDTO.getStatus());
				checkInOutDetails.setDate(checkInDTO.getDate());
				checkInOutDetails.setEmployee(checkInDTO.getEmployee());
				checkInOutDetails.setLocation(checkInDTO.getLocation());
				checkInOutDetails.setNotes(checkInDTO.getNotes());
				checkInOutDetailsList.add(checkInOutDetails);
				x.setDetailsList(checkInOutDetailsList);
				checkInOutRepository.save(x);
			});
		}
		
		
	
		
	}
	@Override
	public List<AssetCheckInOutDTO> getCheckOutInList(String assetId) {
		// TODO Auto-generated method stub
		Optional<AssetCheckInOut>  checkInOutList=checkInOutRepository.findByAssetId(assetId);
		List<AssetCheckInOutDTO>  checkInOutDTOList=new ArrayList<>();
		if(!checkInOutList.isEmpty()) {
			
			checkInOutList.stream().forEach((x)->{
				AssetCheckInOutDTO checkInOutDTO=modelMapper.map(x, AssetCheckInOutDTO.class);
				checkInOutDTOList.add(checkInOutDTO);
			});
		}
		return checkInOutDTOList;
	}
	@Override
	public AssetFile addAssetFile(MultipartFile file,String assetId) throws IOException {
		// TODO Auto-generated method stub
		 String fileName = StringUtils.cleanPath(file.getOriginalFilename());
		    AssetFile assetFile = new AssetFile();
		    assetFile.setAssetId(assetId);
		    assetFile.setFile(file.getBytes());
		    assetFile.setFileName(fileName);

		    return assetFileRepository.save(assetFile);
//		AssetFile assetFile=modelMapper.map(assetFileDTO, AssetFile.class);
//		assetFileRepository.save(assetFile);
		
		
	}
	@Override
	public List<AssetFileDTO> getAssetFile(String assetId) {
		// TODO Auto-generated method stub
		
		List<AssetFile> assetFileList=assetFileRepository.findByAssetId(assetId);
		if(assetFileList.size()==0) {
			return null;
		}
		else {
			List<AssetFileDTO> assetFileDTOList=new ArrayList<>();
			assetFileList.stream().forEach((x)->{
				AssetFileDTO assetFileDTO=modelMapper.map(x, AssetFileDTO.class);
				
				assetFileDTOList.add(assetFileDTO);
			});
			
			
			return assetFileDTOList;
		}
		
	}
	@Override
	public AssetFileDTO downloadFile(String id) {
		// TODO Auto-generated method stub
		Optional<AssetFile> assetFile=assetFileRepository.findById(id);
		AssetFileDTO assetFileDTO=modelMapper.map(assetFile.get(), AssetFileDTO.class);
		return assetFileDTO;
	}
	@Override
	public void deleteFile(String id) {
		// TODO Auto-generated method stub
		assetFileRepository.deleteById(id);
		
	}
	@Override
	public void updateShowFields(AssetShowFields showFields) {
		// TODO Auto-generated method stub
		Optional<AssetShowFields> showFieldsOptional=showFieldsRepository.findByNameAndCompanyId(showFields.getName(),showFields.getCompanyId());
		AssetShowFields myShowFields=new AssetShowFields();
		if(showFieldsOptional.isPresent()) {
			myShowFields=showFieldsOptional.get();
			showFields.setId(myShowFields.getId());
			
		}
		showFieldsRepository.save(showFields);
		
	}
	@Override
	public void updateMandatoryFields(AssetMandatoryFields mandatoryFields) {
		// TODO Auto-generated method stub
		Optional<AssetMandatoryFields> mandatoryFieldsOptional=mandatoryFieldsRepository.findByNameAndCompanyId(mandatoryFields.getName(),mandatoryFields.getCompanyId());
		AssetMandatoryFields myMandatoryFields=new AssetMandatoryFields();
		if(mandatoryFieldsOptional.isPresent()) {
			myMandatoryFields=mandatoryFieldsOptional.get();
			mandatoryFields.setId(myMandatoryFields.getId());
			
		}
		mandatoryFieldsRepository.save(mandatoryFields);
	}

	@Override
	public AssetShowFields getShowFields(String name, String companyId) {
		// TODO Auto-generated method stub
		Optional<AssetShowFields> showFieldsOptional=showFieldsRepository.findByNameAndCompanyId(name,companyId);
		if(showFieldsOptional.isPresent()) {
			return showFieldsOptional.get();
			}
			else {
				return null;
			}
	}
	@Override
	public AssetMandatoryFields getMandatoryFields(String name, String companyId) {
		// TODO Auto-generated method stub
		Optional<AssetMandatoryFields> mandatoryFieldsOptional=mandatoryFieldsRepository.findByNameAndCompanyId(name,companyId);
		if(mandatoryFieldsOptional.isPresent()) {
		return mandatoryFieldsOptional.get();
		}
		else {
			return null;
		}
	}
	@Override
	public List<AssetShowFields> getAllShowFields(String companyId) {
		// TODO Auto-generated method stub
		List<AssetShowFields> showFieldsList=showFieldsRepository.findByCompanyId(companyId);
		return showFieldsList;
	}
	@Override
	public List<AssetMandatoryFields> getAllMandatoryFields(String companyId) {
		// TODO Auto-generated method stub
		List<AssetMandatoryFields> mandatoryFieldsList=mandatoryFieldsRepository.findByCompanyId(companyId);
		return mandatoryFieldsList;
	}
	@Override
	public void deleteShowAndMandatoryFields(String companyId, String name) {
		// TODO Auto-generated method stub
		Optional<AssetShowFields> showFieldsOptional=showFieldsRepository.findByNameAndCompanyId(name, companyId);
		showFieldsRepository.delete(showFieldsOptional.get());
		Optional<AssetMandatoryFields> mandatoryFieldsOptional=mandatoryFieldsRepository.findByNameAndCompanyId(name, companyId);
		if(mandatoryFieldsOptional.isPresent()) {
		mandatoryFieldsRepository.delete(mandatoryFieldsOptional.get());
		}
	}
	@Override
	public void updateAssetWithFile(List<AssetsDTO> assetsDTOList,String companyId) {
		// TODO Auto-generated method stub
		
		
		
		
		
		assetsDTOList.stream().forEach((x)->{
			//System.out.println("------------------------->>>"+x.getAssetId());
			Assets assets=assetsRepository.findByAssetIdAndCompanyId(x.getAssetId(),companyId);
			x.setId(assets.getId());
			x.setImage(assets.getImage());
			
			
			Assets myasset=modelMapper.map(x, Assets.class);
			assetsRepository.save(myasset);
		});
		
	}
	@Override
	public void qrDataUpdation(AssetQR qr) {
		// TODO Auto-generated method stub
		Optional<AssetQR> optionalQr = qrRepository.findByCompanyId(qr.getCompanyId());
		if(optionalQr.isEmpty()) {
			qrRepository.save(qr);
		}
		else {
			qr.setId(optionalQr.get().getId());
			qrRepository.save(qr);
		}
		
		
	}
	@Override
	public AssetQR getQRData(String companyId) {
		// TODO Auto-generated method stub
		Optional<AssetQR> optionalQr = qrRepository.findByCompanyId(companyId);
		if(optionalQr.isPresent()) {
			return optionalQr.get();
		}
		return null;
	}
	@Override
	public PaginatedResultDTO<String> getAllAssetDetails(String companyId) {
		// TODO Auto-generated method stub
		List<AssetExtraFieldName> extraFieldNameList=extraFieldNameRepository.findByCompanyId(companyId);

		List<Assets> assetList= assetsRepository.findByCompanyId(companyId);

		
		
		List<String> mapList=new ArrayList<>();
		assetList.stream().forEach((order)->{
			List<AssetExtraFields> extraFieldsList=extraFieldsRepository.findByAssetId(order.getId());
			Map<String,String> m=new HashMap<>();
			extraFieldNameList.stream().forEach((x)->{
				m.put(x.getName(), "");
				extraFieldsList.stream().forEach((x1)->{
					m.put(x1.getName(), x1.getValue());
				});
			});
			m.put("id", order.getId());
			m.put("image",order.getImage());
			m.put("email",order.getEmail());
			m.put("name",order.getName());
			m.put("assetId",order.getAssetId().toString());
			m.put("companyId",order.getCompanyId());
			m.put("serialNumber",order.getSerialNumber());
			m.put("category",order.getCategory());
			m.put("customer",order.getCustomer());
			m.put("customerId",order.getCustomerId());
			m.put("location",order.getLocation());
			m.put("status",order.getStatus());
			m.put("updatedAt",order.getUpdatedAt());
			
			
//			if(order.getDueDate()!=null) m.put("dueDate",order.getDueDate().toString());
//			if(order.getLastUpdate()!=null) m.put("lastUpdate",order.getLastUpdate().toString());
//			if(order.getPriority()!=null) m.put("priority",order.getPriority().toString());
//			if(order.getStatus()!=null) m.put("status",order.getStatus().toString());
			ObjectMapper objectMapper = new ObjectMapper();
			try {
	            // Convert POJO to JSON string
	            String json = objectMapper.writeValueAsString(m);
	           
	            mapList.add(json);
//	            System.out.print(json);
//	            System.out.print(m);
	           
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	           
	            
	        }
			
			
		});
		

		
		 return new PaginatedResultDTO<>(mapList, mapList.size());
	}
	@Override
	public PaginatedResultDTO<String> sortAssets(String companyId, String field,Integer pageNumber,Integer pageSize) {
		System.out.println("--->"+field);
		AssetExtraFieldName extraFieldName=extraFieldNameRepository.findByNameAndCompanyId(field, companyId);
//		//System.out.println(extraFieldName);

		List<Map<String,String> > mapList=new ArrayList<>();
		PaginatedResultDTO<String> myList=getAllAssetDetails(companyId);
		ObjectMapper objectMapper = new ObjectMapper();
		myList.getData().stream().forEach((asset)->{
			Map<String,String> m=new HashMap<>();
			 try {
		         
		            m= objectMapper.readValue(asset, new TypeReference<Map<String, String>>() {});
		            mapList.add(m);
		           
		        } catch (Exception e) {
		         
		            e.printStackTrace();
		          
		        }
		});

		Comparator<Map<String, String>> customComparator=null;

			customComparator = Comparator.comparing(m -> m.get(field));

		

		
		List<Map<String, String>> res= mapList.stream().sorted(customComparator).collect(Collectors.toList());
//		 //System.out.println(res);
//		 //System.out.println("------------------------->"+res.size());
		 List<String> resList=new ArrayList<>();
		 for(int i=0;i<res.size();i++) {
			 try {

		          String json = objectMapper.writeValueAsString(res.get(i));
		          
		          resList.add(json);
		           System.out.print(json);

		          
		            
		       } catch (Exception e) {
		            e.printStackTrace();
		          
		           
		        }
		 }
//		 res.stream().forEach((mydata)->{
//			 try {
////	            // Convert POJO to JSON string
//	          String json = objectMapper.writeValueAsString(mydata);
//	          
//	          resList.add(json);
//	           System.out.print(json);
//
//	          
//	            
//	       } catch (Exception e) {
//	            e.printStackTrace();
//	          
//	           
//	        }
//		 });
//		 String jsonString;
//		try {
//			jsonString = objectMapper.writeValueAsString(res);
//			resList = objectMapper.readValue(jsonString, new TypeReference<List<String>>() {});
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	
		 
		 int startItem = pageNumber * pageSize;
         int endItem = Math.min(startItem + pageSize, resList.size());
         if (startItem > resList.size()) {

             return new PaginatedResultDTO<>(Collections.emptyList(), 0);
         }
         int totalPage=resList.size();
         resList = resList.subList(startItem, endItem);
         return new PaginatedResultDTO<>(resList, totalPage);
	}
	@Override
	public List<String> searchedAssets(String companyId, String data, String field) {
		// TODO Auto-generated method stub
		List<Map<String,String> > mapList=new ArrayList<>();
		PaginatedResultDTO<String> myList=getAllAssetDetails(companyId);
		ObjectMapper objectMapper = new ObjectMapper();
		myList.getData().stream().forEach((asset)->{
			Map<String,String> m=new HashMap<>();
			 try {
		            // Convert JSON string to Map<String, String>
		            m= objectMapper.readValue(asset, new TypeReference<Map<String, String>>() {});
		            mapList.add(m);
		           
		        } catch (Exception e) {
		            // Handle exception
		            e.printStackTrace();
		          
		        }
		});
//		//System.out.println(field);
		List<Map<String, String>> res= mapList.stream().filter(m->{
			//System.out.println("---->"+m.get(field));
			String s=m.get(field);
//			return s.contains(data);
			return s.toLowerCase().contains(data.toLowerCase());
				
			
		
			}).collect(Collectors.toList());
		 //System.out.println(res);
		 //System.out.println(res.size());
		 List<String> resList=new ArrayList<>();
		 res.stream().forEach((mydata)->{
			 try {
//	            // Convert POJO to JSON string
	          String json = objectMapper.writeValueAsString(mydata);
	          
	          resList.add(json);
	           System.out.print(json);

	          
	            
	       } catch (Exception e) {
	            e.printStackTrace();
	          
	           
	        }
		 });
		 return resList;
	}
	@Override
	public PaginatedResultDTO<String> getAssetsDetailsByCustomerId(String customerId,Integer pageNumber) {
		// TODO Auto-generated method stub
	
		List<Assets> assetsList=assetsRepository.findByCustomerId(customerId);
		List<String> assetsDTOList=new ArrayList<String>();
//		assetsList.stream().forEach(x->{
//			AssetsDTO assetsDTO=modelMapper.map(x, AssetsDTO.class);
//			assetsDTOList.add(assetsDTO.toString());
//			
//		});
		Map<String,String> m=new HashMap<>();
		for(int i=0;i<assetsList.size();i++) {
			AssetsDTO assetsDTO=modelMapper.map(assetsList.get(i), AssetsDTO.class);
			m.put("id", assetsDTO.getId());
			m.put("image",assetsDTO.getImage());
			m.put("email",assetsDTO.getEmail());
			m.put("name",assetsDTO.getName());
			m.put("assetId",assetsDTO.getAssetId().toString());
			m.put("companyId",assetsDTO.getCompanyId());
			m.put("serialNumber",assetsDTO.getSerialNumber());
			m.put("category",assetsDTO.getCategory());
			m.put("customer",assetsDTO.getCustomer());
			m.put("customerId",assetsDTO.getCustomerId());
			m.put("location",assetsDTO.getLocation());
			m.put("status",assetsDTO.getStatus());
			m.put("updatedAt",assetsDTO.getUpdatedAt());
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				String json = objectMapper.writeValueAsString(m);
				assetsDTOList.add(json);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		  int startItem = pageNumber * 5;
          int endItem = Math.min(startItem + 5, assetsDTOList.size());
          if (startItem > assetsDTOList.size()) {

              return new PaginatedResultDTO<>( Collections.emptyList(), 0);
          }
          int totalPage=assetsDTOList.size();
          assetsDTOList = assetsDTOList.subList(startItem, endItem);
		//System.out.println("Asset by cutomer"+assetsDTOList.size());
          return new PaginatedResultDTO<>(assetsDTOList, totalPage);
	}
	@Override
	public void updateAssetsWithInActive(String customerId) {
		// TODO Auto-generated method stub
		List<Assets> assetsList=assetsRepository.findByCustomerId(customerId);
		
		assetsList.stream().forEach(x->{
			x.setStatus("inActive");
			assetsRepository.save(x);
			
		});
		
		
	}
	@Override
	public PaginatedResultDTO<String> advanceFilter(Object filter, int pageNumber, int pageSize,String sortField,String searchData,Boolean asc) {
		// TODO Auto-generated method stub
		 List<String> filteredAssetsWithAllFields=new ArrayList<>();
		 long totalPage=0;
		 System.out.println("----searchData--->"+searchData+"---"+searchData.length());
//		 if(searchData=="null"){
//			 searchData="";
//		 }

		 if (filter instanceof Map) {
	            // Cast the filter to a Map
	            Map<?, ?> filterMap = (Map<?, ?>) filter;
	            
	            // Get all keys
	            Set<?> keys = filterMap.keySet();
	            Map<String,String> mapping=new HashMap<String,String>();
	            
	            // Print keys or do something with them
	            
	            for (Map.Entry<?, ?> entry : filterMap.entrySet()) {
	                Object key = entry.getKey();
	                Object value = entry.getValue();
//	                //System.out.println("Key: " + key + ", Value: " + value);
	                if(value!=null) { mapping.put(key.toString(), value.toString());}
	               
	            }
	            PaginatedResultDTO<String> assetsWithAllFields=getAllAssetDetails(mapping.get("companyId"));
	           

	            
	            
	            filteredAssetsWithAllFields = assetsWithAllFields.getData().stream().filter(data -> {
	                ObjectMapper mapper = new ObjectMapper();
	                int flag = 1;
	                try {
	                    Map<String, String> map = mapper.readValue(data, Map.class);
	                    for (Map.Entry<?, ?> entry : filterMap.entrySet()) {
	                        Object key = entry.getKey();
	                        Object value = entry.getValue();
	                        if (value != null) {
	                            mapping.put(key.toString(), value.toString());
	                            String myValue = map.get(key);
	                            String expectedValue = value.toString();
	                            String keyString = key.toString();
	                            if (!keyString.equals("companyId") && myValue != null && value != null && !value.toString().isEmpty()) {
	                                myValue = myValue.toLowerCase();
	                                if (!myValue.contains(expectedValue.toLowerCase()) && !myValue.equals(expectedValue.toLowerCase())) {
	                                    flag = 0;
	                                }
	                            }
	                        }
	                    }
	                    if (flag == 1) {
	                        return true;
	                    }
	                } catch (JsonMappingException e) {
	                    e.printStackTrace();
	                } catch (JsonProcessingException e) {
	                    e.printStackTrace();
	                }
	                return false;
	            }).collect(Collectors.toList());
	            
//	            Sorting if it is enabled
	            
	            System.out.println("Sort-"+sortField);
			 System.out.println("Search-"+searchData);

	            if(sortField!=null&&(sortField.equals("")==false)) {
	            	 System.out.println("going inside-"+sortField );
	            	Comparator<String> customComparator=null;
	            	ObjectMapper objectMapper = new ObjectMapper();
	    			customComparator = Comparator.comparing(data->{
	    				String jsonString=(String) data;
	    			
	    		        Map<String, String> myMap=new HashMap<>();
						try {
							myMap = objectMapper.readValue(jsonString, new TypeReference<Map<String, String>>() {});
						} catch (JsonMappingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (JsonProcessingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	    		        return myMap.get(sortField);
	    			},String.CASE_INSENSITIVE_ORDER);
					if(asc==true){
						filteredAssetsWithAllFields= filteredAssetsWithAllFields.stream().sorted(customComparator).collect(Collectors.toList());
					}
					else{
						filteredAssetsWithAllFields= filteredAssetsWithAllFields.stream().sorted(customComparator.reversed()).collect(Collectors.toList());
					}

	            	
	            }


	            System.out.println("TOTAL__LENGTH___"+filteredAssetsWithAllFields.size());
				String mySearchData;
				if(searchData.equals("null")){
					mySearchData="";
				}
				else{
					mySearchData=searchData;
				}
	            if(mySearchData!=null&&mySearchData!=""){
					System.out.println("---------->"+mySearchData);
					filteredAssetsWithAllFields=filteredAssetsWithAllFields.stream().filter((data)->
						data.toLowerCase().contains(mySearchData.toLowerCase())
					).collect(Collectors.toList());
				}
	            
	            int startItem = pageNumber * pageSize;
	            int endItem = Math.min(startItem + pageSize, filteredAssetsWithAllFields.size());
	            if (startItem > filteredAssetsWithAllFields.size()) {

	                return new PaginatedResultDTO<>( Collections.emptyList(), 0);
	            }
	            totalPage=filteredAssetsWithAllFields.size();
	            filteredAssetsWithAllFields = filteredAssetsWithAllFields.subList(startItem, endItem);
	            
	           
	           
	        } else {
	            System.out.println("The filter is not a Map instance");
	        }
		 
//		//System.out.println(filteredAssetsWithAllFields.size());
		//System.out.println("----------"+filteredAssetsWithAllFields.size());
		 return new PaginatedResultDTO<>(filteredAssetsWithAllFields, totalPage);

		
	}

	@Override
	public CheckInCheckOutCountDTO checkInCheckOut(String companyId) {
		List<Assets> assetsList= assetsRepository.findByCompanyId(companyId);
		AtomicReference<Integer> checkIn = new AtomicReference<>(0);
		AtomicReference<Integer> checkOut = new AtomicReference<>(0);
		List<AssetCheckInOutDTO> assetCheckInOutDTOList=new ArrayList<>();
		assetsList.stream().forEach((data)->{
			Optional<AssetCheckInOut> assetCheckInOutDTO=checkInOutRepository.findByAssetId(data.getId());
			if(assetCheckInOutDTO.isPresent()&&assetCheckInOutDTO.get().getStatus().equals("Checked Out")){
				checkIn.getAndSet(checkIn.get() + 1);
			}
			else{
				checkOut.getAndSet(checkOut.get() + 1);
			}
//			assetCheckInOutDTOmyList.forEach((checkList)->{
//
//				checkList.getDetailsList().forEach((ele)->{
//					if(ele.getStatus().equals("Checked Out")){
//						checkIn.getAndSet(checkIn.get() + 1);
//					}
//					else{
//						checkOut.getAndSet(checkOut.get() + 1);
//					}
//
//				});
//			});

		});
		System.out.println("===>"+checkIn.get()+" "+checkOut.get());
		CheckInCheckOutCountDTO checkInCheckOutCountDTO=new CheckInCheckOutCountDTO();
		checkInCheckOutCountDTO.setCheckIn(checkIn.get());
		checkInCheckOutCountDTO.setCheckOut(checkOut.get());
		return  checkInCheckOutCountDTO;



	}

	@Override
	public List<AssetsDTO> assetListFromSerialNumber(String companyId, String serialNumber) {
		List<AssetsDTO> assetList=assetsRepository.findByCompanyIdAndSerialNumber(companyId,serialNumber);
		return assetList;

	}

	@Override
	public List<AssetCheckInOut> filterByCheckedInOut(String companyId, Boolean checkedIn) {

			List<AssetCheckInOut> assetCheckInOutDTOmyList=checkInOutRepository.findByCompanyId(companyId);
			if(checkedIn==true){

				assetCheckInOutDTOmyList=assetCheckInOutDTOmyList.stream().filter(data-> data.getStatus().toLowerCase().equals("checked in")).toList();
			}
			else{

				assetCheckInOutDTOmyList=assetCheckInOutDTOmyList.stream().filter(data-> data.getStatus().toLowerCase().equals("checked out")).toList();
			}


		return assetCheckInOutDTOmyList;
	}
	@Override
	public void addCategory(CategoryDTO categoryDTO) throws CategoryException {
		AssetCategory category=modelMapper.map(categoryDTO, AssetCategory.class);
		Optional<AssetCategory> optionalAssetCategory=assetCategoryRepository.findByName(categoryDTO.getName());
		if(optionalAssetCategory.isEmpty()){
			assetCategoryRepository.save(category);
		}
		else{
			throw new CategoryException("Category Already Present");
		}


	}

	@Override
	public void updateCategory(CategoryDTO categoryDTO) {
		AssetCategory category=modelMapper.map(categoryDTO, AssetCategory.class);

			assetCategoryRepository.save(category);

	}

	@Override
	public List<AssetCategory> getCategoryList(String companyId) {

		List<AssetCategory> categoryList= assetCategoryRepository.findByCompanyId(companyId);

		return categoryList;

	}

	@Override
	public void deleteCategory(String id) {
		Optional<AssetCategory> category=assetCategoryRepository.findById(id);
		if(category.isPresent()){
			assetCategoryRepository.delete(category.get());
		}
	}

	@Override
	public AssetCategory getCategoryListById(String companyId, String id) {
		Optional<AssetCategory> categoryOptional= assetCategoryRepository.findById(id);
		return categoryOptional.orElse(null);
	}
	public List<AssetCategory> getActiveCategoryList(String companyId) {
		List<AssetCategory> categoryList= assetCategoryRepository.findByCompanyIdAndStatus(companyId,"active");
		return categoryList;
	}

	@Override
	public List<AssetsDTO> getActiveAssets(String companyId) {
		List<Assets> assetList=assetsRepository.findByCompanyId(companyId);
		List<AssetsDTO> filteredList=new ArrayList<>();
		assetList.stream().forEach((ele)->{
			if(ele.getStatus().equals("active")){
				AssetsDTO assetsDTO=modelMapper.map(ele,AssetsDTO.class);

				filteredList.add(assetsDTO);
			}
		});
		return filteredList;

	}

	@Override
	public Map<String,List<AssetsDTO>> getAssetByCategory(String companyId) {
		List<Assets> assetList=assetsRepository.findByCompanyId(companyId);
		List<AssetCategory> assetCategoryList=assetCategoryRepository.findByCompanyId(companyId);
		Map<String,List<AssetsDTO>> categoryAssetMap=new HashMap<>();
		List<AssetsDTO> filteredList=new ArrayList<>();
		assetCategoryList.forEach((category)->{
			assetList.forEach((asset)->{
				if(asset.getCategory().equals(category.getName())) {
					if (categoryAssetMap.containsKey(category.getName())) {
						List<AssetsDTO> assetsDTOList = categoryAssetMap.get(category.getName());
						AssetsDTO assetsDTO = modelMapper.map(asset, AssetsDTO.class);
						assetsDTOList.add(assetsDTO);
						categoryAssetMap.put(category.getName(), assetsDTOList);
					} else {
						AssetsDTO assetsDTO = modelMapper.map(asset, AssetsDTO.class);
						List<AssetsDTO> assetsDTOList = new ArrayList<>();
						assetsDTOList.add(assetsDTO);
						categoryAssetMap.put(category.getName(), assetsDTOList);
					}
				}
			});

        });
		return categoryAssetMap;
	}

}
