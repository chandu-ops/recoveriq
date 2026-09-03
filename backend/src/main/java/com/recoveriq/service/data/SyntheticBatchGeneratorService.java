package com.recoveriq.service.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import com.recoveriq.domain.Customer;
import com.recoveriq.domain.RecoveryCase;
import com.recoveriq.enums.CaseStatus;
import com.recoveriq.enums.FailureCategory;
import com.recoveriq.repository.CustomerRepository;
import com.recoveriq.repository.RecoveryCaseRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SyntheticBatchGeneratorService {
    private final CustomerRepository customerRepository;
    private final RecoveryCaseRepository recoveryCaseRepository;
    private static final FailureCategory[] CATEGORIES = FailureCategory.values();

    public List<RecoveryCase> generateBatch(int size) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            customers.add(Customer.builder()
                    .name("Synthetic Customer " + i)
                    .email("synthetic" + i + "_" + System.nanoTime() + "@example.com")
                    .phone("9" + String.format("%09d", i))
                    .build());
        }
        customers = customerRepository.saveAll(customers);

        List<RecoveryCase> cases = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            cases.add(RecoveryCase.builder()
                    .customer(customers.get(i))
                    .amountAtRisk(BigDecimal.valueOf(random.nextInt(500, 50000)))
                    .failureCategory(CATEGORIES[random.nextInt(CATEGORIES.length)])
                    .status(CaseStatus.OPEN)
                    .retriesUsed(0).contactsUsed(0).messagesUsed(0).humanEscalationsUsed(0)
                    .build());
        }
        return recoveryCaseRepository.saveAll(cases);
    }
}