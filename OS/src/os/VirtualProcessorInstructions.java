package os;

import resource.memory.Word;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static resource.memory.Block.WORDS_PER_BLOCK;
import static resource.memory.Word.CHARS_PER_WORD;
import static util.RegisterUtils.parseRegisterToInt;
import static util.RegisterUtils.toHexWithPadding;
import static util.StringUtils.splitStringEveryNCharacters;

public class VirtualProcessorInstructions {
    private final VirtualProcessor virtualProcessor;

    private static final Set<String> FOUR_CHARS_INSTRUCTIONS = Set.of("SWAP","DLTE", "INPT", "ADIT", "SBTR", "MLTP", "DIVD", "CMPR", "OUTA", "OUTS", "CLOZ", "RITE", "HALT");
    private static final Set<String> THREE_CHARS_INSTRUCTIONS = Set.of("RYD");
    private static final Set<String> TWO_CHARS_INSTRUCTIONS = Set.of("LD", "ST", "JM", "JE", "JN", "JA", "JL", "OP");

    public static final int DATA_SEGMENT_START_BLOCK = 8;
    public VirtualProcessorInstructions(VirtualProcessor virtualProcessor) {
        this.virtualProcessor = virtualProcessor;
    }

    public void executeInstruction(String instruction) {
        if (FOUR_CHARS_INSTRUCTIONS.contains(instruction)) {
            callInstruction(instruction);
        }
        if (THREE_CHARS_INSTRUCTIONS.contains(instruction.substring(0, 3))) {
            callInstruction(instruction.substring(0, 3), parseRegisterToInt(instruction.substring(3, 4)));
        }
        if (TWO_CHARS_INSTRUCTIONS.contains(instruction.substring(0, 2))) {
            callInstruction(instruction.substring(0, 2), parseRegisterToInt(instruction.substring(2, 3)), parseRegisterToInt(instruction.substring(3, 4)));
        }
    }

    public void ADIT() {
        int a = getRegister("A");
        int b = getRegister("B");

        if (a >= 0x8000) {
            a -= 0x10000;
        }
        if (b >= 0x8000) {
            b -= 0x10000;
        }

        int result = a + b;
        setRegister("A", result & 0xFFFF, 4);

        boolean zeroFlag = (result & 0xFFFF) == 0;
        boolean signFlag = (result & 0x8000) != 0;
        boolean carryFlag = ((a & 0xFFFF) + (b & 0xFFFF)) > 0xFFFF;
        boolean overflowFlag = (~(a ^ b) & (a ^ result) & 0x8000) != 0;

        int statusFlag = toNumber(signFlag) << 3 + toNumber(zeroFlag) << 2 + toNumber(carryFlag) << 2 + toNumber(overflowFlag);
        setRegister("SF", statusFlag, 1);
        setRegister("SI", "");
    }

    public void SBTR() {
        int a = getRegister("A");
        int b = getRegister("B");

        if (a >= 0x8000) {
            a -= 0x10000;
        }
        if (b >= 0x8000) {
            b -= 0x10000;
        }
        int result = a - b;
        setRegister("A", result & 0xFFFF, 4);

        boolean signFlag = (result & 0x8000) != 0;
        boolean zeroFlag = (result & 0xFFFF) == 0;
        boolean carryFlag = (a & 0xFFFF) < (b & 0xFFFF);
        boolean overflowFlag = ((a ^ b) & (a ^ result) & 0x8000) != 0;

        int statusFlag = toNumber(signFlag) << 3 + toNumber(zeroFlag) << 2 + toNumber(carryFlag) << 2 + toNumber(overflowFlag);
        setRegister("SF", statusFlag, 1);
    }

