package os;

import static util.RegisterUtils.parseRegisterToInt;

public class VirtualProcessor {
    private final VirtualProcessorInstructions virtualProcessorInstructions = new VirtualProcessorInstructions(this);
    private final VirtualMachine virtualMachine;

    private boolean done;

    public VirtualProcessor(VirtualMachine virtualMachine) {
        this.virtualMachine = virtualMachine;
    }

    public VirtualMachine getVirtualMachine() {
        return virtualMachine;
    }

    public void halt() {
        done = true;
    }

    public boolean isDone() {
        return done;
    }

    public boolean isInterrupt() {
        return getRegister("PI") != 0 || getRegister("SI") != 0;
    }

    private int getRegister(String registerName) {
        return parseRegisterToInt(getVirtualMachine().getRegister(registerName));
    }

    public void executeStep() {
        clearInterrupts();
        String instruction = virtualMachine.readCommandAtCommandCounter();
        virtualProcessorInstructions.executeInstruction(instruction);
        getVirtualMachine().setRegister("TI",String.valueOf(Integer.parseInt(getRegisterString("TI")) - 1) );
    }

    private void clearInterrupts() {
        setRegister("IOI", "0");
        setRegister("SI", "0");
        setRegister("PI", "0");
        if (Integer.parseInt(getRegisterString("TI")) == 0) {
            setRegister("TI", "9");
        }
    }

    private String getRegisterString(String registerName) {
        return virtualMachine.getRegisters().get(registerName);
    }

    private void setRegister(String registerName, String registerValue) {
        virtualMachine.setRegister(registerName, registerValue);
    }
}
