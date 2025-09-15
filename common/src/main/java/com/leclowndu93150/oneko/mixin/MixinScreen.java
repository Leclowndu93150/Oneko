package com.leclowndu93150.oneko.mixin;

import com.leclowndu93150.oneko.client.OnekoOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {
    
    @Inject(method = "renderWithTooltip", at = @At("TAIL"))
    private void onRenderWithTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        OnekoOverlay.getInstance().render(guiGraphics);
    }
}