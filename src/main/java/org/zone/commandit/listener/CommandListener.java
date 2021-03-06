package org.zone.commandit.listener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.gravitydevelopment.updater.Updater;

import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.zone.commandit.CommandIt;
import org.zone.commandit.config.CommandBlocks;
import org.zone.commandit.io.FileAdapter;
import org.zone.commandit.io.FileConverter;
import org.zone.commandit.io.SqlAdapter;
import org.zone.commandit.util.Code;
import org.zone.commandit.util.Message;
import org.zone.commandit.util.PlayerState;

public class CommandListener implements CommandExecutor {
    
    private CommandIt plugin;
    
    public CommandListener(CommandIt plugin) {
        this.plugin = plugin;
    }
    
    protected boolean add(final CommandSender sender, Player player, int lineNumber, String[] args) {
        if (player == null) {
            Message.sendMessage(sender, "failure.player_only");
        }
        if (plugin.hasPermission(player, "commandit.create.regular")) {
            clipboard(sender, player, lineNumber, 1, args);
            if (plugin.getPlayerStates().get(player) != PlayerState.EDIT) {
                plugin.getPlayerStates().put(player, PlayerState.ENABLE);
                Message.sendMessage(player, "progress.add");
            }
        } else {
            Message.sendMessage(player, "failure.no_perms");
        }
        return true;
    }
    
    protected boolean batch(final CommandSender sender, Player player, String[] args) {
        PlayerState ps = plugin.getPlayerStates().get(player);
        if (ps == null) {
            Message.sendMessage(player, "failure.not_in_mode");
            return false;
        }
        switch (ps) {
            case REMOVE:
                player.sendMessage("Switched to batch remove mode.");
                ps = PlayerState.BATCH_REMOVE;
                break;
            case BATCH_REMOVE:
                player.sendMessage("Switched to single remove mode.");
                ps = PlayerState.REMOVE;
                break;
            case ENABLE:
                player.sendMessage("Switched to batch enable mode.");
                ps = PlayerState.BATCH_ENABLE;
                break;
            case BATCH_ENABLE:
                player.sendMessage("Switched to single enable mode.");
                ps = PlayerState.ENABLE;
                break;
            case READ:
                player.sendMessage("Switched to batch read mode.");
                ps = PlayerState.BATCH_READ;
                break;
            case BATCH_READ:
                player.sendMessage("Switched to single read mode.");
                ps = PlayerState.READ;
                break;
            case TOGGLE:
                player.sendMessage("Switched to batch toggle mode.");
                ps = PlayerState.BATCH_TOGGLE;
                break;
            case BATCH_TOGGLE:
                player.sendMessage("Switched to single toggle mode.");
                ps = PlayerState.TOGGLE;
                break;
            case REDSTONE:
                player.sendMessage("Switched to batch redstone mode.");
                ps = PlayerState.BATCH_REDSTONE;
                break;
            case BATCH_REDSTONE:
                player.sendMessage("Switched to single redstone mode.");
                ps = PlayerState.REDSTONE;
                break;
            default:
                Message.sendMessage(player, "failure.no_batch");
        }
        plugin.getPlayerStates().put(player, ps);
        return true;
    }
    
    protected boolean clear(final CommandSender sender, Player player, String[] args) {
        if (player == null) {
            Message.sendMessage(sender, "failure.player_only");
        }
        if (plugin.hasPermission(player, "commandit.remove")) {
            PlayerState ps = plugin.getPlayerStates().get(player);
            if (ps == PlayerState.EDIT || ps == PlayerState.EDIT_SELECT) {
                finishEditing(player);
            }
            plugin.getPlayerStates().remove(player);
            plugin.getPlayerCode().remove(player);
            Message.sendMessage(player, "success.cleared");
        } else {
            Message.sendMessage(player, "failure.no_perms");
        }
        return true;
    }
    
