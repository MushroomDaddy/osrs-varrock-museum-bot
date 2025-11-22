package org.agentorange.varrokmuseum

import org.powbot.api.Condition
import org.powbot.api.Random
import org.powbot.api.Tile
import org.powbot.api.Area
import org.powbot.api.rt4.*
import org.powbot.api.rt4.walking.local.Utils
import org.powbot.api.rt4.walking.model.Skill
import org.powbot.api.script.*
import org.powbot.api.script.paint.Paint
import org.powbot.api.script.paint.PaintBuilder
import org.powbot.api.script.tree.SimpleBranch
import org.powbot.api.script.tree.SimpleLeaf
import org.powbot.api.script.tree.TreeComponent
import org.powbot.api.script.tree.TreeScript
import org.powbot.mobile.service.ScriptUploader

/**
 * Varrock Museum Cleaner v32.0 FINAL
 * TreeScript - READY TO TEST
 * 
 * @author SuperNinja
 * @version 32.0.0
 */

fun main() {
    ScriptUploader().uploadAndStart(
        "Varrock Museum Cleaner v32",
        "main",
        "127.0.0.1:5585",
        true,
        false
    )
}

@ScriptManifest(
    name = "Varrock Museum Cleaner v32",
    description = "TreeScript - FINAL WORKING VERSION",
    version = "32.0.0",
    author = "SuperNinja",
    category = ScriptCategory.Slayer
)
class VarrockMuseumCleaner : TreeScript() {

    var totalCleaned = 0
    var totalStored = 0
    var totalDropped = 0
    var lampsUsed = 0
    var slayerXpGained = 0
    var tripsCompleted = 0

    object C {
        const val UNCLEANED_FIND = 11175
        const val TROWEL = 676
        const val ROCK_PICK = 675
        const val SPECIMEN_BRUSH = 670
        const val LEATHER_GLOVES = 1059
        const val LEATHER_BOOTS = 1061
        
        // ALL Varrock Museum Antique Lamp IDs (from different quests/activities)
        val ANTIQUE_LAMP = intArrayOf(
            11189,  // Uncleaned finds (museum cleaning) ← PRIMARY ONE FROM CLEANING!
            11188,  // Curse of the Empty Lord
            11187,  // Making History
            11186,  // Shield of Arrav
            11185,  // Merlin's Crystal
            28820   // Defender of Varrock
        )
        
        // ALL items that come from cleaning (including coins, pots, daggers, etc.)
        // NOTE: Lamp IDs (11185, 11186, 11187) removed - they should be USED not stored!
        val CLEANED_ARTIFACTS = intArrayOf(
            11177, 11178, 11179, 11180, 11181, 11182, 11183, 11184,  // Museum artifacts (removed lamp IDs)
            995,   // Coins
            1203,  // Iron dagger
            1931,  // Pot
            1923,  // Bowl
            453, 440, 436, 447,  // Ores (coal, iron, copper, bronze)
            1627, 1629,  // Uncut jade, opal
            863,   // Iron ore
            526, 532,  // Bones
            314, 886,  // Feathers, arrows
            823, 819, 820, 821, 822  // Various items
        )
        
        // ONLY broken items that can't be stored
        val JUNK_ITEMS = intArrayOf(11174, 11173)  // Broken arrow, Broken glass
        
        val CLEANING_AREA = Area(Tile(3253, 3440, 0), Tile(3268, 3455, 0))
    }

    override val rootComponent: TreeComponent<*> by lazy {
        SimpleBranch(this, "In museum?",
            successComponent = branchNeedsGear(),  // Start with normal flow, NOT lamp check!
            failedComponent = leafIdle(),
            validator = { C.CLEANING_AREA.contains(Players.local()) }
        )
    }

