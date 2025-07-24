# IceRings Spigot Plugin

A Minecraft Spigot plugin for magical ice sphere generation.

## Description

IceRings is a Spigot plugin that adds magical ice sphere functionality to your Minecraft server. Players can place special blue ice blocks that instantly create protective hollow ice spheres around the placement location!

## Features

- **Special Blue Ice**: Custom blue ice items with unique properties and magical appearance
- **Hollow Ice Spheres**: Creates beautiful hollow spheres of colored glass blocks
- **Three-Stage Durability**: Ice blocks progress through three stages (Blue → Cyan → Light Blue) before breaking
- **Smart Block Restoration**: Original blocks are restored when timer expires (but not when manually broken)
- **Configurable Replaceable Blocks**: Control which blocks can be replaced by ice spheres
- **Inverse Block Mode**: Option to replace everything except specified blocks
- **WorldGuard Integration**: Optional region restrictions for placement (soft dependency)
- **Permission System**: Fine-grained control over who can use what features
- **Configurable Settings**: Customize sphere radius, duration, and block behavior
- **Admin Commands**: Give special ice blocks and manage the plugin

## Commands

- `/icerings` - Show plugin information
- `/icerings help` - Display available commands
- `/icerings give [player] [amount]` - Give special blue ice to a player (admin only)
- `/icerings reload` - Reload plugin configuration (admin only)
- `/icerings blocks list` - List current replaceable blocks and inverse mode status
- `/icerings blocks add <block>` - Add a block to the replaceable blocks list
- `/icerings blocks remove <block>` - Remove a block from the replaceable blocks list
- `/icerings blocks inverse [true|false]` - Toggle inverse mode for replaceable blocks

## Permissions

- `icerings.use` - Basic plugin usage (default: true)
- `icerings.give` - Permission to give special ice blocks (default: op)
- `icerings.admin` - Administrative commands (default: op)
- `icerings.*` - All permissions

## How It Works

1. **Getting Special Blue Ice**: Use `/icerings give` to obtain magical ice ring generator items
2. **Placing**: Place the special blue ice like regular blocks (respects WorldGuard regions if configured)
3. **Instant Sphere Creation**: A hollow sphere of blue stained glass instantly forms around the placement location
4. **Block Replacement**: The plugin stores the original blocks that were replaced by the ice sphere
5. **Three-Stage Durability System**: 
   - **Stage 1 (Blue Glass)**: Immune to most explosions, can be broken to advance to cyan
   - **Stage 2 (Cyan Glass)**: Vulnerable to all explosions, can be broken to advance to light blue
   - **Stage 3 (Light Blue Glass)**: Vulnerable to all explosions, breaks normally when hit
6. **Smart Cleanup**: 
   - **Timer Expiration**: Original blocks are automatically restored when the timer runs out
   - **Manual Breaking**: Blocks broken by players are permanently removed (no restoration)
   - **Explosion Damage**: Blocks destroyed by explosions are permanently removed (no restoration)

## Installation

1. Download the compiled JAR file from the releases
2. Place it in your server's `plugins` folder
3. Restart your server or use a plugin manager to load it
4. Configure the plugin by editing `config.yml` in the plugin folder

## Development

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Spigot/Paper server for testing
- WorldGuard (optional, for region restrictions)

### Building

```bash
mvn clean package
```

The compiled JAR will be available in the `target` folder.

### Development Setup

1. Clone this repository
2. Import into your IDE (VS Code with Java extensions recommended)
3. Run `mvn clean compile` to download dependencies
4. Start developing!

## Configuration

The plugin creates a `config.yml` file with various settings:

- **General Settings**: Debug mode, message prefix
- **Ice Rings Settings**: 
  - Sphere radius and duration
  - Replaceable blocks list (which blocks can be overridden)
  - Inverse mode toggle (replace everything except listed blocks)
- **WorldGuard Integration**:
  - Enable/disable region restrictions
  - Specify allowed regions for placement
- **Custom Messages**: All player-facing messages are configurable

### Replaceable Blocks System

IceRings includes a flexible system for controlling which blocks can be replaced by ice spheres:

**Normal Mode (default)**: Ice spheres will **only** replace blocks in the configured list
```yaml
inverse-replaceable-blocks: false
replaceable-blocks:
  - "AIR"
  - "WATER"
  - "GRASS"
```

**Inverse Mode**: Ice spheres will replace **all blocks except** those in the configured list
```yaml
inverse-replaceable-blocks: true
replaceable-blocks:
  - "BEDROCK"
  - "OBSIDIAN"
```

Use partial matches for block families (e.g., "GRASS" matches all grass-related blocks).

### WorldGuard Integration

IceRings supports WorldGuard as a soft dependency:
- **Not Installed**: Ice rings work globally
- **Installed + No Regions Configured**: Works globally
- **Installed + Regions Configured**: Only works in specified regions

Example configuration:
```yaml
worldguard:
  enabled: true
  allowed-regions:
    - "build-zone"
    - "pvp-arena"
    - "creative-world"
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support, issues, or feature requests, please create an issue on the GitHub repository.
