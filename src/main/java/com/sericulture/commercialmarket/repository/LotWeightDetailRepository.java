package com.sericulture.commercialmarket.repository;

import com.sericulture.commercialmarket.model.entity.LotWeightDetail;
import com.sericulture.commercialmarket.model.entity.MarketAuction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigInteger;
import java.util.List;

public interface LotWeightDetailRepository extends PagingAndSortingRepository<LotWeightDetail, BigInteger> {

    public Iterable<LotWeightDetail> saveAll(Iterable<LotWeightDetail> lotWeightDetails);

    @Query("select netWeight from LotWeightDetail where lotId=:lotId")
    public List<Float> findAllByLotId(BigInteger lotId);


}
