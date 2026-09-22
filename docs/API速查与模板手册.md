# 苍穹外卖 · API 速查与模板手册

> 用途:**写代码时照着查,不要背**。本手册内容全部来自本项目真实代码,代码变了记得回来更新。
> 拿不准的时候,优先回到项目里搜(`Ctrl+Shift+F`),而不是硬想。

---

## 〇、三条铁律(比记 API 名字重要 10 倍)

1. **先搜后写**:不知道怎么写 → 搜同类关键词 → 找到项目里的先例 → 抄结构改细节。
2. **先 SQL 后 Java**:先在数据库里把 SQL 跑通,再往 Mapper 里搬。SQL 都不对,Java 写得再漂亮也没用。
3. **改完必三查**:① IDEA 控制台 debug SQL ② 数据库里的真实数据 ③ 前端页面表现。三者一致才算"完成"。

---

## 一、一图看懂数据流(每个功能先在心里画一遍)

```
浏览器 (Vue 管理端, nginx 80 端口托管)
  │  POST /api/dish        ← 前端 api/dish.ts 里的 addDish()
  ▼
nginx:80   location /api/  →  proxy_pass http://localhost:8080/admin/
  ▼
Spring Boot:8080
  ├─ JwtTokenAdminInterceptor   拦截 /admin/**,校验请求头 token → BaseContext.setCurrentId(empId)
  ├─ Controller                 @RestController,只负责收参数、调 Service、包 Result
  ├─ Service                    @Service,业务步骤 + @Transactional(事务边界)
  ├─ Mapper                     @Mapper,只写 SQL(注解或 XML)
  └─ AutoFillAspect             @Before 拦截带 @AutoFill 的 Mapper 方法,反射填 4 个公共字段
  ▼
MySQL:3306  sky_take_out 库(dish / dish_flavor / category / employee ...)
```

**记住这条分层的意义**:任何"接口 404 / 参数没收到 / 数据没落库"的问题,都可以沿着这条线一层层定位。

| 现象 | 大概率在哪一层 |
|---|---|
| 404 | 路径不对(Controller 的 `@RequestMapping` + `@PostMapping` 拼起来是什么?) |
| 401 | token 没带 / 过期 / 被拦截器拦下 |
| 参数为 null | DTO 字段名和 JSON key 不一致,或该用 `@RequestBody` 用了普通参数 |
| 500 + SQL 报错 | Mapper 的 SQL / 动态标签 / 参数名问题 |
| 数据没进库 | 事务回滚(看控制台异常)或 SQL 根本没执行 |

---

## 二、场景 → 关键词速查表(正文,想不起名字就查这里)

| 我要干的事 | 搜这些关键词 | 本项目现成例子 |
|---|---|---|
| DTO 属性拷贝到实体 | `BeanUtils.copyProperties` | `CategoryServiceImpl.save`、`DishServiceImpl.savewithflavor` |
| 只设置几个字段,不想写一堆 setter | `.builder()`(Lombok `@Builder`) | `CategoryServiceImpl.startOrStop`、`EmployeeController.login` 里的 `EmployeeLoginVO.builder()` |
| 新增后要拿到自增主键 | `useGeneratedKeys="true" keyProperty="id"` | `DishMapper.xml` 的 `insert` |
| 一次插入多条数据 | `<foreach collection item separator>` + `@Param` | `DishFlavorMapper.xml` 的 `insertBatch` |
| 条件不确定的动态查询 | `<where>` + `<if test="...">` | `CategoryMapper.xml` 的 `pageQuery` |
| 只更新非空字段 | `<set>` + `<if>` | `CategoryMapper.xml` 的 `update` |
| 分页 | `PageHelper.startPage` + `Page<T>` + `PageResult` | `CategoryServiceImpl.pageQuery`、`EmployeeServiceImpl.pageQuery` |
| 多张表写入必须同生共死 | `@Transactional` | `DishServiceImpl.savewithflavor` |
| 统一返回给前端 | `Result.success()` / `Result.success(data)` / `Result.error(msg)` | 所有 Controller |
| 业务出错要提示用户 | `throw new XxxException(MessageConstant.XXX)` | `EmployeeServiceImpl.login`、`CategoryServiceImpl.deleteById` |
| 全局兜住业务异常 | `@RestControllerAdvice` + `@ExceptionHandler` | `GlobalExceptionHandler` |
| 拿当前登录员工 id | `BaseContext.getCurrentId()` | `AutoFillAspect`、`EmployeeServiceImpl.save` |
| 打日志(**别用 `System.out`**) | `@Slf4j` + `log.info("xx:{}", v)` | `CommonController`、`EmployeeController` |
| 读 yml 配置到对象 | `@ConfigurationProperties(prefix = "sky.xxx")` | `AliOssProperties`、`JwtProperties` |
| 把第三方工具类交给容器管理 | `@Bean` + `@ConditionalOnMissingBean` | `OssConfiguration` |
| 接收上传的文件 | `MultipartFile` + `file.getBytes()` + `file.getOriginalFilename()` | `CommonController.upload` |
| 拦截请求做校验 | `HandlerInterceptor` + `addInterceptors` | `JwtTokenAdminInterceptor`、`WebMvcConfiguration` |
| 生成/解析 token | `JwtUtil.createJWT` / `JwtUtil.parseJWT` | `EmployeeController.login` / `JwtTokenAdminInterceptor` |
| 生成不重复文件名 | `UUID.randomUUID().toString()` | `CommonController.upload` |
| 集合过滤/转换 | `list.stream().filter(...).collect(Collectors.toList())` | 可参考课程,本项目的口味过滤还没写 |
| 密码 MD5 | `DigestUtils.md5DigestAsHex(password.getBytes())` | `EmployeeServiceImpl.login` |
| 常用 Lombok | `@Data @Builder @NoArgsConstructor @AllArgsConstructor @Slf4j` | 实体、VO、DTO |

