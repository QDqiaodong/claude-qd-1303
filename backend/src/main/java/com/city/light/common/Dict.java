package com.city.light.common;

import java.util.List;
import java.util.Map;

/**
 * 城市照明管理所业务词典与规则参数表：片区划分、路段状态流转、灯具类型与状态流转、
 * 巡检结论、报修故障类型与工单状态流转。规则集中在此，便于出题时被要求调整或扩展。
 */
public final class Dict {

    private Dict() {
    }

    // ---------------- 全局片区 ----------------
    /** 城东 / 城西 / 城南 / 城北 / 开发区 */
    public static final List<String> ZONES = List.of("城东", "城西", "城南", "城北", "开发区");

    /** 前端片区下拉的「全部」哨兵值 */
    public static final String ZONE_ALL = "全部";

    // ---------------- 路段 road ----------------
    /** 正常 / 施工 / 停用 */
    public static final List<String> ROAD_STATUS = List.of("正常", "施工", "停用");

    /** 可新增灯杆、可排巡检的路段状态：只有「正常」 */
    public static final String ROAD_NORMAL = "正常";

    /** 停用：不可新增灯杆、不可排巡检 */
    public static final String ROAD_DISABLED = "停用";

    /** 路段状态机：正常↔施工，任意状态可转停用，停用可恢复为正常 */
    public static final Map<String, List<String>> ROAD_FLOW = Map.of(
            "正常", List.of("施工", "停用"),
            "施工", List.of("正常", "停用"),
            "停用", List.of("正常"));

    // ---------------- 灯具 lamp ----------------
    /** LED / 高压钠灯 / 太阳能 */
    public static final List<String> LAMP_TYPES = List.of("LED", "高压钠灯", "太阳能");

    /** 亮灯 / 故障 / 维修中 / 报废 */
    public static final List<String> LAMP_STATUS = List.of("亮灯", "故障", "维修中", "报废");

    /** 终态：报废不可再改 */
    public static final String LAMP_SCRAP = "报废";

    /** 只有「故障」灯具可以报修 */
    public static final String LAMP_FAULT = "故障";

    /** 维修完成回到「亮灯」 */
    public static final String LAMP_ON = "亮灯";

    /** 灯具状态机：亮灯→故障，故障→维修中，维修中→亮灯，报废为终态 */
    public static final Map<String, List<String>> LAMP_FLOW = Map.of(
            "亮灯", List.of("故障", "报废"),
            "故障", List.of("维修中", "报废"),
            "维修中", List.of("亮灯"),
            "报废", List.of());

    // ---------------- 巡检记录 inspection ----------------
    /** 正常 / 有故障 / 待复查 */
    public static final List<String> INSPECT_STATUS = List.of("正常", "有故障", "待复查");

    /** 故障数大于 0 时，巡检结论只能是「有故障」或「待复查」 */
    public static final List<String> INSPECT_FAULT_STATUS = List.of("有故障", "待复查");

    // ---------------- 报修工单 repair ----------------
    /** 不亮 / 闪烁 / 破损 / 线路故障 */
    public static final List<String> FAULT_TYPES = List.of("不亮", "闪烁", "破损", "线路故障");

    /** 待派工 / 维修中 / 已修复 / 无法修复 */
    public static final List<String> REPAIR_STATUS = List.of("待派工", "维修中", "已修复", "无法修复");

    /** 终态：已修复 / 无法修复 不可再改 */
    public static final List<String> REPAIR_FINAL = List.of("已修复", "无法修复");

    /** 已修复：灯具回到「亮灯」 */
    public static final String REPAIR_DONE = "已修复";

    /** 无法修复：灯具置「报废」 */
    public static final String REPAIR_FAILED = "无法修复";

    /** 工单状态机：待派工→维修中，维修中→已修复 / 无法修复，两个终态不可回退 */
    public static final Map<String, List<String>> REPAIR_FLOW = Map.of(
            "待派工", List.of("维修中", "无法修复"),
            "维修中", List.of("已修复", "无法修复"),
            "已修复", List.of(),
            "无法修复", List.of());

    // ---------------- 取值方法 ----------------

    public static boolean isValidZone(String z) {
        return z != null && ZONES.contains(z);
    }

    public static boolean isValidRoadStatus(String s) {
        return s != null && ROAD_STATUS.contains(s);
    }

    public static boolean isValidLampType(String t) {
        return t != null && LAMP_TYPES.contains(t);
    }

    public static boolean isValidLampStatus(String s) {
        return s != null && LAMP_STATUS.contains(s);
    }

    public static boolean isValidInspectStatus(String s) {
        return s != null && INSPECT_STATUS.contains(s);
    }

    public static boolean isValidFaultType(String t) {
        return t != null && FAULT_TYPES.contains(t);
    }

    public static boolean isValidRepairStatus(String s) {
        return s != null && REPAIR_STATUS.contains(s);
    }

    /** 是否终态工单（不可再修改） */
    public static boolean isFinalRepair(String s) {
        return s != null && REPAIR_FINAL.contains(s);
    }

    /** 片区过滤：null / 空 / 「全部」都视为不过滤 */
    public static boolean isZoneFilter(String z) {
        return z != null && !z.isBlank() && !ZONE_ALL.equals(z);
    }

    /**
     * 路段状态流转校验：正常↔施工；停用只能恢复为正常。
     */
    public static boolean canTransferRoad(String from, String to) {
        if (!isValidRoadStatus(from) || !isValidRoadStatus(to)) {
            return false;
        }
        List<String> next = ROAD_FLOW.get(from);
        return next != null && next.contains(to);
    }

    /**
     * 灯具状态流转校验：报废为终态不可回退；只有「故障」才能转「维修中」。
     */
    public static boolean canTransferLamp(String from, String to) {
        if (!isValidLampStatus(from) || !isValidLampStatus(to)) {
            return false;
        }
        if (LAMP_SCRAP.equals(from)) {
            return false;
        }
        List<String> next = LAMP_FLOW.get(from);
        return next != null && next.contains(to);
    }

    /**
     * 报修工单状态流转校验：已修复 / 无法修复 为终态不可回退。
     */
    public static boolean canTransferRepair(String from, String to) {
        if (!isValidRepairStatus(from) || !isValidRepairStatus(to)) {
            return false;
        }
        List<String> next = REPAIR_FLOW.get(from);
        return next != null && next.contains(to);
    }
}
