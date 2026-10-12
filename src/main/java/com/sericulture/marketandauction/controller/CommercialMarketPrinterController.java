package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.api.marketauction.CommercialMarketForPrintRequest;
import com.sericulture.marketandauction.service.CommercialMarketPrinterService;
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
