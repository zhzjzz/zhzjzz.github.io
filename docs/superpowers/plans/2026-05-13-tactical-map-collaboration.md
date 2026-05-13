# Tactical Map Collaboration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a tactical-map style itinerary collaboration layer where teammates vote on travel spots and see consensus changes in real time.

**Architecture:** Add a lightweight `ItinerarySpotVote` backend model with REST and WebSocket entry points, then render a tactical map panel inside the existing itinerary page. Keep voting independent from the existing itinerary CRUD model so the current optimistic-lock itinerary editing flow remains unchanged.

**Tech Stack:** Spring Boot 3.5, Java 17, MyBatis XML mappers, SQLite schema initialization, STOMP WebSocket, Vue 3 Composition API, Pinia, Axios, Element Plus, Node built-in test runner for frontend utility tests.

---

## Scope Check

The approved spec is one feature with backend persistence, backend realtime broadcasting, and frontend visualization. These parts are coupled by the vote data contract and should ship together as one implementation plan. GPS location sharing, full GIS editing, route negotiation, and Ban/Pick flows are excluded.

## File Structure

Backend files:

- Create `data-structure-design-backend/src/main/java/com/travel/system/model/ItinerarySpotVote.java`: vote persistence model.
- Create `data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotVoteMessage.java`: REST/WebSocket vote input.
- Create `data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotVoteBroadcastMessage.java`: realtime broadcast payload.
- Create `data-structure-design-backend/src/main/java/com/travel/system/mapper/ItinerarySpotVoteMapper.java`: MyBatis mapper interface.
- Create `data-structure-design-backend/src/main/resources/mapper/ItinerarySpotVoteMapper.xml`: SQL for query, insert, and upsert update.
- Modify `data-structure-design-backend/src/main/resources/schema-sqlite.sql`: add `itinerary_spot_vote` table and unique index.
- Create `data-structure-design-backend/src/main/java/com/travel/system/service/ItinerarySpotVoteService.java`: validation and upsert logic.
- Create `data-structure-design-backend/src/main/java/com/travel/system/controller/ItinerarySpotVoteController.java`: REST query and vote fallback.
- Create `data-structure-design-backend/src/main/java/com/travel/system/controller/ItineraryVoteWebSocketController.java`: realtime vote entry point and broadcast.
- Create `data-structure-design-backend/src/test/java/com/travel/system/service/ItinerarySpotVoteServiceTest.java`: service-level behavior tests.
- Create `data-structure-design-backend/src/test/java/com/travel/system/controller/ItineraryVoteWebSocketControllerTest.java`: WebSocket broadcast behavior tests.

Frontend files:

- Modify `data-structure-design-frontend/package.json`: add `test` script and STOMP/SockJS dependencies.
- Modify `data-structure-design-frontend/src/api/travel.js`: add spot vote REST helpers.
- Create `data-structure-design-frontend/src/utils/itineraryVotes.js`: pure consensus and vote normalization utilities.
- Create `data-structure-design-frontend/src/utils/itineraryVotes.test.js`: Node tests for consensus logic.
- Create `data-structure-design-frontend/src/components/itinerary/TacticalMapPanel.vue`: map-style node view.
- Create `data-structure-design-frontend/src/components/itinerary/SpotDecisionCard.vue`: vote card.
- Create `data-structure-design-frontend/src/components/itinerary/SquadPingFeed.vue`: realtime action feed.
- Create `data-structure-design-frontend/src/components/itinerary/ConsensusProgress.vue`: consensus summary.
- Modify `data-structure-design-frontend/src/views/ItineraryView.vue`: integrate tactical collaboration section and WebSocket client.

---

### Task 1: Backend Vote Service Contract

**Files:**
- Create: `data-structure-design-backend/src/test/java/com/travel/system/service/ItinerarySpotVoteServiceTest.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/model/ItinerarySpotVote.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotVoteMessage.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/mapper/ItinerarySpotVoteMapper.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/service/ItinerarySpotVoteService.java`

- [ ] **Step 1: Write the failing service test**

Create `data-structure-design-backend/src/test/java/com/travel/system/service/ItinerarySpotVoteServiceTest.java`:

```java
package com.travel.system.service;

import com.travel.system.dto.ItinerarySpotVoteMessage;
import com.travel.system.mapper.ItinerarySpotVoteMapper;
import com.travel.system.model.ItinerarySpotVote;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItinerarySpotVoteServiceTest {

    private final FakeVoteMapper mapper = new FakeVoteMapper();
    private final ItinerarySpotVoteService service = new ItinerarySpotVoteService(mapper);

    @Test
    void createsVoteWhenUserHasNotVotedForSpot() {
        ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();
        message.setSpotId(101L);
        message.setSpotName("外滩");
        message.setUsername("小周");
        message.setVoteType("must");
        message.setReason("夜景必看");

        ItinerarySpotVote vote = service.saveVote(7L, message);

        assertThat(vote.getId()).isEqualTo(1L);
        assertThat(vote.getItineraryId()).isEqualTo(7L);
        assertThat(vote.getSpotId()).isEqualTo(101L);
        assertThat(vote.getSpotName()).isEqualTo("外滩");
        assertThat(vote.getUsername()).isEqualTo("小周");
        assertThat(vote.getVoteType()).isEqualTo("must");
        assertThat(vote.getReason()).isEqualTo("夜景必看");
        assertThat(vote.getCreatedAt()).isNotNull();
        assertThat(vote.getUpdatedAt()).isNotNull();
    }

    @Test
    void updatesExistingVoteForSameItinerarySpotAndUser() {
        ItinerarySpotVoteMessage first = new ItinerarySpotVoteMessage();
        first.setSpotId(101L);
        first.setSpotName("外滩");
        first.setUsername("小周");
        first.setVoteType("want");
        first.setReason("顺路");
        service.saveVote(7L, first);

        ItinerarySpotVoteMessage second = new ItinerarySpotVoteMessage();
        second.setSpotId(101L);
        second.setSpotName("外滩");
        second.setUsername("小周");
        second.setVoteType("avoid");
        second.setReason("时间太紧");
        ItinerarySpotVote updated = service.saveVote(7L, second);

        assertThat(mapper.rows).hasSize(1);
        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getVoteType()).isEqualTo("avoid");
        assertThat(updated.getReason()).isEqualTo("时间太紧");
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(updated.getCreatedAt());
    }

    @Test
    void rejectsInvalidVoteType() {
        ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();
        message.setSpotId(101L);
        message.setSpotName("外滩");
        message.setUsername("小周");
        message.setVoteType("favorite");

        assertThatThrownBy(() -> service.saveVote(7L, message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("voteType");
    }

    @Test
    void rejectsMissingUsername() {
        ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();
        message.setSpotId(101L);
        message.setSpotName("外滩");
        message.setUsername(" ");
        message.setVoteType("must");

        assertThatThrownBy(() -> service.saveVote(7L, message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }

    private static class FakeVoteMapper implements ItinerarySpotVoteMapper {
        private final List<ItinerarySpotVote> rows = new ArrayList<>();
        private long nextId = 1L;

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
            vote.setId(nextId++);
            rows.add(copy(vote));
        }

        @Override
        public int updateByUnique(ItinerarySpotVote vote) {
            ItinerarySpotVote existing = findByUnique(vote.getItineraryId(), vote.getSpotId(), vote.getUsername());
            if (existing == null) {
                return 0;
            }
            existing.setSpotName(vote.getSpotName());
            existing.setVoteType(vote.getVoteType());
            existing.setReason(vote.getReason());
            existing.setUpdatedAt(vote.getUpdatedAt());
            return 1;
        }

        private ItinerarySpotVote copy(ItinerarySpotVote vote) {
            ItinerarySpotVote copy = new ItinerarySpotVote();
            copy.setId(vote.getId());
            copy.setItineraryId(vote.getItineraryId());
            copy.setSpotId(vote.getSpotId());
            copy.setSpotName(vote.getSpotName());
            copy.setUsername(vote.getUsername());
            copy.setVoteType(vote.getVoteType());
            copy.setReason(vote.getReason());
            copy.setCreatedAt(vote.getCreatedAt());
            copy.setUpdatedAt(vote.getUpdatedAt());
            return copy;
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
./mvnw test -Dtest=ItinerarySpotVoteServiceTest
```

