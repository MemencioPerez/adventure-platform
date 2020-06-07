/*
 * This file is part of text-extras, licensed under the MIT License.
 *
 * Copyright (c) 2018 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.kyori.adventure.platform.bukkit;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.AdventurePlatform;
import net.kyori.adventure.platform.PlatformAudience;
import net.kyori.adventure.platform.impl.HandledAudience;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.platform.impl.HandlerCollection;
import net.kyori.adventure.platform.impl.VersionedGsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

// TODO: implement Listener and use singletons
public final class BukkitPlatform implements AdventurePlatform {

  // A derivative of the Gson serializer that will serialize text appropriately based on the server version
  /* package */ static final VersionedGsonComponentSerializer GSON_SERIALIZER;

  static {
    if(Crafty.enumValue(Material.class, "NETHERITE_PICKAXE", Integer.MAX_VALUE) != null) { // we are 1.16
      GSON_SERIALIZER = VersionedGsonComponentSerializer.MODERN;
    } else {
      GSON_SERIALIZER = VersionedGsonComponentSerializer.PRE_1_16;
    }
  }

  // Type handlers

  static HandlerCollection<? super CommandSender, ? extends Handler.Chat<? super CommandSender, ?>> CHAT = new HandlerCollection<>(new SpigotHandlers.Chat(),
    new CraftBukkitHandlers.Chat(), new BukkitHandlers.Chat());
  static HandlerCollection<Player, Handler.ActionBar<Player, ?>> ACTION_BAR = new HandlerCollection<>(new SpigotHandlers.ActionBar(),
    new CraftBukkitHandlers.ActionBarModern(), new CraftBukkitHandlers.ActionBar1_8thru1_11());
  static HandlerCollection<Player, Handler.Title<Player>> TITLE = new HandlerCollection<>(new PaperHandlers.Title(), new CraftBukkitHandlers.Title());
  static HandlerCollection<Player, Handler.BossBar<Player>> BOSS_BAR = new HandlerCollection<>(new BukkitHandlers.BossBar());
  static HandlerCollection<Player, Handler.PlaySound<Player>> PLAY_SOUND = new HandlerCollection<>(new BukkitHandlers.PlaySound_WithCategory(),
    new BukkitHandlers.PlaySound_NoCategory());

  private final Server server;
  private final Audience console;
  private final Audience players;

  public BukkitPlatform() {
    this(Bukkit.getServer());
  }

  public BukkitPlatform(final @NonNull Server server) {
    this.server = requireNonNull(server, "bukkit server");
    this.console = audience(server.getConsoleSender());
    this.players = new PlayersAudience(server);
  }

  // TODO: ugly but it's here to test with until proper solution
  public static PlatformAudience<Player> audience(final @NonNull Player player) {
    return new HandledAudience<>(requireNonNull(player, "player"), CHAT, ACTION_BAR, TITLE, BOSS_BAR, PLAY_SOUND);
  }

  public static PlatformAudience<? extends CommandSender> audience(final @NonNull CommandSender sender) {
    requireNonNull(sender, "sender");
    if(sender instanceof Player) {
      return audience((Player) sender);
    } else {
      return new HandledAudience<>(sender, CHAT, null, null, null, null);
    }
  }

  @Override
  public @NonNull String name() {
    return this.server.getBukkitVersion();
  }

  @Override
  public @NonNull Audience console() {
    return this.console;
  }

  @Override
  public @NonNull Audience players() {
    return this.players;
  }

  @Override
  public @NonNull Audience player(final @NonNull UUID playerId) {
    final Player player = this.server.getPlayer(playerId);
    if (player == null) return Audience.empty();
    return audience(player);
  }

  @Override
  public @NonNull Audience permission(final @NonNull String permission) {
    return new PermissibleAudience(this.server, permission);
  }

  @Override
  public @NonNull Audience world(final @NonNull UUID worldId) {
    return new WorldAudience(this.server, worldId);
  }
}
