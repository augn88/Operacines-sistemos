package os;

import resource.memory.Memory;
import resource.memory.Word;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static resource.memory.Block.WORDS_PER_BLOCK;
import static resource.memory.Memory.TOTAL_MEMORY_BLOCKS;
import static resource.memory.Word.CHARS_PER_WORD;

public class RealMachine {
    public static final Pattern FILE_MEMORY_BLOCK_PATTERN = Pattern.compile("(?:(....)(?: (....))*)?");
    private final Memory memory = new Memory();
    private final Map<String, String> cpuRegisters = new HashMap<>();
    private final OS os;

    public RealMachine() {
        initRegisters();
        this.os = new OS(memory, cpuRegisters);
    }

    public void setVirtualMemory(int block, int blockOffset, Word word) {
        os.getCurrentVM().getBlock(block).writeWord(blockOffset, word);
    }



    public void printNextCommand() {;
        System.out.println(os.getCurrentVM().peekNextCommand());
    }

    public void doStep() {
        os.getCurrentVM().getVirtualProcessor().executeStep();
    }

    public void printUserMemory(){
        for (int i = 0; i < TOTAL_MEMORY_BLOCKS; i++) {
            System.out.print(i + ": ");
            for (int j = 0; j < WORDS_PER_BLOCK; j++) {
                System.out.print(memory.getBlock(i).readWord(j) + " ");
            }
            System.out.println();
        }
    }

    public void printVMMemory() {
        os.getCurrentVM().printMemory();
    }

    public String getRegister(String registerName){
        return os.getCurrentVM().getRegisters().get(registerName);
    }

    public void printRegisters(){
        System.out.println("SF: " + getRegister("SF") + "\t"
                + "PC: " + getRegister("PC") + "\t"
                + "A: " + getRegister("A") + "\t"
                + "B: " + getRegister("B") + "\t"
                + "PTR: " + getRegister("PTR"));

        System.out.println("MODE: " + getRegister("MODE") + "\t"
                + "PI: " + getRegister("PI") + "\t"
                + "SI: " + getRegister("SI") + "\t"
                + "TI: " + getRegister("TI") + "\t"
                + "IOI: " + getRegister("IOI"));
    }

    public void setVirtualMemoryFromFile(String filePath) {
        Path file = Paths.get(filePath);
        try {
            List<String> readLines = Files.readAllLines(file);
            setVmMemoryFromFileData(readLines);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setVmMemoryFromFileData(List<String> readLines) {
        VirtualMachine virtualMachine = os.getCurrentVM();
        if (readLines.size() > virtualMachine.getAmountOfBlocks()) {
            throw new IllegalStateException("Error while reading VM memory from file: more lines in file (" + readLines.size() + ") found than exists VM blocks (" + virtualMachine.getAmountOfBlocks() + ").");
        }

        for (int i = 0; i < readLines.size(); i++) {
            Matcher matcher = FILE_MEMORY_BLOCK_PATTERN.matcher(readLines.get(i));

            if (!matcher.matches()) {
                throw new IllegalStateException("Error while reading VM memory from file: expected to find words separated by space, but this pattern was not matched in " + (i + 1) + " line.");
            }

            String lineWithoutSpaces = readLines.get(i).replaceAll(" ", "");

            if (lineWithoutSpaces.length() > WORDS_PER_BLOCK * CHARS_PER_WORD) {
                throw new IllegalStateException("Error while reading VM memory from file: block size in " + (i + 1) + " line is bigger than defined by spec.");
            }

            virtualMachine.getBlock(i).writeBlock(lineWithoutSpaces);
        }
    }

    private void initRegisters() {
        cpuRegisters.put("SF", "0"); //status flag
        cpuRegisters.put("PC", "00"); // komandu skaitliukas
        cpuRegisters.put("A", "0000"); // bendr. pask. registras
        cpuRegisters.put("B", "0000"); // bendr. pask. registras
        cpuRegisters.put("PTR", "00"); // puslapiu lenteles adresu registras
        cpuRegisters.put("MODE", "0"); // registras nusakantis darbo režimą
        cpuRegisters.put("PI", "0"); // Programiniu pertraukimu registras
        cpuRegisters.put("SI", "0"); // supervizoriniu pertraukimu registras
        cpuRegisters.put("TI", "9"); // timerio pertraukimo registras
        cpuRegisters.put("IOI", "0"); // ivedimo/isvedimo/failu kanalu pertraukimu registras
        cpuRegisters.put("CHST[1]", "0");
        cpuRegisters.put("CHST[2]", "0");
        cpuRegisters.put("CHST[3]", "0");

    }

    public OS getOs() {
        return os;
    }
}
