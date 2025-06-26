package os;

public class FileDescriptorInfo {
    private final String filename;
    private final int fileStartBlock;
    private final int fileStartBlockOffset;
    private int readBytes;

    public FileDescriptorInfo(String filename, int fileStartBlock, int fileStartBlockOffset) {
        this.filename = filename;
        this.fileStartBlock = fileStartBlock;
        this.fileStartBlockOffset = fileStartBlockOffset;
    }

    public int getReadBytes() {
        return readBytes;
    }

    public void setReadBytes(int readBytes) {
        this.readBytes = readBytes;
    }

    public String getFilename() {
        return filename;
    }

    public int getFileStartBlock() {
        return fileStartBlock;
    }

    public int getFileStartBlockOffset() {
        return fileStartBlockOffset;
    }
}
