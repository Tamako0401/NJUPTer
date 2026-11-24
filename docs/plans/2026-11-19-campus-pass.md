# 校园通行（在校/在寝状态）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 服务页新增「校园通行」卡片与子页：绑定 0trust cdr JWT（约 3h 有效）后展示在校/在寝状态、最后出行时间地点、宿舍楼栋房号。

**Architecture:** 完全对齐既有晨跑模式——独立 token 解析/持久化、OkHttp 仓库（两个业务接口）、ViewModel 四态机、ServicesScreen 追加卡片、子页 + 绑定弹窗。零信任网关已实测：公网直连，业务请求仅需 `token` header（HS256 真校验、按学号作用域），网关按 User-Agent 拦截非浏览器客户端，OkHttp 默认 UA 可过。

**Tech Stack:** Kotlin + Compose M3 + OkHttp + Gson（全部既有依赖，无新增）。

**Spec:** 本文件即完整规格（数据面与鉴权事实已由 2026-11-19 实测确认，见 Context）。

## Context（已确认的接口事实）

- 网关 `https://0trust-y.njupt.edu.cn:9458`，公网直连，无需 VPN/零信任客户端。
- 业务请求仅要求 `token: <cdr JWT>` header（OkHttp 默认 UA 可过；`Python-urllib` 默认 UA 被网关 403 拦截）。
- cdr JWT payload：`{"iat":<秒>,"exp":<秒>,"no":"<学号>","iss":"cdr"}`，HS256，exp-iat=10800（3h），篡改签名返回 `{"msg":"400","data":"请重新登录"}`。
- `POST /accessRecord/vx/selectStuState`，multipart form `personCode=<学号>` → `{"data":{"code":"<学号>","state":1},"status":200,"msg":"success","time":...}`；`state`: 1=在校 0=不在校。
- `POST /accessRecord/vx/getstubytable`，JSON `{"page":0,"size":1,"yq":"","xh":"<学号>"}` → `data.result[0]`：`inorout`（1|3=在寝，其余=不在寝，无记录=未返校）、`access_DATA` 最后刷脸时间、`yq` 园区、`lh` 楼、`fh` 房号。`xm/xy/bj`（姓名/学院/班级）**不展示**。
- token 获取：企业微信工作台打开校园通行链接（企微自动附加一次性 `?code=`）→ 抓包 `POST /accessRecord/department/vxLogin` 响应 → 复制 `data.token`（整段响应 JSON 亦可）。
- 门禁权限类接口（getCheck/selectByCode/getBlackTime/VxGroup 等）一律不调用。

## Global Constraints

- 所有新文件放 `data/campuspass/`、`viewmodels/`、`ui/services/` 既有包结构；不新建模块。
- 遵循晨跑既有模式：StateFlow 仓库接口扩展、`kotlin.io.encoding.Base64`（JVM 测试可跑）、响应 262145 字节上限、10s/15s/20s 超时。
- 文案中英双语，`values/strings.xml` 与 `values-zh/strings.xml` 键完全对齐。
- UI 状态语义按 SPA：`inorout 1|3 = 在寝`。
- 图标：卡片 `Icons.Default.LocationOn`；在校徽章 `Icons.Default.School`；在寝徽章 `Icons.Default.NightShelter`（material-icons-extended 已有依赖）。

## Review Focus

- cdr JWT 与晨跑 JWT 结构不同（无 userInfo 字符串，直接 `{iat,exp,no,iss}`）——解析器各自独立，禁止复用晨跑解析器。
- `getstubytable` 返回空 `result`（未返校/无记录）→ Ready 页显示「未返校」，不得崩溃或空白。
- 网关按 UA 拦截：仓库必须携带浏览器 UA 或验证 OkHttp 默认 UA 可用（已实测 okhttp UA 可过，直接依赖默认 UA 但测试用 Interceptor 不经过真网关）。
- multipart 端点（selectStuState）误用 JSON body 会返回 Whitelabel Error Page——仓库必须用 FormBody。
- token 过期表现：无独立 errCode，业务接口返回 `{"msg":"400","data":"请重新登录"}`——按 token 失效处理。

