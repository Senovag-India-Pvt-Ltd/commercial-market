package com.sericulture.commercialmarket.repository;

import com.sericulture.commercialmarket.model.entity.ExceptionalTime;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDate;

public interface ExceptionalTimeRepository extends PagingAndSortingRepository<ExceptionalTime, Integer> {

    public ExceptionalTime save(ExceptionalTime exceptionalTime);

    public ExceptionalTime findByMarketIdAndAuctionDate(int marketId, LocalDate auctionDate);
}
