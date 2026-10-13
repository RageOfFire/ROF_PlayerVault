# Default Configuration

Below is the full `config.yml` with every option explained.
Copy it to `plugins/ROF_PlayerVault/config.yml` if you need to reset to defaults.

```yaml
#####################################################
#               ROF_PlayerVault                     #
#               by RageOfFire                       #
#####################################################

# ── GENERAL ────────────────────────────────────────
# %number% is replaced with the vault number.
vault-title: '&6ROF Vault &f#%number%'

# Highest vault number that can ever be opened.
# Players still need rofvault.storage.X permissions.
max-vaults: 10

# ── VAULT SIZE ──────────────────────────────────────
vault-size:
  # Fallback when no rofvault.size.X permission matches the player.
  default-rows: 3
  # Per-vault row overrides (1–6). Uncomment to enable.
  per-vault:
    # 1: 3
    # 2: 4
    # 3: 6

# ── STORAGE BACKEND ────────────────────────────────
storage:
  type: FILE    # FILE | MYSQL

  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: root
    password: password
    pool-size: 5
    table-prefix: rofvault_
    properties: "useSSL=false&autoReconnect=true&characterEncoding=utf8"

# ── AUTO-PICKUP ─────────────────────────────────────
vault-pickup:
  enable: false
  target-vault: 1     # Items go into this vault number
  interval: 3         # Seconds between scans
  radius: 3           # Block radius around the player
  # WHITELIST: only listed items are picked up
  # BLACKLIST: all items EXCEPT listed are picked up
  mode: BLACKLIST
  items: []

# ── ITEM BLACKLIST ──────────────────────────────────
# Items that can never be placed in ANY vault.
blacklist-items: []

# ── VAULT OPEN SOUND & ANIMATION ───────────────────
vault-open:
  sound: BLOCK_CHEST_OPEN   # '' to disable
  volume: 1.0
  pitch: 1.0
  show-title: false
  title: '&6ROF Vault &f#%number%'
  subtitle: '&7Your personal storage'
  title-fade-in: 10   # ticks
  title-stay: 40
  title-fade-out: 10

# ── ECONOMY ─────────────────────────────────────────
economy:
  enable: false
  cost-per-open:
    # 1: 0
    # 2: 100
    # 3: 500
  admin-bypass: true  # rofvault.admin skips cost

# ── VAULT DELETION BEHAVIOUR ────────────────────────
# DROP  → items dropped at player's location (or world spawn)
# CLEAR → items silently deleted
delete-action: DROP

# ── MESSAGES ────────────────────────────────────────
messages:
  prefix: "&f[&6ROF_PlayerVault&f] "
  only-player: "&cOnly a player can execute this command."
  command-reload: "&aPlugin reloaded successfully!"
  no-permission: "&cYou don't have permission to do that."
  usage-main: "&cUsage: /rofvault <open|list|delete|reload>"
  usage-open: "&cUsage: /rofvault open <player> [vault#]"
  usage-delete: "&cUsage: /rofvault delete <player> [vault#]"

  vault-opened: "&aOpened vault &6#%number%&a."
  vault-list: "&aYour vaults: &6%vaults%"
  vault-list-empty: "&cYou don't have access to any vaults."

  vault-deleted: "&aVault &6#%number% &aof &6%player% &adeleted successfully!"
  vault-deleted-items-dropped: "&aItems from the deleted vault were dropped at their last location."
  vault-not-found: "&cVault &6#%number% &cdoes not exist or is empty."
  vault-no-access: "&cYou don't have access to vault &6#%number%&c."

  blacklisted-item: "&cThis item is blacklisted and cannot be placed in a vault."

  economy-insufficient: "&cYou need &6%cost% &cto open vault &6#%number%&c."
  economy-charged: "&aCharged &6%cost% &ato open vault &6#%number%&a."

  player-not-found: "&cPlayer &6%player% &cwas not found."
  invalid-vault-number: "&cInvalid vault number. Must be between 1 and %max%."

  admin-viewing: "&e[Admin] Viewing vault &6#%number% &eof &6%player%&e."
  admin-editing: "&e[Admin] Editing vault &6#%number% &eof &6%player%&e."
```
