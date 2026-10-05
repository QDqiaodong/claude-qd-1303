SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;

-- ---------------------------------------------------------------------
-- 城市照明管理所 · 4 模块业务库
-- 库名 city_light；application.yml 里 ddl-auto: none，表结构全部由本文件在 MySQL 镜像构建期导入。
-- 注意：mysql 容器是 UTC，时间戳一律写 NOW()，日期用 DATE_SUB(CURDATE(), INTERVAL N DAY)。
-- 两处 schema（backend/src/main/resources/schema.sql 与 mysql/schema.sql）内容必须完全一致。
-- 外键顺序：road → lamp → repair / inspection（lamp、inspection 都引用 road，repair 引用 lamp）。

CREATE DATABASE IF NOT EXISTS city_light
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE city_light;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS repair;
DROP TABLE IF EXISTS inspection;
DROP TABLE IF EXISTS lamp;
DROP TABLE IF EXISTS road;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------- 1. 路段 road（父表，须最先建立） ----------------
CREATE TABLE road (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    road_no      VARCHAR(32) UNIQUE COMMENT '路段编号，全所唯一',
    name         VARCHAR(64) COMMENT '路段名',
    zone         VARCHAR(16) COMMENT '所属片区：城东 / 城西 / 城南 / 城北 / 开发区',
    length_meter DECIMAL(10,2) COMMENT '路段长度（米），必须大于 0',
    lamp_count   INT DEFAULT 0 COMMENT '灯杆数，与实际灯具数联动（新增 +1 / 报废 -1）',
    status       VARCHAR(16) COMMENT '路段状态：正常 / 施工 / 停用（停用不可新增灯杆、不可排巡检）',
    created_at   DATETIME COMMENT '建档时间',
    updated_at   DATETIME COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 2. 灯具 lamp（引用 road，须在 repair 之前） ----------------
CREATE TABLE lamp (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    lamp_no      VARCHAR(32) UNIQUE COMMENT '灯具编号，全所唯一',
    road_id      BIGINT COMMENT '所属路段 id',
    pole_no      VARCHAR(16) COMMENT '杆号（路段内定位灯杆）',
    type         VARCHAR(16) COMMENT '灯具类型：LED / 高压钠灯 / 太阳能',
    watt         INT COMMENT '功率（W），必须大于 0',
    install_date DATE COMMENT '安装日期',
    status       VARCHAR(16) COMMENT '灯具状态：亮灯 / 故障 / 维修中 / 报废（报废为终态）',
    created_at   DATETIME COMMENT '建档时间',
    updated_at   DATETIME COMMENT '更新时间',
    CONSTRAINT fk_lamp_road FOREIGN KEY (road_id) REFERENCES road (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 3. 巡检记录 inspection（引用 road） ----------------
CREATE TABLE inspection (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    road_id      BIGINT COMMENT '被巡检路段 id',
    inspector    VARCHAR(32) COMMENT '巡检员姓名',
    check_date   DATE COMMENT '巡检日期',
    lit_count    INT DEFAULT 0 COMMENT '亮灯数',
    fault_count  INT DEFAULT 0 COMMENT '故障数（>0 时结论必须是有故障或待复查）',
    status       VARCHAR(16) COMMENT '巡检结论：正常 / 有故障 / 待复查',
    created_at   DATETIME COMMENT '录入时间',
    updated_at   DATETIME COMMENT '更新时间',
    CONSTRAINT fk_inspection_road FOREIGN KEY (road_id) REFERENCES road (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 4. 报修工单 repair（引用 lamp） ----------------
CREATE TABLE repair (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    lamp_id      BIGINT COMMENT '报修灯具 id（只能给「故障」灯具报修）',
    fault_type   VARCHAR(16) COMMENT '故障类型：不亮 / 闪烁 / 破损 / 线路故障',
    report_date  DATE COMMENT '报修日期',
    worker       VARCHAR(32) COMMENT '维修人员（待派工时为空）',
    status       VARCHAR(16) COMMENT '工单状态：待派工 / 维修中 / 已修复 / 无法修复（后两者为终态）',
    finish_date  DATE COMMENT '完工日期（已修复必填，且不能早于报修日期）',
    cost         DECIMAL(10,2) COMMENT '维修费用（元）',
    created_at   DATETIME COMMENT '开单时间',
    updated_at   DATETIME COMMENT '更新时间',
    CONSTRAINT fk_repair_lamp FOREIGN KEY (lamp_id) REFERENCES lamp (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================================
-- 种子数据（全部中文，覆盖每条业务规则；含异常态：停用路段 / 施工路段 /
-- 报废灯具 / 故障灯具 / 已修复工单 / 无法修复工单 / 有故障巡检）
-- =====================================================================

-- ---------------- road 种子：6 条 ----------------
INSERT INTO road (id, road_no, name, zone, length_meter, lamp_count, status, created_at, updated_at) VALUES
(1, 'RD-2026-001', '人民路',   '城东',   1200.00, 6, '正常', NOW(), NOW()),
(2, 'RD-2026-002', '解放路',   '城西',    860.00, 4, '正常', NOW(), NOW()),
(3, 'RD-2026-003', '建设大道', '城南',   1500.00, 2, '施工', NOW(), NOW()),
(4, 'RD-2026-004', '环城北路', '城北',    940.00, 2, '停用', NOW(), NOW()),
(5, 'RD-2026-005', '开发区大道', '开发区', 2000.00, 3, '正常', NOW(), NOW()),
(6, 'RD-2026-006', '文昌街',   '城东',    620.00, 2, '正常', NOW(), NOW());

-- ---------------- lamp 种子：19 条（含故障 / 维修中 / 报废） ----------------
INSERT INTO lamp (id, lamp_no, road_id, pole_no, type, watt, install_date, status, created_at, updated_at) VALUES
(1,  'L-0001', 1, '001', 'LED',       120, DATE_SUB(CURDATE(), INTERVAL 400 DAY), '亮灯',   NOW(), NOW()),
(2,  'L-0002', 1, '002', 'LED',       120, DATE_SUB(CURDATE(), INTERVAL 380 DAY), '故障',   NOW(), NOW()),
(3,  'L-0003', 1, '003', '高压钠灯',   250, DATE_SUB(CURDATE(), INTERVAL 360 DAY), '亮灯',   NOW(), NOW()),
(4,  'L-0004', 1, '004', 'LED',       120, DATE_SUB(CURDATE(), INTERVAL 350 DAY), '维修中', NOW(), NOW()),
(5,  'L-0005', 1, '005', '太阳能',      80, DATE_SUB(CURDATE(), INTERVAL 500 DAY), '报废',   NOW(), NOW()),
(6,  'L-0006', 1, '006', 'LED',       120, DATE_SUB(CURDATE(), INTERVAL 300 DAY), '亮灯',   NOW(), NOW()),
(7,  'L-0007', 2, '001', 'LED',       150, DATE_SUB(CURDATE(), INTERVAL 300 DAY), '亮灯',   NOW(), NOW()),
(8,  'L-0008', 2, '002', '高压钠灯',   250, DATE_SUB(CURDATE(), INTERVAL 290 DAY), '故障',   NOW(), NOW()),
(9,  'L-0009', 2, '003', 'LED',       150, DATE_SUB(CURDATE(), INTERVAL 280 DAY), '亮灯',   NOW(), NOW()),
(10, 'L-0010', 2, '004', '太阳能',      80, DATE_SUB(CURDATE(), INTERVAL 270 DAY), '亮灯',   NOW(), NOW()),
(11, 'L-0011', 3, '001', 'LED',       200, DATE_SUB(CURDATE(), INTERVAL 200 DAY), '亮灯',   NOW(), NOW()),
(12, 'L-0012', 3, '002', 'LED',       200, DATE_SUB(CURDATE(), INTERVAL 190 DAY), '亮灯',   NOW(), NOW()),
(13, 'L-0013', 4, '001', '高压钠灯',   250, DATE_SUB(CURDATE(), INTERVAL 600 DAY), '亮灯',   NOW(), NOW()),
(14, 'L-0014', 4, '002', '高压钠灯',   250, DATE_SUB(CURDATE(), INTERVAL 600 DAY), '报废',   NOW(), NOW()),
(15, 'L-0015', 5, '001', 'LED',       180, DATE_SUB(CURDATE(), INTERVAL 150 DAY), '亮灯',   NOW(), NOW()),
(16, 'L-0016', 5, '002', 'LED',       180, DATE_SUB(CURDATE(), INTERVAL 140 DAY), '亮灯',   NOW(), NOW()),
(17, 'L-0017', 5, '003', '太阳能',     100, DATE_SUB(CURDATE(), INTERVAL 130 DAY), '亮灯',   NOW(), NOW()),
(18, 'L-0018', 6, '001', 'LED',        90, DATE_SUB(CURDATE(), INTERVAL 120 DAY), '亮灯',   NOW(), NOW()),
(19, 'L-0019', 6, '002', 'LED',        90, DATE_SUB(CURDATE(), INTERVAL 110 DAY), '故障',   NOW(), NOW());

-- ---------------- inspection 种子：5 条（含故障数 > 0 的巡检） ----------------
INSERT INTO inspection (id, road_id, inspector, check_date, lit_count, fault_count, status, created_at, updated_at) VALUES
(1, 1, '张巡检', DATE_SUB(CURDATE(), INTERVAL 2 DAY),  4, 2, '有故障', NOW(), NOW()),
(2, 1, '李巡检', DATE_SUB(CURDATE(), INTERVAL 9 DAY),  6, 0, '正常',   NOW(), NOW()),
(3, 2, '王巡检', DATE_SUB(CURDATE(), INTERVAL 5 DAY),  2, 2, '待复查', NOW(), NOW()),
(4, 5, '赵巡检', DATE_SUB(CURDATE(), INTERVAL 3 DAY),  3, 0, '正常',   NOW(), NOW()),
(5, 3, '孙巡检', DATE_SUB(CURDATE(), INTERVAL 1 DAY),  1, 1, '有故障', NOW(), NOW());

-- ---------------- repair 种子：4 条（含已修复 / 无法修复终态工单） ----------------
INSERT INTO repair (id, lamp_id, fault_type, report_date, worker, status, finish_date, cost, created_at, updated_at) VALUES
(1, 2, '不亮',     DATE_SUB(CURDATE(), INTERVAL 5 DAY),  '',       '待派工',   NULL,                                     NULL,    NOW(), NOW()),
(2, 8, '闪烁',     DATE_SUB(CURDATE(), INTERVAL 4 DAY),  '陈维修', '维修中',   NULL,                                     NULL,    NOW(), NOW()),
(3, 3, '破损',     DATE_SUB(CURDATE(), INTERVAL 12 DAY), '刘电工', '已修复',   DATE_SUB(CURDATE(), INTERVAL 9 DAY),      260.00,  NOW(), NOW()),
(4, 5, '线路故障', DATE_SUB(CURDATE(), INTERVAL 20 DAY), '周电工', '无法修复', DATE_SUB(CURDATE(), INTERVAL 15 DAY),     480.00,  NOW(), NOW());
