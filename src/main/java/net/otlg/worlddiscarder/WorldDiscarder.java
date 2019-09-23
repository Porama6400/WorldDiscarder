package net.otlg.worlddiscarder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.List;

public class WorldDiscarder extends JavaPlugin {
    @Getter
    private static WorldDiscarder plugin;
    @Getter
    List<String> denySaveList;
    @Getter
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    @Getter
    File configFile;
    @Getter
    WorldDiscarderConfig discarderConfig = new WorldDiscarderConfig();
    @Getter
    AuthManager authManager = new AuthManager();

    @Override
    public void onEnable() {
        inject();
        loadConfig();
    }

    public void inject() {
        try {

            ClassPool classPool = new ClassPool(true);
            classPool.importPackage("java.util.List");
            classPool.importPackage("java.util.ArrayList");
            CtClass providerClass = classPool.get(ReflectionUtil.getChunkProviderClasspath());

            final CtMethod saveChunk = providerClass.getDeclaredMethod("saveChunk");
            final CtMethod saveChunkNOP = providerClass.getDeclaredMethod("saveChunkNOP");

            //CtField discardChangesField = CtField.make("public boolean discardChanges = true;", providerClass);
            //providerClass.addField(discardChangesField);

            CtField denySaveField = CtField.make("public static List denySaveList = new ArrayList();", providerClass);
            providerClass.addField(denySaveField);

            saveChunk.insertBefore("if(denySaveList.contains(chunk.world.getWorld().getName())) return;");
            saveChunkNOP.insertBefore("if(denySaveList.contains(chunk.world.getWorld().getName())) return;");

            // INJECT!
            providerClass.toClass();
            getLogger().info("Injection success!");

            Field field = ReflectionUtil.getChunkProviderServerField(classPool.getClassLoader(), "denySaveList");
            //noinspection unchecked
            denySaveList = (List<String>) field.get(null);
        } catch (Exception exception) {
            exception.printStackTrace();
            getLogger().severe("Injection failed!");
        }
    }

    public void saveWDConfig() {
        try {
            FileWriter fileWriter = new FileWriter(configFile);
            gson.toJson(discarderConfig, fileWriter);
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean loadConfig() {
        try {
            configFile = new File(this.getDataFolder() + File.separator + "config.yml");
            if (!configFile.exists()) {
                getDataFolder().mkdirs();
                saveWDConfig();
            } else discarderConfig = gson.fromJson(new FileReader(configFile), WorldDiscarderConfig.class);

            if (denySaveList == null) {
                getLogger().severe("Failed to access deny list");
                return false;
            }

            denySaveList.clear();
            denySaveList.addAll(discarderConfig.getDenyList());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            boolean freezeStateCache = true;
            switch (args[0].toLowerCase()) {
                case "reload":
                case "load":
                    if (getDiscarderConfig().isAuthEnabled() && !authManager.isSessionValid() && !getDiscarderConfig().isUnauthAllowReload()) {
                        sender.sendMessage(Messages.SESSION_INVALID);
                        return true;
                    }
                    loadConfig();
                    sender.sendMessage(ChatColor.GREEN + "Config reloaded!");
                    break;
                case "save":
                    if (getDiscarderConfig().isAuthEnabled() && !authManager.isSessionValid() && !getDiscarderConfig().isUnauthAllowReload()) {
                        sender.sendMessage(Messages.SESSION_INVALID);
                        return true;
                    }
                    saveWDConfig();
                    sender.sendMessage(ChatColor.GREEN + "config saved!");
                    break;
                case "unfreeze":
                case "unlock":
                case "allow":
                case "thaw":
                    freezeStateCache = false;


                case "freeze":
                case "lock":
                case "deny":
                case "discard":

                    if (getDiscarderConfig().isAuthEnabled() && !authManager.isSessionValid() && !(freezeStateCache && getDiscarderConfig().isUnauthAllowFreeze())) {
                        sender.sendMessage(Messages.SESSION_INVALID);
                        return true;
                    }

                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.YELLOW + "/worlddiscarder " + args[0] + " [world]");
                        sender.sendMessage(ChatColor.GREEN + "Valid worlds:");
                        Bukkit.getWorlds().forEach(world -> {
                            sender.sendMessage(ChatColor.GREEN + world.getName());
                        });
                        break;
                    }
                    String worldName = args[1];
                    if (Bukkit.getWorld(worldName) == null) {
                        sender.sendMessage(ChatColor.RED + worldName + " does not exist!");
                        break;
                    }

                    if (freezeStateCache) {
                        denySaveList.add(worldName);
                        sender.sendMessage(ChatColor.GREEN + worldName + " is now " + ChatColor.AQUA + "frozen!");
                    } else {
                        denySaveList.remove(worldName);
                        sender.sendMessage(ChatColor.GREEN + worldName + " is now " + ChatColor.RED + "thawed!");
                    }

                    break;
                case "list":
                case "info":
                    sender.sendMessage(ChatColor.YELLOW + "Worlds: [" + ChatColor.AQUA + "frozen" + ChatColor.YELLOW + ", " + ChatColor.GRAY + "thawed" + ChatColor.YELLOW + "]");
                    Bukkit.getWorlds().forEach(world -> {
                        boolean isFrozen = denySaveList.contains(world.getName());
                        sender.sendMessage((isFrozen ? ChatColor.AQUA : ChatColor.GRAY) + world.getName());
                    });
                    break;
                case "auth":
                case "authenticate":
                    if (args.length <= 1) {
                        if (sender != Bukkit.getConsoleSender()) {
                            sender.sendMessage(ChatColor.YELLOW + "Please use " + ChatColor.GREEN + "/worlddiscarder auth [passcode]");
                            sender.sendMessage(ChatColor.YELLOW + "You will receive the passcord in the console.");
                        }
                        getLogger().info("Passcode: " + authManager.getCode());
                        break;
                    }

                    if (authManager.checkCode(args[1])) {
                        authManager.resetSession();
                        authManager.resetCode();
                        sender.sendMessage(ChatColor.GREEN + "Authenticated success!");
                    } else {
                        try {
                            Thread.sleep(100);
                            sender.sendMessage(ChatColor.RED + "Wrong password!");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                default:
                    sendHelp(sender);
                    break;
            }
        } else {
            sendHelp(sender);
        }
        return true;
    }

    public void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Commands:");
        sender.sendMessage(ChatColor.GRAY + "/worlddiscarder reload");
        sender.sendMessage(ChatColor.GRAY + "/worlddiscarder save");
        sender.sendMessage(ChatColor.GRAY + "/worlddiscarder list");
        sender.sendMessage(ChatColor.GRAY + "/worlddiscarder [unfreeze,unlock,allow,thaw]");
        sender.sendMessage(ChatColor.GRAY + "/worlddiscarder [freeze,lock,deny,discard]");
        sender.sendMessage(ChatColor.GRAY + "/worlddiscarder auth");
    }
}
