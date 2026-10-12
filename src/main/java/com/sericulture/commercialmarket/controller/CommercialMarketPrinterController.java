package com.sericulture.commercialmarket.controller;

import com.sericulture.commercialmarket.model.api.marketauction.CommercialMarketForPrintRequest;
import com.sericulture.commercialmarket.service.CommercialMarketPrinterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auction/print")
public class CommercialMarketPrinterController {

    @Autowired
    CommercialMarketPrinterService commercialMarketPrinterService;

    @PostMapping("/getPrintableDataForLot")
    public ResponseEntity<?> getPrintableDataForLot(@RequestBody CommercialMarketForPrintRequest commercialMarketForPrintRequest) {
        return commercialMarketPrinterService.getPrintableDataForLot(commercialMarketForPrintRequest);
    }

}
