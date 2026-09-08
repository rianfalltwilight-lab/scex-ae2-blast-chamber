# ME 爆炸配方机 0.1.0

**[直接下载运行 JAR](https://github.com/rianfalltwilight-lab/scex-ae2-blast-chamber/releases/download/v0.1.0/ae2-blast-chamber-1.21.1-neoforge-0.1.0.jar)**

SCEX 原创非官方 AE2 附属，开发预发布。

Minecraft 1.21.1 / NeoForge 21.1.248 / Java 21 / AE2 19.2.17 的独立附属。把火药或 TNT 放进机器，在容器内完成爆炸合成，不破坏周围方块。两端均需安装主 JAR，JEI 可选。

## 使用

1. 合成并放置「ME 爆炸配方机」，右键打开界面。左侧九格放原料，中间燃料格放火药或 TNT，右侧四格取产物。
2. 默认每批消耗 1 爆炸当量、加工 60 tick（正常 20 TPS 时为 3 秒）。火药提供 1 当量，TNT 提供 5 当量，未用完的当量保存在机器中。无需额外能源。
3. 界面可开启/关闭；收到红石强度大于零的信号时暂停，保留进度。关机、空闲和堵料状态使用静态关闭材质，真正工作时正面呈现紫色环流、琥珀色脉冲。动画四帧、每帧 4 tick，启用插值。
4. AE 样板供应器对准侧面输入原料，顶部补充燃料，底部用输入总线或漏斗抽出产物。把已知爆炸配方编码为处理样板即可参与 AE 自动合成；本机不会代替样板终端自动编写样板。

| 面 | 自动化用途 |
|---|---|
| 上面 | 只插入火药/TNT |
| 四周 | 只插入加工原料 |
| 下面 | 只抽出四个产出格 |

机器配方：`OIO / FCF / OTO`，O 为黑曜石，I 为铁锭，F 为福鲁伊克斯水晶，C 为工程处理器，T 为 TNT。

材料在开工时预留，燃料在开工时扣除；输出空间不足时不会继续开新批次。已经开工的批次在完成后等待输出空间，不丢失产物。进度、待产出物、预留原料和剩余当量随世界保存。拆除加工中的机器返还预留原料，不提前发放成品；已消耗的燃料与剩余当量不返还。

## 自动发现与扩展

自动发现当前 RecipeManager 中所有 `ae2:transform` 且 `circumstance.type=explosion` 的配方，不限定配方命名空间。因此 AE2 及使用同一配方类型的扩展、数据包配方都会被识别。水中转化不纳入爆炸配方。量子缠绕奇点通过 AE2 原有 assemble 创建，保留每批独立频率及成对结果。

发现于配方目录首次使用时进行；数据包 `/reload` 会失效缓存并重新发现。机器没有匹配结果时缓存结果，库存改变或重载后才重新检索；每台每 10 tick 最多检查 8 条，不扫描地上物品或世界方块。JEI 提供「密闭爆炸合成」分类和机器配方用途。

“自动发现”基于真实配方类型，不按显示名称猜测，也不运行时联网抓取。其他模组使用独立配方类型时可注册 `RecipeCatalog.registerAdapter(id, adapter)`。硬编码在事件里的合成没有通用可枚举配方表，需要明确适配或数据包；本版已适配下述 DG 行为。

**微小混沌碎片**：同时加载 DG Modules 和 Draconic Evolution 时启用：

`draconicevolution:dragon_heart ×1 + minecraft:nether_star ×1 + minecraft:diamond ×1 → draconicevolution:small_chaos_frag ×2`

来源是怀旧服 DG Modules `1.0.7hotfix2` 的 `ChaosFragmentExplosionEvents.onExplosionDetonate`，不是龙之进化的九合一拆分配方。已用真实 DG 爆炸和本机分别执行并核对结果。本兼容 JSON 的 ID 为 `ae2blast:compat/dgmodules/tiny_chaos_fragment`；只有龙之进化而没有 DG 时不添加此玩法。未来 DG 版本若更改该事件，需要同步检查此适配。

数据包可添加 `data/<namespace>/recipe/<name>.json`（1.21.1 的 recipe 为单数）：

```json
{
  "type": "ae2blast:blasting",
  "ingredients": [
    {"ingredient": {"tag": "c:ingots/iron"}, "count": 3},
    {"ingredient": {"item": "minecraft:redstone"}, "count": 2}
  ],
  "result": {"id": "minecraft:diamond", "count": 1},
  "charges": 2,
  "ticks": 80
}
```

这是格式示例，不会默认添加到模组。支持 1–9 组原料、每组 1–64 个、1–64 当量、1–1200 tick；原料可使用标签，重复和重叠标签按实际数量分配。原料槽共九格，产出必须能装入四格。服务器配置位于世界的 `serverconfig/ae2blast-server.toml`，可控制 AE 自动发现、默认当量和时长；自定义配方有自己的参数。

适配器返回 `BlastRecipeView`，包含带数量原料、预览、当量、时长和装配函数；装配函数收到按输入槽排列的本批消耗物副本，不能访问/修改世界。配方视图应视为只读。机器只在通过库存和空间检查后装配，动态组件输出仍须通过最终空间校验。超过支持边界的配方会记入警告，不截断原料静默执行。

## 构建与验证

需要 Java 21 和 PowerShell 7。首次构建先下载精确上游库，脚本复用现有文件并验证 SHA-256；仓库不重新分发依赖 JAR。

```powershell
./scripts/fetch-libraries.ps1
./gradlew.bat --no-daemon build
python tools/validate_resources.py
```

Linux/macOS 使用 `./gradlew`。缓存齐全后可使用 `--offline`。Gradle 8.8、ModDevGradle 2.0.116 与 NeoForge 21.1.248 均固定，编译前校验 DEPENDENCIES.json。下载脚本默认绕过应用代理；受阻时可显式使用 `-UseProxy`，不改全局配置。

可选 DG 验证使用 `-PwithDG=true`，自行按 DEPENDENCIES.json 准备 integration-libs 中的精确文件；这些集成库不随仓库分发。`src/gametest` 是独立测试模组，probe JAR 不得放入玩家 mods。原始图与生成工具位于 art/ 和 tools/。

本版验证和未测边界见 [Release 说明](docs/releases/0.1.0.md)。代码及原始美术采用 [MIT](LICENSE)，来源和 AI 参与见 [NOTICE.md](NOTICE.md)、[AI-GENERATED.md](AI-GENERATED.md)。
