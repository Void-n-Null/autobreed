package net.voidnull.autobreed.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/**
 * Memory-efficient storage of block positions relative to their chunk.
 * Stores x/z as 4 bits (0-15) and y as 9 bits (-64 to 320).
 * Total storage: 17 bits packed into a single int.
 */
public class ChunkRelativePos {
    private final int packedPos;
    
    private ChunkRelativePos(int x, int y, int z) {
        // Pack coordinates into a single int
        // x: 4 bits (0-15)
        // z: 4 bits (0-15)
        // y: 9 bits (-64 to 320)
        this.packedPos = (x & 0xF) | ((z & 0xF) << 4) | ((y + 64) << 8);
    }
    
    public static ChunkRelativePos fromBlockPos(BlockPos pos) {
        // Convert world coordinates to chunk-relative coordinates
        int x = pos.getX() & 0xF;  // Same as % 16
        int z = pos.getZ() & 0xF;
        return new ChunkRelativePos(x, pos.getY(), z);
    }
    
    public BlockPos toBlockPos(ChunkPos chunk) {
        // Convert back to world coordinates using chunk position
        int x = (packedPos & 0xF) + (chunk.x << 4);
        int y = ((packedPos >> 8) & 0x1FF) - 64;  // 9 bits for y
        int z = ((packedPos >> 4) & 0xF) + (chunk.z << 4);
        return new BlockPos(x, y, z);
    }
    
    public int getX() {
        return packedPos & 0xF;
    }
    
    public int getY() {
        return ((packedPos >> 8) & 0x1FF) - 64;
    }
    
    public int getZ() {
        return (packedPos >> 4) & 0xF;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkRelativePos)) return false;
        ChunkRelativePos that = (ChunkRelativePos) o;
        return packedPos == that.packedPos;
    }
    
    @Override
    public int hashCode() {
        return packedPos;
    }
} 