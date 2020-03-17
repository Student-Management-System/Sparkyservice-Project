package net.ssehub.sparkyservice.db;

import org.testcontainers.containers.PostgreSQLContainer;

public class SparkysPostgresqlContainer extends PostgreSQLContainer<SparkysPostgresqlContainer>{
    private static final String IMAGE_VERSION = "postgres:latest";
    private static SparkysPostgresqlContainer container;
    
    private SparkysPostgresqlContainer() {
        super(IMAGE_VERSION);
    }
    
    public static SparkysPostgresqlContainer getInstance() {
        if (container == null) {
            container = new SparkysPostgresqlContainer()
                    .withDatabaseName("testdb")
                    .withPassword("testdb")
                    .withUsername("testdb")
                    .withStartupTimeoutSeconds(10);
        }
        return container;
    }
    
    @Override
    public void start() {
        super.start();
        System.setProperty("DB_URL", container.getJdbcUrl());
        System.setProperty("DB_USERNAME", container.getUsername());
        System.setProperty("DB_PASSWORD", container.getPassword());
    }
    
    @Override
    public void stop() {
        //do nothing, JVM handles shut down
    }

}