package com.city.light.repair;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepairRepository extends JpaRepository<Repair, Long> {

    List<Repair> findByLampId(Long lampId);

    List<Repair> findByLampIdIn(Collection<Long> lampIds);
}
