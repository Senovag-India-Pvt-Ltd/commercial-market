package com.sericulture.commercialmarket.controller;

import com.sericulture.commercialmarket.model.api.marketauction.DTROnlineReportRequest;
import com.sericulture.commercialmarket.model.api.marketauction.reporting.DashboardReportRequest;
import com.sericulture.commercialmarket.model.api.marketauction.reporting.ReportRequest;
import com.sericulture.commercialmarket.service.CommercialMarketReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auction/report")
public class CommercialMarketReportController {

    @Autowired
    private CommercialMarketReportService commercialMarketReportService;

    @PostMapping("/getLotAndAmount")
    public ResponseEntity<?> getDisplayLotNos(@RequestBody com.sericulture.commercialmarket.model.api.RequestBody requestBody ){
        return commercialMarketReportService.getDisplayLotNos(requestBody);
    }

    @PostMapping("/getDTROnlineReport")
    public ResponseEntity<?> getDTROnlineReport(@RequestBody DTROnlineReportRequest dtrOnlineReportRequest){
        return commercialMarketReportService.getDTROnlineReport(dtrOnlineReportRequest);
    }

    @PostMapping("/getBlankDTROnlineReport")
    public ResponseEntity<?> getBlankDTROnlineReport(@RequestBody DTROnlineReportRequest dtrOnlineReportRequest){
        return commercialMarketReportService.getBlankDTROnlineReport(dtrOnlineReportRequest);
    }

    @PostMapping("/getUnitCounterReport")
    public ResponseEntity<?> getUnitCounterReport(@RequestBody ReportRequest reportRequest){
        return commercialMarketReportService.getUnitCounterReport(reportRequest);
    }

    @PostMapping("/getReelerMFReport")
    public ResponseEntity<?> getReelerMFReport(@RequestBody ReportRequest reportRequest){
        return commercialMarketReportService.getReelerMFReport(reportRequest);
    }

    @PostMapping("/getPendingLotReport")
    public ResponseEntity<?> getPendingLotReport(@RequestBody ReportRequest reportRequest){
        return commercialMarketReportService.getPendingLotReport(reportRequest);
    }

    @PostMapping("/getReelerPendingReport")
    public ResponseEntity<?> getReelerPendingReport(@RequestBody com.sericulture.commercialmarket.model.api.RequestBody requestBody){
        return commercialMarketReportService.getReelerPendingReport(requestBody);
    }

    @PostMapping("/getDashboardReport")
    public ResponseEntity<?> getDashboardReport(@RequestBody DashboardReportRequest reportRequest){
        return commercialMarketReportService.getDashboardReport(reportRequest);
    }

    @PostMapping("/getDashboardReportAllMarket")
    public ResponseEntity<?> getDashboardReportAllMarket(){
        return commercialMarketReportService.getDashboardReportAllMarket();
    }

}