---

### Task 1: cdr token 解析器 + 持久化

**Files:**
- Create: `app/src/main/java/com/example/njupter/data/campuspass/CampusPassToken.kt`
- Modify: `app/src/main/java/com/example/njupter/data/SettingsRepository.kt`
- Test: `app/src/test/java/com/example/njupter/data/campuspass/CampusPassTokenParserTest.kt`
- Modify: `app/src/test/java/com/example/njupter/data/FileTimetableRepositoryTest.kt`（FakeSettingsRepository 补新成员）

**Interfaces:**
- Produces: `data class CampusPassToken(raw, studentId, expiresAtEpochSeconds)`；`object CampusPassTokenParser { fun parse(input: String): CampusPassToken? }`——JWT 正则从任意文本提取三段式，payload 直接 JSON（`no`=学号，空/缺失拒绝），`iss=="cdr"` 不强制（宽松）。
- Produces: `SettingsRepository` 新增 `fun getCampusPassToken(): Flow<String?>`、`suspend fun setCampusPassToken(token: String?)`、默认实现 `peekCampusPassToken(): String?`；SharedPreferences 键 `campus_pass_token`。

- [ ] **Step 1: 写失败测试**

`CampusPassTokenParserTest`：
- `parses full cdr token`：手工构造三段式（header `{"alg":"HS256","typ":"JWT"}`、payload `{"iat":1700000000,"exp":1700010800,"no":"test-campus-student","iss":"cdr"}`，ABSENT padding 编码），断言 `studentId=="test-campus-student"`、`expiresAtEpochSeconds==1700010800`、`raw==原串`。
- `extracts token from full vxLogin response json`：`{"data":{"token":"<JWT>","id":"test-campus-student"}}` 文本 → 解析成功。
- `rejects missing no`：payload 无 `no` → null。
- `rejects non token input`：`"abc"`、`""` → null。

- [ ] **Step 2: 运行确认 RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.example.njupter.data.campuspass.*"`
Expected: 编译失败 `Unresolved reference CampusPassTokenParser`。

- [ ] **Step 3: 实现**

`CampusPassToken.kt`：复用晨跑 `MiniProgramToken` 的结构但独立实现——`Base64.UrlSafe.withPadding(PaddingOption.ABSENT)` 解 payload；`JsonParser` 读 `exp`（asLong，缺失 null）与 `no`（asString，isNullOrBlank 拒绝）。
`SettingsRepository.kt`：接口 + 实现照 `morning_run_token` 模式三处（接口声明、StateFlow、getter/setter + KEY 常量）。
`FileTimetableRepositoryTest.kt`：FakeSettingsRepository 补 `campusPassToken = MutableStateFlow<String?>(null)` + getter/setter。

