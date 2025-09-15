package com.leclowndu93150.oneko.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.leclowndu93150.oneko.Constants;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class OnekoConfig {
    private static OnekoConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(Minecraft.getInstance().gameDirectory, "config/" + Constants.MOD_ID + ".json");

    public boolean enabled = true;
    public String enabled_description = "Enable or disable the Oneko overlay";
    
    public int catSize = 10;
    public String catSize_description = "Size of the cat sprite in pixels";
    
    public double runSpeed = 5.0;
    public String runSpeed_description = "Movement speed of the cat in pixels per frame";
    
    public double triggerDistance = 24.0;
    public String triggerDistance_description = "Distance in pixels the mouse must move before the cat starts chasing";
    
    public double catchDistance = 5.0;
    public String catchDistance_description = "Distance in pixels at which the cat stops chasing and goes idle";
    
    public long frameInterval = 100;
    public String frameInterval_description = "Milliseconds between animation frame updates";

    public static OnekoConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static OnekoConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                return GSON.fromJson(reader, OnekoConfig.class);
            } catch (IOException e) {
                Constants.LOG.error("Failed to load config, using defaults", e);
            }
        }
        
        OnekoConfig config = new OnekoConfig();
        config.save();
        return config;
    }

    public void save() {
        CONFIG_FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            Constants.LOG.error("Failed to save config", e);
        }
    }
}