---

## 三、四套核心模板(结构背下来,细节查着写)

### 模板 A:Mapper 动态查询(注解 + XML 混合)

`sky-server/src/main/resources/mapper/CategoryMapper.xml`

```xml
<mapper namespace="com.sky.mapper.CategoryMapper">
    <select id="pageQuery" resultType="com.sky.entity.Category">
        select * from category
        <where>
            <if test="name != null and name != ''">
                and name like concat('%',#{name},'%')
            </if>
            <if test="type != null">
                and type = #{type}
            </if>
        </where>
        order by sort asc, create_time desc
    </select>
</mapper>
```

逐行要点:
- `namespace` 必须是 Mapper **接口全限定名**,写错了就是 `Invalid bound statement`。
- `<where>` 会自动去掉第一个多余的 `and`,不用自己写 `where 1=1`。
- `<if test="...">` 里的名字是 **DTO 的属性名**,不是数据库字段名。
- `#{name}` 是预编译占位符(防 SQL 注入);`${name}` 是字符串拼接(危险,除了排序字段这种不得已的情况别用)。
- 用 `like concat('%',#{name},'%')`,不要用 `like '%#{name}%'`。
- 入参是单个对象时不需要 `@Param`;是集合/多个基本类型时必须加 `@Param`。

### 模板 B:分页(PageHelper)

Service 层(`CategoryServiceImpl.pageQuery`):

```java
public PageResult pageQuery(CategoryPageQueryDTO dto) {
    PageHelper.startPage(dto.getPage(), dto.getPageSize());   // 只对"紧随其后的那一条查询"生效
    Page<Category> page = categoryMapper.pageQuery(dto);       // 返回类型用 Page<T>,才能拿到 total
    return new PageResult(page.getTotal(), page.getResult());
}
```

Controller 层:

```java
@GetMapping("/page")
public Result<PageResult> page(CategoryPageQueryDTO dto) {   // GET 的 query 参数,不用 @RequestBody
    return Result.success(categoryService.pageQuery(dto));
}
```

要点:
- `PageHelper.startPage` **只影响下一条 SQL**,所以它必须紧挨着那条查询。
- Mapper 方法声明成 `Page<T> xxx(...)`,PageHelper 会自动往 SQL 里拼 `limit` —— 所以 XML 里**不要自己写 limit**。
- 返回给前端用 `PageResult(total, records)`,`PageResult` 有 `@AllArgsConstructor`,所以可以直接 new。
- PageHelper 依赖已经在 `sky-server/pom.xml`(pagehelper-spring-boot-starter 1.3.0),不用再加。

### 模板 C:多表事务写入(新增菜品 + 口味)

Service(`DishServiceImpl.savewithflavor`):

