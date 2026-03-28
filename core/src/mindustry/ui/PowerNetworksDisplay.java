package mindustry.ui;

import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Interval;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.blocks.power.PowerGraph;

public class PowerNetworksDisplay extends Table {
    // 用於控制掃描頻率的計時器
    private final Interval timer = new Interval();
    // 快取當前的電網清單，用來比對電網是否發生分裂或合併
    private final Seq<PowerGraph> cachedGraphs = new Seq<>();

    public PowerNetworksDisplay() {
        // 設定類似 CoreItemsDisplay 的半透明黑底
        super(Styles.black6);
        margin(6f);

        // 註冊每幀更新邏輯
        update(() -> {
            // 每 60 幀 (約 1 秒) 掃描一次全域電網，避免嚴重吃 CPU
            if (timer.get(60f)) {
                refreshGraphs();
            }
        });
    }

    /**
     * 掃描並過濾出當前玩家隊伍所有獨立的電網分區
     */
    private void refreshGraphs() {
        if (!Vars.state.isPlaying() || Vars.player == null || Vars.player.team() == null) return;

        ObjectSet<PowerGraph> currentGraphs = new ObjectSet<>();

        // 遍歷當前隊伍所有建築，收集唯一的 PowerGraph
        for (Building b : Vars.player.team().data().buildings) {
            if (b.block.hasPower && b.power != null && b.power.graph != null) {
                if (b.power.graph.getTotalBatteryCapacity() > 1f && b.power.graph.getLastPowerProduced() > 1f && b.power.graph.getLastPowerNeeded() > 1f) { // 只顯示有電池且有電量生產/使用的分區
                    currentGraphs.add(b.power.graph);
                }
            }
        }

        // 檢查電網數量或對象是否發生變化 (例如玩家蓋了新發電機建立新分區，或用節點連線了兩個分區)
        boolean changed = cachedGraphs.size != currentGraphs.size;
        if (!changed) {
            for (PowerGraph g : currentGraphs) {
                if (!cachedGraphs.contains(g)) {
                    changed = true;
                    break;
                }
            }
        }

        // 如果電網分區有變動，重新建立 UI
        if (changed) {
            cachedGraphs.clear();
            cachedGraphs.addAll(currentGraphs.toSeq());
            // 根據建築總數量由大到小排序，確保規模最大的電網排在最前面，UI 不會亂跳
            cachedGraphs.sort(g -> -g.all.size);
            
            rebuildUI();
        }
    }

    /**
     * 重新建構 UI 佈局 (僅在電網分區改變時呼叫)
     */
    private void rebuildUI() {
        clearChildren();

        if (cachedGraphs.isEmpty()) {
            return; // 沒有電網時不顯示任何東西
        }

        for (int i = 0; i < cachedGraphs.size; i++) {
            PowerGraph graph = cachedGraphs.get(i);
            // 建立 Bar 組件 (文字供應器, 顏色供應器, 進度比例供應器)
            Bar powerBar = new Bar(
                () -> {
                    // 電量增減 (每秒)。getPowerBalance() 是單幀的，所以乘 60
                    int netPower = (int) (graph.getPowerBalance() * 60f); 
                    int amount = (int) graph.getLastPowerStored();
                    int maxBat = (int) graph.getLastCapacity();
                    // 為了美觀，正數加上 "+" 號
                    String sign = netPower > 0 ? "+" : ""; 
                    
                    // 格式: "%d (%d)" -> 電量增減 (總電池上限)
                    return sign + mindustry.core.UI.formatAmount(netPower) + " (" + mindustry.core.UI.formatAmount(maxBat) + ")";
                },
                () -> Pal.powerBar, // 使用 Mindustry 標準的電力黃色/橘色
                () -> {
                    // 背景填充比例 [電池電量/總電池上限]
                    float amount = graph.getLastPowerStored();
                    float max = graph.getLastCapacity();
                    if (max <= 0.001f) return 0f; // 避免除以 0
                    return amount / max;
                }
            );

            // 加入 Table，設定大小並留白
            add(powerBar).size(200f, 20f).pad(2f);

            // 如果分區太多，每 3 個分區換一行 (可依需求調整)
            if ((i+1) % 3 == 0) {
                row();
            }
        }
    }
}