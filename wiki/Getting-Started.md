# Getting Started

## Requirements

| Requirement | Version |
|---|---|
| Java | 8 or newer |
| Spigot / Paper | **1.8.8 – 1.21+** |
| Vault *(optional)* | Any modern release |
| PlaceholderAPI *(optional)* | 2.11+ |

> ROF_PlayerVault targets **1.8.8** as its minimum and is tested through the latest Paper release.
> There is **no upper version cap** — the plugin adapts at runtime using version-safe helpers.

---

## Installation

1. Download `ROF_PlayerVault-X.X.X.jar` from the [Releases](https://github.com/RageOfFire/ROF_PlayerVault/releases) page.
2. Place the jar in your server's `/plugins/` folder.
3. Start (or restart) your server.
4. Edit `plugins/ROF_PlayerVault/config.yml` to your liking.
5. Run `/rofvault reload` or restart again to apply changes.

---

## Optional dependencies

### Vault (economy)
Install [Vault](https://www.spigotmc.org/resources/vault.34315/) and a compatible economy plugin
(EssentialsX, CMI, etc.), then set `economy.enable: true` in `config.yml`.

### PlaceholderAPI
Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/).
The plugin hooks automatically on startup — no ecloud download needed.
See [Placeholders](Placeholders.md) for the full list.

---

## Quick-start permission setup (LuckPerms example)

```bash
# Give all players vault #1 with 3 rows
/lp group default permission set rofvault.use true
/lp group default permission set rofvault.storage.1 true
/lp group default permission set rofvault.size.3 true

# Give VIP rank vaults #1–3 with 6 rows (double chest)
/lp group vip permission set rofvault.storage.2 true
/lp group vip permission set rofvault.storage.3 true
/lp group vip permission set rofvault.size.6 true
```
