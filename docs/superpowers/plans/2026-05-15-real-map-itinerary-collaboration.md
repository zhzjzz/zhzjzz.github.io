# Real Map Itinerary Collaboration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current mock tactical collaboration map with a real AMap-based collaboration map where every votable spot is a real destination from project data.

**Architecture:** Add itinerary spot candidates as a first-class backend resource linked to real `Destination` rows with latitude and longitude. Rework the frontend collaboration drawer so users search/add real destinations, render them on AMap, and vote only on those real map markers. Keep the existing itinerary editing WebSocket flow and spot-vote broadcast channel, but enrich vote nodes from candidate records instead of synthetic fallback nodes.

**Tech Stack:** Spring Boot 3.5, Java 17, MyBatis XML, SQLite schema initialization, JUnit 5, Mockito, Vue 3 Composition API, Axios, Element Plus, AMap JSAPI 2.0 via `@amap/amap-jsapi-loader`, STOMP/SockJS, Node built-in test runner.

---

## Scope Check

This is one cohesive feature change across backend persistence, vote validation, and frontend rendering. It should be implemented on the existing `codex/tactical-map-collaboration` worktree because that branch already contains spot votes, collaboration WebSocket code, and mock tactical map components.

Explicitly in scope:

- Real AMap map inside the itinerary collaboration drawer.
- Votable spots come only from real `Destination` records with coordinates.
- User can search real destinations and add them to an itinerary collaboration map.
- Votes continue to broadcast in real time and REST fallback remains available.
- Existing itinerary text collaboration remains unchanged.

Explicitly out of scope:

- Freehand drawing, GPS live sharing, route optimization inside the collaboration drawer, and custom POI creation outside the real destination dataset.

## File Structure

Backend files:

- Create `data-structure-design-backend/src/main/java/com/travel/system/model/ItinerarySpotCandidate.java`: persisted real destination candidate for one itinerary.
- Create `data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotCandidateRequest.java`: request body for adding a real destination to an itinerary map.
- Create `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryMapSpot.java`: combined frontend payload containing candidate metadata and its votes.
- Create `data-structure-design-backend/src/main/java/com/travel/system/mapper/ItinerarySpotCandidateMapper.java`: MyBatis mapper interface.
- Create `data-structure-design-backend/src/main/resources/mapper/ItinerarySpotCandidateMapper.xml`: SQL for candidate list/upsert/find.
- Modify `data-structure-design-backend/src/main/java/com/travel/system/mapper/DestinationMapper.java`: add `findById`.
- Modify `data-structure-design-backend/src/main/resources/mapper/DestinationMapper.xml`: add `findById` SQL.
- Modify `data-structure-design-backend/src/main/resources/schema-sqlite.sql`: add `itinerary_spot_candidate` table and indexes.
- Create `data-structure-design-backend/src/main/java/com/travel/system/service/ItinerarySpotCandidateService.java`: validates real destination coordinates and builds map spot payloads.
- Create `data-structure-design-backend/src/main/java/com/travel/system/controller/ItinerarySpotCandidateController.java`: REST endpoints for map spots and candidate add.
- Modify `data-structure-design-backend/src/main/java/com/travel/system/service/ItinerarySpotVoteService.java`: require a vote target to be a candidate in the itinerary.
- Modify `data-structure-design-backend/src/main/java/com/travel/system/controller/ItineraryVoteWebSocketController.java`: broadcast map spots after vote changes.
- Modify `data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotVoteBroadcastMessage.java`: add `mapSpots`.
- Create `data-structure-design-backend/src/test/java/com/travel/system/service/ItinerarySpotCandidateServiceTest.java`: candidate validation and map payload tests.
- Modify `data-structure-design-backend/src/test/java/com/travel/system/service/ItinerarySpotVoteServiceTest.java`: voting rejects non-candidate destination.
- Modify `data-structure-design-backend/src/test/java/com/travel/system/controller/ItineraryVoteWebSocketControllerTest.java`: broadcast includes map spots.

Frontend files:

- Modify `data-structure-design-frontend/src/api/travel.js`: add `listItineraryMapSpots` and `addItinerarySpotCandidate`.
- Modify `data-structure-design-frontend/src/utils/itineraryVotes.js`: replace coordinate fallback node building with real map spot normalization.
- Modify `data-structure-design-frontend/src/utils/itineraryVotes.test.js`: test real coordinate grouping and no synthetic spot creation.
- Replace `data-structure-design-frontend/src/components/itinerary/TacticalMapPanel.vue`: render AMap markers instead of CSS-positioned fake nodes.
- Create `data-structure-design-frontend/src/components/itinerary/RealSpotSearchPanel.vue`: search real destinations and add selected destination as a candidate.
- Modify `data-structure-design-frontend/src/views/ItineraryView.vue`: load map spots, wire candidate add/search, use real marker selection, remove synthetic fallback nodes.
- Modify `docs/demo-runbook.md`: update demo steps to use real destination search and AMap.

---

### Task 1: Backend Real Spot Candidate Contract

**Files:**
- Create: `data-structure-design-backend/src/test/java/com/travel/system/service/ItinerarySpotCandidateServiceTest.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/model/ItinerarySpotCandidate.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotCandidateRequest.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryMapSpot.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/mapper/ItinerarySpotCandidateMapper.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/service/ItinerarySpotCandidateService.java`

- [ ] **Step 1: Write the failing candidate service test**

Create `data-structure-design-backend/src/test/java/com/travel/system/service/ItinerarySpotCandidateServiceTest.java`:

