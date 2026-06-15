# 仓库库存管理系统 - 开发与测试指南

## 一、实验环境配置

### 1.1 硬件环境
- **设备**: 华为平板 TD-LTE 无线数据终端
- **型号**: KOB2KZ-AL00
- **系统**: HarmonyOS 3.0.0.114
- **处理器**: HUAWEI Kirin 710A

### 1.2 软件开发环境
- **IDE**: DevEco Studio 3.1.1 Release
- **SDK**: API 6 (HarmonyOS 2.x 兼容)
- **项目模型**: FA (Feature Ability) 模型
- **开发语言**: Java
- **构建工具**: Gradle 7.3

### 1.3 环境配置步骤

#### 步骤1: 安装 DevEco Studio 3.1.1
1. 从华为开发者官网下载 DevEco Studio 3.1.1 Release 版本
2. 安装时选择完整安装（包含 SDK）
3. 安装路径建议不要有中文或空格

#### 步骤2: 配置 SDK
1. 打开 DevEco Studio → Configure → SDK Manager
2. 确认 API Version 6 已安装（在 Platforms 标签页）
3. 确认 Tools 标签页中 Build Tools 已安装
4. SDK 默认路径如: `D:\SDKSetup`（可在 `local.properties` 中修改）

#### 步骤3: 配置 Node.js
1. Node.js 路径需配置在 `local.properties` 中: `nodejs.dir=C:/nodejs`
2. 如果尚未安装 Node.js，请安装 Node.js 14.x 版本

#### 步骤4: 打开项目
1. 启动 DevEco Studio
2. File → Open → 选择 `D:\LocalNWS6` 目录
3. 等待 Gradle 同步完成（首次可能需要下载依赖，请保持网络连接）

## 二、项目结构说明

```
LocalNWS6/
├── entry/                          # 主模块
│   ├── libs/
│   │   └── zxing-core-3.4.1.jar   # ZXing 条码解码库
│   └── src/main/
│       ├── config.json             # 应用配置 (权限、Ability声明)
│       ├── java/com/sunyicheng/myapplication/
│       │   ├── MyApplication.java   # 应用入口 (数据库初始化)
│       │   ├── MainAbility.java     # 主Ability (路由配置)
│       │   ├── slice/               # 12个页面(AbilitySlice)
│       │   │   ├── MainSlice.java           # 首页功能菜单
│       │   │   ├── ProductListSlice.java     # 商品列表
│       │   │   ├── ProductEditSlice.java     # 商品新增/编辑
│       │   │   ├── StockInSlice.java         # 入库操作
│       │   │   ├── StockOutSlice.java        # 出库操作
│       │   │   ├── BarcodeScanSlice.java     # 摄像头扫码
│       │   │   ├── SignatureSlice.java       # 手写签名
│       │   │   ├── RecordListSlice.java      # 出入库记录
│       │   │   ├── RecordDetailSlice.java    # 记录详情
│       │   │   ├── FixedAssetListSlice.java  # 固定资产列表
│       │   │   ├── FixedAssetEditSlice.java  # 固定资产编辑
│       │   │   └── StatisticsSlice.java      # 统计报表
│       │   ├── db/
│       │   │   └── DatabaseManager.java      # 数据库管理 (SQLite)
│       │   ├── model/
│       │   │   ├── Product.java              # 商品实体
│       │   │   ├── StockRecord.java          # 出入库记录实体
│       │   │   └── FixedAsset.java           # 固定资产实体
│       │   ├── component/
│       │   │   └── SignatureView.java        # 手写签名组件
│       │   └── util/
│       │       ├── BarcodeDecoder.java       # ZXing条码解码
│       │       └── DateUtils.java            # 日期工具
│       └── resources/base/
│           ├── element/string.json           # 字符串资源
│           └── layout/                       # 9个XML布局文件
```

## 三、签名配置 (部署到平板必需的步骤)

