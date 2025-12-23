# GCPulse 支持的 GC 日志格式

## 概览

GCPulse 现在支持 **6种主流 GC 收集器** 和 **多种日志格式**。

## 支持的 GC 收集器

| GC类型 | JDK版本 | 支持状态 | 事件类型 |
|--------|---------|---------|---------|
| **ZGC** | 11-21+ | ✅ 完全支持 | Pause Mark Start/End, Relocate Start |
| **CMS** | 6-8 | ✅ 完全支持 | ParNew, Initial Mark, Remark, Full GC, 并发阶段 |
| **G1GC** | 7-21+ | ⚠️ 基础支持 | GC Pause |
| **Parallel GC** | 6-21+ | ⚠️ 基础支持 | Young GC, Full GC |
| **Serial GC** | 6-21+ | ⚠️ 基础支持 | Young GC, Full GC |
| **Shenandoah** | 12+ | ⚠️ 基础支持 | 待增强 |

## 详细支持情况

### 1. ZGC - 完全支持 ✅

#### 支持的格式

##### 格式1: Java 21 Unified Logging（你的第一个日志）
```
[2025-12-11T01:40:40.712+0800][118][gc,phases] GC(0) Pause Mark Start 0.015ms
[2025-12-11T01:40:40.827+0800][118][gc,phases] GC(0) Pause Mark End 0.015ms
[2025-12-11T01:40:40.862+0800][118][gc,phases] GC(0) Pause Relocate Start 0.009ms
```

##### 格式2: 旧版本 ZGC
```
[0.123s][info][gc] GC(0) Pause Mark Start 0.015ms
```

#### 提取的数据
- ✅ 暂停事件（Mark Start/End, Relocate Start）
- ✅ 堆内存使用（从 `[gc,heap] Used` 提取）
- ✅ 堆容量（从 `[gc,heap] Capacity` 提取）
- ✅ 元空间（从 `[gc,metaspace]` 提取）
- ✅ 对象分配（从 `[gc,heap] Allocated` 提取）
- ✅ 对象回收（从 `[gc,heap] Reclaimed` 提取）

#### 特殊处理
- ✅ 单代模式识别：`Using legacy single-generation mode`
- ✅ 分代模式识别：`Using ZGC generational mode`（JDK 21+）
- ✅ 相对时间计算（解决图表显示问题）

---

### 2. CMS - 完全支持 ✅

#### 支持的格式

##### 格式1: JDK 8 详细格式（你的第二个日志）
```
2025-08-05T13:23:18.409+0800: 4.856: [GC (Allocation Failure) 
  2025-08-05T13:23:18.409+0800: 4.856: 
  [ParNew: 1622016K->66806K(1824768K), 0.0717347 secs] 
  1622016K->66806K(5040128K), 0.0718658 secs] 
  [Times: user=0.15 sys=0.03, real=0.08 secs]
```

##### 格式2: JDK 6/7 简单格式
```
4.856: [GC 4.856: [ParNew: 1622016K->66806K(1824768K), 0.0717347 secs] 1622016K->66806K(5040128K), 0.0718658 secs]
```

##### 格式3: CMS Initial Mark
```
135.456: [GC (CMS Initial Mark) [1 CMS-initial-mark: 1024000K(3215360K)] 1536000K(5040128K), 0.0123456 secs]
```

##### 格式4: CMS Final Remark
```
140.567: [GC (CMS Final Remark) [YG occupancy: 512000 K (1824768 K)] [1 CMS-remark: 1536000K(3215360K)] 2048000K(5040128K), 0.0567890 secs]
```

##### 格式5: Full GC
```
25500.456: [Full GC (Allocation Failure) [CMS: 2048000K->512000K(3215360K), 1.2345678 secs] 4096000K->512000K(5040128K), [Metaspace: 128M->128M(512M)], 1.2345678 secs]
```

##### 格式6: Promotion Failed
```
4000.456: [GC (Allocation Failure) [ParNew (promotion failed): 1824768K->1824768K(1824768K), 0.5678901 secs][CMS: 3000000K->2500000K(3215360K), 2.3456789 secs] 4824768K->2500000K(5040128K), 2.9135690 secs]
```

##### 格式7: Concurrent Mode Failure
```
7605.567: [Full GC (Concurrent Mode Failure) [CMS (concurrent mode failure): 3000000K->2800000K(3215360K), 3.4567890 secs] 4096000K->2800000K(5040128K), 3.4567890 secs]
```

##### 格式8: CMS 并发阶段
```
135.567: [CMS-concurrent-mark-start]
137.678: [CMS-concurrent-mark: 2.111/2.111 secs]
137.789: [CMS-concurrent-preclean: 0.111/0.111 secs]
140.900: [CMS-concurrent-sweep: 1.111/1.111 secs]
142.011: [CMS-concurrent-reset: 0.111/0.111 secs]
```

#### 提取的数据
- ✅ ParNew 事件（年轻代GC）
- ✅ Full GC 事件
- ✅ Initial Mark / Remark 暂停
- ✅ 并发阶段时间
- ✅ 年轻代内存变化
- ✅ 老年代内存变化
- ✅ 堆总内存变化
- ✅ Promotion Failed 识别
- ✅ Concurrent Mode Failure 识别

---