```java
package com.travel.system.service;

import com.travel.system.dto.ItineraryMapSpot;
import com.travel.system.dto.ItinerarySpotCandidateRequest;
import com.travel.system.mapper.ItinerarySpotCandidateMapper;
import com.travel.system.mapper.ItinerarySpotVoteMapper;
import com.travel.system.model.Destination;
import com.travel.system.model.ItinerarySpotCandidate;
import com.travel.system.model.ItinerarySpotVote;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItinerarySpotCandidateServiceTest {
    private final FakeCandidateMapper candidateMapper = new FakeCandidateMapper();
    private final FakeVoteMapper voteMapper = new FakeVoteMapper();
    private final FakeDestinationService destinationService = new FakeDestinationService();
    private final ItinerarySpotCandidateService service =
            new ItinerarySpotCandidateService(candidateMapper, voteMapper, destinationService);

    @Test
    void addsRealDestinationWithCoordinatesAsCandidate() {
        destinationService.destinations.add(new Destination(
                11L, "西湖", "scenic", "lake", 98.0, 4.9,
                "杭州真实景点", 30.259244, 120.13026));

        ItinerarySpotCandidateRequest request = new ItinerarySpotCandidateRequest();
        request.setDestinationId(11L);
        ItinerarySpotCandidate candidate = service.addCandidate(7L, request);

        assertThat(candidate.getItineraryId()).isEqualTo(7L);
        assertThat(candidate.getDestinationId()).isEqualTo(11L);
        assertThat(candidate.getSpotName()).isEqualTo("西湖");
        assertThat(candidate.getLatitude()).isEqualTo(30.259244);
        assertThat(candidate.getLongitude()).isEqualTo(120.13026);
    }

    @Test
    void rejectsDestinationWithoutCoordinates() {
        destinationService.destinations.add(new Destination(
                12L, "无坐标景点", "scenic", "unknown", 1.0, 1.0,
                "missing coords", null, null));
        ItinerarySpotCandidateRequest request = new ItinerarySpotCandidateRequest();
        request.setDestinationId(12L);

        assertThatThrownBy(() -> service.addCandidate(7L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coordinates");
    }

    @Test
    void buildsMapSpotsWithVotesGroupedByDestination() {
        candidateMapper.rows.add(candidate(7L, 11L, "西湖", 30.259244, 120.13026));
        voteMapper.rows.add(vote(7L, 11L, "西湖", "小周", "must"));
        voteMapper.rows.add(vote(7L, 11L, "西湖", "小林", "avoid"));

        List<ItineraryMapSpot> spots = service.listMapSpots(7L);

        assertThat(spots).hasSize(1);
        assertThat(spots.get(0).getDestinationId()).isEqualTo(11L);
        assertThat(spots.get(0).getSpotId()).isEqualTo(11L);
        assertThat(spots.get(0).getSpotName()).isEqualTo("西湖");
        assertThat(spots.get(0).getLatitude()).isEqualTo(30.259244);
        assertThat(spots.get(0).getLongitude()).isEqualTo(120.13026);
        assertThat(spots.get(0).getVotes()).hasSize(2);
    }

    private static ItinerarySpotCandidate candidate(Long itineraryId, Long destinationId, String name,
                                                    Double latitude, Double longitude) {
        ItinerarySpotCandidate candidate = new ItinerarySpotCandidate();
        candidate.setId(destinationId);
        candidate.setItineraryId(itineraryId);
        candidate.setDestinationId(destinationId);
        candidate.setSpotName(name);
        candidate.setLatitude(latitude);
        candidate.setLongitude(longitude);
        candidate.setCreatedAt(LocalDateTime.now());
        candidate.setUpdatedAt(LocalDateTime.now());
        return candidate;
    }

    private static ItinerarySpotVote vote(Long itineraryId, Long spotId, String spotName,
                                          String username, String voteType) {
        ItinerarySpotVote vote = new ItinerarySpotVote();
        vote.setId((long) username.hashCode());
        vote.setItineraryId(itineraryId);
        vote.setSpotId(spotId);
        vote.setSpotName(spotName);
        vote.setUsername(username);
        vote.setVoteType(voteType);
        vote.setReason("");
        vote.setCreatedAt(LocalDateTime.now());
        vote.setUpdatedAt(LocalDateTime.now());
        return vote;
    }

    private static class FakeDestinationService extends DestinationService {
        private final List<Destination> destinations = new ArrayList<>();

        FakeDestinationService() {
            super(null, null);
        }

        @Override
        public Destination findById(Long id) {
            return destinations.stream()
                    .filter(destination -> Objects.equals(destination.getId(), id))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static class FakeCandidateMapper implements ItinerarySpotCandidateMapper {
        private final List<ItinerarySpotCandidate> rows = new ArrayList<>();

        @Override
        public List<ItinerarySpotCandidate> findByItineraryId(Long itineraryId) {
            return rows.stream()
                    .filter(row -> Objects.equals(row.getItineraryId(), itineraryId))
                    .toList();
        }

        @Override
        public ItinerarySpotCandidate findByUnique(Long itineraryId, Long destinationId) {
            return rows.stream()
                    .filter(row -> Objects.equals(row.getItineraryId(), itineraryId))
                    .filter(row -> Objects.equals(row.getDestinationId(), destinationId))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public void insert(ItinerarySpotCandidate candidate) {
            candidate.setId((long) rows.size() + 1);
            rows.add(candidate);
        }

        @Override
        public int updateByUnique(ItinerarySpotCandidate candidate) {
            ItinerarySpotCandidate existing = findByUnique(candidate.getItineraryId(), candidate.getDestinationId());
            if (existing == null) return 0;
            existing.setSpotName(candidate.getSpotName());
            existing.setLatitude(candidate.getLatitude());
            existing.setLongitude(candidate.getLongitude());
            existing.setUpdatedAt(candidate.getUpdatedAt());
            return 1;
        }
    }

    private static class FakeVoteMapper implements ItinerarySpotVoteMapper {
        private final List<ItinerarySpotVote> rows = new ArrayList<>();

        @Override
        public List<ItinerarySpotVote> findByItineraryId(Long itineraryId) {
            return rows.stream()
                    .filter(row -> Objects.equals(row.getItineraryId(), itineraryId))
                    .toList();
        }

        @Override
        public ItinerarySpotVote findByUnique(Long itineraryId, Long spotId, String username) {
            return rows.stream()
                    .filter(row -> Objects.equals(row.getItineraryId(), itineraryId))
                    .filter(row -> Objects.equals(row.getSpotId(), spotId))
                    .filter(row -> Objects.equals(row.getUsername(), username))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public void insert(ItinerarySpotVote vote) {
            rows.add(vote);
        }

        @Override
        public int updateByUnique(ItinerarySpotVote vote) {
            return 0;
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
cd data-structure-design-backend
$env:JAVA_HOME='D:\software\jdk-26'
$env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'
mvn.cmd test -Dtest=ItinerarySpotCandidateServiceTest
```

