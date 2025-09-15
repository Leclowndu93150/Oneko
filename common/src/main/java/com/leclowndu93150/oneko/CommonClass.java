package com.leclowndu93150.oneko;

import com.leclowndu93150.oneko.config.OnekoConfig;

public class CommonClass {
    
    public static void init() {
        Constants.LOG.info("Oneko mod initialized");
        OnekoConfig.getInstance();
    }
}
