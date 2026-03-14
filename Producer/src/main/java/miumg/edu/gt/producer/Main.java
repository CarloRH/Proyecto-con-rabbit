package miumg.edu.gt.producer;

import miumg.edu.gt.model.LoteTransacciones;
import miumg.edu.gt.model.Transaccion;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Main {

    private static final String GET_URL =
        "https://hly784ig9d.execute-api.us-east-1.amazonaws.com/default/transacciones";

    private static final String RABBITMQ_HOST = "localhost";

    public static void main(String[] args) {
        System.out.println("=== PRODUCER INICIADO ===");

        // 1. Obtener transacciones desde la API
        String jsonResponse = obtenerTransacciones();
        if (jsonResponse == null) {
            System.err.println("ERROR: No se pudo obtener transacciones.");
            return;
        }

        // 2. Parsear el JSON
        ObjectMapper mapper = new ObjectMapper();
        LoteTransacciones lote;
        try {
            lote = mapper.readValue(jsonResponse, LoteTransacciones.class);
            System.out.println("Lote recibido: " + lote.getLoteId());
            System.out.println("Total transacciones: " + lote.getTransacciones().size());
        } catch (Exception e) {
            System.err.println("ERROR al parsear JSON: " + e.getMessage());
            return;
        }

        // 3. Conectar a RabbitMQ y publicar mensajes
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            for (Transaccion transaccion : lote.getTransacciones()) {
                String banco = transaccion.getBancoDestino();

                // Crear cola si no existe (durable = true para no perder mensajes)
                channel.queueDeclare(banco, true, false, false, null);

                // Convertir transaccion a JSON
                String mensaje = mapper.writeValueAsString(transaccion);

                // Publicar en la cola del banco correspondiente
                channel.basicPublish("", banco, null, mensaje.getBytes());

                System.out.println("Enviado a cola [" + banco + "]: " + transaccion.getIdTransaccion());
            }

            System.out.println("=== TODAS LAS TRANSACCIONES ENVIADAS ===");

        } catch (Exception e) {
            System.err.println("ERROR con RabbitMQ: " + e.getMessage());
        }
    }

    private static String obtenerTransacciones() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GET_URL))
                .header("Content-Type", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("GET exitoso.");
                return response.body();
            } else {
                System.err.println("GET fallido. Status: " + response.statusCode());
                return null;
            }

        } catch (Exception e) {
            System.err.println("ERROR en HTTP GET: " + e.getMessage());
            return null;
        }
    }
}