Expected: compilation fails because candidate model, DTO, mapper, service, `DestinationMapper.findById`, and `DestinationService.findById` do not exist.

- [ ] **Step 3: Add destination lookup to mapper and service**

Modify `data-structure-design-backend/src/main/java/com/travel/system/mapper/DestinationMapper.java`:

```java
Destination findById(@Param("id") Long id);
```

Add to `data-structure-design-backend/src/main/resources/mapper/DestinationMapper.xml`:

```xml
<select id="findById" parameterType="long" resultType="com.travel.system.model.Destination">
    SELECT s.spot_id AS id,
           s.name AS name,
           s.category AS sceneType,
           s.category AS category,
           s.hotness AS heat,
           s.rating AS rating,
           s.description AS description,
           nc.latitude AS latitude,
           nc.longitude AS longitude
    FROM spots s
    LEFT JOIN (
        SELECT spot_name, AVG(y) AS latitude, AVG(x) AS longitude
        FROM nodes
        WHERE y IS NOT NULL AND x IS NOT NULL
        GROUP BY spot_name
    ) nc ON nc.spot_name = s.name
    WHERE s.spot_id = #{id}
</select>
```

Modify `data-structure-design-backend/src/main/java/com/travel/system/service/DestinationService.java` by adding:

```java
public Destination findById(Long id) {
    if (id == null) {
        return null;
    }
    return destinationMapper.findById(id);
}
```

- [ ] **Step 4: Add candidate model and DTOs**

Create `data-structure-design-backend/src/main/java/com/travel/system/model/ItinerarySpotCandidate.java`:

```java
package com.travel.system.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ItinerarySpotCandidate {
    private Long id;
    private Long itineraryId;
    private Long destinationId;
    private String spotName;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

Create `data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotCandidateRequest.java`:

```java
package com.travel.system.dto;

import lombok.Data;

@Data
public class ItinerarySpotCandidateRequest {
    private Long destinationId;
}
```

Create `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryMapSpot.java`:

```java
package com.travel.system.dto;

import com.travel.system.model.ItinerarySpotVote;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ItineraryMapSpot {
    private Long candidateId;
    private Long destinationId;
    private Long spotId;
    private String spotName;
    private Double latitude;
    private Double longitude;
    private List<ItinerarySpotVote> votes = new ArrayList<>();
}
```

- [ ] **Step 5: Add candidate mapper interface**

Create `data-structure-design-backend/src/main/java/com/travel/system/mapper/ItinerarySpotCandidateMapper.java`:

```java
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
```

- [ ] **Step 6: Add candidate service**

Create `data-structure-design-backend/src/main/java/com/travel/system/service/ItinerarySpotCandidateService.java`:

```java
package com.travel.system.service;

import com.travel.system.dto.ItineraryMapSpot;
import com.travel.system.dto.ItinerarySpotCandidateRequest;
import com.travel.system.mapper.ItinerarySpotCandidateMapper;
import com.travel.system.mapper.ItinerarySpotVoteMapper;
import com.travel.system.model.Destination;
import com.travel.system.model.ItinerarySpotCandidate;
import com.travel.system.model.ItinerarySpotVote;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItinerarySpotCandidateService {
    private final ItinerarySpotCandidateMapper candidateMapper;
    private final ItinerarySpotVoteMapper voteMapper;
    private final DestinationService destinationService;

    public ItinerarySpotCandidateService(ItinerarySpotCandidateMapper candidateMapper,
                                         ItinerarySpotVoteMapper voteMapper,
                                         DestinationService destinationService) {
        this.candidateMapper = candidateMapper;
        this.voteMapper = voteMapper;
        this.destinationService = destinationService;
    }

    public List<ItinerarySpotCandidate> findByItineraryId(Long itineraryId) {
        if (itineraryId == null) {
            throw new IllegalArgumentException("itineraryId is required");
        }
        return candidateMapper.findByItineraryId(itineraryId);
    }

    public boolean exists(Long itineraryId, Long destinationId) {
        if (itineraryId == null || destinationId == null) {
            return false;
        }
        return candidateMapper.findByUnique(itineraryId, destinationId) != null;
    }

    public ItinerarySpotCandidate addCandidate(Long itineraryId, ItinerarySpotCandidateRequest request) {
        validateRequest(itineraryId, request);
        Destination destination = destinationService.findById(request.getDestinationId());
        if (destination == null) {
            throw new IllegalArgumentException("destination does not exist");
        }
        if (destination.getLatitude() == null || destination.getLongitude() == null) {
            throw new IllegalArgumentException("destination coordinates are required");
        }

        LocalDateTime now = LocalDateTime.now();
        ItinerarySpotCandidate candidate = new ItinerarySpotCandidate();
        candidate.setItineraryId(itineraryId);
        candidate.setDestinationId(destination.getId());
        candidate.setSpotName(destination.getName());
        candidate.setLatitude(destination.getLatitude());
        candidate.setLongitude(destination.getLongitude());
        candidate.setUpdatedAt(now);

        ItinerarySpotCandidate existing = candidateMapper.findByUnique(itineraryId, destination.getId());
        if (existing == null) {
            candidate.setCreatedAt(now);
            candidateMapper.insert(candidate);
        } else {
            candidate.setCreatedAt(existing.getCreatedAt());
            candidateMapper.updateByUnique(candidate);
        }
        return candidateMapper.findByUnique(itineraryId, destination.getId());
    }

    public List<ItineraryMapSpot> listMapSpots(Long itineraryId) {
        List<ItinerarySpotCandidate> candidates = findByItineraryId(itineraryId);
        Map<Long, List<ItinerarySpotVote>> votesBySpotId = voteMapper.findByItineraryId(itineraryId).stream()
                .collect(Collectors.groupingBy(ItinerarySpotVote::getSpotId));

        return candidates.stream().map(candidate -> {
            ItineraryMapSpot spot = new ItineraryMapSpot();
            spot.setCandidateId(candidate.getId());
            spot.setDestinationId(candidate.getDestinationId());
            spot.setSpotId(candidate.getDestinationId());
            spot.setSpotName(candidate.getSpotName());
            spot.setLatitude(candidate.getLatitude());
            spot.setLongitude(candidate.getLongitude());
            spot.setVotes(votesBySpotId.getOrDefault(candidate.getDestinationId(), List.of()));
            return spot;
        }).toList();
    }

    private void validateRequest(Long itineraryId, ItinerarySpotCandidateRequest request) {
        if (itineraryId == null) {
            throw new IllegalArgumentException("itineraryId is required");
        }
        if (request == null || request.getDestinationId() == null) {
            throw new IllegalArgumentException("destinationId is required");
        }
    }
}
```

- [ ] **Step 7: Run the candidate service test**

Run:

```powershell
mvn.cmd test -Dtest=ItinerarySpotCandidateServiceTest
```

Expected: `Tests run: 3, Failures: 0, Errors: 0`.

- [ ] **Step 8: Commit backend candidate contract**

```powershell
git add data-structure-design-backend/src/test/java/com/travel/system/service/ItinerarySpotCandidateServiceTest.java data-structure-design-backend/src/main/java/com/travel/system/model/ItinerarySpotCandidate.java data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotCandidateRequest.java data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryMapSpot.java data-structure-design-backend/src/main/java/com/travel/system/mapper/ItinerarySpotCandidateMapper.java data-structure-design-backend/src/main/java/com/travel/system/mapper/DestinationMapper.java data-structure-design-backend/src/main/resources/mapper/DestinationMapper.xml data-structure-design-backend/src/main/java/com/travel/system/service/ItinerarySpotCandidateService.java data-structure-design-backend/src/main/java/com/travel/system/service/DestinationService.java
git commit -m "feat: add itinerary real spot candidates"
```

---

### Task 2: Backend Persistence, REST API, and Vote Validation

**Files:**
- Modify: `data-structure-design-backend/src/main/resources/schema-sqlite.sql`
- Create: `data-structure-design-backend/src/main/resources/mapper/ItinerarySpotCandidateMapper.xml`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/controller/ItinerarySpotCandidateController.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/ItinerarySpotVoteService.java`
- Modify: `data-structure-design-backend/src/test/java/com/travel/system/service/ItinerarySpotVoteServiceTest.java`