    override fun onStart() {
        println("=== Varrock Museum Cleaner v32 Started ===")
        println("Expected: 5-6 lamps/hour = 2500-3000 Slayer XP/hour")
        
        // Check if tap-to-drop is enabled
        checkTapToDrop()
        
        // Debug: Show current inventory
        println("\n[DEBUG] Current inventory:")
        val items = Inventory.stream().toList()
        for (item in items) {
            println("[DEBUG] - ${item.name()} (ID: ${item.id()}, Stack: ${item.stackSize()})")
        }
        println("[DEBUG] ANTIQUE_LAMP IDs we're looking for: ${C.ANTIQUE_LAMP.joinToString(", ")}")
        println()
        
        addPaint()
    }
    
    private fun checkTapToDrop() {
        println("[SETUP] Checking tap-to-drop status...")
        
        // On mobile/emulators, tap-to-drop is needed (shift-drop doesn't work)
        if (Inventory.shiftDroppingEnabled()) {
            println("[SETUP] ✓ Tap/shift-drop is ENABLED")
        } else {
            println("========================================")
            println("[SETUP] ⚠️⚠️⚠️ TAP-TO-DROP NOT ENABLED!")
            println("[SETUP] Enable it:")
            println("[SETUP] 1. Tap the DROP icon in sidebar (mobile)")
            println("[SETUP] 2. OR Settings → Tap to Drop")
            println("========================================")
        }
    }

    private fun addPaint() {
        val p: Paint = PaintBuilder.newBuilder()
            .addString("Last leaf:") { lastLeaf.name }
            .addString("Trips:") { "$tripsCompleted" }
            .addString("Lamps:") { "$lampsUsed | XP: $slayerXpGained" }
            .trackSkill(Skill.Slayer)
            .trackInventoryItems(*C.ANTIQUE_LAMP)
            .y(45)
            .x(40)
            .build()
        addPaint(p)
    }

    override fun onStop() {
        println("=== Final Stats ===")
        println("Trips: $tripsCompleted")
        println("Lamps used: $lampsUsed")
        println("Slayer XP: $slayerXpGained")
    }

    // ========== STATE CHECKS ==========
    
    private fun checkGearEquipped() = Equipment.stream().id(C.LEATHER_GLOVES).isNotEmpty() && 
                                      Equipment.stream().id(C.LEATHER_BOOTS).isNotEmpty()
    
    private fun checkTools() = Inventory.stream().id(C.TROWEL).isNotEmpty() && 
                              Inventory.stream().id(C.ROCK_PICK).isNotEmpty() && 
                              Inventory.stream().id(C.SPECIMEN_BRUSH).isNotEmpty()
    
    private fun checkUncleanedFinds() = Inventory.stream().id(C.UNCLEANED_FIND).isNotEmpty()
    private fun checkCleanedArtifacts() = Inventory.stream().id(*C.CLEANED_ARTIFACTS).isNotEmpty()
    private fun checkJunk() = Inventory.stream().id(*C.JUNK_ITEMS).isNotEmpty()
    
    private fun adjustCameraForObject(obj: Interactive, targetName: String) {
        if (obj.inViewport()) {
            println("[CAMERA] $targetName already visible")
            return
        }
        
        println("[CAMERA] ⚠️ $targetName NOT visible - adjusting camera...")
        
        // First, turn camera towards the object
        if (obj is GameObject) {
            println("[CAMERA] Turning to face $targetName...")
            Camera.turnTo(obj.tile)
            Condition.sleep(Random.nextInt(800, 1200))
            
            if (obj.inViewport()) {
                println("[CAMERA] ✓ $targetName now visible!")
                return
            }
        }
        
        // If not visible yet, adjust pitch to look UP (not down!)
        val currentPitch = Camera.pitch()
        println("[CAMERA] Current pitch: $currentPitch (need to pitch UP to see objects)")
        
        // Pitch UP to see objects (90-180 = looking up at objects)
        // Lower values = looking up, higher values = looking down at ground
        if (currentPitch > 200) {
            // Currently looking at ground, pitch way up
            println("[CAMERA] Pitching UP significantly...")
            Camera.pitch(Random.nextInt(90, 140))
        } else {
            // Just adjust a bit
            println("[CAMERA] Adjusting pitch UP...")
            Camera.pitch(Random.nextInt(120, 180))
        }
        
        Condition.sleep(Random.nextInt(1000, 1500))
        
        if (obj.inViewport()) {
            println("[CAMERA] ✓✓✓ $targetName visible after pitch adjustment!")
        } else {
            println("[CAMERA] Trying maximum pitch UP...")
            Camera.pitch(Random.nextInt(80, 120))
            Condition.sleep(Random.nextInt(800, 1200))
            
            if (obj.inViewport()) {
                println("[CAMERA] ✓ $targetName finally visible!")
            } else {
                println("[CAMERA] ⚠️ $targetName still not visible - continuing anyway...")
            }
        }
    }
    
