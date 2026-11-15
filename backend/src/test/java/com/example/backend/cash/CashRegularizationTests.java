package com.example.backend.cash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.backend.accounts.AccountRole;
import com.example.backend.accounts.UserAccount;
import com.example.backend.accounts.UserAccountRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CashRegularizationTests {

    @Test
    void movesDisbursedMissionsPastTheirEndDateIntoRegularizationAndLogsSystemAction() {
        Instant now = Instant.parse("2026-11-14T01:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        LocalDate today = LocalDate.now(clock);
        UserAccount requester = mock(UserAccount.class);
        UserAccount manager = mock(UserAccount.class);
        UserAccount director = mock(UserAccount.class);
        when(manager.getRole()).thenReturn(null);
        when(director.getRole()).thenReturn(AccountRole.DIRECTEUR_GENERAL);

        CashRequest request = new CashRequest(
                UUID.randomUUID(),
                "Mission terminée",
                "Abidjan",
                today.minusDays(3),
                today.minusDays(1),
                "Transport",
                new BigDecimal("10000"),
                null,
                requester,
                now.minusSeconds(86400),
                now.minusSeconds(86400));
        request.submit(manager, director, now.minusSeconds(3600));
        request.approveN1(now.minusSeconds(3000));
        request.approveN2(now.minusSeconds(2000));
        request.setFinalAmount(new BigDecimal("10000"), null, now.minusSeconds(1000));
        request.markVoucherIssued(now.minusSeconds(900));
        request.markDisbursed(now.minusSeconds(800));

        CashRequestRepository requests = mock(CashRequestRepository.class);
        CashRequestEventRepository events = mock(CashRequestEventRepository.class);
        when(requests.findAllByStatusAndEndDateBefore(CashRequestStatus.DECAISSEE, today))
                .thenReturn(List.of(request));
        CashRequestService service = new CashRequestService(
                requests, events, mock(UserAccountRepository.class), clock, 10);

        int changed = service.regularizeExpiredRequests();

        assertEquals(1, changed);
        assertEquals(CashRequestStatus.EN_REGULARISATION, request.getStatus());
        verify(requests).save(request);
        ArgumentCaptor<CashRequestEvent> eventCaptor = ArgumentCaptor.forClass(CashRequestEvent.class);
        verify(events).save(eventCaptor.capture());
        assertNull(eventCaptor.getValue().getActorId());
        assertEquals("SYSTEM_REGULARIZATION_STARTED", eventCaptor.getValue().getAction());
        assertEquals(CashRequestStatus.DECAISSEE.name(), eventCaptor.getValue().getPreviousStatus());
        assertEquals(CashRequestStatus.EN_REGULARISATION.name(), eventCaptor.getValue().getNewStatus());
    }

    @Test
    void leavesMissionsEndingTodayOutOfRegularization() {
        Instant now = Instant.parse("2026-11-14T01:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        LocalDate today = LocalDate.now(clock);
        CashRequestRepository requests = mock(CashRequestRepository.class);
        CashRequestEventRepository events = mock(CashRequestEventRepository.class);
        when(requests.findAllByStatusAndEndDateBefore(CashRequestStatus.DECAISSEE, today))
                .thenReturn(List.of());
        CashRequestService service = new CashRequestService(
                requests, events, mock(UserAccountRepository.class), clock, 10);

        int changed = service.regularizeExpiredRequests();

        assertEquals(0, changed);
        verify(events, org.mockito.Mockito.never()).save(any(CashRequestEvent.class));
    }

    @Test
    void closeRegularizationTransitionsFromInRegularizationToRegularized() {
        Instant now = Instant.parse("2026-11-14T01:00:00Z");
        CashRequest request = new CashRequest(
                UUID.randomUUID(),
                "Mission terminée",
                "Abidjan",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 10),
                "Transport",
                new BigDecimal("10000"),
                null,
                mock(UserAccount.class),
                now.minusSeconds(86400),
                now.minusSeconds(86400));
        // Simulate reaching EN_REGULARISATION status
        java.lang.reflect.Field statusField;
        try {
            statusField = CashRequest.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(request, CashRequestStatus.EN_REGULARISATION);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        request.closeRegularization(now);

        assertEquals(CashRequestStatus.REGULARISEE, request.getStatus());
        assertEquals(now, request.getUpdatedAt());
    }

    @Test
    void closeRegularizationThrowsWhenNotInRegularization() {
        Instant now = Instant.parse("2026-11-14T01:00:00Z");
        CashRequest request = new CashRequest(
                UUID.randomUUID(),
                "Mission terminée",
                "Abidjan",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 10),
                "Transport",
                new BigDecimal("10000"),
                null,
                mock(UserAccount.class),
                now.minusSeconds(86400),
                now.minusSeconds(86400));
        // Status is DRAFT by default

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> request.closeRegularization(now));

        assertEquals("La demande n\u2019est pas \u00e0 cette \u00e9tape du circuit.", ex.getMessage());
    }
}
