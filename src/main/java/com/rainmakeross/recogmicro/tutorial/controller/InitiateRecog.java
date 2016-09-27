package com.rainmakeross.recogmicro.tutorial.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainmakeross.recogmicro.tutorial.model.FaceRectangle;
import com.rainmakeross.recogmicro.tutorial.model.MSFaceJson;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RestController
public class InitiateRecog {
    @Value("${microsoft.api.key}")
    private String MICROSOFT_API_KEY;

    private String MICROSOT_REQ_URL = "https://api.projectoxford.ai/face/v1.0/detect";

    private AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();

    private ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping("/")
    public String index() throws ExecutionException, InterruptedException, IOException {
        byte[] imageBytes = scrapImageFromInstagram("http://bellard.org/bpg/lena30.jpg");
        /*
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("url", "http://bellard.org/bpg/lena30.jpg");
        System.out.println(node.toString());*/
        Future<Response> resp = asyncHttpClient.preparePost(MICROSOT_REQ_URL)
                .addHeader("Ocp-Apim-Subscription-Key", MICROSOFT_API_KEY)
                .addHeader("Content-type", "application/octet-stream")
                .setBody(imageBytes).execute();

        writeImgToFile(drawRectangles(faceJsonsFromResponse(resp.get().getResponseBody()),
                bufImageFromBytes(imageBytes)));

        return resp.get().getResponseBody();
    }

    private byte[] scrapImageFromInstagram(String imageUrl) throws ExecutionException, InterruptedException {
        Future<Response> f = asyncHttpClient.prepareGet(imageUrl).execute();
        return f.get().getResponseBodyAsBytes();
    }

    private BufferedImage bufImageFromBytes(byte[] imageInByte) throws IOException {
        // convert byte array back to BufferedImage
        InputStream in = new ByteArrayInputStream(imageInByte);
        return ImageIO.read(in);
    }

    private List<MSFaceJson> faceJsonsFromResponse(String respStr) throws IOException {
        return objectMapper.readValue(respStr, new TypeReference<List<MSFaceJson>>(){});
    }

    private BufferedImage drawRectangles(List<MSFaceJson> faceJsonList, BufferedImage originalImg){
        Graphics2D graph = originalImg.createGraphics();
        for(MSFaceJson faceJson: faceJsonList) {
            FaceRectangle faceRectangle = faceJson.getFaceRectangle();
            Stroke oldStroke = graph.getStroke();
            graph.setStroke(new BasicStroke(2.0f));
            graph.drawRect(faceRectangle.getLeft(), faceRectangle.getTop(),
                    faceRectangle.getWidth(), faceRectangle.getHeight());
            graph.setStroke(oldStroke);
        }

        graph.dispose();
        return originalImg;
    }

    private void writeImgToFile(BufferedImage imgContent) throws IOException {
        ImageIO.write(imgContent, "jpg", new File(
                "lena-recognized.jpg"));

    }
}
