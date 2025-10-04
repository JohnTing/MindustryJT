package mindustry;

import static mindustry.Vars.player;
import static mindustry.Vars.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import arc.Core;
import arc.Events;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.core.NetClient;
import mindustry.core.World;
import mindustry.game.EventType.ConfigEvent;
import mindustry.gen.Building;
import mindustry.world.Tile;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.blocks.power.PowerNode;

public class ClientLogic {
    ClientLogic() {

    }
    void init() {

    }
    private static final Seq<Building> outArray1 = new Seq<>();
    private static final Seq<Building> outArray2 = new Seq<>();


    public static float powerUse(PowerGraph powerGraph ) {
        return (powerGraph.getLastPowerNeeded() + powerGraph.getLastPowerProduced())*60;
    }

    public static boolean hidddenRender() {
        return arc.Core.settings.getBool("hiddenrender") || (arc.Core.settings.getBool("hiddenrenderonbuild") && mindustry.Vars.control.input.isPlacing()); 
    }

/**
     * 檢查移除一個建築後，其所在的電力網絡（PowerGraph）是否會分裂成多個。
     *
     * @param buildingToRemove 準備被移除的建築。
     * @return 如果會分裂，返回 true；否則返回 false。
     */
    public static boolean willPowerGraphSplit(Building buildingToRemove) {
        // 1. 找出所有直接連接的鄰居
        buildingToRemove.getPowerConnections(outArray1);

        // 2. 處理邊界情況：如果鄰居少於2個，不可能分裂
        if (outArray1.size <= 1) {
            return false;
        }

        // 3. 從第一個鄰居開始，執行廣度優先搜索（BFS）
        //    目標是查看是否能訪問到所有其他鄰居
        Building startNode = outArray1.get(0);
        
        Set<Building> visited = new HashSet<>();
        Queue<Building> queue = new LinkedList<>();

        queue.add(startNode);
        visited.add(startNode);

        while (!queue.isEmpty()) {
            Building current = queue.poll();
            current.getPowerConnections(outArray2);
            // 遍歷當前節點的所有連接   
            for (Building next : current.getPowerConnections(outArray2)) {
                // 關鍵：模擬建築被移除，所以不能通過 buildingToRemove
                if (next.equals(buildingToRemove)) {
                    continue;
                }

                // 如果 next 節點尚未被訪問過
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }

        // 4. 檢查是否所有原始鄰居都已經被訪問過
        //    我們從 outArray1.get(1) 開始檢查，因為 startNode (outArray1.get(0)) 肯定在 visited 集合裡
        for (int i = 1; i < outArray1.size; i++) {
            Building neighborToCheck = outArray1.get(i);
            if (!visited.contains(neighborToCheck)) {
                // 只要找到一個鄰居無法從 startNode 到達，就意味著電網會分裂
                return true;
            }
        }

        // 如果所有鄰居都能互相到達，則電網不會分裂
        return false;
    }
}
