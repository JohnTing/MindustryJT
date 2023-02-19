package mindustry.input;

import arc.Core;
import arc.Events;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.Vars;
public class HideUnit {
    
    boolean hidding1 = false;
    boolean hidding2 = false;
    boolean setHidding = false;
    boolean buildhideunit = false;

    public HideUnit() {

        Events.on(WorldLoadEvent.class, e -> {
            hidding1 = false;
            hidding2 = false;
            buildhideunit = Core.settings.getBool("buildhideunit");
            Core.settings.put("hideunit", false);
        });

        // isBuilding
        Events.run(Trigger.update, () -> {
            
            if(Core.input.keyTap(Binding.hide_units)) {
                hidding1 = !hidding1;
            }
            if(buildhideunit && Vars.control.input.isPlacing()) {
                hidding2 = true;
            } else {
                hidding2 = false;
            }
            if(setHidding != (hidding1 || hidding2)) {
                setHidding = (hidding1 || hidding2);
                Core.settings.put("hideunit", setHidding);
            }
        });


    }



}
