package com.city.light.road;

import com.city.light.common.BizException;
import com.city.light.common.Dict;
import com.city.light.lamp.LampRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoadService {

    private final RoadRepository repo;
    private final LampRepository lampRepo;

    public RoadService(RoadRepository repo, LampRepository lampRepo) {
        this.repo = repo;
        this.lampRepo = lampRepo;
    }

    /** 按片区过滤；zone 为 null / 空 / 「全部」时返回全部路段。 */
    public List<Road> list(String zone) {
        if (Dict.isZoneFilter(zone)) {
            return repo.findByZone(zone);
        }
        return repo.findAll();
    }

    public Road get(Long id) {
        return repo.findById(id).orElseThrow(() -> new BizException("路段不存在"));
    }

    public Road create(Road in) {
        if (in.getRoadNo() == null || in.getRoadNo().isBlank()) {
            throw new BizException("路段编号必填");
        }
        String roadNo = in.getRoadNo().trim();
        if (repo.existsByRoadNo(roadNo)) {
            throw new BizException("路段编号已存在，不能重复登记");
        }
        if (in.getName() == null || in.getName().isBlank()) {
            throw new BizException("路段名必填");
        }
        if (!Dict.isValidZone(in.getZone())) {
            throw new BizException("片区必须是：城东 / 城西 / 城南 / 城北 / 开发区");
        }
        if (in.getLengthMeter() == null || in.getLengthMeter().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("路段长度必须大于 0");
        }
        int lampCount = (in.getLampCount() == null) ? 0 : in.getLampCount();
        if (lampCount < 0) {
            throw new BizException("灯杆数不能为负数");
        }
        String status = (in.getStatus() == null || in.getStatus().isBlank()) ? Dict.ROAD_NORMAL : in.getStatus();
        if (!Dict.isValidRoadStatus(status)) {
            throw new BizException("路段状态必须是：正常 / 施工 / 停用");
        }

        Road r = new Road();
        r.setRoadNo(roadNo);
        r.setName(in.getName().trim());
        r.setZone(in.getZone());
        r.setLengthMeter(in.getLengthMeter());
        r.setLampCount(lampCount);
        r.setStatus(status);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return repo.save(r);
    }

    public Road update(Long id, Road in) {
        Road r = get(id);
        if (in.getRoadNo() != null && !in.getRoadNo().isBlank()) {
            String roadNo = in.getRoadNo().trim();
            if (!roadNo.equals(r.getRoadNo()) && repo.existsByRoadNo(roadNo)) {
                throw new BizException("路段编号已存在，不能重复登记");
            }
            r.setRoadNo(roadNo);
        }
        if (in.getName() != null && !in.getName().isBlank()) {
            r.setName(in.getName().trim());
        }
        if (in.getZone() != null) {
            if (!Dict.isValidZone(in.getZone())) {
                throw new BizException("片区必须是：城东 / 城西 / 城南 / 城北 / 开发区");
            }
            r.setZone(in.getZone());
        }
        if (in.getLengthMeter() != null) {
            if (in.getLengthMeter().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("路段长度必须大于 0");
            }
            r.setLengthMeter(in.getLengthMeter());
        }
        if (in.getLampCount() != null) {
            if (in.getLampCount() < 0) {
                throw new BizException("灯杆数不能为负数");
            }
            r.setLampCount(in.getLampCount());
        }
        if (in.getStatus() != null && !in.getStatus().isBlank() && !in.getStatus().equals(r.getStatus())) {
            if (!Dict.canTransferRoad(r.getStatus(), in.getStatus())) {
                throw new BizException("路段状态不能从「" + r.getStatus() + "」直接变更为「" + in.getStatus() + "」");
            }
            r.setStatus(in.getStatus());
        }
        r.setUpdatedAt(LocalDateTime.now());
        return repo.save(r);
    }

    /** 路段状态流转：正常↔施工；停用只能恢复为正常。 */
    public Road changeStatus(Long id, String status) {
        Road r = get(id);
        if (!Dict.isValidRoadStatus(status)) {
            throw new BizException("路段状态必须是：正常 / 施工 / 停用");
        }
        if (!Dict.canTransferRoad(r.getStatus(), status)) {
            throw new BizException("路段状态不能从「" + r.getStatus() + "」直接变更为「" + status + "」");
        }
        r.setStatus(status);
        r.setUpdatedAt(LocalDateTime.now());
        return repo.save(r);
    }

    /** 灯杆数联动：新增灯杆 +1，报废灯杆 -1（不会低于 0）。 */
    public void adjustLampCount(Long roadId, int delta) {
        Road r = repo.findById(roadId).orElse(null);
        if (r == null) {
            return;
        }
        int next = (r.getLampCount() == null ? 0 : r.getLampCount()) + delta;
        r.setLampCount(Math.max(0, next));
        r.setUpdatedAt(LocalDateTime.now());
        repo.save(r);
    }

    /** 按实际灯具数量重算路段灯杆数（数据校准用）。 */
    public Road syncLampCount(Long id) {
        Road r = get(id);
        r.setLampCount(lampRepo.findByRoadId(id).size());
        r.setUpdatedAt(LocalDateTime.now());
        return repo.save(r);
    }
}
