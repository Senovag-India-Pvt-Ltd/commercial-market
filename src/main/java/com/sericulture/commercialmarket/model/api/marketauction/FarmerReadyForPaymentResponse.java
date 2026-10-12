package com.sericulture.commercialmarket.model.api.marketauction;

import com.sericulture.commercialmarket.model.api.ResponseBody;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class FarmerReadyForPaymentResponse extends ResponseBody {

    private List<FarmerPaymentInfoResponse> farmerPaymentInfoResponseList;

    private double totalAmountToFarmer;
}
