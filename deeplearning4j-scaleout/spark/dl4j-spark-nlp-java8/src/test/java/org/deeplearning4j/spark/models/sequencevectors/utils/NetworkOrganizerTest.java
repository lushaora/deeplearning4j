package org.deeplearning4j.spark.models.sequencevectors.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.deeplearning4j.spark.models.sequencevectors.primitives.NetworkInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author raver119@gmail.com
 */
@Slf4j
public class NetworkOrganizerTest {
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSelectionUniformNetworkC1() {
        List<NetworkInformation> collection = new ArrayList<>();

        for(int i = 1; i < 128; i++) {
            NetworkInformation information = new NetworkInformation();

            information.addIpAddress("192.168.0." + i);
            information.addIpAddress(getRandomIp());

            collection.add(information);
        }

        NetworkOrganizer discoverer = new NetworkOrganizer(collection, "192.168.0.0/24");

        // check for primary subset (aka Shards)
        List<String> shards = discoverer.getSubset(10);

        assertEquals(10, shards.size());

        for (String ip: shards) {
            assertNotEquals(null, ip);
            assertTrue(ip.startsWith("192.168.0"));
        }


        // check for secondary subset (aka Backup)
        List<String> backup = discoverer.getSubset(10, shards);
        assertEquals(10, backup.size());
        for (String ip: backup) {
            assertNotEquals(null, ip);
            assertTrue(ip.startsWith("192.168.0"));
            assertFalse(shards.contains(ip));
        }
    }


    @Test
    public void testSelectionDisjointNetworkC1() {
        List<NetworkInformation> collection = new ArrayList<>();

        for(int i = 1; i < 128; i++) {
            NetworkInformation information = new NetworkInformation();

            if (i < 20)
                information.addIpAddress("172.12.0." + i);

            information.addIpAddress(getRandomIp());

            collection.add(information);
        }

        NetworkOrganizer discoverer = new NetworkOrganizer(collection, "172.12.0.0/24");

        // check for primary subset (aka Shards)
        List<String> shards = discoverer.getSubset(10);

        assertEquals(10, shards.size());

        List<String> backup = discoverer.getSubset(10, shards);

        // we expect 9 here, thus backups will be either incomplete or complex sharding will be used for them

        assertEquals(9, backup.size());
        for (String ip: backup) {
            assertNotEquals(null, ip);
            assertTrue(ip.startsWith("172.12.0"));
            assertFalse(shards.contains(ip));
        }
    }


    /**
     * In this test we'll check shards selection in "casual" AWS setup
     * By default AWS box has only one IP from 172.16.0.0/12 space + local loopback IP, which isn't exposed
     *
     * @throws Exception
     */
    @Test
    public void testSelectionWithoutMaskB1() throws Exception {
        List<NetworkInformation> collection = new ArrayList<>();

        // we imitiate 512 cluster nodes here
        for(int i = 0; i < 512; i++) {
            NetworkInformation information = new NetworkInformation();

            information.addIpAddress("172."+ RandomUtils.nextInt(16, 32) +"." + RandomUtils.nextInt(1, 255) + "." + RandomUtils.nextInt(1, 255));
            collection.add(information);
        }

        NetworkOrganizer organizer = new NetworkOrganizer(collection);

        List<String> shards = organizer.getSubset(10);

        assertEquals(10, shards.size());

        List<String> backup = organizer.getSubset(10, shards);

        assertEquals(10, backup.size());
        for (String ip: backup) {
            assertNotEquals(null, ip);
            assertTrue(ip.startsWith("172."));
            assertFalse(shards.contains(ip));
        }
    }


    /**
     * Here we just check formatting for octets
     */
    @Test
    public void testFormat1() throws Exception {
        for(int i = 0; i < 256; i++) {
            String octet = NetworkOrganizer.toBinaryOctet(i);
            assertEquals(8, octet.length());
            log.trace("i: {}; Octet: {}", i, octet);
        }
    }


    @Test
    public void testFormat2() throws Exception {
        for (int i = 0; i < 1000; i++) {
            String octets = NetworkOrganizer.convertIpToOctets(getRandomIp());

            // we just expect 8 bits per bloc, 4 blocks in total, plus 3 dots between blocks
            assertEquals(35, octets.length());
        }
    }


    @Test
    public void testNetTree1() throws Exception {
        List<String> ips = Arrays.asList("192.168.0.1","192.168.0.2");

        NetworkOrganizer.VirtualTree tree = new NetworkOrganizer.VirtualTree();

        for (String ip: ips)
            tree.map(NetworkOrganizer.convertIpToOctets(ip));

        assertEquals(2, tree.getUniqueBranches());
        assertEquals(2, tree.getTotalBranches());
    }

    @Test
    public void testNetTree2() throws Exception {
        List<String> ips = Arrays.asList("192.168.12.2","192.168.0.2","192.168.0.2","192.168.62.92");

        NetworkOrganizer.VirtualTree tree = new NetworkOrganizer.VirtualTree();

        for (String ip: ips)
            tree.map(NetworkOrganizer.convertIpToOctets(ip));

        assertEquals(3, tree.getUniqueBranches());
        assertEquals(4, tree.getTotalBranches());
    }

    @Test
    public void testNetTree3() throws Exception {
        List<String> ips = new ArrayList<>();

        NetworkOrganizer.VirtualTree tree = new NetworkOrganizer.VirtualTree();

        for (int i = 0; i < 3000; i++)
            ips.add(getRandomIp());


        for (int i = 0; i < 20; i++)
            ips.add("192.168.12." + i);

        Collections.shuffle(ips);

        Set<String> uniqueIps = new HashSet<>(ips);

        for (String ip: uniqueIps)
            tree.map(NetworkOrganizer.convertIpToOctets(ip));

        assertEquals(uniqueIps.size(), tree.getTotalBranches());
        assertEquals(uniqueIps.size(), tree.getUniqueBranches());

    }

    @Test
    public void testNetTree4() throws Exception {
        List<String> ips = Arrays.asList("192.168.12.2","192.168.0.2","192.168.0.2","192.168.62.92","5.3.4.5");

        NetworkOrganizer.VirtualTree tree = new NetworkOrganizer.VirtualTree();

        for (String ip: ips)
            tree.map(NetworkOrganizer.convertIpToOctets(ip));

        assertEquals(4, tree.getUniqueBranches());
        assertEquals(5, tree.getTotalBranches());
    }

    protected String getRandomIp() {
        StringBuilder builder = new StringBuilder();

        builder.append(RandomUtils.nextInt(1, 172)).append(".");
        builder.append(RandomUtils.nextInt(0, 255)).append(".");
        builder.append(RandomUtils.nextInt(0, 255)).append(".");
        builder.append(RandomUtils.nextInt(1, 255));

        return builder.toString();
    }
}