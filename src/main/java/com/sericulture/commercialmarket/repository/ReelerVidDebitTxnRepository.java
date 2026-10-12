package com.sericulture.commercialmarket.repository;

import com.sericulture.commercialmarket.model.entity.ReelerVidDebitTxn;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigInteger;

public interface ReelerVidDebitTxnRepository extends PagingAndSortingRepository<ReelerVidDebitTxn, BigInteger> {

    public ReelerVidDebitTxn save(ReelerVidDebitTxn reelerVidDebitTxn);
}
