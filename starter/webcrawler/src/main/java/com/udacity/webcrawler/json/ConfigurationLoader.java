package com.udacity.webcrawler.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A static utility class that loads a JSON configuration file.
 */
public final class ConfigurationLoader {

    private final Path path;

    /**
     * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
     */
    public ConfigurationLoader(Path path) {
        System.out.println(path.getFileName());
        this.path = Objects.requireNonNull(path);
    }

    /**
     * Loads crawler configuration from the given reader.
     *
     * @param reader a Reader pointing to a JSON string that contains crawler configuration.
     * @return a crawler configuration
     */
    public static CrawlerConfiguration read(Reader reader) {
        // This is here to get rid of the unused variable warning.
        Objects.requireNonNull(reader);
        CrawlerConfiguration crwlCnfg = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        try {
            crwlCnfg = mapper.readValue(reader, CrawlerConfiguration.class);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }


        return crwlCnfg;
    }

    /**
     * Loads configuration from this {@link ConfigurationLoader}'s path
     *
     * @return the loaded {@link CrawlerConfiguration}.
     */
    public CrawlerConfiguration load() throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return read(reader);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
