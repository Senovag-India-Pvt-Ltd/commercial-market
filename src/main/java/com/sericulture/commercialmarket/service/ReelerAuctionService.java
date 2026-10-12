package com.sericulture.commercialmarket.service;


import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.commercialmarket.helper.CommercialMarketHelper;
import com.sericulture.commercialmarket.helper.Util;
import com.sericulture.commercialmarket.model.ResponseWrapper;
import com.sericulture.commercialmarket.model.api.ResponseBody;
import com.sericulture.commercialmarket.model.api.marketauction.*;
import com.sericulture.commercialmarket.model.entity.Lot;
import com.sericulture.commercialmarket.model.entity.ReelerAuction;
import com.sericulture.commercialmarket.model.entity.ReelerAuctionAccepted;
import com.sericulture.commercialmarket.model.enums.LotStatus;
import com.sericulture.commercialmarket.model.exceptions.MessageLabelType;
import com.sericulture.commercialmarket.model.exceptions.ValidationException;
import com.sericulture.commercialmarket.model.exceptions.ValidationMessage;
import com.sericulture.commercialmarket.repository.LotRepository;
import com.sericulture.commercialmarket.repository.ReelerAuctionRepository;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ReelerAuctionService {

    @Autowired
    ReelerAuctionRepository reelerAuctionRepository;

    @Autowired
    CommercialMarketHelper commercialMarketHelper;

    @Autowired
    LotRepository lotRepository;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    public ResponseEntity<?> getHighestBidPerLotDetails(LotStatusRequest lotStatusRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(ResponseBody.class);
        int currentSession = commercialMarketHelper.checkCurrentAuction(lotStatusRequest.getMarketId());
        //ReelerAuction ra = reelerAuctionRepository.getHighestBidForLotAndActive(lotStatusRequest.getAllottedLotId(), lotStatusRequest.getMarketId(), Util.getISTLocalDate(), currentSession);

        EntityManager entityManager = null;
        try{
            entityManager = entityManagerFactory.createEntityManager();
            StoredProcedureQuery procedureQuery = entityManager
                    .createStoredProcedureQuery("GET_ACCEPT_REELER_AUCTION");
            procedureQuery.registerStoredProcedureParameter("LotId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("MarketId", Integer.class, ParameterMode.IN);
            procedureQuery.registerStoredProcedureParameter("AuctionDate", LocalDate.class, ParameterMode.IN);
            entityManager.getTransaction().begin();
            procedureQuery.setParameter("LotId",  lotStatusRequest.getAllottedLotId());
            procedureQuery.setParameter("MarketId", lotStatusRequest.getMarketId());
            procedureQuery.setParameter("AuctionDate", Util.getISTLocalDate());
            procedureQuery.execute();
            entityManager.getTransaction().commit();

            List<Object[]> reelerLotHighestAndHisBidList = procedureQuery.getResultList();
            if(reelerLotHighestAndHisBidList.size()>0) {
                Object[] obj = reelerLotHighestAndHisBidList.get(0);
                ReelerAuction ra = reelerAuctionRepository.findById(BigInteger.valueOf(Long.parseLong(Util.objectToString(obj[0]))));

                LotBidDetailResponse lbdr = new LotBidDetailResponse();
                lbdr.setAllottedlotid(lotStatusRequest.getAllottedLotId());
                if (ra != null) {
                    Object[][] reelerDetails = reelerAuctionRepository.getReelerDetailsForHighestBidWithReelingNumber(ra.getId());
                    if (reelerDetails == null || reelerDetails.length == 0) {
                        rw.setErrorCode(-1);
                        rw.setErrorMessages(List.of("No reeler found please check for allotedLotId:" + lotStatusRequest.getAllottedLotId() + "and market: " + lotStatusRequest.getMarketId()));

                        return ResponseEntity.ok(rw);
                    }

                    lbdr.setAmount(ra.getAmount());
                    lbdr.setReelerAuctionId(ra.getId());
                    Object[][] ldrDetails = reelerAuctionRepository.getLotBidDetailResponse(lotStatusRequest.getAllottedLotId(), Util.getISTLocalDate(), lotStatusRequest.getMarketId());
                    if (ldrDetails == null || ldrDetails.length == 0) {
                        rw.setErrorCode(-1);
                        rw.setErrorMessages(List.of("No farmer details found please check for allotedLotId:" + lotStatusRequest.getAllottedLotId() + "and market: " + lotStatusRequest.getMarketId()));

                        return ResponseEntity.ok(rw);
                    }
                    lbdr.setFarmerFirstName(ldrDetails[0][0] == null ? "" : String.valueOf(ldrDetails[0][0]));
                    lbdr.setFarmerMiddleName(ldrDetails[0][1] == null ? "" : String.valueOf(ldrDetails[0][1]));
                    lbdr.setFarmerLastName(ldrDetails[0][2] == null ? "" : String.valueOf(ldrDetails[0][2]));
                    lbdr.setFarmerNumber(ldrDetails[0][3] == null ? "" : String.valueOf(ldrDetails[0][3]));
                    lbdr.setReelerName(reelerDetails[0][0] == null ? "" : String.valueOf(reelerDetails[0][0]));
                    lbdr.setReelingLicenseNumber(reelerDetails[0][2] == null ? "" : String.valueOf(reelerDetails[0][2]));
                    lbdr.setReelerFruitsId(reelerDetails[0][1] == null ? "" : String.valueOf(reelerDetails[0][1]));
                    lbdr.setFarmervillageName(ldrDetails[0][4] == null ? "" : String.valueOf(ldrDetails[0][4]));
                    lbdr.setLotApproxWeightBeforeWeighment(ldrDetails[0][5] == null ? 0 : Integer.valueOf(String.valueOf(ldrDetails[0][5])));
                    lbdr.setStatus(ldrDetails[0][6] == null ? "" : String.valueOf(ldrDetails[0][6]));
                    lbdr.setBidAcceptedBy(ldrDetails[0][7] == null ? "" : String.valueOf(ldrDetails[0][7]));
                    rw.setContent(lbdr);
                } else {
                    commercialMarketHelper.retrunIfError(rw, "No bid found for the given lot please check the input");
                }
            }
        }finally {
            if(entityManager!=null && entityManager.isOpen()){
                entityManager.close();
            }
        }

        return ResponseEntity.ok(rw);

    }

    @Transactional
    public ResponseEntity<?> acceptReelerBidForGivenLot(ReelerBidAcceptRequest lotStatusRequest) {
        log.info("Accept bid received for request: " + lotStatusRequest);
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        EntityManager entityManager = null;
        try {
            JwtPayloadData token = commercialMarketHelper.getAuthToken(lotStatusRequest);

            boolean canIssue = commercialMarketHelper.canPerformActivity(CommercialMarketHelper.activityType.AUCTIONACCEPT, lotStatusRequest.getMarketId(), lotStatusRequest.getGodownId());
            if (!canIssue) {
                ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(), "Cannot accept bid as time either over or not started", "-1");
                rw.setErrorCode(-1);
                rw.setErrorMessages(List.of(validationMessage));
                return ResponseEntity.ok(rw);
            }
            Lot lot = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(lotStatusRequest.getMarketId(), lotStatusRequest.getAllottedLotId(), Util.getISTLocalDate());
            if (!Util.isNullOrEmptyOrBlank(lot.getStatus())) {
                throw new ValidationException(String.format("expected Lot status is blank but found:%s for the allottedLotId: %s",lot.getStatus(),lot.getAllottedLotId()));
            }
            ReelerAuction reelerAuction = new ReelerAuction();


                entityManager = entityManagerFactory.createEntityManager();
                StoredProcedureQuery procedureQuery = entityManager
                        .createStoredProcedureQuery("GET_ACCEPT_REELER_AUCTION");
                procedureQuery.registerStoredProcedureParameter("LotId", Integer.class, ParameterMode.IN);
                procedureQuery.registerStoredProcedureParameter("MarketId", Integer.class, ParameterMode.IN);
                procedureQuery.registerStoredProcedureParameter("AuctionDate", LocalDate.class, ParameterMode.IN);
                entityManager.getTransaction().begin();
                procedureQuery.setParameter("LotId",  lotStatusRequest.getAllottedLotId());
                procedureQuery.setParameter("MarketId", lotStatusRequest.getMarketId());
                procedureQuery.setParameter("AuctionDate", Util.getISTLocalDate());
                procedureQuery.execute();
                entityManager.getTransaction().commit();

                List<Object[]> reelerLotHighestAndHisBidList = procedureQuery.getResultList();
                if(reelerLotHighestAndHisBidList.size()>0) {
                    Object[] obj = reelerLotHighestAndHisBidList.get(0);
                    reelerAuction = reelerAuctionRepository.findById(BigInteger.valueOf(Long.parseLong(Util.objectToString(obj[0]))));

                    if (reelerAuction != null) {
                        Object[][] reelerDetails = reelerAuctionRepository.getReelerDetailsForHighestBidWithReelingNumber(reelerAuction.getId());
                        if (reelerDetails == null || reelerDetails.length == 0) {
                            rw.setErrorCode(-1);
                            rw.setErrorMessages(List.of("No reeler found please check for allotedLotId:" + lotStatusRequest.getAllottedLotId() + "and market: " + lotStatusRequest.getMarketId()));

                            return ResponseEntity.ok(rw);
                        }
                    } else {
                        commercialMarketHelper.retrunIfError(rw, "No bid found for the given lot please check the input");
                    }
                }


            if (reelerAuction != null) {
                Lot l = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(reelerAuction.getMarketId(), reelerAuction.getAllottedLotId(), Util.getISTLocalDate());
                l.setStatus(LotStatus.ACCEPTED.getLabel());
                l.setReelerAuctionId(reelerAuction.getId());
                l.setBidAcceptedBy(token.getUsername());
                reelerAuction.setStatus(LotStatus.ACCEPTED.getLabel());
                lotRepository.save(l);
                reelerAuctionRepository.save(reelerAuction);

                // Create and save ReelerAuctionAccepted entity
                ReelerAuctionAccepted reelerAuctionAccepted = new ReelerAuctionAccepted();
                reelerAuctionAccepted.setMarketId(reelerAuction.getMarketId());
                reelerAuctionAccepted.setAllottedLotId(reelerAuction.getAllottedLotId());
                reelerAuctionAccepted.setReelerId(reelerAuction.getReelerId());
                reelerAuctionAccepted.setAmount(reelerAuction.getAmount());
                reelerAuctionAccepted.setStatus(reelerAuction.getStatus());
                reelerAuctionAccepted.setAuctionDate(reelerAuction.getAuctionDate());
                reelerAuctionAccepted.setSurrogateBid(reelerAuction.isSurrogateBid());
                reelerAuctionAccepted.setAuctionSession(reelerAuction.getAuctionSession());
                reelerAuctionRepository.save(reelerAuctionAccepted);

                l.setReelerAuctionAcceptedId(reelerAuctionAccepted.getId());
            } else {
                throw new ValidationException(String.format("No bids found for the given lot %s",lotStatusRequest.getAllottedLotId()));
            }
            log.info("Accept bid completed for request: " + lotStatusRequest);
            return ResponseEntity.ok(rw);
        } catch (Exception ex) {
            if(ex instanceof ValidationException){
                throw ex;
            }
            log.error("Error while processing request: " + lotStatusRequest + "with error:" + ex);
            return commercialMarketHelper.retrunIfError(rw, "error while processing the request");
        }finally {
            if(entityManager!=null && entityManager.isOpen()){
                entityManager.close();
            }
        }
    }

    public ResponseEntity<?> getReelerCurrentBalance(ReelerCurrentBalanceRequest reelerCurrentBalanceRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(ReelerCurrentBalanceResponse.class);

        Object[][] reelerBalanceRespObject = reelerAuctionRepository.getReelerCurrentBalance(reelerCurrentBalanceRequest.getReelerLicenceNumber(),reelerCurrentBalanceRequest.getMobileNumber(), reelerCurrentBalanceRequest.getMarketId());

        if (reelerBalanceRespObject == null || reelerBalanceRespObject.length == 0) {
            commercialMarketHelper.retrunIfError(rw, "Reeler data not found for the reelerId and market" + reelerCurrentBalanceRequest.getReelerLicenceNumber() + " market: " + reelerCurrentBalanceRequest.getMarketId());
        }

        List<ReelerCurrentBalanceResponse> reelerCurrentBalanceResponses = new ArrayList<>();
        for (Object[] balanceData : reelerBalanceRespObject) {
            ReelerCurrentBalanceResponse reelerCurrentBalanceResponse = ReelerCurrentBalanceResponse.builder()
                    .slNo(Util.objectToString(balanceData[0]))
                    .reelerId(Util.objectToInteger(balanceData[1]))
                    .reelerLicenseNumber(Util.objectToString(balanceData[2]))
                    .name(Util.objectToString(balanceData[3]))
                    .reelerVirtualAccount(Util.objectToString(balanceData[4]))
                    .mobileNumber(Util.objectToString(balanceData[5]))
                    .balance(Util.objectToFloat(balanceData[6]))
                    .updatedDate(Util.objectToDate(balanceData[7]))
                    .total(Util.objectToString(balanceData[8]))
                    .build();
            reelerCurrentBalanceResponses.add(reelerCurrentBalanceResponse);
        }
        rw.setContent(reelerCurrentBalanceResponses);
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getReelerTransaction(ReelerTransactionRequest reelerTransactionRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(ReelerTransactionResponse.class);

        Object[][] reelerTransactionRespObject = reelerAuctionRepository.getReelerTransaction(reelerTransactionRequest.getReelerLicenceNumber(),reelerTransactionRequest.getMobileNumber(), reelerTransactionRequest.getMarketId(),reelerTransactionRequest.getTransactionDate());

        if (reelerTransactionRespObject == null || reelerTransactionRespObject.length == 0) {
            commercialMarketHelper.retrunIfError(rw, "Reeler data not found for the reelerId and market" + reelerTransactionRequest.getReelerLicenceNumber() + " market: " + reelerTransactionRequest.getMarketId());
        }

        List<ReelerTransactionResponse> reelerTransactionResponses = new ArrayList<>();
        for (Object[] transactionData : reelerTransactionRespObject) {
            ReelerTransactionResponse reelerCurrentBalanceResponse = ReelerTransactionResponse.builder()
                    .slNo(Util.objectToString(transactionData[0]))
                    .reelerId(Util.objectToInteger(transactionData[1]))
                    .reelerLicenseNumber(Util.objectToString(transactionData[2]))
                    .name(Util.objectToString(transactionData[3]))
                    .reelerVirtualAccount(Util.objectToString(transactionData[4]))
                    .mobileNumber(Util.objectToString(transactionData[5]))
                    .remitterAccount(Util.objectToString(transactionData[6]))
                    .remitterBank(Util.objectToString(transactionData[7]))
                    .sqNo(Util.objectToString(transactionData[8]))
                    .refNo(Util.objectToString(transactionData[9]))
                    .amount(Util.objectToFloat(transactionData[10]))
                    .updatedDateTime(Util.objectToDateTime(transactionData[11]))
                    .postingDate(Util.objectToDate(transactionData[12]))
                    .total(Util.objectToString(transactionData[13]))
                    .build();
            reelerTransactionResponses.add(reelerCurrentBalanceResponse);
        }
        rw.setContent(reelerTransactionResponses);
        return ResponseEntity.ok(rw);
    }

    public ResponseEntity<?> getReelerCreditDetailsAllMarket(ReelerTransactionRequest reelerTransactionRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(ReelerTransactionResponse.class);

        List<Object[]> reelerCreditDetailsAllMarket = reelerAuctionRepository.getReelerCreditDetailsAllMarket(reelerTransactionRequest.getTransactionDate());

        List<ReelerCreditResponse> reelerTransactionResponses = new ArrayList<>();
        for (Object[] transactionData : reelerCreditDetailsAllMarket) {
            ReelerCreditResponse reelerCreditResponse = ReelerCreditResponse.builder()
                    .totalDepositedAmount(Util.objectToString(transactionData[0]))
                    .depositCount(Util.objectToString(transactionData[1]))
                    .totalReelersCount(Util.objectToString(transactionData[2]))
                    .marketName(Util.objectToString(transactionData[3]))
                    .totalOfAllMarketDepositedAmount(Util.objectToString(transactionData[5]))
                    .postDate(Util.objectToString(reelerTransactionRequest.getTransactionDate()))
                    .build();
            reelerTransactionResponses.add(reelerCreditResponse);
        }
        rw.setContent(reelerTransactionResponses);
        return ResponseEntity.ok(rw);
    }

}
