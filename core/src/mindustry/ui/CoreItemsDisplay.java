package mindustry.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectSet;
import arc.util.Time;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

import static mindustry.Vars.*;

public class CoreItemsDisplay extends Table {
    private final ObjectSet<Item> usedItems = new ObjectSet<>();
    private CoreBuild core;

    // --- 新增的成員變數 ---
    /** 儲存上一秒的各個物品數量，用於計算差異 */
    private final ObjectIntMap<Item> lastAmounts = new ObjectIntMap<>();
    /** 儲存每秒的物品數量變化 (delta) */
    private final ObjectIntMap<Item> deltaAmounts = new ObjectIntMap<>();
    /** 計時器，每 60 幀 (1秒) 更新一次 delta */
    private float timer;

    private int sec = 2;

    // --- 結束 ---

    public CoreItemsDisplay() {
        rebuild();
    }

    public void resetUsed() {
        usedItems.clear();
        // --- 新增：重置時清空追蹤資料 ---
        lastAmounts.clear();
        deltaAmounts.clear();
        // --- 結束 ---
        background(null);
    }

    /**
     * 每秒更新一次資源變化量
     */
    private void updateDeltas() {
        if (core == null) return;

        // 遍歷所有正在顯示的物品
        for (Item item : usedItems) {
            int currentAmount = core.items.get(item);
            // 如果 lastAmounts 中有記錄，則計算差值
            if (lastAmounts.containsKey(item)) {
                int lastAmount = lastAmounts.get(item);
                deltaAmounts.put(item, currentAmount - lastAmount);
            }
            // 更新上一秒的數量為當前數量，供下一秒計算使用
            lastAmounts.put(item, currentAmount);
        }
    }

    void rebuild() {
        clear();
        if (usedItems.size > 0) {
            background(Styles.black6);
            margin(4);
        }

        update(() -> {
            core = Vars.player.team().core();

            if (content.items().contains(item -> core != null && core.items.get(item) > 0 && usedItems.add(item))) {
                rebuild();
            }

            // --- 新增的計時器和更新邏輯 ---
            if (core == null) {
                // 如果沒有核心，清除所有追蹤資料
                lastAmounts.clear();
                deltaAmounts.clear();
            } else {
                // 計時器累加
                timer += Time.delta;
                // 每 60 幀 (約 1 秒) 觸發一次
                if (timer >= 60f * sec) {
                    updateDeltas();
                    timer = 0f; // 重置計時器
                }
            }
            // --- 結束 ---
        });

        int i = 0;

        for (Item item : content.items()) {
            if (usedItems.contains(item)) {
                image(item.uiIcon).size(iconSmall).padRight(3).tooltip(t -> t.background(Styles.black6).margin(4f).add(item.localizedName).style(Styles.outlineLabel));

                // --- 修改 Label 的創建邏輯 ---
                // 為了在 lambda 中使用 item，需要一個 final 的副本
                final Item currentItem = item; 
                label(() -> {
                    if (core == null) return "0";

                    // 從 deltaAmounts 中獲取變化量，如果沒有則為 0
                    int delta = deltaAmounts.get(currentItem, 0)/sec;
                    String prefix = "";

                    if (delta > 0) {
                        // 資源增加：綠色上箭頭
                        // Pal.heal 是遊戲內建的綠色，.toString() 會轉換成 Mindustry 格式的顏色代碼
                        prefix = " ([lime]+" + UI.formatAmount(delta) + "[])";
                    } else if (delta < 0) {
                        // 資源減少：紅色下箭頭
                        // Pal.remove 是遊戲內建的紅色
                        prefix = " ([scarlet]" + UI.formatAmount(delta) + "[])";
                    }
                    
                    // 組合字串：箭頭 + 格式化後的數量
                    return UI.formatAmount(core.items.get(currentItem)) + prefix;
                }).padRight(3).minWidth(52f).left().tooltip(t -> t.background(Styles.black6).margin(4f).label(() -> {
                    if (core == null) return "0";
                    
                    // 同樣為 tooltip 添加變化量顯示
                    int delta = deltaAmounts.get(currentItem, 0)/sec;
                    String deltaText = "";
                    if(delta > 0){
                        deltaText = " ([lime]+" + UI.formatAmount(delta) + "/s[])";
                    }else if(delta < 0){
                        deltaText = " ([scarlet]" + UI.formatAmount(delta) + "/s[])";
                    }

                    return core.items.get(currentItem) + deltaText;
                }).style(Styles.outlineLabel));
                // --- 結束 ---

                if (++i % 4 == 0) {
                    row();
                }
            }
        }
    }
}