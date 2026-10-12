package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.service.ReelerAuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auction/reeler")
public class ReelerAuctionController {

    @Autowired
    ReelerAuctionService reelerAuctionService;

    @PostMapping("/getHighestBidPerLotDetails")
    public ResponseEntity<?> getHighestBidDetails(@RequestBody LotStatusRequest lotStatusRequest){
        return reelerAuctionService.getHighestBidPerLotDetails(lotStatusRequest);

    }


    @PostMapping("/acceptReelerBidForGivenLot")
    public ResponseEntity<?> acceptReelerBidForGivenLot(@RequestBody ReelerBidAcceptRequest reelerBidAcceptRequest){
        return reelerAuctionService.acceptReelerBidForGivenLot(reelerBidAcceptRequest);

    }

}
