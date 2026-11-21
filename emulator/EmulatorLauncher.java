
package br.com.cupcake.emulator.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Prepara e inicia o processo externo do QEMU.
 *
 * Este componente nao inclui o executavel QEMU.
 * Ele prepara a chamada para um executavel compativel
 * que devera ser fornecido separadamente.
 */
public final class EmulatorLauncher {

    /**
     * Verifica se o executavel QEMU esta disponivel.
     */
    public boolean isAvailable(File executable) {

        return executable != null
                && executable.exists()
                && executable.isFile()
                && executable.canRead()
                && executable.canExecute();
    }

    /**
     * Monta o comando completo sem executar o QEMU.
     */
    public List<String> buildCommand(
            File executable,
            EmulatorConfig config) throws IOException {

        validateExecutable(executable);

        if (config == null) {
            throw new IOException(
                    "Configuracao do emulador nao informada."
            );
        }

        List<String> command = new ArrayList<String>();

        command.add(executable.getCanonicalPath());
        command.addAll(config.buildArguments());

        return command;
    }

    /**
     * Valida os arquivos, prepara userdata e inicia o QEMU.
     *
     * A saida padrao e os erros do processo sao gravados
     * no arquivo de log informado.
     */
    public Process launch(
            File executable,
            EmulatorConfig config,
            File logFile) throws IOException {

        validateExecutable(executable);

        if (config == null) {
            throw new IOException(
                    "Configuracao do emulador nao informada."
            );
        }

        if (logFile == null) {
            throw new IOException(
                    "Arquivo de log nao informado."
            );
        }

        // Preparar a imagem persistente do usuario.
        UserdataManager userdataManager =
                new UserdataManager();

        userdataManager.prepare(config);

        // Validar toda a configuracao.
        EmulatorValidator validator =
                new EmulatorValidator();

        EmulatorValidator.ValidationReport report =
                validator.validate(config);

        if (!report.isReadyForQemu()) {
            throw new IOException(
                    buildValidationError(report)
            );
        }

        // Preparar o arquivo de log.
        File logParent = logFile.getAbsoluteFile()
                .getParentFile();

        if (logParent != null && !logParent.exists()) {
            if (!logParent.mkdirs()) {
                throw new IOException(
                        "Nao foi possivel criar a pasta de logs."
                );
            }
        }

        if (logFile.exists() && !logFile.isFile()) {
            throw new IOException(
                    "O destino do log nao e um arquivo."
            );
        }

        // Montar os argumentos como itens separados.
        List<String> command =
                buildCommand(executable, config);

        ProcessBuilder builder =
                new ProcessBuilder(command);

        builder.directory(config.getDataDirectory());

        // Unificar saida normal e mensagens de erro.
        builder.redirectErrorStream(true);

        // Gravar a saida para evitar bloquear o processo.
        builder.redirectOutput(logFile);

        try {
            return builder.start();

        } catch (IOException exception) {
            throw new IOException(
                    "Falha ao iniciar QEMU: "
                    + exception.getMessage(),
                    exception
            );
        }
    }

    private void validateExecutable(File executable)
            throws IOException {

        if (executable == null) {
            throw new IOException(
                    "Executavel QEMU nao informado."
            );
        }

        if (!executable.exists()) {
            throw new IOException(
                    "Executavel QEMU nao encontrado: "
                    + executable.getAbsolutePath()
            );
        }

        if (!executable.isFile()) {
            throw new IOException(
                    "O caminho QEMU nao e um arquivo."
            );
        }

        if (!executable.canRead()) {
            throw new IOException(
                    "O executavel QEMU nao pode ser lido."
            );
        }

        if (!executable.canExecute()) {
            throw new IOException(
                    "O arquivo QEMU nao possui permissao de execucao."
            );
        }
    }

    private String buildValidationError(
            EmulatorValidator.ValidationReport report) {

        StringBuilder message = new StringBuilder();

        message.append(
                "Configuracao do emulador invalida:"
        );

        for (String error : report.getErrors()) {
            message.append("\n- ");
            message.append(error);
        }

        return message.toString();
    }
}