```java
@Transactional                                              // ① 两张表同生共死
public void savewithflavor(DishDTO dishDTO) {
    Dish dish = new Dish();
    BeanUtils.copyProperties(dishDTO, dish);                // ② DTO → 实体
    dishMapper.insert(dish);                                // ③ 插主表
    Long dishId = dish.getId();                             // ④ 拿回填的主键
    List<DishFlavor> flavors = dishDTO.getFlavors();
    if (flavors != null && flavors.size() > 0) {
        flavors.forEach(f -> f.setDishId(dishId));          // ⑤ 子表补外键
        dishFlavorMapper.insertBatch(flavors);              // ⑥ 批量插子表
    }
}
```

主表 XML(`DishMapper.xml`)—— **`useGeneratedKeys` 是模板 C 的命门**:

```xml
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    insert into dish(name, category_id, price, image, description, status,
                     create_time, update_time, create_user, update_user)
    values (#{name}, #{categoryId}, #{price}, #{image}, #{description}, #{status},
            #{createTime}, #{updateTime}, #{createUser}, #{updateUser})
</insert>
```

子表 XML(`DishFlavorMapper.xml`):

```xml
<insert id="insertBatch">
    insert into dish_flavor(dish_id, name, value) VALUES
    <foreach collection="flavors" item="df" separator=",">
        (#{df.dishId}, #{df.name}, #{df.value})
    </foreach>
</insert>
```

对应 Mapper 接口(**这里必须加 `@Param`**):

```java
void insertBatch(@Param("flavors") List<DishFlavor> flavors);
```

要点:
- **顺序不能反**:先插主表拿到 id,再给子表补外键。
- `useGeneratedKeys + keyProperty="id"` 的作用:把数据库自增主键写回**入参对象**的 id 字段。缺了它 → `dish.getId()` 为 null → 子表 `dish_id` 为 null → `dish_id bigint NOT NULL` 直接报 1048 → 事务回滚,菜和口味一起没了。(这个坑本项目已经踩过并修复)
- `@Transactional` 生效前提:方法是被 Spring 代理调用的(跨类注入调用)。**同类内部 `this.xxx()` 自调用会失效。**
- `collection="flavors"` 这个名字来自 `@Param("flavors")`;不加 `@Param` 时 MyBatis 只认 `list` / `collection`。

### 模板 D:横切关注点(AOP + 拦截器 + 配置类)

**D1 公共字段自动填充** `AutoFillAspect`:
```
拦触点: execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)
时机:   @Before,SQL 执行前
做法:   反射调用 setCreateTime/setCreateUser/setUpdateTime/setUpdateUser(方法名来自 AutoFillConstant)
数据来自: BaseContext.getCurrentId() —— 由 JwtTokenAdminInterceptor 在 preHandle 里塞进去的
```
使用方式:在需要填充的 Mapper 方法上加 `@AutoFill(OperationType.INSERT / UPDATE)`。

**D2 登录鉴权链**(理解这条链 = 理解整个后端的"上下文"):
```
前端登录 → EmployeeController.login
  → Service 校验账号/密码(MD5 比对)
  → JwtUtil.createJWT(secretKey, ttl, {empId: ...}) 生成 token
  → 返回 EmployeeLoginVO(token);前端存到 cookie
之后每次请求:
  请求头带 token → JwtTokenAdminInterceptor.preHandle
  → JwtUtil.parseJWT 校验 → BaseContext.setCurrentId(empId)
  → 后续 Service / 切面都能用 BaseContext.getCurrentId()
  （失败则 response.setStatus(401) 并 return false）
```

**D3 配置类 + 工具类装配**:`AliOssProperties`(读 yml)→ `AliOssUtil`(干活,普通类)→ `OssConfiguration`(用 `@Bean` 装配)。

---

## 四、新增一个功能的 7 步清单(照着勾)

- [ ] **1. 看原型/需求**:谁在用?点了什么按钮?要什么结果?
- [ ] **2. 查接口文档**:`资料\day01\项目接口\苍穹外卖-管理端接口.json`,确认路径 / 方法 / 请求体 / 响应体(**不要自己发明格式**)
- [ ] **3. 查数据库设计**:`资料\day01\数据库\数据库设计文档.md`,确认落哪张表、哪些字段、谁是外键
- [ ] **4. 先在 Navicat 把 SQL 写通**(增删改查 + 联表)
- [ ] **5. 写代码(固定顺序)**:DTO/Entity → Mapper 接口 + XML → Service(事务) → Controller
- [ ] **6. 自测**:`http://localhost:8080/doc.html`(Knife4j)先调通,再看控制台 debug SQL
- [ ] **7. 验证三查**:控制台 SQL / 数据库数据 / 前端页面;最后清理测试数据(先删子表再删主表)

