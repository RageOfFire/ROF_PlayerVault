# Commands & Permissions

## Commands

All commands are available via `/rofvault`, `/vault`, or `/pv`.

| Command | Description |
|---|---|
| `/rofvault` | Opens your vault #1 (shortcut) |
| `/rofvault open [#]` | Opens your vault (default: #1) |
| `/rofvault open <player> [#]` | **[Admin]** Opens another player's vault |
| `/rofvault list` | Lists all vault numbers you have access to |
| `/rofvault delete <player> [#]` | **[Admin]** Deletes a player's vault |
| `/rofvault reload` | **[Admin]** Reloads the config |
| `/rofvault help` | Shows the help menu |

---

## Permissions

### Player permissions

| Permission | Description | Default |
|---|---|---|
| `rofvault.use` | Allows the player to use vaults | `true` |
| `rofvault.list` | Allows the player to list their vaults | `true` |
| `rofvault.storage.1` | Grants access to vault #1 | `true` |
| `rofvault.storage.2` | Grants access to vault #2 | `op` |
| `rofvault.storage.3` | Grants access to vault #3 | `op` |
| `rofvault.storage.<n>` | Grants access to vault #n | `op` |

### Vault size permissions

The **highest** matching `rofvault.size.X` permission the player holds determines their vault row count (overridden per-vault in config).

| Permission | Vault size | Default |
|---|---|---|
| `rofvault.size.1` | 1 row — 9 slots | `false` |
| `rofvault.size.2` | 2 rows — 18 slots | `false` |
| `rofvault.size.3` | 3 rows — 27 slots | `true` |
| `rofvault.size.4` | 4 rows — 36 slots | `false` |
| `rofvault.size.5` | 5 rows — 45 slots | `false` |
| `rofvault.size.6` | 6 rows — 54 slots (double chest) | `op` |

### Admin permissions

| Permission | Description | Default |
|---|---|---|
| `rofvault.admin` | Grants all admin sub-permissions | `op` |
| `rofvault.admin.reload` | Permission to reload the plugin | `op` |
| `rofvault.admin.delete` | Permission to delete any player's vault | `op` |
| `rofvault.admin.view` | Permission to view a vault read-only | `op` |
| `rofvault.admin.edit` | Permission to edit another player's vault | `op` |
| `rofvault.admin.open` | Permission to open any vault for any player | `op` |

> **Note:** `rofvault.admin.open` without `rofvault.admin.edit` grants read-only access.