    public void DIVD() {
        int a = getRegister("A");
        int b = getRegister("B");

        boolean overflowFlag =(a == 0x8000) && (b == 0xFFFF);

        if (a >= 0x8000) {
            a -= 0x10000;
        }
        if (b >= 0x8000) {
            b -= 0x10000;
        }

        if (b == 0) {
            boolean zeroFlag = false;
            boolean signFlag = false;
            boolean carryFlag = true;
            overflowFlag = false;
            int statusFlag = toNumber(signFlag) << 3 + toNumber(zeroFlag) << 2 + toNumber(carryFlag) << 2 + toNumber(overflowFlag);
            setRegister("SF", statusFlag, 1);
            setRegister("PI", "4");
            return;
        }

        int result = a / b;

        setRegister("A", result & 0xFFFF, 4);

        boolean zeroFlag = (result & 0xFFFF) == 0;
        boolean signFlag = (result & 0x8000) != 0;
        boolean carryFlag = false;

        int statusFlag = toNumber(signFlag) << 3 + toNumber(zeroFlag) << 2 + toNumber(carryFlag) << 2 + toNumber(overflowFlag);
        setRegister("SF", statusFlag, 1);
    }
    public void MLTP() {
        int a = getRegister("A");
        int b = getRegister("B");

        if (a >= 0x8000) {
            a -= 0x10000;
        }
        if (b >= 0x8000) {
            b -= 0x10000;
        }
        int result = a * b;
        setRegister("A", result & 0xFFFF, 4);


        boolean signFlag = (result & 0x8000) != 0;
        boolean carryFlag = result > 0xFFFF;
        boolean overflowFlag = (result > 0x7FFF) || (result < -0x8000);
        boolean zeroFlag = (result & 0xFFFF) == 0;

        int statusFlag = toNumber(signFlag) << 3 + toNumber(zeroFlag) << 2 + toNumber(carryFlag) << 2 + toNumber(overflowFlag);
        setRegister("SF", statusFlag, 1);
    }

    public void CMPR() {
        int a = getRegister("A");
        int b = getRegister("B");


        if (a >= 0x8000) {
            a -= 0x10000;
        }
        if (b >= 0x8000) {
            b -= 0x10000;
        }

        int diff = (a - b) & 0xFFFF;

        boolean signFlag     = (diff & 0x8000) != 0;
        boolean zeroFlag     = diff == 0;
        boolean carryFlag    = ( (a & 0xFFFF) < (b & 0xFFFF) );
        boolean overflowFlag = ((a ^ b) & (a ^ diff) & 0x8000) != 0;


        int statusFlag = toNumber(signFlag) << 3 + toNumber(zeroFlag) << 2 + toNumber(carryFlag) << 2 + toNumber(overflowFlag);
        setRegister("SF", statusFlag, 1);
    }

    public void SWAP() {
        String temp = getRegisterString("B");
        String temp1 = getRegisterString("A");
        setRegister("B", temp1);
        setRegister("A", temp);
    }
    public void LD(int x, int y) {
        setRegister("A", virtualProcessor.getVirtualMachine().getWordAtAddress(x, y).getWordData());
    }


    public void ST(int x, int y) {
        virtualProcessor.getVirtualMachine().setWordAtAddress(x, y, new Word(getRegister("A")));
    }

    public void JM(int x, int y) {
        setRegister("PC", (WORDS_PER_BLOCK * x) + y, 2);
    }

    public void JE(int x, int y) {
        int sf = getRegister("SF");
        if ((sf & 0b0100) > 0) {
            setRegister("PC", (WORDS_PER_BLOCK * x) + y, 2);
        }
    }

    public void JL(int x, int y) {
        int sf = getRegister("SF");
        if ((sf & 0b1000) > 0 != (sf & 0b0001) > 0) {
            setRegister("PC", (WORDS_PER_BLOCK * x) + y, 2);
        }
    }

    public void JN(int x, int y) {
        int sf = getRegister("SF");
        if ((sf & 0b0100) > 0) {
            setRegister("PC", (WORDS_PER_BLOCK * x) + y, 2);
        }
    }

    public void JA(int x, int y) {
        int sf = getRegister("SF");
        if ((sf & 0b0110) == 0){
            setRegister("PC", (WORDS_PER_BLOCK * x) + y, 2);
        }
    }
    public void INPT() {
        setRegister("SI", "1");
    }

    public void OUTA() {
        setRegister("SI", "2");
        int a = getRegister("A");
        if (a >= 0x8000) {
            a -= 0x10000;
        }
        setRegister("IOI", toHexWithPadding(getRegister("IOI") + 2, 1));
//        System.out.println("Reiksme: " + a);

    }

    public void OUTS() {
        int howManyBytes = getRegister("A");
        int block = Integer.parseInt(getRegisterString("B").substring(3,4));
        int word = Integer.parseInt(getRegisterString("B").substring(2,3));
        int offsetInWord = Integer.parseInt(getRegisterString("B").substring(1,2));

        int absStartPosition = block * WORDS_PER_BLOCK * CHARS_PER_WORD + word * CHARS_PER_WORD + offsetInWord;
        int absEndPosition = absStartPosition + howManyBytes;

        if(absEndPosition > 255 || absEndPosition < 0 || absStartPosition > 255 || absStartPosition < 0 ){
            setRegister("PI", "3");
            return;
        }
        setRegister("SI", "3");
    }