    private fun interactWithObject(name: String, action: String): Boolean {
        println("[INTERACT] Looking for '$name'...")
        
        val obj = Objects.stream().name(name).within(C.CLEANING_AREA).nearest().first()
        if (!obj.valid()) {
            println("[ERROR] '$name' not found")
            return false
        }
        
        println("[INTERACT] Found '$name' at distance: ${obj.distance()}")
        
        // Adjust camera if object not visible
        if (!obj.inViewport()) {
            adjustCameraForObject(obj, name)
        }
        
        // Try up to 3 times with walkAndInteract (smooth + natural)
        var attempts = 0
        while (attempts < 3) {
            attempts++
            println("[INTERACT] Attempt $attempts/3 for '$name'...")
            
            // Use Utils.walkAndInteract for smooth walking + clicking in one motion
            if (Utils.walkAndInteract(obj, action)) {
                println("[INTERACT] ✓✓✓ Successfully interacted with '$name'")
                Condition.sleep(Random.nextInt(600, 1000))
                return true
            }
            
            println("[INTERACT] Failed, waiting before retry...")
            Condition.sleep(Random.nextInt(1500, 2500))
        }
        
        println("[ERROR] Failed to interact with '$name' after $attempts attempts")
        return false
    }

    // ========== BRANCHES ==========
    
    private fun branchNeedsGear(): SimpleBranch<VarrockMuseumCleaner> {
        return SimpleBranch(this, "Needs gear?",
            successComponent = leafEquipGear(),
            failedComponent = branchNeedsTools(),
            validator = { !checkGearEquipped() }
        )
    }
    
    private fun branchNeedsTools(): SimpleBranch<VarrockMuseumCleaner> {
        return SimpleBranch(this, "Needs tools?",
            successComponent = leafGetTools(),
            failedComponent = branchHasCleanedArtifacts(),  // Check cleaned FIRST!
            validator = { !checkTools() }
        )
    }
    
    private fun branchHasCleanedArtifacts(): SimpleBranch<VarrockMuseumCleaner> {
        return SimpleBranch(this, "Has cleaned?",
            successComponent = leafStoreAll(),  // STORE IMMEDIATELY (highest priority!)
            failedComponent = branchHasJunk(),
            validator = { checkCleanedArtifacts() }
        )
    }
    
    private fun branchHasJunk(): SimpleBranch<VarrockMuseumCleaner> {
        return SimpleBranch(this, "Has junk?",
            successComponent = leafDropJunk(),
            failedComponent = branchInventoryNotFull(),  // Then check if need more
            validator = { checkJunk() }
        )
    }
    
    private fun branchInventoryNotFull(): SimpleBranch<VarrockMuseumCleaner> {
        return SimpleBranch(this, "Inv not full?",
            successComponent = leafFillInventory(),  // Fill for new batch
            failedComponent = branchHasUncleanedFinds(),  // Then clean
            validator = { !Inventory.isFull() }
        )
    }
    
