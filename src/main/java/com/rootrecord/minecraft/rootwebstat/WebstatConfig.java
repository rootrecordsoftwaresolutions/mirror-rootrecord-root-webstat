package com.rootrecord.minecraft.rootwebstat;

import com.rootrecord.minecraft.common.RootRecordFolders;
import com.rootrecord.minecraft.common.config.RootMcDatabaseConfig;
import com.rootrecord.minecraft.common.config.RootRecordYamlConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/** Snapshot of {@code plugins/RootMC/root-webstat.yml}. */
public final class WebstatConfig {

    private final boolean enabled;
    private final String bind;
    private final int port;
    private final String publicUrl;
    private final String token;
    private final String dayWorld;
    private final boolean onVanillaDay;
    private final int refreshSeconds;
    private final String economyTablePrefix;
    private final boolean includeZeroBalances;
    private final boolean pushEnabled;
    private final int pushIntervalSeconds;
    private final String serverNameOverride;

    public WebstatConfig(
            boolean enabled,
            String bind,
            int port,
            String publicUrl,
            String token,
            String dayWorld,
            boolean onVanillaDay,
            int refreshSeconds,
            String economyTablePrefix,
            boolean includeZeroBalances,
            boolean pushEnabled,
            int pushIntervalSeconds,
            String serverNameOverride) {
        this.enabled = enabled;
        this.bind = bind;
        this.port = port;
        this.publicUrl = publicUrl;
        this.token = token;
        this.dayWorld = dayWorld;
        this.onVanillaDay = onVanillaDay;
        this.refreshSeconds = refreshSeconds;
        this.economyTablePrefix = economyTablePrefix;
        this.includeZeroBalances = includeZeroBalances;
        this.pushEnabled = pushEnabled;
        this.pushIntervalSeconds = pushIntervalSeconds;
        this.serverNameOverride = serverNameOverride;
    }

    public static WebstatConfig from(JavaPlugin plugin, FileConfiguration cfg) {
        if (cfg == null) {
            cfg = new YamlConfiguration();
        }
        String prefix = cfg.getString("stats.economy-table-prefix", "");
        if (prefix == null || prefix.isBlank()) {
            prefix = resolveEssentialsPrefix(plugin);
        }
        int port = cfg.getInt("web.port", 8765);
        if (port < 1 || port > 65535) {
            port = 8765;
        }
        return new WebstatConfig(
                cfg.getBoolean("enabled", true),
                str(cfg.getString("web.bind"), "0.0.0.0"),
                port,
                str(cfg.getString("web.public-url"), ""),
                str(cfg.getString("web.token"), ""),
                str(cfg.getString("stats.day-world"), ""),
                cfg.getBoolean("stats.on-vanilla-day", true),
                Math.max(0, cfg.getInt("stats.refresh-seconds", 0)),
                prefix,
                cfg.getBoolean("stats.include-zero-balances", false),
                cfg.getBoolean("cloud.push-enabled", true),
                Math.max(0, cfg.getInt("cloud.push-interval-seconds", 0)),
                str(cfg.getString("server-name"), ""));
    }

    private static String resolveEssentialsPrefix(JavaPlugin plugin) {
        // Prefer shared database.yml (canonical) over legacy essentials-only keys.
        try {
            RootMcDatabaseConfig.DatabaseSettings db = RootMcDatabaseConfig.resolve(plugin, null);
            if (db != null) {
                String p = db.tablePrefix();
                if (p != null && !p.isBlank()) {
                    return p.trim();
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        File file = RootRecordFolders.configFile(plugin, RootRecordFolders.ROOT_ESSENTIALS_CONFIG);
        if (file.isFile()) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            String p = yml.getString("mysql.table-prefix", yml.getString("table-prefix", ""));
            if (p != null && !p.isBlank()) {
                return p.trim();
            }
        }
        return "root_";
    }

    private static String str(String v, String fallback) {
        return v == null || v.isBlank() ? fallback : v.trim();
    }

    public boolean enabled() {
        return enabled;
    }

    public String bind() {
        return bind;
    }

    public int port() {
        return port;
    }

    public String publicUrl() {
        return publicUrl;
    }

    public String token() {
        return token;
    }

    public String dayWorld() {
        return dayWorld;
    }

    public boolean onVanillaDay() {
        return onVanillaDay;
    }

    public int refreshSeconds() {
        return refreshSeconds;
    }

    public String economyTablePrefix() {
        return economyTablePrefix;
    }

    public boolean includeZeroBalances() {
        return includeZeroBalances;
    }

    public boolean pushEnabled() {
        return pushEnabled;
    }

    public int pushIntervalSeconds() {
        return pushIntervalSeconds;
    }

    public String serverNameOverride() {
        return serverNameOverride;
    }

    public String economyBalancesTable() {
        return economyTablePrefix + "economy_balances";
    }
}
