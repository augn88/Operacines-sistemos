import os.OS;
import os.RealMachine;

public class Main {

    public static void main(String[] args) {
        RealMachine realMachine = new RealMachine();
        realMachine.setVirtualMemoryFromFile("src/vm_memory_1.txt");
        OS os = realMachine.getOs();

        os.boot();
        os.planner();
        os.planner();
        os.planner();
    }
}