
package br.com.cupcake.emulator.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Gerencia a imagem gravavel de dados do Android Cupcake.
 *
 * userdata.img:
 *     imagem original, preservada.
 *
 * userdata-qemu.img:
 *     copia gravavel, preservada entre inicializacoes.
 */
public final class UserdataManager {

    private static final int BUFFER_SIZE = 8192;

    /**
     * Prepara a imagem persistente.
     *
     * Se userdata-qemu.img ja existir, ela sera reutilizada.
     * Caso contrario, sera criada uma copia de userdata.img.
     */
    public synchronized File prepare(EmulatorConfig config)
            throws IOException {

        if (config == null) {
            throw new IOException(
                    "Configuracao do emulador nao informada."
            );
        }

        File initialImage = config.getInitialUserdata();
        File writableImage = config.getWritableUserdata();
        File dataDirectory = config.getDataDirectory();

        validateInitialImage(initialImage);
        prepareDirectory(dataDirectory);
        validateDestination(initialImage, writableImage);

        // Preservar os dados existentes.
        if (writableImage.exists()) {
            validateWritableImage(writableImage);
            return writableImage;
        }

        // Criar uma copia inicial sem modificar userdata.img.
        copyInitialImage(initialImage, writableImage);

        validateWritableImage(writableImage);

        return writableImage;
    }

    private void validateInitialImage(File image)
            throws IOException {

        if (image == null || !image.exists()) {
            throw new IOException(
                    "userdata.img nao foi encontrado."
            );
        }

        if (!image.isFile()) {
            throw new IOException(
                    "userdata.img nao e um arquivo valido."
            );
        }

        if (image.length() <= 0) {
            throw new IOException(
                    "userdata.img esta vazio."
            );
        }

        if (!image.canRead()) {
            throw new IOException(
                    "userdata.img nao pode ser lido."
            );
        }
    }

    private void prepareDirectory(File directory)
            throws IOException {

        if (directory == null) {
            throw new IOException(
                    "Diretorio de dados nao informado."
            );
        }

        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException(
                    "Nao foi possivel criar a pasta de dados."
            );
        }

        if (!directory.isDirectory()) {
            throw new IOException(
                    "O caminho de dados nao e uma pasta."
            );
        }

        if (!directory.canRead() || !directory.canWrite()) {
            throw new IOException(
                    "A pasta de dados precisa permitir leitura e gravacao."
            );
        }
    }

    private void validateDestination(
            File initialImage,
            File writableImage) throws IOException {

        if (writableImage == null) {
            throw new IOException(
                    "Destino userdata-qemu.img nao informado."
            );
        }

        File parent = writableImage.getCanonicalFile()
                .getParentFile();

        File directory = writableImage.getParentFile();

        if (parent == null || directory == null
                || !parent.equals(directory.getCanonicalFile())) {
            throw new IOException(
                    "Destino de dados invalido."
            );
        }

        if (initialImage.getCanonicalFile().equals(
                writableImage.getCanonicalFile())) {

            throw new IOException(
                    "A imagem original nao pode ser sobrescrita."
            );
        }

        if (writableImage.exists() && !writableImage.isFile()) {
            throw new IOException(
                    "O destino nao e um arquivo normal."
            );
        }
    }

    private void copyInitialImage(
            File source,
            File destination) throws IOException {

        File temporary = File.createTempFile(
                "userdata-",
                ".tmp",
                destination.getParentFile()
        );

        boolean completed = false;

        try {
            copyFile(source, temporary);

            // Nao substituir uma imagem que tenha surgido
            // enquanto a copia estava sendo preparada.
            if (destination.exists()) {
                throw new IOException(
                        "A imagem gravavel ja existe."
                );
            }

            if (!temporary.renameTo(destination)) {
                throw new IOException(
                        "Nao foi possivel finalizar userdata-qemu.img."
                );
            }

            completed = true;

        } finally {
            if (!completed && temporary.exists()) {
                temporary.delete();
            }
        }
    }

    private void copyFile(File source, File destination)
            throws IOException {

        FileInputStream input = null;
        FileOutputStream output = null;

        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(destination);

            byte[] buffer = new byte[BUFFER_SIZE];
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }

            output.flush();

        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                    // Preservar a excecao principal.
                }
            }

            if (output != null) {
                output.close();
            }
        }
    }

    private void validateWritableImage(File image)
            throws IOException {

        if (!image.exists() || !image.isFile()) {
            throw new IOException(
                    "userdata-qemu.img nao foi criado corretamente."
            );
        }

        if (image.length() <= 0) {
            throw new IOException(
                    "userdata-qemu.img esta vazio."
            );
        }

        if (!image.canRead() || !image.canWrite()) {
            throw new IOException(
                    "userdata-qemu.img precisa permitir leitura e gravacao."
            );
        }
    }
}
