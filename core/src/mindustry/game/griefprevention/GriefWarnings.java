package mindustry.game.griefprevention;

import arc.Core;
import arc.Events;
import arc.func.Func;
import arc.graphics.Color;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.core.World;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.game.EventType.BuildSelectEvent;
import mindustry.game.EventType.ConfigEvent;
import mindustry.game.EventType.DepositEvent;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.TileChangeEvent;
import mindustry.game.EventType.WithdrawEvent;
import mindustry.game.EventType.WorldLoadEvent;

import mindustry.game.griefprevention.TileInfo.TileAction;
import mindustry.gen.Builderc;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.logic.LAssembler;
import mindustry.logic.LExecutor;
import mindustry.logic.LStatement;
import mindustry.logic.LStatements;
import mindustry.logic.LUnitControl;
import mindustry.logic.LExecutor.EndI;
import mindustry.logic.LExecutor.LInstruction;
import mindustry.logic.LExecutor.UnitControlI;
import mindustry.mod.Plugin;
import mindustry.net.Administration.TraceInfo;
import mindustry.net.Packets.AdminAction;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.Sorter;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;
import mindustry.world.blocks.power.ItemLiquidGenerator;
import mindustry.world.blocks.power.NuclearReactor;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.blocks.power.PowerNode;
import mindustry.world.blocks.sandbox.ItemSource;
import mindustry.world.blocks.sandbox.LiquidSource;
import mindustry.world.blocks.storage.StorageBlock;
import mindustry.world.blocks.storage.Unloader;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.BlockGroup;

import static mindustry.Vars.*;
import static mindustry.Vars.player;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Vars.griefWarnings.autoStoplogic = true
public class GriefWarnings {

    public GriefWarnings() {
        // js Groups.player.index(0).team(Team.crux)
        // js Groups.player.index(0).team(Team.sharded)
        Events.on(DepositEvent.class, this::handleDeposit);
        Events.on(BuildSelectEvent.class, this::handleBuildSelectEvent);
        Events.on(BlockBuildEndEvent.class, this::handleBlockBuildEndEvent);
        Events.on(ConfigEvent.class, this::handleConfigEvent);
        Events.on(WorldLoadEvent.class, this::handleWorldLoadEvent);
        // Tile tile = Vars.world.tile((int)Core.input.mouseWorldX()/8, (int)Core.input.mouseWorldY()/8);

        Player player = Vars.player;
        if (player.unit().stack.amount > 0) {
            ItemStack stack = player.unit().stack;

            //Vars.indexer.getAllied(Vars.player.team(), BlockFlag.turret).iterator();
            
            for(Tile tile : Vars.indexer.getAllied(Vars.player.team(), BlockFlag.turret)) {
                if(tile.build != null) {
                    Building build = tile.build;

                    if(build.acceptStack(stack.item, stack.amount, player.unit()) > 0 && build.interactable(player.team()) && build.block.hasItems && build.interactable(player.team() )) {
                        Call.transferInventory(player, build);
                        return;
                    }
                }
            } 
        }

        

        /*

        if(build != null && build.acceptStack(stack.item, stack.amount, player.unit()) > 0 && build.interactable(player.team()) && build.block.hasItems && player.unit().stack().amount > 0 && build.interactable(player.team())){
            Call.transferInventory(player, build);
        }else{
*/
        //Geometry.findClosest(Vars.player.x, Vars.player.y, Vars.indexer.getAllied(Vars.player.team(), BlockFlag.turret));
        // Vars.indexer.getAllied(Vars.player.team(), BlockFlag.turret).first()
        // player.within(build, Vars.buildingRange)
        // Call.takeItems(build, item, unit.maxAccepted(item), Vars.player);

        // Blocks.blastMixer
        // 
        //Drawf.circles(x, y, 5);
        // Groups.player.index(1).;
        // player.con.viewX
        
        //Groups.player.index(1).unit().team
        // Groups.player.index(1).con.viewX
        //Groups.player.index(1).

        // Vars.player.unit().mineTile = Vars.world.tile(World.toTile(Core.input.mouseX()), World.toTile(Core.input.mouseY()))
        //player.unit().mineTile = tileAt(Core.input.mouseX(), Core.input.mouseY());
        
        // Vars.player.unit().mineTile = Vars.world.tile(Core.input.mouseWorldX()/8, Core.input.mouseWorldY()/8);
        
    }

