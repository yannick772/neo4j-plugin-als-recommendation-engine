package recommendation.als;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.util.Objects;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AlsBaseTest<T extends AlsProcedure> {

    protected Driver driver;
    protected Neo4j embeddedDatabaseServer;

    private final Class<T> clazz;

    public AlsBaseTest() {
        ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
        clazz = (Class<T>) genericSuperclass.getActualTypeArguments()[0];
    }

    @BeforeAll
    void initializeNeo4j() {
        embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withProcedure(clazz)
                .build();

        driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
    }

    @AfterAll
    void closeDriver() {
        driver.close();
        embeddedDatabaseServer.close();
    }

    @AfterEach
    void cleanDb() {
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");
        }
    }

    protected void loadFauxDataset(String fileName) {
//        closeDriver();
        StringWriter stringWriter = new StringWriter();
        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(
                                getClass().getResourceAsStream(String.format("/%s.cypher", fileName))
                        )
                )
        )) {
            bufferedReader.transferTo(stringWriter);
            stringWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cleanDb();
        try (Session session = driver.session()) {
            session.run(stringWriter.toString()).consume();
        }

//        embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
//                .withDisabledServer()
//                .withProcedure(clazz)
//                .withFixture(stringWriter.toString())
//                .build();
//
//        driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
    }

}