---

## 五、常量与业务约定(这些查不到,必须记)

| 项 | 值 / 位置 | 说明 |
|---|---|---|
| `Result.code` | 1 = 成功,0 或其他 = 失败 | 前端判断 `res.data.code === 1` |
| `StatusConstant` | `ENABLE=1`、`DISABLE=0` | 员工/菜品/分类的启停 |
| 分类 `type` | 1 = 菜品分类,2 = 套餐分类 | 前端 `GET /category/list?type=1` |
| token 请求头名 | `sky.jwt.admin-token-name` = `token` | 见 `application.yml` |
| JWT 载荷 key | `JwtClaimsConstant.EMP_ID` = `empId` | 登录时 put,拦截器里 get |
| 密码 | `PasswordConstant.DEFAULT_PASSWORD` = `123456`,存库为 **32 位小写 MD5** | 前后端/数据库三边必须一致 |
| 口味 `value` | **字符串形式的 JSON 数组**,如 `["无糖","少糖"]` | 前端 `JSON.stringify` 后传,回显时 `JSON.parse` |
| 表关系 | `dish_flavor.dish_id → dish.id`;`dish.category_id → category.id` | 主从表,`dish_id` 为 NOT NULL |
| 端口 | nginx 80 / 后端 8080 / MySQL 3306 | nginx 把 `/api/` 反代到 `localhost:8080/admin/` |

---

## 六、排查工具箱

**1. 看真实 SQL**(最有用)
`application.yml` 里已经开了:
```yaml
logging:
  level:
    com:
      sky:
        mapper: debug
```
控制台会打印 SQL、参数、返回行数。

**2. 接口单测**:`http://localhost:8080/doc.html`(Knife4j,由 `WebMvcConfiguration.docket()` 生成)

**3. 查库验证**(Navicat,或命令行 mysql)
```sql
-- 新增后核对主从表
SELECT * FROM dish         WHERE id = (SELECT MAX(id) FROM dish);
SELECT * FROM dish_flavor  WHERE dish_id = (SELECT MAX(id) FROM dish);
-- 清理测试数据(先子后主)
DELETE FROM dish_flavor WHERE dish_id = ?;
DELETE FROM dish        WHERE id = ?;
```

**4. 前端/nginx 侧**
- nginx 目录:`E:\forheadnginx\nginx-1.20.2`(配置文件 `conf\nginx.conf`,访问日志 `logs\access.log`,错误日志 `logs\error.log`)
- `access.log` 能看到每次请求的**状态码**,是判断"前端到底发没发请求"的第一手证据。
- `error.log` 里 `10061 refused` = 后端 8080 没启动。

**5. 常见报错 → 原因对照**

| 报错 | 原因 |
|---|---|
| `Invalid bound statement (not found)` | XML 的 `namespace`/`id` 和 Mapper 接口对不上,或 `mapper-locations` 没扫到 |
| `Parameter 'xxx' not found` | XML 里的参数名和 `@Param`/属性名不一致 |
| `Column 'xxx' cannot be null`(1048) | 外键/必填字段没赋值(如主键回填失败) |
| `Duplicate entry 'xxx' for key` | 唯一索引冲突(本项目 employee.username、dish.name 都有唯一约束) |
| `MaxUploadSizeExceededException` | 上传文件超过 multipart 限制(默认单文件 1MB) |
| 401 | token 缺失/过期,或被拦截器拦下 |
| 数据没进库但接口返回成功 | 事务被回滚(看异常)或压根没执行 SQL |
| `@Transactional` 不生效 | 同类内部自调用(没走代理)或方法不是 public |

---

## 七、高频坑清单(本项目踩过的都在这)