    private fun branchHasUncleanedFinds(): SimpleBranch<VarrockMuseumCleaner> {
        return SimpleBranch(this, "Has uncleaned?",
            successComponent = leafCleanAll(),  // Clean full inventory
            failedComponent = leafIdle(),  // All done
            validator = { checkUncleanedFinds() }
        )
    }

    // ========== LEAVES ==========
    
    private fun leafEquipGear(): SimpleLeaf<VarrockMuseumCleaner> {
        return SimpleLeaf(this, "Equip gear") {
            println("[GEAR] Equipping...")
            
            // ALWAYS ensure inventory tab is open
            if (Game.tab() != Game.Tab.INVENTORY) {
                println("[GEAR] Opening inventory tab...")
                Game.tab(Game.Tab.INVENTORY)
                Condition.sleep(Random.nextInt(500, 800))
            }
            
            if (Equipment.stream().id(C.LEATHER_GLOVES).isEmpty()) {
                val gloves = Inventory.stream().id(C.LEATHER_GLOVES).first()
                if (gloves.valid() && gloves.interact("Wear")) {
                    Condition.sleep(Random.nextInt(1200, 1800))
                    return@SimpleLeaf
                }
            }
            
            if (Equipment.stream().id(C.LEATHER_BOOTS).isEmpty()) {
                val boots = Inventory.stream().id(C.LEATHER_BOOTS).first()
                if (boots.valid() && boots.interact("Wear")) {
                    Condition.sleep(Random.nextInt(1200, 1800))
                    return@SimpleLeaf
                }
            }
            
            interactWithObject("Tools", "Take")
            Condition.sleep(2000)
        }
    }
    
    private fun leafGetTools(): SimpleLeaf<VarrockMuseumCleaner> {
        return SimpleLeaf(this, "Get tools") {
            println("[TOOLS] Getting...")
            interactWithObject("Tools", "Take")
            Condition.sleep(2000)
        }
    }
    
    private fun leafDropJunk(): SimpleLeaf<VarrockMuseumCleaner> {
        return SimpleLeaf(this, "Drop junk") {
            println("[DROP] Dropping junk...")
            
            // ALWAYS ensure inventory tab is open
            if (Game.tab() != Game.Tab.INVENTORY) {
                println("[DROP] Opening inventory tab...")
                Game.tab(Game.Tab.INVENTORY)
                Condition.sleep(Random.nextInt(500, 800))
            }
            
            val junkItems = Inventory.stream().id(*C.JUNK_ITEMS).toList()
            if (junkItems.isNotEmpty()) {
                println("[DROP] Dropping ${junkItems.size} junk items...")
                
                // RANDOMIZE drop order
                val shuffledJunk = junkItems.shuffled()
                
                // Just use tap-to-drop - slower, more human timing
                for ((index, item) in shuffledJunk.withIndex()) {
                    if (!item.valid()) {
                        continue
                    }
                    
                    println("[DROP] Drop #${index + 1}/${shuffledJunk.size}: ${item.name()}")
                    
                    // Simple click - tap-to-drop must be enabled
                    item.click()
                    
                    // Slower, more human timing
                    val baseDelay = when {
                        index >= 4 -> Random.nextInt(150, 250)
                        index >= 2 -> Random.nextInt(180, 280)
                        else -> Random.nextInt(200, 320)
                    }
                    
                    val hesitation = if (Random.nextInt(0, 100) < 12) {
                        Random.nextInt(80, 180)
                    } else {
                        0
                    }
                    
                    Condition.sleep(baseDelay + hesitation)
                    totalDropped++
                }
            }
        }
    }
    
