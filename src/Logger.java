import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {

    private String path;
    private StringBuilder stringBuilder;

    public Logger() {
        this.stringBuilder = new StringBuilder();
    }

    public void log(String string) {
        this.stringBuilder.append(string).append("\n");
    }

    public void emptyLine() {
        this.stringBuilder.append("\n");
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void writeFile() {
        try {
            FileWriter fileWriter = new FileWriter(path);
            fileWriter.write(stringBuilder.toString());
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
