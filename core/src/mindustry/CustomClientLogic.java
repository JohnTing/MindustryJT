package mindustry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.IntSeq;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Strings;
import mindustry.core.NetClient;
import mindustry.core.UI;
import mindustry.core.World;
import mindustry.game.EventType.BuildSelectEvent;
import mindustry.game.EventType.ConfigEvent;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.power.NuclearReactor;
import mindustry.world.blocks.power.PowerBlock;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.blocks.power.PowerNode;
import mindustry.world.blocks.power.PowerNode.PowerNodeBuild;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
// mindustry.CustomClientLogic
public class CustomClientLogic {

    public static int hiddenItemTransparency = 50;


    CustomClientLogic() {
        init();
    }
    void init() {
        Events.on(BuildSelectEvent.class, this::handleBuildSelectEvent);
        

    }
    
    private Instant lastWarningTime = Instant.now();
    private int handleWarningCooldown = 3;
    
    public void handleBuildSelectEvent(BuildSelectEvent event) {

        Team team = event.team;
        Unit builder = event.builder;
        Tile tile = event.tile;
        boolean breaking = event.breaking;
        Player player = builder.getPlayer();
        if (player != null && !breaking && team == player.team() && builder.getPlayer() != null && builder.buildPlan() != null) {

            Block cblock = builder.buildPlan().block;
            
            // persistent warnings that keep showing
            if (cblock instanceof NuclearReactor && Instant.now().isAfter(lastWarningTime.plusSeconds(handleWarningCooldown))) {
                lastWarningTime = Instant.now();
                
                float progress = builder.buildPlan().progress;
                float coreDistance = getDistanceToCore(builder.team(), tile.getX(), tile.getY()) /8f;
                // Vars.ui.showLabel(""+coreDistance, 5f, tile.worldx(), tile.worldy());
                if(coreDistance < 19) {
                    String message = "[scarlet]WARNING[] " + formatPlayer(player) + " is building a reactor at "
                            + formatTile(tile) + " [stat]" + Math.round(coreDistance) + "[] blocks from core. [stat]"
                            + Math.round(progress * 100) + "%";
                    sendMessage(message);
                    
                    
                }
            }
        }
    }
    static public void handlebeginBreakEvent(@Nullable Unit unit, Team team, int x, int y) {

        if(arc.Core.settings.getInt("removepowerwarn", 0) <= 0) {
            return;
        }
        Tile tile = Vars.world.tileBuilding(x, y);
        if(tile.build.block instanceof PowerBlock) {
            if(unit != null && unit.getPlayer() != null && unit.getPlayer().team().id == team.id) {
                int splitPower = Mathf.floor(tile.build.power.graph.getPowerProduced() * 60 );
                if(splitPower >= arc.Core.settings.getInt("removepowerwarn", 0)) {
                    if(mindustry.CustomClientLogic.willPowerGraphSplit(tile.build)) {
                        String message = String.format("[%s] remove power (%s) at (%d, %d)", Strings.stripColors(unit.getPlayer().name), UI.formatAmount(splitPower), tile.x, tile.y);
                        Vars.ui.showLabel(message, 10f, tile.worldx(), tile.worldy());
                        Vars.ui.chatfrag.addMessage(message);
                        Vars.ui.consolefrag.add(message);
                        
                    }
                }
            }
        }
    }

    static public void handleConfigEvent(@Nullable Player player, Building build, @Nullable Object value) {

        if(player == null || build == null || value == null) {
            return;
        }
        if(arc.Core.settings.getInt("splitpowerwarn", 0) <= 0) {
            return;
        }
        

        if(value instanceof Point2[] values) {
            /*
            Vars.ui.chatfrag.addMessage("length" + values.length);
            for (Point2 point2 : values) {
                Vars.ui.chatfrag.addMessage(point2.toString());
                Vars.ui.showLabel(point2.toString(), 10f, build.tile.worldx()+point2.x*Vars.tilesize, build.tile.worldy()+point2.y*Vars.tilesize);
            }*/
            if(values.length == 0) {
                if(build.block instanceof PowerNode) {
                    if(player.team().id == build.team.id) {
                        int splitPower = Mathf.floor(build.power.graph.getPowerProduced() * 60) ;
                        if(splitPower >= arc.Core.settings.getInt("splitpowerwarn", 0)) {
                            if(mindustry.CustomClientLogic.willPowerGraphSplit(build)) {
                                String message = String.format("[%s] split power (%s) at (%d, %d)", 
                                Strings.stripColors(player.name), UI.formatAmount(splitPower), build.tile.x, build.tile.y);
                                Vars.ui.showLabel(message, 10f, build.tile.worldx(), build.tile.worldy());
                                Vars.ui.chatfrag.addMessage(message);
                                Vars.ui.consolefrag.add(message);
                            }
                        }
                    }
                }
            }
        }

        if(value instanceof Integer valuei) {
            Building other = Vars.world.build(valuei);
            if(build.block instanceof PowerNode && other != null) {
                if(player.team().id == build.team.id) {
                    int splitPower = Mathf.floor(build.power.graph.getPowerProduced() * 60) ;
                    if(splitPower >= arc.Core.settings.getInt("splitpowerwarn", 0)) {
                        if(mindustry.CustomClientLogic.willPowerGraphSplitOther(build, other)) {
                            String message = String.format("[%s] split power (%s) at (%d, %d)", 
                            Strings.stripColors(player.name), UI.formatAmount(splitPower), build.tile.x, build.tile.y);
                            Vars.ui.showLabel(message, 10f, build.tile.worldx(), build.tile.worldy());
                            Vars.ui.chatfrag.addMessage(message);
                            Vars.ui.consolefrag.add(message);
                        }
                    }
                }
            }
        }

    }
    
