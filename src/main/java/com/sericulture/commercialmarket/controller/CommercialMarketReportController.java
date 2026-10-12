package com.sericulture.commercialmarket.controller;

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

}
