package com.sericulture.marketandauction.controller;

import com.sericulture.marketandauction.helper.MAConstants;
import com.sericulture.marketandauction.model.api.marketauction.*;
import com.sericulture.marketandauction.service.CustomValidator;
import com.sericulture.marketandauction.service.CommercialMarketCancelService;
import com.sericulture.marketandauction.service.CommercialMarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auction")
public class CommercialMarketController {

    @Autowired
    CommercialMarketService commercialMarketService;

    @Autowired
    CommercialMarketCancelService commercialMarketCancelService;

    @Autowired
    CustomValidator customValidator;

    @Operation(summary = "This API allocates bin and generates a lot",
            description = "Majorly creates a record in market auction, bin and lot. This allowing the farmer to participate in the auction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok Response", content = {
                    @Content(mediaType = "application/json", schema =
                    @Schema(example = MAConstants.SUCCESS_LOT_ALLOTMENT_OUTPUT))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request - Has validation errors",
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.VALIDATION_ERROR_LOT_ALLOTMENT))
                            }),
            @ApiResponse(responseCode = "500", description = MAConstants.INTERNAL_SERVER_ERROR_MESSAGE,
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.INTERNAL_SERVER_ERROR_OUTPUT_FORMAT))
                            })
    })
    @PostMapping("/allot")
    public ResponseEntity<?> allotBidToFarmer(@RequestBody CommercialMarketRequest commercialMarketRequest){

        //customValidator.validate(commercialMarketRequest);
        return commercialMarketService.marketAuctionFacade(commercialMarketRequest);

    }

    @Operation(summary = "This API cancels the market auction slip by lot Id ",
            description = "Cancels the lot allocated to the farmer by Id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ok Response", content = {
                    @Content(mediaType = "application/json", schema =
                    @Schema(example = ""))
            }),
            @ApiResponse(responseCode = "400", description = "Bad Request - Has validation errors",
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.VALIDATION_ERROR_LOT_ALLOTMENT))
                            }),
            @ApiResponse(responseCode = "500", description = MAConstants.INTERNAL_SERVER_ERROR_MESSAGE,
                    content =
                            {
                                    @Content(mediaType = "application/json", schema =
                                    @Schema(example = MAConstants.INTERNAL_SERVER_ERROR_OUTPUT_FORMAT))
                            })
    })
    @PostMapping("/cancelLot")
    public ResponseEntity<?> cancelLot(@RequestBody CancelAuctionByLotRequest cancellationRequest){
        return commercialMarketCancelService.cancelLot(cancellationRequest);
    }



}
