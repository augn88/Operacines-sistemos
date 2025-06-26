package resource.channel;

import os.OS;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

import static resource.memory.Word.CHARS_PER_WORD;
import static util.StringUtils.splitStringEveryNCharacters;

public class ChannelDevice {
    private final Map<String, String> cpuRegisters;
    private OS os;

    public ChannelDevice (Map<String, String> cpuRegisters, OS os) {
        this.os = os;
        this.cpuRegisters = cpuRegisters;
    }

    public String receiveFromKeyboard() {
        setRegister("CHST[1]", "1");
        System.out.println("Iveskite reiksme: ");
        String value = String.valueOf((new Scanner(System.in).next()));

        setRegister("CHST[1]", "0");
        return value;
    }

    public void sendToDisplay(String string) {
        setRegister("CHST[2]", "1");
        System.out.println(string);
        setRegister("CHST[2]", "0");
    }

    public String[] readFile(int descriptor, int bytesToRead, int offset){
        setRegister("CHST[3]", "1");
        int numberOfCharactersToSkip = offset;
        try (FileReader file = new FileReader("src/external_device.txt");
             BufferedReader input = new BufferedReader(file)
        ) {
            for(int i = 0; i < descriptor - 1; i++) {
                input.readLine();
            }
            char[] buffer = new char[bytesToRead];

            // todo: fileDescriptorInfo.setReadBytes(fileDescriptorInfo.getReadBytes() + bytesToRead);
            for (int i = 1; i <= numberOfCharactersToSkip; i++) {
                int status = input.read();
            }

            for (int i = 0; i < bytesToRead; i++) {
                int readCharacter = input.read();

                if (readCharacter == -1) {
                    setRegister("PI", "5");
                }

                buffer[i] = (char) readCharacter;
            }

            String gottenCharacters = new String(buffer, 0, bytesToRead);

            String[] splitGottenCharacters = splitStringEveryNCharacters(gottenCharacters, CHARS_PER_WORD);
            while(splitGottenCharacters[splitGottenCharacters.length-1].length() < 4){
                splitGottenCharacters[splitGottenCharacters.length-1] += "0";
            }
            setRegister("CHST[3]", "0");
            return splitGottenCharacters;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    public void writeFile(int descriptor, int fileOffset, String contentToWrite){
        setRegister("CHST[3]", "1");
        try (RandomAccessFile file = new RandomAccessFile("src/external_device.txt", "rw");){
            for(int i = 0; i < descriptor - 1; i++) {
                file.readLine();
            }
            file.seek(fileOffset);
            byte[] bytesToWrite = contentToWrite.getBytes(StandardCharsets.UTF_8);
            file.write(bytesToWrite);
        } catch (IOException e) {
            setRegister("PI", "5");
            throw new IllegalStateException(e);
        }
        setRegister("CHST[3]", "0");
    }

    public void deleteFile(int descriptor){
        setRegister("CHST[3]", "1");
        try (RandomAccessFile file = new RandomAccessFile("src/external_device.txt", "rw");) {
            long lineStartPos = 0;
            String lineToDelete = null;
            for (int i = 0; i < descriptor + 1; i++) {
                lineStartPos = file.getFilePointer();
                lineToDelete = file.readLine();
            }

            if (lineToDelete != null) {
                long lineLength = lineToDelete.getBytes().length;
                file.seek(lineStartPos);

                for (int i = 0; i < lineLength; i++) {
                    file.writeByte(' ');
                }
            }
            setRegister("CHST[3]", "0");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    public void setRegister(String registerName, String registerValue) {
        cpuRegisters.replace(registerName, registerValue);
    }

}
