package io.polyglotted.eswrapper;

import io.polyglotted.eswrapper.services.AdminWrapper;
import io.polyglotted.eswrapper.services.IndexerWrapper;
import io.polyglotted.eswrapper.services.QueryWrapper;
import lombok.SneakyThrows;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.io.File;

import static org.elasticsearch.common.io.FileSystemUtils.deleteRecursively;

@ContextConfiguration(classes = AbstractElasticTestConfig.class)
public abstract class AbstractElasticTest extends AbstractTestNGSpringContextTests {

    @Autowired
    protected Client client;

    @Autowired
    protected AdminWrapper admin;

    @Autowired
    protected QueryWrapper query;

    @Autowired
    protected IndexerWrapper indexer;

    @BeforeSuite
    public static void cleanES() {
        deleteRecursively(new File("target", "elastic-test"), true);
    }

    @BeforeMethod
    @SneakyThrows
    public void setUp() throws Exception {
        performSetup();
        Thread.sleep(50);
    }

    protected void performSetup() {
        //sub-classes can extend
    }
}
