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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (getPartyId(leader.getUniqueId()) != null) {
                MessageUtil.send(leader, "party.already_in");
                return;
            }
            String partyId = UUID.randomUUID().toString();
            try (Connection c = db.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO parties(party_id,leader_uuid) VALUES(?,?)")) {
                    ps.setString(1, partyId);
                    ps.setString(2, leader.getUniqueId().toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO party_members(party_id,member_uuid,role) VALUES(?,?,?)")) {
                    ps.setString(1, partyId);
                    ps.setString(2, leader.getUniqueId().toString());
                    ps.setString(3, "LEADER");
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("createParty error: " + e.getMessage());
            }
            MessageUtil.send(leader, "party.created");
        });
    }

    public void invite(Player leader, String targetName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String partyId = getPartyId(leader.getUniqueId());
            if (partyId == null || !isLeader(leader.getUniqueId(), partyId)) {
                MessageUtil.send(leader, "party.not_leader");
                return;
            }
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                MessageUtil.send(leader, "error.player_not_found", Map.of("player", targetName));
                return;
            }
            if (getPartyId(target.getUniqueId()) != null) {
                MessageUtil.send(leader, "party.target_in_party", Map.of("player", target.getName()));
                return;
            }
            // Check if target is a friend
            if (!plugin.getFriendManager().areFriends(leader.getUniqueId(), target.getUniqueId())) {
                MessageUtil.send(leader, "party.not_friend", Map.of("player", target.getName()));
                return;
            }
            MessageUtil.actionbar(target, "actionbar.party_invite", Map.of("player", leader.getName()));
            target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            pendingInvites.put(target.getUniqueId(), partyId);
            MessageUtil.send(leader, "party.invited", Map.of("player", target.getName()));
        });
    }

    public void accept(Player player, String leaderName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String partyId = pendingInvites.get(player.getUniqueId());
            if (partyId == null) {
                MessageUtil.send(player, "party.no_invite");
                return;
            }
            try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO party_members(party_id,member_uuid,role) VALUES(?,?,?)")) {
                ps.setString(1, partyId);
                ps.setString(2, player.getUniqueId().toString());
                ps.setString(3, "MEMBER");
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("accept invite error: " + e.getMessage());
            }
            pendingInvites.remove(player.getUniqueId());
            MessageUtil.send(player, "party.joined");
        });
    }

    public void deny(Player player, String leaderName) {
        pendingInvites.remove(player.getUniqueId());
        MessageUtil.send(player, "party.denied");
    }

    public void leave(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String partyId = getPartyId(player.getUniqueId());
            if (partyId == null) {
                MessageUtil.send(player, "party.not_in");
                return;
            }
            try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM party_members WHERE party_id=? AND member_uuid=?")) {
                ps.setString(1, partyId);
                ps.setString(2, player.getUniqueId().toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("leave party error: " + e.getMessage());
            }
            MessageUtil.send(player, "party.left");
        });
    }

    public void kick(Player leader, String memberName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String partyId = getPartyId(leader.getUniqueId());
            if (partyId == null || !isLeader(leader.getUniqueId(), partyId)) {
                MessageUtil.send(leader, "party.not_leader");
                return;
            }
            UUID member = resolveUuid(memberName);
            if (member == null) {
                MessageUtil.send(leader, "error.player_not_found", Map.of("player", memberName));
                return;
            }
            try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM party_members WHERE party_id=? AND member_uuid=?")) {
                ps.setString(1, partyId);
                ps.setString(2, member.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("kick error: " + e.getMessage());
            }
            MessageUtil.send(leader, "party.kicked", Map.of("player", memberName));
        });
    }

    public void promote(Player leader, String memberName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String partyId = getPartyId(leader.getUniqueId());
            if (partyId == null || !isLeader(leader.getUniqueId(), partyId)) {
                MessageUtil.send(leader, "party.not_leader");
                return;
            }
            UUID member = resolveUuid(memberName);
            if (member == null) {
                MessageUtil.send(leader, "error.player_not_found", Map.of("player", memberName));
                return;
            }
            try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE party_members SET role='LEADER' WHERE party_id=? AND member_uuid=?")) {
                ps.setString(1, partyId);
                ps.setString(2, member.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("promote error: " + e.getMessage());
            }
            try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE parties SET leader_uuid=? WHERE party_id=?")) {
                ps.setString(1, member.toString());
                ps.setString(2, partyId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("transfer leader error: " + e.getMessage());
            }
            MessageUtil.send(leader, "party.promoted", Map.of("player", memberName));
        });
    }

    public void transfer(Player leader, String memberName) {
        promote(leader, memberName);
    }

    public void disband(Player leader) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String partyId = getPartyId(leader.getUniqueId());
            if (partyId == null || !isLeader(leader.getUniqueId(), partyId)) {
                MessageUtil.send(leader, "party.not_leader");
                return;
            }
            try (Connection c = db.getConnection()) {
                c.createStatement().executeUpdate("DELETE FROM party_members WHERE party_id='" + partyId + "'");
                c.createStatement().executeUpdate("DELETE FROM parties WHERE party_id='" + partyId + "'");
            } catch (SQLException e) {
                plugin.getLogger().warning("disband error: " + e.getMessage());
            }
            MessageUtil.send(leader, "party.disbanded");
        });
    }

    public void sendPartyChat(Player sender, String message) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String partyId = getPartyId(sender.getUniqueId());
            if (partyId == null) {
                MessageUtil.send(sender, "party.not_in");
                return;
            }
            List<UUID> members = getMembers(partyId);
            for (UUID m : members) {
                Player p = Bukkit.getPlayer(m);
                if (p != null)
                    MessageUtil.send(p, "chat.party_format", Map.of("player", sender.getName(), "message", message));
            }
            plugin.getSpyManager().broadcastPartySpy(sender, message);
        });
    }

    private String getPartyId(UUID memberUuid) {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT party_id FROM party_members WHERE member_uuid=?")) {
            ps.setString(1, memberUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            plugin.getLogger().warning("getPartyId error: " + e.getMessage());
        }
        return null;
    }

    private boolean isLeader(UUID uuid, String partyId) {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT 1 FROM parties WHERE party_id=? AND leader_uuid=?")) {
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
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT member_uuid FROM party_members WHERE party_id=?")) {
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
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT party_id FROM party_members WHERE member_uuid=?")) {
            ps.setString(1, memberUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            plugin.getLogger().warning("getPartyId error: " + e.getMessage());
        }
        return null;
    }

    public UUID getPartyLeader(String partyId) {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT leader_uuid FROM parties WHERE party_id=?")) {
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
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT member_uuid FROM party_members WHERE party_id=?")) {
            ps.setString(1, partyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(UUID.fromString(rs.getString(1)));
        } catch (SQLException e) {
            plugin.getLogger().warning("getMembers error: " + e.getMessage());
        }
        return list;
    }

    public String getPartyRole(UUID memberUuid, String partyId) {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT role FROM party_members WHERE party_id=? AND member_uuid=?")) {
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


