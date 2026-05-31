package com.quantumai.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;



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
@Tag(name = "AssetInspection", description = "AssetInspection Management API")
public class AssetInspectionAPI {

        @Autowired
        private AssetInspectionService assetInspectionService;

        @Operation(summary = "User Inspection Analytics", description = "Endpoint to user inspection analytics")
        @GetMapping("/user-inspection-analytics/{companyId}")
        @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
        public List<UserInspectionAnalyticsDTO> userInspectionAnalytics(
                @PathVariable Long companyId,
                @RequestParam LocalDate startDate,
                @RequestParam LocalDate endDate)  {
//                Thread.sleep(3000);
                return assetInspectionService.getUserInspectionAnalytics(companyId, startDate, endDate);
        }

        @Operation(summary = "Status Distribution", description = "Endpoint to status distribution")
        @GetMapping("/status-distribution/{companyId}")
        @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
        public Map<InspectionInstanceStatus, Long> statusDistribution(
                @PathVariable Long companyId,
                @RequestParam LocalDate startDate,
                @RequestParam LocalDate endDate)  {
//                Thread.sleep(3000);
                return assetInspectionService.getStatusDistribution(companyId, startDate, endDate);
        }

        @Operation(summary = "Inspection Type Completion", description = "Endpoint to inspection type completion")
        @GetMapping("/inspection-type-completion/{companyId}")
        @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
        public Map<String, Long> inspectionTypeCompletion(
                @PathVariable Long companyId,
                LocalDate startDate,
                LocalDate endDate) {
                return assetInspectionService.getInspectionTypeCompletion(companyId, startDate, endDate);
        }

        @Operation(summary = "Lead Inspector", description = "Endpoint to lead inspector")
        @GetMapping("/lead-inspector/{companyId}")
        @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
        public Map<String, Long> leadInspector(
                @PathVariable Long companyId,
                LocalDate startDate,
                LocalDate endDate) {
                return assetInspectionService.getLeadInspector(companyId, startDate, endDate);
        }

        @Operation(summary = "Inspection Details", description = "Endpoint to inspection details")
        @GetMapping("/inspection-details/{companyId}")
        @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
        public Map<String, Long> inspectionDetails(@PathVariable Long companyId) {
                return assetInspectionService.getAssetInspectionDetails(companyId);
        }

        @Operation(summary = "Inspection Complete Per Day", description = "Endpoint to inspection complete per day")
        @GetMapping("/inspection-complete-per-day/{companyId}")
        @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
        public List<InspectionCompletedCountPerDayDTO> inspectionCompletePerDay(
                @PathVariable Long companyId,
                @RequestParam LocalDate startDate,
                @RequestParam LocalDate endDate) {
                return assetInspectionService.getInspectionCompletionPerDay(companyId, startDate, endDate);
        }

        @Operation(summary = "Inspection Detailed Export", description = "Endpoint to inspection detailed export")
        @GetMapping("/inspection-detailed-export/{companyId}/{assetId}")
        @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
        public ResponseEntity<byte[]> inspectionDetailedExport(
                @PathVariable Long companyId,
                @PathVariable String assetId) throws Exception {
                return ResponseEntity.ok(assetInspectionService.exportInspectionExcel(companyId, assetId));
        }


        @Operation(summary = "Inspection Overview Export", description = "Endpoint to inspection overview export")
        @GetMapping("/inspection-overview-export/{companyId}/{assetId}")
        public ResponseEntity<byte[]> inspectionOverviewExport(@PathVariable Long companyId,@PathVariable String assetId) throws Exception {
//                return assetInspectionService.getInspectionCompletionPerDay(companyId, startDate, endDate);
                return ResponseEntity.ok(assetInspectionService.exportInspectionOverviewExcel(companyId,assetId));


        }

}
