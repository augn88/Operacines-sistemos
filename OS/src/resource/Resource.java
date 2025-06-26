package resource;
import process.Process;

public class Resource {
    private final String name;
    private int resourceId;
    private String type;
    private int processId;
    private Process creator;

    public Resource(String name) {
        this.name = name;
    }

    public Process getCreator() {
        return creator;
    }

    public void setCreator(Process creator) {
        this.creator = creator;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getProcessId() {
        return processId;
    }

    public void setProcessId(int processId) {
        this.processId = processId;
    }

    public String getName() {
        return name;
    }
}
