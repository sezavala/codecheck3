package controllers;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.horstmann.codecheck.checker.ResourceLoader;

import io.quarkus.arc.All;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.sql.DataSource;

@ApplicationScoped
public class Config implements ResourceLoader {

    private final org.eclipse.microprofile.config.Config config = ConfigProvider.getConfig();

    @Override
    public InputStream loadResource(String path) throws IOException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/resources/resources/" + path);
        if (inputStream == null) {
            throw new IOException("Resource not found: " + path);
        }
        return inputStream;
    }

    @Override
    public String getProperty(String key) {
        return config.getOptionalValue(key, String.class).orElse(null);
    }

    public String getString(String key) {
        return getProperty(key);
    }

    public boolean hasPath(String key) {
        return config.getOptionalValue(key, String.class).isPresent();
    }

    @Inject
    @All // https://quarkus.io/guides/cdi-reference#injecting-multiple-bean-instances-intuitively
    List<DataSource> dataSourceList;

    public Connection getDatabaseConnection() throws SQLException {
        return dataSourceList.get(0).getConnection();
        /*
        Need to inject a DataSource
        Maybe this is a poor spot for injection?
        Quarkus has a @LookupIfProperty that would give what we want for the StorageConnection.
        https://quarkus.io/guides/cdi-reference#declaratively-choose-beans-that-can-be-obtained-by-programmatic-lookup
         */
    }
}
