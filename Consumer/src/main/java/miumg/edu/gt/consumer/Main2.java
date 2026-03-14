package miumg.edu.gt.consumer;

import miumg.edu.gt.model.TransaccionPost;
import miumg.edu.gt.model.Transaccion;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class Main2 {
	
	//Main2 basado en Main original para cola rechazados
	
    private static final String RABBITMQ_HOST   = "localhost";
    private static final String POST_URL        =
        "https://7e0d9ogwzd.execute-api.us-east-1.amazonaws.com/default/guardarTransacciones";

    private static final String NOMBRE          = "Carlo René Hermógenes Rivera Estrada";
    private static final String CARNET          = "0905-24-7010";

    private static final String[] COLAS         = {"BANRURAL", "GYT", "BAC", "BI"};
    private static final String COLA_RECHAZADOS = "cola_rechazados";
    private static final double MONTO_MINIMO    = 4000.0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== CONSUMER V2 INICIADO ===");
        System.out.println("Solo se procesan transacciones con monto > Q" + MONTO_MINIMO);
        System.out.println("------------------------------------------------------------");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        ObjectMapper mapper = new ObjectMapper();
        HttpClient httpClient = HttpClient.newHttpClient();

        channel.basicQos(1);

        channel.queueDeclare(COLA_RECHAZADOS, true, false, false, null);
        System.out.println("Cola de rechazados lista: " + COLA_RECHAZADOS);
        System.out.println("------------------------------------------------------------");

        for (String cola : COLAS) {
            channel.queueDeclare(cola, true, false, false, null);
            System.out.println("Escuchando cola: " + cola);

            DeliverCallback callback = (consumerTag, delivery) -> {
                long deliveryTag = delivery.getEnvelope().getDeliveryTag();
                String mensaje = new String(delivery.getBody(), StandardCharsets.UTF_8);

                try {
                    Transaccion transaccion = mapper.readValue(mensaje, Transaccion.class);
                    String id     = transaccion.getIdTransaccion();
                    double monto  = transaccion.getMonto();

                    if (monto > MONTO_MINIMO) {
                        TransaccionPost body = new TransaccionPost(transaccion, NOMBRE, CARNET);
                        boolean exito = enviarPost(httpClient, mapper, body);

                        if (exito) {
                            channel.basicAck(deliveryTag, false);
                            System.out.printf("[%s] ID: %-12s | Monto: Q%10.2f | Estado: ACEPTADA%n",
                                cola, id, monto);
                        } else {
                            channel.basicNack(deliveryTag, false, true);
                            System.err.printf("[%s] ID: %-12s | Monto: Q%10.2f | Estado: REINTENTANDO%n",
                                cola, id, monto);
                        }

                    } else {
                        channel.basicPublish("", COLA_RECHAZADOS, null, mensaje.getBytes());
                        channel.basicAck(deliveryTag, false);
                        System.out.printf("[%s] ID: %-12s | Monto: Q%10.2f | Estado: RECHAZADA → cola_rechazados%n",
                            cola, id, monto);
                    }

                } catch (Exception e) {
                    System.err.println("[" + cola + "] ERROR procesando mensaje: " + e.getMessage());
                    channel.basicNack(deliveryTag, false, true);
                }
            };

            channel.basicConsume(cola, false, callback, consumerTag -> {});
        }

        System.out.println("------------------------------------------------------------");
        System.out.println("=== ESPERANDO MENSAJES (Ctrl+C o Stop para detener) ===");
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

            return response.statusCode() == 200 || response.statusCode() == 201;

        } catch (Exception e) {
            System.err.println("ERROR en POST: " + e.getMessage());
            return false;
        }
    }
}