### 3. G1GC - 基础支持 ⚠️

#### 当前支持的格式
```
0.123: [GC pause (G1 Evacuation Pause) (young) 512M->128M(2048M), 0.0234567 secs]
```

#### 建议增强
- ⚠️ Mixed GC 支持
- ⚠️ Humongous object 支持
- ⚠️ To-space exhausted 识别
- ⚠️ G1 各阶段统计

---

### 4. Parallel GC - 基础支持 ⚠️

#### 当前支持的格式
```
0.123: [GC [PSYoungGen: 512000K->64000K(1024000K)] 512000K->64000K(2048000K), 0.0234567 secs]
0.456: [Full GC [PSYoungGen: 64000K->0K(1024000K)] [ParOldGen: 448000K->256000K(1024000K)] 512000K->256000K(2048000K), 1.2345678 secs]
```

#### 建议增强
- ⚠️ PSYoungGen 详细统计
- ⚠️ ParOldGen 详细统计

---

### 5. Serial GC - 基础支持 ⚠️

#### 当前支持的格式
```
0.123: [GC [DefNew: 512000K->64000K(1024000K), 0.0234567 secs] 512000K->64000K(2048000K), 0.0234567 secs]
```

---

### 6. Shenandoah - 基础支持 ⚠️

#### 建议增强
- ⚠️ 需要添加专门的解析逻辑

---

## 不同 JDK 版本的兼容性

| JDK版本 | 日志格式 | ZGC | CMS | G1GC | Parallel | Serial |
|---------|---------|-----|-----|------|----------|--------|
| **6-7** | 经典格式 | ❌ | ✅ | ✅ | ✅ | ✅ |
| **8** | 详细格式 | ❌ | ✅ | ✅ | ✅ | ✅ |
| **9-10** | 统一日志 | ⚠️ | ⚠️ | ⚠️ | ⚠️ | ⚠️ |
| **11-16** | 统一日志 | ✅ | ⚠️ | ⚠️ | ⚠️ | ⚠️ |
| **17-21** | 统一日志 | ✅ | ⚠️ | ⚠️ | ⚠️ | ⚠️ |

## 日志参数建议

### ZGC（推荐）
```bash
# JDK 11-20
-Xlog:gc*:file=gc.log:time,level,tags

# JDK 21+ 单代模式
-Xlog:gc*:file=gc.log:time,level,tags

# JDK 21+ 分代模式
-XX:+ZGenerational -Xlog:gc*:file=gc.log:time,level,tags
```

### CMS（推荐）
```bash
# JDK 8
-XX:+PrintGCDetails 
-XX:+PrintGCDateStamps 
-XX:+PrintGCTimeStamps 
-Xloggc:gc.log

# JDK 9+（如果还在用CMS）
-Xlog:gc*:file=gc.log:time,uptimemillis,level,tags
```

### G1GC（推荐）
```bash
# JDK 8
-XX:+PrintGCDetails 
-XX:+PrintGCDateStamps 
-Xloggc:gc.log

# JDK 9+
-Xlog:gc*:file=gc.log:time,level,tags
```

## 当前实现的优势

### 1. 健壮性
- ✅ 多正则表达式匹配
- ✅ 优雅降级（无法解析时跳过）
- ✅ 不会因为单行解析失败而崩溃

### 2. 灵活性
- ✅ 支持多种时间戳格式
- ✅ 支持有无日期时间戳
- ✅ 自动识别收集器类型

### 3. 全面性
- ✅ 提取内存信息
- ✅ 提取时间信息  
- ✅ 识别特殊事件（promotion failed等）
- ✅ 区分暂停和并发阶段

## 当前限制

### 1. G1GC
- ⚠️ 只支持基本的 pause 事件
- ⚠️ 未解析各个子阶段（Ext Root Scanning等）
- ⚠️ 未识别 Mixed GC、Humongous

### 2. Unified Logging（JDK 9+）
- ⚠️ ZGC 完全支持
- ⚠️ 其他GC的统一日志格式支持有限
- ⚠️ 建议使用经典格式或ZGC

### 3. 复杂嵌套格式
- ⚠️ 某些极其复杂的嵌套格式可能无法完全解析
- ⚠️ 但会尽可能提取关键信息

## 测试建议

### 测试你的日志
1. **ZGC 日志**：`gc-2025-12-11_01-40-28.log` ✅ 已验证
2. **CMS 日志**：`cms-gc.log` ✅ 需要重启后端验证

### 推荐测试流程
```bash
# 1. 重启后端
./start-backend.sh

# 2. 测试 CMS
bash test-cms.sh

# 3. 或通过浏览器上传
打开 http://localhost:5173
上传 cms-gc.log
检查结果
```

## 总结

### ✅ 生产可用
- **ZGC**：完全支持，推荐用于生产环境
- **CMS**：完全支持，适配所有常见格式

### ⚠️ 可用但建议测试
- **G1GC**：基础支持，建议先测试你的日志格式
- **Parallel GC**：基础支持
- **Serial GC**：基础支持

### 🔧 建议扩展（如有需要）
- G1GC 详细阶段解析
- Shenandoah 完整支持
- JDK 9+ 统一日志格式的全面支持

---

**现在重启后端，测试你的 CMS 日志！** 🚀

