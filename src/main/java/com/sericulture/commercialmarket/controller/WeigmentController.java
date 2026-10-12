package com.sericulture.commercialmarket.controller;

import com.sericulture.commercialmarket.model.api.marketauction.*;
import com.sericulture.commercialmarket.service.WeigmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auction/weigment")
public class WeigmentController {


    @Autowired
    private WeigmentService weigmentService;

    @PostMapping("/getUpdateWeighmentByLotId")
    public ResponseEntity<?> getLotUpdateWeighmentScreenPrep(@RequestBody LotStatusRequest lotStatusRequest){

       return weigmentService.getWeigmentByLotAndMarketAndAuctionDate(lotStatusRequest);

    }

    @PostMapping("/canContinuetoWeighment")
    public ResponseEntity<?> canContinuetoWeighment(@RequestBody CanContinueToWeighmentRequest canContinueToWeighmentRequest){
        return weigmentService.canContinueToWeighmentProcess(canContinueToWeighmentRequest);
    }

    @PostMapping("/completeWeighmentForLot")
    public ResponseEntity<?> completeWeighMentForLot(@RequestBody  CompleteLotWeighmentRequest completeLotWeighmentRequest){
        return weigmentService.completeLotWeighMent(completeLotWeighmentRequest);
    }

}
