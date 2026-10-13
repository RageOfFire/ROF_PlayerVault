# PlaceholderAPI Placeholders

ROF_PlayerVault registers placeholders automatically when
[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) is installed.
No ecloud download is required.

| Placeholder | Returns | Example |
|---|---|---|
| `%rofvault_count%` | Number of vaults the player can access | `3` |
| `%rofvault_list%` | Comma-separated list of accessible vault numbers | `#1, #2, #3` |
| `%rofvault_has_<n>%` | `true`/`false` — whether the player can open vault #n | `true` |
| `%rofvault_rows_<n>%` | Resolved row count for vault #n | `6` |
| `%rofvault_size_<n>%` | Resolved slot count for vault #n (rows × 9) | `54` |
| `%rofvault_open%` | `true`/`false` — whether the player has a vault open right now | `false` |
| `%rofvault_open_number%` | Vault number currently open, or empty string | `2` |

## Examples

```
# Show how many vaults a player has in a scoreboard
Vaults: %rofvault_count%

# Conditionally show vault 2 info (with CMI conditionals or similar)
%rofvault_has_2%

# Show how big the player's vault 1 is
Vault #1 size: %rofvault_size_1% slots
```
