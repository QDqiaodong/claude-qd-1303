package com.city.light.road;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadRepository extends JpaRepository<Road, Long> {

    Optional<Road> findByRoadNo(String roadNo);

    boolean existsByRoadNo(String roadNo);

    List<Road> findByZone(String zone);
}
