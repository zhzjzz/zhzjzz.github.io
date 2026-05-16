package com.travel.system.mapper;

import com.travel.system.model.ItinerarySpotCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ItinerarySpotCandidateMapper {
    List<ItinerarySpotCandidate> findByItineraryId(Long itineraryId);

    ItinerarySpotCandidate findByUnique(@Param("itineraryId") Long itineraryId,
                                        @Param("destinationId") Long destinationId);

    void insert(ItinerarySpotCandidate candidate);

    int updateByUnique(ItinerarySpotCandidate candidate);
}
