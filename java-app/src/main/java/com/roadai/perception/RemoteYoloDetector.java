package com.roadai.perception;

import com.roadai.domain.BoundingBox;
import com.roadai.domain.DamageClass;
import com.roadai.domain.Observation;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.yaml.snakeyaml.Yaml;

public class RemoteYoloDetector implements DamageDetector {
  private final String endpointUrl;
  private final String apiKey;
  private final HttpClient httpClient;

  public RemoteYoloDetector(String endpointUrl, String apiKey, Duration timeout) {
    this.endpointUrl = endpointUrl;
    this.apiKey = apiKey;
    this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
  }

  @Override
  public List<Observation> detect(Path image) throws ApiException {
    try {
      String boundary = "Boundary-" + UUID.randomUUID().toString();
      byte[] fileBytes = Files.readAllBytes(image);

      String header =
          "--"
              + boundary
              + "\r\n"
              + "Content-Disposition: form-data; name=\"file\"; filename=\""
              + image.getFileName()
              + "\"\r\n"
              + "Content-Type: image/jpeg\r\n\r\n";
      String footer = "\r\n--" + boundary + "--\r\n";

      byte[] headerBytes = header.getBytes();
      byte[] footerBytes = footer.getBytes();
      byte[] bodyBytes = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
      System.arraycopy(headerBytes, 0, bodyBytes, 0, headerBytes.length);
      System.arraycopy(fileBytes, 0, bodyBytes, headerBytes.length, fileBytes.length);
      System.arraycopy(
          footerBytes, 0, bodyBytes, headerBytes.length + fileBytes.length, footerBytes.length);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(endpointUrl))
              .header("X-API-Key", apiKey)
              .header("Content-Type", "multipart/form-data; boundary=" + boundary)
              .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      return parseResponse(response, image);

    } catch (IOException | InterruptedException e) {
      throw new ApiException("Failed to communicate with detection service", e);
    }
  }

  @SuppressWarnings("unchecked")
  private List<Observation> parseResponse(HttpResponse<String> response, Path image)
      throws ApiException {
    Yaml yaml = new Yaml();
    Map<String, Object> body;
    try {
      body = yaml.load(response.body());
    } catch (Exception e) {
      throw new ApiException(response.statusCode(), "BAD_RESPONSE", "Invalid JSON from service");
    }

    if (response.statusCode() >= 400) {
      String errorCode = "UNKNOWN_ERROR";
      String message = "Detection service error";
      if (body != null && body.containsKey("error")) {
        Map<String, Object> err = (Map<String, Object>) body.get("error");
        errorCode = String.valueOf(err.get("code"));
        message = String.valueOf(err.get("message"));
      }
      throw new ApiException(response.statusCode(), errorCode, message);
    }

    if (body == null) {
      throw new ApiException(500, "BAD_RESPONSE", "Empty response body");
    }

    Map<String, Object> imageInfo = (Map<String, Object>) body.get("image");
    int frameW = ((Number) imageInfo.get("w")).intValue();
    int frameH = ((Number) imageInfo.get("h")).intValue();

    List<Observation> observations = new ArrayList<>();
    List<Map<String, Object>> detections = (List<Map<String, Object>>) body.get("detections");

    if (detections != null) {
      String fileHash = image.getFileName().toString().replaceFirst("[.][^.]+$", "");
      for (Map<String, Object> det : detections) {
        String clsName = String.valueOf(det.get("class"));
        DamageClass damageClass = mapClass(clsName);
        if (damageClass == null) continue; // Ignore mapped-out classes

        double conf = ((Number) det.get("conf")).doubleValue();
        List<Number> bbox = (List<Number>) det.get("bbox");
        BoundingBox box =
            new BoundingBox(
                bbox.get(0).intValue(),
                bbox.get(1).intValue(),
                bbox.get(2).intValue(),
                bbox.get(3).intValue());

        observations.add(new Observation(fileHash, damageClass, conf, box, frameW, frameH));
      }
    }

    return observations;
  }

  private DamageClass mapClass(String pythonClass) {
    return switch (pythonClass.toUpperCase()) {
      case "D00", "CRACK_LINEAR" -> DamageClass.CRACK_LINEAR;
      case "D40", "POTHOLE" -> DamageClass.POTHOLE;
      case "ALLIGATOR" -> DamageClass.ALLIGATOR;
      case "PATCH" -> DamageClass.PATCH;
      default -> null;
    };
  }
}
