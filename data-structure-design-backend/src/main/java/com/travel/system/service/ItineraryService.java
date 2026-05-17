package com.travel.system.service;

import com.travel.system.dto.ItineraryBroadcastMessage;
import com.travel.system.dto.ItineraryEditMessage;
import com.travel.system.mapper.ItineraryMapper;
import com.travel.system.model.Itinerary;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ItineraryService {

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ItineraryMapper itineraryMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<Long, Set<String>> onlineUsersByItinerary = new ConcurrentHashMap<>();

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

    public boolean delete(Long id) {
        Itinerary existing = itineraryMapper.findById(id);
        if (existing == null) {
            return false;
        }
        itineraryMapper.deleteById(id);
        onlineUsersByItinerary.remove(id);
        return true;
    }

    public void handleEdit(ItineraryEditMessage msg) {
        handleEdit(msg.getItineraryId(), msg);
    }

    public void handleEdit(Long id, ItineraryEditMessage msg) {
        msg.setItineraryId(id);
        ItineraryEditMessage.Type type = msg.getType() == null ? ItineraryEditMessage.Type.PATCH : msg.getType();
        switch (type) {
            case JOIN -> handleJoin(id, msg.getUsername());
            case LEAVE -> handleLeave(id, msg.getUsername());
            case EDITING -> broadcastEditing(id, msg.getUsername(), msg.getField());
            case PATCH -> handlePatch(id, msg);
        }
    }

    private void handleJoin(Long id, String username) {
        if (hasText(username)) {
            onlineUsersByItinerary.computeIfAbsent(id, key -> ConcurrentHashMap.newKeySet()).add(username.trim());
        }
        messagingTemplate.convertAndSend("/topic/itinerary/" + id,
                broadcast(ItineraryBroadcastMessage.Type.JOINED, username, null, null, id, null, null));
    }

    private void handleLeave(Long id, String username) {
        Set<String> onlineUsers = onlineUsersByItinerary.get(id);
        if (onlineUsers != null && hasText(username)) {
            onlineUsers.remove(username.trim());
        }
        messagingTemplate.convertAndSend("/topic/itinerary/" + id,
                broadcast(ItineraryBroadcastMessage.Type.LEFT, username, null, null, id, null, null));
    }

    private void broadcastEditing(Long id, String username, String field) {
        messagingTemplate.convertAndSend("/topic/itinerary/" + id,
                broadcast(ItineraryBroadcastMessage.Type.EDITING, username, null, null, id, field, null));
    }

    private void handlePatch(Long id, ItineraryEditMessage msg) {
        Itinerary existing = itineraryMapper.findById(id);
        if (existing == null) {
            broadcastConflict(id, msg.getUsername(), "行程不存在");
            return;
        }

        LocalDateTime expectedUpdatedAt = null;
        if (hasText(msg.getExpectedUpdatedAt())) {
            expectedUpdatedAt = LocalDateTime.parse(msg.getExpectedUpdatedAt(), ISO_FMT);
        }

        String changedField = applyPatch(existing, msg);
        existing.setUpdatedAt(LocalDateTime.now());

        int changed = itineraryMapper.updateIfUnchanged(existing, expectedUpdatedAt);
        if (changed == 0) {
            broadcastConflict(id, msg.getUsername(), "该行程已被其他协作者更新，请刷新后重试");
            return;
        }

        Itinerary updated = itineraryMapper.findById(id);
        broadcastUpdated(id, msg.getUsername(), updated, changedField, msg.getValue());
    }

    private String applyPatch(Itinerary existing, ItineraryEditMessage msg) {
        if (hasText(msg.getField())) {
            String field = msg.getField().trim();
            String value = msg.getValue() == null ? "" : msg.getValue();
            switch (field) {
                case "name" -> existing.setName(value);
                case "owner" -> existing.setOwner(value);
                case "collaborators" -> existing.setCollaborators(value);
                case "strategy" -> existing.setStrategy(value);
                case "transportMode" -> existing.setTransportMode(value);
                case "notes" -> existing.setNotes(value);
                default -> throw new IllegalArgumentException("Unsupported itinerary field: " + field);
            }
            return field;
        }

        if (msg.getName() != null) existing.setName(msg.getName());
        if (msg.getOwner() != null) existing.setOwner(msg.getOwner());
        if (msg.getCollaborators() != null) existing.setCollaborators(msg.getCollaborators());
        if (msg.getStrategy() != null) existing.setStrategy(msg.getStrategy());
        if (msg.getTransportMode() != null) existing.setTransportMode(msg.getTransportMode());
        if (msg.getNotes() != null) existing.setNotes(msg.getNotes());
        return null;
    }

    private void broadcastUpdated(Long itineraryId, String username, Itinerary itinerary, String field, String value) {
        messagingTemplate.convertAndSend("/topic/itinerary/" + itineraryId,
                broadcast(ItineraryBroadcastMessage.Type.UPDATED, username, itinerary, null, itineraryId, field, value));
    }

    private void broadcastConflict(Long itineraryId, String username, String errorMessage) {
        messagingTemplate.convertAndSend("/topic/itinerary/" + itineraryId,
                broadcast(ItineraryBroadcastMessage.Type.CONFLICT, username, null, errorMessage, itineraryId, null, null));
    }

    private ItineraryBroadcastMessage broadcast(ItineraryBroadcastMessage.Type type,
                                                String username,
                                                Itinerary itinerary,
                                                String message,
                                                Long itineraryId,
                                                String field,
                                                String value) {
        ItineraryBroadcastMessage broadcast = new ItineraryBroadcastMessage(
                type,
                username,
                itinerary,
                message,
                LocalDateTime.now());
        broadcast.setOnlineUsers(onlineUsers(itineraryId));
        broadcast.setField(field);
        broadcast.setValue(value);
        return broadcast;
    }

    private List<String> onlineUsers(Long itineraryId) {
        Set<String> onlineUsers = onlineUsersByItinerary.get(itineraryId);
        if (onlineUsers == null || onlineUsers.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(onlineUsers);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
