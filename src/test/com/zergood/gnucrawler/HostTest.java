package com.zergood.gnucrawler;

import org.junit.Test;
import java.net.InetSocketAddress;
import java.net.InetAddress;

import static org.junit.Assert.assertTrue;
/**
 *
 */
public class HostTest {
    @Test
    public void testEqualsAbsolute() throws Exception {
        Host A = new Host("174.19.25.32", 46871, true);
        Host B = new Host("174.19.25.32", 46871, true);
        assertTrue(A.equals(B));
    }

    @Test
    public void testEqualsLeafAndUpeers() throws Exception {
        Host A = new Host("174.19.25.32", 46871, true);
        Host B = new Host("174.19.25.32", 46871, false);
        assertTrue(A.equals(B));
    }

    @Test
    public void testIPresolving() throws Exception {
        InetSocketAddress address1 =  new InetSocketAddress("173.32.100.92", 23812);
    }
}
