package com.leclowndu93150.oneko.client;

import com.leclowndu93150.oneko.Constants;
import com.leclowndu93150.oneko.config.OnekoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class OnekoOverlay {
    private static OnekoOverlay instance;
    private final Minecraft minecraft;
    private final Map<Integer, ResourceLocation> textures = new HashMap<>();

    private double mouseX, mouseY;
    private double catX = -1, catY = -1;
    private int currentFrame = 0;
    private int animationTick = 0;
    private int state = 0;
    private int sleepCounter = 0;
    private int scratchCounter = 0;
    private boolean mouseMoved = false;
    private double lastMouseX, lastMouseY;
    private double targetX, targetY;

    private long lastFrameTime = 0;
    private int frameCount = 0;

    private enum CatState {
        IDLE(0),
        RUNNING(1),
        SLEEPING(2),
        SURPRISED(3);

        final int value;
        CatState(int value) { this.value = value; }
    }

    private OnekoOverlay() {
        this.minecraft = Minecraft.getInstance();
        loadTextures();
    }

    public static OnekoOverlay getInstance() {
        if (instance == null) {
            instance = new OnekoOverlay();
        }
        return instance;
    }

    private void loadTextures() {
        for (int i = 1; i <= 32; i++) {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/oneko/" + i + ".png");
            textures.put(i, location);
        }
        textures.put(0, textures.get(25));
    }

    public void render(GuiGraphics guiGraphics) {
        OnekoConfig config = OnekoConfig.getInstance();
        if (!config.enabled || !isCursorVisible()) {
            return;
        }

        if (catX < 0 || catY < 0) {
            catX = minecraft.getWindow().getGuiScaledWidth() / 2.0;
            catY = minecraft.getWindow().getGuiScaledHeight() / 2.0;
            targetX = catX;
            targetY = catY;
        }

        long currentTime = System.currentTimeMillis();
        
        updateMousePosition();
        
        if (currentTime - lastFrameTime >= config.frameInterval) {
            frameCount++;
            updateCatBehavior();
            lastFrameTime = currentTime;
        }
        
        renderCat(guiGraphics);
    }

    private boolean isCursorVisible() {
        if (minecraft.screen != null) {
            return true;
        }
        
        return minecraft.level != null && 
               minecraft.player != null && 
               !minecraft.options.hideGui &&
                !minecraft.mouseHandler.isMouseGrabbed();
    }

    private void updateMousePosition() {
        long window = minecraft.getWindow().getWindow();
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        GLFW.glfwGetCursorPos(window, xpos, ypos);

        double guiScale = minecraft.getWindow().getGuiScale();
        mouseX = xpos[0] / guiScale;
        mouseY = ypos[0] / guiScale;

        mouseX = Math.max(0, Math.min(mouseX, minecraft.getWindow().getGuiScaledWidth()));
        mouseY = Math.max(0, Math.min(mouseY, minecraft.getWindow().getGuiScaledHeight()));

        if (Math.abs(mouseX - lastMouseX) > 2 || Math.abs(mouseY - lastMouseY) > 2) {
            mouseMoved = true;
            targetX = mouseX;
            targetY = mouseY;
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    private void updateCatBehavior() {
        OnekoConfig config = OnekoConfig.getInstance();
        double diffX = catX - targetX;
        double diffY = catY - targetY;
        double distance = Math.sqrt(diffX * diffX + diffY * diffY);

        if (distance < config.catchDistance) {
            idle();
            return;
        }

        if (state != CatState.RUNNING.value && distance < config.triggerDistance) {
            idle();
            return;
        }

        if (state != CatState.RUNNING.value) {
            state = CatState.RUNNING.value;
            sleepCounter = 0;
        }

        catX -= (diffX / distance) * config.runSpeed;
        catY -= (diffY / distance) * config.runSpeed;
        
        catX = Math.max(config.catSize/2.0, Math.min(catX, minecraft.getWindow().getGuiScaledWidth() - config.catSize/2.0));
        catY = Math.max(config.catSize/2.0, Math.min(catY, minecraft.getWindow().getGuiScaledHeight() - config.catSize/2.0));
        
        currentFrame = getRunFrame();
    }
    
    private void idle() {
        sleepCounter++;
        
        if (sleepCounter > 100 && Math.random() < 0.005 && state == CatState.IDLE.value) {
            state = CatState.SLEEPING.value;
            currentFrame = 29;
        }
        
        switch (state) {
            case 2:
                currentFrame = (frameCount % 8 < 4) ? 29 : 30;
                break;
            case 0:
            default:
                currentFrame = 25;
                if (state != CatState.IDLE.value) {
                    state = CatState.IDLE.value;
                    sleepCounter = 0;
                }
                break;
        }
    }

    private int getRunFrame() {
        double diffX = catX - targetX;
        double diffY = catY - targetY;
        double distance = Math.sqrt(diffX * diffX + diffY * diffY);
        
        if (distance == 0) return currentFrame;
        
        double dirX = diffX / distance;
        double dirY = diffY / distance;
        
        String direction = "";
        if (dirY > 0.5) direction += "N";
        if (dirY < -0.5) direction += "S";
        if (dirX > 0.5) direction += "W";
        if (dirX < -0.5) direction += "E";
        
        int frame1, frame2;
        switch (direction) {
            case "N": frame1 = 1; frame2 = 2; break;
            case "NE": frame1 = 3; frame2 = 4; break;
            case "E": frame1 = 5; frame2 = 6; break;
            case "SE": frame1 = 7; frame2 = 8; break;
            case "S": frame1 = 9; frame2 = 10; break;
            case "SW": frame1 = 11; frame2 = 12; break;
            case "W": frame1 = 13; frame2 = 14; break;
            case "NW": frame1 = 15; frame2 = 16; break;
            default: return currentFrame;
        }
        
        return (frameCount % 2 == 0) ? frame1 : frame2;
    }

    private void renderCat(GuiGraphics guiGraphics) {
        OnekoConfig config = OnekoConfig.getInstance();
        ResourceLocation texture = textures.get(currentFrame);
        if (texture == null) {
            texture = textures.get(0);
        }
        if (texture == null) {
            return;
        }

        int x = (int)(catX - config.catSize/2);
        int y = (int)(catY - config.catSize/2);
        
        guiGraphics.blit(texture, x, y, 0, 0, config.catSize, config.catSize, config.catSize, config.catSize);
    }
}