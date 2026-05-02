package com.travel.system.service;

import com.travel.system.dto.ItineraryBroadcastMessage;
import com.travel.system.dto.ItineraryEditMessage;
import com.travel.system.mapper.ItineraryMapper;
import com.travel.system.model.Itinerary;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ItineraryService {

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ItineraryMapper itineraryMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public ItineraryService(ItineraryMapper itineraryMapper,
                            SimpMessagingTemplate messagingTemplate) {
        this.itineraryMapper = itineraryMapper;
        this.messagingTemplate = messagingTemplate;
    }

    public List<Itinerary> findAll() {
        return itineraryMapper.findAll();
    }

    public Itinerary findById(Long id) {
        return itineraryMapper.findById(id);
    }

    public Itinerary create(Itinerary itinerary) {
        itinerary.setUpdatedAt(LocalDateTime.now());
        return itineraryMapper.save(itinerary);
    }

    /**
     * REST PUT 更新，返回 null 表示冲突或不存在
     */
    public Itinerary update(Long id, Itinerary itinerary) {
        Itinerary existing = itineraryMapper.findById(id);
        if (existing == null) {
            return null;
        }
        itinerary.setId(id);
        var expectedUpdatedAt = itinerary.getUpdatedAt();
        itinerary.setUpdatedAt(LocalDateTime.now());
        int changed = itineraryMapper.updateIfUnchanged(itinerary, expectedUpdatedAt);
        if (changed == 0) {
            return null;
        }
        return itineraryMapper.findById(id);
    }

    /**
     * WebSocket 协作编辑：合并非 null 字段 → 乐观锁更新 → 广播结果
     */
    public void handleEdit(ItineraryEditMessage msg) {
        Long id = msg.getItineraryId();
        Itinerary existing = itineraryMapper.findById(id);
        if (existing == null) {
            broadcastConflict(id, msg.getUsername(), "行程不存在");
            return;
        }

        LocalDateTime expectedUpdatedAt = null;
        if (msg.getExpectedUpdatedAt() != null && !msg.getExpectedUpdatedAt().isBlank()) {
            expectedUpdatedAt = LocalDateTime.parse(msg.getExpectedUpdatedAt(), ISO_FMT);
        }

        if (msg.getName() != null) existing.setName(msg.getName());
        if (msg.getStrategy() != null) existing.setStrategy(msg.getStrategy());
        if (msg.getTransportMode() != null) existing.setTransportMode(msg.getTransportMode());
        if (msg.getNotes() != null) existing.setNotes(msg.getNotes());

        existing.setUpdatedAt(LocalDateTime.now());

        int changed = itineraryMapper.updateIfUnchanged(existing, expectedUpdatedAt);
        if (changed == 0) {
            broadcastConflict(id, msg.getUsername(), "该行程已被其他协作者更新，请刷新后重试");
            return;
        }

        Itinerary updated = itineraryMapper.findById(id);
        broadcastUpdated(id, msg.getUsername(), updated);
    }

    private void broadcastUpdated(Long itineraryId, String username, Itinerary itinerary) {
        messagingTemplate.convertAndSend("/topic/itinerary/" + itineraryId,
                new ItineraryBroadcastMessage(
                        ItineraryBroadcastMessage.Type.UPDATED,
                        username, itinerary, null, LocalDateTime.now()));
    }

    private void broadcastConflict(Long itineraryId, String username, String errorMessage) {
        messagingTemplate.convertAndSend("/topic/itinerary/" + itineraryId,
                new ItineraryBroadcastMessage(
                        ItineraryBroadcastMessage.Type.CONFLICT,
                        username, null, errorMessage, LocalDateTime.now()));
    }
}