    private fun leafStoreAll(): SimpleLeaf<VarrockMuseumCleaner> {
        return SimpleLeaf(this, "Store all") {
            println("[STORE] Storing all cleaned items...")
            
            // Find storage crate (don't open inventory tab - not needed for clicking objects!)
            val crate = Objects.stream()
                .name("Storage crate")
                .within(C.CLEANING_AREA)
                .nearest()
                .first()
            
            if (!crate.valid()) {
                println("[STORE] No crate found")
                return@SimpleLeaf
            }
            
            println("[STORE] Found crate at distance: ${crate.distance()}")
            
            // Adjust camera if crate not visible
            if (!crate.inViewport()) {
                println("[STORE] Crate not visible, adjusting camera...")
                adjustCameraForObject(crate, "Storage crate")
            }
            
            // Short wait before clicking (reduced for faster transitions)
            Condition.sleep(Random.nextInt(200, 400))
            
            val countBefore = Inventory.stream().id(*C.CLEANED_ARTIFACTS).count().toInt()
            println("[STORE] Have $countBefore items to store")
            
            // Click crate ONCE and verify it worked
            println("[STORE] Clicking storage crate...")
            val clicked = Utils.walkAndInteract(crate, "Add finds")
            
            if (clicked) {
                println("[STORE] Click registered, waiting for storage to start...")
                
                // Wait for storage to actually start (items disappearing or dialog)
                var storageStarted = false
                for (i in 0..10) {
                    Condition.sleep(300)
                    
                    val currentCount = Inventory.stream().id(*C.CLEANED_ARTIFACTS).count().toInt()
                    if (currentCount < countBefore || Chat.chatting()) {
                        println("[STORE] ✓ Storage confirmed! Items being stored...")
                        storageStarted = true
                        break
                    }
                }
                
                if (!storageStarted) {
                    println("[STORE] ⚠️ Storage didn't start, retrying...")
                    Condition.sleep(Random.nextInt(500, 800))
                    
                    // One retry
                    if (Utils.walkAndInteract(crate, "Add finds")) {
                        Condition.sleep(Random.nextInt(1000, 1500))
                    }
                }
            } else {
                println("[STORE] ⚠️ Failed to click crate, retrying...")
                Condition.sleep(Random.nextInt(800, 1200))
                Utils.walkAndInteract(crate, "Add finds")
                Condition.sleep(Random.nextInt(1000, 1500))
            }
            
            // Now wait for all items to be stored
            println("[STORE] Waiting for items to be stored...")
            var waitTime = 0
            
            while (waitTime < 30 && Inventory.stream().id(*C.CLEANED_ARTIFACTS).isNotEmpty()) {
                Condition.sleep(1000)
                waitTime++
                
                // Check for archaeologist dialog
                if (Chat.chatting()) {
                    println("[STORE] Dialog detected - handling...")
                    
                    // Click through dialog first
                    var clickAttempts = 0
                    while (Chat.chatting() && clickAttempts < 5) {
                        if (Chat.canContinue()) {
                            println("[STORE] Clicking continue...")
                            Chat.clickContinue()
                            Condition.sleep(Random.nextInt(1000, 1500))
                            clickAttempts++
                        } else {
                            Condition.sleep(500)
                        }
                    }
                    
                    println("[STORE] ========== DIALOG CLEARED ==========")
                    
                    // Debug: Show current inventory state
                    println("[STORE] [DEBUG] Current inventory items:")
                    val currentItems = Inventory.stream().toList()
                    for (item in currentItems) {
                        println("[STORE] [DEBUG]   - ${item.name()} (ID: ${item.id()}, Stack: ${item.stackSize()})")
                    }
                    
                    // CRITICAL: Check if we still have uncleaned finds
                    val uncleanedCount = Inventory.stream().id(C.UNCLEANED_FIND).count().toInt()
                    println("[STORE] [DEBUG] Uncleaned finds: $uncleanedCount")
                    if (uncleanedCount > 0) {
                        println("[STORE] ⚠️ WARNING: Still have $uncleanedCount uncleaned finds!")
                        println("[STORE] Not dropping - need to clean first")
                        return@SimpleLeaf
                    }
                    
                    // CRITICAL: Check for lamps - USE THEM FIRST before dropping!
                    val lampCount = Inventory.stream().id(*C.ANTIQUE_LAMP).count().toInt()
                    println("[STORE] [DEBUG] Checking for lamps... Found: $lampCount")
                    println("[STORE] [DEBUG] Looking for lamp IDs: ${C.ANTIQUE_LAMP.joinToString(", ")}")
                    
                    if (lampCount > 0) {
                        println("========================================")
                        println("[STORE] 🏆🏆🏆 LAMP(S) DETECTED AFTER STORAGE!")
                        println("[STORE] Found $lampCount lamp(s) - using NOW before dropping!")
                        println("========================================")
                        
                        // Use all lamps
                        for (lampNum in 1..lampCount) {
                            val lamp = Inventory.stream().id(*C.ANTIQUE_LAMP).first()
                            if (!lamp.valid()) {
                                println("[STORE] Lamp disappeared, continuing...")
                                break
                            }
                            
                            // Ensure inventory tab open
                            if (Game.tab() != Game.Tab.INVENTORY) {
                                Game.tab(Game.Tab.INVENTORY)
                                Condition.sleep(Random.nextInt(500, 800))
                            }
                            
                            println("[STORE] Using lamp $lampNum/$lampCount on Slayer...")
                            if (lamp.interact("Rub")) {
                                println("[STORE] Lamp rubbed, waiting for skill selection grid...")
                                Condition.sleep(Random.nextInt(2500, 3500))
                                
                                // Wait for the skill selection widget to appear (widget 134)
                                println("[STORE] Waiting for widget 134 (skill grid)...")
                                Condition.wait({ Widgets.widget(134).valid() }, 500, 20)
                                
                                val skillWidget = Widgets.widget(134)
                                if (!skillWidget.valid()) {
                                    println("[STORE] ⚠️ Widget 134 not found!")
                                    continue  // Skip to next lamp
                                }
                                
                                println("[STORE] Widget 134 found! Searching for Slayer skill...")
                                
                                // Slayer is typically at index 18 in the skill grid
                                // But let's search to be sure
                                var slayerFound = false
                                var slayerChild = -1
                                
                                // Search through child components (skills are typically 0-23)
                                for (i in 0..25) {
                                    val child = skillWidget.component(i)
                                    if (child.valid() && child.visible()) {
                                        val tooltip = child.tooltip()
                                        
                                        // Check if this is Slayer
                                        if (tooltip.contains("Slayer", ignoreCase = true)) {
                                            slayerChild = i
                                            slayerFound = true
                                            println("[STORE] ✓ Found Slayer at child index $i (tooltip: '$tooltip')")
                                            break
                                        }
                                    }
                                }
                                
                                if (!slayerFound) {
                                    // Fallback: Try index 18 directly (common Slayer position)
                                    println("[STORE] Trying fallback index 18...")
                                    slayerChild = 18
                                    slayerFound = skillWidget.component(18).valid()
                                }
                                
                                if (slayerFound) {
                                    val slayerComponent = skillWidget.component(slayerChild)
                                    println("[STORE] Clicking Slayer skill at index $slayerChild...")
                                    
                                    if (slayerComponent.interact("Select") || slayerComponent.click()) {
                                        println("[STORE] ✓ Slayer skill clicked!")
                                        Condition.sleep(Random.nextInt(1000, 1500))
                                        
                                        // Now find Confirm button (usually at the bottom of the widget)
                                        println("[STORE] Looking for Confirm button...")
                                        
                                        // Confirm button is usually a child component too
                                        var confirmClicked = false
                                        for (i in 26..35) {
                                            val confirmComponent = skillWidget.component(i)
                                            if (confirmComponent.valid() && confirmComponent.visible()) {
                                                val text = confirmComponent.text()
                                                if (text.contains("Confirm", ignoreCase = true)) {
                                                    println("[STORE] Found Confirm at index $i, clicking...")
                                                    if (confirmComponent.click()) {
                                                        println("[STORE] ✓ Confirm clicked!")
                                                        confirmClicked = true
                                                        break
                                                    }
                                                }
                                            }
                                        }
                                        
                                        if (!confirmClicked) {
                                            // Fallback: search all components
                                            val confirmComp = Components.stream()
                                                .widget(134)
                                                .text("Confirm")
                                                .first()
                                            
                                            if (confirmComp.valid() && confirmComp.click()) {
                                                println("[STORE] ✓ Confirm clicked (fallback method)!")
                                                confirmClicked = true
                                            }
                                        }
                                        
                                        if (confirmClicked) {
                                            Condition.sleep(Random.nextInt(2000, 3000))
                                            
                                            // Handle any follow-up dialog
                                            var confirmAttempts = 0
                                            while (Chat.canContinue() && confirmAttempts < 5) {
                                                Chat.clickContinue()
                                                Condition.sleep(Random.nextInt(800, 1200))
                                                confirmAttempts++
                                            }
                                            
                                            lampsUsed++
                                            slayerXpGained += 500
                                            
                                            println("========================================")
                                            println("[STORE] ✓✓✓ LAMP #$lampsUsed USED ON SLAYER (+500 XP)")
                                            println("[STORE] Total Slayer XP: $slayerXpGained")
                                            println("========================================")
                                        } else {
                                            println("[STORE] ⚠️ Could not find/click Confirm button")
                                        }
                                    } else {
                                        println("[STORE] ⚠️ Failed to click Slayer skill")
                                    }
                                } else {
                                    println("[STORE] ⚠️ Could not find Slayer in skill grid")
                                    println("[STORE] Dumping all visible children of widget 134:")
                                    for (i in 0..30) {
                                        val child = skillWidget.component(i)
                                        if (child.valid() && child.visible()) {
                                            println("[STORE]   Child $i: tooltip='${child.tooltip()}', text='${child.text()}'")
                                        }
                                    }
                                }
                            } else {
                                println("[STORE] ⚠️ Failed to rub lamp")
                            }
                        }
                        
                        println("[STORE] ========== ALL LAMPS USED ==========")
                    } else {
                        println("[STORE] ========== NO LAMPS FOUND ==========")
                        println("[STORE] Proceeding to drop remaining items...")
                    }
                    
                    // ALWAYS ensure inventory tab is open
                    if (Game.tab() != Game.Tab.INVENTORY) {
                        println("[STORE] Opening inventory tab...")
                        Game.tab(Game.Tab.INVENTORY)
                        Condition.sleep(Random.nextInt(500, 800))
                    }
                    
                    // Get ALL items except tools and gear
                    val remaining = Inventory.stream()
                        .filtered { item -> 
                            item.id() != C.TROWEL && 
                            item.id() != C.ROCK_PICK && 
                            item.id() != C.SPECIMEN_BRUSH &&
                            item.id() != C.LEATHER_GLOVES &&
                            item.id() != C.LEATHER_BOOTS
                        }
                        .toList()
                    
                    println("[STORE] ========== STARTING DROP PHASE ==========")
                    println("[STORE] Items to drop: ${remaining.size}")
                    
                    // RANDOMIZE drop order (more human-like!)
                    val shuffledItems = remaining.shuffled()
                    
                    println("[STORE] Dropping ${shuffledItems.size} items (tap-to-drop)...")
                    
                    // Just use tap-to-drop - slower, more human timing
                    for ((index, item) in shuffledItems.withIndex()) {
                        if (!item.valid()) {
                            continue
                        }
                        
                        println("[STORE] Drop #${index + 1}/${shuffledItems.size}: ${item.name()}")
                        
                        // Simple click - tap-to-drop must be enabled
                        item.click()
                        
                        // Slower, more human timing
                        val baseDelay = when {
                            index >= 4 -> Random.nextInt(150, 250)  // Comfortable rhythm
                            index >= 2 -> Random.nextInt(180, 280)  // Getting started
                            else -> Random.nextInt(200, 320)        // First few items
                        }
                        
                        // Occasional micro-hesitation (12% chance for more variation)
                        val hesitation = if (Random.nextInt(0, 100) < 12) {
                            Random.nextInt(80, 180)
                        } else {
                            0
                        }
                        
                        Condition.sleep(baseDelay + hesitation)
                        totalDropped++
                    }
                    
                    println("[STORE] ✓ Cleared inventory, ready to continue")
                    break
                }
            }
            
            val countAfter = Inventory.stream().id(*C.CLEANED_ARTIFACTS).count().toInt()
            val stored = countBefore - countAfter
            totalStored += stored
            
            val lampCount = Inventory.stream().id(*C.ANTIQUE_LAMP).count().toInt()
            
            if (lampCount > 0) {
                println("[STORE] ✓ Stored $stored items, got $lampCount lamp(s)!")
            } else {
                println("[STORE] ✓ Stored/dropped $stored items")
            }
            
            tripsCompleted++
        }
    }
    
