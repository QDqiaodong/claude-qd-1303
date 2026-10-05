package com.city.light.lamp;

import com.city.light.common.BizException;
import com.city.light.common.Dict;
import com.city.light.road.Road;
import com.city.light.road.RoadRepository;
import com.city.light.road.RoadService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class LampService {

    private final LampRepository repo;
    private final RoadRepository roadRepo;
    private final RoadService roadService;

    public LampService(LampRepository repo, RoadRepository roadRepo, RoadService roadService) {
        this.repo = repo;
        this.roadRepo = roadRepo;
        this.roadService = roadService;
    }

    /** 支持按片区过滤（片区来自路段），也支持按路段过滤。 */
    public List<Lamp> list(Long roadId, String zone) {
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

    public Lamp get(Long id) {
        return repo.findById(id).orElseThrow(() -> new BizException("灯具不存在"));
    }

    public Lamp create(Lamp in) {
        if (in.getLampNo() == null || in.getLampNo().isBlank()) {
            throw new BizException("灯具编号必填");
        }
        String lampNo = in.getLampNo().trim();
        if (repo.existsByLampNo(lampNo)) {
            throw new BizException("灯具编号已存在，不能重复登记");
        }
        Long roadId = in.getRoadId();
        if (roadId == null || !roadRepo.existsById(roadId)) {
            throw new BizException("灯具必须挂在一个已登记的路段下");
        }
        Road road = roadRepo.findById(roadId).orElseThrow(() -> new BizException("灯具必须挂在一个已登记的路段下"));
        if (!Dict.ROAD_NORMAL.equals(road.getStatus())) {
            throw new BizException("只能挂在「正常」路段下（施工 / 停用路段不可新增灯具）");
        }
        if (in.getPoleNo() == null || in.getPoleNo().isBlank()) {
            throw new BizException("杆号必填");
        }
        if (!Dict.isValidLampType(in.getType())) {
            throw new BizException("灯具类型必须是：LED / 高压钠灯 / 太阳能");
        }
        if (in.getWatt() == null || in.getWatt() <= 0) {
            throw new BizException("灯具功率必须大于 0");
        }
        String status = (in.getStatus() == null || in.getStatus().isBlank()) ? Dict.LAMP_ON : in.getStatus();
        if (!Dict.isValidLampStatus(status)) {
            throw new BizException("灯具状态必须是：亮灯 / 故障 / 维修中 / 报废");
        }

        Lamp l = new Lamp();
        l.setLampNo(lampNo);
        l.setRoadId(roadId);
        l.setPoleNo(in.getPoleNo().trim());
        l.setType(in.getType());
        l.setWatt(in.getWatt());
        l.setInstallDate(in.getInstallDate() == null ? LocalDate.now() : in.getInstallDate());
        l.setStatus(status);
        l.setCreatedAt(LocalDateTime.now());
        l.setUpdatedAt(LocalDateTime.now());
        Lamp saved = repo.save(l);
        roadService.adjustLampCount(roadId, 1);
        return saved;
    }

    public Lamp update(Long id, Lamp in) {
        Lamp l = get(id);
        if (Dict.LAMP_SCRAP.equals(l.getStatus())) {
            throw new BizException("报废灯具为终态，不可再修改");
        }
        if (in.getLampNo() != null && !in.getLampNo().isBlank()) {
            String lampNo = in.getLampNo().trim();
            if (!lampNo.equals(l.getLampNo()) && repo.existsByLampNo(lampNo)) {
                throw new BizException("灯具编号已存在，不能重复登记");
            }
            l.setLampNo(lampNo);
        }
        if (in.getRoadId() != null && !in.getRoadId().equals(l.getRoadId())) {
            Road road = roadRepo.findById(in.getRoadId())
                    .orElseThrow(() -> new BizException("灯具必须挂在一个已登记的路段下"));
            if (!Dict.ROAD_NORMAL.equals(road.getStatus())) {
                throw new BizException("只能挂在「正常」路段下（施工 / 停用路段不可新增灯具）");
            }
            roadService.adjustLampCount(l.getRoadId(), -1);
            roadService.adjustLampCount(in.getRoadId(), 1);
            l.setRoadId(in.getRoadId());
        }
        if (in.getPoleNo() != null && !in.getPoleNo().isBlank()) {
            l.setPoleNo(in.getPoleNo().trim());
        }
        if (in.getType() != null) {
            if (!Dict.isValidLampType(in.getType())) {
                throw new BizException("灯具类型必须是：LED / 高压钠灯 / 太阳能");
            }
            l.setType(in.getType());
        }
        if (in.getWatt() != null) {
            if (in.getWatt() <= 0) {
                throw new BizException("灯具功率必须大于 0");
            }
            l.setWatt(in.getWatt());
        }
        if (in.getInstallDate() != null) {
            l.setInstallDate(in.getInstallDate());
        }
        if (in.getStatus() != null && !in.getStatus().isBlank() && !in.getStatus().equals(l.getStatus())) {
            if (!Dict.canTransferLamp(l.getStatus(), in.getStatus())) {
                throw new BizException("灯具状态不能从「" + l.getStatus() + "」直接变更为「" + in.getStatus() + "」（只有故障灯具可以转维修中）");
            }
            applyStatus(l, in.getStatus());
        }
        l.setUpdatedAt(LocalDateTime.now());
        return repo.save(l);
    }

    /** 灯具状态流转：亮灯→故障，故障→维修中，维修中→亮灯，报废为终态。 */
    public Lamp changeStatus(Long id, String status) {
        Lamp l = get(id);
        if (!Dict.isValidLampStatus(status)) {
            throw new BizException("灯具状态必须是：亮灯 / 故障 / 维修中 / 报废");
        }
        if (Dict.LAMP_SCRAP.equals(l.getStatus())) {
            throw new BizException("报废灯具为终态，不可再变更状态");
        }
        if (Dict.LAMP_SCRAP.equals(status)) {
            throw new BizException("灯具报废请走报修工单的「无法修复」结论");
        }
        if (!Dict.canTransferLamp(l.getStatus(), status)) {
            throw new BizException("灯具状态不能从「" + l.getStatus() + "」直接变更为「" + status + "」（只有故障灯具可以转维修中）");
        }
        applyStatus(l, status);
        l.setUpdatedAt(LocalDateTime.now());
        return repo.save(l);
    }

    /** 报修完工后由 RepairService 直接改写灯具状态，并处理灯杆数联动。 */
    public void settleStatus(Long lampId, String status) {
        Lamp l = repo.findById(lampId).orElseThrow(() -> new BizException("灯具不存在"));
        if (status.equals(l.getStatus())) {
            return;
        }
        applyStatus(l, status);
        l.setUpdatedAt(LocalDateTime.now());
        repo.save(l);
    }

    private void applyStatus(Lamp l, String status) {
        boolean wasScrap = Dict.LAMP_SCRAP.equals(l.getStatus());
        l.setStatus(status);
        if (!wasScrap && Dict.LAMP_SCRAP.equals(status)) {
            roadService.adjustLampCount(l.getRoadId(), -1);
        }
    }
}