### 3.1 在 DevEco Studio 中配置签名

1. **登录华为开发者账号**
   - DevEco Studio → File → Settings → Huawei Developers → 登录你的华为账号
   - 如果没有账号，需要在 developer.huawei.com 注册

2. **创建/关联项目**
   - 登录后，在 IDE 中关联你的华为开发者项目
   - 或者创建新项目（应用包名: com.sunyicheng.myapplication）

3. **生成调试证书**
   - File → Project Structure → Signing Configs
   - 点击 "+" 添加签名配置
   - 选择 "Generate Key and CSR" 自动生成调试证书
   - 或者使用已有的 .p12 证书文件

4. **配置签名**
   - 在 Project Structure → Signing Configs 中:
     - Store File: 选择或生成 .p12 文件
     - Store Password: 输入密码
     - Key Alias: 输入别名
     - Sign Alg: 选择 SHA256withECDSA

5. **在 config.json 中确认包名**
   - 包名 `com.sunyicheng.myapplication` 必须与签名证书匹配

### 3.2 在平板上开启调试模式

1. **开启开发者选项**
   - 平板设置 → 关于平板电脑 → 连续点击"版本号"7次
   - 返回设置 → 系统和更新 → 开发人员选项

2. **开启USB调试**
   - 开发人员选项 → 开启 "USB调试"
   - 开启 "仅充电模式下允许ADB调试"

3. **连接平板到电脑**
   - 使用USB数据线连接平板和电脑
   - 平板会弹出"是否允许USB调试"对话框 → 点击"确定"
   - 在 DevEco Studio 中应该能看到设备

## 四、编译构建

### 4.1 在 DevEco Studio 中构建

1. **同步项目**
   - File → Sync Project with Gradle Files
   - 等待同步完成，确保无错误

2. **清理构建**
   - Build → Clean Project

3. **编译 HAP**
   - Build → Build HAP(s)
   - 编译成功后在 `entry/build/outputs/hap/` 下生成 .hap 文件

4. **直接运行到平板**
   - 在顶部工具栏选择你的设备 (KOB2KZ-AL00)
   - 点击绿色运行按钮 ▶
   - 或使用 Run → Run 'entry'

### 4.2 命令行构建

```bash
# Windows 下使用 Git Bash 或 CMD
cd D:\LocalNWS6

# 清理
./gradlew clean

# 编译 debug HAP
./gradlew assembleDebug

# 编译 release HAP
./gradlew assembleRelease
```

### 4.3 编译问题排查

如果遇到编译错误:

| 错误类型 | 可能原因 | 解决方法 |
|---------|---------|---------|
| 找不到 ZXing 类 | jar未正确引用 | 确认 `entry/libs/zxing-core-3.4.1.jar` 存在 |
| SDK 找不到 | local.properties 路径错误 | 修改 SDK 路径为你的实际路径 |
| 签名错误 | 未配置签名 | 参考第三章配置签名 |
| ResourceTable 引用错误 | 资源文件缺失 | 确认所有 layout XML 文件存在 |
| ohos.media.camera 相关错误 | API版本问题 | 检查 compileSdkVersion 是否为 6 |

## 五、功能测试

### 5.1 应用启动测试
1. 安装到平板后，在桌面找到"仓库管理"图标
2. 点击图标启动应用
3. 首次启动会自动创建数据库
4. 应显示主页面，包含6个功能按钮和底部统计栏

### 5.2 商品管理测试
1. 点击"商品管理" → 进入商品列表页
2. 点击"新增" → 进入新增页面
3. 填写商品信息:
   - 商品名称: A4打印纸
   - 条码: 手动输入或点击扫码
   - 分类: 办公用品
   - 单位: 包
   - 库存数量: 100
4. 点击"保存" → 返回列表页，应看到新增的商品
5. 搜索框输入"A4"测试搜索功能

### 5.3 条码扫描测试

