package com.quantumai.customer.controller;


import com.quantumai.customer.dto.InspectionCompletedCountPerDayDTO;
import com.quantumai.customer.dto.UserInspectionAnalyticsDTO;
import com.quantumai.customer.entity.CompanyCustomer;
import com.quantumai.customer.entity.CompanyCustomerExtraFieldName;
import com.quantumai.customer.entity.CompanyCustomerExtraFields;
import com.quantumai.customer.entity.enums.InspectionInstanceStatus;
import com.quantumai.customer.service.AssetInspectionService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@CrossOrigin(
        origins = {
                "http://localhost:4200",
                "http://assetyugg.com.s3-website-us-east-1.amazonaws.com"
        },
        allowedHeaders = {"device-id", "Content-Type", "Authorization"}
)
@RestController
@RequestMapping("inspection/")
public class AssetInspectionAPI {

        @Autowired
        private AssetInspectionService assetInspectionService;

        @GetMapping("/user-inspection-analytics/{companyId}")
        @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
        public List<UserInspectionAnalyticsDTO> userInspectionAnalytics(
                @PathVariable Long companyId,
                LocalDate startDate,
                LocalDate endDate) {
                return assetInspectionService.getUserInspectionAnalytics(companyId, startDate, endDate);
        }

        @GetMapping("/status-distribution/{companyId}")
        @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
        public Map<InspectionInstanceStatus, Long> statusDistribution(
                @PathVariable Long companyId,
                LocalDate startDate,
                LocalDate endDate) {
                return assetInspectionService.getStatusDistribution(companyId, startDate, endDate);
        }

        @GetMapping("/inspection-type-completion/{companyId}")
        @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
        public Map<String, Long> inspectionTypeCompletion(
                @PathVariable Long companyId,
                LocalDate startDate,
                LocalDate endDate) {
                return assetInspectionService.getInspectionTypeCompletion(companyId, startDate, endDate);
        }

        @GetMapping("/lead-inspector/{companyId}")
        @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
        public Map<String, Long> leadInspector(
                @PathVariable Long companyId,
                LocalDate startDate,
                LocalDate endDate) {
                return assetInspectionService.getLeadInspector(companyId, startDate, endDate);
        }

        @GetMapping("/inspection-details/{companyId}")
        @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
        public Map<String, Long> inspectionDetails(@PathVariable Long companyId) {
                return assetInspectionService.getAssetInspectionDetails(companyId);
        }

        @GetMapping("/inspection-complete-per-day/{companyId}")
        @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
        public List<InspectionCompletedCountPerDayDTO> inspectionCompletePerDay(
                @PathVariable Long companyId,
                @RequestParam LocalDate startDate,
                @RequestParam LocalDate endDate) {
                return assetInspectionService.getInspectionCompletionPerDay(companyId, startDate, endDate);
        }

        @GetMapping("/inspection-export/{companyId}/{assetId}")
        @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
        public ResponseEntity<byte[]> inspectionDetailedExport(
                @PathVariable Long companyId,
                @PathVariable String assetId) throws Exception {
                return ResponseEntity.ok(assetInspectionService.exportInspectionExcel(companyId, assetId));
        }

}
