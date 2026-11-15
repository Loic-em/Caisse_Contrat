package com.example.backend.cash;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CashRegularizationScheduler {

    private final CashRequestService requests;

    public CashRegularizationScheduler(CashRequestService requests) {
        this.requests = requests;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpAfterStartup() {
        requests.regularizeExpiredRequests();
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Africa/Abidjan")
    public void regularizeAtMissionEnd() {
        requests.regularizeExpiredRequests();
    }
}
