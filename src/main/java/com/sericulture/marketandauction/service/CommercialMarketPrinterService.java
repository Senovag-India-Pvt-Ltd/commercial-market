package com.sericulture.marketandauction.service;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.helper.CommercialMarketHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.CommercialMarketForPrintRequest;
import com.sericulture.marketandauction.model.api.marketauction.CommercialMarketForPrintResponse;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.repository.BinRepository;
import com.sericulture.marketandauction.repository.LotRepository;
import com.sericulture.marketandauction.repository.LotWeightDetailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class CommercialMarketPrinterService {

    @Autowired
    LotRepository lotRepository;

    @Autowired
    LotWeightDetailRepository lotWeightDetailRepository;

    @Autowired
    BinRepository binRepository;

    @Autowired
    CommercialMarketHelper commercialMarketHelper;


    public ResponseEntity<?> getPrintableDataForLot(CommercialMarketForPrintRequest commercialMarketForPrintRequest) {

        JwtPayloadData token = commercialMarketHelper.getAuthToken(commercialMarketForPrintRequest);

        ResponseWrapper rw = ResponseWrapper.createWrapper(CommercialMarketForPrintResponse.class);
        CommercialMarketForPrintResponse commercialMarketForPrintResponse = null;
        Object[][] lotDetails = lotRepository.getAcceptedLotDetails(commercialMarketForPrintRequest.getAuctionDate(), commercialMarketForPrintRequest.getMarketId(), commercialMarketForPrintRequest.getAllottedLotId());
        float reelerCurrentBalance = 0;
        boolean foundAcceptedLot = false;
        if (lotDetails != null && lotDetails.length > 0) {
            foundAcceptedLot = true;
            reelerCurrentBalance = Util.objectToFloat(lotDetails[0][33]);

        } else {
            lotDetails = lotRepository.getNewlyCreatedLotDetails(commercialMarketForPrintRequest.getAuctionDate(), commercialMarketForPrintRequest.getMarketId(), commercialMarketForPrintRequest.getAllottedLotId());
        }
        if (foundAcceptedLot || (lotDetails != null && lotDetails.length > 0)) {
            for (Object[] response : lotDetails) {
                if(LotStatus.CANCELLED.getLabel().equals(Util.objectToString(response[18]))){
                    return commercialMarketHelper.retrunIfError(rw,"Lot is cancelled and hence cannot be printed");
                }
                BigInteger lotId = BigInteger.valueOf(Long.parseLong(String.valueOf(response[16])));
                commercialMarketForPrintResponse = prepareResponseForLotBaseResponse(token, response, lotId);

                if (foundAcceptedLot) {
                    commercialMarketForPrintResponse.setAuctionDateWithTime((Date)(response[24]));
                    commercialMarketForPrintResponse.setReelerLicense(Util.objectToString(response[25]));
                    commercialMarketForPrintResponse.setReelerName(Util.objectToString(response[26]));
                    commercialMarketForPrintResponse.setReelerAddress(Util.objectToString(response[27]));
                    commercialMarketForPrintResponse.setLotWeight(Util.objectToFloat(response[28]));
                    commercialMarketForPrintResponse.setReelerMarketFee(Util.objectToFloat(response[29]));
                    commercialMarketForPrintResponse.setFarmerMarketFee(Util.objectToFloat(response[30]));
                    commercialMarketForPrintResponse.setLotSoldOutAmount(Util.objectToFloat(response[31]));
                    commercialMarketForPrintResponse.setBidAmount(Util.objectToFloat(response[32]));
                    commercialMarketForPrintResponse.setReelerCurrentBalance(reelerCurrentBalance);
                    commercialMarketForPrintResponse.setReelerNameKannada(Util.objectToString(response[34]));
                    commercialMarketForPrintResponse.setReelerMobileNumber(Util.objectToString(response[35]));
                    commercialMarketForPrintResponse.setFruitsId(Util.objectToString(response[38]));
                    commercialMarketForPrintResponse.setReelerNumber(Util.objectToString(response[36]));
                    commercialMarketForPrintResponse.setFarmerAmount(commercialMarketForPrintResponse.getLotSoldOutAmount() - commercialMarketForPrintResponse.getFarmerMarketFee());
                    commercialMarketForPrintResponse.setReelerAmount(commercialMarketForPrintResponse.getLotSoldOutAmount() + commercialMarketForPrintResponse.getReelerMarketFee());



                    List<Float> lotWeightList = lotWeightDetailRepository.findAllByLotId(lotId);
                    if(!Util.isNullOrEmptyList(lotWeightList))
                    commercialMarketForPrintResponse.setLotWeightDetail(lotWeightList);

                }
            }
        } else {
            return commercialMarketHelper.retrunIfError(rw, "No Lot found for given request ");
        }
        rw.setContent(commercialMarketForPrintResponse);
        return ResponseEntity.ok(rw);

    }

    public CommercialMarketForPrintResponse prepareResponseForLotBaseResponse(JwtPayloadData token, Object[] response, BigInteger lotId) {
        CommercialMarketForPrintResponse commercialMarketForPrintResponse;
        commercialMarketForPrintResponse = CommercialMarketForPrintResponse.builder().
                farmerNumber(Util.objectToString(response[0]))
                .farmerFirstName(Util.objectToString(response[1]))
                .farmerMiddleName(Util.objectToString(response[2]))
                .farmerLastName(Util.objectToString(response[3]))
                .farmerAddress(Util.objectToString(response[4]))
                .farmerTaluk(Util.objectToString(response[5]))
                .farmerVillage(Util.objectToString(response[6]))
                .ifscCode(Util.objectToString(response[7]))
                .accountNumber(Util.objectToString(response[8]))
                .allottedLotId(Integer.parseInt(String.valueOf(response[9])))
                .auctionDate(Util.objectToString(response[10]))
                .farmerEstimatedWeight(Util.objectToFloat(response[11]))
                .marketName(Util.objectToString(response[12]))
                .race(Util.objectToString(response[13]))
                .source(Util.objectToString(response[14]))
                .tareWeight(Util.objectToFloat(response[15]))
                .serialNumber(Util.objectToString(response[17])+ lotId)
                .marketNameKannada(Util.objectToString(response[19]))
                .farmerNameKannada(Util.objectToString(response[20]))

                .farmerMobileNumber(Util.objectToString(response[21]))
                .fruitsId(Util.objectToString(response[25]))
                .marketAuctionId((BigDecimal) response[22])
                .fatherNameKan(Util.objectToString(response[23]))
                .auctionDateWithTime((Date)(response[24]))
                .build();
        commercialMarketForPrintResponse.setSmallBinList(binRepository.findAllByMarketAuctionIdAndType(commercialMarketForPrintResponse.getMarketAuctionId().toBigInteger(),"small"));
        commercialMarketForPrintResponse.setBigBinList(binRepository.findAllByMarketAuctionIdAndType(commercialMarketForPrintResponse.getMarketAuctionId().toBigInteger(),"big"));
        commercialMarketForPrintResponse.setLoginName(token.getUsername());
        return commercialMarketForPrintResponse;
    }

}
