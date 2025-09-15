package com.leclowndu93150.oneko.client;

import com.leclowndu93150.oneko.Constants;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OnekoOverlay {
    private static OnekoOverlay instance;
    private final Minecraft minecraft;
    private final Map<Integer, Integer> textures = new HashMap<>();

    private double mouseX, mouseY;
    private double catX = 100, catY = 100;
    private int currentFrame = 0;
    private int animationTick = 0;
    private int state = 0;
    private int sleepCounter = 0;
    private int scratchCounter = 0;
    private int sharpCounter = 0;
    private boolean mouseMoved = false;
    private double lastMouseX, lastMouseY;
    private double targetX, targetY;

    private static final int CAT_SIZE = 32;
    private static final double RUN_SPEED = 4.0;
    private static final double TRIGGER_DISTANCE = 48.0;
    private static final double CATCH_DISTANCE = 8.0;

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
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/neko/" + i + ".png");
            int textureId = createTextureFromResource(location);
            if (textureId != -1) {
                textures.put(i, textureId);
            }
        }
        textures.put(0, textures.getOrDefault(25, -1));
    }

    private int createTextureFromResource(ResourceLocation location) {
        try {
            Optional<Resource> resource = minecraft.getResourceManager().getResource(location);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().open()) {
                    NativeImage image = NativeImage.read(stream);
                    int textureId = GlStateManager._genTexture();

                    RenderSystem.bindTexture(textureId);
                    image.upload(0, 0, 0, false);

                    GlStateManager._texParameter(3553, 10241, 9728);
                    GlStateManager._texParameter(3553, 10240, 9728);
                    GlStateManager._texParameter(3553, 10242, 10496);
                    GlStateManager._texParameter(3553, 10243, 10496);

                    image.close();
                    return textureId;
                }
            }
        } catch (IOException e) {
            Constants.LOG.warn("Failed to load neko texture: " + location, e);
        }
        return -1;
    }

    public void render() {
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        updateMousePosition();
        updateCatBehavior();
        renderCat();
    }

    private void updateMousePosition() {
        long window = minecraft.getWindow().getWindow();
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        GLFW.glfwGetCursorPos(window, xpos, ypos);

        double guiScale = minecraft.getWindow().getGuiScale();
        mouseX = xpos[0] / guiScale;
        mouseY = ypos[0] / guiScale;

        if (Math.abs(mouseX - lastMouseX) > 2 || Math.abs(mouseY - lastMouseY) > 2) {
            mouseMoved = true;
            targetX = mouseX;
            targetY = mouseY;
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    private void updateCatBehavior() {
        animationTick++;

        double dx = targetX - catX;
        double dy = targetY - catY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.atan2(dy, dx);

        if (distance > TRIGGER_DISTANCE && state != CatState.RUNNING.value) {
            state = CatState.RUNNING.value;
            currentFrame = 32;
            sleepCounter = 0;
            scratchCounter = 0;
        }

        switch (state) {
            case 1: // Running
                if (distance > CATCH_DISTANCE) {
                    double moveX = Math.cos(angle) * Math.min(RUN_SPEED, distance);
                    double moveY = Math.sin(angle) * Math.min(RUN_SPEED, distance);
                    catX += moveX;
                    catY += moveY;

                    currentFrame = getRunFrame(angle);
                } else {
                    state = CatState.IDLE.value;
                    currentFrame = 0;
                }
                mouseMoved = false;
                break;

            case 0: // Idle
                if (mouseMoved && distance > TRIGGER_DISTANCE) {
                    state = CatState.SURPRISED.value;
                    currentFrame = 32;
                } else if (animationTick % 10 == 0) {
                    sleepCounter++;
                    if (sleepCounter < 20) {
                        currentFrame = (currentFrame == 31) ? 25 : 31;
                    } else if (sleepCounter < 40) {
                        currentFrame = (currentFrame == 27) ? 28 : 27;
                        scratchCounter++;
                    } else if (sleepCounter == 40) {
                        currentFrame = 26;
                    } else if (sleepCounter > 45) {
                        state = CatState.SLEEPING.value;
                        currentFrame = 29;
                    }
                }
                break;

            case 2: // Sleeping
                if (mouseMoved && distance > TRIGGER_DISTANCE) {
                    state = CatState.SURPRISED.value;
                    currentFrame = 32;
                    sleepCounter = 0;
                } else if (animationTick % 30 == 0) {
                    currentFrame = (currentFrame == 29) ? 30 : 29;
                }
                break;

            case 3: // Surprised
                if (animationTick % 10 == 0) {
                    state = CatState.RUNNING.value;
                }
                break;
        }
    }

    private int getRunFrame(double angle) {
        if (animationTick % 5 != 0) {
            return currentFrame;
        }

        double pi = Math.PI;
        if (angle >= -pi/8 && angle <= pi/8) {
            return (currentFrame == 5) ? 6 : 5;
        } else if (angle > pi/8 && angle < 3*pi/8) {
            return (currentFrame == 3) ? 4 : 3;
        } else if (angle >= 3*pi/8 && angle <= 5*pi/8) {
            return (currentFrame == 1) ? 2 : 1;
        } else if (angle > 5*pi/8 && angle < 7*pi/8) {
            return (currentFrame == 15) ? 16 : 15;
        } else if (angle >= 7*pi/8 || angle <= -7*pi/8) {
            return (currentFrame == 13) ? 14 : 13;
        } else if (angle > -7*pi/8 && angle < -5*pi/8) {
            return (currentFrame == 11) ? 12 : 11;
        } else if (angle >= -5*pi/8 && angle <= -3*pi/8) {
            return (currentFrame == 9) ? 10 : 9;
        } else if (angle > -3*pi/8 && angle < -pi/8) {
            return (currentFrame == 7) ? 8 : 7;
        }
        return currentFrame;
    }

    private void renderCat() {
        Integer textureId = textures.get(currentFrame);
        if (textureId == null || textureId == -1) {
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.bindTexture(textureId);

        Matrix4f matrix = new Matrix4f().ortho(0.0F,
                (float)minecraft.getWindow().getGuiScaledWidth(),
                (float)minecraft.getWindow().getGuiScaledHeight(),
                0.0F, 1000.0F, 3000.0F);

        RenderSystem.setShaderTexture(0, textureId);

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        float x = (float)catX - CAT_SIZE/2;
        float y = (float)catY - CAT_SIZE/2;

        bufferBuilder.addVertex(matrix, x, y + CAT_SIZE, 0).setUv(0, 1);
        bufferBuilder.addVertex(matrix, x + CAT_SIZE, y + CAT_SIZE, 0).setUv(1, 1);
        bufferBuilder.addVertex(matrix, x + CAT_SIZE, y, 0).setUv(1, 0);
        bufferBuilder.addVertex(matrix, x, y, 0).setUv(0, 0);

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public void cleanup() {
        textures.values().forEach(GlStateManager::_deleteTexture);
        textures.clear();
    }
}