    public void handleWorldLoadEvent(WorldLoadEvent event) {
        Core.settings.put("hideunit", false);

    }

    public static boolean isHiding() {
        return Core.settings.getBool("hideunit") || (Core.settings.getBool("buildhideunit", false)
                && (control.input.isPlacing() || control.input.isBreaking()));
    }

    Map<Tile, Player> powerSplit = new HashMap<Tile, Player>();

    private Instant lastWarningTime = Instant.now();

    public boolean sendMessage(String message, int cooldown, boolean log) {

        if (!Instant.now().isAfter(lastWarningTime.plusSeconds(cooldown))) {
            // ui.chatfrag.addMessage("skip", null);
            return false;
        }
        lastWarningTime = Instant.now();

        ui.chatfrag.addMessage(message, null);
        if (log) {
            ui.scriptfrag.addMessage(message);
        }
        return true;
    }

    public boolean sendMessage(String message) {
        return sendMessage(message, 1, false);
    }

    public boolean sendMessage(String message, int cooldown) {
        return sendMessage(message, cooldown, false);
    }

    // Vars.griefWarnings.searchAndStopGriefLogic()
    public int foo1() {
        return searchAndStopGriefLogic();
    }

    public int searchAndStopGriefLogic() {
        int count = 0;
        for (int x = 0; x < world.width(); x++) {
            for (int y = 0; y < world.height(); y++) {
                Building other = world.build(x, y);
                if (other == null) {
                    continue;
                }
                if (checkGriefLogic(other) > 0) {
                    String message = "stop logic at " + formatTile(other.tile());
                    ui.scriptfrag.addMessage(message);
                    stopLogic((LogicBuild) other);
                    count += 1;
                }
                /*
                 * if(other instanceof LogicBuild logicBuild) {
                 * if(logicBuild.executor.instructions.length == 0) { String message =
                 * "stop none logic at " + formatTile(other.tile());
                 * ui.scriptfrag.addMessage(message);
                 * logicBuild.configure(LogicBlock.compress("End",
                 * logicBuild.relativeConnections())); count += 1; } }
                 */
            }
        }
        return count;
    }

    public int checkGriefLogic(Building build) {
        if (build instanceof LogicBuild logicBuild) {
            if (logicBuild.executor.instructions.length > 0 && logicBuild.executor.instructions[0] instanceof EndI) {
                return 0;
            }
            for (LInstruction li : logicBuild.executor.instructions) {
                if (li instanceof UnitControlI unitControlI) {

                    if (unitControlI.type.equals(LUnitControl.build)) {

                        String message = "unitControlI.p5 = " + unitControlI.p5;
                        ui.scriptfrag.addMessage(message);
                        if (unitControlI.p5 == 3) {
                            return 2;
                        }
                        return 1;
                    }
                }
            }
        }
        return 0;
    }

    public void handleConfigEvent(ConfigEvent event) {
        /*
         * if (checkGriefLogic(event.tile) && event.player != null) { String message =
         * "[lightgray]WARNING[] " + formatPlayer(event.player) +
         * " config a [yellow]unit-build logic processor[] at " +
         * formatTile(event.tile.tile()); sendMessage(message, 0, true); }
         */
    }

    public void handleBlockBuildEndEvent(BlockBuildEndEvent event) {
        if (!event.breaking && checkGriefLogic(event.tile.build) > 0) {

            if (event.unit != null) {
                if (event.unit.getPlayer() != null) {
                    String message = "[scarlet]WARNING[] " + formatPlayer(event.unit.getPlayer())
                            + " build a [yellow]unit-build logic processor[] at " + formatTile(event.tile);
                    sendMessage(message, 0, true);
                } else {
                    /*
                     * String message = "[scarlet]WARNING[] #" + event.unit.toString() +
                     * " build a [yellow]unit-build logic processor[] at " + formatTile(event.tile);
                     * sendMessage(message, 0, true);
                     */
                }
            }
            boolean autoStoplogic = Core.settings.getBool("stopunitbuildlogic");
            if (autoStoplogic) {
                searchAndStopGriefLogic();
                /*
                 * LogicBuild logicBuild = (LogicBuild)event.tile.build; stopLogic(logicBuild);
                 */
            }
        }
    }

