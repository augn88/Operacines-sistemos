package resource.memory;

public class PagingDevice {
    private final Memory memory;

    public PagingDevice(Memory memory) {
        this.memory = memory;
    }

    public PagesTable addPagesTable() {
        return new PagesTable(memory);
    }
}