1. **主键回填**:`DishMapper.insert` 必须写 `useGeneratedKeys="true" keyProperty="id"`。✔ 已修复
2. **`@Param` 不能省**:`DishFlavorMapper.insertBatch` 目前靠编译参数 `-parameters` 侥幸能用 `collection="flavors"`,建议补上 `@Param("flavors")` 才稳。
3. **口味没有校验**:`DishServiceImpl` 对 `flavors` 只判了 null/size,`name` 为空的行也会入库(库里已有两条空记录)。建议过滤空 name。
4. **脏数据清理**:`DELETE FROM dish_flavor WHERE dish_id = 70 AND name = '';`
5. **上传大小限制未配置**:默认单文件 1MB,前端提示 2M。要支持就得加
   ```yaml
   spring:
     servlet:
       multipart:
         max-file-size: 10MB
         max-request-size: 10MB
   ```
6. **上传文件名后缀**:`lastIndexOf(".")` 返回 -1 时 `substring(-1)` 会抛 `StringIndexOutOfBoundsException`,不是 IOException,不会被 Controller 的 catch 接住。
7. **OSS 异常被吞**:`AliOssUtil` catch 住异常只打印,然后照样返回 URL → 上传其实失败但接口返回"成功"。
8. **`GlobalExceptionHandler` 里 SQL 异常解析有 bug**:`String[] split = message.split("")` 会把字符串拆成单个字符数组,`split[2]` 取不到用户名,应该用正则提取 `Duplicate entry 'xxx'`。
9. **敏感配置**:`application-oss.yml` 里有明文 AccessKey,别推到公开仓库。
10. **密码三边一致**:代码按 MD5 比对时,数据库里也必须是 MD5,否则永远"密码错误"。
11. **`System.out.println` 满天飞**:临时调试可以,记得清理或换成 `log.info`(项目里已有几处,包括打印 `categoryMapper` 真实类型)。
12. **前端口味下拉要点两下**:`html\sky\js\shopTable.fe534d8f.js` 里 `e.mak=t}),200` 的 200ms 是"失焦即关闭"的延时,改成 2000 可变成单击可用(改前备份,改后 `Ctrl+Shift+R` 强刷)。

---

## 八、我的笔记区(每天抄 3 条,不背)

| 日期 | 场景(我要干什么) | 写法(抄下来) | 出处(文件:行) |
|---|---|---|---|
| | | | |
| | | | |
| | | | |

> 抄一星期你就会发现:高频 API 也就 30~40 个,来回组合而已。

---

# 附录 A:主键 id 从需求到接口(全链路)

> 为什么单独讲 id:它是新增功能里**唯一由数据库生成、又必须被代码拿回来**的字段。搞懂它 = 打通"表 → SQL → 代码 → 接口"四层。

## A1 需求层:id 解决什么问题

1. 前端行标识(表格 `key`、选中、操作按钮定位到哪一行);
2. 后续操作的入参(`GET /admin/dish/{id}`、`DELETE /admin/dish?ids=1,2`、起售停售);
3. 子表外键(`dish_flavor.dish_id = dish.id`);
4. 排障:日志里打 id 才能顺着查 SQL;
5. 幂等/追溯:重试时用 id 定位同一条记录。

**认知**:id 是**技术主键**,不是业务字段。它不需要有意义,只要唯一、稳定、短。

## A2 表设计层

