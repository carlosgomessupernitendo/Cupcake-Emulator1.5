
package br.com.cupcake.emulator.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class EmulatorLogManager {

    private final File logDirectory;

    private PrintWriter allLog;
    private PrintWriter stdoutLog;
    private PrintWriter stderrLog;
    private PrintWriter infoLog;
    private PrintWriter warningLog;
    private PrintWriter errorLog;

    private final Object lock = new Object();
    private final AtomicInteger streamsRemaining =
            new AtomicInteger(2);

    private boolean started = false;

    private File allLogFile;
    private File stdoutLogFile;
    private File stderrLogFile;
    private File infoLogFile;
    private File warningLogFile;
    private File errorLogFile;

    public EmulatorLogManager(File logDirectory) {
        if (logDirectory == null) {
            throw new IllegalArgumentException(
                    "O diretório de logs não pode ser nulo."
            );
        }

        this.logDirectory = logDirectory;
    }

    /**
     * Inicia a captura das saídas do processo QEMU.
     */
    public LogCapture startCapture(Process process)
            throws IOException {

        if (process == null) {
            throw new IllegalArgumentException(
                    "O processo não pode ser nulo."
            );
        }

        synchronized (lock) {
            if (started) {
                throw new IllegalStateException(
                        "Este gerenciador já está capturando um processo."
                );
            }

            started = true;
            createLogFiles();
        }

        Thread stdoutThread = new Thread(
                new StreamReader(process.getInputStream(), "STDOUT"),
                "Cupcake-Log-STDOUT"
        );

        Thread stderrThread = new Thread(
                new StreamReader(process.getErrorStream(), "STDERR"),
                "Cupcake-Log-STDERR"
        );

        stdoutThread.setDaemon(true);
        stderrThread.setDaemon(true);

        stdoutThread.start();
        stderrThread.start();

        return new LogCapture(stdoutThread, stderrThread);
    }

    /**
     * Cria os arquivos de registro.
     */
    private void createLogFiles() throws IOException {

        if (!logDirectory.exists()
                && !logDirectory.mkdirs()) {
            throw new IOException(
                    "Não foi possível criar: "
                            + logDirectory.getAbsolutePath()
            );
        }

        if (!logDirectory.isDirectory()
                || !logDirectory.canWrite()) {
            throw new IOException(
                    "O diretório de logs não pode ser utilizado."
            );
        }

        String timestamp = new SimpleDateFormat(
                "yyyyMMdd-HHmmss-SSS",
                Locale.US
        ).format(new Date());

        allLogFile = new File(
                logDirectory, "all-" + timestamp + ".log"
        );

        stdoutLogFile = new File(
                logDirectory, "stdout-" + timestamp + ".log"
        );

        stderrLogFile = new File(
                logDirectory, "stderr-" + timestamp + ".log"
        );

        infoLogFile = new File(
                logDirectory, "info-" + timestamp + ".log"
        );

        warningLogFile = new File(
                logDirectory, "warnings-" + timestamp + ".log"
        );

        errorLogFile = new File(
                logDirectory, "errors-" + timestamp + ".log"
        );

        allLog = openWriter(allLogFile);
        stdoutLog = openWriter(stdoutLogFile);
        stderrLog = openWriter(stderrLogFile);
        infoLog = openWriter(infoLogFile);
        warningLog = openWriter(warningLogFile);
        errorLog = openWriter(errorLogFile);
    }

    private PrintWriter openWriter(File file)
            throws IOException {

        return new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream(file, true),
                        "UTF-8"
                ),
                true
        );
    }

    /**
     * Registra uma linha recebida do processo.
     */
    private void writeLine(String source, String message) {

        synchronized (lock) {

            if (allLog == null) {
                return;
            }

            String timestamp = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss.SSS",
                    Locale.US
            ).format(new Date());

            String formatted =
                    "[" + timestamp + "]"
                    + " [" + source + "] "
                    + message;

            allLog.println(formatted);

            if ("STDOUT".equals(source)) {
                stdoutLog.println(formatted);
            } else {
                stderrLog.println(formatted);
            }

            Level level = classify(message);

            switch (level) {
                case WARNING:
                    warningLog.println(formatted);
                    break;

                case ERROR:
                    errorLog.println(formatted);
                    break;

                case INFO:
                default:
                    infoLog.println(formatted);
                    break;
            }
        }
    }

    /**
     * Classifica as mensagens por palavras e indicadores
     * comuns encontrados nos registros do QEMU e do kernel.
     */
    private Level classify(String message) {

        String text = message.toLowerCase(Locale.US);

        if (text.matches("^\\s*<3>.*")
                || text.contains("[e]")
                || text.contains("error:")
                || text.contains("fatal:")
                || text.contains("panic:")
                || text.contains("failed")
                || text.contains("failure")
                || text.contains("cannot ")
                || text.contains("unable to")
                || text.contains("assertion")) {

            return Level.ERROR;
        }

        if (text.matches("^\\s*<4>.*")
                || text.contains("[w]")
                || text.contains("warning:")
                || text.contains("warn:")) {

            return Level.WARNING;
        }

        return Level.INFO;
    }

    private enum Level {
        INFO,
        WARNING,
        ERROR
    }

    /**
     * Lê continuamente um dos fluxos do processo.
     */
    private class StreamReader implements Runnable {

        private final InputStream stream;
        private final String source;

        StreamReader(InputStream stream, String source) {
            this.stream = stream;
            this.source = source;
        }

        @Override
        public void run() {

            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, "UTF-8")
                );

                String line;

                while ((line = reader.readLine()) != null) {
                    writeLine(source, line);
                }

                reader.close();

            } catch (IOException exception) {

                writeLine(
                        "LOGGER",
                        "Erro ao capturar " + source
                                + ": " + exception.getMessage()
                );

            } finally {

                if (streamsRemaining.decrementAndGet() == 0) {
                    closeWriters();
                }
            }
        }
    }

    /**
     * Fecha todos os arquivos após a captura dos dois fluxos.
     */
    private void closeWriters() {

        synchronized (lock) {

            closeWriter(allLog);
            closeWriter(stdoutLog);
            closeWriter(stderrLog);
            closeWriter(infoLog);
            closeWriter(warningLog);
            closeWriter(errorLog);

            allLog = null;
            stdoutLog = null;
            stderrLog = null;
            infoLog = null;
            warningLog = null;
            errorLog = null;
        }
    }

    private void closeWriter(PrintWriter writer) {
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }

    public File getLogDirectory() {
        return logDirectory;
    }

    public File getAllLogFile() {
        return allLogFile;
    }

    public File getStdoutLogFile() {
        return stdoutLogFile;
    }

    public File getStderrLogFile() {
        return stderrLogFile;
    }

    public File getInfoLogFile() {
        return infoLogFile;
    }

    public File getWarningLogFile() {
        return warningLogFile;
    }

    public File getErrorLogFile() {
        return errorLogFile;
    }

    /**
     * Permite aguardar até que os dois fluxos terminem.
     */
    public static class LogCapture {

        private final Thread stdoutThread;
        private final Thread stderrThread;

        LogCapture(Thread stdoutThread, Thread stderrThread) {
            this.stdoutThread = stdoutThread;
            this.stderrThread = stderrThread;
        }

        public void awaitCompletion()
                throws InterruptedException {

            stdoutThread.join();
            stderrThread.join();
        }
    }
}