    public void stopLogic(LogicBuild logicBuild) {

        Seq<LStatement> statements = LAssembler.read(logicBuild.code);
        for (LStatement ls : statements) {
            if (ls instanceof LStatements.JumpStatement jump) {
                jump.destIndex += 1;
            }
        }
        statements.truncate(LExecutor.maxInstructions);
        statements.insert(0, new LStatements.EndStatement());

        logicBuild.configure(LogicBlock.compress(LAssembler.write(statements), logicBuild.relativeConnections()));
    }

    public void handleBuildSelectEvent(BuildSelectEvent event) {

        Team team = event.team;
        Unit builder = event.builder;
        Tile tile = event.tile;
        boolean breaking = event.breaking;

        if (!breaking && team == player.team() && builder.getPlayer() != null && builder.buildPlan() != null) {

            Player player = builder.getPlayer();
            float progress = builder.buildPlan().progress;

            float coreDistance = getDistanceToCore(player.unit(), tile);
            Block cblock = builder.buildPlan().block;

            // persistent warnings that keep showing
            if (coreDistance < 19 && cblock instanceof NuclearReactor) {
                String message = "[scarlet]WARNING[] " + formatPlayer(player) + " is building a reactor at "
                        + formatTile(tile) + " [stat]" + Math.round(coreDistance) + "[] blocks from core. [stat]"
                        + Math.round(progress * 100) + "%";
                sendMessage(message, 10 + 30 * (int) (1 - progress));
            }

            if (cblock instanceof NuclearReactor) {
                String message = "[lightgray]Notice[] " + formatPlayer(player) + " is building a reactor at "
                        + formatTile(tile) + "[stat]" + Math.round(progress * 100) + "%";
                sendMessage(message, 20 + 60 * (int) (1 - progress));
            }
        }
    }

    public void handlePowerGraphSplit(Tile tile, Player player) {
        sendMessage("[lightgray]Notice[] Power split by " + formatPlayer(player) + " at " + formatTile(tile));
    }

    public void handleDeposit(DepositEvent event) {
        Player targetPlayer = event.player;

        Tile tile = event.tile.tile();
        Item item = event.item;
        int amount = event.amount;
        if (targetPlayer == null)
            return;

        if (item.equals(Items.thorium) && tile.block() instanceof NuclearReactor) {
            String message = "[scarlet]WARNING[] " + targetPlayer.name + "[white] ([stat]#" + targetPlayer.id
                    + "[]) transfers [accent]" + amount + "[] thorium to a reactor. " + formatTile(tile);
            sendMessage(message);
        }
        /*
         * else if (item.explosiveness > 0.5f) { Block block = tile.block(); if (block
         * instanceof ItemLiquidGenerator) { String message = "[scarlet]WARNING[] " +
         * formatPlayer(targetPlayer) + " transfers [accent]" + amount +
         * "[] blast to a generator. " + formatTile(tile); sendMessage(message); } else
         * if (block instanceof StorageBlock) { String message = "[scarlet]WARNING[] " +
         * formatPlayer(targetPlayer) + " transfers [accent]" + amount +
         * "[] blast to a Container. " + formatTile(tile); sendMessage(message); } }
         */
    }

    public void handleThoriumReactorheat(Tile tile, float heat) {
        if (heat > 0.15f && tile.interactable(player.team())) {
            sendMessage("[scarlet]WARNING[] Thorium reactor at " + formatTile(tile) + " is overheating! Heat: [accent]"
                    + heat, 10 + 30 * (int) (1 - heat));
        }

    }

    public float getDistanceToCore(Unit unit, float x, float y) {
        if (unit == null) {
            return Integer.MAX_VALUE;
        }
        if (unit.closestCore() == null) {
            return Integer.MAX_VALUE;
        }
        Tile nearestCore = unit.closestCore().tile();
        if (nearestCore == null) {
            return Integer.MAX_VALUE;
        }
        return Mathf.dst(x, y, nearestCore.x, nearestCore.y);
    }

    public float getDistanceToCore(Unit unit, Tile tile) {
        if (tile == null) {
            return Integer.MAX_VALUE;
        }
        return getDistanceToCore(unit, tile.x, tile.y);
    }

    public float getDistanceToCore(Unit unit) {
        return getDistanceToCore(unit, unit.x, unit.y);
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
