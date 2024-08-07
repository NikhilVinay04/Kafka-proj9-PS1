package org.PS1;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import java.util.Properties;
import org.apache.kafka.common.serialization.StringSerializer;

// This class serves as a Kafka producer for Sales type data.
// It reads data from the sales_data.json file and sends it to the Kafka cluster.
// It also updates the Inventory quantity of the item being sold using the PATCH REST request of Gilhari.
public class Producer_Sales {
    private static final Logger log = LoggerFactory.getLogger(Producer_Sales.class);

    public static void main(String[] args) {
        log.info("I am a Kafka Producer");
        HttpClient client = HttpClient.newHttpClient();
        String bootstrapServers = "127.0.0.1:9092";

        // create Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the first id not yet posted");
        int startID = sc.nextInt();
        double currentQuantity = 0.0, quantity = 0.0;
        // send data - asynchronous
        String topic = "Sales";
        try {
            String salesJson = new String(Files.readAllBytes(Paths.get("src/main/java/org/PS1/sales_data.json")), StandardCharsets.UTF_8);
            JSONArray salesArray = new JSONArray(salesJson);

            for (int i = 0; i < salesArray.length(); i++) {
                JSONObject s = salesArray.getJSONObject(i);
                if (Integer.parseInt(s.getString("id")) >= startID) {
                    String key = "id_" + s.getString("id");

                    JSONObject entity = new JSONObject();
                    entity.put("entity",s);
                    String value = entity.toString();
                    ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, value);
                    producer.send(producerRecord, new Callback() {
                        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                            // executes every time a record is successfully sent or an exception is thrown
                            if (e == null) {
                                // the record was successfully sent
                                log.info("Received new metadata. \n" +
                                        "Topic:" + recordMetadata.topic() + "\n" +
                                        "Key:" + producerRecord.key() + "\n" +
                                        "Partition: " + recordMetadata.partition() + "\n" +
                                        "Offset: " + recordMetadata.offset() + "\n" +
                                        "Timestamp: " + recordMetadata.timestamp());
                            } else {
                                log.error("Error while producing", e);
                            }
                        }
                    });
                    try {
                        URI uri = URI.create("http://localhost:80/gilhari/v1/Inventory/getObjectById?filter=itemID=" + s.getInt("itemID"));
                        HttpRequest request = HttpRequest.newBuilder()
                                .GET()
                                .uri(uri)
                                .build();

                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        int getStatusCode = response.statusCode();
                        System.out.println(getStatusCode);

                        JSONObject inv = new JSONObject(response.body());
                        currentQuantity = inv.getDouble("quantity");
                        quantity = s.getDouble("quantity");
                        double currInv = currentQuantity - quantity;
                        JSONObject js = new JSONObject();
                        JSONArray jsa = new JSONArray();
                        jsa.put("quantity");
                        jsa.put(currInv);
                        js.put("newValues", jsa);

                        URI uri2 = URI.create("http://localhost:80/gilhari/v1/Inventory?filter=itemID=" + s.getInt("itemID"));
                        HttpRequest req1 = HttpRequest.newBuilder()
                                .uri(uri2)
                                .method("PATCH", HttpRequest.BodyPublishers.ofString(js.toString()))
                                .header("Content-Type", "application/json")
                                .build();

                        HttpResponse<String> resp1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
                        int patchStatusCode = resp1.statusCode();
                        System.out.println(patchStatusCode);

                        if (patchStatusCode >= 200 && patchStatusCode < 300) {
                            log.info("Response from API: " + resp1.body());
                            continue;
                        } else {
                            log.error("HTTP error code: " + patchStatusCode);
                        }

                    } catch (Exception e) {
                        log.error("Exception occurred while sending HTTP request: {}", e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        // flush data - synchronous
        producer.flush();
        // flush and close producer
        producer.close();
    }
}
