package com.sinefine.util.pdf;

import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This class contains tests for the {@code Pdfs} class.
 */
public class PdfsTest {
    
    public PdfsTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testIdenticalPdfsAreEqual() throws IOException {
      assertTrue("These PDF files are not equal!", areEqual("test001.pdf"));
    }
    
    private static boolean areEqual(final String resourceName) 
            throws IOException {
      try (InputStream actual = getResource("actual/" + resourceName);
          InputStream expected = getResource("expected/" + resourceName)) {
        return Pdfs.areEqual(actual, expected);
      }
    }
    
    private static InputStream getResource(final String resourceName) {
        return Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourceName);
    }
    
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
}
