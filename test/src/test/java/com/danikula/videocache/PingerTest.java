package com.danikula.videocache;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayOutputStream;
import java.net.Socket;

import static com.danikula.videocache.support.ProxyCacheTestUtils.getPort;
import static com.danikula.videocache.support.ProxyCacheTestUtils.getPortWithoutPing;
import static com.danikula.videocache.support.ProxyCacheTestUtils.installExternalSystemProxy;
import static com.danikula.videocache.support.ProxyCacheTestUtils.resetSystemProxy;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link Pinger}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class PingerTest extends BaseTest {

    @Before
    public void setup() throws Exception {
        resetSystemProxy();
    }

    @Test
    public void testPingSuccess() throws Exception {
        HttpProxyCacheServer server = new HttpProxyCacheServer(RuntimeEnvironment.application);
        Pinger pinger = new Pinger("127.0.0.1", getPort(server));
        boolean pinged = pinger.ping(1, 100);
        assertThat(pinged).isTrue();

        server.shutdown();
    }

    @Test
    public void testPingFail() throws Exception {
        Pinger pinger = new Pinger("127.0.0.1", 33);
        boolean pinged = pinger.ping(3, 70);
        assertThat(pinged).isFalse();
    }

    @Test
    public void testIsPingRequest() throws Exception {
        Pinger pinger = new Pinger("127.0.0.1", 1);
        assertThat(pinger.isPingRequest("ping")).isTrue();
        assertThat(pinger.isPingRequest("notPing")).isFalse();
    }

    @Test
    public void testResponseToPing() throws Exception {
        Pinger pinger = new Pinger("127.0.0.1", 1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(out);
        pinger.responseToPing(socket);
        assertThat(out.toString()).isEqualTo("HTTP/1.1 200 OK\n\nping ok");
    }

    @Test // https://github.com/danikula/AndroidVideoCache/issues/28
    public void testPingedWithExternalProxy() throws Exception {
        installExternalSystemProxy();

        HttpProxyCacheServer server = new HttpProxyCacheServer(RuntimeEnvironment.application);
        Pinger pinger = new Pinger("127.0.0.1", getPortWithoutPing(server));
        assertThat(pinger.ping(1, 100)).isTrue();
    }

    @Test // https://github.com/danikula/AndroidVideoCache/issues/28
    public void testIsNotPingedWithoutCustomProxySelector() throws Exception {
        HttpProxyCacheServer server = new HttpProxyCacheServer(RuntimeEnvironment.application);
        // IgnoreHostProxySelector is set in HttpProxyCacheServer constructor. So let reset it by custom.
        installExternalSystemProxy();

        Pinger pinger = new Pinger("127.0.0.1", getPortWithoutPing(server));
        assertThat(pinger.ping(1, 100)).isFalse();
    }
}