    private void clipboard(final CommandSender sender, Player player, int lineNumber, int textStart, String[] args) {
        if (lineNumber < 1) {
            Message.sendMessage(player, "failure.invalid_line");
        } else {
            if (plugin.getPlayerStates().get(player) == PlayerState.EDIT_SELECT) {
                Message.sendMessage(player, "failure.must_select");
            }
            Code text = plugin.getPlayerCode().get(player);
            if (text == null) {
                text = new Code(player.getName());
                plugin.getPlayerCode().put(player, text);
            }
            String line = StringUtils.join(args, " ", textStart, args.length);
            if (line.startsWith("/*") && !plugin.hasPermission(player, "commandit.create.super", false)) {
                Message.sendMessage(player, "failure.no_super");
            }
            if ((line.startsWith("/^") || line.startsWith("/#")) && !plugin.hasPermission(player, "commandit.create.op", false)) {
                Message.sendMessage(player, "failure.no_op");
            }
            text.setLine(lineNumber, line);
            Message.sendRaw(player, "success.line_print", "" + lineNumber, line);
        }
    }
    
    protected boolean copy(final CommandSender sender, Player player, String[] args) {
        if (player == null) {
            Message.sendMessage(sender, "failure.player_only");
        }
        if (plugin.hasPermission(player, "commandit.create.regular")) {
            PlayerState ps = plugin.getPlayerStates().get(player);
            if (ps == PlayerState.EDIT || ps == PlayerState.EDIT_SELECT) {
                finishEditing(player);
            }
            plugin.getPlayerStates().put(player, PlayerState.COPY);
            Message.sendMessage(player, "progress.copy");
        } else {
            Message.sendMessage(player, "failure.no_perms");
        }
        return true;
    }
    
    protected boolean edit(final CommandSender sender, Player player, String[] args) {
        if (plugin.hasPermission(sender, "commandit.edit", false)) {
            PlayerState ps = plugin.getPlayerStates().get(player);
            if (ps == PlayerState.EDIT_SELECT || ps == PlayerState.EDIT) {
                finishEditing(player);
            } else {
                plugin.getPlayerStates().put(player, PlayerState.EDIT_SELECT);
                plugin.getPlayerCode().remove(player);
                Message.sendMessage(player, "progress.select_sign");
            }
        }
        return true;
    }
    
    public void finishEditing(Player player) {
        plugin.getPlayerStates().remove(player);
        plugin.getPlayerCode().remove(player);
        Message.sendMessage(player, "success.done_editing");
    }
    
    public boolean importData(final CommandSender sender, Player player, String[] args) {
        if (plugin.hasPermission(sender, "commandit.import", false)) {
            String source = "null";
            try {
                source = args[1];
                if (source.equals("database")) {
                    // Source is database
                    CommandBlocks blocks = new CommandBlocks(new SqlAdapter(plugin));
                    plugin.getCommandBlocks().putAll(blocks);
                } else if (source.equals("old")) {
                    // Source is old signs.yml file
                    CommandBlocks blocks = new CommandBlocks(new FileConverter(plugin, "signs.yml"));
                    plugin.getCommandBlocks().putAll(blocks);
                } else if (source.endsWith(".yml")) {
                    // Source is a YAML file
                    CommandBlocks blocks = new CommandBlocks(new FileAdapter(plugin, source));
                    plugin.getCommandBlocks().putAll(blocks);
                } else {
                    throw new IllegalArgumentException("Unknown source type.");
                }
            } catch (Exception ex) {
                Message.sendMessage(player, "failure.import_fail", source);
                return true;
            }
            Message.sendMessage(player, "success.import_success");
        } else {
            Message.sendMessage(player, "failure.no_perms");
        }
        return true;
    }
    
    protected boolean insert(final CommandSender sender, Player player, int lineNumber, String[] args) {
        if (player == null) {
            Message.sendMessage(sender, "failure.player_only");
        }
        if (plugin.hasPermission(player, "commandit.create.regular")) {
            clipboard(sender, player, lineNumber, 2, args);
            if (plugin.getPlayerStates().get(player) != PlayerState.EDIT) {
                plugin.getPlayerStates().put(player, PlayerState.INSERT);
                Message.sendMessage(player, "progress.add");
            }
        } else {
            Message.sendMessage(player, "failure.no_perms");
        }
        return true;
    }
    