- [ ] **Step 1: Add failing vote validation test**

Modify `data-structure-design-backend/src/test/java/com/travel/system/service/ItinerarySpotVoteServiceTest.java`:

```java
@Test
void rejectsVoteForSpotThatIsNotAnItineraryCandidate() {
    ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();
    message.setSpotId(101L);
    message.setSpotName("西湖");
    message.setUsername("小周");
    message.setVoteType("must");

    ItinerarySpotCandidateService candidateService = mock(ItinerarySpotCandidateService.class);
    when(candidateService.exists(7L, 101L)).thenReturn(false);
    ItinerarySpotVoteService service = new ItinerarySpotVoteService(mapper, candidateService);

    assertThatThrownBy(() -> service.saveVote(7L, message))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("candidate");
}
```

At the top of the file, add:

```java
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
```

Update existing `ItinerarySpotVoteService` construction in tests to pass a candidate service whose `exists` method returns `true`.

- [ ] **Step 2: Run the vote service test to verify it fails**

Run:

```powershell
mvn.cmd test -Dtest=ItinerarySpotVoteServiceTest
```

Expected: compilation fails because the service constructor still accepts only `ItinerarySpotVoteMapper`.

- [ ] **Step 3: Add schema table**

Append to `data-structure-design-backend/src/main/resources/schema-sqlite.sql`:

```sql
CREATE TABLE IF NOT EXISTS itinerary_spot_candidate (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    itinerary_id INTEGER NOT NULL,
    destination_id INTEGER NOT NULL,
    spot_name TEXT NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_itinerary_spot_candidate_unique
ON itinerary_spot_candidate(itinerary_id, destination_id);

CREATE INDEX IF NOT EXISTS idx_itinerary_spot_candidate_itinerary
ON itinerary_spot_candidate(itinerary_id);
```

- [ ] **Step 4: Add MyBatis XML mapper**

Create `data-structure-design-backend/src/main/resources/mapper/ItinerarySpotCandidateMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.travel.system.mapper.ItinerarySpotCandidateMapper">
    <select id="findByItineraryId" parameterType="long" resultType="com.travel.system.model.ItinerarySpotCandidate">
        SELECT id,
               itinerary_id AS itineraryId,
               destination_id AS destinationId,
               spot_name AS spotName,
               latitude,
               longitude,
               created_at AS createdAt,
               updated_at AS updatedAt
        FROM itinerary_spot_candidate
        WHERE itinerary_id = #{itineraryId}
        ORDER BY updated_at DESC, id DESC
    </select>

    <select id="findByUnique" resultType="com.travel.system.model.ItinerarySpotCandidate">
        SELECT id,
               itinerary_id AS itineraryId,
               destination_id AS destinationId,
               spot_name AS spotName,
               latitude,
               longitude,
               created_at AS createdAt,
               updated_at AS updatedAt
        FROM itinerary_spot_candidate
        WHERE itinerary_id = #{itineraryId}
          AND destination_id = #{destinationId}
    </select>

    <insert id="insert" parameterType="com.travel.system.model.ItinerarySpotCandidate"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO itinerary_spot_candidate(
            itinerary_id, destination_id, spot_name, latitude, longitude, created_at, updated_at
        )
        VALUES(
            #{itineraryId}, #{destinationId}, #{spotName}, #{latitude}, #{longitude}, #{createdAt}, #{updatedAt}
        )
    </insert>

    <update id="updateByUnique" parameterType="com.travel.system.model.ItinerarySpotCandidate">
        UPDATE itinerary_spot_candidate
        SET spot_name = #{spotName},
            latitude = #{latitude},
            longitude = #{longitude},
            updated_at = #{updatedAt}
        WHERE itinerary_id = #{itineraryId}
          AND destination_id = #{destinationId}
    </update>
</mapper>
```