- [ ] **Step 4: 运行确认 GREEN**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.example.njupter.data.campuspass.*" --tests "com.example.njupter.data.FileTimetableRepositoryTest"`
Expected: 全 PASS。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/njupter/data/campuspass/CampusPassToken.kt app/src/main/java/com/example/njupter/data/SettingsRepository.kt app/src/test/java/com/example/njupter/data/campuspass/CampusPassTokenParserTest.kt app/src/test/java/com/example/njupter/data/FileTimetableRepositoryTest.kt
git commit -m "feat(campus-pass): parse and persist 0trust cdr token"
```

### Task 2: CampusPassRepository（TDD）

**Files:**
- Create: `app/src/main/java/com/example/njupter/data/campuspass/CampusPassRepository.kt`
- Test: `app/src/test/java/com/example/njupter/data/campuspass/CampusPassRepositoryTest.kt`

**Interfaces:**
- Consumes: `CampusPassToken.studentId` 作为查询参数。
- Produces: `data class CampusPassSnapshot(onCampus: Boolean?, inDorm: Boolean?, lastAccessTime: String, lastAccessPlace: String, dormBuilding: String, dormRoom: String)`；`class CampusPassRepository(client)`，`suspend fun fetch(token: CampusPassToken): CampusPassSnapshot`；`class CampusPassTokenRejectedException : IOException`（无固定 errCode，msg 400/请重新登录 即失效）。

- [ ] **Step 1: 写失败测试**

Interceptor 按 URL 分流（照 `MorningRunRepositoryTest` 模式）：
- `fetch aggregates state and latest record`：用合成响应。 `selectStuState` 返回 `{"data":{"code":"test-campus-student","state":1},"status":200,"msg":"success"}`， `getstubytable` 保留实测字段结构并使用虚构值。 断言 `onCampus==true`、`inDorm==true`、`lastAccessTime=="2023-11-14 22:13:20"`、`lastAccessPlace=="虚构校区"`、`dormBuilding=="测试宿舍 A 栋"`、`dormRoom=="T-101"`。
- `empty result means not returned`：`getstubytable` 返回空 result → `inDorm==null`、时间/地点为空串。
- `token rejection when msg 400`：两接口均返回 `{"msg":"400","data":"请重新登录","time":...}` → 断言抛 `CampusPassTokenRejectedException`。
- `requests carry token header and form body`：捕获 `token` header 值与 `personCode` form 值断言。

- [ ] **Step 2: 运行确认 RED**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.example.njupter.data.campuspass.CampusPassRepositoryTest"`
Expected: 编译失败。

- [ ] **Step 3: 实现**

OkHttp 模式照 `MorningRunRepository`：`BASE = "https://0trust-y.njupt.edu.cn:9458"`。
- `selectStuState`：`FormBody.Builder().add("personCode", token.studentId)`，POST，读 `data.state`（1→true，0→false，其他→null）。
- `getstubytable`：JSON body `{"page":0,"size":1,"yq":"","xh":<学号>}`，读 `data.result[0]`；空 result → `inDorm=null`、其余空串。
- `inDorm`：`inorout==1||inorout==3` → true；其他数值 → false；无记录 → null。
- 失效判定：`requireSuccess` 风格的 envelope 检查——`msg=="400"` 或 `data=="请重新登录"` → 抛 `CampusPassTokenRejectedException`；其余非 200 `status` 抛 `IOException(msg)`。
- 两接口独立 `runCatching`？**否**——任一失效即抛（状态与记录同源失效无意义）；`getstubytable` 解析失败按空记录处理（不影响主状态）。

- [ ] **Step 4: 运行确认 GREEN**

