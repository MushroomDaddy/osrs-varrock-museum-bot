Varrock Museum Cleaner Bot (Powbot)

An advanced OSRS bot script for Powbot that automates cleaning specimen finds at the Varrock Museum.

## ðŸŽ¯ Features

- **Fully Automated**: Mines specimens â†’ Cleans finds â†’ Stores artifacts â†’ Repeats
- **Smart Camera Control**: Automatically adjusts camera to see objects
- **Antique Lamp Detection**: Automatically uses lamps on Slayer skill
- **Randomized Patterns**: 
  - Randomized drop order (different every time)
  - Variable timing with micro-hesitations
  - Human-like rhythm building
- **Fast & Efficient**: 
  - Tap-to-drop support (150-250ms per item)
  - Quick task transitions
  - Minimal waiting
- **Error Handling**: 
  - Retry logic for failed interactions
  - Camera adjustment when objects not visible
  - Prevents spam-clicking

## ðŸ“‹ Requirements

- **OSRS Account** with access to Varrock Museum
- **Powbot** - [Download here](https://powbot.org/)
- **Gear Needed**:
  - Leather gloves
  - Leather boots
  - (Script will equip them automatically from inventory)

## âš™ï¸ Setup

### 1. Enable Tap-to-Drop in OSRS
**CRITICAL:** This script requires tap-to-drop to be enabled!

- **Option 1**: Tap the ðŸ—‘ï¸ DROP icon in the right sidebar (mobile/tablet view)
- **Option 2**: Settings â†’ Controls â†’ "Tap to Drop" toggle ON

### 2. Starting Location
- Be at the **Varrock Museum** in the specimen cleaning area
- Have leather gloves and boots in your inventory (script will equip them)

### 3. Installation
1. Copy the script file `VarrockMuseumCleaner_v32_FINAL.kt`
2. Place it in your Powbot scripts folder:
   - Windows: `C:\Users\[YourName]\.powbot\scripts\src`
   - macOS: `~/.powbot/scripts/src`
3. Restart Powbot or click "Refresh Scripts"

### 4. Running the Script
1. Open Powbot
2. Find "Varrock Museum Cleaner v32" in the script list
3. Click Start
4. The script will automatically:
   - Equip gear
   - Get cleaning tools
   - Start mining and cleaning specimens

## ðŸŽ® How It Works

### The Loop:
```
1. Get gear & tools (if needed)
2. Mine specimen rocks â†’ Fill inventory
3. Clean all specimens at specimen table (AFK)
4. Store cleaned artifacts in storage crate
5. Handle dialog & use any antique lamps
6. Drop junk items (broken glass, broken arrows)
7. Repeat!
```

### Task Priority:
1. **Equip gear** (gloves & boots)
2. **Get tools** (trowel, rock pick, specimen brush)
3. **Store cleaned artifacts** (highest priority!)
4. **Drop junk items**
5. **Fill inventory** with specimens
6. **Clean everything**

### Antique Lamp Handling:
- Detects all 6 types of Varrock Museum lamps
- Automatically clicks lamp â†’ Selects Slayer skill â†’ Confirms
- Uses widget-based detection for reliability
- Tracks total XP gained

## ðŸ“Š Statistics Tracking

The script displays real-time stats:
- Items cleaned
- Items stored
- Items dropped
- Lamps used
- Total Slayer XP gained
- Trips completed

## ðŸ”§ Customization

You can modify the script behavior by changing these values in the code:

### Drop Speed
```kotlin
// Lines ~670-680
val baseDelay = when {
    index >= 4 -> Random.nextInt(150, 250)  // Fast rhythm
    index >= 2 -> Random.nextInt(180, 280)  // Medium
    else -> Random.nextInt(200, 320)        // Slow start
}
```

### Camera Angles
```kotlin
// Lines ~215-220
Camera.pitch(Random.nextInt(90, 140))  // Adjust up/down viewing angle
```

## âš ï¸ Important Notes

### Tap-to-Drop REQUIRED
- The script **only** uses tap-to-drop (no right-click drop)
- If tap-to-drop is disabled, items won't be dropped
- Enable it in OSRS settings before starting!

### Performance Tips
- **Use mobile view** (BlueStacks/emulator) for best tap-to-drop support
- Keep the game window **in focus** while running
- Adjust camera manually before starting if needed
- The script works best in **fixed screen mode**

### Known Issues
- If camera gets stuck, manually adjust it once and restart script
- If lamp detection fails, check the console for widget debug info
- Script may pause briefly during dialog handling (this is normal)

## ðŸ“ Version History

### v32 - Current Version
- âœ… Tap-to-drop only (removed Drop action)
- âœ… Fixed camera adjustment (pitches UP properly)
- âœ… Removed inventory opening before object clicks
- âœ… Slower, more human-like drop timing (150-250ms)
- âœ… Randomized drop order every time
- âœ… Widget-based lamp detection (all 6 lamp types)
- âœ… Smart storage crate clicking (no spam)
- âœ… Faster task transitions

## ðŸ¤ Contributing

Feel free to fork this repository and submit pull requests! Some ideas for improvements:

- Add support for different cleaning locations
- Bank integration for gear/tools
- Multiple skill selection for lamps
- Break handler
- Paint overlay improvements

## âš–ï¸ Legal Disclaimer

This bot is for **educational purposes only**. Use of automation tools in OSRS may violate the game's Terms of Service and could result in account bans. Use at your own risk.

## ðŸ“§ Support

If you encounter issues:
1. Check that tap-to-drop is enabled
2. Make sure you're in the correct location (Varrock Museum)
3. Check the console output for error messages
4. Manually adjust camera if it's pointing at the ground

## ðŸ† Credits

Developed for Powbot OSRS automation framework.

**Website**: [powbot.org](https://powbot.org/)

---

**Enjoy botting responsibly!** ðŸ¤–