- [ ] **Step 5: Add REST controller**

Create `data-structure-design-backend/src/main/java/com/travel/system/controller/ItinerarySpotCandidateController.java`:

```java
package com.travel.system.controller;

import com.travel.system.dto.ItineraryMapSpot;
import com.travel.system.dto.ItinerarySpotCandidateRequest;
import com.travel.system.model.ItinerarySpotCandidate;
import com.travel.system.service.ItinerarySpotCandidateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/itineraries/{itineraryId}/map-spots")
public class ItinerarySpotCandidateController {
    private final ItinerarySpotCandidateService candidateService;

    public ItinerarySpotCandidateController(ItinerarySpotCandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping
    public List<ItineraryMapSpot> list(@PathVariable Long itineraryId) {
        return candidateService.listMapSpots(itineraryId);
    }

    @PostMapping
    public ItinerarySpotCandidate add(@PathVariable Long itineraryId,
                                      @RequestBody ItinerarySpotCandidateRequest request) {
        try {
            return candidateService.addCandidate(itineraryId, request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
```

- [ ] **Step 6: Validate votes against candidates**

Modify `data-structure-design-backend/src/main/java/com/travel/system/service/ItinerarySpotVoteService.java`:

```java
private final ItinerarySpotCandidateService candidateService;

public ItinerarySpotVoteService(ItinerarySpotVoteMapper voteMapper,
                                ItinerarySpotCandidateService candidateService) {
    this.voteMapper = voteMapper;
    this.candidateService = candidateService;
}
```

Inside `validate(Long itineraryId, ItinerarySpotVoteMessage message)`, after `spotId` validation, add:

```java
if (!candidateService.exists(itineraryId, message.getSpotId())) {
    throw new IllegalArgumentException("spot must be an itinerary candidate");
}
```

- [ ] **Step 7: Run backend service tests**

Run:

```powershell
mvn.cmd test -Dtest=ItinerarySpotCandidateServiceTest,ItinerarySpotVoteServiceTest
```

Expected: both service test classes pass.

- [ ] **Step 8: Commit backend persistence and validation**

```powershell
git add data-structure-design-backend/src/main/resources/schema-sqlite.sql data-structure-design-backend/src/main/resources/mapper/ItinerarySpotCandidateMapper.xml data-structure-design-backend/src/main/java/com/travel/system/controller/ItinerarySpotCandidateController.java data-structure-design-backend/src/main/java/com/travel/system/service/ItinerarySpotVoteService.java data-structure-design-backend/src/test/java/com/travel/system/service/ItinerarySpotVoteServiceTest.java
git commit -m "feat: require real map spots for votes"
```

---

### Task 3: WebSocket Broadcast Real Map Spots

**Files:**
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotVoteBroadcastMessage.java`
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/controller/ItineraryVoteWebSocketController.java`
- Modify: `data-structure-design-backend/src/test/java/com/travel/system/controller/ItineraryVoteWebSocketControllerTest.java`

- [ ] **Step 1: Add failing broadcast assertion**

Modify `ItineraryVoteWebSocketControllerTest` so the successful vote test captures the broadcast and asserts:

```java
ItinerarySpotVoteBroadcastMessage broadcast = captor.getValue();
assertThat(broadcast.getType()).isEqualTo(ItinerarySpotVoteBroadcastMessage.Type.SPOT_VOTE_UPDATED);
assertThat(broadcast.getMapSpots()).hasSize(1);
assertThat(broadcast.getMapSpots().get(0).getDestinationId()).isEqualTo(101L);
assertThat(broadcast.getMapSpots().get(0).getLatitude()).isEqualTo(30.259244);
assertThat(broadcast.getMapSpots().get(0).getLongitude()).isEqualTo(120.13026);
```

- [ ] **Step 2: Run the WebSocket test to verify it fails**

Run:

```powershell
mvn.cmd test -Dtest=ItineraryVoteWebSocketControllerTest
```

Expected: compilation fails because `mapSpots` does not exist on the broadcast DTO.

- [ ] **Step 3: Add mapSpots to broadcast DTO**

Modify `data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotVoteBroadcastMessage.java`:

```java
private List<ItineraryMapSpot> mapSpots;
```

Update the all-args constructor usage or replace it with explicit setter construction in the controller. Prefer explicit setter construction so adding fields stays readable.

- [ ] **Step 4: Broadcast current map spot payloads**

Modify `ItineraryVoteWebSocketController` constructor:

```java
private final ItinerarySpotCandidateService candidateService;

public ItineraryVoteWebSocketController(ItinerarySpotVoteService voteService,
                                        ItinerarySpotCandidateService candidateService,
                                        SimpMessagingTemplate messagingTemplate) {
    this.voteService = voteService;
    this.candidateService = candidateService;
    this.messagingTemplate = messagingTemplate;
}
```

In the success branch, replace constructor use with:

```java
ItinerarySpotVoteBroadcastMessage broadcast = new ItinerarySpotVoteBroadcastMessage();
broadcast.setType(ItinerarySpotVoteBroadcastMessage.Type.SPOT_VOTE_UPDATED);
broadcast.setUsername(saved.getUsername());
broadcast.setVote(saved);
broadcast.setVotes(votes);
broadcast.setMapSpots(candidateService.listMapSpots(id));
broadcast.setServerTimestamp(LocalDateTime.now());
messagingTemplate.convertAndSend("/topic/itinerary/" + id, broadcast);
```

In the rejection branch, set `mapSpots` to `List.of()`.

- [ ] **Step 5: Run WebSocket test**

Run:

```powershell
mvn.cmd test -Dtest=ItineraryVoteWebSocketControllerTest
```

Expected: `Tests run: 2, Failures: 0, Errors: 0`.

- [ ] **Step 6: Commit broadcast update**

```powershell
git add data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotVoteBroadcastMessage.java data-structure-design-backend/src/main/java/com/travel/system/controller/ItineraryVoteWebSocketController.java data-structure-design-backend/src/test/java/com/travel/system/controller/ItineraryVoteWebSocketControllerTest.java
git commit -m "feat: broadcast real itinerary map spots"
```

