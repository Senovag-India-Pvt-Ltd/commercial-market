package com.sericulture.commercialmarket.model.api.marketauction;

import com.sericulture.commercialmarket.model.api.ResponseBody;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class DTROnlineReportResponse extends ResponseBody {
    private List<DTROnlineReportUnitDetail> dtrOnlineReportUnitDetailList = new ArrayList<>();
    private int totalLots;
    private float totalFarmerAmount;
    private float totalReelerAmount;
    private float totalTraderAmount;
    private float totalTraderMarketFee;
    private float totalReelerMarketFee;
    private float totalFarmerMarketFee;
    private int totalBidAmount;
    private float totalWeight;
    private float totallotSoldOutAmount;
    private String marketNameKannada;
    private int paymentSuccessLots;
    private Long maxAmount;
    private Long minAmount;
    private float avgAmount;
    private long notTransactedLots;

}