If `mvnw` is unavailable, run:

```powershell
mvn test -Dtest=ItinerarySpotVoteServiceTest
```

Expected: compilation fails because `ItinerarySpotVote`, `ItinerarySpotVoteMessage`, `ItinerarySpotVoteMapper`, and `ItinerarySpotVoteService` do not exist.

- [ ] **Step 3: Add model, DTO, mapper interface, and service**

Create `data-structure-design-backend/src/main/java/com/travel/system/model/ItinerarySpotVote.java`:

```java
package com.travel.system.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ItinerarySpotVote {
    private Long id;
    private Long itineraryId;
    private Long spotId;
    private String spotName;
    private String username;
    private String voteType;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

Create `data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotVoteMessage.java`:

```java
package com.travel.system.dto;

import lombok.Data;

@Data
public class ItinerarySpotVoteMessage {
    private Long itineraryId;
    private Long spotId;
    private String spotName;
    private String username;
    private String voteType;
    private String reason;
}
```

Create `data-structure-design-backend/src/main/java/com/travel/system/mapper/ItinerarySpotVoteMapper.java`:

```java
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
```

Create `data-structure-design-backend/src/main/java/com/travel/system/service/ItinerarySpotVoteService.java`:

```java
package com.travel.system.service;

import com.travel.system.dto.ItinerarySpotVoteMessage;
import com.travel.system.mapper.ItinerarySpotVoteMapper;
import com.travel.system.model.ItinerarySpotVote;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ItinerarySpotVoteService {
    private static final Set<String> ALLOWED_TYPES = Set.of("must", "want", "avoid", "backup");

    private final ItinerarySpotVoteMapper voteMapper;

    public ItinerarySpotVoteService(ItinerarySpotVoteMapper voteMapper) {
        this.voteMapper = voteMapper;
    }

    public List<ItinerarySpotVote> findByItineraryId(Long itineraryId) {
        if (itineraryId == null) {
            throw new IllegalArgumentException("itineraryId is required");
        }
        return voteMapper.findByItineraryId(itineraryId);
    }

    public ItinerarySpotVote saveVote(Long itineraryId, ItinerarySpotVoteMessage message) {
        validate(itineraryId, message);
        String username = message.getUsername().trim();
        String voteType = message.getVoteType().trim();
        LocalDateTime now = LocalDateTime.now();

        ItinerarySpotVote existing = voteMapper.findByUnique(itineraryId, message.getSpotId(), username);
        ItinerarySpotVote vote = new ItinerarySpotVote();
        vote.setItineraryId(itineraryId);
        vote.setSpotId(message.getSpotId());
        vote.setSpotName(message.getSpotName().trim());
        vote.setUsername(username);
        vote.setVoteType(voteType);
        vote.setReason(normalizeReason(message.getReason()));
        vote.setUpdatedAt(now);

        if (existing == null) {
            vote.setCreatedAt(now);
            voteMapper.insert(vote);
        } else {
            vote.setCreatedAt(existing.getCreatedAt());
            voteMapper.updateByUnique(vote);
        }

        return voteMapper.findByUnique(itineraryId, message.getSpotId(), username);
    }

    private void validate(Long itineraryId, ItinerarySpotVoteMessage message) {
        if (itineraryId == null) {
            throw new IllegalArgumentException("itineraryId is required");
        }
        if (message == null) {
            throw new IllegalArgumentException("message is required");
        }
        if (message.getSpotId() == null) {
            throw new IllegalArgumentException("spotId is required");
        }
        if (message.getSpotName() == null || message.getSpotName().isBlank()) {
            throw new IllegalArgumentException("spotName is required");
        }
        if (message.getUsername() == null || message.getUsername().isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (message.getVoteType() == null || !ALLOWED_TYPES.contains(message.getVoteType().trim())) {
            throw new IllegalArgumentException("voteType must be one of must, want, avoid, backup");
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return "";
        }
        return reason.trim();
    }
}
```

- [ ] **Step 4: Run the service test**

Run:

```powershell
mvn test -Dtest=ItinerarySpotVoteServiceTest
```

Expected: `Tests run: 4, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit backend service contract**

```powershell
git add data-structure-design-backend/src/test/java/com/travel/system/service/ItinerarySpotVoteServiceTest.java data-structure-design-backend/src/main/java/com/travel/system/model/ItinerarySpotVote.java data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotVoteMessage.java data-structure-design-backend/src/main/java/com/travel/system/mapper/ItinerarySpotVoteMapper.java data-structure-design-backend/src/main/java/com/travel/system/service/ItinerarySpotVoteService.java
git commit -m "feat: add itinerary spot vote service"
```

---

### Task 2: Backend Persistence and REST Fallback

**Files:**
- Modify: `data-structure-design-backend/src/main/resources/schema-sqlite.sql`
- Create: `data-structure-design-backend/src/main/resources/mapper/ItinerarySpotVoteMapper.xml`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/controller/ItinerarySpotVoteController.java`

- [ ] **Step 1: Add persistence schema**

Modify `data-structure-design-backend/src/main/resources/schema-sqlite.sql` by appending:

```sql
CREATE TABLE IF NOT EXISTS itinerary_spot_vote (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    itinerary_id INTEGER NOT NULL,
    spot_id INTEGER NOT NULL,
    spot_name TEXT NOT NULL,
    username TEXT NOT NULL,
    vote_type TEXT NOT NULL,
    reason TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_itinerary_spot_vote_unique
ON itinerary_spot_vote(itinerary_id, spot_id, username);
```

- [ ] **Step 2: Add MyBatis SQL mapper**

Create `data-structure-design-backend/src/main/resources/mapper/ItinerarySpotVoteMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.travel.system.mapper.ItinerarySpotVoteMapper">

    <select id="findByItineraryId" parameterType="long" resultType="com.travel.system.model.ItinerarySpotVote">
        SELECT id,
               itinerary_id AS itineraryId,
               spot_id AS spotId,
               spot_name AS spotName,
               username,
               vote_type AS voteType,
               reason,
               created_at AS createdAt,
               updated_at AS updatedAt
        FROM itinerary_spot_vote
        WHERE itinerary_id = #{itineraryId}
        ORDER BY updated_at DESC, id DESC
    </select>

    <select id="findByUnique" resultType="com.travel.system.model.ItinerarySpotVote">
        SELECT id,
               itinerary_id AS itineraryId,
               spot_id AS spotId,
               spot_name AS spotName,
               username,
               vote_type AS voteType,
               reason,
               created_at AS createdAt,
               updated_at AS updatedAt
        FROM itinerary_spot_vote
        WHERE itinerary_id = #{itineraryId}
          AND spot_id = #{spotId}
          AND username = #{username}
    </select>

    <insert id="insert" parameterType="com.travel.system.model.ItinerarySpotVote"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO itinerary_spot_vote(
            itinerary_id, spot_id, spot_name, username, vote_type, reason, created_at, updated_at
        )
        VALUES(
            #{itineraryId}, #{spotId}, #{spotName}, #{username}, #{voteType}, #{reason}, #{createdAt}, #{updatedAt}
        )
    </insert>

    <update id="updateByUnique" parameterType="com.travel.system.model.ItinerarySpotVote">
        UPDATE itinerary_spot_vote
        SET spot_name = #{spotName},
            vote_type = #{voteType},
            reason = #{reason},
            updated_at = #{updatedAt}
        WHERE itinerary_id = #{itineraryId}
          AND spot_id = #{spotId}
          AND username = #{username}
    </update>

</mapper>
```

- [ ] **Step 3: Add REST controller**

Create `data-structure-design-backend/src/main/java/com/travel/system/controller/ItinerarySpotVoteController.java`:

```java
package com.travel.system.controller;

import com.travel.system.dto.ItinerarySpotVoteMessage;
import com.travel.system.model.ItinerarySpotVote;
import com.travel.system.service.ItinerarySpotVoteService;
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
@RequestMapping("/api/itineraries/{itineraryId}/spot-votes")
public class ItinerarySpotVoteController {
    private final ItinerarySpotVoteService voteService;

    public ItinerarySpotVoteController(ItinerarySpotVoteService voteService) {
        this.voteService = voteService;
    }

    @GetMapping
    public List<ItinerarySpotVote> list(@PathVariable Long itineraryId) {
        return voteService.findByItineraryId(itineraryId);
    }

    @PostMapping
    public ItinerarySpotVote vote(@PathVariable Long itineraryId,
                                  @RequestBody ItinerarySpotVoteMessage message) {
        try {
            return voteService.saveVote(itineraryId, message);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
```

- [ ] **Step 4: Run backend tests**

Run:

```powershell
mvn test
```

Expected: all existing tests and `ItinerarySpotVoteServiceTest` pass.

- [ ] **Step 5: Commit persistence and REST fallback**

```powershell
git add data-structure-design-backend/src/main/resources/schema-sqlite.sql data-structure-design-backend/src/main/resources/mapper/ItinerarySpotVoteMapper.xml data-structure-design-backend/src/main/java/com/travel/system/controller/ItinerarySpotVoteController.java
git commit -m "feat: add spot vote persistence api"
```

---

### Task 3: WebSocket Vote Broadcasting

**Files:**
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotVoteBroadcastMessage.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/controller/ItineraryVoteWebSocketController.java`
- Create: `data-structure-design-backend/src/test/java/com/travel/system/controller/ItineraryVoteWebSocketControllerTest.java`

- [ ] **Step 1: Write failing WebSocket controller test**

Create `data-structure-design-backend/src/test/java/com/travel/system/controller/ItineraryVoteWebSocketControllerTest.java`:

```java
package com.travel.system.controller;

import com.travel.system.dto.ItinerarySpotVoteBroadcastMessage;
import com.travel.system.dto.ItinerarySpotVoteMessage;
import com.travel.system.model.ItinerarySpotVote;
import com.travel.system.service.ItinerarySpotVoteService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItineraryVoteWebSocketControllerTest {

    @Test
    void broadcastsUpdatedVoteAfterSave() {
        ItinerarySpotVoteService service = mock(ItinerarySpotVoteService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ItineraryVoteWebSocketController controller = new ItineraryVoteWebSocketController(service, messagingTemplate);

        ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();
        message.setSpotId(101L);
        message.setSpotName("外滩");
        message.setUsername("小周");
        message.setVoteType("must");
        message.setReason("夜景必看");

        ItinerarySpotVote saved = new ItinerarySpotVote();
        saved.setId(1L);
        saved.setItineraryId(7L);
        saved.setSpotId(101L);
        saved.setSpotName("外滩");
        saved.setUsername("小周");
        saved.setVoteType("must");
        saved.setReason("夜景必看");
        saved.setCreatedAt(LocalDateTime.now());
        saved.setUpdatedAt(LocalDateTime.now());
        when(service.saveVote(7L, message)).thenReturn(saved);
        when(service.findByItineraryId(7L)).thenReturn(List.of(saved));

        controller.vote(7L, message);

        verify(messagingTemplate).convertAndSend(eq("/topic/itinerary/7"),
                org.mockito.ArgumentMatchers.argThat(payload -> {
                    ItinerarySpotVoteBroadcastMessage broadcast = (ItinerarySpotVoteBroadcastMessage) payload;
                    return broadcast.getType() == ItinerarySpotVoteBroadcastMessage.Type.SPOT_VOTE_UPDATED
                            && broadcast.getVote() == saved
                            && broadcast.getVotes().size() == 1
                            && "小周".equals(broadcast.getUsername());
                }));
    }

    @Test
    void broadcastsRejectedVoteWhenValidationFails() {
        ItinerarySpotVoteService service = mock(ItinerarySpotVoteService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ItineraryVoteWebSocketController controller = new ItineraryVoteWebSocketController(service, messagingTemplate);

        ItinerarySpotVoteMessage message = new ItinerarySpotVoteMessage();
        message.setUsername("小周");
        when(service.saveVote(7L, message)).thenThrow(new IllegalArgumentException("voteType must be one of must, want, avoid, backup"));

        controller.vote(7L, message);

        verify(messagingTemplate).convertAndSend(eq("/topic/itinerary/7"),
                org.mockito.ArgumentMatchers.argThat(payload -> {
                    ItinerarySpotVoteBroadcastMessage broadcast = (ItinerarySpotVoteBroadcastMessage) payload;
                    return broadcast.getType() == ItinerarySpotVoteBroadcastMessage.Type.SPOT_VOTE_REJECTED
                            && broadcast.getMessage().contains("voteType")
                            && "小周".equals(broadcast.getUsername());
                }));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
mvn test -Dtest=ItineraryVoteWebSocketControllerTest
```

Expected: compilation fails because `ItinerarySpotVoteBroadcastMessage` and `ItineraryVoteWebSocketController` do not exist.

- [ ] **Step 3: Add broadcast DTO and WebSocket controller**

Create `data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotVoteBroadcastMessage.java`:

```java
package com.travel.system.dto;

import com.travel.system.model.ItinerarySpotVote;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItinerarySpotVoteBroadcastMessage {
    public enum Type { SPOT_VOTE_UPDATED, SPOT_VOTE_REJECTED }

    private Type type;
    private String username;
    private ItinerarySpotVote vote;
    private List<ItinerarySpotVote> votes;
    private String message;
    private LocalDateTime serverTimestamp;
}
```

Create `data-structure-design-backend/src/main/java/com/travel/system/controller/ItineraryVoteWebSocketController.java`:

```java
package com.travel.system.controller;

import com.travel.system.dto.ItinerarySpotVoteBroadcastMessage;
import com.travel.system.dto.ItinerarySpotVoteMessage;
import com.travel.system.model.ItinerarySpotVote;
import com.travel.system.service.ItinerarySpotVoteService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class ItineraryVoteWebSocketController {
    private final ItinerarySpotVoteService voteService;
    private final SimpMessagingTemplate messagingTemplate;

    public ItineraryVoteWebSocketController(ItinerarySpotVoteService voteService,
                                            SimpMessagingTemplate messagingTemplate) {
        this.voteService = voteService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/itinerary/{id}/spot-vote")
    public void vote(@DestinationVariable Long id, ItinerarySpotVoteMessage message) {
        try {
            message.setItineraryId(id);
            ItinerarySpotVote saved = voteService.saveVote(id, message);
            List<ItinerarySpotVote> votes = voteService.findByItineraryId(id);
            messagingTemplate.convertAndSend("/topic/itinerary/" + id,
                    new ItinerarySpotVoteBroadcastMessage(
                            ItinerarySpotVoteBroadcastMessage.Type.SPOT_VOTE_UPDATED,
                            saved.getUsername(),
                            saved,
                            votes,
                            null,
                            LocalDateTime.now()));
        } catch (IllegalArgumentException ex) {
            String username = message == null ? "" : message.getUsername();
            messagingTemplate.convertAndSend("/topic/itinerary/" + id,
                    new ItinerarySpotVoteBroadcastMessage(
                            ItinerarySpotVoteBroadcastMessage.Type.SPOT_VOTE_REJECTED,
                            username,
                            null,
                            List.of(),
                            ex.getMessage(),
                            LocalDateTime.now()));
        }
    }
}
```

- [ ] **Step 4: Run WebSocket controller tests**

Run:

```powershell
mvn test -Dtest=ItineraryVoteWebSocketControllerTest
```

Expected: `Tests run: 2, Failures: 0, Errors: 0`.

- [ ] **Step 5: Run all backend tests**

Run:

```powershell
mvn test
```

Expected: all backend tests pass.

- [ ] **Step 6: Commit WebSocket broadcasting**

```powershell
git add data-structure-design-backend/src/main/java/com/travel/system/dto/ItinerarySpotVoteBroadcastMessage.java data-structure-design-backend/src/main/java/com/travel/system/controller/ItineraryVoteWebSocketController.java data-structure-design-backend/src/test/java/com/travel/system/controller/ItineraryVoteWebSocketControllerTest.java
git commit -m "feat: broadcast itinerary spot votes"
```

---

### Task 4: Frontend Vote Utilities and Tests

**Files:**
- Modify: `data-structure-design-frontend/package.json`
- Create: `data-structure-design-frontend/src/utils/itineraryVotes.js`
- Create: `data-structure-design-frontend/src/utils/itineraryVotes.test.js`

- [ ] **Step 1: Add frontend test script**

Modify `data-structure-design-frontend/package.json` scripts:

```json
"scripts": {
  "dev": "npx vite",
  "build": "npx vite build",
  "preview": "npx vite preview",
  "test": "node --test src/**/*.test.js"
}
```

- [ ] **Step 2: Write failing utility tests**

Create `data-structure-design-frontend/src/utils/itineraryVotes.test.js`:

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildSpotNodes,
  computeConsensus,
  summarizeConsensus,
  voteTypeLabel,
} from './itineraryVotes.js'

test('computeConsensus returns must when must votes are the majority', () => {
  const result = computeConsensus([
    { username: '小周', voteType: 'must' },
    { username: '阿杰', voteType: 'must' },
    { username: '林同学', voteType: 'want' },
  ])

  assert.equal(result, 'must')
})

test('computeConsensus returns conflict when avoid votes are significant', () => {
  const result = computeConsensus([
    { username: '小周', voteType: 'avoid' },
    { username: '阿杰', voteType: 'avoid' },
    { username: '林同学', voteType: 'must' },
  ])

  assert.equal(result, 'conflict')
})

test('buildSpotNodes groups votes by spot and keeps stable coordinates', () => {
  const nodes = buildSpotNodes([
    { spotId: 101, spotName: '外滩', username: '小周', voteType: 'must' },
    { spotId: 101, spotName: '外滩', username: '阿杰', voteType: 'want' },
    { spotId: 102, spotName: '南京路', username: '小周', voteType: 'backup' },
  ])

  assert.equal(nodes.length, 2)
  assert.equal(nodes[0].spotName, '外滩')
  assert.equal(nodes[0].votes.length, 2)
  assert.equal(nodes[0].consensus, 'want')
  assert.equal(nodes[0].x, 18)
  assert.equal(nodes[0].y, 28)
})

test('summarizeConsensus counts agreed conflict and must nodes', () => {
  const summary = summarizeConsensus([
    { consensus: 'must' },
    { consensus: 'want' },
    { consensus: 'conflict' },
    { consensus: 'backup' },
  ])

  assert.deepEqual(summary, {
    total: 4,
    agreed: 2,
    conflicts: 1,
    must: 1,
  })
})

test('voteTypeLabel returns Chinese UI labels', () => {
  assert.equal(voteTypeLabel('must'), '必去')
  assert.equal(voteTypeLabel('want'), '想去')
  assert.equal(voteTypeLabel('avoid'), '不想去')
  assert.equal(voteTypeLabel('backup'), '备选')
  assert.equal(voteTypeLabel('unknown'), '未标记')
})
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```powershell
npm test
```

Expected: fails because `src/utils/itineraryVotes.js` does not exist.

- [ ] **Step 4: Implement vote utilities**

Create `data-structure-design-frontend/src/utils/itineraryVotes.js`:

```js
export const VOTE_TYPES = ['must', 'want', 'avoid', 'backup']

const DEFAULT_COORDS = [
  { x: 18, y: 28 },
  { x: 39, y: 58 },
  { x: 63, y: 34 },
  { x: 78, y: 68 },
  { x: 50, y: 18 },
  { x: 25, y: 74 },
]

export const voteTypeLabel = (type) => ({
  must: '必去',
  want: '想去',
  avoid: '不想去',
  backup: '备选',
}[type] || '未标记')

export const voteTypeTone = (type) => ({
  must: 'danger',
  want: 'success',
  avoid: 'warning',
  backup: 'info',
}[type] || 'info')

export const computeConsensus = (votes = []) => {
  if (!votes.length) return 'backup'
  const counts = votes.reduce((acc, vote) => {
    const type = vote.voteType || vote.type
    acc[type] = (acc[type] || 0) + 1
    return acc
  }, {})
  const total = votes.length
  if ((counts.avoid || 0) >= 2 || (counts.avoid || 0) / total >= 0.5) return 'conflict'
  if ((counts.must || 0) / total > 0.5) return 'must'
  if (((counts.must || 0) + (counts.want || 0)) / total > 0.5) return 'want'
  return 'backup'
}

export const buildSpotNodes = (votes = []) => {
  const grouped = new Map()
  votes.forEach((vote) => {
    const spotId = vote.spotId
    if (!grouped.has(spotId)) {
      grouped.set(spotId, {
        spotId,
        spotName: vote.spotName || `景点 ${spotId}`,
        votes: [],
      })
    }
    grouped.get(spotId).votes.push(vote)
  })

  return Array.from(grouped.values()).map((node, index) => {
    const coord = DEFAULT_COORDS[index % DEFAULT_COORDS.length]
    return {
      ...node,
      x: coord.x,
      y: coord.y,
      consensus: computeConsensus(node.votes),
    }
  })
}

export const summarizeConsensus = (nodes = []) => ({
  total: nodes.length,
  agreed: nodes.filter((node) => ['must', 'want'].includes(node.consensus)).length,
  conflicts: nodes.filter((node) => node.consensus === 'conflict').length,
  must: nodes.filter((node) => node.consensus === 'must').length,
})

export const makePingText = (vote) => {
  if (!vote) return ''
  return `${vote.username || '队友'}标记“${vote.spotName || '景点'}”为${voteTypeLabel(vote.voteType || vote.type)}`
}
```

- [ ] **Step 5: Run frontend utility tests**

Run:

```powershell
npm test
```

Expected: all utility tests pass.

- [ ] **Step 6: Commit frontend utilities**

```powershell
git add data-structure-design-frontend/package.json data-structure-design-frontend/src/utils/itineraryVotes.js data-structure-design-frontend/src/utils/itineraryVotes.test.js
git commit -m "feat: add itinerary vote utilities"
```

---

### Task 5: Frontend Tactical Map Components

**Files:**
- Create: `data-structure-design-frontend/src/components/itinerary/TacticalMapPanel.vue`
- Create: `data-structure-design-frontend/src/components/itinerary/SpotDecisionCard.vue`
- Create: `data-structure-design-frontend/src/components/itinerary/SquadPingFeed.vue`
- Create: `data-structure-design-frontend/src/components/itinerary/ConsensusProgress.vue`

- [ ] **Step 1: Create `TacticalMapPanel.vue`**

Create `data-structure-design-frontend/src/components/itinerary/TacticalMapPanel.vue`:

```vue
<script setup>
import { computed } from 'vue'
import { voteTypeLabel } from '../../utils/itineraryVotes'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  selectedSpotId: { type: [Number, String], default: null },
})

const emit = defineEmits(['select-node'])

const lines = computed(() => props.nodes.slice(1).map((node, index) => ({
  key: `${props.nodes[index].spotId}-${node.spotId}`,
  x1: props.nodes[index].x,
  y1: props.nodes[index].y,
  x2: node.x,
  y2: node.y,
})))

const nodeClass = (node) => [
  'tactical-node',
  `node-${node.consensus || 'backup'}`,
  { active: String(node.spotId) === String(props.selectedSpotId) },
]
</script>

<template>
  <section class="tactical-map-panel">
    <div class="map-copy">
      <span class="map-eyebrow">协作战术地图</span>
      <h3>像游戏 ping 地图一样共同选择景点</h3>
      <p>点击节点查看队友投票，并快速标记必去、想去、不想去或备选。</p>
    </div>
    <div class="map-stage">
      <svg class="route-lines" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
        <line
          v-for="line in lines"
          :key="line.key"
          :x1="line.x1"
          :y1="line.y1"
          :x2="line.x2"
          :y2="line.y2"
        />
      </svg>
      <button
        v-for="node in nodes"
        :key="node.spotId"
        type="button"
        :class="nodeClass(node)"
        :style="{ left: `${node.x}%`, top: `${node.y}%` }"
        @click="emit('select-node', node)"
      >
        <strong>{{ node.spotName }}</strong>
        <span>{{ voteTypeLabel(node.consensus === 'conflict' ? 'avoid' : node.consensus) }}</span>
        <small>{{ node.votes.length }} 票</small>
      </button>
      <div v-if="!nodes.length" class="empty-map">暂无投票节点，先为一个景点发起投票。</div>
    </div>
  </section>
</template>

<style scoped>
.tactical-map-panel {
  display: grid;
  grid-template-columns: minmax(220px, 0.42fr) minmax(0, 1fr);
  gap: 18px;
  padding: 22px;
  border-radius: 24px;
  background: linear-gradient(135deg, #11151c, #1d2b3a);
  color: #f8fafc;
}
.map-eyebrow {
  display: inline-block;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 56, 92, 0.16);
  color: #ff8ba0;
  font-size: 12px;
  font-weight: 900;
}
.map-copy h3 {
  margin: 12px 0 8px;
  font-size: 24px;
}
.map-copy p {
  color: #a7b0bf;
  line-height: 1.7;
}
.map-stage {
  position: relative;
  min-height: 360px;
  overflow: hidden;
  border-radius: 22px;
  background:
    radial-gradient(circle at 18% 28%, rgba(255, 56, 92, 0.18), transparent 18%),
    radial-gradient(circle at 70% 72%, rgba(243, 208, 138, 0.18), transparent 20%),
    linear-gradient(135deg, #e6f5ed, #fff4d9);
}
.route-lines {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
.route-lines line {
  stroke: rgba(34, 34, 34, 0.42);
  stroke-width: 1.4;
  stroke-linecap: round;
}
.tactical-node {
  position: absolute;
  transform: translate(-50%, -50%);
  min-width: 108px;
  border: 3px solid #ffffff;
  border-radius: 18px;
  padding: 10px 12px;
  color: #ffffff;
  box-shadow: 0 18px 38px rgba(0, 0, 0, 0.22);
  cursor: pointer;
}
.tactical-node strong,
.tactical-node span,
.tactical-node small {
  display: block;
}
.tactical-node strong {
  font-size: 14px;
}
.tactical-node span,
.tactical-node small {
  margin-top: 3px;
  font-size: 12px;
}
.node-must { background: #ff385c; }
.node-want { background: #0f766e; }
.node-conflict { background: #f59e0b; }
.node-backup { background: #64748b; }
.tactical-node.active {
  outline: 4px solid rgba(255, 255, 255, 0.66);
}
.empty-map {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #475569;
  font-weight: 800;
}
@media (max-width: 900px) {
  .tactical-map-panel {
    grid-template-columns: 1fr;
  }
}
</style>
```

- [ ] **Step 2: Create `SpotDecisionCard.vue`**

Create `data-structure-design-frontend/src/components/itinerary/SpotDecisionCard.vue`:

```vue
<script setup>
import { computed, ref, watch } from 'vue'
import { voteTypeLabel, voteTypeTone, VOTE_TYPES } from '../../utils/itineraryVotes'

const props = defineProps({
  node: { type: Object, default: null },
  currentUser: { type: String, default: '' },
  saving: { type: Boolean, default: false },
})

const emit = defineEmits(['submit-vote'])
const reason = ref('')

const myVote = computed(() => props.node?.votes?.find((vote) => vote.username === props.currentUser))

watch(() => props.node?.spotId, () => {
  reason.value = myVote.value?.reason || ''
})

const submit = (voteType) => {
  if (!props.node) return
  emit('submit-vote', {
    spotId: props.node.spotId,
    spotName: props.node.spotName,
    voteType,
    reason: reason.value,
  })
}
</script>

<template>
  <aside class="decision-card">
    <template v-if="node">
      <div class="decision-head">
        <span>景点决策卡</span>
        <strong>{{ node.spotName }}</strong>
        <small>当前状态：{{ voteTypeLabel(node.consensus === 'conflict' ? 'avoid' : node.consensus) }}</small>
      </div>
      <el-input v-model="reason" type="textarea" :rows="3" placeholder="补一句理由，例如：夜景必看、太绕路、适合备选" />
      <div class="vote-buttons">
        <el-button
          v-for="type in VOTE_TYPES"
          :key="type"
          :type="voteTypeTone(type)"
          :loading="saving"
          @click="submit(type)"
        >
          {{ voteTypeLabel(type) }}
        </el-button>
      </div>
      <div class="vote-list">
        <div v-for="vote in node.votes" :key="`${vote.username}-${vote.voteType}`" class="vote-row">
          <strong>{{ vote.username }}</strong>
          <span>{{ voteTypeLabel(vote.voteType) }}</span>
          <small>{{ vote.reason || '没有填写理由' }}</small>
        </div>
      </div>
    </template>
    <div v-else class="decision-empty">选择一个地图节点后查看投票。</div>
  </aside>
</template>

<style scoped>
.decision-card {
  min-height: 100%;
  padding: 18px;
  border-radius: 20px;
  background: #ffffff;
  border: 1px solid rgba(148, 163, 184, 0.22);
}
.decision-head span,
.decision-head strong,
.decision-head small {
  display: block;
}
.decision-head span {
  color: #ff385c;
  font-size: 12px;
  font-weight: 900;
}
.decision-head strong {
  margin-top: 6px;
  color: #222222;
  font-size: 22px;
}
.decision-head small {
  margin: 6px 0 14px;
  color: #64748b;
}
.vote-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.vote-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}
.vote-row {
  display: grid;
  gap: 3px;
  padding: 10px;
  border-radius: 12px;
  background: #f8fafc;
}
.vote-row strong {
  color: #222222;
}
.vote-row span {
  color: #ff385c;
  font-weight: 800;
}
.vote-row small {
  color: #64748b;
}
.decision-empty {
  display: grid;
  min-height: 220px;
  place-items: center;
  color: #64748b;
  font-weight: 800;
}
</style>
```

- [ ] **Step 3: Create `SquadPingFeed.vue`**

Create `data-structure-design-frontend/src/components/itinerary/SquadPingFeed.vue`:

```vue
<script setup>
import { makePingText } from '../../utils/itineraryVotes'

defineProps({
  events: { type: Array, default: () => [] },
})
</script>

<template>
  <section class="ping-feed">
    <h3>队友 Ping</h3>
    <div v-if="events.length" class="ping-list">
      <div v-for="event in events" :key="event.key || `${event.username}-${event.serverTimestamp}`" class="ping-item">
        <span>{{ event.username?.slice(0, 1) || '队' }}</span>
        <p>{{ event.text || makePingText(event.vote || event) }}</p>
      </div>
    </div>
    <div v-else class="ping-empty">暂无队友动作。</div>
  </section>
</template>

<style scoped>
.ping-feed {
  padding: 18px;
  border-radius: 20px;
  background: #17191d;
  color: #f8fafc;
}
.ping-feed h3 {
  margin: 0 0 12px;
}
.ping-list {
  display: grid;
  gap: 10px;
}
.ping-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.08);
}
.ping-item span {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #ff385c;
  font-weight: 900;
}
.ping-item p {
  margin: 0;
  color: #d7dce5;
}
.ping-empty {
  color: #a7b0bf;
}
</style>
```

- [ ] **Step 4: Create `ConsensusProgress.vue`**

Create `data-structure-design-frontend/src/components/itinerary/ConsensusProgress.vue`:

```vue
<script setup>
import { computed } from 'vue'
import { summarizeConsensus } from '../../utils/itineraryVotes'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
})

const summary = computed(() => summarizeConsensus(props.nodes))
const percent = computed(() => summary.value.total ? Math.round((summary.value.agreed / summary.value.total) * 100) : 0)
</script>

<template>
  <section class="consensus-progress">
    <div>
      <span>团队共识进度</span>
      <strong>{{ summary.agreed }} / {{ summary.total }}</strong>
      <small>{{ summary.conflicts }} 个景点存在分歧，{{ summary.must }} 个景点被标记为必去</small>
    </div>
    <el-progress type="circle" :percentage="percent" :width="86" color="#ff385c" />
  </section>
</template>

<style scoped>
.consensus-progress {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(135deg, #fff8f8, #fff1f4);
  border: 1px solid #ffd8e1;
}
.consensus-progress span,
.consensus-progress strong,
.consensus-progress small {
  display: block;
}
.consensus-progress span {
  color: #ff385c;
  font-size: 12px;
  font-weight: 900;
}
.consensus-progress strong {
  margin-top: 4px;
  color: #222222;
  font-size: 28px;
}
.consensus-progress small {
  margin-top: 4px;
  color: #64748b;
}
</style>
```

- [ ] **Step 5: Run frontend tests and build**

Run:

```powershell
npm test
npm run build
```

Expected: utility tests pass and Vite build completes.

- [ ] **Step 6: Commit tactical map components**

```powershell
git add data-structure-design-frontend/src/components/itinerary/TacticalMapPanel.vue data-structure-design-frontend/src/components/itinerary/SpotDecisionCard.vue data-structure-design-frontend/src/components/itinerary/SquadPingFeed.vue data-structure-design-frontend/src/components/itinerary/ConsensusProgress.vue
git commit -m "feat: add tactical map components"
```

---

### Task 6: Frontend API and Itinerary Page Integration

**Files:**
- Modify: `data-structure-design-frontend/package.json`
- Modify: `data-structure-design-frontend/src/api/travel.js`
- Modify: `data-structure-design-frontend/src/views/ItineraryView.vue`

- [ ] **Step 1: Install realtime dependencies**

Run:

```powershell
npm install @stomp/stompjs sockjs-client
```

Expected: `package.json` and `package-lock.json` include `@stomp/stompjs` and `sockjs-client`.

- [ ] **Step 2: Add spot vote API helpers**

Modify `data-structure-design-frontend/src/api/travel.js` by adding below the itinerary APIs:

```js
export const listItinerarySpotVotes = (id) => http.get(`/itineraries/${id}/spot-votes`)
export const submitItinerarySpotVote = (id, payload) => http.post(`/itineraries/${id}/spot-votes`, payload)
```

- [ ] **Step 3: Integrate tactical collaboration state**

Modify the `<script setup>` imports in `data-structure-design-frontend/src/views/ItineraryView.vue`:

```js
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAppStore } from '../stores/app'
import TacticalMapPanel from '../components/itinerary/TacticalMapPanel.vue'
import SpotDecisionCard from '../components/itinerary/SpotDecisionCard.vue'
import SquadPingFeed from '../components/itinerary/SquadPingFeed.vue'
import ConsensusProgress from '../components/itinerary/ConsensusProgress.vue'
import { buildSpotNodes, makePingText } from '../utils/itineraryVotes'
import {
  createItinerary,
  listItineraries,
  listItinerarySpotVotes,
  submitItinerarySpotVote,
  updateItinerary,
} from '../api/travel'
```

Add state after existing refs:

```js
const appStore = useAppStore()
const activeCollabRow = ref(null)
const spotVotes = ref([])
const selectedNode = ref(null)
const voteSaving = ref(false)
const pingEvents = ref([])
const stompClient = ref(null)
const wsConnected = ref(false)
```

Add computed values:

```js
const currentUsername = computed(() => appStore.user.name || '')
const tacticalNodes = computed(() => buildSpotNodes(spotVotes.value))
const selectedSpotId = computed(() => selectedNode.value?.spotId || null)
```

Add collaboration methods:

```js
const loadSpotVotes = async (row) => {
  if (!row?.id) return
  const { data } = await listItinerarySpotVotes(row.id)
  spotVotes.value = Array.isArray(data) ? data : []
  selectedNode.value = tacticalNodes.value[0] || null
}

const openTacticalMap = async (row) => {
  activeCollabRow.value = row
  await loadSpotVotes(row)
  await nextTick()
  connectVoteSocket(row.id)
}

const selectNode = (node) => {
  selectedNode.value = node
}

const pushPing = (payload) => {
  const vote = payload.vote || payload
  pingEvents.value = [{
    key: `${Date.now()}-${vote.username}-${vote.spotId}`,
    username: vote.username,
    vote,
    text: makePingText(vote),
    serverTimestamp: payload.serverTimestamp,
  }, ...pingEvents.value].slice(0, 8)
}

const applyVoteBroadcast = (payload) => {
  if (payload.type === 'SPOT_VOTE_REJECTED') {
    ElMessage.warning(payload.message || '投票未保存')
    return
  }
  if (payload.type !== 'SPOT_VOTE_UPDATED') return
  spotVotes.value = Array.isArray(payload.votes) ? payload.votes : spotVotes.value
  pushPing(payload)
  selectedNode.value = tacticalNodes.value.find((node) => node.spotId === selectedSpotId.value) || tacticalNodes.value[0] || null
}

const connectVoteSocket = (itineraryId) => {
  if (stompClient.value) {
    stompClient.value.deactivate()
  }
  const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  const wsBase = apiBase.replace(/\/api\/?$/, '')
  const client = new Client({
    webSocketFactory: () => new SockJS(`${wsBase}/ws`),
    reconnectDelay: 4000,
    onConnect: () => {
      wsConnected.value = true
      client.subscribe(`/topic/itinerary/${itineraryId}`, (message) => {
        applyVoteBroadcast(JSON.parse(message.body))
      })
    },
    onWebSocketClose: () => {
      wsConnected.value = false
    },
    onStompError: () => {
      wsConnected.value = false
    },
  })
  stompClient.value = client
  client.activate()
}

const submitVote = async ({ spotId, spotName, voteType, reason }) => {
  if (!activeCollabRow.value?.id) return
  if (!currentUsername.value) {
    ElMessage.warning('请先登录后参与协作')
    return
  }
  voteSaving.value = true
  const payload = {
    spotId,
    spotName,
    username: currentUsername.value,
    voteType,
    reason,
  }
  try {
    if (wsConnected.value && stompClient.value) {
      stompClient.value.publish({
        destination: `/app/itinerary/${activeCollabRow.value.id}/spot-vote`,
        body: JSON.stringify(payload),
      })
    } else {
      const { data } = await submitItinerarySpotVote(activeCollabRow.value.id, payload)
      await loadSpotVotes(activeCollabRow.value)
      pushPing(data)
      ElMessage.success('投票已保存')
    }
  } finally {
    voteSaving.value = false
  }
}

onBeforeUnmount(() => {
  if (stompClient.value) {
    stompClient.value.deactivate()
  }
})
```

- [ ] **Step 4: Add tactical map action to each itinerary row**

Inside `.itinerary-actions`, add this button before detail:

```vue
<button type="button" @click="openTacticalMap(row)">
  <People theme="outline" size="14" fill="currentColor" /> 战术图
</button>
```

- [ ] **Step 5: Add tactical collaboration template section**

Add this section after the stats row and before the list card:

```vue
<el-card v-if="activeCollabRow" class="module-card tactical-card">
  <div class="tactical-layout">
    <TacticalMapPanel
      :nodes="tacticalNodes"
      :selected-spot-id="selectedSpotId"
      @select-node="selectNode"
    />
    <div class="tactical-side">
      <ConsensusProgress :nodes="tacticalNodes" />
      <SpotDecisionCard
        :node="selectedNode"
        :current-user="currentUsername"
        :saving="voteSaving"
        @submit-vote="submitVote"
      />
      <SquadPingFeed :events="pingEvents" />
    </div>
  </div>
</el-card>
```

- [ ] **Step 6: Add tactical layout styles**

Append to `ItineraryView.vue` scoped CSS:

```css
.tactical-card {
  overflow: hidden;
}

.tactical-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
}

.tactical-side {
  display: grid;
  gap: 14px;
}

@media (max-width: 1100px) {
  .tactical-layout {
    grid-template-columns: 1fr;
  }
}
```

- [ ] **Step 7: Run frontend verification**

Run:

```powershell
npm test
npm run build
```

Expected: frontend tests pass and Vite build completes.

- [ ] **Step 8: Commit frontend integration**

```powershell
git add data-structure-design-frontend/package.json data-structure-design-frontend/package-lock.json data-structure-design-frontend/src/api/travel.js data-structure-design-frontend/src/views/ItineraryView.vue
git commit -m "feat: integrate tactical map voting"
```

---

### Task 7: End-to-End Verification and Documentation

**Files:**
- Modify: `docs/demo-runbook.md`

- [ ] **Step 1: Add demo runbook section**

Append to `docs/demo-runbook.md`:

```markdown
## Tactical Map Collaboration Demo

1. Start the backend and frontend.
2. Log in with a demo user.
3. Open the itinerary page.
4. Choose an itinerary and click `战术图`.
5. Select a map node and vote `必去`.
6. Open a second browser session with another demo user.
7. Vote `不想去` for the same node.
8. Confirm the node changes to a conflict state and the Ping panel shows both actions.
9. Disconnect the backend WebSocket or refresh during reconnect, then submit a vote and confirm the REST fallback saves it.
```

- [ ] **Step 2: Run backend verification**

Run:

```powershell
cd data-structure-design-backend
mvn test
```

Expected: all backend tests pass.

- [ ] **Step 3: Run frontend verification**

Run:

```powershell
cd data-structure-design-frontend
npm test
npm run build
```

Expected: utility tests pass and Vite build completes.

- [ ] **Step 4: Check git diff**

Run:

```powershell
git status --short
git diff --stat HEAD
```

Expected: only intended backend, frontend, and runbook files are changed since the previous commit.

- [ ] **Step 5: Commit verification docs**

```powershell
git add docs/demo-runbook.md
git commit -m "docs: add tactical map collaboration demo"
```

---

## Self-Review

Spec coverage:

- Tactical map visualization: Task 5 and Task 6.
- Spot voting types `must`, `want`, `avoid`, `backup`: Task 1 and Task 4.
- REST fallback: Task 2 and Task 6.
- WebSocket realtime broadcast: Task 3 and Task 6.
- Ping feed: Task 5 and Task 6.
- Consensus progress: Task 4, Task 5, and Task 6.
- Duplicate vote upsert: Task 1 and Task 2.
- Error handling for missing user, invalid type, missing spot data, and WebSocket disconnect: Task 1, Task 3, and Task 6.
- Tests for service, broadcast, consensus, build, and demo flow: Tasks 1, 3, 4, and 7.

Type consistency:

- Backend vote type field is `voteType` in Java DTO/model and frontend API payload.
- Backend message route is `/app/itinerary/{id}/spot-vote`; broadcast topic is `/topic/itinerary/{id}`.
- Frontend node fields are `spotId`, `spotName`, `votes`, `x`, `y`, and `consensus`.
- Broadcast success type is `SPOT_VOTE_UPDATED`; rejection type is `SPOT_VOTE_REJECTED`.
