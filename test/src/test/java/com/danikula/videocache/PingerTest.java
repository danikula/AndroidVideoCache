package com.danikula.videocache;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link Pinger}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class PingerTest extends BaseTest {

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

    private int getPort(HttpProxyCacheServer server) {
        String proxyUrl = server.getProxyUrl("test");
        Pattern pattern = Pattern.compile("http://127.0.0.1:(\\d*)/test");
        Matcher matcher = pattern.matcher(proxyUrl);
        assertThat(matcher.find()).isTrue();
        String portAsString = matcher.group(1);
        return Integer.parseInt(portAsString);
    }
}
