<div align="center">

# 🗃️ ROF_PlayerVault

**Give your players personal virtual storage — configurable, async, and built for every server size.**

[![Downloads](https://img.shields.io/github/downloads/RageOfFire/ROF_PlayerVault/total?style=flat-square&color=blue)](https://github.com/RageOfFire/ROF_PlayerVault/releases)
[![Release](https://img.shields.io/github/v/release/RageOfFire/ROF_PlayerVault?style=flat-square&color=green)](https://github.com/RageOfFire/ROF_PlayerVault/releases/latest)
[![Commits](https://img.shields.io/github/commit-activity/m/RageOfFire/ROF_PlayerVault?style=flat-square)](https://github.com/RageOfFire/ROF_PlayerVault/commits/master)
[![Stars](https://img.shields.io/github/stars/RageOfFire/ROF_PlayerVault?style=flat-square&color=yellow)](https://github.com/RageOfFire/ROF_PlayerVault/stargazers)
[![Discord](https://img.shields.io/discord/752171524919918672?style=flat-square&color=5865F2&label=discord)](https://discord.gg/752171524919918672)
[![Spigot](https://img.shields.io/badge/spigot-114665-orange?style=flat-square)](https://www.spigotmc.org/resources/rof_playervault.114665/)

</div>

---

## ✨ Features

| Feature | Details |
|---|---|
| 🗄️ **Multiple vaults** | Up to 10 vaults per player (configurable), each with its own permission node |
| 📐 **Per-vault sizing** | 1–6 rows per vault — set by permission node or per-vault config override |
| ⚡ **Async I/O** | All reads and writes happen off the main thread — zero lag on large servers |
| 💾 **FILE or MySQL** | Choose your storage backend; MySQL uses a connection pool |
| 💰 **Economy costs** | Optional per-vault open cost via [Vault](https://www.spigotmc.org/resources/vault.34315/) |
| 📊 **PlaceholderAPI** | 7 placeholders for scoreboards, holograms, and conditionals |
| 🔊 **Open sound & title** | Configurable sound, volume, pitch, and title/subtitle animation |
| 🔒 **Item blacklist** | Block specific materials from ever being placed in a vault |
| 🧲 **Auto-pickup** | Redirect ground items into a vault when the player's inventory is full |
| 🛡️ **Admin tools** | View (read-only) or edit any player's vault, delete with safe item drop |
| 📋 **Tab completion** | All subcommands, player names, and vault numbers auto-complete |
| 🔁 **1.8 → 1.21+** | One jar covers all modern Spigot/Paper versions |

---

## 🚀 Quick Start

### Requirements

| | Minimum | Notes |
|---|---|---|
| Java | 8 | |
| Spigot / Paper | **1.8.8** | Tested through 1.21+ |
| Vault | Any | Optional — required for economy costs |
| PlaceholderAPI | 2.11+ | Optional — auto-detected on startup |

### Installation

1. Download the latest jar from [Releases](https://github.com/RageOfFire/ROF_PlayerVault/releases/latest) or [SpigotMC](https://www.spigotmc.org/resources/rof_playervault.114665/)
2. Place it in your server's `/plugins/` folder
3. Restart your server
4. Edit `plugins/ROF_PlayerVault/config.yml`
5. Run `/rofvault reload` to apply changes without a restart

### Give players their first vault (LuckPerms example)

```bash
/lp group default permission set rofvault.use true
/lp group default permission set rofvault.storage.1 true
/lp group default permission set rofvault.size.3 true   # 3 rows = 27 slots

# VIP: vaults 1–3 with a double chest
/lp group vip permission set rofvault.storage.2 true
/lp group vip permission set rofvault.storage.3 true
/lp group vip permission set rofvault.size.6 true       # 6 rows = 54 slots
```

---

## 🕹️ Commands

| Command | Description |
|---|---|
| `/rofvault` | Open your vault #1 |
| `/rofvault open [#]` | Open one of your vaults |
| `/rofvault open <player> [#]` | **[Admin]** Open another player's vault |
| `/rofvault list` | List all vaults you can access |
| `/rofvault delete <player> [#]` | **[Admin]** Delete a vault (items dropped safely) |
| `/rofvault reload` | **[Admin]** Reload config |

Aliases: `/vault`, `/pv`

---

## 🔑 Key Permissions

| Permission | Description | Default |
|---|---|---|
| `rofvault.use` | Use vaults | `true` |
| `rofvault.storage.<n>` | Access vault number n | `true` for #1 |
| `rofvault.size.<n>` | Vault opens with n rows (1–6) | `true` for 3 |
| `rofvault.admin` | All admin permissions | `op` |

→ Full permission list in the [Wiki](https://github.com/RageOfFire/ROF_PlayerVault/wiki/Commands-and-Permissions)

---

## 📊 PlaceholderAPI

```
%rofvault_count%        → number of vaults the player can access
%rofvault_list%         → "#1, #2, #3"
%rofvault_has_<n>%      → true / false
%rofvault_size_<n>%     → slot count for vault #n
%rofvault_open%         → true if a vault is currently open
%rofvault_open_number%  → vault number currently open
```

→ Full list in the [Placeholders wiki page](https://github.com/RageOfFire/ROF_PlayerVault/wiki/Placeholders)

---

## ⚙️ Configuration highlights

```yaml
storage:
  type: FILE        # or MYSQL

vault-size:
  default-rows: 3
  per-vault:        # override size per vault number
    3: 6            # vault #3 always opens as a double chest

economy:
  enable: true
  cost-per-open:
    2: 100          # costs $100 to open vault #2
    3: 500

delete-action: DROP  # DROP items to the ground, or CLEAR to wipe silently
```

→ Full annotated config in the [Wiki](https://github.com/RageOfFire/ROF_PlayerVault/wiki/Default-Configuration)

---

## 📚 Documentation

- [Getting Started](https://github.com/RageOfFire/ROF_PlayerVault/wiki/Getting-Started)
- [Commands & Permissions](https://github.com/RageOfFire/ROF_PlayerVault/wiki/Commands-and-Permissions)
- [Default Configuration](https://github.com/RageOfFire/ROF_PlayerVault/wiki/Default-Configuration)
- [Placeholders](https://github.com/RageOfFire/ROF_PlayerVault/wiki/Placeholders)

---

## 🐛 Support

- **Bug reports / feature requests** → [GitHub Issues](https://github.com/RageOfFire/ROF_PlayerVault/issues)
- **Questions** → [Discord](https://discord.gg/752171524919918672)

![Issues](https://img.shields.io/github/issues/RageOfFire/ROF_PlayerVault?style=flat-square)
![PRs](https://img.shields.io/github/issues-pr/RageOfFire/ROF_PlayerVault?style=flat-square)

---

## 💼 Work With Me

Enjoying this plugin or need something custom built?
I'm available for freelance Minecraft plugin and software development.

[![Hire Me on Fiverr](https://img.shields.io/badge/Hire_Me_on-Fiverr-1DBF73?style=for-the-badge)](https://www.fiverr.com/s/d00632G)

---

<div align="center">

Made with ❤️ by [RageOfFire](https://github.com/RageOfFire)

</div>
