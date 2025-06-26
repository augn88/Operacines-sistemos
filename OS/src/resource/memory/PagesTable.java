package resource.memory;

import static resource.memory.Block.WORDS_PER_BLOCK;

public class PagesTable {
    private Block pagingTableBlock;
    private Block[] reservedBlocksForVM;

    public PagesTable(Memory memory) {
        reservePages(memory);
    }

    public int getAddress() {
        return pagingTableBlock.getAddress();
    }

    public Block[] getReservedBlocksForVM() {
        return reservedBlocksForVM;
    }

    private void reservePages(Memory memory) {
        pagingTableBlock = memory.askForAResource(1)[0];
        reservedBlocksForVM = memory.askForAResource(WORDS_PER_BLOCK);

        for (int i = 0; i < WORDS_PER_BLOCK; i++) {
            pagingTableBlock.writeWord(i, new Word(reservedBlocksForVM[i].getAddress()));
        }
    }
}
