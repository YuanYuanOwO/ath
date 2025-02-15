package org.akadia.ath.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.akadia.ath.util.Util;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

@Plugin(id = "ath", name = "Ath", version = "1.0.0", authors = "akadia")
public class Main {

    private final String FILENAME = "record.txt";
    @Inject
    @DataDirectory
    public Path configDir;
    int maxCount;
    String achievedDate;
    File configFile;
    @Inject
    private ProxyServer server;
    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws IOException {
        maxCount = 0;
        achievedDate = "";
        File dataFolder = configDir.toFile();
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        configFile = new File(dataFolder, FILENAME);
        if (!configFile.exists()) {
            configFile.createNewFile();
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line = reader.readLine();
                if (line == null || line.trim().isEmpty()) {
                    maxCount = 0;
                } else {
                    try {
                        maxCount = Integer.parseInt(line);
                    } catch (NumberFormatException e) {
                        logger.error("配置文件格式错误，无法解析整数，使用默认值 0");
                        maxCount = 0;
                    }
                }

                String dateLine = reader.readLine();
                achievedDate = (dateLine == null) ? "" : dateLine;
            }
        }
    }

    @Subscribe
    public void onServerPostConnect(PlayerChooseInitialServerEvent event) throws IOException {
        int onlineCount = server.getPlayerCount();

        if (onlineCount <= maxCount) {
            return;
        }
        maxCount = onlineCount;
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        achievedDate = date;
        FileWriter fw = new FileWriter(configFile, false);
        fw.write(String.valueOf(maxCount) + '\n');
        fw.write(achievedDate);
        fw.flush();
        fw.close();
        logger.info(("Concurrent Online Player Record: %player_count%")
                .replaceAll("%player_count%", String.valueOf(maxCount)));

        for (Player player : server.getAllPlayers()) {
            player.sendMessage(
                    Component.text(Util.toColor("&6✿ &7服务器达到了新的玩家在线记录 &b%player_count%&7 在 &b%date% &6✿&7")
                            .replaceAll("%player_count%", String.valueOf(maxCount))
                            .replaceAll("%date%", date)));
        }

        server.getEventManager().fire(new AthRecordEvent(maxCount, achievedDate));
    }

}