# 社区图书漂流箱

居民捐书 → 管理员审核上架 → 读者扫码借还的社区图书漂流系统。

- **后端**：Java 17 + Quarkus 3 + Hibernate ORM (Panache) + SQLite
- **前端**：Svelte 4 + Vite（构建产物由 Quarkus 托管，单端口访问）
- **数据**：SQLite 本地文件（`data/library.db`），封面存 `data/covers/`
- **提醒**：模拟接口，写入 `data/notifications.log` 并打印到控制台

## 一键启动

```bash
./start.sh
```

打开 http://localhost:8080 。首次启动自动建表并写入演示数据
（3 个箱、若干在架书、在借/逾期/跨箱归还记录、一笔待审核捐赠）。

> 需要 JDK 17+、Maven 3.9+、Node 18+。无 Node 时可用 `./start.sh --skip-frontend`
> 跳过前端构建（需已有构建产物）。

## 运行测试

```bash
cd backend && mvn test
```

覆盖核心规则：实体编号唯一、箱满拒绝上架、跨箱归还后位置与完整流转、
同 ISBN 分别借出、同一实体编号不能同时借给两人、匿名捐赠者不外泄、
逾期/破损处理记录。

## 核心概念

| 概念 | 说明 |
| --- | --- |
| 实体编号 | 每本实物书一个全局唯一编号 `BK-XXXXXX`，借还定位均以编号为准；同书名/同 ISBN 是不同实物 |
| 箱位 | 每个箱有容量上限，在架数达到容量后拒绝继续上架/归还 |
| 借阅 | 借出 30 天应还；同一实体编号同时只能借给一人 |
| 归还 | 可还到任意有空位的箱，系统自动更新所在位置 |
| 流转 | 捐赠、上架、借出、归还、破损、重新上架全程留痕，可按编号追查 |
| 处理记录 | 逾期（定时扫描 + 手动触发）与归还时发现破损都会生成处理记录 |
| 匿名捐赠 | 读者接口（查书/详情/流转）绝不返回捐赠者信息 |

## 主要接口

读者：
- `GET /api/books/search?q=` 按编号/书名/ISBN/作者检索
- `GET /api/copies/{copyCode}` 查某本实体书当前位置或是否借出
- `GET /api/copies/{copyCode}/history` 完整流转轨迹
- `POST /api/loans` 借书 `{copyCode, borrowerName, borrowerContact}`
- `POST /api/loans/return` 还书 `{copyCode, boxCode, damageNote?}`

管理：
- `POST /api/admin/donations` 登记捐赠（自动分配实体编号）
- `POST /api/admin/donations/{id}/approve|reject` 审核并上架
- `GET /api/admin/loans` 当前借阅记录
- `GET /api/admin/incidents` 逾期/破损处理记录
- `POST /api/admin/reminders/run` 手动触发逾期扫描与模拟提醒
- `POST /api/admin/copies/{copyCode}/cover` 上传封面（multipart）

## 开发模式

```bash
# 后端（8080，热重载；首次运行先建数据目录）
cd backend && mkdir -p data && mvn quarkus:dev

# 前端（5173，代理 /api 到 8080）
cd frontend && npm install && npm run dev
```

## 目录

```
backend/    Quarkus 后端（实体、REST、定时逾期扫描、种子数据）
frontend/   Svelte 前端（查书/借书/还书/管理）
data/       运行时生成：library.db、covers/、notifications.log
start.sh    一键启动
```
