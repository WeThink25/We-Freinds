# We-Friends

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Paper](https://img.shields.io/badge/Paper-1.21.1+-blue.svg)](https://papermc.io/)
[![Folia](https://img.shields.io/badge/Folia-Supported-green.svg)](https://github.com/PaperMC/Folia)

A comprehensive friend and party management system for Minecraft servers with cross-server support, built for Paper/Spigot and Folia.

## ✨ Features

### 🤝 Friend System
- **Friend Requests**: Send, accept, and deny friend requests
- **Friend List**: View online/offline friends with status indicators
- **Friend Chat**: Private messaging and group chat with friends
- **Join/Quit Notifications**: Get notified when friends join or leave
- **Request Toggle**: Enable/disable receiving friend requests
- **Cross-Server Support**: Friends can interact across multiple servers

### 🎉 Party System
- **Party Creation**: Create and manage parties with friends
- **Party Invites**: Invite friends to join your party
- **Party Chat**: Dedicated party chat channel
- **Party Roles**: Leader and member roles with permissions
- **Party Management**: Kick, promote, transfer leadership, and disband
- **Cross-Server Parties**: Party members can be on different servers

### 🌐 Cross-Server Support
- **Multi-Server Network**: Connect multiple servers with Redis
- **Real-time Sync**: Instant notifications across servers
- **Server Identification**: See which server friends/party members are on
- **Seamless Experience**: Chat and interact across the entire network

### 🔧 Advanced Features
- **Database Support**: SQLite and MySQL support
- **Redis Integration**: For cross-server communication
- **PlaceholderAPI**: Custom placeholders for other plugins
- **Folia Support**: Full compatibility with Folia servers
- **Chat Spy**: Staff can monitor friend/party chats
- **Chat Modes**: Toggle between normal, friend, and party chat

## 📋 Requirements

- **Java**: 17 or higher
- **Server Software**: Paper 1.21.1+ or Folia
- **Database**: SQLite (included) or MySQL
- **Redis**: Required for cross-server functionality (optional)

## 🚀 Installation

1. **Download** the latest release from [Releases](https://github.com/WeThink25/We-Friends/releases)
2. **Place** the JAR file in your server's `plugins` folder
3. **Start** your server to generate the configuration files
4. **Configure** the plugin (see Configuration section)
5. **Restart** your server

## ⚙️ Configuration

### Basic Setup (Single Server)

```yaml
# config.yml
database:
  type: sqlite # Use SQLite for single server

server:
  name: lobby # Your server name

cross-server:
  enabled: false # Disable for single server

redis:
  enabled: false # Not needed for single server
```

### Multi-Server Setup

```yaml
# config.yml
database:
  type: mysql # Required for multi-server
  mysql:
    host: localhost
    port: 3306
    database: wefriends
    username: root
    password: "your_password"

server:
  name: lobby # Unique name for each server

cross-server:
  enabled: true # Enable cross-server features
  sync-interval: 30
  notification-timeout: 5

redis:
  enabled: true # Required for cross-server
  host: localhost
  port: 6379
  password: ""
  channel: wefriends:events

limits:
  max-friends: 200
  max-party-size: 8
```

### Database Setup (MySQL)

```sql
CREATE DATABASE wefriends;
CREATE USER 'wefriends'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON wefriends.* TO 'wefriends'@'%';
FLUSH PRIVILEGES;
```

## 🎮 Commands

### Friend Commands
- `/friend add <player>` - Send a friend request
- `/friend accept <player>` - Accept a friend request
- `/friend deny <player>` - Deny a friend request
- `/friend remove <player>` - Remove a friend
- `/friend list` - View your friends list
- `/friend requests` - View pending requests
- `/friend toggle` - Toggle friend requests on/off
- `/fchat <message>` - Send a message to all friends
- `/fmsg <friend> <message>` - Send a private message to a friend

### Party Commands
- `/party create` - Create a new party
- `/party invite <player>` - Invite a friend to your party
- `/party accept <player>` - Accept a party invite
- `/party deny <player>` - Deny a party invite
- `/party leave` - Leave your current party
- `/party kick <player>` - Kick a member from the party (leader only)
- `/party promote <player>` - Promote a member to leader (leader only)
- `/party transfer <player>` - Transfer leadership (leader only)
- `/party disband` - Disband the party (leader only)
- `/pc <message>` - Send a message to party members

### Chat Mode Commands
- `/fchatmode` - Toggle friend chat mode
- `/pchatmode` - Toggle party chat mode
- `/chatmode` - View current chat mode

### Admin Commands
- `/fchatspy` - Toggle friend chat spy (permission required)
- `/partyspy` - Toggle party chat spy (permission required)
- `/wefriends reload` - Reload the plugin configuration

## 🔑 Permissions

### Player Permissions
- `wefriends.friend.use` - Use friend commands
- `wefriends.party.use` - Use party commands
- `wefriends.chat.friend` - Use friend chat
- `wefriends.chat.party` - Use party chat

### Admin Permissions
- `wefriends.admin` - Access admin commands
- `wefriends.spy.friend` - Spy on friend chats
- `wefriends.spy.party` - Spy on party chats
- `wefriends.reload` - Reload plugin configuration

## 📊 PlaceholderAPI

The plugin provides placeholders for use with PlaceholderAPI:

- `%wefriends_friends_online%` - Number of online friends
- `%wefriends_friends_total%` - Total number of friends
- `%wefriends_party_size%` - Current party size
- `%wefriends_party_leader%` - Party leader name
- `%wefriends_in_party%` - Whether player is in a party (true/false)

## 🔧 API Usage

### Maven Dependency
```xml
<dependency>
    <groupId>me.wethink</groupId>
    <artifactId>wefriends</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Basic API Examples

```java
// Get the plugin instance
WeFriends plugin = WeFriends.getInstance();

// Check if two players are friends
boolean areFriends = plugin.getFriendManager().areFriends(player1UUID, player2UUID);

// Get a player's friends
Set<UUID> friends = plugin.getFriendManager().getFriendUuidsPublic(playerUUID);

// Get party information
String partyId = plugin.getPartyManager().getPartyIdPublic(playerUUID);
List<UUID> partyMembers = plugin.getPartyManager().getPartyMembers(partyId);
```

## 🌐 Cross-Server Setup Guide

### Step 1: Database Configuration
Set up a shared MySQL database accessible by all servers:

```yaml
database:
  type: mysql
  mysql:
    host: your-database-host
    port: 3306
    database: wefriends_network
    username: wefriends_user
    password: secure_password
```

### Step 2: Redis Configuration
Configure Redis for real-time communication:

```yaml
redis:
  enabled: true
  host: your-redis-host
  port: 6379
  password: redis_password
  channel: wefriends:network
```

### Step 3: Server Identification
Give each server a unique name:

```yaml
server:
  name: lobby    # For lobby server
  name: survival # For survival server
  name: creative # For creative server
```

### Step 4: Enable Cross-Server Features
```yaml
cross-server:
  enabled: true
  sync-interval: 30
  notification-timeout: 5
```

## 🐛 Troubleshooting

### Common Issues

**Database Connection Failed**
- Verify MySQL credentials and host accessibility
- Check firewall settings
- Ensure database exists and user has proper permissions

**Redis Connection Failed**
- Verify Redis server is running
- Check Redis host and port configuration
- Verify Redis password if authentication is enabled

**Cross-Server Not Working**
- Ensure all servers use the same MySQL database
- Verify Redis configuration is identical across servers
- Check that server names are unique
- Confirm `cross-server.enabled` is true

**Plugin Not Loading**
- Verify Java 17+ is installed
- Check server software compatibility (Paper 1.21.1+)
- Review server logs for error messages

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Development Setup

1. Clone the repository
2. Import into your IDE (IntelliJ IDEA recommended)
3. Run `mvn clean install` to build
4. Test on a Paper 1.21.1+ server

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Paper Team** - For the excellent server software
- **Folia Team** - For multi-threaded server support
- **PlaceholderAPI** - For placeholder integration
- **HikariCP** - For database connection pooling
- **Jedis** - For Redis connectivity

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/WeThink25/We-Friends/issues)
- **Discussions**: [GitHub Discussions](https://github.com/WeThink25/We-Friends/discussions)

---

Made with ❤️ by [WeThink](https://github.com/WeThink25)

