package os;

import process.Process;
import process.StartStop;
import resource.Resource;
import resource.channel.ChannelDevice;
import resource.memory.Memory;
import resource.memory.PagesTable;
import resource.memory.PagingDevice;
import resource.memory.Word;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;
import static os.VirtualProcessorInstructions.DATA_SEGMENT_START_BLOCK;
import static process.ProcessState.*;
import static resource.memory.Block.WORDS_PER_BLOCK;
import static resource.memory.Word.CHARS_PER_WORD;
import static util.RegisterUtils.parseRegisterToInt;
import static util.RegisterUtils.toHexWithPadding;

public class OS {
    private final PagingDevice pagingDevice;
    private final Map<String, String> cpuRegisters;
    private final Map<Integer, FileDescriptorInfo> openFileDescriptors = new HashMap<>();
    private VirtualMachine currentVM;
    private ChannelDevice channelDevice;
    private final Map<String, Resource> dynamicResources = new HashMap<>();
    private final List<Process> allProcesses = new ArrayList<>();
    private final PriorityQueue<Process> blockedProcesses = new PriorityQueue<>();
    private final PriorityQueue<Process> readyProcesses = new PriorityQueue<>();
    private Process currentProcess;

    public OS(Memory memory, Map<String, String> cpuRegisters) {
        this.pagingDevice = new PagingDevice(memory);
        this.cpuRegisters = cpuRegisters;
        channelDevice = new ChannelDevice(cpuRegisters, this);
        createVM();
    }

    public void boot() {
        StartStop startStop = new StartStop();
        allProcesses.add(startStop);
        moveProcessToReadyQueue(startStop);
    }

    public void run(){
        Process process = currentProcess;
        if(process == null){
            System.out.println("no processes are running");
        }
        else{
            process.execute(this);
        }
    }

    public void askForMessageResource(Process askingProcess, String messageName, Consumer<Object> consumer) {

        Resource resource = dynamicResources.get(messageName);
        if (resource == null) {
            askingProcess.setState(BLOCKED);
            planner();
        }

        if (consumer != null) {
            consumer.accept(null);
        }
        dynamicResources.remove(messageName);
    }

    public void createResource(Process owner, Resource resource) {
        System.out.println(resource.getName() + " resource created");
        resource.setCreator(owner);
        owner.addResource(resource);
        dynamicResources.put(resource.getName(), resource);
    }

    public void runProcess(Process process) {
        System.out.println("running process " + process.getName());
        process.setState(RUNNING);
        process.execute(this);
    }

    public void planner() {
        Process highestPriorityReadyProcess = readyProcesses.peek();

        if (currentProcess == null) {
            setHighestPriorityReadyProcessToRun();
        } else if (currentProcess.getState() == BLOCKED) {
            System.out.println(currentProcess.getName() + " is blocked");
            blockedProcesses.add(currentProcess);
            setHighestPriorityReadyProcessToRun();
        } else if (highestPriorityReadyProcess != null && highestPriorityReadyProcess.getPriority() > currentProcess.getPriority()) {
            moveProcessToReadyQueue(currentProcess);
            setHighestPriorityReadyProcessToRun();
        }

        if (currentProcess.getClass().equals(VirtualMachine.class)) {
            setRegister("MODE", "0");
        } else {
            setRegister("MODE", "1");
        }

        runProcess(currentProcess);
    }

    private void moveProcessToReadyQueue(Process process) {
        process.setState(READY);
        readyProcesses.add(process);
    }

    private void setHighestPriorityReadyProcessToRun() {
        currentProcess = requireNonNull(readyProcesses.poll(), "Klaida! Nėra pasiruošusių procesų.");;
        currentProcess.setState(RUNNING);
    }

    public void createProcess(Process parentProcess, Process createdProcess) {
        allProcesses.add(createdProcess);
        createdProcess.setState(READY);
        readyProcesses.add(createdProcess);

        if (parentProcess != null) {
            parentProcess.getChild().add(createdProcess);
            createdProcess.setParent(parentProcess);
        }
        System.out.println(createdProcess.getName() + " process created");
    }

