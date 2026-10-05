package com.city.light.inspection;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {

    List<Inspection> findByRoadId(Long roadId);

    List<Inspection> findByRoadIdIn(Collection<Long> roadIds);
}