    @Override
    public boolean onCommand(final CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("CommandIt")) {
            if (args.length < 1 || args[0].equalsIgnoreCase("help")) {
                // Messaging.sendMessage(sender, "usage");
                return false;
            }
            Player tp = null;
            if (sender instanceof Player) {
                tp = (Player) sender;
            }
            final Player player = tp;
            String command = args[0].toLowerCase();
            Pattern pattern = Pattern.compile("(line|l)?(\\d+)");
            Matcher matcher = pattern.matcher(command);
            if (matcher.matches()) {
                return add(sender, player, Integer.parseInt(matcher.group(2)), args);
            } else if (command.equals("batch")) {
                return batch(sender, player, args);
            } else if (command.equals("clear")) {
                return clear(sender, player, args);
            } else if (command.equals("copy")) {
                return copy(sender, player, args);
            } else if (command.equals("edit")) {
                return edit(sender, player, args);
            } else if (command.equals("import")) {
                return importData(sender, player, args);
            } else if (command.equals("insert") && args.length > 1) {
                pattern = Pattern.compile("(line|l)?(\\d+)");
                matcher = pattern.matcher(args[1].toLowerCase());
                if (matcher.matches())
                    return insert(sender, player, Integer.parseInt(matcher.group(2)), args);
            } else if (command.equals("read")) {
                return read(sender, player, args);
            } else if (command.equals("redstone")) {
                return redstone(sender, player, args);
            } else if (command.equals("reload")) {
                return reload(sender, player, args);
            } else if (command.equals("remove")) {
                return remove(sender, player, args);
            } else if (command.equals("save")) {
                return save(sender, player, args);
            } else if (command.equals("toggle")) {
                return toggle(sender, player, args);
            } else if (command.equals("update")) {
                return update(sender, player, args);
            } else if (command.equals("view")) {
                return view(sender, player, args);
            } else {
                Message.sendMessage(sender, "failure.wrong_syntax");
                return true;
            }
        }
        return false;
    }
    
    protected boolean read(final CommandSender sender, Player player, String[] args) {
        if (player == null) {
            Message.sendMessage(sender, "failure.player_only");
        }
        if (plugin.hasPermission(player, "commandit.create.regular")) {
            PlayerState ps = plugin.getPlayerStates().get(player);
            if (ps == PlayerState.EDIT || ps == PlayerState.EDIT_SELECT) {
                finishEditing(player);
            }
            plugin.getPlayerStates().put(player, PlayerState.READ);
            Message.sendMessage(player, "progress.read");
        } else {
            Message.sendMessage(player, "failure.no_perms");
        }
        return true;
    }
    
    protected boolean redstone(final CommandSender sender, Player player, String[] args) {
        if (player == null) {
            Message.sendMessage(sender, "failure.player_only");
        }
        if (plugin.hasPermission(player, "commandit.create.redstone")) {
            PlayerState ps = plugin.getPlayerStates().get(player);
            if (ps == PlayerState.EDIT || ps == PlayerState.EDIT_SELECT) {
                finishEditing(player);
            }
            plugin.getPlayerStates().put(player, PlayerState.REDSTONE);
            Message.sendMessage(player, "progress.redstone");
        } else {
            Message.sendMessage(player, "failure.no_perms");
        }
        return true;
    }
    
    protected boolean reload(final CommandSender sender, Player player, String[] args) {
        if (plugin.hasPermission(sender, "commandit.reload", false)) {
            plugin.load();
            Message.sendMessage(sender, "success.reloaded");
        } else {
            Message.sendMessage(player, "failure.no_perms");
        }
        return true;
    }
    
    protected boolean remove(final CommandSender sender, Player player, String[] args) {
        if (player == null) {
            Message.sendMessage(sender, "failure.player_only");
        }
        if (plugin.hasPermission(player, "commandit.remove")) {
            PlayerState ps = plugin.getPlayerStates().get(player);
            if (ps == PlayerState.EDIT || ps == PlayerState.EDIT_SELECT) {
                finishEditing(player);
            }
            plugin.getPlayerStates().put(player, PlayerState.REMOVE);
            Message.sendMessage(player, "progress.remove");
        } else {
            Message.sendMessage(player, "failure.no_perms");
        }
        return true;
    }
    
    protected boolean save(final CommandSender sender, Player player, String[] args) {
        if (plugin.hasPermission(sender, "commandit.save", false)) {
            plugin.getCommandBlocks().save();
            Message.sendMessage(sender, "success.saved");
        }
        return true;
    }
    
    protected boolean toggle(final CommandSender sender, Player player, String[] args) {
        if (player == null) {
            Message.sendMessage(sender, "failure.player_only");
        }
        if (plugin.hasPermission(player, "commandit.toggle")) {
            PlayerState ps = plugin.getPlayerStates().get(player);
            if (ps == PlayerState.EDIT || ps == PlayerState.EDIT_SELECT) {
                finishEditing(player);
            }
            plugin.getPlayerStates().put(player, PlayerState.TOGGLE);
            Message.sendMessage(player, "progress.toggle");
        } else {
            Message.sendMessage(player, "failure.no_perms");
        }
        return true;
    }
    
    protected boolean update(final CommandSender sender, Player player, String[] args) {
        if (plugin.hasPermission(sender, "commandit.update")) {
            Updater updater = plugin.getUpdater();
            if (updater == null) updater = new Updater(plugin, plugin.getBukkitId(), plugin.getFile(), Updater.UpdateType.NO_DOWNLOAD, false);
            
            if (updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE) {
                if (args.length > 0 && args[0].equals("check")) {
                    
                    // Check only
                    Message.sendMessage(player, "update.notify", updater.getLatestName());
                    
                } else {
                    
                    // Update
                    Message.sendMessage(player, "update.start");
                    double time = System.currentTimeMillis();
                    Updater downloader = new Updater(plugin, plugin.getBukkitId(), plugin.getFile(), Updater.UpdateType.NO_VERSION_CHECK, false);
                    switch(downloader.getResult()) {
                        case SUCCESS:
                            Message.sendMessage(player, "update.finish", downloader.getLatestName(), (System.currentTimeMillis() - time) / 1000);
                            break;
                        case NO_UPDATE:
                            Message.sendMessage(player, "update.up_to_date", downloader.getLatestName());
                            break;
                        case DISABLED:
                            Message.sendMessage(player, "update.fetch_error", "updater is disabled in configuration (plugins/Updater/config.yml).");
                            break;
                        case FAIL_DOWNLOAD:
                            Message.sendMessage(player, "update.fetch_error", "failed to download.");
                            break;
                        case FAIL_DBO:
                            Message.sendMessage(player, "update.fetch_error", "unable to contact Bukkit Dev at this time.");
                            break;
                        case FAIL_NOVERSION:
                            Message.sendMessage(player, "update.fetch_error", "unable to check latest version.");
                            break;
                        case FAIL_BADID:
                            Message.sendMessage(player, "update.fetch_error", "the plugin was not found on Bukkit Dev!");
                            break;
                        case FAIL_APIKEY:
                            Message.sendMessage(player, "update.fetch_error", "the API key provided is invalid (plugins/Updater/config.yml).");
                            break;
                        default:
                            Message.sendMessage(player, "update.fetch_error", "I have no idea what just happened.");
                            break;
                    }
                }
            } else {
                Message.sendMessage(player, "update.up_to_date", updater.getLatestName());
            }
        }
        return true;
    }
    
    protected boolean view(final CommandSender sender, Player player, String[] args) {
        if (player == null) {
            Message.sendMessage(sender, "failure.player_only");
        }
        if (plugin.hasPermission(player, "commandit.create.regular")) {
            Code text = plugin.getPlayerCode().get(player);
            if (text == null) {
                player.sendMessage("No text in clipboard");
            } else {
                int i = 1;
                for (String s : text) {
                    if (!s.equals("")) {
                        player.sendMessage(i + ": " + s);
                    }
                    i++;
                }
            }
            plugin.getPlayerStates().remove(player);
        } else {
            Message.sendMessage(player, "failure.no_perms");
        }
        return true;
    }
    
}
