
package br.com.cupcake.emulator.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Valida imagens e configuracao antes da futura
 * integracao com o QEMU/Goldfish.
 */
public final class EmulatorValidator {

    public ValidationReport validate(EmulatorConfig config) {

        List<String> errors = new ArrayList<String>();
        List<String> warnings = new ArrayList<String>();

        if (config == null) {
            errors.add("Configuracao do emulador nao informada.");
            return new ValidationReport(errors, warnings);
        }

        // 1. Validar as quatro imagens obrigatorias.
        validateImage("Kernel", config.getKernel(), errors);
        validateImage("Ramdisk", config.getRamdisk(), errors);
        validateImage("System", config.getSystem(), errors);
        validateImage("Userdata inicial",
                config.getInitialUserdata(), errors);

        // 2. Validar a pasta de dados persistentes.
        File dataDirectory = config.getDataDirectory();

        if (dataDirectory == null) {
            errors.add("Pasta de dados nao informada.");
        } else if (!dataDirectory.exists()) {
            errors.add("Pasta de dados nao existe: "
                    + dataDirectory.getAbsolutePath());
        } else if (!dataDirectory.isDirectory()) {
            errors.add("O caminho de dados nao e uma pasta.");
        } else {
            if (!dataDirectory.canRead()) {
                errors.add("A pasta de dados nao pode ser lida.");
            }

            if (!dataDirectory.canWrite()) {
                errors.add("A pasta de dados nao permite gravacao.");
            }
        }

        // 3. Validar o arquivo gravavel do usuario.
        File writableUserdata = config.getWritableUserdata();

        if (writableUserdata == null) {
            errors.add("Arquivo userdata-qemu.img nao configurado.");
        } else {
            if (writableUserdata.exists()) {
                if (!writableUserdata.isFile()) {
                    errors.add(
                            "userdata-qemu.img nao e um arquivo normal."
                    );
                } else if (!writableUserdata.canWrite()) {
                    errors.add(
                            "userdata-qemu.img nao permite gravacao."
                    );
                }
            } else {
                warnings.add(
                        "userdata-qemu.img ainda nao existe. "
                        + "Precisara ser criado a partir de userdata.img."
                );
            }

            validateDataPath(
                    dataDirectory,
                    writableUserdata,
                    errors
            );
        }

        // 4. Validar os argumentos preparados para o QEMU.
        validateArguments(config, errors);

        return new ValidationReport(errors, warnings);
    }

    private void validateImage(
            String name,
            File image,
            List<String> errors) {

        if (image == null) {
            errors.add(name + ": caminho nao informado.");
            return;
        }

        if (!image.exists()) {
            errors.add(name + ": arquivo nao encontrado.");
            return;
        }

        if (!image.isFile()) {
            errors.add(name + ": o caminho nao e um arquivo.");
            return;
        }

        if (image.length() <= 0) {
            errors.add(name + ": arquivo vazio.");
            return;
        }

        if (!image.canRead()) {
            errors.add(name + ": arquivo sem permissao de leitura.");
            return;
        }

        try {
            String canonicalPath = image.getCanonicalPath();

            if (canonicalPath.length() == 0) {
                errors.add(name + ": caminho invalido.");
            }
        } catch (IOException exception) {
            errors.add(name + ": nao foi possivel resolver o caminho.");
        }
    }

    private void validateDataPath(
            File directory,
            File userdata,
            List<String> errors) {

        if (directory == null || userdata == null) {
            return;
        }

        try {
            File canonicalDirectory = directory.getCanonicalFile();
            File canonicalParent = userdata.getCanonicalFile()
                    .getParentFile();

            if (canonicalParent == null
                    || !canonicalDirectory.equals(canonicalParent)) {

                errors.add(
                        "userdata-qemu.img deve ficar dentro "
                        + "da pasta de dados configurada."
                );
            }

        } catch (IOException exception) {
            errors.add(
                    "Nao foi possivel verificar o caminho dos dados."
            );
        }
    }

    private void validateArguments(
            EmulatorConfig config,
            List<String> errors) {

        List<String> arguments;

        try {
            arguments = config.buildArguments();
        } catch (RuntimeException exception) {
            errors.add("Nao foi possivel construir os argumentos do QEMU.");
            return;
        }

        if (arguments == null || arguments.isEmpty()) {
            errors.add("Lista de argumentos do QEMU vazia.");
            return;
        }

        String[] requiredOptions = {
            "-kernel",
            "-ramdisk",
            "-system",
            "-initdata",
            "-data",
            "-datadir"
        };

        for (String option : requiredOptions) {
            int index = arguments.indexOf(option);

            if (index < 0) {
                errors.add(
                        "Opcao obrigatoria ausente: " + option
                );
            } else if (index + 1 >= arguments.size()) {
                errors.add(
                        "Caminho ausente depois de " + option
                );
            } else {
                String value = arguments.get(index + 1);

                if (value == null || value.length() == 0) {
                    errors.add(
                            "Valor vazio para a opcao " + option
                    );
                } else if (!new File(value).isAbsolute()) {
                    errors.add(
                            "O caminho de " + option
                            + " precisa ser absoluto."
                    );
                }
            }
        }
    }

    /**
     * Resultado da validacao.
     */
    public static final class ValidationReport {

        private final List<String> errors;
        private final List<String> warnings;

        private ValidationReport(
                List<String> errors,
                List<String> warnings) {

            this.errors = Collections.unmodifiableList(
                    new ArrayList<String>(errors)
            );

            this.warnings = Collections.unmodifiableList(
                    new ArrayList<String>(warnings)
            );
        }

        public boolean isReadyForQemu() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public int getErrorCount() {
            return errors.size();
        }

        public int getWarningCount() {
            return warnings.size();
        }
    }
}