---

### Task 4: Frontend Real Map Spot Utilities and API

**Files:**
- Modify: `data-structure-design-frontend/src/api/travel.js`
- Modify: `data-structure-design-frontend/src/utils/itineraryVotes.js`
- Modify: `data-structure-design-frontend/src/utils/itineraryVotes.test.js`

- [ ] **Step 1: Write failing utility tests**

Modify `data-structure-design-frontend/src/utils/itineraryVotes.test.js`:

```js
import {
  buildRealSpotNodes,
  computeConsensus,
  summarizeConsensus,
  voteTypeLabel,
} from './itineraryVotes.js'

test('buildRealSpotNodes keeps real destination coordinates', () => {
  const nodes = buildRealSpotNodes([
    {
      destinationId: 11,
      spotId: 11,
      spotName: '西湖',
      latitude: 30.259244,
      longitude: 120.13026,
      votes: [{ username: '小周', voteType: 'must' }],
    },
  ])

  assert.equal(nodes.length, 1)
  assert.equal(nodes[0].spotName, '西湖')
  assert.equal(nodes[0].latitude, 30.259244)
  assert.equal(nodes[0].longitude, 120.13026)
  assert.equal(nodes[0].consensus, 'must')
})

test('buildRealSpotNodes filters records without real coordinates', () => {
  const nodes = buildRealSpotNodes([
    { destinationId: 12, spotId: 12, spotName: '无坐标', latitude: null, longitude: null, votes: [] },
  ])

  assert.deepEqual(nodes, [])
})
```

- [ ] **Step 2: Run frontend tests to verify failure**

Run:

```powershell
cd data-structure-design-frontend
npm.cmd test
```

Expected: fails because `buildRealSpotNodes` is not exported.

- [ ] **Step 3: Add API helpers**

Modify `data-structure-design-frontend/src/api/travel.js` below itinerary vote helpers:

```js
export const listItineraryMapSpots = (id) => http.get(`/itineraries/${id}/map-spots`)
export const addItinerarySpotCandidate = (id, payload) => http.post(`/itineraries/${id}/map-spots`, payload)
```

- [ ] **Step 4: Add real spot node builder**

Modify `data-structure-design-frontend/src/utils/itineraryVotes.js`:

```js
export const buildRealSpotNodes = (mapSpots = []) => {
  return mapSpots
    .filter((spot) => Number.isFinite(Number(spot.latitude)) && Number.isFinite(Number(spot.longitude)))
    .map((spot) => {
      const votes = Array.isArray(spot.votes) ? spot.votes : []
      return {
        candidateId: spot.candidateId,
        destinationId: spot.destinationId,
        spotId: spot.spotId || spot.destinationId,
        spotName: spot.spotName || `景点 ${spot.spotId || spot.destinationId}`,
        latitude: Number(spot.latitude),
        longitude: Number(spot.longitude),
        votes,
        consensus: computeConsensus(votes),
      }
    })
}
```

Keep `buildSpotNodes` temporarily for compatibility, but stop using it in `ItineraryView.vue` after Task 6.

- [ ] **Step 5: Run frontend tests**

Run:

```powershell
npm.cmd test
```

Expected: all itinerary vote utility tests pass.

- [ ] **Step 6: Commit API and utility changes**

```powershell
git add data-structure-design-frontend/src/api/travel.js data-structure-design-frontend/src/utils/itineraryVotes.js data-structure-design-frontend/src/utils/itineraryVotes.test.js
git commit -m "feat: add real map spot utilities"
```

---

### Task 5: Replace Mock Tactical Map with AMap

**Files:**
- Modify: `data-structure-design-frontend/src/components/itinerary/TacticalMapPanel.vue`

- [ ] **Step 1: Replace component with real AMap renderer**

Replace `data-structure-design-frontend/src/components/itinerary/TacticalMapPanel.vue` with:

```vue
<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AMapLoader from '@amap/amap-jsapi-loader'
import { voteTypeLabel } from '../../utils/itineraryVotes'
import { wgs84ToGcj02 } from '../../utils/coordTransform'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  selectedSpotId: { type: [Number, String], default: null },
})

const emit = defineEmits(['select-node'])

const mapEl = ref(null)
let AMapApi = null
let map = null
let markers = []

const consensusColor = (consensus) => ({
  must: '#ff385c',
  want: '#0f766e',
  conflict: '#f59e0b',
  backup: '#64748b',
}[consensus] || '#64748b')

const clearMarkers = () => {
  if (map && markers.length) {
    map.remove(markers)
  }
  markers = []
}

const markerContent = (node) => `
  <button class="itinerary-map-marker ${String(node.spotId) === String(props.selectedSpotId) ? 'active' : ''}" type="button">
    <span class="marker-dot" style="background:${consensusColor(node.consensus)}"></span>
    <strong>${node.spotName}</strong>
    <small>${voteTypeLabel(node.consensus === 'conflict' ? 'avoid' : node.consensus)} · ${node.votes.length}票</small>
  </button>