    private fun leafCleanAll(): SimpleLeaf<VarrockMuseumCleaner> {
        return SimpleLeaf(this, "Clean all") {
            println("[CLEAN] Cleaning all (AFK)...")
            
            val countBefore = Inventory.stream().id(C.UNCLEANED_FIND).count().toInt()
            
            if (interactWithObject("Specimen table", "Clean")) {
                // Short wait for animation to start (reduced for faster transitions)
                Condition.sleep(Random.nextInt(1000, 1500))
                
                // Wait for animation to start
                Condition.wait({ Players.local().animation() != -1 }, 500, 10)
                
                println("[CLEAN] AFK cleaning in progress...")
                
                // Wait for all uncleaned to be gone (up to 4 minutes)
                Condition.wait({ 
                    Inventory.stream().id(C.UNCLEANED_FIND).isEmpty() 
                }, 2000, 120)
                
                // Short wait for last item to finish (reduced for faster transitions)
                Condition.sleep(Random.nextInt(1000, 1500))
                
                // Verify we have cleaned items now
                val cleanedCount = Inventory.stream().id(*C.CLEANED_ARTIFACTS).count().toInt()
                
                totalCleaned += countBefore
                println("[CLEAN] ✓ Cleaned $countBefore items → Got $cleanedCount cleaned artifacts")
            }
        }
    }
    
