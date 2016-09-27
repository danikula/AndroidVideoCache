package com.danikula.videocache;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

/**
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class GetRequestTest extends BaseTest {

    @Test
    public void testPartialHttpGet() throws Exception {
        GetRequest getRequest = new GetRequest("" +
                "GET /uri HTTP/1.1\n" +
                "Host: 127.0.0.1:44684\n" +
                "Range: bytes=9860723-" +
                "Accept-Encoding: gzip");
        assertThat(getRequest.rangeOffset).isEqualTo(9860723);
        assertThat(getRequest.uri).isEqualTo("uri");
        assertThat(getRequest.partial).isTrue();
    }

    @Test
    public void testNotPartialHttpGet() throws Exception {
        GetRequest getRequest = new GetRequest("" +
                "GET /uri HTTP/1.1\n" +
                "Host: 127.0.0.1:44684\n" +
                "Accept-Encoding: gzip");
        assertThat(getRequest.rangeOffset).isEqualTo(0);
        assertThat(getRequest.uri).isEqualTo("uri");
        assertThat(getRequest.partial).isFalse();
    }

    @Test
    public void testReadStream() throws Exception {
        String requestString = "GET /uri HTTP/1.1\nRange: bytes=9860723-\n";
        InputStream stream = new ByteArrayInputStream(requestString.getBytes());
        GetRequest getRequest = GetRequest.read(stream);
        assertThat(getRequest.rangeOffset).isEqualTo(9860723);
        assertThat(getRequest.uri).isEqualTo("uri");
        assertThat(getRequest.partial).isTrue();
    }

    @Test
    public void testMinimal() throws Exception {
        GetRequest getRequest = new GetRequest("GET /uri HTTP/1.1");
        assertThat(getRequest.rangeOffset).isEqualTo(0);
        assertThat(getRequest.uri).isEqualTo("uri");
        assertThat(getRequest.partial).isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmpty() throws Exception {
        GetRequest getRequest = new GetRequest("");
        fail("Empty request");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalid() throws Exception {
        GetRequest getRequest = new GetRequest("/uri HTTP/1.1\n");
        fail("Invalid request");
    }

}
