package me.wethink.weFriends.managers;

import me.wethink.weFriends.WeFriends;
import me.wethink.weFriends.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PartyManager {
    private final WeFriends plugin;
    private final DatabaseManager db;
    private final Map<UUID, String> pendingInvites = new HashMap<>();

    public PartyManager(WeFriends plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public void createParty(Player leader) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            UUID partyId = UUID.randomUUID();

            db.createParty(partyId, leader.getUniqueId());
            db.addPartyMember(partyId, leader.getUniqueId(), "LEADER");

            WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                MessageUtil.send(leader, "party.created");
            });
        });
    }

    public void invite(Player leader, String targetName) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            String partyId = getPartyId(leader.getUniqueId());
            if (partyId == null) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                    MessageUtil.send(leader, "party.not_in_party");
                });
                return;
            }
            if (!isLeader(leader.getUniqueId(), partyId)) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                    MessageUtil.send(leader, "party.not_leader");
                });
                return;
            }
            UUID targetUuid = resolveUuid(targetName);
            if (targetUuid == null) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                    MessageUtil.send(leader, "error.player_not_found", Map.of("player", targetName));
                });
                return;
            }
            if (getPartyId(targetUuid) != null) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                    MessageUtil.send(leader, "party.target_in_party", Map.of("player", targetName));
                });
                return;
            }
            if (!plugin.getFriendManager().areFriends(leader.getUniqueId(), targetUuid)) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                    MessageUtil.send(leader, "party.not_friend", Map.of("player", targetName));
                });
                return;
            }

            db.addPartyInvite(UUID.fromString(partyId), leader.getUniqueId(), targetUuid, System.currentTimeMillis());

            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(target, (entityTask) -> {
                    MessageUtil.send(target, "party.invite_received", Map.of("player", leader.getName()));
                    target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                });
            }
            WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                MessageUtil.send(leader, "party.invite_sent", Map.of("player", targetName));
            });
            
            if (plugin.getRedisManager().isEnabled() && plugin.getRedisManager().isCrossServerEnabled()) {
                plugin.getRedisManager().publishPartyInvite(leader.getUniqueId(), leader.getName(), targetName);
            }
        });
    }

    public void accept(Player player, String leaderName) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            UUID leaderUuid = resolveUuid(leaderName);
            if (leaderUuid == null) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(player, (entityTask) -> {
                    MessageUtil.send(player, "error.player_not_found", Map.of("player", leaderName));
                });
                return;
            }
            String partyId = getPartyId(leaderUuid);
            if (partyId == null) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(player, (entityTask) -> {
                    MessageUtil.send(player, "party.no_invite", Map.of("player", leaderName));
                });
                return;
            }

            db.addPartyMember(UUID.fromString(partyId), player.getUniqueId(), "MEMBER");
            db.removePartyInvite(player.getUniqueId());

            pendingInvites.remove(player.getUniqueId());
            WeFriends.getFoliaLib().getImpl().runAtEntity(player, (entityTask) -> {
                MessageUtil.send(player, "party.joined");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            });

            List<UUID> partyMembers = getPartyMembers(partyId);
            for (UUID memberUuid : partyMembers) {
                if (!memberUuid.equals(player.getUniqueId())) {
                    Player member = Bukkit.getPlayer(memberUuid);
                    if (member != null) {
                        WeFriends.getFoliaLib().getImpl().runAtEntity(member, (entityTask) -> {
                            MessageUtil.send(member, "party.member_joined", Map.of("player", player.getName()));
                        });
                    }
                }
            }
            
            if (plugin.getRedisManager().isEnabled() && plugin.getRedisManager().isCrossServerEnabled()) {
                plugin.getRedisManager().publishPartyJoin(player.getUniqueId(), player.getName(), partyId);
            }
        });
    }

    public void deny(Player player, String leaderName) {
        pendingInvites.remove(player.getUniqueId());
        MessageUtil.send(player, "party.denied");
    }

    public void leave(Player player) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            String partyId = getPartyId(player.getUniqueId());
            if (partyId == null) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(player, (entityTask) -> {
                    MessageUtil.send(player, "party.not_in_party");
                });
                return;
            }

            db.removePartyMember(UUID.fromString(partyId), player.getUniqueId());

            WeFriends.getFoliaLib().getImpl().runAtEntity(player, (entityTask) -> {
                MessageUtil.send(player, "party.left");
            });
            
            if (plugin.getRedisManager().isEnabled() && plugin.getRedisManager().isCrossServerEnabled()) {
                plugin.getRedisManager().publishPartyLeave(player.getUniqueId(), player.getName(), partyId);
            }
        });
    }

    public void kick(Player leader, String memberName) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            String partyId = getPartyId(leader.getUniqueId());
            if (partyId == null || !isLeader(leader.getUniqueId(), partyId)) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                    MessageUtil.send(leader, "party.not_leader");
                });
                return;
            }
            UUID member = resolveUuid(memberName);
            if (member == null) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                    MessageUtil.send(leader, "error.player_not_found", Map.of("player", memberName));
                });
                return;
            }

            db.removePartyMember(UUID.fromString(partyId), member);

            WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                MessageUtil.send(leader, "party.kicked", Map.of("player", memberName));
            });
            
            if (plugin.getRedisManager().isEnabled() && plugin.getRedisManager().isCrossServerEnabled()) {
                plugin.getRedisManager().publishPartyLeave(member, memberName, partyId);
            }
        });
    }

    public void promote(Player leader, String memberName) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            String partyId = getPartyId(leader.getUniqueId());
            if (partyId == null || !isLeader(leader.getUniqueId(), partyId)) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                    MessageUtil.send(leader, "party.not_leader");
                });
                return;
            }
            UUID member = resolveUuid(memberName);
            if (member == null) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                    MessageUtil.send(leader, "error.player_not_found", Map.of("player", memberName));
                });
                return;
            }

            db.updatePartyMemberRole(UUID.fromString(partyId), member, "LEADER");
            db.updatePartyLeader(UUID.fromString(partyId), member);

            WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                MessageUtil.send(leader, "party.promoted", Map.of("player", memberName));
            });
        });
    }

    public void transfer(Player leader, String memberName) {
        promote(leader, memberName);
    }

    public void disband(Player leader) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            String partyId = getPartyId(leader.getUniqueId());
            if (partyId == null || !isLeader(leader.getUniqueId(), partyId)) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                    MessageUtil.send(leader, "party.not_leader");
                });
                return;
            }

            List<UUID> members = getPartyMembers(partyId);
            for (UUID memberUuid : members) {
                db.removePartyMember(UUID.fromString(partyId), memberUuid);
            }
            db.deleteParty(UUID.fromString(partyId));

            WeFriends.getFoliaLib().getImpl().runAtEntity(leader, (entityTask) -> {
                MessageUtil.send(leader, "party.disbanded");
            });
        });
    }

    public void sendPartyChat(Player sender, String message) {
        WeFriends.getFoliaLib().getImpl().runAsync((task) -> {
            String partyId = getPartyId(sender.getUniqueId());
            if (partyId == null) {
                WeFriends.getFoliaLib().getImpl().runAtEntity(sender, (entityTask) -> {
                    MessageUtil.send(sender, "party.not_in_party");
                });
                return;
            }
            List<UUID> members = getPartyMembers(partyId);
            WeFriends.getFoliaLib().getImpl().runAtEntity(sender, (entityTask) -> {
                for (UUID m : members) {
                    Player p = Bukkit.getPlayer(m);
                    if (p != null)
                        MessageUtil.send(p, "chat.party_format", Map.of("player", sender.getName(), "message", message));
                }
                plugin.getSpyManager().broadcastPartySpy(sender, message);
            });
            
            if (plugin.getRedisManager().isEnabled() && plugin.getRedisManager().isCrossServerEnabled()) {
                plugin.getRedisManager().publishPartyChat(sender.getUniqueId(), sender.getName(), partyId, message);
            }
        });
    }

    private String getPartyId(UUID memberUuid) {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT party_id FROM party_members WHERE player_uuid=?")) {
            ps.setString(1, memberUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            plugin.getLogger().warning("getPartyId error: " + e.getMessage());
        }
        return null;
    }

    private boolean isLeader(UUID uuid, String partyId) {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT 1 FROM parties WHERE id=? AND leader_uuid=?")) {
            ps.setString(1, partyId);
            ps.setString(2, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().warning("isLeader error: " + e.getMessage());
            return false;
        }
    }

    private List<UUID> getMembers(String partyId) {
        List<UUID> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT player_uuid FROM party_members WHERE party_id=?")) {
            ps.setString(1, partyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(UUID.fromString(rs.getString(1)));
        } catch (SQLException e) {
            plugin.getLogger().warning("getMembers error: " + e.getMessage());
        }
        return list;
    }

    private UUID resolveUuid(String name) {
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(name);
        if (op == null) op = Bukkit.getOfflinePlayer(name);
        return op.getUniqueId();
    }

    public void shutdown() {
    }

    public String getPartyIdPublic(UUID memberUuid) {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT party_id FROM party_members WHERE player_uuid=?")) {
            ps.setString(1, memberUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            plugin.getLogger().warning("getPartyId error: " + e.getMessage());
        }
        return null;
    }

    public UUID getPartyLeader(String partyId) {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT leader_uuid FROM parties WHERE id=?")) {
            ps.setString(1, partyId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return UUID.fromString(rs.getString(1));
        } catch (SQLException e) {
            plugin.getLogger().warning("getPartyLeader error: " + e.getMessage());
        }
        return null;
    }

    public List<UUID> getPartyMembers(String partyId) {
        List<UUID> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT player_uuid FROM party_members WHERE party_id=?")) {
            ps.setString(1, partyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(UUID.fromString(rs.getString(1)));
        } catch (SQLException e) {
            plugin.getLogger().warning("getMembers error: " + e.getMessage());
        }
        return list;
    }

    public String getPartyRole(UUID memberUuid, String partyId) {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT role FROM party_members WHERE party_id=? AND player_uuid=?")) {
            ps.setString(1, partyId);
            ps.setString(2, memberUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            plugin.getLogger().warning("getPartyRole error: " + e.getMessage());
        }
        return "MEMBER";
    }
}
