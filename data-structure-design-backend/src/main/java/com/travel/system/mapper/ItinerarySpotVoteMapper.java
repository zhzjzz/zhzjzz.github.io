package com.travel.system.mapper;

import com.travel.system.model.ItinerarySpotVote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ItinerarySpotVoteMapper {
    List<ItinerarySpotVote> findByItineraryId(Long itineraryId);

    ItinerarySpotVote findByUnique(@Param("itineraryId") Long itineraryId,
                                   @Param("spotId") Long spotId,
                                   @Param("username") String username);

    void insert(ItinerarySpotVote vote);

    int updateByUnique(ItinerarySpotVote vote);
}
