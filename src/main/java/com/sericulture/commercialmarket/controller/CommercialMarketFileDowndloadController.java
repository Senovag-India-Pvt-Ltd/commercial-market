package com.sericulture.commercialmarket.controller;

import com.sericulture.commercialmarket.helper.Util;
import com.sericulture.commercialmarket.model.enums.LotStatus;
import com.sericulture.commercialmarket.service.CommercialMarketFileDowndloadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/auction/filedownloader")
public class CommercialMarketFileDowndloadController {

    @Autowired
    CommercialMarketFileDowndloadService commercialMarketFileDowndloadService;


    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> getFile(@RequestParam int marketId, @RequestParam String fileName) {

        InputStreamResource file = new InputStreamResource(commercialMarketFileDowndloadService.generateCSV(marketId,fileName));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName+ Util.getISTLocalDate()+".csv")
                .contentType(MediaType.parseMediaType("application/csv"))
                .body(file);
    }
}
