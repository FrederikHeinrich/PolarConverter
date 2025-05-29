package net.uebliche.polarconverter.mixin;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.hollowcube.polar.AnvilPolar;
import net.hollowcube.polar.ChunkSelector;
import net.hollowcube.polar.PolarWorld;
import net.hollowcube.polar.PolarWriter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import net.minestom.server.MinecraftServer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(EditWorldScreen.class)
public class EditWorldScreenMixin extends Screen {



    private static final Logger LOGGER = LogUtils.getLogger();
    @Shadow
    @Final
    private DirectionalLayoutWidget layout;
    @Shadow
    @Final
    private LevelStorage.Session storageSession;
    protected EditWorldScreenMixin() {
        super(Text.empty());
    }
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/DirectionalLayoutWidget;add(Lnet/minecraft/client/gui/widget/Widget;)Lnet/minecraft/client/gui/widget/Widget;", ordinal = 9))
    private void addConvertToPolarButton(MinecraftClient client, LevelStorage.Session session, String levelName, BooleanConsumer callback, CallbackInfo ci){
        layout.add(ButtonWidget.builder(Text.literal("Convert to Polar"), button -> {
            button.setMessage(Text.literal("Processing..."));
            button.active = false;
            new Thread(()-> {

                MinecraftServer.init();
                if(convertToPolar(session.getDirectory().path(), Path.of(session.getDirectory().path().toString(), session.getDirectoryName()+".polar"))){
                    MinecraftClient.getInstance().submit(() -> MinecraftClient.getInstance().getToastManager().add(new SystemToast(SystemToast.Type.WORLD_BACKUP, Text.of("Polar Converter Finished!!"), Text.of("World Folder Opened..."))));
                    Util.getOperatingSystem().open(session.getDirectory(WorldSavePath.ROOT).toFile());
                }else{
                    MinecraftClient.getInstance().submit(() -> MinecraftClient.getInstance().getToastManager().add(new SystemToast(SystemToast.Type.WORLD_BACKUP, Text.of("Polar Converter FAILED!!"), Text.of("please check console."))));
                }
                MinecraftServer.stopCleanly();
                button.setMessage(Text.literal("Convert to Polar"));
                button.active = true;
            }).start();
        }).width(200).build());

    }

    @Unique
    public Boolean convertToPolar(Path anvilPath, Path outputPath) {
        PolarWorld polarWorld;
        try {
            polarWorld = AnvilPolar.anvilToPolar(anvilPath, ChunkSelector.all());
        } catch (IOException e) {
            LOGGER.warn("Fail to read world", e);
            return false;
        }
        var result = PolarWriter.write(polarWorld);
        try {
            Files.write(outputPath, result);
            return true;
        } catch (IOException e) {
            LOGGER.warn("Fail to save world", e);
            return false;
        }
    }
}
