package com.city.light.inspection;

import com.city.light.common.BizException;
import com.city.light.common.Dict;
import com.city.light.road.Road;
import com.city.light.road.RoadRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InspectionService {

    private final InspectionRepository repo;
    private final RoadRepository roadRepo;

    public InspectionService(InspectionRepository repo, RoadRepository roadRepo) {
        this.repo = repo;
        this.roadRepo = roadRepo;
    }

    /** 支持按片区过滤（片区来自路段），也支持按路段过滤。 */
    public List<Inspection> list(Long roadId, String zone) {
        if (Dict.isZoneFilter(zone)) {
            Set<Long> roadIds = roadRepo.findByZone(zone).stream().map(Road::getId).collect(Collectors.toSet());
            if (roadId != null) {
                return roadIds.contains(roadId) ? repo.findByRoadId(roadId) : List.of();
            }
            return repo.findByRoadIdIn(roadIds);
        }
        if (roadId != null) {
            return repo.findByRoadId(roadId);
        }
        return repo.findAll();
    }

    public Inspection get(Long id) {
        return repo.findById(id).orElseThrow(() -> new BizException("巡检记录不存在"));
    }

    public Inspection create(Inspection in) {
        Inspection n = new Inspection();
        n.setCreatedAt(LocalDateTime.now());
        fill(n, in);
        return repo.save(n);
    }

    public Inspection update(Long id, Inspection in) {
        Inspection n = get(id);
        fill(n, in);
        return repo.save(n);
    }

    private void fill(Inspection n, Inspection in) {
        Long roadId = (in.getRoadId() != null) ? in.getRoadId() : n.getRoadId();
        if (roadId == null || !roadRepo.existsById(roadId)) {
            throw new BizException("巡检记录必须关联一个已登记的路段");
        }
        Road road = roadRepo.findById(roadId).orElseThrow(() -> new BizException("巡检记录必须关联一个已登记的路段"));
        if (Dict.ROAD_DISABLED.equals(road.getStatus())) {
            throw new BizException("停用路段不可排巡检");
        }
        if (n.getRoadId() == null && (in.getInspector() == null || in.getInspector().isBlank())) {
            throw new BizException("巡检员必填");
        }
        int litCount = (in.getLitCount() != null) ? in.getLitCount() : (n.getLitCount() == null ? 0 : n.getLitCount());
        int faultCount = (in.getFaultCount() != null) ? in.getFaultCount() : (n.getFaultCount() == null ? 0 : n.getFaultCount());
        if (litCount < 0 || faultCount < 0) {
            throw new BizException("亮灯数与故障数都不能为负数");
        }
        int total = road.getLampCount() == null ? 0 : road.getLampCount();
        if (litCount + faultCount > total) {
            throw new BizException("亮灯数 + 故障数（" + (litCount + faultCount) + "）不得超过该路段灯杆总数（" + total + "）");
        }
        String status = (in.getStatus() != null && !in.getStatus().isBlank()) ? in.getStatus() : n.getStatus();
        if (status == null || status.isBlank()) {
            status = faultCount > 0 ? "有故障" : "正常";
        }
        if (!Dict.isValidInspectStatus(status)) {
            throw new BizException("巡检结论必须是：正常 / 有故障 / 待复查");
        }
        if (faultCount > 0 && !Dict.INSPECT_FAULT_STATUS.contains(status)) {
            throw new BizException("故障数大于 0 时，巡检结论必须是有故障或待复查");
        }
        if (faultCount == 0 && !"正常".equals(status)) {
            throw new BizException("没有故障灯时，巡检结论只能是正常");
        }

        n.setRoadId(roadId);
        if (in.getInspector() != null && !in.getInspector().isBlank()) {
            n.setInspector(in.getInspector().trim());
        }
        if (in.getCheckDate() != null) {
            n.setCheckDate(in.getCheckDate());
        }
        if (n.getCheckDate() == null) {
            n.setCheckDate(LocalDate.now());
        }
        n.setLitCount(litCount);
        n.setFaultCount(faultCount);
        n.setStatus(status);
        n.setUpdatedAt(LocalDateTime.now());
    }
}
