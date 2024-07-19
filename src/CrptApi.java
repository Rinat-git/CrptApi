

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration duration;
    private final int requestLimit;
    private int requestCount;
    private final ScheduledExecutorService scheduler;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.duration = Duration.ofMillis(timeUnit.toMillis(1));
        this.requestLimit = requestLimit;
        this.requestCount = 0;
        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> requestCount = 0, 0, 1, timeUnit);
    }

    public void createDocument(Document document, String signature) throws IOException, InterruptedException {
        synchronized (this) {
            while (requestCount >= requestLimit) {
                wait(duration.toMillis());
            }
            requestCount++;
        }

        try {
            String jsonDocument = objectMapper.writeValueAsString(document);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt1.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response status code: " + response.statusCode());
            System.out.println("Response body: " + response.body());
        } finally {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        public static class Description {
            public String participantInn;
        }

        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
    	System.out.println("Start");
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 1);
        
        Document document = new Document();        
        document.doc_id = "1";
        document.doc_status = "new";
        document.doc_type = "LP_INTRODUCE_GOODS";
        document.importRequest = true;
        document.owner_inn = "012345678901";
        document.participant_inn = "123456789012";
        document.producer_inn = "234567890123";
        document.production_date = "2020-01-23";
        document.production_type = "food";        
        document.reg_date = "2020-01-23";
        document.reg_number = "n1";
        
        Document.Description description = new Document.Description();
        description.participantInn = "345678901234";
        document.description = description;

        Document.Product product = new Document.Product();
        product.certificate_document = "licence";
        product.certificate_document_date = "2020-01-23";
        product.certificate_document_number = "n2";
        product.owner_inn = "456789012345";
        product.producer_inn = "567890123456";
        product.production_date = "2020-01-23";
        product.tnved_code = "0123456789";
        product.uit_code = "012456789012345678LLLLLLLLLLLLL";
        product.uitu_code = "012345678901234567";
        document.products = new Document.Product[]{product};
        
        api.createDocument(document, "signatureId");
    }
}
