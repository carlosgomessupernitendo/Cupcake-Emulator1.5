
package br.com.cupcake.emulator.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuracao de inicializacao do Android 1.5 Cupcake.
 *
 * Esta classe prepara os argumentos para a futura
 * integracao com o QEMU compativel com Goldfish.
 */
public class EmulatorConfig {

    private final ImageInfo images;
    private final File dataDirectory;
    private final File writableUserdata;

    public EmulatorConfig(
            ImageInfo images,
            File dataDirectory) {

        if (images == null) {
            throw new IllegalArgumentException(
                    "ImageInfo nao pode ser nulo."
            );
        }

        if (dataDirectory == null) {
            throw new IllegalArgumentException(
                    "Diretorio de dados nao informado."
            );
        }

        this.images = images;
        this.dataDirectory = dataDirectory;

        this.writableUserdata = new File(
                dataDirectory,
                "userdata-qemu.img"
        );
    }

    public File getKernel() {
        return images.getKernel();
    }

    public File getRamdisk() {
        return images.getRamdisk();
    }

    public File getSystem() {
        return images.getSystem();
    }

    public File getInitialUserdata() {
        return images.getUserdata();
    }

    public File getWritableUserdata() {
        return writableUserdata;
    }

    public File getDataDirectory() {
        return dataDirectory;
    }

    /**
     * Monta os argumentos esperados pelo QEMU Android.
     * Nao inicia o processo de emulacao.
     */
    public List<String> buildArguments() {

        List<String> arguments =
                new ArrayList<String>();

        arguments.add("-kernel");
        arguments.add(getKernel().getAbsolutePath());

        arguments.add("-ramdisk");
        arguments.add(getRamdisk().getAbsolutePath());

        arguments.add("-system");
        arguments.add(getSystem().getAbsolutePath());

        arguments.add("-initdata");
        arguments.add(getInitialUserdata().getAbsolutePath());

        arguments.add("-data");
        arguments.add(getWritableUserdata().getAbsolutePath());

        arguments.add("-datadir");
        arguments.add(dataDirectory.getAbsolutePath());

        return Collections.unmodifiableList(arguments);
    }
}
