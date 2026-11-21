
package br.com.cupcake.emulator.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BootAnalyzer {

    public enum BootStage {
        QEMU_STARTUP,
        KERNEL,
        RAMDISK,
        INIT,
        FILESYSTEM,
        ANDROID_RUNTIME,
        ZYGOTE,
        SYSTEM_SERVER,
        ANDROID_INTERFACE,
        BOOT_COMPLETED,
        UNKNOWN
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    private final File logDirectory;

    public BootAnalyzer(File logDirectory) {
        if (logDirectory == null) {
            throw new IllegalArgumentException(
                    "O diretório de logs não pode ser nulo."
            );
        }

        this.logDirectory = logDirectory;
    }

    /**
     * Analisa o log completo produzido pelo EmulatorLogManager.
     */
    public AnalysisReport analyze(File logFile)
            throws IOException {

        if (logFile == null
                || !logFile.exists()
                || !logFile.isFile()
                || !logFile.canRead()) {

            throw new IOException(
                    "Arquivo de log inexistente ou ilegível."
            );
        }

        List<Finding> findings = new ArrayList<Finding>();

        BootStage lastStage = BootStage.UNKNOWN;
        int totalLines = 0;
        int errorCount = 0;
        int warningCount = 0;

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(logFile),
                        "UTF-8"
                )
        );

        try {
            String line;

            while ((line = reader.readLine()) != null) {

                totalLines++;

                BootStage detectedStage =
                        detectStage(line);

                if (detectedStage != BootStage.UNKNOWN) {
                    lastStage = detectedStage;
                }

                Finding finding = analyzeLine(
                        totalLines,
                        line,
                        detectedStage
                );

                if (finding != null) {

                    findings.add(finding);

                    if (finding.getSeverity() == Severity.ERROR
                            || finding.getSeverity()
                            == Severity.CRITICAL) {

                        errorCount++;

                    } else if (finding.getSeverity()
                            == Severity.WARNING) {

                        warningCount++;
                    }
                }
            }

        } finally {
            reader.close();
        }

        return new AnalysisReport(
                logFile,
                lastStage,
                totalLines,
                errorCount,
                warningCount,
                findings
        );
    }

    /**
     * Identifica a etapa provável a partir de mensagens conhecidas.
     */
    private BootStage detectStage(String line) {

        String text = line.toLowerCase(Locale.US);

        if (containsAny(text,
                "boot completed",
                "boot is complete",
                "android is ready")) {

            return BootStage.BOOT_COMPLETED;
        }

        if (containsAny(text,
                "launcher",
                "home application",
                "home screen",
                "activitymanager: starting")) {

            return BootStage.ANDROID_INTERFACE;
        }

        if (containsAny(text,
                "system_server",
                "system server",
                "systemserver")) {

            return BootStage.SYSTEM_SERVER;
        }

        if (containsAny(text,
                "zygote",
                "zygoteinit",
                "startzygote")) {

            return BootStage.ZYGOTE;
        }

        if (containsAny(text,
                "dalvik",
                "androidruntime",
                "dexopt",
                "preloading classes")) {

            return BootStage.ANDROID_RUNTIME;
        }

        if (containsAny(text,
                "init: starting",
                "init process",
                "init: processing",
                "init.rc")) {

            return BootStage.INIT;
        }

        if (containsAny(text,
                "mounting",
                "mounted",
                "filesystem",
                "mount system",
                "mount /system",
                "mount /data")) {

            return BootStage.FILESYSTEM;
        }

        if (containsAny(text,
                "ramdisk",
                "initrd")) {

            return BootStage.RAMDISK;
        }

        if (containsAny(text,
                "linux version",
                "kernel command line",
                "kernel panic",
                "vfs:",
                "cpu:",
                "memory:")) {

            return BootStage.KERNEL;
        }

        if (containsAny(text,
                "qemu",
                "goldfish",
                "emulator:")) {

            return BootStage.QEMU_STARTUP;
        }

        return BootStage.UNKNOWN;
    }

    /**
     * Procura indicadores de problemas em cada linha.
     */
    private Finding analyzeLine(
            int lineNumber,
            String line,
            BootStage stage) {

        String text = line.toLowerCase(Locale.US);

        if (containsAny(text,
                "kernel panic",
                "unable to mount root",
                "no working init found",
                "not syncing:")) {

            return new Finding(
                    lineNumber,
                    Severity.CRITICAL,
                    stage,
                    "Possível falha crítica no kernel.",
                    line
            );
        }

        if (containsAny(text,
                "fatal:",
                "fatal error",
                "segmentation fault",
                "assertion failed",
                "cannot execute /init",
                "init: cannot find")) {

            return new Finding(
                    lineNumber,
                    Severity.CRITICAL,
                    stage,
                    "Falha crítica durante a inicialização.",
                    line
            );
        }

        if (containsAny(text,
                "error:",
                "failed to",
                "failure",
                "cannot open",
                "unable to",
                "no such file or directory")) {

            return new Finding(
                    lineNumber,
                    Severity.ERROR,
                    stage,
                    "Possível erro de arquivo, serviço ou recurso.",
                    line
            );
        }

        if (containsAny(text,
                "warning:",
                "warn:",
                "deprecated",
                "not found")) {

            return new Finding(
                    lineNumber,
                    Severity.WARNING,
                    stage,
                    "Aviso durante a inicialização.",
                    line
            );
        }

        return null;
    }

    private boolean containsAny(
            String text,
            String... patterns) {

        for (String pattern : patterns) {

            if (text.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gera um relatório de diagnóstico em formato TXT.
     */
    public File saveReport(AnalysisReport report)
            throws IOException {

        if (report == null) {
            throw new IllegalArgumentException(
                    "O relatório não pode ser nulo."
            );
        }

        if (!logDirectory.exists()
                && !logDirectory.mkdirs()) {

            throw new IOException(
                    "Não foi possível criar a pasta de relatórios."
            );
        }

        String timestamp = new SimpleDateFormat(
                "yyyyMMdd-HHmmss",
                Locale.US
        ).format(new Date());

        File reportFile = new File(
                logDirectory,
                "boot-analysis-" + timestamp + ".txt"
        );

        PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream(reportFile),
                        "UTF-8"
                )
        );

        try {

            writer.println("CUPCAKE EMULATOR");
            writer.println("BOOT ANALYSIS REPORT");
            writer.println("==============================");

            writer.println("Data: " + new Date());
            writer.println(
                    "Log analisado: "
                            + report.getLogFile().getName()
            );

            writer.println(
                    "Linhas analisadas: "
                            + report.getTotalLines()
            );

            writer.println(
                    "Última etapa identificada: "
                            + report.getLastStage()
            );

            writer.println(
                    "Erros: " + report.getErrorCount()
            );

            writer.println(
                    "Avisos: " + report.getWarningCount()
            );

            writer.println();
            writer.println("DIAGNOSTICOS");
            writer.println("==============================");

            if (report.getFindings().isEmpty()) {

                writer.println(
                        "Nenhum indicador de problema foi encontrado."
                );

            } else {

                for (Finding finding : report.getFindings()) {

                    writer.println();
                    writer.println(
                            "[" + finding.getSeverity() + "]"
                    );

                    writer.println(
                            "Etapa: " + finding.getStage()
                    );

                    writer.println(
                            "Linha: " + finding.getLineNumber()
                    );

                    writer.println(
                            "Descrição: " + finding.getDescription()
                    );

                    writer.println(
                            "Mensagem original: "
                                    + finding.getOriginalLine()
                    );
                }
            }

            writer.println();
            writer.println("==============================");
            writer.println("Fim do relatório.");

        } finally {
            writer.close();
        }

        return reportFile;
    }

    /**
     * Resultado completo da análise.
     */
    public static class AnalysisReport {

        private final File logFile;
        private final BootStage lastStage;
        private final int totalLines;
        private final int errorCount;
        private final int warningCount;
        private final List<Finding> findings;

        AnalysisReport(
                File logFile,
                BootStage lastStage,
                int totalLines,
                int errorCount,
                int warningCount,
                List<Finding> findings) {

            this.logFile = logFile;
            this.lastStage = lastStage;
            this.totalLines = totalLines;
            this.errorCount = errorCount;
            this.warningCount = warningCount;
            this.findings = findings;
        }

        public File getLogFile() {
            return logFile;
        }

        public BootStage getLastStage() {
            return lastStage;
        }

        public int getTotalLines() {
            return totalLines;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public int getWarningCount() {
            return warningCount;
        }

        public List<Finding> getFindings() {
            return new ArrayList<Finding>(findings);
        }

        public boolean hasErrors() {
            return errorCount > 0;
        }

        public boolean hasCriticalErrors() {

            for (Finding finding : findings) {

                if (finding.getSeverity()
                        == Severity.CRITICAL) {

                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Representa um problema encontrado no log.
     */
    public static class Finding {

        private final int lineNumber;
        private final Severity severity;
        private final BootStage stage;
        private final String description;
        private final String originalLine;

        Finding(
                int lineNumber,
                Severity severity,
                BootStage stage,
                String description,
                String originalLine) {

            this.lineNumber = lineNumber;
            this.severity = severity;
            this.stage = stage;
            this.description = description;
            this.originalLine = originalLine;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public Severity getSeverity() {
            return severity;
        }

        public BootStage getStage() {
            return stage;
        }

        public String getDescription() {
            return description;
        }

        public String getOriginalLine() {
            return originalLine;
        }
    }
}
