# AuraSkills-Extra Commands

This page documents all available commands in AuraSkills-Extra.

## Player Commands

### Spells
- **`/ske spells`**
    - Opens the main Spells menu where you can learn and equip spells.
- **`/ske spells admin`**
    - Opens the Spell Administration menu to give spells to online players.
- **`/ske spells getwand`**
    - Gives the player a **Spell Wand** (Blaze Rod) used for casting.

## Admin Commands
*Requires `ske.admin` permission.*

### Origins Management
- **`/ske origins`**
    - Opens the Origins selection menu.
- **`/ske origins set <player> <origin>`**
    - Manually set a player's origin.
- **`/ske origins remove <player>`**
    - Remove a player's current origin.
- **`/ske origins enable`**
    - Enables the entire Origins system and reapplies modifiers to online players.
- **`/ske origins disable`**
    - Disables the Origins system and removes all active modifiers.
- **`/ske origins reload`**
    - Reloads all origin configuration files from `config/origins/`.

### Spells Management
- **`/ske spells getwand`**
    - Gives the player a **Spell Wand** (Blaze Rod) used for casting.
- **`/ske admin spell equip <player> <spell>`**
    - Force-equips a spell for a player.
- **`/ske admin spell unequip <player> <spell>`**
    - Force-unequips a spell from a player.

### System Commands
- **`/ske reload`**
    - Reloads the main plugin configuration.
