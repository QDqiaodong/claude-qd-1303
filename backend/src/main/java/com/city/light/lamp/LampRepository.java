package com.city.light.lamp;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LampRepository extends JpaRepository<Lamp, Long> {

    List<Lamp> findByRoadId(Long roadId);

    List<Lamp> findByRoadIdIn(Collection<Long> roadIds);

    List<Lamp> findByStatus(String status);

    boolean existsByLampNo(String lampNo);
}
