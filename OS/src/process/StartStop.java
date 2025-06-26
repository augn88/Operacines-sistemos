package process;

import os.OS;
import resource.memory.Memory;

public class StartStop extends Process {
    public StartStop() {
        setPriority(100);
    }

    @Override
    public void execute(OS os) {
        switch (getStep()) {
            case 1 -> {
                os.createResource(this, new Memory());
                increaseStep();
            }
            case 2 -> {
                os.createProcess(this, new Main());
                os.createProcess(this, new Interrupt());
                os.createProcess(this, new JCL());
                os.createProcess(this, new JobGovernor());
                os.createProcess(this, new VirtualMachine());
                os.createProcess(this, new Write());
                increaseStep();
            }
            case 3 -> {
                os.askForMessageResource(this, "MOS_PABAIGA", (data) -> increaseStep());
            }
        }
    }
}
