package net.otlg.worlddiscarder;

import lombok.Getter;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class WorldDiscarderConfig {
    @Getter
    private List<String> denyList = new ArrayList<>();

    @Getter
    private boolean authEnabled = false;

    @Getter
    private boolean unauthAllowFreeze = true;

    @Getter
    private boolean unauthAllowReload = true;

    public WorldDiscarderConfig(){
        //denyList.add("example_world");
        //denyList.add("example_world2");
    }
}