    public boolean sendMessage(String message) {
            Vars.ui.chatfrag.addMessage(message);
        return true;
    }



    public static float powerUse(PowerGraph powerGraph ) {
        return (powerGraph.getLastPowerProduced())*60;
    }

    public static boolean hidddenRender() {
        return arc.Core.settings.getBool("hiddenrender") || (arc.Core.settings.getBool("hiddenrenderonbuild") && mindustry.Vars.control.input.isPlacing()); 
    }

    private static final Seq<Building> outArray1 = new Seq<>();
    private static final Seq<Building> outArray2 = new Seq<>();

    private static final Set<Building> visited = new HashSet<>();
    private static final Queue<Building> queue = new LinkedList<>();
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
        
        visited.clear();
        queue.clear();

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


    /**
     * 檢查移除兩個建築之間的直接電源連線後，電力網絡（PowerGraph）是否會因此分裂。
     *
     * @param buildingA 連線的一端。
     * @param buildingB 連線的另一端。
     * @return 如果移除這條連線會導致分裂，返回 true；否則返回 false。
     */
    public static boolean willPowerGraphSplitOther(Building buildingA, Building buildingB) {
        // 1. 基本檢查：如果 A 和 B 原本就沒有直接相連，移除一個不存在的連線不會造成任何影響。
        buildingA.getPowerConnections(outArray1);
        if (!outArray1.contains(buildingB)) {
            return false; // 它們之間沒有直接連線，所以不會分裂
        }
        
        // 核心思想：從 A 開始進行圖遍歷（例如 BFS），
        // 看看在不經過「A 到 B」這條捷徑的情況下，是否還能到達 B。

        visited.clear();
        queue.clear();

        queue.add(buildingA);
        visited.add(buildingA);

        while (!queue.isEmpty()) {
            Building current = queue.poll();

            // 遍歷當前節點的所有鄰居
            current.getPowerConnections(outArray1);
            for (Building next : outArray1) {

                // 關鍵：模擬 A 和 B 之間的連線被移除。
                // 我們只禁止從 A 直接走到 B。
                // 因為我們從 A 開始搜索，所以只需要檢查這一個方向。
                if (current.equals(buildingA) && next.equals(buildingB)) {
                    continue; // 跳過這條被"移除"的路徑
                }

                // 如果我們找到了另一條通往 B 的路徑
                if (next.equals(buildingB)) {
                    // 找到了替代路徑！這意味著即使移除直接連線，A 和 B 仍然連通。
                    // 所以電網不會分裂。
                    return false;
                }

                // 如果 next 節點尚未被訪問過
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }

        // 3. 遍歷結束後，如果從未通過其他路徑到達 B，
        // 這表示 A 和 B 之間的直接連線是它們唯一的橋樑。
        // 移除它將會導致電網分裂。
        return true;
    }




    public float getDistanceToCore(Team team, float x, float y) {

        if (team == null) {
            return Integer.MAX_VALUE;
        }
        CoreBuild nearestCore = Geometry.findClosest(x, y, Vars.state.teams.get(team).cores);
        return Mathf.dst(x, y, nearestCore.tile.getX(), nearestCore.tile.getY());
    }


    public String formatPlayer(Player target) {
        String playerString;
        if (target != null) {
            playerString = target.name;
        } else {
            playerString = "[lightgray]unknown[]";
        }
        return playerString;
    }

    public String formatTile(Tile tile) {
        if (tile == null)
            return "(none)";
        return "(" + tile.x + ", " + tile.y + ")";
    }
}
