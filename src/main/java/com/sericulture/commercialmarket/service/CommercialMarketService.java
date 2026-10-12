package com.sericulture.commercialmarket.service;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.commercialmarket.helper.CommercialMarketHelper;
import com.sericulture.commercialmarket.helper.Util;
import com.sericulture.commercialmarket.model.ResponseWrapper;
import com.sericulture.commercialmarket.model.api.marketauction.*;
import com.sericulture.commercialmarket.model.entity.*;
import com.sericulture.commercialmarket.model.exceptions.MessageLabelType;
import com.sericulture.commercialmarket.model.exceptions.ValidationException;
import com.sericulture.commercialmarket.model.exceptions.ValidationMessage;
import com.sericulture.commercialmarket.model.mapper.Mapper;
import com.sericulture.commercialmarket.repository.*;
import jakarta.persistence.*;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
public class CommercialMarketService {

    @Autowired
    private CommercialMarketRepository commercialMarketRepository;

    @Autowired
    private BinCounterRepository binCounterRepository;

    @Autowired
    private BinMasterRepository binMasterRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private BinCounterMasterRepository binCounterMasterRepository;

    @Autowired
    Mapper mapper;

    @Autowired
    Util util;
    @Autowired
    private CustomValidator validator;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private CommercialMarketHelper commercialMarketHelper;
    public  ResponseEntity<?> marketAuctionFacade(CommercialMarketRequest commercialMarketRequest) {

        LocalDateTime tStart = LocalDateTime.now();

        ResponseWrapper rw = ResponseWrapper.createWrapper(CommercialMarketResponse.class);
        CommercialMarketResponse commercialMarketResponse = new CommercialMarketResponse();

        JwtPayloadData token = commercialMarketHelper.getAuthToken(commercialMarketRequest);

        int marketId = Util.getMarketId(token);
        int goDownId = Util.getGodownId(token);


        //validator.validate(commercialMarketResponse);
        boolean canIssue = commercialMarketHelper.canPerformActivity(CommercialMarketHelper.activityType.ISSUEBIDSLIP,commercialMarketRequest.getMarketId(), commercialMarketRequest.getGodownId());

        if(!canIssue){
            ValidationMessage validationMessage = new ValidationMessage(MessageLabelType.NON_LABEL_MESSAGE.name(),
                    util.getMessageByCode("MA00002.GEN.FLEXTIME"),"MA00002.GEN.FLEXTIME");
            rw.setErrorCode(-1);
            rw.setErrorMessages(List.of(validationMessage));
            return ResponseEntity.ok(rw);
        }
        MarketAuction marketAuction = null;
        try {
            marketAuction = saveMarketAuction(commercialMarketRequest);
            commercialMarketResponse.setTransactionId(marketAuction.getId());
            commercialMarketResponse.setMarketId(marketAuction.getMarketId());
            commercialMarketResponse.setGodownId(marketAuction.getGodownId());
            commercialMarketResponse.setFarmerId(marketAuction.getFarmerId());
            commercialMarketResponse.setReelerId(marketAuction.getReelerId());
            // saves bin and the lot
            log.info(String.format("marketAuction: processing the Requrest for marketId: %s gowdownId: %s and farmer: %s"
                    ,marketAuction.getMarketId(),marketAuction.getGodownId(),marketAuction.getFarmerId(),marketAuction.getReelerId()));
            saveBinAndLot(commercialMarketResponse, marketAuction);

            rw.setContent(commercialMarketResponse);
            marketAuction.setStatus("generated");
            log.info(String.format("marketAuction: succesfull generation of market auction Requrest for marketId: %s gowdownId: %s and farmer: %s"
                    ,marketAuction.getMarketId(),marketAuction.getGodownId(),marketAuction.getFarmerId(),marketAuction.getReelerId()));
        } catch (Exception e) {
            marketAuction.setStatus("error");
            e.printStackTrace();
            log.error("Error occurred while processing the request %s", commercialMarketRequest);
                throw e;
        } finally {
            if(Objects.nonNull(marketAuction)) {
                commercialMarketRepository.save(marketAuction);
            }
        }

        log.info("total time is: "+ ChronoUnit.MILLIS.between(tStart,LocalDateTime.now()));

        return ResponseEntity.ok(rw);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private MarketAuction saveMarketAuction(CommercialMarketRequest commercialMarketRequest) {
        MarketAuction marketAuction = mapper.marketAuctionObjectToEntity(commercialMarketRequest, MarketAuction.class);
        validator.validate(marketAuction);
        marketAuction.setMarketAuctionDate(Util.getISTLocalDate());
        marketAuction.setStatus("in creation");

        return commercialMarketRepository.save(marketAuction);
    }

    /** Runs ina single transaction to allocate bins and lot together rollbacks if there are any exception during the process
     *
     * @param commercialMarketResponse
     * @param marketAuction
     */
    //@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE,rollbackFor = Exception.class)
    private void saveBinAndLot(CommercialMarketResponse commercialMarketResponse, MarketAuction marketAuction) {

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try{

            entityManager.getTransaction().begin();
            LocalDateTime tStart = LocalDateTime.now();
            Map<String, List<Integer>> allotedBins = saveBin(marketAuction.getId(), marketAuction.getNumberOfSmallBin(), marketAuction.getNumberOfBigBin(), marketAuction.getMarketId(), marketAuction.getGodownId(),entityManager);

            commercialMarketResponse.setAllotedBigBinList(allotedBins.get("big"));
            commercialMarketResponse.setAllotedSmallBinList(allotedBins.get("small"));

            List<Integer> lotList = saveLot(marketAuction.getId(), marketAuction.getNumberOfLot(), marketAuction.getMarketId(), marketAuction.getGodownId(),marketAuction.getEstimatedWeight(),entityManager);
            commercialMarketResponse.setAllotedLotList(lotList);
            entityManager.getTransaction().commit();
            log.info("lots saved successfully for the request: "+marketAuction);
        }catch (Exception ex){
            entityManager.getTransaction().rollback();
            log.error("Error while creating lots for the request: "+marketAuction);
            throw ex;
        }finally {
            if(entityManager!=null && entityManager.isOpen()){
                entityManager.close();
            }
        }


    }
    private List<Integer> saveLot(BigInteger id, int numberOfLot, int marketId, int godownId,float estimatedWeight,EntityManager entityManager) {
        List<Integer> lotList = new ArrayList<>();
        Integer lotCounter = 0;
        lotCounter = lotRepository.findByMarketIdAndAuctionDate(marketId, Util.getISTLocalDate());
        if (lotCounter == null) {
            lotCounter = 0;
        }
        float approxWeightPerLot = estimatedWeight / numberOfLot;
        List<Lot> lots = new ArrayList<>();
        for (int i = 0; i < numberOfLot; i++) {
            Lot lot = new Lot();
            int allotedLot = lotCounter + 1 + i;
            lot.setAllottedLotId(allotedLot);
            lotList.add(allotedLot);
            lot.setMarketAuctionId(id);
            lot.setMarketId(marketId);
            lot.setAuctionDate(Util.getISTLocalDate());
            lot.setCustomerReferenceNumber(Util.getCRN(Util.getISTLocalDate(),marketId,allotedLot));
            lot.setLotApproxWeightBeforeWeighment(approxWeightPerLot);
            lots.add(lot);
            entityManager.persist(lot);
        }
        //lotRepository.saveAll(lots);

        return lotList;
    }


    private Map<String, List<Integer>> saveBin(BigInteger marketAuctionId, int numberOfSmallBin, int numberOfBigBin, int marketId, int godownId,EntityManager entityManager) {
        BinCounter bc = null;
        Map<String, List<Integer>> allotedBins = new HashMap<>();
        int smallSequenceEnd = 0;
        int bigSequenceEnd = 0;
        int smallBinStart = 0;
        int bigBinStart = 0;
        List<Integer> smallBins = new ArrayList<>();
        List<Integer> smallAlloted = new ArrayList<>();
        List<Bin> binList = new ArrayList<>();
        bc = binCounterRepository.findByMarketIdAndGodownIdAndAuctionDate(marketId, godownId, Util.getISTLocalDate());
        // in case its null its inserts the new record in separate transaction locking the row, thus syncing the process allowing only one row.
        if (bc == null) {
           bc = checkAndInsertForMaster(marketId, godownId);

        }
        // locks the row for that market for all
        LocalDateTime tStart = LocalDateTime.now();
       // bc = binCounterRepository.getByMarketEntryForTheDayLocked(bc.getId().longValue());

       Query q= entityManager.createNativeQuery("select * from bin_counter with(ROWLOCK,XLOCK) where bin_counter_id= ?1",BinCounter.class);
       q.setParameter(1,bc.getId().longValue());

       //List<Object> bc = q.getResultList();
        bc = (BinCounter) q.getSingleResult();

     //  bc = (BinCounter) bcList.get(0);


        log.info("total time  acquire lock "+ ChronoUnit.MILLIS.between(tStart,LocalDateTime.now()));
        smallBinStart = bc.getSmallBinNextNumber();
        bigBinStart = bc.getBigBinNextNumber();

        BinCounterMaster binCounterMaster = binCounterMasterRepository.findByMarketIdAndGodownId(marketId, godownId);
        //Master fetched to get the last count /** NOT REQUIRED TO LOCK THE MASTER FOR NOW */
        //BinCounterMaster binCounterMaster = binCounterMasterRepository.getByMarketIdAndAuction(byMarketIdAndGodownId.getId());


        if(numberOfSmallBin>0){
            smallSequenceEnd = saveEachTypeOfBin(marketAuctionId, marketId, godownId, "small", smallBinStart, binCounterMaster.getSmallBinEnd(), numberOfSmallBin, allotedBins,entityManager);
        }
        if(numberOfBigBin>0){
            bigSequenceEnd = saveEachTypeOfBin(marketAuctionId, marketId, godownId, "big", bigBinStart, binCounterMaster.getBigBinEnd(), numberOfBigBin, allotedBins,entityManager);
        }

        if(bc==null){
            bc = new BinCounter();
            bc.setMarketId(marketId);
            bc.setGodownId(godownId);
            bc.setAuctionDate(Util.getISTLocalDate());
        }

        if(numberOfBigBin>0)
            bc.setBigBinNextNumber(++bigSequenceEnd);
        if(numberOfSmallBin>0)
            bc.setSmallBinNextNumber(++smallSequenceEnd);
        //binCounterRepository.save(bc);

        entityManager.merge(bc);
        entityManager.persist(bc);

        return allotedBins;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    private BinCounter checkAndInsertForMaster( int marketId, int godownId) {
        //fetch pk to lock only that row
        BinCounterMaster byMarketIdAndGodownId = binCounterMasterRepository.findByMarketIdAndGodownId(marketId, godownId);
        //Master fetched to get the last count
        BinCounterMaster binCounterMaster = binCounterMasterRepository.getByMarketIdAndAuction(byMarketIdAndGodownId.getId());
        BinCounter bc = binCounterRepository.findByMarketIdAndGodownIdAndAuctionDate(marketId, godownId, Util.getISTLocalDate());
        if(Objects.isNull(bc)) {
            int smallBinStart = binCounterMaster.getSmallBinStart();
            int bigBinStart = binCounterMaster.getBigBinStart();
            bc = new BinCounter();
            bc.setBigBinNextNumber(smallBinStart);
            bc.setBigBinNextNumber(bigBinStart);
            bc.setMarketId(marketId);
            bc.setGodownId(godownId);
            bc.setAuctionDate(Util.getISTLocalDate());
            binCounterRepository.save(bc);
        }
        return bc;
    }

    private int saveEachTypeOfBin(BigInteger marketAuctionId, int marketId, int godownId, String type, int binStart, int binEnd, int limit, Map<String, List<Integer>> allotedBins,EntityManager entityManager) {
        List<Integer> bins = binMasterRepository.
                findByMarketIdAndGodownIdAndTypeAndStatusAndBinNumber(marketId, godownId, type, "available", binStart, binEnd, limit);
        int nextSequence = 0;
        List<Integer> allotedList = new ArrayList<>();
        List<Bin> binList = new ArrayList<>();
        if(bins==null || bins.isEmpty() || bins.size()!=limit){
            throw new ValidationException("No sufficient bins available");
        }
        for (int i = 0; i < limit; i++) {
            Bin bin = new Bin(bins.get(i), marketAuctionId, type,marketId);
            bin.setAuctionDate(Util.getISTLocalDate());
            binList.add(bin);
            nextSequence = bins.get(i);
            allotedList.add(nextSequence);
            entityManager.persist(bin);
        }
        //binRepository.saveAll(binList);
        allotedBins.put(type, allotedList);
        return nextSequence;
    }


}
