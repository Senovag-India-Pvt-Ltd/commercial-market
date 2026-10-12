package com.sericulture.commercialmarket.model.api.marketauction;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class FarmerPaymentInfoRequestByLotList extends FarmerPaymentInfoRequest{

    private List<Integer> allottedLotList;
}
