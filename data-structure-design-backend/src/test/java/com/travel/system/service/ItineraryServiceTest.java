package com.travel.system.service;

import com.travel.system.dto.ItineraryBroadcastMessage;
import com.travel.system.dto.ItineraryEditMessage;
import com.travel.system.mapper.ItineraryMapper;
import com.travel.system.model.Itinerary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItineraryServiceTest {

    private final ItineraryMapper itineraryMapper = mock(ItineraryMapper.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final ItineraryService service = new ItineraryService(itineraryMapper, messagingTemplate);

    @Test
    void joinBroadcastsOnlineCollaborators() {
        ItineraryEditMessage message = new ItineraryEditMessage();
        message.setType(ItineraryEditMessage.Type.JOIN);
        message.setUsername("张三");

        service.handleEdit(7L, message);

        ArgumentCaptor<ItineraryBroadcastMessage> broadcast = ArgumentCaptor.forClass(ItineraryBroadcastMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/itinerary/7"), broadcast.capture());
        assertThat(broadcast.getValue().getType()).isEqualTo(ItineraryBroadcastMessage.Type.JOINED);
        assertThat(broadcast.getValue().getOnlineUsers()).containsExactly("张三");
    }

    @Test
    void patchUpdatesOnlyOneFieldAndBroadcastsLatestItinerary() {
        LocalDateTime originalUpdatedAt = LocalDateTime.of(2026, 5, 12, 16, 0);
        Itinerary existing = itinerary("原行程", "张三", "张三,李四", "省时", "步行", "旧备注", originalUpdatedAt);
        when(itineraryMapper.findById(7L)).thenReturn(existing, existing);
        when(itineraryMapper.updateIfUnchanged(eq(existing), eq(originalUpdatedAt))).thenReturn(1);

        ItineraryEditMessage message = new ItineraryEditMessage();
        message.setType(ItineraryEditMessage.Type.PATCH);
        message.setUsername("李四");
        message.setExpectedUpdatedAt("2026-05-12T16:00:00");
        message.setField("notes");
        message.setValue("新的协作备注");

        service.handleEdit(7L, message);

        assertThat(existing.getName()).isEqualTo("原行程");
        assertThat(existing.getNotes()).isEqualTo("新的协作备注");

        ArgumentCaptor<ItineraryBroadcastMessage> broadcast = ArgumentCaptor.forClass(ItineraryBroadcastMessage.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/itinerary/7"), broadcast.capture());
        assertThat(broadcast.getValue().getType()).isEqualTo(ItineraryBroadcastMessage.Type.UPDATED);
        assertThat(broadcast.getValue().getUsername()).isEqualTo("李四");
        assertThat(broadcast.getValue().getField()).isEqualTo("notes");
        assertThat(broadcast.getValue().getItinerary()).isSameAs(existing);
    }

    @Test
    void deleteRemovesExistingItinerary() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 5, 16, 16, 8);
        Itinerary existing = itinerary("行程副本", "演示用户", "", "", "", "", updatedAt);
        when(itineraryMapper.findById(7L)).thenReturn(existing);

        boolean deleted = service.delete(7L);

        assertThat(deleted).isTrue();
        verify(itineraryMapper).deleteById(7L);
    }

    @Test
    void deleteReturnsFalseWhenItineraryDoesNotExist() {
        when(itineraryMapper.findById(404L)).thenReturn(null);

        boolean deleted = service.delete(404L);

        assertThat(deleted).isFalse();
        verify(itineraryMapper, never()).deleteById(404L);
    }

    private Itinerary itinerary(String name,
                                String owner,
                                String collaborators,
                                String strategy,
                                String transportMode,
                                String notes,
                                LocalDateTime updatedAt) {
        Itinerary itinerary = new Itinerary();
        itinerary.setId(7L);
        itinerary.setName(name);
        itinerary.setOwner(owner);
        itinerary.setCollaborators(collaborators);
        itinerary.setStrategy(strategy);
        itinerary.setTransportMode(transportMode);
        itinerary.setNotes(notes);
        itinerary.setUpdatedAt(updatedAt);
        return itinerary;
    }
}
