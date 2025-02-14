import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;

public class WebServer {
    private static final String TASK_ENDPOINT = "/task";
    private static final String STATUS_ENDPOINT = "/status";
    private static final String SEARCH_TOKEN_ENDPOINT = "/searchtoken"; // Nuevo endpoint

    private final int port;
    private HttpServer server;
    public static void main(String[] args) {
        int serverPort = 8081;
        if (args.length == 1) {
            serverPort = Integer.parseInt(args[0]);
        }
        WebServer webServer = new WebServer(serverPort);
        webServer.startServer();
        System.out.println("Servidor escuchando en el puerto " + serverPort);
    }
    public WebServer(int port) {
        this.port = port;
    }
    public void startServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        HttpContext statusContext = server.createContext(STATUS_ENDPOINT);
        HttpContext taskContext = server.createContext(TASK_ENDPOINT);
        HttpContext searchTokenContext = server.createContext(SEARCH_TOKEN_ENDPOINT);

        
        statusContext.setHandler(this::handleStatusCheckRequest);
        taskContext.setHandler(this::handleTaskRequest);
        searchTokenContext.setHandler(this::handleSearchTokenRequest);

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }

        private void handleSearchTokenRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.close();
            return;
        }

        Headers headers = exchange.getRequestHeaders();
        boolean isDebugMode = headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true");

        long startTime = System.nanoTime(); // Inicio del contador de tiempo

        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        String requestBody = new String(requestBytes);
        String[] params = requestBody.split(",");
        if (params.length != 2) {
            sendResponse("Invalid request".getBytes(), exchange);
            return;
        }

        // Extracción de parámetros para la generación de la cadena y búsqueda de la subcadena
        int tokenNumber = Integer.parseInt(params[0]);
        String substring = params[1];
        String tokenChain = generateTokenChain(tokenNumber); // Generación de la cadena de tokens
        int count = searchSubstring(tokenChain, substring); // Búsqueda de la subcadena

        String responseMessage = "La subcadena " + substring + " fue encontrada " + count + " veces\n";
        byte[] responseBytes = responseMessage.getBytes();

        long finishTime = System.nanoTime(); // Final del contador de tiempolong startTime = System.nanoTime();
        long durationInNanos = finishTime - startTime;


           if (isDebugMode) {
        long durationInMillis = durationInNanos / 1_000_000;
        long durationInSeconds = durationInMillis / 1000;
        long millisPart = durationInMillis % 1000;
    String debugMessage = String.format("La operacion tomo %d nanosegundos = %d segundos con %d milisegundos", durationInNanos, durationInSeconds, millisPart);
    exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
}
  

        sendResponse(responseBytes, exchange);
    }
    

    // Método para generar una cadena de tokens aleatorios
    private String generateTokenChain(int length) {
        Random random = new Random(); 
        StringBuilder tokenChain = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = (char) ('A' + random.nextInt(26));
            tokenChain.append(c);
        }
        return tokenChain.toString();
    }

    // Método para buscar una subcadena dentro de una cadena mayor
    private int searchSubstring(String tokenChain, String substring) {
        int count = 0;
        for (int i = 0; i < tokenChain.length() - substring.length() + 1; i++) {
            if (tokenChain.substring(i, i + substring.length()).equals(substring)) {
                count++;
            }
        }
        return count;
    }
    
    private void handleTaskRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
            exchange.close();
            return;
        }
        Headers headers = exchange.getRequestHeaders();
        if (headers.containsKey("X-Test") && headers.get("X-Test").get(0).equalsIgnoreCase("true")) {
            String dummyResponse = "123\n";
            sendResponse(dummyResponse.getBytes(), exchange);
            return;
        }
         boolean isDebugMode = false;
    if (headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true")) {
        isDebugMode = true;
    }
        long startTime = System.nanoTime();
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        byte[] responseBytes = calculateResponse(requestBytes);
        long finishTime = System.nanoTime();
        long durationInNanos = finishTime - startTime;
        if (isDebugMode) {
        long durationInMillis = durationInNanos / 1_000_000;
        long durationInSeconds = durationInMillis / 1000;
        long millisPart = durationInMillis % 1000;
    String debugMessage = String.format("La operacion tomo %d nanosegundos = %d segundos con %d milisegundos", durationInNanos, durationInSeconds, millisPart);
    exchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
}
        sendResponse(responseBytes, exchange);
    }
    private byte[] calculateResponse(byte[] requestBytes) {
        String bodyString = new String(requestBytes);
        String[] stringNumbers = bodyString.split(",");
        BigInteger result = BigInteger.ONE;
        for (String number : stringNumbers) {
            BigInteger bigInteger = new BigInteger(number);
            result = result.multiply(bigInteger);
        }
        return String.format("El resultado de la multiplicación es %s\n", result).getBytes();
    }
    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }
        String responseMessage = "El servidor está vivo\n";
        sendResponse(responseMessage.getBytes(), exchange);
    }
    private void sendResponse(byte[] responseBytes, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
        exchange.close();
    }
}