```sql
`id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
PRIMARY KEY (`id`),
UNIQUE KEY `idx_dish_name` (`name`)          -- 业务唯一约束(菜名不重复),不是主键
```

| 决策 | 为什么 |
|---|---|
| `bigint` 而非 `int` | 以后用雪花算法/分库分表会瞬间超过 int 上限,换类型代价大 |
| 不加 `unsigned` | unsigned 上限超过 Java `Long` 可表示范围,映射会错乱;`bigint` 有符号 ↔ `Long` 一一对应 |
| `AUTO_INCREMENT` 由数据库分配 | 天然解决并发冲突,不依赖应用时钟/机器号 |
| 主键 vs 唯一键 | 主键回答"这条记录是谁"(技术),唯一键回答"业务上什么不许重复"(菜名/账号) |

**原理**:InnoDB 的主键就是**聚簇索引**,数据按主键顺序物理存放;二级索引叶子节点存的是**主键值**。所以主键越**短**、越**有序**越好(避免页分裂、减小二级索引)。

## A3 三种主键生成方案

| 方案 | 优点 | 缺点 | 本项目 |
|---|---|---|---|
| 数据库自增 | 简单、有序、短、并发安全 | 分库分表冲突;需一次 INSERT 才拿到 | ✅ 主键 |
| UUID | 应用端生成、全局唯一 | 36 字符太长、随机导致页分裂写放大 | ✅ 只做**上传文件名** |
| 雪花算法 | 全局唯一、趋势递增 | 依赖时钟;值大 → 前端精度问题 | 分布式阶段 |

## A4 Java 层

| 位置 | 写法 | 原因 |
|---|---|---|
| 实体 `Dish` | `private Long id;` | **必须包装类型**:`long` 默认 0 无法表达"未分配";回填靠 setter;`Long` ↔ `bigint` |
| DTO `DishDTO` | `private Long id;` | 新增/修改共用 DTO,靠有没有 id 区分语义(前端 `delete params.id`) |
| VO | 如 `EmployeeLoginVO.id` | 返回给前端展示/后续调用 |

**必须兜住的一行**(安全不能依赖前端):
```java
Dish dish = new Dish();
BeanUtils.copyProperties(dishDTO, dish);   // ← 会把 DTO 的 id 一起拷过来!
dish.setId(null);                          // ★ 新增语义:主键必须由数据库分配
dishMapper.insert(dish);
```

## A5 SQL 层

```xml
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    insert into dish(name, category_id, ... , update_user)   <!-- 列清单里没有 id -->
    values (#{name}, #{categoryId}, ... , #{updateUser})
</insert>
```

- **不写 id 列**:写了就是显式指定主键,会绕过自增分配;
- **`useGeneratedKeys="true" keyProperty="id"` 原理**:
  `PrepareStatement(sql, RETURN_GENERATED_KEYS)` → INSERT 成功 → `getGeneratedKeys()` 读到自增值 → MyBatis 按 `keyProperty` **反射写进入参对象**(`dish.setId(70)`)。变的是你手里的 Java 对象,不是数据库返回值;
- **禁止**"插完再 `selectMaxId()`":并发下会拿到别人插入的 id,是竞态;要拿就用回填(或同连接的 `SELECT LAST_INSERT_ID()`);
- **批量插入**(`DishFlavorMapper.xml`)一般不需要回填子表主键,`useGeneratedKeys` 在那里是多余的。

## A6 Service 层:为什么必须把 id 拿回来

```java
dishMapper.insert(dish);
Long dishId = dish.getId();                     // 回填后的 id
flavors.forEach(f -> f.setDishId(dishId));      // 子表补外键
dishFlavorMapper.insertBatch(flavors);
```

漏掉回填时的真实事故链:
```
getId() == null → dish_id 全为 null → `dish_id bigint NOT NULL` 报 1048
→ @Transactional 回滚 → 菜品和口味一起没插进去(前端看到 500)
```

**自增跳号**:事务回滚**不会退还**已分配的 id;并发批量插入会预分配;显式插入指定 id 也会推进计数器。
自检 SQL(判断是否健康):
```sql
SELECT AUTO_INCREMENT FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='sky_take_out' AND TABLE_NAME='dish';
SELECT MIN(id), MAX(id), COUNT(*) FROM dish;
-- 健康状态:AUTO_INCREMENT = MAX(id) + 1;跳号不是 bug,但说明【不能依赖 id 连续】
```

## A7 接口层:id 在协议里的位置

| 场景 | 设计 | 本项目实例 |
|---|---|---|
| 新增菜品 | 返回 `Result.success()`,**不回 id** | 前端保存后跳回列表页,列表会重新请求分页接口拿 id |
| 保存后要立刻跳详情 | 可改成 `Result.success(dish.getId())` | 取舍点:前端拿到 id 要干什么 |
| 查详情 | 路径参数 `GET /admin/dish/{id}` | `@PathVariable Long id` |
| 批量删除 | query 参数 `?ids=1,2,3` | `@RequestParam List<Long> ids` |
| 起售停售 | 路径 + query 混用 | `@PostMapping("/status/{status}")` + `?id=7` |
| 前端区分新增/修改 | 路由 query:`/dish/add` vs `/dish/add?id=7` | `this.$route.query.id ? 'edit' : 'add'` |

## A8 全链路 10 步(新增菜品)

```
① 前端表单无 id(delete params.id)
② POST /api/dish,JSON 里无 id
③ nginx /api/ → 8080 /admin/ → 拦截器校验 token → BaseContext.setCurrentId
④ @RequestBody → DishDTO{id=null, flavors=[...]}
⑤ BeanUtils.copyProperties → Dish{id=null} → setId(null) 兜底
⑥ insert SQL 不含 id 列 → MySQL 分配 id 并写入聚簇索引
⑦ getGeneratedKeys → MyBatis 反射回填 dish.setId(70)
⑧ dishId=70 → flavors.forEach(setDishId(70))
⑨ 一条 <foreach> 批量 INSERT 子表 → 事务提交
⑩ Result.success() → 前端提示成功 → 列表页用 id 渲染每一行
```

## A9 错误清单

| 现象 | 原因 | 修法 |
|---|---|---|
| `Column 'dish_id' cannot be null`(1048) | 主键没回填 | 加 `useGeneratedKeys` + `keyProperty="id"` |
| `getId()` 一直 null | 同上 / `keyProperty` 名字写错 | 加日志 `log.info("id={}", dish.getId())` 验证 |
| `Duplicate entry 'xx' for key 'PRIMARY'` | DTO 的 id 被一起拷进来 | `dish.setId(null)` 兜底 |
| `Duplicate entry 'xx' for key 'idx_dish_name'` | 业务唯一键冲突(菜名重复) | 业务提示"已存在",别指望主键报错 |
| 自增跳号 | 回滚 / 并发预分配 | 正常现象,不要依赖 id 连续 |
| 主表和子表 id 对不上 | 先插子表后插主表,或用了 selectMax | 顺序不能变;用回填 |
| 前端 id 末位变成 0 | JS Number 精度(2^53-1) | Long 序列化为 String(`JacksonObjectMapper` 里加 `ToStringSerializer`) |

## A10 面试三问

1. **MyBatis 怎么拿到自增主键?** → `RETURN_GENERATED_KEYS` 打开 Statement,INSERT 后 `getGeneratedKeys()` 读回,再按 `keyProperty` 反射写入入参对象。
2. **为什么 InnoDB 推荐自增主键而不是 UUID?** → 主键即聚簇索引、二级索引存主键值;自增短而有序,插入集中在 B+ 树最右端避免页分裂;UUID 长且随机,导致页分裂、写放大、索引膨胀。
3. **为什么不能 `SELECT MAX(id)` 拿刚插入的 id?** → 并发下会拿到别人的行,是竞态;应使用回填或同连接的 `LAST_INSERT_ID()`。

---

# 附录 B:报错阅读法(三步 + 关键词表 + 五问)

> 目标:看到红色报错不慌,能自己定位到"哪一层、哪一句、缺什么"。

## B1 三步读法

**第 1 步 · 找结果标记**
- Maven:结尾 `BUILD FAILURE` / `BUILD SUCCESS`
- IDEA 测试与运行:`Process finished with exit code 0`(成功)/ `exit code 1`(有异常)
- Spring 启动:`Started XxxApplication in x seconds` 才算成功

**第 2 步 · 找"根因句"**
- Spring/Java:从**下往上**找 `Caused by:`,**最下面那一个**才是根因
- Maven:`[ERROR]` 里"描述具体缺什么/哪里不匹配"的那句是根因;`-> [Help 1]`、`Failed to execute goal` 只是外壳和链接,**跳过它**

**第 3 步 · 回头看 WARNING**
- **WARNING 常常出现在 ERROR 之前,它就是原因的前兆**(本项目实例:`The POM for com.sky:sky-common:jar is missing` 就出现在 `Could not resolve dependencies` 之前)

## B2 谁在报错(按"开头特征"判断层)

| 报错特征 | 出问题的层 | 往哪查 |
|---|---|---|
| `BUILD FAILURE`、`Could not resolve dependencies`、`Could not find artifact` | Maven 构建 | 依赖坐标、本地仓库(.m2)、是否漏了 `-am`/父项目构建 |
| `Cannot resolve symbol`、`找不到符号`、类名飘红 | 编译期 | import 是否缺、拼写、注解(如 `@Slf4j`) |
| `Failed to load ApplicationContext` | Spring 启动 | **往下找 Caused by**(真正原因在下面) |
| `Could not resolve placeholder 'xxx'` | yml 配置 | 占位符键名是否一字不差(连空格都不能多) |
| `ERROR 1048 Column 'x' cannot be null` | MySQL | 必填字段/外键为 null,谁负责赋值? |
| `ERROR 1064 You have an error in your SQL syntax` | MySQL / MyBatis | 看控制台打印的真实 SQL(常见:空集合 foreach) |
| `ERROR 1062 Duplicate entry` | MySQL | 唯一约束冲突(主键 / `idx_xxx`) |
| `Invalid bound statement (not found)` | MyBatis | 接口方法名 vs XML 的 `id`、`namespace` |
| `Parameter 'xxx' not found` | MyBatis | `@Param` / `collection` 名字对不上 |
| `ERR Client sent AUTH, but no password is set`、`NOAUTH` | Redis | Redis 有没有 requirepass ↔ 配置里的密码 |
| `Connection refused` / `Unable to connect` | 网络 / 服务 | 服务没启动(6379 Redis、3306 MySQL、8080 后端) |
| `NullPointerException` | 代码 | 那一行有变量是 null,顺着它的来源查 |
| `404`(浏览器 / nginx 日志) | 路由 | 类注解 + 方法注解拼出的路径 vs 前端请求路径 |

## B3 关键词速查(根因句 → 含义 → 动作)

| 根因句里的关键词 | 含义 | 立刻做什么 |
|---|---|---|
| `no password is set` | Redis 没设密码,客户端却发了 AUTH | 用配置文件启动 Redis,或把配置里密码留空 |
| `no live upstreams` / `10061` | nginx 连不上后端 | 看 8080 是否在监听(后端是否启动) |
| `Could not find artifact com.sky:sky-xxx` | 兄弟模块没进本地仓库 | 对**父项目**构建,或加 `-am` |
| `Table 'xxx' doesn't exist` | 表名写错或没建表 | 对数据库设计文档核对表名 |
| `Unknown column 'xxx'` | 列名写错 | 对 `SHOW CREATE TABLE` 核对 |
| `Data too long for column` | 值超出列长度 | 看列定义(如 `name varchar(32)`) |
| `You have an error in your SQL syntax ... near ''` | SQL 被拼残了 | 多半是 `<foreach>` 空集合 / 条件全不成立 |

## B4 通用五问(任何报错都能问)

1. **谁在报错?** 哪一层(编译 / Maven / Spring / MyBatis / MySQL / Redis / 浏览器 404)
2. **关键词是什么?** 拿根因句去对 B3 表
3. **根因句是哪句?** 跳过外壳(Help 链接、Failed to execute goal、最外层的包装异常)
4. **缺的那个东西本来该从哪来?** jar→本地仓库;参数→请求;数据→数据库;配置→yml。**顺着"来源"查,一查一个准**
5. **我最近改了什么?** 80% 的报错来自最后一次改动(改前能跑、改后不能)

## B5 本项目实例:一次真实的 Maven 报错

```
[WARNING] The POM for com.sky:sky-common:jar:1.0-SNAPSHOT is missing      ← ③ 警告就是原因前兆
[ERROR] Failed to execute goal on project sky-server: Could not resolve dependencies   ← ① 外壳
[ERROR] dependency: com.sky:sky-common:jar:1.0-SNAPSHOT (compile)
[ERROR]    Could not find artifact com.sky:sky-common:jar:1.0-SNAPSHOT     ← ② 根因句
[ERROR] -> [Help 1]                                                        ← 跳过(只是链接)
```
**翻译**:父项目是聚合工程(3 个模块),`sky-server` 依赖 `sky-common` / `sky-pojo`;
但这两个模块**从没被 install 到本地仓库**(`C:\Users\Lenovo\.m2\repository\com\sky` 不存在);
而这次 Maven 只对 **sky-server 一个模块**执行 compile(reactor 里没有兄弟模块)→ 找不到 → 失败。

**机制一句话**:单独构建子模块的前提是"兄弟模块已经在本地仓库里";整体构建才不需要。

**修法**:
1. **推荐**:右侧 Maven 面板 → 展开**根项目 `sky-take-out`**(最上层那个)→ Lifecycle → 双击 `compile`(或 `install`)。**不要对着子模块点**
2. 根项目执行一次 `install`,把兄弟模块装进 `.m2`,以后单独编译子模块也不会再报这个错
3. 另:这个报错**不影响在 IDEA 里点绿三角运行项目**(运行用的是 IDEA 自己的编译器 + 模块 classpath)

## B6 三个必须养成的习惯

1. **先看最后一行**(结果标记),再看 `Caused by` 链的**最后一句**(根因),最后**往上翻 WARNING**
2. **不要读全篇** —— 报错里 90% 的行是调用栈噪音
3. **把根因句原样复制去搜索**(或直接问我),比自己猜快 10 倍
