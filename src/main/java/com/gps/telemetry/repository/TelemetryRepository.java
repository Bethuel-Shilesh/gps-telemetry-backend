
package com.gps.telemetry.repository;

import com.gps.telemetry.model.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TelemetryRepository extends
        JpaRepository<Telemetry, Long>,
        JpaSpecificationExecutor<Telemetry> {
}
