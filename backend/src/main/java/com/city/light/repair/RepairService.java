package com.city.light.repair;

import com.city.light.common.BizException;
import com.city.light.common.Dict;
import com.city.light.lamp.Lamp;
import com.city.light.lamp.LampRepository;
import com.city.light.lamp.LampService;
import com.city.light.road.Road;
import com.city.light.road.RoadRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RepairService {

    private final RepairRepository repo;
    private final LampRepository lampRepo;
    private final LampService lampService;
    private final RoadRepository roadRepo;

    public RepairService(RepairRepository repo, LampRepository lampRepo, LampService lampService,
                         RoadRepository roadRepo) {
        this.repo = repo;
        this.lampRepo = lampRepo;
        this.lampService = lampService;
        this.roadRepo = roadRepo;
    }

    /** 支持按片区过滤（片区来自灯具所属路段），也支持按灯具过滤。 */
    public List<Repair> list(Long lampId, String zone) {
        if (Dict.isZoneFilter(zone)) {
            Set<Long> roadIds = roadRepo.findByZone(zone).stream().map(Road::getId).collect(Collectors.toSet());
            Set<Long> lampIds = lampRepo.findByRoadIdIn(roadIds).stream().map(Lamp::getId).collect(Collectors.toSet());
            if (lampId != null) {
                return lampIds.contains(lampId) ? repo.findByLampId(lampId) : List.of();
            }
            return repo.findByLampIdIn(lampIds);
        }
        if (lampId != null) {
            return repo.findByLampId(lampId);
        }
        return repo.findAll();
    }

    public Repair get(Long id) {
        return repo.findById(id).orElseThrow(() -> new BizException("报修工单不存在"));
    }

    public Repair create(Repair in) {
        Long lampId = in.getLampId();
        if (lampId == null || !lampRepo.existsById(lampId)) {
            throw new BizException("报修工单必须关联一盏已登记的灯具");
        }
        Lamp lamp = lampRepo.findById(lampId).orElseThrow(() -> new BizException("报修工单必须关联一盏已登记的灯具"));
        if (!Dict.LAMP_FAULT.equals(lamp.getStatus())) {
            throw new BizException("只能给「故障」状态的灯具报修，当前灯具为「" + lamp.getStatus() + "」");
        }
        if (!Dict.isValidFaultType(in.getFaultType())) {
            throw new BizException("故障类型必须是：不亮 / 闪烁 / 破损 / 线路故障");
        }
        String status = (in.getStatus() == null || in.getStatus().isBlank()) ? "待派工" : in.getStatus();
        if (!Dict.isValidRepairStatus(status)) {
            throw new BizException("工单状态必须是：待派工 / 维修中 / 已修复 / 无法修复");
        }
        if (Dict.isFinalRepair(status)) {
            throw new BizException("新建工单状态只能是待派工或维修中");
        }
        if (in.getCost() != null && in.getCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("维修费用不能为负数");
        }

        Repair r = new Repair();
        r.setLampId(lampId);
        r.setFaultType(in.getFaultType());
        r.setReportDate(in.getReportDate() == null ? LocalDate.now() : in.getReportDate());
        r.setWorker(in.getWorker() == null ? "" : in.getWorker().trim());
        r.setStatus(status);
        r.setFinishDate(in.getFinishDate());
        r.setCost(in.getCost());
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return repo.save(r);
    }

    public Repair update(Long id, Repair in) {
        Repair r = get(id);
        if (Dict.isFinalRepair(r.getStatus())) {
            throw new BizException("已修复 / 无法修复 的工单为终态，不可再修改");
        }
        if (in.getFaultType() != null) {
            if (!Dict.isValidFaultType(in.getFaultType())) {
                throw new BizException("故障类型必须是：不亮 / 闪烁 / 破损 / 线路故障");
            }
            r.setFaultType(in.getFaultType());
        }
        if (in.getWorker() != null) {
            r.setWorker(in.getWorker().trim());
        }
        if (in.getReportDate() != null) {
            r.setReportDate(in.getReportDate());
        }
        if (in.getCost() != null) {
            if (in.getCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("维修费用不能为负数");
            }
            r.setCost(in.getCost());
        }
        if (in.getStatus() != null && !in.getStatus().isBlank() && !in.getStatus().equals(r.getStatus())) {
            if (Dict.isFinalRepair(in.getStatus())) {
                throw new BizException("完工请走「完工登记」，不能直接把工单改为" + in.getStatus());
            }
            if (!Dict.canTransferRepair(r.getStatus(), in.getStatus())) {
                throw new BizException("工单状态不能从「" + r.getStatus() + "」直接变更为「" + in.getStatus() + "」");
            }
            r.setStatus(in.getStatus());
        }
        r.setUpdatedAt(LocalDateTime.now());
        return repo.save(r);
    }

    /** 派工：待派工 → 维修中，必须填写维修人员。 */
    public Repair assign(Long id, String worker) {
        Repair r = get(id);
        if (Dict.isFinalRepair(r.getStatus())) {
            throw new BizException("已修复 / 无法修复 的工单为终态，不可再派工");
        }
        if (!"待派工".equals(r.getStatus())) {
            throw new BizException("只有「待派工」状态的工单可以派工");
        }
        if (worker == null || worker.isBlank()) {
            throw new BizException("派工时必须填写维修人员");
        }
        r.setWorker(worker.trim());
        r.setStatus("维修中");
        r.setUpdatedAt(LocalDateTime.now());
        return repo.save(r);
    }

    /**
     * 完工登记：已修复 / 无法修复。
     * 已修复必须填完工日期且不早于报修日期，并把灯具改回「亮灯」；无法修复把灯具置「报废」。
     */
    public Repair finish(Long id, Repair in) {
        Repair r = get(id);
        if (Dict.isFinalRepair(r.getStatus())) {
            throw new BizException("该工单已完工（" + r.getStatus() + "），不可重复登记");
        }
        String status = in.getStatus();
        if (!Dict.REPAIR_DONE.equals(status) && !Dict.REPAIR_FAILED.equals(status)) {
            throw new BizException("完工结论只能是已修复或无法修复");
        }
        if (!Dict.canTransferRepair(r.getStatus(), status)) {
            throw new BizException("工单状态不能从「" + r.getStatus() + "」直接变更为「" + status + "」");
        }
        LocalDate finishDate = in.getFinishDate();
        if (Dict.REPAIR_DONE.equals(status)) {
            if (finishDate == null) {
                throw new BizException("已修复必须填写完工日期");
            }
            if (finishDate.isBefore(r.getReportDate())) {
                throw new BizException("完工日期不能早于报修日期");
            }
        }
        if (finishDate != null && finishDate.isBefore(r.getReportDate())) {
            throw new BizException("完工日期不能早于报修日期");
        }
        if (finishDate == null) {
            finishDate = LocalDate.now();
        }
        if (in.getCost() != null) {
            if (in.getCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("维修费用不能为负数");
            }
            r.setCost(in.getCost());
        }
        if (in.getWorker() != null && !in.getWorker().isBlank()) {
            r.setWorker(in.getWorker().trim());
        }
        r.setStatus(status);
        r.setFinishDate(finishDate);
        r.setUpdatedAt(LocalDateTime.now());
        Repair saved = repo.save(r);
        lampService.settleStatus(r.getLampId(), Dict.REPAIR_DONE.equals(status) ? Dict.LAMP_ON : Dict.LAMP_SCRAP);
        return saved;
    }
}