    public void OP(int x, int y) {
        setRegister("SI", "4");
        setRegister("A", toHexWithPadding(x, 4));
        setRegister("B", toHexWithPadding(x, 4));
        setRegister("IOI", toHexWithPadding(getRegister("IOI") + 4, 1));

//        setRegister("SI", "4");
//        try (FileReader file = new FileReader("src/external_device.txt");
//             BufferedReader input = new BufferedReader(file)
//        ) {
//            for(int i = 0; i < (16 * x) + y; i++) {
//                input.readLine();
//            }
//            for (int i = 1; i <= 4; i++) {
//                if (input.read() != '-') {
//                    // todo: set error status: file not found
//                    setRegister("PI", "5");
//                }
//            }
//
//            String fileName = "";
//            for (int i = 1; i <= 4; i++) {
//                int readCharacter = input.read();
//
//                if (readCharacter == -1) {
//                    // todo: set error status: file not found
//                    setRegister("PI", "5");
//                }
//
//                fileName += (char) readCharacter;
//            }
//
//            FileDescriptorInfo fileDescriptorInfo = new FileDescriptorInfo(fileName, x, y);
//            fileDescriptorInfo.setReadBytes(8);
//            if(virtualProcessor.getVirtualMachine().getOs().getOpenFileDescriptors().containsKey((16 * x +y))){
//                setRegister("PI", "5");
//                return;
//            }
//            int fileDescriptor = virtualProcessor.getVirtualMachine().getOs().createFileDescriptor(fileDescriptorInfo, (16 * x +y));
//
//            setRegister("A", toHexWithPadding(fileDescriptor, 4));
//        } catch (IOException e) {
//            throw new IllegalStateException(e);
//        }
    }

    public void CLOZ () {
        setRegister("SI", "5");
//        if (!virtualProcessor.getVirtualMachine().getOs().getOpenFileDescriptors().containsKey(getRegister("A"))){
//            setRegister("PI", "5");
//            return;
//        }
//        virtualProcessor.getVirtualMachine().getOs().getOpenFileDescriptors().remove(getRegister("A"));
        setRegister("IOI", toHexWithPadding(getRegister("IOI") + 4, 1));
    }

    public void RYD(int bytesToRead){
        setRegister("SI", "7");
        setRegister("B", toHexWithPadding(bytesToRead, 4));
//        int descriptor = getRegister("A");
//        FileDescriptorInfo fileDescriptorInfo = virtualProcessor.getVirtualMachine().getOs().getOpenFileDescriptors().get(descriptor);
//        try (FileReader file = new FileReader("src/external_device.txt");
//             BufferedReader input = new BufferedReader(file)
//        ) {
//            for(int i = 0; i < descriptor - 1; i++) {
//                input.readLine();
//            }
//            char[] buffer = new char[bytesToRead];
//            int numberOfCharactersToSkip = (fileDescriptorInfo.getFileStartBlock() * WORDS_PER_BLOCK * CHARS_PER_WORD)
//                + (fileDescriptorInfo.getFileStartBlockOffset() * CHARS_PER_WORD)
//                + fileDescriptorInfo.getReadBytes();
//            fileDescriptorInfo.setReadBytes(fileDescriptorInfo.getReadBytes() + bytesToRead);
//            for (int i = 1; i <= numberOfCharactersToSkip; i++) {
//                int status = input.read();
//            }
//
//            for (int i = 0; i < bytesToRead; i++) {
//                int readCharacter = input.read();
//
//                if (readCharacter == -1) {
//                    setRegister("PI", "5");
//                    return;
//                }
//
//                buffer[i] = (char) readCharacter;
//            }
//
//            String gottenCharacters = new String(buffer, 0, bytesToRead);
//
//            String[] splitGottenCharacters = splitStringEveryNCharacters(gottenCharacters, CHARS_PER_WORD);
//            while(splitGottenCharacters[splitGottenCharacters.length-1].length() < 4){
//                splitGottenCharacters[splitGottenCharacters.length-1] += "0";
//            }
//            for(int i = 0; i < splitGottenCharacters.length; i++){
//                virtualProcessor.getVirtualMachine().getBlock(DATA_SEGMENT_START_BLOCK).writeWord(i, new Word(splitGottenCharacters[i]));
//            }
            setRegister("IOI", toHexWithPadding(getRegister("IOI") + 4, 1));
//        } catch (IOException e) {
//            throw new IllegalStateException(e);
//        }

    }

