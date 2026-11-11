package com.residential.management.demo.residential.infrastructure.messaging;

import com.residential.management.demo.residential.domain.model.entities.Unit;
import com.residential.management.demo.residential.domain.model.entities.UserUnit;
import com.residential.management.demo.residential.domain.model.valueobjects.RelationType;
import com.residential.management.demo.residential.infrastructure.messaging.events.ResidentUserCreatedEvent;
import com.residential.management.demo.residential.infrastructure.persistence.jpa.repositories.UserUnitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResidentLinkCompletionConsumerTest {

    @Mock
    private UserUnitRepository userUnitRepository;


    private UserUnit pendingLink() {
        var unit = mock(Unit.class);
        return new UserUnit(unit, "ana@example.com", RelationType.OWNER);
    }

    @Test
    void createdEventActivatesPendingLink() {
        var link = pendingLink();
        when(userUnitRepository.findByResidentEmailAndUnitIdUnitAndStatus("ana@example.com", 7L, "PENDIENTE"))
                .thenReturn(List.of(link));
        when(userUnitRepository.saveAndFlush(any(UserUnit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new ResidentLinkCompletionConsumer(userUnitRepository).onResidentUserCreated(
                new ResidentUserCreatedEvent("ana@example.com", 7L, 42L, "msg-1", "CREATED", null));

        assertEquals("ACTIVE", link.getStatus());
        assertEquals(42L, link.getIdUser());
        assertNull(link.getFailureReason());
    }

    @Test
    void absentStatusIsTreatedAsCreated() {
        var link = pendingLink();
        when(userUnitRepository.findByResidentEmailAndUnitIdUnitAndStatus("ana@example.com", 7L, "PENDIENTE"))
                .thenReturn(List.of(link));
        when(userUnitRepository.saveAndFlush(any(UserUnit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new ResidentLinkCompletionConsumer(userUnitRepository).onResidentUserCreated(
                new ResidentUserCreatedEvent("ana@example.com", 7L, 42L, "msg-2", null, null));

        assertEquals("ACTIVE", link.getStatus());
        assertEquals(42L, link.getIdUser());
    }

    @Test
    void rejectedEventMarksLinkFailedWithEmailRegisteredReason() {
        var link = pendingLink();
        when(userUnitRepository.findByResidentEmailAndUnitIdUnitAndStatus("ana@example.com", 7L, "PENDIENTE"))
                .thenReturn(List.of(link));
        when(userUnitRepository.save(any(UserUnit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new ResidentLinkCompletionConsumer(userUnitRepository).onResidentUserCreated(new ResidentUserCreatedEvent(
                "ana@example.com", 7L, null, "msg-3", "REJECTED", "EMAIL_ALREADY_REGISTERED"));

        assertEquals("FAILED", link.getStatus());
        assertEquals("El correo ya está registrado", link.getFailureReason());
        assertNull(link.getIdUser());
        verify(userUnitRepository).save(link);
        verify(userUnitRepository, never()).saveAndFlush(any(UserUnit.class));
    }

    @Test
    void completionCollisionMarksLinkFailedWithTitularReason() {
        var link = pendingLink();
        when(userUnitRepository.findByResidentEmailAndUnitIdUnitAndStatus("ana@example.com", 7L, "PENDIENTE"))
                .thenReturn(List.of(link));
        when(userUnitRepository.saveAndFlush(any(UserUnit.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"ux_user_unit_titular_activo\"; SQLState 23505"));
        when(userUnitRepository.save(any(UserUnit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new ResidentLinkCompletionConsumer(userUnitRepository).onResidentUserCreated(
                new ResidentUserCreatedEvent("ana@example.com", 7L, 42L, "msg-4", "CREATED", null));

        assertEquals("FAILED", link.getStatus());
        assertEquals("La unidad ya tiene un titular activo", link.getFailureReason());
        verify(userUnitRepository).save(link);
    }

    @Test
    void nonUniqueViolationIsRethrownAndLinkIsNotFailed() {
        var link = pendingLink();
        when(userUnitRepository.findByResidentEmailAndUnitIdUnitAndStatus("ana@example.com", 7L, "PENDIENTE"))
                .thenReturn(List.of(link));
        when(userUnitRepository.saveAndFlush(any(UserUnit.class)))
                .thenThrow(new DataIntegrityViolationException("referential integrity problem"));

        assertThrows(DataIntegrityViolationException.class, () -> new ResidentLinkCompletionConsumer(userUnitRepository).onResidentUserCreated(
                new ResidentUserCreatedEvent("ana@example.com", 7L, 42L, "msg-5", "CREATED", null)));

        assertEquals("ACTIVE", link.getStatus());
        verify(userUnitRepository, never()).save(any(UserUnit.class));
    }

    @Test
    void redeliveryWithoutPendingLinkIsNoOp() {
        when(userUnitRepository.findByResidentEmailAndUnitIdUnitAndStatus("ana@example.com", 7L, "PENDIENTE"))
                .thenReturn(List.of());

        new ResidentLinkCompletionConsumer(userUnitRepository).onResidentUserCreated(
                new ResidentUserCreatedEvent("ana@example.com", 7L, 42L, "msg-6", "CREATED", null));

        verify(userUnitRepository, never()).save(any(UserUnit.class));
        verify(userUnitRepository, never()).saveAndFlush(any(UserUnit.class));
    }
}
