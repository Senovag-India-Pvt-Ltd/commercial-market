package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.helper.CommercialMarketHelper;
import com.sericulture.marketandauction.model.api.RequestBody;
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
