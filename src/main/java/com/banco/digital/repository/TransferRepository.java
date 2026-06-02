package com.banco.digital.repository;

import com.banco.digital.entity.Transfer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByChaveRetry(String chaveRetry);

    @Query("""
            SELECT t FROM Transfer t
            WHERE t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId
            ORDER BY t.createdAt DESC
            """)
    List<Transfer> findMovementsByAccountId(@Param("accountId") Long accountId);
}
