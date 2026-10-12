package com.sericulture.commercialmarket.model.api.marketauction;

import com.sericulture.commercialmarket.helper.CommercialMarketHelper;
import com.sericulture.commercialmarket.model.api.RequestBody;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class FLexTimeRequest extends RequestBody {

    private String activityType;

    private boolean start;



}
