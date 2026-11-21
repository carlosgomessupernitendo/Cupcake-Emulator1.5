
package br.com.cupcake.emulator.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BootReportFormatter {

    private final File outputDirectory;

    public BootReportFormatter(File outputDirectory) {

        if (outputDirectory == null) {
            throw new IllegalArgumentException(
                    "O diretório de saída não pode ser nulo."
            );
        }

        this.outputDirectory = outputDirectory;
    }

    /**
     * Gera um relatório completo a partir da análise.
     */
    public File format(
            BootAnalyzer.AnalysisReport report)
            throws IOException {

        if (report == null) {
            throw new IllegalArgumentException(
                    "O relatório de análise não pode ser nulo."
            );
        }

        createOutputDirectory();

        String timestamp = new SimpleDateFormat(
                "yyyyMMdd-HHmmss",
                Locale.US
        ).format(new Date());

        File outputFile = new File(
                outputDirectory,
                "boot-report-" + timestamp + ".txt"
        );

        PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream(outputFile),
                        "UTF-8"
                )
        );

        try {
            writeHeader(writer);
            writeSummary(writer, report);
            writeStages(writer, report);
            writeErrors(writer, report);
            writeWarnings(writer, report);
            writePossibleCauses(writer, report);
            writeConclusion(writer, report);
            writeFooter(writer);

        } finally {
            writer.close();
        }

        return outputFile;
    }

    private void createOutputDirectory()
            throws IOException {

        if (!outputDirectory.exists()
                && !outputDirectory.mkdirs()) {

            throw new IOException(
                    "Não foi possível criar a pasta de relatórios."
            );
        }

        if (!outputDirectory.isDirectory()
                || !outputDirectory.canWrite()) {

            throw new IOException(
                    "A pasta de relatórios não pode ser utilizada."
            );
        }
    }

    private void writeHeader(PrintWriter writer) {

        writer.println("========================================");
        writer.println("          CUPCAKE EMULATOR");
        writer.println("             BOOT REPORT");
        writer.println("========================================");
        writer.println();

        writer.println(
                "Data: "
                        + new SimpleDateFormat(
                                "dd/MM/yyyy HH:mm:ss",
                                Locale.getDefault()
                        ).format(new Date())
        );

        writer.println();
    }

    /**
     * Resumo geral.
     */
    private void writeSummary(
            PrintWriter writer,
            BootAnalyzer.AnalysisReport report) {

        writer.println("[1] RESUMO GERAL");
        writer.println("----------------------------------------");

        writer.println(
                "Arquivo analisado: "
                        + report.getLogFile().getName()
        );

        writer.println(
                "Linhas analisadas: "
                        + report.getTotalLines()
        );

        writer.println(
                "Etapa mais recente identificada: "
                        + report.getLastStage()
        );

        writer.println(
                "Total de erros: "
                        + report.getErrorCount()
        );

        writer.println(
                "Total de avisos: "
                        + report.getWarningCount()
        );

        writer.println(
                "Erros críticos: "
                        + (report.hasCriticalErrors() ? "SIM" : "NÃO")
        );

        writer.println();
    }

    /**
     * Mostra as etapas identificadas nos registros.
     */
    private void writeStages(
            PrintWriter writer,
            BootAnalyzer.AnalysisReport report) {

        writer.println("[2] ETAPAS IDENTIFICADAS");
        writer.println("----------------------------------------");

        BootAnalyzer.BootStage[] stages =
                BootAnalyzer.BootStage.values();

        for (BootAnalyzer.BootStage stage : stages) {

            if (stage == BootAnalyzer.BootStage.UNKNOWN) {
                continue;
            }

            int occurrences = countStage(
                    report.getFindings(),
                    stage
            );

            if (stage == report.getLastStage()) {

                writer.println(
                        "[ETAPA MAIS RECENTE] " + stage
                );

            } else if (occurrences > 0) {

                writer.println(
                        "[PROBLEMA REGISTRADO] " + stage
                );

            } else {

                writer.println(
                        "[SEM PROBLEMA REGISTRADO] " + stage
                );
            }
        }

        writer.println();
        writer.println(
                "Observação: ausência de problemas registrados "
                + "não confirma que uma etapa foi concluída."
        );

        writer.println();
    }

    /**
     * Conta ocorrências de uma etapa nos problemas encontrados.
     */
    private int countStage(
            List<BootAnalyzer.Finding> findings,
            BootAnalyzer.BootStage stage) {

        int count = 0;

        for (BootAnalyzer.Finding finding : findings) {

            if (finding.getStage() == stage) {
                count++;
            }
        }

        return count;
    }

    /**
     * Lista erros e erros críticos.
     */
    private void writeErrors(
            PrintWriter writer,
            BootAnalyzer.AnalysisReport report) {

        writer.println("[3] ERROS");
        writer.println("----------------------------------------");

        int count = 0;

        for (BootAnalyzer.Finding finding :
                report.getFindings()) {

            if (finding.getSeverity()
                    == BootAnalyzer.Severity.ERROR
                    || finding.getSeverity()
                    == BootAnalyzer.Severity.CRITICAL) {

                count++;

                writer.println();
                writer.println(
                        "Gravidade: "
                                + finding.getSeverity()
                );

                writer.println(
                        "Etapa: " + finding.getStage()
                );

                writer.println(
                        "Linha: " + finding.getLineNumber()
                );

                writer.println(
                        "Descrição: "
                                + finding.getDescription()
                );

                writer.println(
                        "Mensagem original: "
                                + finding.getOriginalLine()
                );
            }
        }

        if (count == 0) {
            writer.println("Nenhum erro identificado.");
        }

        writer.println();
    }

    /**
     * Lista avisos.
     */
    private void writeWarnings(
            PrintWriter writer,
            BootAnalyzer.AnalysisReport report) {

        writer.println("[4] AVISOS");
        writer.println("----------------------------------------");

        int count = 0;

        for (BootAnalyzer.Finding finding :
                report.getFindings()) {

            if (finding.getSeverity()
                    == BootAnalyzer.Severity.WARNING) {

                count++;

                writer.println();
                writer.println(
                        "Etapa: " + finding.getStage()
                );

                writer.println(
                        "Linha: " + finding.getLineNumber()
                );

                writer.println(
                        "Descrição: "
                                + finding.getDescription()
                );

                writer.println(
                        "Mensagem original: "
                                + finding.getOriginalLine()
                );
            }
        }

        if (count == 0) {
            writer.println("Nenhum aviso identificado.");
        }

        writer.println();
    }

    /**
     * Sugere possíveis causas com base nos indicadores.
     * As sugestões não representam diagnósticos confirmados.
     */
    private void writePossibleCauses(
            PrintWriter writer,
            BootAnalyzer.AnalysisReport report) {

        writer.println("[5] POSSÍVEIS CAUSAS");
        writer.println("----------------------------------------");

        boolean found = false;

        for (BootAnalyzer.Finding finding :
                report.getFindings()) {

            String message =
                    finding.getOriginalLine()
                            .toLowerCase(Locale.US);

            String cause = getPossibleCause(message);

            if (cause != null) {

                found = true;

                writer.println();
                writer.println(
                        "Etapa: " + finding.getStage()
                );

                writer.println(
                        "Indicador: "
                                + finding.getOriginalLine()
                );

                writer.println(
                        "Possível causa: " + cause
                );
            }
        }

        if (!found) {
            writer.println(
                    "Não foi possível sugerir uma causa específica."
            );
        }

        writer.println();
        writer.println(
                "As possíveis causas são hipóteses, "
                + "não confirmações técnicas."
        );

        writer.println();
    }

    private String getPossibleCause(String message) {

        if (contains(message,
                "kernel panic",
                "not syncing",
                "unable to mount root")) {

            return "Falha no kernel ou na preparação "
                    + "do sistema de arquivos raiz.";
        }

        if (contains(message,
                "cannot execute /init",
                "init: cannot find",
                "no working init")) {

            return "O processo init ou algum arquivo "
                    + "necessário pode estar ausente "
                    + "ou inacessível.";
        }

        if (contains(message,
                "failed to mount",
                "mounting failed",
                "filesystem")) {

            return "Problema na montagem ou no acesso "
                    + "a uma imagem do sistema.";
        }

        if (contains(message,
                "zygote",
                "zygoteinit")) {

            return "Possível falha na inicialização "
                    + "do ambiente Android Runtime ou "
                    + "do processo Zygote.";
        }

        if (contains(message,
                "system_server",
                "system server")) {

            return "Possível falha durante a inicialização "
                    + "dos serviços centrais do Android.";
        }

        if (contains(message,
                "dalvik",
                "dexopt",
                "androidruntime")) {

            return "Possível problema no ambiente "
                    + "de execução Dalvik ou no carregamento "
                    + "de classes.";
        }

        if (contains(message,
                "no such file or directory",
                "cannot open")) {

            return "Arquivo ou caminho necessário "
                    + "pode estar ausente ou inacessível.";
        }

        if (contains(message,
                "out of memory",
                "cannot allocate memory")) {

            return "Possível falta de memória disponível "
                    + "para a operação.";
        }

        return null;
    }

    private boolean contains(
            String message,
            String... patterns) {

        for (String pattern : patterns) {

            if (message.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Conclusão geral.
     */
    private void writeConclusion(
            PrintWriter writer,
            BootAnalyzer.AnalysisReport report) {

        writer.println("[6] CONCLUSÃO");
        writer.println("----------------------------------------");

        if (report.hasCriticalErrors()) {

            writer.println(
                    "Foram encontrados indicadores de falhas críticas."
            );

            writer.println(
                    "A inicialização precisa de investigação."
            );

        } else if (report.hasErrors()) {

            writer.println(
                    "Foram encontrados erros nos registros."
            );

            writer.println(
                    "Verifique as mensagens originais."
            );

        } else if (report.getWarningCount() > 0) {

            writer.println(
                    "Foram encontrados avisos, mas nenhum "
                    + "erro classificado foi identificado."
            );

        } else {

            writer.println(
                    "Nenhum problema conhecido foi identificado."
            );
        }

        if (report.getLastStage()
                == BootAnalyzer.BootStage.BOOT_COMPLETED) {

            writer.println(
                    "Foi encontrado um indicador de conclusão "
                    + "do boot nos registros."
            );

        } else {

            writer.println(
                    "Não foi identificado um indicador de "
                    + "conclusão do boot."
            );
        }

        writer.println();
        writer.println(
                "Este resultado é uma análise automática "
                + "dos registros, não uma confirmação "
                + "de funcionamento do Android."
        );

        writer.println();
    }

    private void writeFooter(PrintWriter writer) {

        writer.println("========================================");
        writer.println("       FIM DO RELATÓRIO");
        writer.println("========================================");
    }
}
