# Changelog

## [2.0.0] — Unreleased

### Added
- **Per-vault size config** — set `vault-size.per-vault.<n>: <rows>` in config
- **Permission-based vault size** — `rofvault.size.1` through `rofvault.size.6`; highest wins
- **MySQL backend** — optional; switch with `storage.type: MYSQL`
- **Async I/O** — all vault reads and writes are off the main thread
- **Vault open sound** — configurable sound, volume, and pitch
- **Title/subtitle on open** — configurable with fade timings
- **Economy integration** — optional per-vault open cost via Vault API
- **`/rofvault list`** — players can now see which vault numbers they can access
- **`/rofvault open <player> [#]`** — admins can open any numbered vault
- **`rofvault.admin.view`** — read-only admin vault inspection
- **Auto-pickup whitelist mode** — set `vault-pickup.mode: WHITELIST`
- **Auto-pickup per-item filter** — item list respected in both whitelist and blacklist modes
- **PlaceholderAPI support** — 7 placeholders (count, list, has_n, rows_n, size_n, open, open_number)
- **Vault deletion safety** — `delete-action: DROP` drops items at player's last location
- **Tab completion** — all subcommands, player names, and vault numbers
- **GitHub Actions CI** — auto-build on push; auto-release on version tags
- **Wiki** — Commands & Permissions, Getting Started, Default Configuration, Placeholders

### Changed
- Minimum supported version documented as **1.8.8**
- `rofvault.admin.reload` permission deduplicated in wiki (was listed twice)
- `delete-action` replaces silent item loss on vault delete

### Fixed
- Items in open vaults now flush to disk on `/stop` or `/reload`
- Console can now run `/rofvault delete` without a NullPointerException
