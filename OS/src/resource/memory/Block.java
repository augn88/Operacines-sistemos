package resource.memory;

public class Block {
    public static final int WORDS_PER_BLOCK = 16;
    private final Word[] memory = new Word[WORDS_PER_BLOCK];
    private boolean isFree = true;
    private final int address;

    public Block(int address) {
        this.address = address;
        for (int i = 0; i < memory.length; i++) {
            memory[i] = new Word(0);
        }
    }

    public void writeWord(int index, Word word) {
        memory[index] = word;
    }

    public Word readWord(int index) {
        return memory[index];
    }

    public void writeBlock(String string) {
        Word[] toWrite = Word.convertFromString(string);

        for (int i = 0; i < toWrite.length; i++) {
            memory[i] = toWrite[i];
        }
        for (int i = toWrite.length; i < WORDS_PER_BLOCK; i++) {
            memory[i] = new Word(0);
        }
    }

    public void reserve() {
        if (!isFree) {
            throw new IllegalStateException("Block is already reserved");
        }
        isFree = false;
    }

    public void free() {
        if (isFree) {
            throw new IllegalStateException("Block is already free");
        }
        isFree = true;
    }

    public boolean isReserved() {
        return !isFree;
    }

    public boolean isFree() {
        return isFree;
    }

    public int getAddress() {
        return address;
    }
}
