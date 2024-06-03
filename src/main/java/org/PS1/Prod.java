package org.PS1;
import com.google.gson.Gson;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Properties;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.serialization.StringSerializer;
public class Prod
{
    private static final Logger log = LoggerFactory.getLogger(Prod.class);

    public static void main(String[] args)
    {
        log.info("I am a Kafka Producer");
        Scanner sc=new Scanner(System.in);
        String [] name=new String[10];
        System.out.println("Enter 10 names");
        for(int i=0;i<10;i++)
        {
            String a=sc.next();
            name[i]=a;
        }

        String bootstrapServers = "127.0.0.1:9092";

        // create Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);


        // send data - asynchronous
        String topic = "tpcs3";
        Gson gson = new Gson();
        for(int i=0;i<10;i++)
        {
            Random rand = new Random();
            double compensation = rand.nextInt(10) * 100000;
            long dob=rand.nextLong(100000, 200000);
            User user = new User(i, name[i],false,compensation,dob);
            Entity entity=new Entity(user);
            String value = gson.toJson(entity);

            //JSONObject obj = new JSONObject(value);
            String key = "id_" + Integer.toString(i);
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic,key,value);
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
        }
        // flush data - synchronous
        producer.flush();
        // flush and close producer
        producer.close();

    }

}


