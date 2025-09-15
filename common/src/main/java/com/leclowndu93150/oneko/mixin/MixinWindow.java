package com.leclowndu93150.oneko.mixin;

import com.leclowndu93150.oneko.client.OnekoOverlay;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Window.class, remap = false)
public class MixinWindow {
    
    @Inject(method = "updateDisplay", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;flipFrame(J)V", shift = At.Shift.BEFORE))
    private void onUpdateDisplay(CallbackInfo ci) {
        OnekoOverlay.getInstance().render();
    }
}