package com.sericulture.commercialmarket.model.mapper;

import com.sericulture.commercialmarket.model.api.marketauction.*;
import com.sericulture.commercialmarket.model.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class Mapper {

    @Autowired
    ModelMapper mapper;

    /**
     * Maps marketauction Entity to marketauction Response Object
     * @param commercialMarketRequest
     * @param <T>
     */
    public <T> T marketAuctionObjectToEntity(CommercialMarketRequest commercialMarketRequest, Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.debug("Value of mapper is:",mapper, commercialMarketRequest);
        return (T) mapper.map(commercialMarketRequest, claaz);
    }

    /**
     * Maps reelerAuction Entity to marketauction Response Object
     * @param reelerBidRequest
     * @param <T>
     */
    public <T> T reelerAuctionObjectToEntity(ReelerBidRequest reelerBidRequest, Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.debug("Value of mapper is:",mapper, reelerBidRequest);
        return (T) mapper.map(reelerBidRequest, claaz);
    }

    /**
     * Maps flexTimeRequest Entity to flexTimeEntity Response Object
     * @param flexTimeEntity
     * @param <T>
     */
    public <T> T flexTimeEntityToObject(FlexTime flexTimeEntity, Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.debug("Value of mapper is:",mapper, flexTimeEntity);
        return (T) mapper.map(flexTimeEntity, claaz);
    }

    /**
     * Maps flexTime Entity to flexTimeEntity Response Object
     * @param fLexTimeRequest
     * @param <T>
     */
    public <T> T flextimeObjectToEntity(FLexTimeRequest fLexTimeRequest, Class<T> claaz) {
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        log.debug("Value of mapper is:",mapper, fLexTimeRequest);
        return (T) mapper.map(fLexTimeRequest, claaz);
    }

}
