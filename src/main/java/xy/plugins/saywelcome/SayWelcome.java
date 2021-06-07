package xy.plugins.saywelcome;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Utility;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.scheduler.BukkitTask;


import java.io.File;
import java.util.ArrayList;
import java.util.List;


import static org.bukkit.Bukkit.getScheduler;

public final class SayWelcome extends JavaPlugin implements Listener {

    private String SpecialMessage;
    private String ActionMessage;
    private boolean firstwelcome;
    private boolean welcomeback;
    private boolean exactmessage;
    private boolean multiplerewards;
    private boolean actionbarenabled;
    private int messagetimewelcome;
    private int messagetimewelcomeback;
    private  List<String> WelcomeMessages;
    private  List<String> WelcomeRewards;
    private  List<String> WelcomeBackMessages;
    private  List<String> WelcomeBackRewards;
    private  List<String> SpecialRewards;
    private  List<String> Players;



    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        this.loadSettings();
        System.out.println("SayWelcome is running");
        getServer().getPluginManager().registerEvents(this, this);
    }


    public void loadSettings() {
        String mainPath = this.getDataFolder().getPath() + "/";
        File file = new File(mainPath, "config.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        messagetimewelcome = cfg.getInt("MessageTime.Welcome");
        messagetimewelcomeback = cfg.getInt("MessageTime.WelcomeBack");
        WelcomeMessages = cfg.getStringList("MessagesFirstJoin");
        WelcomeRewards = cfg.getStringList("RewardsFirstJoin");
        WelcomeBackMessages = cfg.getStringList("MessagesWelcomeBack");
        WelcomeBackRewards = cfg.getStringList("RewardsWelcomeBack");
        SpecialRewards = cfg.getStringList("SpecialRewards");
        exactmessage = cfg.getBoolean("ExactMessage");
        multiplerewards = cfg.getBoolean("MultipleRewards");
        actionbarenabled = cfg.getBoolean("ActionBar.Enabled");
        SpecialMessage = cfg.getString("SpecialRewardMessage");
        ActionMessage = cfg.getString("ActionBar.Message");
        Players = new ArrayList<>();
    }






    @EventHandler
    public void playerjoin(PlayerJoinEvent event) {
        if (firstwelcome || welcomeback) {
            return;
        }

        Player p = event.getPlayer();
        Players.add(p.getName());


        int time;
        if (!p.hasPlayedBefore()){
            time = messagetimewelcome;
            firstwelcome = true;
            getScheduler().scheduleSyncDelayedTask(this, () -> {
                firstwelcome = false;
                Players.clear();
            }, (messagetimewelcome*20));
        } else {
            time = messagetimewelcomeback;
            welcomeback = true;
            getScheduler().scheduleSyncDelayedTask(this, () -> {
                welcomeback = false;
                Players.clear();
            }, (messagetimewelcomeback*20));
        }
        if (actionbarenabled) {

            if (ActionMessage.contains("&")) {
                ActionMessage = ActionMessage.replace("&", "§");
            }
            int[] timercount = {0};
            BukkitTask countdown = Bukkit.getScheduler().runTaskTimer(this, () -> {
                timercount[0]++;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.equals(p)) {

                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ActionMessage.replace("<time>", "" + (time - timercount[0]))));
                    }
                }
            }, 0, 20);
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> Bukkit.getScheduler().cancelTask(countdown.getTaskId()),(time*20));
        }
    }

    @EventHandler
    public void welcome(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();

        if (isVanished(p)) {
            return;
        }


        if (!firstwelcome && !welcomeback) {
            return;
        }

        boolean claimed = false;
        if (firstwelcome && !Players.contains(p.getName())) {
            for (String message : WelcomeMessages) {
                if (message.contains("<newplayer>")) {
                    message = message.replace("<newplayer>", Players.get(0));
                }
                if (event.getMessage().equals(SpecialMessage) && !claimed) {
                    for (String command : SpecialRewards) {
                        if (command.contains("<player>")) {
                            command = command.replace("<player>", p.getName());
                        }
                        commandrun(event, command);
                    }
                    Players.add(p.getName());
                    if (!multiplerewards) claimed = true;
                } else if ((event.getMessage().toLowerCase().contains(message.toLowerCase()) && !exactmessage && !claimed) || (event.getMessage().equals(message) && exactmessage && !claimed)) {

                    for (String command : WelcomeRewards) {
                        if (command.contains("<player>")) {
                            command = command.replace("<player>", p.getName());
                        }
                        commandrun(event, command);
                    }
                    Players.add(p.getName());
                    if (!multiplerewards) claimed = true;
                }
            }
        }
        if (welcomeback && !Players.contains(p.getName())) {
            for (String message : WelcomeBackMessages) {
                if ((event.getMessage().toLowerCase().contains(message.toLowerCase()) && !exactmessage && !claimed) || (event.getMessage().equals(message) && exactmessage && !claimed)) {
                    for (String command : WelcomeBackRewards) {
                        if (command.contains("<player>")) {
                            command = command.replace("<player>", event.getPlayer().getName());
                        }
                        commandrun(event, command);
                    }
                    Players.add(p.getName());
                    if (!multiplerewards) claimed = true;
                }
            }
        }
    }

    private boolean isVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }


    private void commandrun(AsyncPlayerChatEvent event, String command) {
        if (command.contains("msgplayer ")) {
            Player p = event.getPlayer();
            command = command.replace("msgplayer ", "");
            if (command.contains("&")) {
                command = command.replace("&", "§");
            }
            String finalCommand1 = command;
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> p.sendMessage(finalCommand1), 20);
        } else {
            ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
            String finalCommand = command;
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> Bukkit.dispatchCommand(console, finalCommand), 20);
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("swreload")) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (p.hasPermission("SayWelcome.reload")) {
                    p.sendMessage("Plugin has been reloaded");
                    this.loadSettings();
                } else {
                    p.sendMessage("No permission");
                }
            } else {
                System.out.println("Plugin has been reloaded");
                this.loadSettings();
            }
            return true;
        }
        return false;
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
