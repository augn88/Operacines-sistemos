package os;

import resource.memory.Block;
import resource.memory.Word;

import java.util.Map;

import static resource.memory.Block.WORDS_PER_BLOCK;
import static resource.memory.Word.CHARS_PER_WORD;
import static util.RegisterUtils.parseRegisterToInt;
import static util.RegisterUtils.toHexWithPadding;

public class VirtualMachine {
    private final Block[] blocks;
    private final Map<String, String> registers;
    private final VirtualProcessor virtualProcessor;
    private final OS os;

    public VirtualMachine(Map<String, String> registers, Block[] blocks, OS os) {
        this.registers = registers;
        this.blocks = blocks;
        this.virtualProcessor = new VirtualProcessor(this);
        this.os = os;
    }

    public VirtualProcessor getVirtualProcessor() {
        return virtualProcessor;
    }

    public Block getBlock(int index) {
        return blocks[index];
    }

    public int getAmountOfBlocks() {
        return blocks.length;
    }

    public String readCommandAtCommandCounter() {
        int commandAddress = parseRegisterToInt(registers.get("PC"));
        increaseCommandCounter();
        return getWordAtAddress(commandAddress).getWordData();
    }

    public String peekNextCommand() {
        int commandAddress = parseRegisterToInt(registers.get("PC"));
        return getWordAtAddress(commandAddress).getWordData();
    }

    public void increaseCommandCounter() {
        int commandAddress = parseRegisterToInt(registers.get("PC"));
        commandAddress++;
        setRegister("PC", toHexWithPadding(commandAddress, 4));
    }

    public Word getWordAtAddress(int address) {
        return getWordAtAddress(address / WORDS_PER_BLOCK, address % WORDS_PER_BLOCK);
    }

    public Word getWordAtAddress(int block, int offset) {
        Word word = blocks[block].readWord(offset);
        if (word != null) {
            return word;
        }
        throw new IllegalStateException("Nerastas žodis bloke: " + block + " poslinkis: " + offset);
    }

    public void setWordAtAddress(int block, int offset, Word word) {
       getBlock(block).writeWord(offset, word);
    }

    public void printMemory() {
        for (int i = 0; i < getAmountOfBlocks(); i++) {
            System.out.print(i + ": ");
            for (int j = 0; j < WORDS_PER_BLOCK; j++) {
                System.out.print(blocks[i].readWord(j) + " ");
            }
            System.out.println();
        }
    }

    public char getCharAtAbsolutePosition(int position) {
        return getWordAtAddress(position / CHARS_PER_WORD)
            .getWordData()
            .charAt(position % CHARS_PER_WORD);
    }

    public Map<String, String> getRegisters() {
        return registers;
    }

    public String getRegister(String registerName) {
        return registers.get(registerName);
    }

    public void setRegister(String registerName, String registerValue) {
        registers.replace(registerName, registerValue);
    }

    public OS getOs() {
        return os;
    }
}