Run: `./gradlew.bat :app:testDebugUnitTest --tests "com.example.njupter.data.campuspass.*"`
Expected: 全 PASS。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/njupter/data/campuspass/CampusPassRepository.kt app/src/test/java/com/example/njupter/data/campuspass/CampusPassRepositoryTest.kt
git commit -m "feat(campus-pass): fetch on-campus/dorm status from 0trust gateway"
```

### Task 3: CampusPassViewModel + MainActivity 接线 + ServicesScreen 卡片

**Files:**
- Create: `app/src/main/java/com/example/njupter/viewmodels/CampusPassViewModel.kt`
- Modify: `app/src/main/java/com/example/njupter/MainActivity.kt`
- Modify: `app/src/main/java/com/example/njupter/ui/services/ServicesScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`、`app/src/main/res/values-zh/strings.xml`

**Interfaces:**
- Consumes: Task 1 `peekCampusPassToken`/`setCampusPassToken`；Task 2 `CampusPassRepository.fetch`/`CampusPassTokenRejectedException`。
- Produces: `sealed interface CampusPassUiState`（Unbound/Loading/Error(message, tokenExpired)/Ready(token, snapshot)）；`class CampusPassViewModel(repository, settings)`，`uiState: StateFlow<CampusPassUiState>`、`refresh()`、`bind(input): Boolean`、`clearToken()`、`provideFactory(repository, settings)`。
- Produces: `ServicesScreen` 参数扩展 `campusPassSummary: String?, onCampusPassClick: () -> Unit`（卡片 value：未绑定/在校/不在校，Ready 优先显示在校状态）。

- [ ] **Step 1: CampusPassViewModel**

照 `MorningRunViewModel` 结构逐行对齐：init refresh；无 token→Unbound；`exp*1000<=now`→Error(过期, true)；否则 Loading→fetch→Ready；`CampusPassTokenRejectedException`→Error(msg, true)；IOException→Error(msg, false)；bind 解析失败 false、成功存 token+refresh；clearToken 置 Unbound。

- [ ] **Step 2: strings（en/zh 同步追加）**

`campus_pass`（校园通行/Campus Pass）、`campus_pass_summary`（在校与在寝状态/On-campus & dorm status）、`campus_pass_unbound`（未绑定/Not linked）、`campus_pass_on_campus`（在校/On campus）、`campus_pass_off_campus`（不在校/Off campus）、`campus_pass_in_dorm`（在寝/In dorm）、`campus_pass_not_in_dorm`（不在寝/Out of dorm）、`campus_pass_not_returned`（未返校/Not returned）、`campus_pass_unknown`（未知/Unknown）、`campus_pass_bind`（绑定校园通行 token/Link campus-pass token）、`campus_pass_rebind`（重新绑定/Link again）、`campus_pass_unbind`（解绑/Unlink）、`campus_pass_no_token_title`（尚未绑定/Not linked yet）、`campus_pass_no_token_desc`（粘贴校园通行 token 后即可查看在校与在寝状态。/Paste your campus-pass token to view on-campus and dorm status.）、`campus_pass_token_hint`（粘贴 token（或包含 token 的文本）/Paste the token (or any text containing it)）、`campus_pass_token_invalid`（未找到有效 token/No valid token found）、`campus_pass_paste`（从剪贴板粘贴/Paste from clipboard）、`campus_pass_expires`（有效期至 %1$s/Valid until %1$s）、`campus_pass_expired`（token 已过期，请重新绑定。/Token expired. Link again to continue.）、`campus_pass_last_access`（最后出行记录/Last access）、`campus_pass_dorm_location`（宿舍/Dorm）、`campus_pass_how_to_get`（如何获取 token？/How to get a token?）、`campus_pass_guide_title`（如何获取 token/How to get a token）、`campus_pass_guide_step1`（token 只能从企业微信打开的校园通行页面抓取，需要能解密 HTTPS 的抓包环境（任意 HTTPS 抓包工具均可，推荐 Reqable）。/The token can only be captured from the campus-pass page opened in WeCom, so you need an HTTPS-capture setup (any capture tool that decrypts HTTPS; Reqable is a good choice).）、`campus_pass_guide_step2`（在企业微信工作台打开「校园通行」，页面会自动完成登录。/Open Campus Pass from the WeCom workbench; the page signs in automatically.）、`campus_pass_guide_step3`（在抓包工具里过滤 0trust-y.njupt.edu.cn 的请求。/Filter requests to 0trust-y.njupt.edu.cn in the capture tool.）、`campus_pass_guide_step4`（找到 login/department/vxLogin 的响应。/Find the response of /accessRecord/department/vxLogin.）、`campus_pass_guide_step5`（复制响应里 data.token 的值（粘贴整段响应 JSON 也可以）。/Copy the value of data.token in that response (pasting the whole response JSON also works).）、`campus_pass_guide_step6`（token 有效期约 3 小时，过期后需重新抓取。/A token stays valid for about 3 hours; capture a fresh one after it expires.）

- [ ] **Step 3: MainActivity 接线**

`viewModels<CampusPassViewModel>` 工厂（`CampusPassRepository()`, settingsRepository）；`campusPassState` collectAsState；路由 `tab == TAB_SERVICES && page == "campus_pass"` → `CampusPassScreen(...)`；ServicesScreen 传 `campusPassSummary`（Ready→`snapshot.onCampus==true`?"在校":"不在校"，null→未绑定）与 `onCampusPassClick = { subPage = "campus_pass" }`；predictive back 无需改（`subPage != "main"` 已覆盖）。

- [ ] **Step 4: ServicesScreen 卡片**

`queryItems` 追加 `SettingsItem.Navigation(icon = SettingsIcon.Vector(Icons.Default.LocationOn), title = campus_pass, description = campus_pass_summary, value = campusPassSummary ?: stringResource(campus_pass_unbound), onClick = onCampusPassClick)`。

- [ ] **Step 5: 编译 + 全量测试**

Run: `./gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL，全绿。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/njupter/viewmodels/CampusPassViewModel.kt app/src/main/java/com/example/njupter/MainActivity.kt app/src/main/java/com/example/njupter/ui/services/ServicesScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-zh/strings.xml
git commit -m "feat(campus-pass): view model, services card and routing"
```

### Task 4: CampusPassScreen + 绑定弹窗（指南）

**Files:**
- Create: `app/src/main/java/com/example/njupter/ui/services/CampusPassScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`、`app/src/main/res/values-zh/strings.xml`（Task 3 未覆盖的 UI 细节文案若需追加）

**Interfaces:**
- Consumes: Task 3 `CampusPassUiState`、`CampusPassScreen(uiState, onBack, onRefresh, onBind, onClear)`。
- Produces: `@Composable fun CampusPassScreen(...)`，四态全实现；内部复用既有 `MorningRunTokenSheet`？**否**——弹窗文案/指南不同，独立 `CampusPassTokenSheet`（同构实现，独立指南步骤数组）。

- [ ] **Step 1: CampusPassScreen**

结构照 `MorningRunScreen`：TopAppBar（返回 + Unbind(仅 Ready) + Refresh）；Unbound→居中 `LocationOn` 图标 + `campus_pass_no_token_title/desc` + 绑定按钮；Loading→`PullToRefreshBox(isRefreshing = true)` 包 `CircularProgressIndicator`；Error→红字消息（tokenExpired 时用 `campus_pass_expired` 本地化）+ 重试 + 重新绑定；Ready→`PullToRefreshBox(isRefreshing = uiState is Loading)` 包内容 Column：
1. 状态卡：两行徽章——`School` icon + 在校/不在校（`onCampus==null`→未知），`NightShelter` icon + 在寝/不在寝/未返校（`inDorm==null`→未返校）。
2. 最后出行卡：`campus_pass_last_access` 标题 + `"%1$s · %2$s".format(lastAccessTime, lastAccessPlace)`（时间为空→"无"）。
3. 宿舍卡：`campus_pass_dorm_location` 标题 + `"%1$s %2$s".format(dormBuilding, dormRoom)`（全空→隐藏此卡）。

- [ ] **Step 2: CampusPassTokenSheet**

照 `MorningRunTokenSheet` 同构：标题 + `campus_pass_how_to_get` 入口 → `CampusPassTokenGuideDialog`（六步 `campus_pass_guide_step1..6`，AlertDialog scrollable）+ 输入框/粘贴/确定（onBind false→`campus_pass_token_invalid`）。

- [ ] **Step 3: 编译 + 全量测试**

Run: `./gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL，全绿。

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/njupter/ui/services/CampusPassScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-zh/strings.xml
git commit -m "feat(campus-pass): status screen with bind sheet and token guide"
```

### Task 5: 设备实测

**Files:** 无新文件。

- [ ] **Step 1: 模拟器 installDebug + 走查**

服务页两张卡片；进入校园通行页 → 绑定弹窗（入口/指南六步完整）；使用专用测试账号生成临时令牌，仅在设备会话中使用 → force-stop 重启 → Ready 态显示在校/在寝徽章、最后出行时间与地点及宿舍信息；过期 token → Error 态本地化文案 + 重新绑定；清 token 回 Unbound。

- [ ] **Step 2: 视觉核对（截图）**

徽章颜色：在校/在寝绿（`success` 类比晨跑主色），不在/未知中性色；卡片间距与晨跑页一致。

- [ ] **Step 3: Commit（如有修正）**

按实际修正内容提交。
