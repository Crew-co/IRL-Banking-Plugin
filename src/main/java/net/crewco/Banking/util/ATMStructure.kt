// src/main/java/net/crewco/Banking/util/ATMStructure.kt
package net.crewco.Banking.util

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign

object ATMStructure {

    // ATM Structure:
    // A sign attached to a block (iron block, quartz, etc.)
    // Sign must have "[ATM]" on the first line

    private val VALID_ATM_BASE_BLOCKS = setOf(
        Material.IRON_BLOCK,
        Material.QUARTZ_BLOCK,
        Material.SMOOTH_QUARTZ,
        Material.POLISHED_ANDESITE,
        Material.POLISHED_DIORITE,
        Material.POLISHED_GRANITE,
        Material.POLISHED_BLACKSTONE,
        Material.POLISHED_DEEPSLATE
    )

    private val VALID_SIGN_TYPES = setOf(
        Material.OAK_WALL_SIGN,
        Material.SPRUCE_WALL_SIGN,
        Material.BIRCH_WALL_SIGN,
        Material.JUNGLE_WALL_SIGN,
        Material.ACACIA_WALL_SIGN,
        Material.DARK_OAK_WALL_SIGN,
        Material.CRIMSON_WALL_SIGN,
        Material.WARPED_WALL_SIGN,
        Material.MANGROVE_WALL_SIGN,
        Material.CHERRY_WALL_SIGN,
        Material.BAMBOO_WALL_SIGN
    )

    /**
     * Checks if a block is part of an ATM structure
     */
    fun isATMSign(block: Block): Boolean {
        // Must be a wall sign
        if (!VALID_SIGN_TYPES.contains(block.type)) return false

        val sign = block.state as? Sign ?: return false

        // Check if sign has [ATM] on first line
        val line1 = sign.getSide(org.bukkit.block.sign.Side.FRONT).line(0)
        val plainText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line1)

        if (!plainText.equals("[ATM]", ignoreCase = true)) return false

        // Check if attached to a valid base block
        val attachedBlock = getAttachedBlock(block) ?: return false
        return VALID_ATM_BASE_BLOCKS.contains(attachedBlock.type)
    }

    /**
     * Gets the block a wall sign is attached to
     */
    fun getAttachedBlock(signBlock: Block): Block? {
        val blockData = signBlock.blockData as? WallSign ?: return null
        val facing = blockData.facing
        return signBlock.getRelative(facing.oppositeFace)
    }

    /**
     * Gets the ATM location (the base block location, not the sign)
     */
    fun getATMBaseLocation(signBlock: Block): org.bukkit.Location? {
        if (!isATMSign(signBlock)) return null
        return getAttachedBlock(signBlock)?.location
    }

    /**
     * Checks if a block is the base block of an ATM (has ATM sign attached)
     */
    fun isATMBaseBlock(block: Block): Boolean {
        if (!VALID_ATM_BASE_BLOCKS.contains(block.type)) return false

        // Check all faces for an ATM sign
        for (face in listOf(
            org.bukkit.block.BlockFace.NORTH,
            org.bukkit.block.BlockFace.SOUTH,
            org.bukkit.block.BlockFace.EAST,
            org.bukkit.block.BlockFace.WEST
        )) {
            val adjacentBlock = block.getRelative(face)
            if (VALID_SIGN_TYPES.contains(adjacentBlock.type)) {
                val blockData = adjacentBlock.blockData as? WallSign
                if (blockData?.facing?.oppositeFace == face) {
                    if (isATMSign(adjacentBlock)) return true
                }
            }
        }
        return false
    }

    /**
     * Formats a sign as an ATM display
     */
    fun formatATMSign(sign: Sign, atmId: String?) {
        val side = sign.getSide(org.bukkit.block.sign.Side.FRONT)

        side.line(0, net.kyori.adventure.text.Component.text("[ATM]")
            .color(net.kyori.adventure.text.format.NamedTextColor.DARK_BLUE)
            .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD))

        side.line(1, net.kyori.adventure.text.Component.text("━━━━━━━━")
            .color(net.kyori.adventure.text.format.NamedTextColor.GOLD))

        side.line(2, net.kyori.adventure.text.Component.text("CrewCo Bank")
            .color(net.kyori.adventure.text.format.NamedTextColor.GREEN))

        side.line(3, net.kyori.adventure.text.Component.text("Use Card →")
            .color(net.kyori.adventure.text.format.NamedTextColor.GRAY))

        sign.isWaxed = true // Prevent editing
        sign.update()
    }
}