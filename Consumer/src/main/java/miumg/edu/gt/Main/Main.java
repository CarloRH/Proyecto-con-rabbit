package miumg.edu.gt.Main;

import miumg.edu.gt.model.TransaccionPost;
import miumg.edu.gt.model.Transaccion;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class Main {

    private static final String RABBITMQ_HOST = "localhost";
    private static final String POST_URL =
        "https://7e0d9ogwzd.execute-api.us-east-1.amazonaws.com/default/guardarTransacciones";

    private static final String NOMBRE = "Carlo René Hermógenes Rivera Estrada";
    private static final String CARNET  = "0905-24-7010";

    // Los 4 bancos que encontró el Producer
    private static final String[] COLAS = {"BANRURAL", "GYT", "BAC", "BI"};

    public static void main(String[] args) throws Exception {
        System.out.println("=== CONSUMER INICIADO ===");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        ObjectMapper mapper = new ObjectMapper();
        HttpClient httpClient = HttpClient.newHttpClient();

        // Procesar de a 1 mensaje a la vez por canal
        channel.basicQos(1);

        for (String cola : COLAS) {
            // Declarar la cola por si el Consumer arranca antes que el Producer
            channel.queueDeclare(cola, true, false, false, null);
            System.out.println("Escuchando cola: " + cola);

            DeliverCallback callback = (consumerTag, delivery) -> {
                long deliveryTag = delivery.getEnvelope().getDeliveryTag();
                String mensaje = new String(delivery.getBody(), StandardCharsets.UTF_8);

                try {
                    // 1. Deserializar JSON a objeto Java
                    Transaccion transaccion = mapper.readValue(mensaje, Transaccion.class);
                    System.out.println("[" + cola + "] Procesando: " + transaccion.getIdTransaccion());

                    // 2. Agregar nombre y carnet
                    TransaccionPost body = new TransaccionPost(transaccion, NOMBRE, CARNET);

                    // 3. Enviar POST
                    boolean exito = enviarPost(httpClient, mapper, body);

                    if (exito) {
                        // 4. ACK solo si el POST fue exitoso
                        channel.basicAck(deliveryTag, false);
                        System.out.println("[" + cola + "] ACK enviado: " + transaccion.getIdTransaccion());
                    } else {
                        // Reintento: devolver el mensaje a la cola
                        System.err.println("[" + cola + "] POST falló, reintentando: " + transaccion.getIdTransaccion());
                        channel.basicNack(deliveryTag, false, true);
                    }

                } catch (Exception e) {
                    System.err.println("[" + cola + "] ERROR procesando mensaje: " + e.getMessage());
                    // Devolver a la cola para no perder el mensaje
                    channel.basicNack(deliveryTag, false, true);
                }
            };

            // false = ACK manual
            channel.basicConsume(cola, false, callback, consumerTag -> {});
        }

        // Mantener el Consumer vivo escuchando
        System.out.println("=== ESPERANDO MENSAJES (Ctrl+C para detener) ===");
        Thread.currentThread().join();
    }

    private static boolean enviarPost(HttpClient httpClient, ObjectMapper mapper, TransaccionPost body) {
        try {
            String json = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(POST_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return true;
            } else {
                System.err.println("POST status: " + response.statusCode() + " - " + response.body());
                return false;
            }

        } catch (Exception e) {
            System.err.println("ERROR en POST: " + e.getMessage());
            return false;
        }
    }
}