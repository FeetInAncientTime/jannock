/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sinefine.util.pdf;

import static org.apache.pdfbox.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Roger
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
        assertTrue("These PDF files are identical!", areEqual("test001.pdf"));
    }
    
    private static boolean areEqual(final String resourceName) 
            throws IOException {
        InputStream actual = null;
        InputStream expected = null;
        try {
            actual = getResource("actual/" + resourceName);
            expected = getResource("expected/" + resourceName);
            return Pdfs.areEqual(actual, expected);
        } finally {
            closeQuietly(actual);
            closeQuietly(expected);            
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
