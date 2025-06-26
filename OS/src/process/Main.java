package process;

import os.OS;

public class Main extends Process {
    public Main() {
        setPriority(99);
    }

    @Override
    public void execute(OS os) {
        super.execute(os);

        switch (getStep()) {
            case 1 -> {
                //os.askForMessageResource(this, "TASK_IN_MEMORY", (data) -> increaseStep());
                System.out.println("veikiu: " + this.getName());
            }

        }
    }
}