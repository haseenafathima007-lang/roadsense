package com.roadai.perception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.roadai.domain.DamageClass;
import com.roadai.domain.Observation;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RemoteYoloDetectorTest {
  private static HttpServer server;
  private static int port;
  private static String currentResponse;
  private static int currentStatus;
  private static String currentExpectedKey;

  @BeforeAll
  static void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    port = server.getAddress().getPort();
    server.createContext(
        "/v1/detect/image",
        new HttpHandler() {
          @Override
          public void handle(HttpExchange exchange) throws IOException {
            if (currentExpectedKey != null) {
              String auth = exchange.getRequestHeaders().getFirst("X-API-Key");
              if (!currentExpectedKey.equals(auth)) {
                String resp = "{\"error\": {\"code\": \"UNAUTHORIZED\", \"message\": \"Bad key\"}}";
                exchange.sendResponseHeaders(401, resp.length());
                try (OutputStream os = exchange.getResponseBody()) {
                  os.write(resp.getBytes(StandardCharsets.UTF_8));
                }
                return;
              }
            }

            exchange.sendResponseHeaders(currentStatus, currentResponse.length());
            try (OutputStream os = exchange.getResponseBody()) {
              os.write(currentResponse.getBytes(StandardCharsets.UTF_8));
            }
          }
        });
    server.start();
  }

  @AfterAll
  static void stopServer() {
    server.stop(0);
  }

  @Test
  void happyPath_parsesDetections(@TempDir Path tempDir) throws Exception {
    currentStatus = 200;
    currentExpectedKey = "test-key";
    currentResponse =
        """
            {
              "image": {"w": 640, "h": 480},
              "detections": [
                {"class": "D00", "conf": 0.85, "bbox": [10, 20, 110, 120]},
                {"class": "pothole", "conf": 0.95, "bbox": [0, 0, 50, 50]},
                {"class": "unknown", "conf": 0.5, "bbox": [0, 0, 10, 10]}
              ]
            }
            """;

    Path dummyImage = tempDir.resolve("dummy.jpg");
    Files.write(dummyImage, new byte[] {1, 2, 3});

    String url = "http://localhost:" + port + "/v1/detect/image";
    RemoteYoloDetector detector = new RemoteYoloDetector(url, "test-key", Duration.ofSeconds(2));

    List<Observation> obs = detector.detect(dummyImage);

    assertThat(obs).hasSize(2); // "unknown" is filtered out

    Observation d00 = obs.get(0);
    assertThat(d00.damageClass()).isEqualTo(DamageClass.CRACK_LINEAR);
    assertThat(d00.confidence()).isEqualTo(0.85);
    assertThat(d00.box().x1()).isEqualTo(10);

    Observation pothole = obs.get(1);
    assertThat(pothole.damageClass()).isEqualTo(DamageClass.POTHOLE);
  }

  @Test
  void unauthorized_throwsApiException(@TempDir Path tempDir) throws Exception {
    currentExpectedKey = "test-key"; // we will pass a wrong key
    Path dummyImage = tempDir.resolve("dummy.jpg");
    Files.write(dummyImage, new byte[] {1, 2, 3});

    String url = "http://localhost:" + port + "/v1/detect/image";
    RemoteYoloDetector detector = new RemoteYoloDetector(url, "wrong-key", Duration.ofSeconds(2));

    assertThatThrownBy(() -> detector.detect(dummyImage))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Bad key")
        .extracting("statusCode")
        .isEqualTo(401);
  }
}
