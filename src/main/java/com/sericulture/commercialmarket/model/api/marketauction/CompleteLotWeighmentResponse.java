package com.sericulture.commercialmarket.model.api.marketauction;

import com.sericulture.commercialmarket.model.api.ResponseBody;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CompleteLotWeighmentResponse extends ResponseBody {

    private int allottedLotId;

    private double totalAmountDebited;
}