    private void processInterrupt() {
        if ("1".equals(cpuRegisters.get("SI"))) { //INPT
            String value = channelDevice.receiveFromKeyboard();
            if(value.length() == 4){
                cpuRegisters.replace("PI", "3");
                return;
            }
            cpuRegisters.replace("IOI", toHexWithPadding(Integer.valueOf(cpuRegisters.get("IOI"), 16) + 1, 1));
            cpuRegisters.replace("A", value);
        }
        if ("2".equals(cpuRegisters.get("SI"))) { //OUTA
            channelDevice.sendToDisplay(cpuRegisters.get("A"));
        }
        if ("3".equals(cpuRegisters.get("SI"))) { //OUTS
            int howManyBytes = Integer.valueOf(cpuRegisters.get("A"), 16);
            int block = Integer.parseInt(cpuRegisters.get("B").substring(3,4));
            int word = Integer.parseInt(cpuRegisters.get("B").substring(2,3));
            int offsetInWord = Integer.parseInt(cpuRegisters.get("B").substring(1,2));

            int absStartPosition = block * WORDS_PER_BLOCK * CHARS_PER_WORD + word * CHARS_PER_WORD + offsetInWord;
            int absEndPosition = absStartPosition + howManyBytes;

            String result = IntStream.range(absStartPosition, absEndPosition)
                    .mapToObj(getCurrentVM()::getCharAtAbsolutePosition)
                    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                    .toString();
            cpuRegisters.replace("IOI", toHexWithPadding(Integer.valueOf(cpuRegisters.get("A"), 16) + 2 , 1));
            channelDevice.sendToDisplay(result);
        }
        if ("4".equals(cpuRegisters.get("SI"))) { // OPEN
            int x = Integer.valueOf(cpuRegisters.get("A"), 16);
            int y = Integer.valueOf(cpuRegisters.get("B"), 16);
            try (FileReader file = new FileReader("src/external_device.txt");
                 BufferedReader input = new BufferedReader(file)
            ) {
                for(int i = 0; i < (16 * x) + y; i++) {
                    input.readLine();
                }
                for (int i = 1; i <= 4; i++) {
                    if (input.read() != '-') {
                        setRegister("PI", "5");
                    }
                }

                String fileName = "";
                for (int i = 1; i <= 4; i++) {
                    int readCharacter = input.read();

                    if (readCharacter == -1) {
                        setRegister("PI", "5");
                    }

                    fileName += (char) readCharacter;
                }

                FileDescriptorInfo fileDescriptorInfo = new FileDescriptorInfo(fileName, x, y);
                fileDescriptorInfo.setReadBytes(8);
                if(currentVM.getOs().getOpenFileDescriptors().containsKey((16 * x +y))){
                    setRegister("PI", "5");
                    return;
                }
                int fileDescriptor = currentVM.getOs().createFileDescriptor(fileDescriptorInfo, (16 * x +y));

                setRegister("A", toHexWithPadding(fileDescriptor, 4));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        if ("5".equals(cpuRegisters.get("SI"))) { //CLOSE
            if (!currentVM.getOs().getOpenFileDescriptors().containsKey(getRegister("A"))){
                setRegister("PI", "5");
                return;
            }
            currentVM.getOs().getOpenFileDescriptors().remove(getRegister("A"));
        }
        if ("6".equals(cpuRegisters.get("SI"))) { //DELETE
            int descriptor = getRegister("A");
            channelDevice.deleteFile(descriptor);

            setRegister("IOI", toHexWithPadding(getRegister("IOI") + 4, 1));

        }
        if ("7".equals(cpuRegisters.get("SI"))) { //READ
            int bytesToRead = Integer.valueOf(cpuRegisters.get("B"), 16);
            int descriptor = getRegister("A");
            FileDescriptorInfo fileDescriptorInfo = currentVM.getOs().getOpenFileDescriptors().get(descriptor);
            int numberOfCharactersToSkip = (fileDescriptorInfo.getFileStartBlock() * WORDS_PER_BLOCK * CHARS_PER_WORD)
                    + (fileDescriptorInfo.getFileStartBlockOffset() * CHARS_PER_WORD)
                    + fileDescriptorInfo.getReadBytes();
            String[] splitGottenCharacters = channelDevice.readFile(descriptor, bytesToRead, numberOfCharactersToSkip);
            for(int i = 0; i < splitGottenCharacters.length; i++){
                currentVM.getBlock(DATA_SEGMENT_START_BLOCK).writeWord(i, new Word(splitGottenCharacters[i]));
            }
        }
        if ("8".equals(cpuRegisters.get("SI"))) { //WRITE
            String bytesToRead = getRegisterString("A").substring(0,2);
            int descriptor = Integer.parseInt(getRegisterString("A").substring(2,4), 16);
            int fileOffset = Integer.parseInt(getRegisterString("B").substring(0,1)) * WORDS_PER_BLOCK
                    + Integer.parseInt(getRegisterString("B").substring(1,2)) * 4 + 8;
            int dataSegmentOffsetWord = Integer.parseInt(getRegisterString("B").substring(3,4));
            int dataSegmentOffsetBlock = Integer.parseInt(getRegisterString("B").substring(2,3));
            String contentToWrite = new String();
            int fullWords = Integer.parseInt(bytesToRead) / 4;
            int nonFullWord = Integer.parseInt(bytesToRead) % 4;

            for (int i = 0; i < fullWords; i++) {
                contentToWrite += String.valueOf(currentVM.getWordAtAddress(8+dataSegmentOffsetBlock,dataSegmentOffsetWord));
            }

            String tempNonFull = new String();
            if (nonFullWord != 0) {
                tempNonFull = String.valueOf(currentVM.getWordAtAddress(8+dataSegmentOffsetBlock,dataSegmentOffsetWord + fullWords));

                for(int i = 0; i < 4; i++) {
                    if(i < nonFullWord) {
                        contentToWrite += tempNonFull.charAt(i);
                    }
                    if(i >= nonFullWord) {
                        contentToWrite += " ";
                    }
                }
            }
            channelDevice.writeFile(descriptor, fileOffset, contentToWrite);

        }
        if ("9".equals(cpuRegisters.get("SI"))) { //HALT
            System.out.println("Darbas sustabdytas");
            currentVM.getVirtualProcessor().halt();
        }

    }

    private String getRegisterString(String registerName) {
        return currentVM.getRegister(registerName);
    }
    private int getRegister(String registerName) {
        return parseRegisterToInt(currentVM.getRegister(registerName));
    }
    private void setRegister(String registerName, String registerValue) {
        currentVM.setRegister(registerName, registerValue);
    }

    public Integer createFileDescriptor(FileDescriptorInfo fileDescriptorInfo, int LineNumber) {
        int assumedFreeFileDescriptorNumber = LineNumber;

        Set<Integer> existingFileDescriptors = openFileDescriptors.keySet();
        while (existingFileDescriptors.contains(assumedFreeFileDescriptorNumber)) {
            assumedFreeFileDescriptorNumber++;
        }

        openFileDescriptors.put(assumedFreeFileDescriptorNumber, fileDescriptorInfo);

        return assumedFreeFileDescriptorNumber;
    }

    public Map<Integer, FileDescriptorInfo> getOpenFileDescriptors() {
        return openFileDescriptors;
    }

    public VirtualMachine getCurrentVM() {
        return currentVM;
    }

    private void createVM() {
        PagesTable pagesTable = pagingDevice.addPagesTable();
        cpuRegisters.replace("PTR", toHexWithPadding(pagesTable.getAddress(), 2));

        currentVM = new VirtualMachine(cpuRegisters, pagesTable.getReservedBlocksForVM(), this);
    }
}
