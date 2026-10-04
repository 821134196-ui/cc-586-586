# 社区图书漂流箱上架与借阅登记系统

居民捐书 → 管理员检查上架 → 读者扫码借还 → 全程可追查的社区图书漂流系统。

- **后端**：Java 17 + Quarkus 3 + Hibernate ORM (Panache)，SQLite 持久化
- **前端**：Svelte 5 + Vite，构建产物由 Quarkus 统一对外服务
- **封面图**：本地文件存储（`backend/data/covers/`）
- **提醒**：模拟接口（写日志 + `backend/data/notifications.log`），可替换为真实短信/微信实现

## 一条命令启动

```bash
./start.sh
```

打开 http://localhost:8080 即可使用（首次启动自动写入演示数据）。
运行测试：`./start.sh test` 或 `cd backend && mvn test`。

> 脚本优先使用系统 `java`/`mvn`，否则使用仓库 `tools/` 下内置的 JDK 17 与 Maven。

## 角色与流程

1. **捐赠登记**（管理后台）：录入书名/ISBN/书况/捐赠人，可勾选匿名；系统分配唯一实体编号 `BK-000001`，打印贴于书上。
2. **检查上架**（管理后台）：确认书况、选择箱位；**箱位满了拒绝上架**。
3. **扫码借书**（读者）：扫描书上二维码（链接形如 `/#/borrow?code=BK-000001`）或输入编号，登记姓名即可带走；**同一实体编号不能同时借给两个人**，同 ISBN 的不同副本互不影响。
4. **扫码还书**（读者）：可放回原箱或**另一只箱**（跨箱归还），系统更新所在位置；归还时发现破损会留下处理记录。
5. **查询**（读者）：按编号/书名/ISBN 查到某本实体书**当前在哪个箱、或是否已借出**（含应还日期），并可查看完整流转历史。
6. **管理**：查看当前借阅记录、一键检查逾期并发送模拟提醒（留处理记录）、查看破损/逾期处理记录、管理箱位与容量。

## 关键设计

| 需求 | 实现 |
| --- | --- |
| 实体编号唯一 | `books.code` 数据库唯一约束，`BK-` + 自增序号；同 ISBN 副本是不同的 `Book` 行 |
| 不能仅凭书名认定同一实物 | 借还/查询一律按实体编号；`GET /api/books/{code}` 精确定位单册 |
| 箱容量限制 | 上架与归还前校验 `在架数 < 容量`，满箱返回 409 |
| 同一实体不同时借两人 | 借出要求状态在架且无 ACTIVE 借阅，违反返回 409 |
| 跨箱归还追查 | `location_events` 记录捐赠/上架/借出/归还全轨迹（含 from/to 箱位） |
| 逾期/破损留记录 | `handling_records`（OVERDUE / DAMAGE），逾期提醒幂等不重复记录 |
| 匿名捐赠者 | 读者视图只显示「匿名捐赠」，身份信息仅管理员视图可见 |

## API 一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/books?q=&status=` | 检索（编号/书名/ISBN/作者） |
| GET | `/api/books/{code}` | 读者视图：当前位置/借出状态 |
| GET | `/api/books/{code}/admin` | 管理员视图（含捐赠人、借阅人） |
| GET | `/api/books/{code}/history` | 完整流转历史 |
| GET | `/api/books/pending` | 待上架列表 |
| POST | `/api/donations` | 捐赠登记（分配实体编号） |
| POST | `/api/books/{code}/shelve` | 检查上架（箱满 409） |
| POST | `/api/books/{code}/cover` | 上传封面（multipart） |
| GET | `/api/covers/{file}` | 读取本地封面 |
| POST | `/api/borrow` | 扫码借书 |
| POST | `/api/returns` | 扫码还书（可跨箱、可上报破损） |
| GET | `/api/boxes` / POST | 箱位列表（含占用）/ 新增箱位 |
| GET | `/api/loans?active=true` | 当前借阅记录 |
| POST | `/api/admin/remind-overdue` | 逾期检查 + 模拟提醒 |
| GET | `/api/admin/handling-records` | 处理记录 |

## 目录结构

```
├── start.sh                 # 一条命令启动
├── backend/                 # Quarkus + Hibernate + SQLite
│   ├── src/main/java/org/community/bookbox/
│   │   ├── model/           # Box/Book/Donor/Loan/HandlingRecord/LocationEvent
│   │   ├── service/         # 业务规则 + 模拟通知接口
│   │   └── web/             # REST 资源与 DTO
│   └── src/test/            # 编号唯一 / 箱容量 / 跨箱归还等验证测试
├── frontend/                # Svelte + Vite（查书/借书/还书/管理后台）
└── tools/                   # 本地 JDK 与 Maven（被 .gitignore 忽略）
```

## 验证测试

`backend/src/test/java/org/community/bookbox/BookFlowTest.java` 覆盖需求要求的三个验证点：

1. **实体编号唯一**：同 ISBN 两册获得不同编号且可分别借出；同一编号重复借出返回 409；
2. **箱容量限制**：容量为 1 的箱上架第二本书返回 409「已满」；
3. **跨箱归还**：从 A 箱借出、归还到 B 箱后，查询显示位于 B 箱，流转历史完整（捐赠→上架A→借出→归还B）。

另有逾期提醒（模拟接口 + 处理记录幂等）、破损归还留痕、匿名捐赠者对读者隐藏等测试，共 8 个用例。