    public void RITE() {
        setRegister("SI", "8");
//        String bytesToRead = getRegisterString("A").substring(0,2);
//        int descriptor = Integer.parseInt(getRegisterString("A").substring(2,4), 16);
//        int fileOffset = Integer.parseInt(getRegisterString("B").substring(0,1)) * WORDS_PER_BLOCK
//                + Integer.parseInt(getRegisterString("B").substring(1,2)) * 4 + 8;
//        int dataSegmentOffsetWord = Integer.parseInt(getRegisterString("B").substring(3,4));
//        int dataSegmentOffsetBlock = Integer.parseInt(getRegisterString("B").substring(2,3));
//        String contentToWrite = new String();
//        int fullWords = Integer.parseInt(bytesToRead) / 4;
//        int nonFullWord = Integer.parseInt(bytesToRead) % 4;
//
//        for (int i = 0; i < fullWords; i++) {
//            contentToWrite += String.valueOf(virtualProcessor.getVirtualMachine().getWordAtAddress(8+dataSegmentOffsetBlock,dataSegmentOffsetWord));
//        }
//
//        String tempNonFull = new String();
//        if (nonFullWord != 0) {
//            tempNonFull = String.valueOf(virtualProcessor.getVirtualMachine().getWordAtAddress(8+dataSegmentOffsetBlock,dataSegmentOffsetWord + fullWords));
//
//            for(int i = 0; i < 4; i++) {
//                if(i < nonFullWord) {
//                 contentToWrite += tempNonFull.charAt(i);
//             }
//             if(i >= nonFullWord) {
//                  contentToWrite += " ";
//             }
//         }
//        }
//
//        try (RandomAccessFile file = new RandomAccessFile("src/external_device.txt", "rw");){
//            for(int i = 0; i < descriptor - 1; i++) {
//                file.readLine();
//            }
//            file.seek(fileOffset);
//            byte[] bytesToWrite = contentToWrite.getBytes(StandardCharsets.UTF_8);
//            file.write(bytesToWrite);
            setRegister("IOI", toHexWithPadding(getRegister("IOI") + 4, 1));
//        } catch (IOException e) {
//            setRegister("PI", "5");
//            throw new IllegalStateException(e);
//        }
    }

    public void DLTE() {
        setRegister("SI", "6");
//        int descriptor = getRegister("A");
//        try (RandomAccessFile file = new RandomAccessFile("src/external_device.txt", "rw");) {
//            long lineStartPos = 0;
//            String lineToDelete = null;
//            for (int i = 0; i < descriptor + 1; i++) {
//                lineStartPos = file.getFilePointer();
//                lineToDelete = file.readLine();
//            }
//
//            if (lineToDelete != null) {
//                long lineLength = lineToDelete.getBytes().length;
//                file.seek(lineStartPos);
//
//                for (int i = 0; i < lineLength; i++) {
//                    file.writeByte(' ');
//                }
//            }
            setRegister("IOI", toHexWithPadding(getRegister("IOI") + 4, 1));
//        } catch (IOException e) {
//            throw new IllegalStateException(e);
//        }
    }
    private int toNumber(boolean bool) {
        return bool ? 1 : 0;
    }

    private int getRegister(String registerName) {
        return parseRegisterToInt(virtualProcessor.getVirtualMachine().getRegister(registerName));
    }

    private String getRegisterString(String registerName) {
        return virtualProcessor.getVirtualMachine().getRegister(registerName);
    }

    private void setRegister(String registerName, int registerValue, int amountOfChars) {
        virtualProcessor.getVirtualMachine().setRegister(registerName, toHexWithPadding(registerValue, amountOfChars));
    }

    private void setRegister(String registerName, String registerValue) {
        virtualProcessor.getVirtualMachine().setRegister(registerName, registerValue);
    }

    public void HALT() {
        setRegister("SI", "9");
    }

    private void callInstruction(String instruction) {
        try {
            getClass().getMethod(instruction).invoke(this);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unknown instruction: " + instruction);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to invoke instruction: " + instruction, e);
        }
    }

    private void callInstruction(String instruction, int firstArgument) {
        try {
            getClass().getMethod(instruction, int.class).invoke(this, firstArgument);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unknown instruction: " + instruction);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to invoke instruction: " + instruction, e);
        }
    }

    private void callInstruction(String instruction, int firstArgument, int secondArgument) {
        try {
            getClass().getMethod(instruction, int.class, int.class).invoke(this, firstArgument, secondArgument);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unknown instruction: " + instruction);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to invoke instruction: " + instruction, e);
        }
    }
}
