package com.sericulture.commercialmarket.service;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.commercialmarket.helper.CommercialMarketHelper;
import com.sericulture.commercialmarket.helper.Util;
import com.sericulture.commercialmarket.model.ResponseWrapper;
import com.sericulture.commercialmarket.model.api.RequestBody;
import com.sericulture.commercialmarket.model.api.marketauction.DTROnlineReportRequest;
import com.sericulture.commercialmarket.model.api.marketauction.DTROnlineReportResponse;
import com.sericulture.commercialmarket.model.api.marketauction.DTROnlineReportUnitDetail;
import com.sericulture.commercialmarket.model.api.marketauction.LotHighestBidResponse;
import com.sericulture.commercialmarket.model.api.marketauction.reporting.*;
import com.sericulture.commercialmarket.model.entity.MarketMaster;
import com.sericulture.commercialmarket.model.exceptions.MessageLabelType;
import com.sericulture.commercialmarket.model.exceptions.ValidationException;
import com.sericulture.commercialmarket.model.exceptions.ValidationMessage;
import com.sericulture.commercialmarket.repository.BinRepository;
import com.sericulture.commercialmarket.repository.LotRepository;
import com.sericulture.commercialmarket.repository.MarketMasterRepository;
import com.sericulture.commercialmarket.repository.ReelerAuctionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CommercialMarketReportService {

    @Autowired
    private CommercialMarketHelper commercialMarketHelper;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private MarketMasterRepository marketMasterRepository;

    @Autowired
    private ReelerAuctionRepository reelerAuctionRepository;

    @Autowired
    private BinRepository binRepository;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

//    SP Code For Display Lot Nos
    public ResponseEntity<?> getDisplayLotNos(RequestBody requestBody) {
        log.info("Get Display Lot Nos request for MarketId: " + requestBody.getMarketId() + ", AuctionDate: " + requestBody.getAuctionDate());
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        EntityManager entityManager = null;
        List<LotHighestBidResponse> lotHighestBidResponseList = new ArrayList<>();
        try {
            entityManager = entityManagerFactory.createEntityManager();
            StoredProcedureQuery procedureQuery = entityManager
                    .createStoredProcedureQuery("GET_DISPAY_LOT_NOS");
            procedureQuery.registerStoredProcedureParameter("MarketId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("AuctionDate", LocalDate.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("Error", String.class, ParameterMode.OUT);
            procedureQuery.registerStoredProcedureParameter("Success", Integer.class, ParameterMode.OUT);

            procedureQuery.setParameter("MarketId", requestBody.getMarketId());
            procedureQuery.setParameter("AuctionDate", requestBody.getAuctionDate());
            procedureQuery.execute();

            String error = (String) procedureQuery.getOutputParameterValue("Error");
            Object success = procedureQuery.getOutputParameterValue("Success");
            System.out.println("Out status: " + success);

            List<Object[]> reelerLotHighestAndHisBidList = procedureQuery.getResultList();
            for (Object[] bidDetail : reelerLotHighestAndHisBidList) {
                boolean notFound = true;
                int allottedLot = Integer.parseInt(String.valueOf(bidDetail[0]));
                int bidAmount = Integer.parseInt(String.valueOf(bidDetail[1]));
                String currentTime = String.valueOf(bidDetail[2]);
                LotHighestBidResponse lotHighestBidResponse = new LotHighestBidResponse();
                lotHighestBidResponse.setAllottedLotId(allottedLot);
                lotHighestBidResponse.setHighestBid(bidAmount);
                lotHighestBidResponse.setCurrentTime(currentTime);
                lotHighestBidResponseList.add(lotHighestBidResponse);
            }
            rw.setContent(lotHighestBidResponseList);

            if (StringUtils.isNotEmpty(error)) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(), error, "-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }
        } catch (Exception ex) {
            if (ex instanceof ValidationException) {
                throw ex;
            }
            log.error("Error While fetching display lot nos for MarketId: " + requestBody.getMarketId() + ", AuctionDate: " + requestBody.getAuctionDate() + " error id: " + ex);
            return commercialMarketHelper.retrunIfError(rw, "error occurred while fetching display lot nos.");
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getDTROnlineReport(DTROnlineReportRequest dtrOnlineReportRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<Integer> reelerIdList = null;
        if (dtrOnlineReportRequest.getReelerId() > 0) {
            reelerIdList = List.of(dtrOnlineReportRequest.getReelerId());
        }
        List<Object[]> reportPaymentSuccessResponse = lotRepository.
                getPaymentSuccessLots(dtrOnlineReportRequest.getMarketId(), dtrOnlineReportRequest.getFromDate(), dtrOnlineReportRequest.getToDate(), reelerIdList);

        MarketMaster marketMaster = marketMasterRepository.findById(dtrOnlineReportRequest.getMarketId());
        List<Object[]> reportResponse;
        if(marketMaster.getPaymentMode().equals("cash")){
            reportResponse = lotRepository.
                    getDTROnlineReportForCash(dtrOnlineReportRequest.getMarketId(), dtrOnlineReportRequest.getFromDate(), dtrOnlineReportRequest.getToDate(), reelerIdList);
        }else{
            reportResponse = lotRepository.
                    getDTROnlineReport(dtrOnlineReportRequest.getMarketId(), dtrOnlineReportRequest.getFromDate(), dtrOnlineReportRequest.getToDate(), reelerIdList);
        }

        DTROnlineReportResponse dtrOnlineReportResponse = new DTROnlineReportResponse();
        if(reportResponse.size()>0) {
            dtrOnlineReportResponse.setMarketNameKannada(Util.objectToString(reportResponse.get(0)[19]));
        }
        prepareDTROnlineInfo(dtrOnlineReportResponse, reportResponse);
        if(reportPaymentSuccessResponse.size()>0) {
            dtrOnlineReportResponse.setPaymentSuccessLots(Util.objectToInteger(reportPaymentSuccessResponse.get(0)[0]));
        }

        int totalLots = dtrOnlineReportResponse.getTotalLots();
        int paymentSuccessLots = dtrOnlineReportResponse.getPaymentSuccessLots();
        int notTransactedLots = totalLots - paymentSuccessLots;

        dtrOnlineReportResponse.setTotalLots(totalLots);
        dtrOnlineReportResponse.setNotTransactedLots(notTransactedLots);

        rw.setContent(dtrOnlineReportResponse);
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getBlankDTROnlineReport(DTROnlineReportRequest dtrOnlineReportRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<Integer> reelerIdList = null;
        if (dtrOnlineReportRequest.getReelerId() > 0) {
            reelerIdList = List.of(dtrOnlineReportRequest.getReelerId());
        }
        List<Object[]> reportPaymentSuccessResponse = lotRepository.
                getPaymentSuccessLotsForBlankReport(dtrOnlineReportRequest.getMarketId(), dtrOnlineReportRequest.getFromDate(), dtrOnlineReportRequest.getToDate(), reelerIdList);

        MarketMaster marketMaster = marketMasterRepository.findById(dtrOnlineReportRequest.getMarketId());
        List<Object[]> reportResponse;
        if(marketMaster.getPaymentMode().equals("cash")){
            reportResponse = lotRepository.
                    getDTROnlineReportForCashForBlankReport(dtrOnlineReportRequest.getMarketId(), dtrOnlineReportRequest.getFromDate(), dtrOnlineReportRequest.getToDate(), reelerIdList);
        }else{
            reportResponse = lotRepository.
                    getDTROnlineReportForCashForBlankReport(dtrOnlineReportRequest.getMarketId(), dtrOnlineReportRequest.getFromDate(), dtrOnlineReportRequest.getToDate(), reelerIdList);
        }

        DTROnlineReportResponse dtrOnlineReportResponse = new DTROnlineReportResponse();
        if(reportResponse.size()>0) {
            dtrOnlineReportResponse.setMarketNameKannada(Util.objectToString(reportResponse.get(0)[19]));
        }
        prepareDTROnlineInfoForBlankReport(dtrOnlineReportResponse, reportResponse);
        if(reportPaymentSuccessResponse.size()>0) {
            dtrOnlineReportResponse.setPaymentSuccessLots(Util.objectToInteger(reportPaymentSuccessResponse.get(0)[0]));
        }

        int totalLots = dtrOnlineReportResponse.getTotalLots();
        int paymentSuccessLots = dtrOnlineReportResponse.getPaymentSuccessLots();
        int notTransactedLots = totalLots - paymentSuccessLots;

        dtrOnlineReportResponse.setTotalLots(totalLots);
        dtrOnlineReportResponse.setNotTransactedLots(notTransactedLots);

        rw.setContent(dtrOnlineReportResponse);
        return ResponseEntity.ok(rw);
    }

    private void prepareDTROnlineInfo(DTROnlineReportResponse dtrOnlineReportResponse, List<Object[]> queryResponse) {
        Long minAmount = Long.MAX_VALUE;
        Long maxAmount = Long.MIN_VALUE;
        Float totalLotSoldOutAmount = 0.0f;
        Float totalWeight = 0.0f;

        for (Object[] unit : queryResponse) {
            DTROnlineReportUnitDetail dtrOnlineReportUnitDetail = DTROnlineReportUnitDetail.builder()
                    .serialNumber(Util.objectToInteger(unit[0]))
                    .allottedLotId(Util.objectToInteger(unit[1]))
                    .farmerFirstName(Util.objectToString(unit[2]))
                    .farmerMiddleName(Util.objectToString(unit[3]))
                    .farmerLastName(Util.objectToString(unit[4]))
                    .farmerNumber(Util.objectToString(unit[5]))
                    .farmerMobileNumber(Util.objectToString(unit[6]))
                    .weight(Util.objectToFloat(unit[7]))
                    .bidAmount(Util.objectToInteger(unit[8]))
                    .lotSoldOutAmount(Math.round(Util.objectToFloat(unit[9]) * 100.0f) / 100.0f)
                    .farmerMarketFee(Math.round(Util.objectToFloat(unit[10]) * 100.0f) / 100.0f)
                    .reelerMarketFee(Math.round(Util.objectToFloat(unit[11]) * 100.0f)/100.0f)
                    .reelerLicense(Util.objectToString(unit[12]))
                    .reelerName(Util.objectToString(unit[13]))
                    .reelerMobile(Util.objectToString(unit[14]))
                    .bankName(Util.objectToString(unit[15]))
                    .branchName(Util.objectToString(unit[16]))
                    .ifscCode(Util.objectToString(unit[17]))
                    .accountNumber(Util.objectToString(unit[18]))
                    .farmerAddress(Util.objectToString(unit[20]))
                    .auctionDate(((java.sql.Date) unit[21]).toLocalDate())
                    .farmerTaluk(Util.objectToString(unit[22]))
                    .farmerVillage(Util.objectToString(unit[23]))
                    .raceName(Util.objectToString(unit[24]))
                    .cocoonAge(Util.objectToLong(unit[25]))
                    .farmerNameKannada(Util.objectToString(unit[26]))
                    .fatherNameKannada(Util.objectToString(unit[27]))
                    .talukNameInKannada(Util.objectToString(unit[28]))
                    .villageNameInKannada(Util.objectToString(unit[29]))
                    .minAmount(Util.objectToLong(unit[30]))
                    .maxAmount(Util.objectToLong(unit[31]))
                    .avgAmount(Math.round(Util.objectToFloat(unit[32])))
                    .build();
            dtrOnlineReportUnitDetail.setReelerAmount(Math.round((dtrOnlineReportUnitDetail.getLotSoldOutAmount() + dtrOnlineReportUnitDetail.getReelerMarketFee()) * 100.0f) / 100.0f);
            dtrOnlineReportUnitDetail.setFarmerAmount(Math.round((dtrOnlineReportUnitDetail.getLotSoldOutAmount() - dtrOnlineReportUnitDetail.getFarmerMarketFee()) * 100.0f) / 100.0f);
            dtrOnlineReportResponse.setTotalFarmerMarketFee(Math.round((dtrOnlineReportResponse.getTotalFarmerMarketFee() + dtrOnlineReportUnitDetail.getFarmerMarketFee()) * 100.0f) / 100.0f);
            dtrOnlineReportResponse.setTotalReelerMarketFee(Math.round((dtrOnlineReportResponse.getTotalReelerMarketFee() + dtrOnlineReportUnitDetail.getReelerMarketFee()) * 100.0f) / 100.0f);
            dtrOnlineReportResponse.setTotalFarmerAmount(Math.round((dtrOnlineReportResponse.getTotalFarmerAmount() + dtrOnlineReportUnitDetail.getFarmerAmount()) * 100.0f) / 100.0f);
            dtrOnlineReportResponse.setTotalReelerAmount(Math.round((dtrOnlineReportResponse.getTotalReelerAmount() + dtrOnlineReportUnitDetail.getReelerAmount()) * 100.0f) / 100.0f);
            dtrOnlineReportResponse.setTotallotSoldOutAmount(Math.round((dtrOnlineReportResponse.getTotallotSoldOutAmount() + dtrOnlineReportUnitDetail.getLotSoldOutAmount()) * 100.0f) / 100.0f);
            dtrOnlineReportResponse.setTotalWeight(dtrOnlineReportResponse.getTotalWeight() + dtrOnlineReportUnitDetail.getWeight());
            dtrOnlineReportResponse.getDtrOnlineReportUnitDetailList().add(dtrOnlineReportUnitDetail);

            if (dtrOnlineReportUnitDetail.getMaxAmount() != null) {
                maxAmount = Math.max(maxAmount, dtrOnlineReportUnitDetail.getMaxAmount());
            }
            if (dtrOnlineReportUnitDetail.getMinAmount() != null && dtrOnlineReportUnitDetail.getMinAmount() != 0 ) {
                minAmount = Math.min(minAmount, dtrOnlineReportUnitDetail.getMinAmount());
            }

            totalLotSoldOutAmount += dtrOnlineReportUnitDetail.getLotSoldOutAmount();
            totalWeight += dtrOnlineReportUnitDetail.getWeight();
        }

        dtrOnlineReportResponse.setMinAmount(minAmount.equals(Long.MAX_VALUE) ? null : minAmount);
        dtrOnlineReportResponse.setMaxAmount(maxAmount.equals(Long.MIN_VALUE) ? null : maxAmount);

        if (totalWeight > 0) {
            dtrOnlineReportResponse.setAvgAmount(totalLotSoldOutAmount / totalWeight);
        } else {
            dtrOnlineReportResponse.setAvgAmount(0.0f);
        }

        dtrOnlineReportResponse.setTotalLots(queryResponse.size());
    }

    private void prepareDTROnlineInfoForBlankReport(DTROnlineReportResponse dtrOnlineReportResponse, List<Object[]> queryResponse) {
        Long minAmount = Long.MAX_VALUE;
        Long maxAmount = Long.MIN_VALUE;
        Float totalLotSoldOutAmount = 0.0f;
        Float totalWeight = 0.0f;

        for (Object[] unit : queryResponse) {
            DTROnlineReportUnitDetail dtrOnlineReportUnitDetail = DTROnlineReportUnitDetail.builder()
                    .serialNumber(Util.objectToInteger(unit[0]))
                    .allottedLotId(Util.objectToInteger(unit[1]))
                    .farmerFirstName(Util.objectToString(unit[2]))
                    .farmerMiddleName(Util.objectToString(unit[3]))
                    .farmerLastName(Util.objectToString(unit[4]))
                    .farmerNumber(Util.objectToString(unit[5]))
                    .farmerMobileNumber(Util.objectToString(unit[6]))
                    .weight(Util.objectToFloat(unit[7]))
                    .bidAmount(Util.objectToInteger(unit[8]))
                    .lotSoldOutAmount(Math.round(Util.objectToFloat(unit[9]) * 100.0f) / 100.0f)
                    .farmerMarketFee(Math.round(Util.objectToFloat(unit[10]) * 100.0f) / 100.0f)
                    .reelerMarketFee(Math.round(Util.objectToFloat(unit[11]) * 100.0f)/100.0f)
                    .reelerLicense(Util.objectToString(unit[12]))
                    .reelerName(Util.objectToString(unit[13]))
                    .reelerMobile(Util.objectToString(unit[14]))
                    .bankName(Util.objectToString(unit[15]))
                    .branchName(Util.objectToString(unit[16]))
                    .ifscCode(Util.objectToString(unit[17]))
                    .accountNumber(Util.objectToString(unit[18]))
                    .farmerAddress(Util.objectToString(unit[20]))
                    .auctionDate(((java.sql.Date) unit[21]).toLocalDate())
                    .farmerTaluk(Util.objectToString(unit[22]))
                    .farmerVillage(Util.objectToString(unit[23]))
                    .raceName(Util.objectToString(unit[24]))
                    .cocoonAge(Util.objectToLong(unit[25]))
                    .farmerNameKannada(Util.objectToString(unit[26]))
                    .fatherNameKannada(Util.objectToString(unit[27]))
                    .talukNameInKannada(Util.objectToString(unit[28]))
                    .villageNameInKannada(Util.objectToString(unit[29]))
                    .minAmount(Util.objectToLong(unit[30]))
                    .maxAmount(Util.objectToLong(unit[31]))
                    .avgAmount(Math.round(Util.objectToFloat(unit[32])))
                    .build();
            dtrOnlineReportUnitDetail.setReelerAmount(Math.round((dtrOnlineReportUnitDetail.getLotSoldOutAmount() + dtrOnlineReportUnitDetail.getReelerMarketFee()) * 100.0f) / 100.0f);
            dtrOnlineReportUnitDetail.setFarmerAmount(Math.round((dtrOnlineReportUnitDetail.getLotSoldOutAmount() - dtrOnlineReportUnitDetail.getFarmerMarketFee()) * 100.0f) / 100.0f);
            dtrOnlineReportResponse.setTotalFarmerMarketFee(Math.round((dtrOnlineReportResponse.getTotalFarmerMarketFee() + dtrOnlineReportUnitDetail.getFarmerMarketFee()) * 100.0f) / 100.0f);
            dtrOnlineReportResponse.setTotalReelerMarketFee(Math.round((dtrOnlineReportResponse.getTotalReelerMarketFee() + dtrOnlineReportUnitDetail.getReelerMarketFee()) * 100.0f) / 100.0f);
            dtrOnlineReportResponse.setTotalFarmerAmount(Math.round((dtrOnlineReportResponse.getTotalFarmerAmount() + dtrOnlineReportUnitDetail.getFarmerAmount()) * 100.0f) / 100.0f);
            dtrOnlineReportResponse.setTotalReelerAmount(Math.round((dtrOnlineReportResponse.getTotalReelerAmount() + dtrOnlineReportUnitDetail.getReelerAmount()) * 100.0f) / 100.0f);
            dtrOnlineReportResponse.setTotallotSoldOutAmount(Math.round((dtrOnlineReportResponse.getTotallotSoldOutAmount() + dtrOnlineReportUnitDetail.getLotSoldOutAmount()) * 100.0f) / 100.0f);
            dtrOnlineReportResponse.setTotalWeight(dtrOnlineReportResponse.getTotalWeight() + dtrOnlineReportUnitDetail.getWeight());
            dtrOnlineReportResponse.getDtrOnlineReportUnitDetailList().add(dtrOnlineReportUnitDetail);

            if (dtrOnlineReportUnitDetail.getMaxAmount() != null) {
                maxAmount = Math.max(maxAmount, dtrOnlineReportUnitDetail.getMaxAmount());
            }
            if (dtrOnlineReportUnitDetail.getMinAmount() != null && dtrOnlineReportUnitDetail.getMinAmount() != 0 ) {
                minAmount = Math.min(minAmount, dtrOnlineReportUnitDetail.getMinAmount());
            }

            totalLotSoldOutAmount += dtrOnlineReportUnitDetail.getLotSoldOutAmount();
            totalWeight += dtrOnlineReportUnitDetail.getWeight();
        }

        dtrOnlineReportResponse.setMinAmount(minAmount.equals(Long.MAX_VALUE) ? null : minAmount);
        dtrOnlineReportResponse.setMaxAmount(maxAmount.equals(Long.MIN_VALUE) ? null : maxAmount);

        if (totalWeight > 0) {
            dtrOnlineReportResponse.setAvgAmount(totalLotSoldOutAmount / totalWeight);
        } else {
            dtrOnlineReportResponse.setAvgAmount(0.0f);
        }

        dtrOnlineReportResponse.setTotalLots(queryResponse.size());
    }

    public ResponseEntity<?> getUnitCounterReport(ReportRequest reportRequest) {
        List<UnitCounterReportResponse> unitCounterReportResponses = new ArrayList<>();
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        List<Object[]> resultSet;
        if (reportRequest.getReelerNumber() == null || reportRequest.getReelerNumber().isEmpty()) {
            resultSet = reelerAuctionRepository.getUnitCounterReportWithoutReelerNumber(
                    reportRequest.getFromDate(), reportRequest.getToDate(),
                    reportRequest.getMarketId());
        } else {
            resultSet = reelerAuctionRepository.getUnitCounterReport(
                    reportRequest.getFromDate(), reportRequest.getToDate(),
                    reportRequest.getMarketId(), reportRequest.getReelerNumber());
        }

        if (Util.isNullOrEmptyList(resultSet)) {
            commercialMarketHelper.retrunIfError(rw, "No data found");
            return ResponseEntity.ok(rw);
        }

        for (Object[] row : resultSet) {
            UnitCounterReportResponse unitCounterReportResponse = UnitCounterReportResponse.builder()
                    .allottedLotId(Util.objectToInteger(row[0]))
                    .lotTransactionDate(Util.objectToString(row[1]))
                    .weight(Util.objectToFloat(row[2]))
                    .bidAmount(Util.objectToInteger(row[3]))
                    .lotSoldOutAmount(Util.objectToFloat(row[4]))
                    .farmerMarketFee(Util.objectToFloat(row[5]))
                    .reelerMarketFee(Util.objectToFloat(row[6]))
                    .reelerLicense(Util.objectToString(row[7]))
                    .reelerName(Util.objectToString(row[8])).build();
            unitCounterReportResponses.add(unitCounterReportResponse);
        }

        rw.setContent(unitCounterReportResponses);
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getReelerMFReport(ReportRequest reportRequest) {
        List<ReelerMFResponse> reelerMFResponses = new ArrayList<>();
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        List<Object[]> resultSet;
        if (reportRequest.getReelerNumber() == null || reportRequest.getReelerNumber().isEmpty()) {
            resultSet = reelerAuctionRepository.getReelerMFReportWithoutReelerNumber(
                    reportRequest.getFromDate(), reportRequest.getToDate(),
                    reportRequest.getMarketId());
        } else {
            resultSet = reelerAuctionRepository.getReelerMFReport(
                    reportRequest.getFromDate(), reportRequest.getToDate(),
                    reportRequest.getMarketId(), reportRequest.getReelerNumber());
        }

        if (Util.isNullOrEmptyList(resultSet)) {
            commercialMarketHelper.retrunIfError(rw, "No data found");
            return ResponseEntity.ok(rw);
        }

        for (Object[] row : resultSet) {
            ReelerMFResponse reelerMFResponse = ReelerMFResponse.builder()
                    .allottedLotId(Util.objectToInteger(row[0]))
                    .lotTransactionDate(Util.objectToString(row[1]))
                    .weight(Util.objectToFloat(row[2]))
                    .bidAmount(Util.objectToInteger(row[3]))
                    .lotSoldOutAmount(Util.objectToFloat(row[4]))
                    .farmerMarketFee(Util.objectToFloat(row[5]))
                    .reelerMarketFee(Util.objectToFloat(row[6]))
                    .reelerLicense(Util.objectToString(row[7]))
                    .reelerName(Util.objectToString(row[8]))
                    .serialNumber(Util.objectToInteger(row[9])).build();
            reelerMFResponses.add(reelerMFResponse);
        }

        rw.setContent(reelerMFResponses);
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getPendingLotReport(ReportRequest requestBody) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        JwtPayloadData token = commercialMarketHelper.getAuthToken(requestBody);
        List<LotPendingReportResponse> lotPendingReportResponses = new ArrayList<>();
        List<Object[]> lotDetails = lotRepository.getAcceptedLotDetailsNullForPendingReport(requestBody.getReportFromDate(), requestBody.getMarketId());
        List<Object[]> binForPendingReportList = binRepository.findAllByAuctionDateAndMarketId(requestBody.getReportFromDate(),requestBody.getMarketId());

        Map<BigInteger, List<Integer>> bigBins = new HashMap<>();
        Map<BigInteger, List<Integer>> smallBins = new HashMap<>();
        for(Object[] binForPendingReport:binForPendingReportList){
            if(binForPendingReport[1].equals("big")){
                List<Integer> bigones = bigBins.get(binForPendingReport[2]);
                if(bigones==null){
                    bigones = new ArrayList<>();
                    bigBins.put((BigInteger) binForPendingReport[2],bigones);
                }
                bigones.add((Integer) binForPendingReport[0]);
            }else {
                List<Integer> smallOnes = smallBins.get(binForPendingReport[2]);
                if(smallOnes==null){
                    smallOnes = new ArrayList<>();
                    smallBins.put((BigInteger) binForPendingReport[2],smallOnes);
                }
                smallOnes.add((Integer) binForPendingReport[0]);
            }
        }

        long serailNumberForPagination = 0;
        List<Integer> acceptedLotList = new ArrayList<>();

        for (Object[] response : lotDetails) {
            BigInteger lotId = BigInteger.valueOf(Long.parseLong(String.valueOf(response[16])));
            LotPendingReportResponse lotPendingReportResponse = prepareResponseForLotBaseResponse(token, response, lotId);
            lotPendingReportResponse.setAuctionDateWithTime(Util.objectToString(response[24]));
            lotPendingReportResponse.setReelerLicense(Util.objectToString(response[25]));
            lotPendingReportResponse.setReelerName(Util.objectToString(response[26]));
            lotPendingReportResponse.setReelerAddress(Util.objectToString(response[27]));
            lotPendingReportResponse.setLotWeight(Util.objectToFloat(response[28]));
            lotPendingReportResponse.setBidAmount(Util.objectToFloat(response[32]));
            lotPendingReportResponse.setReelerCurrentBalance(Util.objectToFloat(response[33]));
            lotPendingReportResponse.setReelerFatherName(Util.objectToString(response[34]));
            lotPendingReportResponse.setReelerMobileNumber(Util.objectToString(response[35]));
            lotPendingReportResponse.setReelerNumber(Util.objectToString(response[36]));
            lotPendingReportResponse.setAcceptedBy(Util.objectToString(response[37]));
            lotPendingReportResponse.setShed(String.valueOf(response[39]));
            lotPendingReportResponse.setSerailNumberForPagination(++serailNumberForPagination);
            lotPendingReportResponse.setBigBinList(bigBins.get(lotPendingReportResponse.getMarketAuctionId().toBigInteger()));
            lotPendingReportResponse.setSmallBinList(smallBins.get(lotPendingReportResponse.getMarketAuctionId().toBigInteger()));
            lotPendingReportResponses.add(lotPendingReportResponse);
            acceptedLotList.add(lotPendingReportResponse.getAllottedLotId());
        }

        List<Object[]> newlyCreatedLotDetails = lotRepository.getNewlyCreatedLotDetailsNullForPendingReport(requestBody.getReportFromDate(), requestBody.getMarketId());

        for (Object[] response : newlyCreatedLotDetails) {
            BigInteger lotId = BigInteger.valueOf(Long.parseLong(String.valueOf(response[16])));
            LotPendingReportResponse lotPendingReportResponse = prepareResponseForLotBaseResponse(token, response, lotId);
            lotPendingReportResponse.setSerailNumberForPagination(++serailNumberForPagination);
            lotPendingReportResponse.setBigBinList(bigBins.get(lotPendingReportResponse.getMarketAuctionId().toBigInteger()));
            lotPendingReportResponse.setSmallBinList(smallBins.get(lotPendingReportResponse.getMarketAuctionId().toBigInteger()));
            lotPendingReportResponse.setShed(String.valueOf(response[24]));
            lotPendingReportResponses.add(lotPendingReportResponse);
        }
        rw.setContent(lotPendingReportResponses);
        return ResponseEntity.ok(rw);
    }

    public LotPendingReportResponse prepareResponseForLotBaseResponse(JwtPayloadData token, Object[] response, BigInteger lotId) {
        LotPendingReportResponse lotPendingReportResponse;
        lotPendingReportResponse = LotPendingReportResponse.builder().
                farmerNumber(Util.objectToString(response[0]))
                .farmerFirstName(Util.objectToString(response[1]))
                .farmerMiddleName(Util.objectToString(response[2]))
                .farmerLastName(Util.objectToString(response[3]))
                .farmerAddress(Util.objectToString(response[4]))
                .farmerTalukInKannada(Util.objectToString(response[5]))
                .farmerVillageInKannada(Util.objectToString(response[6]))
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
                .marketAuctionId((java.math.BigDecimal) response[22])
                .farmerVillage(Util.objectToString(response[23]))
                .build();
        lotPendingReportResponse.setLoginName(token.getUsername());
        return lotPendingReportResponse;
    }

    public ResponseEntity<?> getReelerPendingReport(RequestBody requestBody) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        List<Object[]> responses = lotRepository.getReelerPendingReport(requestBody.getMarketId());

        List<Object[]> responsesDebitTotal = lotRepository.geTotalDebitTxnToday(requestBody.getMarketId(), LocalDate.now());

        List<Object[]> responsesCreditTotal = lotRepository.getTotalCreditTxnToday(requestBody.getMarketId(), LocalDate.now());

        List<Object[]> responsesBalance = lotRepository.getTotalReelerBalance(requestBody.getMarketId());

        List<Object[]> responsesMarket = lotRepository.getMarketName(requestBody.getMarketId());

        if(Util.isNullOrEmptyList(responses))
        {
            throw new ValidationException("No data found");
        }
        ReelerPendingReportResponse reelerPendingReportResponse = new ReelerPendingReportResponse();
        if(responsesBalance.size()>0){
            if(responsesBalance.get(0) != null) {
                reelerPendingReportResponse.setBalance(Util.objectToString(responsesBalance.get(0)[0]));
            }
        }
        if(responsesCreditTotal.size()>0){
            if(responsesCreditTotal.get(0) != null) {
                reelerPendingReportResponse.setCreditTotal(Util.objectToString(responsesCreditTotal.get(0)[0]));
            }
        }
        if(responsesDebitTotal.size()>0){
            if(responsesDebitTotal.get(0) != null) {
                reelerPendingReportResponse.setDebitTotal(Util.objectToString(responsesDebitTotal.get(0)[0]));
            }
        }
        if(responsesMarket.size()>0){
            if(responsesMarket.get(0) != null) {
                reelerPendingReportResponse.setMarketName(Util.objectToString(responsesMarket.get(0)[0]));
            }
        }
        List<ReelerPendingInfo> reelerPendingInfoList = new ArrayList<>();
        int i= 1;
        for(Object[] response:responses){
            String lastTxn = "";
            if(Util.objectToString(response[6]) != null && !Util.objectToString(response[6]).equals("")){
                lastTxn = Util.objectToString(response[6]);
            }else{
                lastTxn = Util.objectToString(response[5]);
            }

            String suspend = "";
            if(Util.objectToFloat(response[4])< 0){
                suspend = "Yes";
            }

            ReelerPendingInfo reelerPendingInfo = ReelerPendingInfo.builder()
                    .reelerName(Util.objectToString(response[1]))
                    .reelerNumber(Util.objectToString(response[0]))
                    .reelingLicenseNumber(Util.objectToString(response[2]))
                    .mobileNumber(Util.objectToString(response[3]))
                    .currentBalance(Util.objectToString(response[4]))
                    .lastTxnTime(lastTxn)
                    .serialNumber(String.valueOf(i))
                    .counter("")
                    .onlineTxn("Yes")
                    .suspend(suspend)
                    .build();
            reelerPendingInfoList.add(reelerPendingInfo);
            i=i+1;
        }
        reelerPendingReportResponse.setReelerPendingInfoList(reelerPendingInfoList);
        rw.setContent(reelerPendingReportResponse);
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getDashboardReportAllMarket(){
        DashboardReportRequest dashboardReportRequest = new DashboardReportRequest();
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        List<Integer> marketIds = marketMasterRepository.getListOfMarketIds();
        List<Object> dashboardReportInfoList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDate formattedDate = now.toLocalDate();
        if(!marketIds.isEmpty()){
            for(int marketId : marketIds){
                dashboardReportRequest.setMarketId(marketId);
                dashboardReportRequest.setDashboardReportDate(formattedDate);
                ResponseEntity<?> response = getDashboardReport(dashboardReportRequest);
                log.info(String.valueOf(response.getBody()));
                dashboardReportInfoList.add(response.getBody());
            }
            rw.setContent(dashboardReportInfoList);
        }
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getDashboardReport(DashboardReportRequest dashboardReportRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);

        EntityManager entityManager = null;
        EntityManager entityManager2 = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();
            StoredProcedureQuery procedureQuery = entityManager
                    .createStoredProcedureQuery("sp_CheckBidStatus");

            procedureQuery.registerStoredProcedureParameter("marketId", Integer.class, ParameterMode.IN);

            entityManager.getTransaction().begin();
            procedureQuery.setParameter("marketId", dashboardReportRequest.getMarketId());
            procedureQuery.execute();

            entityManager.getTransaction().commit();
            List<Object[]> responsesIsAuctionStarted = procedureQuery.getResultList();

            entityManager2 = entityManagerFactory.createEntityManager();
            StoredProcedureQuery procedureQuery2 = entityManager
                    .createStoredProcedureQuery("sp_CheckAuctionStatus");

            procedureQuery2.registerStoredProcedureParameter("marketId", Integer.class, ParameterMode.IN);

            entityManager2.getTransaction().begin();
            procedureQuery2.setParameter("marketId", dashboardReportRequest.getMarketId());
            procedureQuery2.execute();

            entityManager2.getTransaction().commit();
            List<Object[]> responsesIsAcceptanceStarted = procedureQuery2.getResultList();

            List<Object[]> marketNameResponse = lotRepository.getMarketName(dashboardReportRequest.getMarketId());
            List<Object[]> responses = lotRepository.getDashboardCount(dashboardReportRequest.getMarketId(), dashboardReportRequest.getDashboardReportDate());

            DashboardReport dashboardReport = new DashboardReport();
            dashboardReport.setMarketName(Util.objectToString(marketNameResponse.get(0)[0]));
            dashboardReport.setAuctionStarted(Util.objectToString(responsesIsAuctionStarted.get(0)));
            dashboardReport.setAcceptanceStarted(Util.objectToString(responsesIsAcceptanceStarted.get(0)));

            List<DashboardReportInfo> dashboardReportInfoList = new ArrayList<>();
            for (Object[] response : responses) {
                DashboardReportInfo dashboardReportInfo = DashboardReportInfo.builder()
                        .raceName(Util.objectToString(response[0]))
                        .totalLots(Util.objectToString(response[1]))
                        .totalSoldOutAmount(Util.objectToString(response[13]))
                        .totalLotsBid(Util.objectToString(response[2]))
                        .totalBids(Util.objectToString(response[10]))
                        .totalReelers(Util.objectToString(response[4]))
                        .accecptedLots(Util.objectToString(response[5]))
                        .accecptedLotsMaxBid(Util.objectToString(response[6]))
                        .accectedLotsMinBid(Util.objectToString(response[7]))
                        .averagRate(Util.objectToString(response[8]))
                        .weighedLots(Util.objectToString(response[9]))
                        .currentAuctionMaxAmount(Util.objectToString(response[11]))
                        .totalLotsNotBid(Util.objectToString(response[12]))
                        .auctionCount(Util.objectToString(response[14]))
                        .totalWeight(Util.objectToString(response[15]))
                        .build();

                dashboardReportInfoList.add(dashboardReportInfo);
            }
            dashboardReport.setDashboardReportInfoList(dashboardReportInfoList);
            rw.setContent(dashboardReport);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
            if (entityManager2 != null && entityManager2.isOpen()) {
                entityManager2.close();
            }
        }
        return ResponseEntity.ok(rw);
    }

}
