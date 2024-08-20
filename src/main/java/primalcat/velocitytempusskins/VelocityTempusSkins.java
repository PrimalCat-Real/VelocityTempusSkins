package primalcat.velocitytempusskins;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.connections.MineSkinAPI;
import net.skinsrestorer.api.connections.model.MineSkinResponse;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import net.skinsrestorer.api.storage.SkinStorage;
import org.slf4j.Logger;

import java.util.Optional;

@Plugin(
        id = "velocitytempusskins",
        name = "VelocityTempusSkins",
        version = "1.0-SNAPSHOT"
)
public class VelocityTempusSkins {
    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.from("tempusskins:main");
    @Inject
    private Logger logger;
    private final ProxyServer proxyServer;

    @Inject
    public VelocityTempusSkins(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.proxyServer.getChannelRegistrar().register(new ChannelIdentifier[]{IDENTIFIER});
        this.logger.info("Plugin messaging started");
    }

    @Subscribe
    public void onPluginMessageFromPlayer(PluginMessageEvent event) {
        if (event.getSource() instanceof Player) {
            Player player = (Player)event.getSource();
            if (event.getIdentifier() == IDENTIFIER) {
                ByteArrayDataInput ou = ByteStreams.newDataInput(event.getData());
            }
        }
    }

    @Subscribe
    public void onPluginMessageFromBackend(PluginMessageEvent event) {
        if (event.getSource() instanceof ServerConnection) {
            ServerConnection backend = (ServerConnection)event.getSource();
            if (event.getIdentifier() == IDENTIFIER) {
                ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
                String channel = in.readUTF();
                String target = in.readUTF();
                String subchannel = in.readUTF();
                String skinValue;
                Optional property;
                if (subchannel.equals("SecretInternalChannel")) {
                    String skinName = in.readUTF();
                    skinValue = in.readUTF();
                    SkinStorage skinStorage = SkinsRestorerProvider.get().getSkinStorage();
                    skinStorage.setCustomSkinData(skinName, SkinProperty.of(skinValue, skinValue));

                    try {
                        property = skinStorage.findOrCreateSkinData(skinName);
                        if (property.isPresent()) {
                            PlayerStorage playerStorage = SkinsRestorerProvider.get().getPlayerStorage();
                            playerStorage.setSkinIdOfPlayer(((ServerConnection)event.getSource()).getPlayer().getUniqueId(), ((InputDataResult)property.get()).getIdentifier());
                            Player player = ((ServerConnection)event.getSource()).getPlayer();
                            SkinsRestorerProvider.get().getSkinApplier(Player.class).applySkin(player);
                        }
                    } catch (Exception var19) {
                        Exception e = var19;
                        System.out.println(e);
                    }
                }

                if (subchannel.equals("AddSkinChannel")) {
                    PlayerStorage playerStorage = SkinsRestorerProvider.get().getPlayerStorage();
                    Player player = ((ServerConnection)event.getSource()).getPlayer();

                    try {
                        property = playerStorage.getSkinForPlayer(player.getUniqueId(), player.getUsername());
                        if (!property.isPresent()) {
                            player.sendMessage(Component.text("§4Для сохранения скина вам нужно его установить!"));
                            return;
                        }

                        String textureUrl = PropertyUtils.getSkinTextureUrl((SkinProperty)property.get());
                        MineSkinAPI mineSkinAPI = SkinsRestorerProvider.get().getMineSkinAPI();
                        MineSkinResponse response = mineSkinAPI.genSkin(textureUrl, PropertyUtils.getSkinVariant((SkinProperty)property.get()));
                        SkinProperty skinProperty = response.getProperty();
                        skinValue = skinProperty.getValue();
                        String skinSignature = skinProperty.getSignature();
                        ByteArrayDataOutput outputStream = ByteStreams.newDataOutput();
                        outputStream.writeUTF("Forward");
                        outputStream.writeUTF("ALL");
                        outputStream.writeUTF("AddSkinChannel");
                        outputStream.writeUTF(skinValue);
                        outputStream.writeUTF(skinSignature);
                        Optional<ServerConnection> connection = ((ServerConnection)event.getSource()).getPlayer().getCurrentServer();
                        connection.ifPresent((serverConnection) -> {
                            serverConnection.sendPluginMessage(IDENTIFIER, outputStream.toByteArray());
                        });
                    } catch (Exception var18) {
                        player.sendMessage(Component.text("§4Для сохранения скина вам нужно его установить!"));
                        return;
                    }
                }

            }
        }
    }
}