> **v1.1 修改说明**: 因 HarmonyOS 3.0 设备上 `CameraKit` + `SurfaceProvider` 预览管线
> `getSurfaceOps()` 始终返回空，已将扫码方案改为调用**系统相机拍照 + ZXing 解码**，
> 跨版本兼容性更好。

1. 在入库/出库/商品编辑页点击"扫码" → 进入扫码页
2. 点击"拍照扫描条码" → 系统相机自动打开
3. 将商品条形码对准，用系统相机拍照
4. 拍照后自动返回扫码页，系统自动调用 ZXing 解码
5. 识别成功自动返回前页并填入条码
6. 如识别失败，可点击"手动输入条码"直接输入条形码数字

### 5.4 入库测试
1. 点击首页"入库操作"
2. 扫描或输入条码 → 点击"查询商品信息"
3. 输入入库数量: 50
4. 输入经办部门: 行政部
5. 输入经办人: 张三
6. 点击"去签字" → 在签名区手写签名
7. 点击"确认签字" → 返回入库页
8. 点击"确认入库"
9. 应提示"入库成功"，库存从100变为150

### 5.5 出库测试
1. 点击首页"出库操作"
2. 扫描条码查询商品
3. 输入出库数量: 3
4. 输入领取部门: 技术部
5. 输入领取人: 李四
6. 签名确认
7. 点击"确认出库"
8. 应提示"出库成功"，库存从150变为147
9. 尝试出库超过库存的数量 → 应提示"库存不足"

### 5.6 记录查询测试
1. 点击首页"记录查询"
2. 应看到刚才的入库和出库两条记录
3. 点击"入库"筛选 → 只显示入库记录
4. 点击"出库"筛选 → 只显示出库记录
5. 点击某条记录 → 查看详情（含商品信息、人员、签字状态）

### 5.7 固定资产测试
1. 点击首页"固定资产"
2. 点击"新增"
3. 填写信息:
   - 资产名称: 铁皮文件柜
   - 分类: 家具
   - 存放位置: 3楼档案室
   - 所属部门: 行政部
   - 数量: 5
   - 单位: 个
   - 购入日期: 2024-01-15
   - 价值: 3000
4. 保存 → 列表显示新资产
5. 点击可编辑，点击删除可移除

### 5.8 统计报表测试
1. 点击首页"统计报表"
2. 查看商品库存统计（种类、总量、低库存预警）
3. 查看分类库存分布
4. 查看出入库统计（次数、总量）
5. 查看固定资产统计（数量、状态分布、总价值）

## 六、常见问题

### Q1: 摄像头无法打开
- 检查是否授予了相机权限（设置 → 应用 → 仓库管理 → 权限）
- 确认 config.json 中已声明 `ohos.permission.CAMERA`

### Q2: 条码扫描不识别
- 确保光线充足，条码清晰
- 将条码对准摄像头，保持稳定
- 尝试调整距离（10-30cm最佳）
- 也可以用"手动输入"功能直接输入条码

### Q3: 签名保存失败
- 检查存储权限是否已授予
- 确认签名区域已手写内容（非空白）

### Q4: 编译时 ResourceTable 报错
- 执行 Build → Clean Project
- 然后 Build → Rebuild Project
- ResourceTable 是自动生成的，rebuild 会重新生成

### Q5: 安装到平板失败
- 检查签名配置是否正确
- 检查USB连接是否正常
- 检查平板是否开启了USB调试
- 尝试更换USB数据线或USB接口

## 七、数据库位置

应用数据存储在平板的私有目录:
```
/data/storage/el2/base/databases/warehouse.db
```
签名图片存储在:
```
/data/storage/el2/base/files/signatures/
```

如需导出数据库进行备份，可以使用 hdc 命令:
```bash
hdc shell
# 进入平板shell后
cp /data/storage/el2/base/databases/warehouse.db /sdcard/
exit
hdc file recv /sdcard/warehouse.db ./
```