`

const renderMarkers = () => {
  if (!map || !AMapApi) return
  clearMarkers()
  markers = props.nodes.map((node) => {
    const [lng, lat] = wgs84ToGcj02(node.longitude, node.latitude)
    const marker = new AMapApi.Marker({
      position: [lng, lat],
      anchor: 'bottom-center',
      content: markerContent(node),
      zIndex: String(node.spotId) === String(props.selectedSpotId) ? 120 : 100,
    })
    marker.on('click', () => emit('select-node', node))
    return marker
  })
  if (markers.length) {
    map.add(markers)
    map.setFitView(markers, false, [64, 64, 64, 64], 16)
  }
}

const initMap = async () => {
  await nextTick()
  if (map || !mapEl.value) return
  const amapKey = (import.meta.env.VITE_AMAP_KEY || '').trim()
  if (!amapKey) {
    ElMessage.warning('未配置 VITE_AMAP_KEY，协作地图无法加载')
    return
  }
  const amapSecret = (import.meta.env.VITE_AMAP_SECRET || '').trim()
  if (amapSecret) {
    window._AMapSecurityConfig = { securityJsCode: amapSecret }
  }
  AMapApi = await AMapLoader.load({
    key: amapKey,
    version: '2.0',
    plugins: ['AMap.Scale', 'AMap.ToolBar'],
  })
  map = new AMapApi.Map(mapEl.value, {
    zoom: 4,
    center: [104.1954, 35.8617],
    mapStyle: 'amap://styles/normal',
    viewMode: '2D',
    features: ['bg', 'road', 'point'],
  })
  map.addControl(new AMapApi.Scale())
  map.addControl(new AMapApi.ToolBar({ position: 'RB' }))
  renderMarkers()
}

watch(() => props.nodes, () => {
  initMap().then(renderMarkers)
}, { deep: true, immediate: true })

watch(() => props.selectedSpotId, renderMarkers)

onBeforeUnmount(() => {
  clearMarkers()
  map?.destroy?.()
  map = null
  AMapApi = null
})
</script>

<template>
  <section class="tactical-map-panel">
    <div ref="mapEl" class="real-map-stage"></div>
    <div v-if="!nodes.length" class="empty-map">先添加真实景点</div>
  </section>
</template>

<style scoped>
.tactical-map-panel {
  position: relative;
  min-height: 520px;
  overflow: hidden;
  border-radius: 8px;
  background: #e5e7eb;
  border: 1px solid #d7dde7;
}

.real-map-stage {
  width: 100%;
  height: 520px;
}

.empty-map {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.72);
  color: #475569;
  font-weight: 800;
  pointer-events: none;
}

:global(.itinerary-map-marker) {
  display: grid;
  min-width: 132px;
  gap: 2px;
  padding: 8px 10px;
  border: 2px solid #ffffff;
  border-radius: 8px;
  background: #111827;
  color: #ffffff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.26);
  cursor: pointer;
  text-align: left;
}

:global(.itinerary-map-marker.active) {
  outline: 3px solid rgba(255, 56, 92, 0.42);
}

:global(.itinerary-map-marker strong) {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

:global(.itinerary-map-marker small) {
  color: #d7dce5;
  font-size: 11px;
}

:global(.marker-dot) {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

@media (max-width: 900px) {
  .tactical-map-panel,
  .real-map-stage {
    min-height: 420px;
    height: 420px;
  }
}
</style>
```

- [ ] **Step 2: Run frontend build**

Run:

```powershell
npm.cmd run build
```

Expected: Vite build completes and `@amap/amap-jsapi-loader` resolves.

- [ ] **Step 3: Commit AMap component**

```powershell
git add data-structure-design-frontend/src/components/itinerary/TacticalMapPanel.vue
git commit -m "feat: render collaboration spots on amap"
```

---

### Task 6: Frontend Real Destination Search and Itinerary Integration

**Files:**
- Create: `data-structure-design-frontend/src/components/itinerary/RealSpotSearchPanel.vue`
- Modify: `data-structure-design-frontend/src/views/ItineraryView.vue`

- [ ] **Step 1: Create real spot search panel**

Create `data-structure-design-frontend/src/components/itinerary/RealSpotSearchPanel.vue`:

```vue
<script setup>
defineProps({
  keyword: { type: String, default: '' },
  results: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  addingId: { type: [Number, String], default: null },
})

const emit = defineEmits(['update:keyword', 'search', 'add'])
</script>

<template>
  <section class="real-spot-search">
    <div class="search-row">
      <el-input
        :model-value="keyword"
        placeholder="搜索真实景点"
        clearable
        @update:model-value="emit('update:keyword', $event)"
        @keyup.enter="emit('search')"
      />
      <el-button type="primary" :loading="loading" @click="emit('search')">搜索</el-button>
    </div>
    <div class="result-list">
      <button
        v-for="spot in results"
        :key="spot.id"
        type="button"
        class="result-item"
        :disabled="!spot.latitude || !spot.longitude"
        @click="emit('add', spot)"
      >
        <strong>{{ spot.name }}</strong>
        <span>{{ spot.category || spot.sceneType || '景点' }} · {{ spot.rating || '-' }}分</span>
        <small v-if="!spot.latitude || !spot.longitude">缺少坐标，不能加入地图</small>
      </button>
      <div v-if="!results.length" class="result-empty">输入景点名称后搜索</div>
    </div>
  </section>
</template>

<style scoped>
.real-spot-search {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
}

.result-list {
  display: grid;
  gap: 8px;
  max-height: 220px;
  overflow: auto;
}

.result-item {
  display: grid;
  gap: 4px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  text-align: left;
  cursor: pointer;
}

.result-item:disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.result-item strong {
  color: #222222;
}

.result-item span,
.result-item small,
.result-empty {
  color: #64748b;
  font-size: 12px;
}
</style>
```

- [ ] **Step 2: Update imports in itinerary view**

Modify `data-structure-design-frontend/src/views/ItineraryView.vue` imports:

```js
import RealSpotSearchPanel from '../components/itinerary/RealSpotSearchPanel.vue'
import { buildRealSpotNodes, makePingText } from '../utils/itineraryVotes'
import {
  addItinerarySpotCandidate,
  createItinerary,
  getItinerary,
  listItineraries,
  listItineraryMapSpots,
  searchDestinations,
  submitItinerarySpotVote,
  updateItinerary,
} from '../api/travel'
```

Remove `listItinerarySpotVotes` and `buildSpotNodes` from imports.

- [ ] **Step 3: Replace synthetic node state**

In `ItineraryView.vue`, replace:

```js
const spotVotes = ref([])
```

with:

```js
const mapSpots = ref([])
const spotSearchKeyword = ref('')
const spotSearchResults = ref([])
const spotSearchLoading = ref(false)
const spotAddingId = ref(null)
```

Replace `fallbackTacticalNodes` and `tacticalNodes` with:

```js
const tacticalNodes = computed(() => buildRealSpotNodes(mapSpots.value))
```

- [ ] **Step 4: Replace map spot loading and broadcast handling**

Replace `loadSpotVotes` with:

```js
const loadMapSpots = async (row) => {
  if (!row?.id) {
    mapSpots.value = []
    selectedNode.value = null
    return
  }
  const { data } = await listItineraryMapSpots(row.id)
  mapSpots.value = Array.isArray(data) ? data : []
  syncSelectedNode()
}
```

In `applyVoteBroadcast`, replace:

```js
if (Array.isArray(payload.votes)) {
  spotVotes.value = payload.votes
}
```

with:

```js
if (Array.isArray(payload.mapSpots)) {
  mapSpots.value = payload.mapSpots
}
```

In REST fallback inside `submitVote`, replace `await loadSpotVotes(collabRow.value)` with:

```js
await loadMapSpots(collabRow.value)
```

- [ ] **Step 5: Add real destination search handlers**

Add to `ItineraryView.vue`:

```js
const searchRealSpots = async () => {
  const keyword = spotSearchKeyword.value.trim()
  if (!keyword) {
    spotSearchResults.value = []
    return
  }
  spotSearchLoading.value = true
  try {
    const { data } = await searchDestinations(keyword)
    spotSearchResults.value = Array.isArray(data)
      ? data.filter((item) => item.latitude != null && item.longitude != null).slice(0, 10)
      : []
  } finally {
    spotSearchLoading.value = false
  }
}

const addRealSpot = async (spot) => {
  if (!collabRow.value?.id || !spot?.id) return
  spotAddingId.value = spot.id
  try {
    await addItinerarySpotCandidate(collabRow.value.id, { destinationId: spot.id })
    await loadMapSpots(collabRow.value)
    ElMessage.success('景点已加入协作地图')
  } finally {
    spotAddingId.value = null
  }
}
```

- [ ] **Step 6: Update open and close collaboration**

In `openCollaboration`, replace:

```js
await loadSpotVotes(data)
```

with:

```js
spotSearchKeyword.value = ''
spotSearchResults.value = []
await loadMapSpots(data)
```

In `closeCollaboration`, replace:

```js
spotVotes.value = []
```

with:

```js
mapSpots.value = []
spotSearchKeyword.value = ''
spotSearchResults.value = []
```

- [ ] **Step 7: Add search panel to template**

Inside `.tactical-side`, before `ConsensusProgress`, add:

```vue
<RealSpotSearchPanel
  v-model:keyword="spotSearchKeyword"
  :results="spotSearchResults"
  :loading="spotSearchLoading"
  :adding-id="spotAddingId"
  @search="searchRealSpots"
  @add="addRealSpot"
/>
```

- [ ] **Step 8: Run frontend verification**

Run:

```powershell
npm.cmd test
npm.cmd run build
```

Expected: Node tests pass and Vite build completes.

- [ ] **Step 9: Commit real spot integration**

```powershell
git add data-structure-design-frontend/src/components/itinerary/RealSpotSearchPanel.vue data-structure-design-frontend/src/views/ItineraryView.vue
git commit -m "feat: use real destinations in collaboration map"
```

---

### Task 7: Documentation and End-to-End Verification

**Files:**
- Modify: `docs/demo-runbook.md`

- [ ] **Step 1: Update demo runbook**

Replace the tactical collaboration demo section in `docs/demo-runbook.md` with:

```markdown
## Tactical Map Collaboration Demo

1. Start the backend and frontend from `codex/tactical-map-collaboration`.
2. Confirm `data-structure-design-frontend/.env` has `VITE_AMAP_KEY` and `VITE_AMAP_SECRET`.
3. Open `http://localhost:5173/#/itineraries`.
4. Choose an itinerary and click `协作`.
5. Search a real destination such as `西湖`.
6. Add a result with latitude and longitude to the collaboration map.
7. Confirm the AMap panel shows a marker at the real location.
8. Vote `必去` for that marker.
9. Open a second browser session with another demo user and vote `不想去` for the same marker.
10. Confirm the marker changes to conflict state and the Ping panel shows both actions.
11. Stop WebSocket connectivity or refresh during reconnect, submit another vote, and confirm REST fallback saves it.
```

- [ ] **Step 2: Run backend full verification**

Run:

```powershell
cd data-structure-design-backend
$env:JAVA_HOME='D:\software\jdk-26'
$env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'
mvn.cmd test
```

Expected: all backend tests pass.

- [ ] **Step 3: Run frontend full verification**

Run:

```powershell
cd data-structure-design-frontend
npm.cmd test
npm.cmd run build
```

Expected: utility tests pass and Vite build completes.

- [ ] **Step 4: Manually verify in browser**

Run backend and frontend:

```powershell
cd data-structure-design-backend
$env:JAVA_HOME='D:\software\jdk-26'
$env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH
$env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'
mvn.cmd spring-boot:run
```

```powershell
cd data-structure-design-frontend
npm.cmd run dev
```

Expected browser behavior:

- `http://localhost:5173/#/itineraries` loads.
- Opening `协作` shows a real AMap map, not a fake CSS node panel.
- Searching `西湖` returns real destination records.
- Adding a real destination places a marker at its coordinate.
- Voting updates the marker state and Ping feed.

- [ ] **Step 5: Commit docs and verification notes**

```powershell
git add docs/demo-runbook.md
git commit -m "docs: update real map collaboration demo"
```

---

## Self-Review

Spec coverage:

- Real API map: Task 5 replaces CSS mock map with AMap JSAPI.
- Real votable spots: Tasks 1, 2, 4, and 6 use `Destination` records with latitude/longitude.
- Existing vote collaboration retained: Tasks 2 and 3 keep REST/WebSocket voting.
- Frontend user workflow: Task 6 adds real destination search and candidate add.
- Demo and verification: Task 7 covers local manual and automated verification.

Placeholder scan:

- No `TBD`, `TODO`, or "implement later" markers remain.
- Each code-changing step contains concrete code or exact replacement instructions.
- Each verification step has an exact command and expected outcome.

Type consistency:

- Backend candidate identity uses `destinationId`.
- Vote `spotId` is the same value as `destinationId`.
- Frontend nodes expose `spotId`, `destinationId`, `spotName`, `latitude`, `longitude`, `votes`, and `consensus`.
- Broadcast success payload uses `mapSpots`; frontend `applyVoteBroadcast` consumes `payload.mapSpots`.
