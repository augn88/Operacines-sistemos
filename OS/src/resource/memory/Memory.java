package resource.memory;

import resource.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class Memory extends Resource {
    public static final int TOTAL_MEMORY_BLOCKS = 50;
    private final Block[] allMemory = new Block[TOTAL_MEMORY_BLOCKS];

    public Memory() {
        super("User memory");
        IntStream.range(0, TOTAL_MEMORY_BLOCKS)
            .forEach(i -> allMemory[i] = new Block(i));
    }

    public Block[] askForAResource(int amountOfBlocks) {
        List<Block> freeBlocks = getAllFreeUnorderedBlocks();

        if (freeBlocks.size() < amountOfBlocks) {
            // should be put in a waiting queue for a resource
            throw new IllegalStateException("Nepakanka atminties resursu");
        } else {
            List<Block> reservedBlocks = freeBlocks.subList(0, amountOfBlocks);
            reservedBlocks.forEach(Block::reserve);
            return reservedBlocks.toArray(new Block[amountOfBlocks]);
        }
    }

    public void freeResource(List<Block> blocks) {
        blocks.forEach(Block::free);
    }

    public Block getBlock(int index) {
        return allMemory[index];
    }

    private List<Block> getAllFreeUnorderedBlocks() {
        List<Block> blocks = new ArrayList<>(Arrays.stream(allMemory)
            .filter(Block::isFree)
            .toList());
        Collections.shuffle(blocks);
        return blocks;
    }
}
