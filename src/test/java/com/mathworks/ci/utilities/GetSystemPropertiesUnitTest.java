package com.mathworks.ci.utilities;

/**
 * Copyright 2024, The MathWorks, Inc.
 */

import static org.junit.Assert.assertArrayEquals;

import com.mathworks.ci.utilities.GetSystemProperties;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class GetSystemPropertiesUnitTest {

    @Mock
    private GetSystemProperties getSystemProperties;

    @Before
    public void setUp() {
        getSystemProperties = new GetSystemProperties("os.name", "os.arch", "os.version");
    }

    @Test
    public void testCall() {
        String[] expected = { System.getProperty("os.name"), System.getProperty("os.arch"),
                System.getProperty("os.version") };
        String[] result = getSystemProperties.call();
        assertArrayEquals(expected, result);
    }
}
