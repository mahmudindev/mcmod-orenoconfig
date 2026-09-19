package com.github.mahmudindev.mcmod.orenoconfig.config.configs;

import com.github.mahmudindev.mcmod.orenoconfig.config.Config;
import com.github.mahmudindev.mcmod.orenoconfig.config.parser.ConfigParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class FileConfig extends Config {
    private final Path directory;
    private final String name;
    private String fileExtension;
    private ConfigParser parser;

    public FileConfig(Path directory, String name, ConfigParser parser) {
        super();

        this.directory = directory;
        this.parser = parser;

        int is = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        int ie = name.lastIndexOf('.');
        if (ie > is) {
            this.name = name.substring(0, ie);
            this.fileExtension = name.substring(ie + 1);
        } else {
            this.name = name;
        }
    }

    public FileConfig(FileConfig fileConfig) {
        this(fileConfig, false);
    }

    public FileConfig(FileConfig fileConfig, boolean onlyDefault) {
        super(fileConfig, onlyDefault);

        this.directory = fileConfig.directory;
        this.name = fileConfig.name;
        this.fileExtension = fileConfig.fileExtension;
        this.parser = fileConfig.parser;
    }

    public Path getDirectory() {
        return this.directory;
    }

    public String getName() {
        return this.name;
    }

    private String getFileExtension() {
        String fileExtension = this.fileExtension;

        if (fileExtension == null && this.parser != null) {
            fileExtension = this.parser.getFileExtension();
        }

        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public void setParser(ConfigParser parser) {
        this.parser = parser;
    }

    public void load() {
        String name = this.getName();

        String fileExtension = this.getFileExtension();
        if (fileExtension != null) {
            name += "." + fileExtension;
        }

        File file =  this.getDirectory().resolve(name).toFile();
        if (!file.exists()) {
            this.save();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            this.load(this.parser, reader);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to read %s file", name), e);
        }
    }

    public void save() {
        String name = this.getName();

        String fileExtension = this.getFileExtension();
        if (fileExtension != null) {
            name += "." + fileExtension;
        }

        File file =  this.getDirectory().resolve(name).toFile();

        try (FileWriter writer = new FileWriter(file)) {
            this.save(this.parser, writer);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to write %s file", name), e);
        }
    }
}
