package com.arpita.reconciliation.repository;

import com.arpita.reconciliation.entity.IngestionErrors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngestionErrorsRepository extends JpaRepository<IngestionErrors, Long> {

}
