package com.sericulture.commercialmarket.service;

import com.sericulture.commercialmarket.helper.CommercialMarketHelper;
import com.sericulture.commercialmarket.model.ResponseWrapper;
import com.sericulture.commercialmarket.model.api.RequestBody;
import com.sericulture.commercialmarket.model.api.marketauction.LotHighestBidResponse;
import com.sericulture.commercialmarket.model.exceptions.MessageLabelType;
import com.sericulture.commercialmarket.model.exceptions.ValidationException;
import com.sericulture.commercialmarket.model.exceptions.ValidationMessage;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CommercialMarketReportService {

    @Autowired
    private CommercialMarketHelper commercialMarketHelper;

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

}
