package com.banco.digital.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.banco.digital.dto.TransferRequest;
import com.banco.digital.entity.Account;
import com.banco.digital.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TransferConcurrencyTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldMaintainConsistencyUnderConcurrentTransfers() throws InterruptedException {
        Account source = accountRepository.save(new Account("Origem", new BigDecimal("1000.00")));
        Account destination = accountRepository.save(new Account("Destino", new BigDecimal("0.00")));

        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            int index = i;
            pool.submit(() -> {
                try {
                    start.await();
                    transferService.transfer(
                            new TransferRequest(source.getId(), destination.getId(), new BigDecimal("1.00")),
                            "concurrent-key-" + index
                    );
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await();
        pool.shutdown();

        Account sourceFinal = accountRepository.findById(source.getId()).orElseThrow();
        Account destinationFinal = accountRepository.findById(destination.getId()).orElseThrow();

        assertEquals(0, sourceFinal.getBalance().compareTo(new BigDecimal("900.00")));
        assertEquals(0, destinationFinal.getBalance().compareTo(new BigDecimal("100.00")));
    }
}