    private fun leafFillInventory(): SimpleLeaf<VarrockMuseumCleaner> {
        return SimpleLeaf(this, "Fill inventory") {
            println("[FILL] Filling inventory...")
            
            val beforeCount = Inventory.stream().id(C.UNCLEANED_FIND).count().toInt()
            
            if (interactWithObject("Dig Site specimen rocks", "Take")) {
                println("[FILL] Waiting for inventory to fill...")
                
                // Wait for inventory to actually be full (up to 60 seconds)
                Condition.wait({ Inventory.isFull() }, 1000, 60)
                
                // Extra safety check - make sure we have items
                Condition.wait({ Inventory.stream().id(C.UNCLEANED_FIND).count().toInt() > beforeCount }, 500, 20)
                
                val afterCount = Inventory.stream().id(C.UNCLEANED_FIND).count().toInt()
                println("[FILL] ✓ Got ${afterCount - beforeCount} finds (Inventory: ${Inventory.stream().count().toInt()}/28)")
            } else {
                println("[FILL] Failed to interact with rocks")
            }
        }
    }
    
    private fun leafIdle(): SimpleLeaf<VarrockMuseumCleaner> {
        return SimpleLeaf(this, "Idle") {
            println("[IDLE] Waiting...")
            Condition.sleep(2000)
        }
    }
}
