package uk.co.mattburns.pwinty;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import uk.co.mattburns.pwinty.Order.Status;
import uk.co.mattburns.pwinty.Photo.Sizing;
import uk.co.mattburns.pwinty.Photo.Type;
import uk.co.mattburns.pwinty.gson.TypeDeserializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.api.representation.Form;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;

public class Pwinty {

    private String merchantId;
    private String apiKey;

    private WebResource webResource;

    /**
     * This is the main class for talking to the Pwinty API. See
     * http://www.pwinty.com/api.html for detailed examples.
     * 
     * For all method calls, if an error occurs, a {@link PwintyError} will be
     * thrown
     * 
     * @param environment
     *            Choose SANDBOX for testing and LIVE for real orders you wish
     *            to print
     * @param merchantId
     *            As supplied by http://www.pwinty.com
     * @param apiKey
     *            As supplied by http://www.pwinty.com
     */
    public Pwinty(Environment environment, String merchantId, String apiKey) {
        this(null, environment, merchantId, apiKey);
    }

    /**
     * This is the main class for talking to the Pwinty API. See
     * http://www.pwinty.com/api.html
     * 
     * @param environment
     *            Choose SANDBOX for testing and LIVE for real orders you wish
     *            to print
     * @param merchantId
     *            As supplied by http://www.pwinty.com
     * @param apiKey
     *            As supplied by http://www.pwinty.com
     * @param loggingStream
     *            Requests and responses will be written to this
     */
    public Pwinty(Environment environment, String merchantId, String apiKey,
            PrintStream loggingStream) {
        this(new LoggingFilter(loggingStream), environment, merchantId, apiKey);
    }

    /**
     * This is the main class for talking to the Pwinty API. See
     * http://www.pwinty.com/api.html
     * 
     * @param environment
     *            Choose SANDBOX for testing and LIVE for real orders you wish
     *            to print
     * @param merchantId
     *            As supplied by http://www.pwinty.com
     * @param apiKey
     *            As supplied by http://www.pwinty.com
     * @param logger
     *            Requests and responses will be written to this
     */
    public Pwinty(Environment environment, String merchantId, String apiKey,
            Logger logger) {
        this(new LoggingFilter(logger), environment, merchantId, apiKey);
    }
    private String getOrderUpdateUrl(int orderId) {
    	return String.format("Orders/%s", orderId);
    }
    private String getAddPhotoUrl(int orderId) {
    	return String.format("Orders/%s/Photos", orderId);
    }
    private String getSubmissionStatusUrl(int orderId) {
    	return String.format("Orders/%s/SubmissionStatus", orderId);
    }
    private String getPhotoUrl(int orderId, int photoId) {
    	return String.format("Orders/%s/Photos/%s", orderId,photoId);
	}
    private String getOrderStatusUrl(int orderId) {
    	return String.format("Orders/%s/Status", orderId);
	}
    private Pwinty(LoggingFilter loggingFilter, Environment environment,
            String merchantId, String apiKey) {
        this.merchantId = merchantId;
        this.apiKey = apiKey;

        Client client = Client.create();
        if (loggingFilter != null) {
            client.addFilter(loggingFilter);
        }
        client.setConnectTimeout(0);
        client.setReadTimeout(0);

        webResource = client.resource(environment.url);
    }

    public List<Order> getOrders() {
        String ordersJSON = webResource.path("Orders")
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("X-Pwinty-MerchantId", merchantId)
                .header("X-Pwinty-REST-API-Key", apiKey).get(String.class);
        Order[] orders = createGson().fromJson(ordersJSON, Order[].class);

        for (Order order : orders) {
            order.setPwinty(this);
        }
        return Arrays.asList(orders);
    }

    public Order getOrder(int orderId) {
        ClientResponse response = webResource.path(getOrderUpdateUrl(orderId))
                .queryParam("id", "" + orderId)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("X-Pwinty-MerchantId", merchantId)
                .header("X-Pwinty-REST-API-Key", apiKey)
                .get(ClientResponse.class);

        Order order = createReponse(response, Order.class);
        order.setPwinty(this);
        return order;
    }

    Order createOrder(Order newOrder) {
        Form form = createOrderForm(newOrder);
        ClientResponse response = webResource.path("Orders")
                .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("X-Pwinty-MerchantId", merchantId)
                .header("X-Pwinty-REST-API-Key", apiKey)
                .post(ClientResponse.class, form);

        return createReponse(response, Order.class);
    }
    
    Order updateOrder(int orderId, Order newOrder) {
        Form form = createOrderForm(newOrder);
        String url  = getOrderUpdateUrl(orderId);
        ClientResponse response = webResource.path(url)
                .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("X-Pwinty-MerchantId", merchantId)
                .header("X-Pwinty-REST-API-Key", apiKey)
                .put(ClientResponse.class, form);

        return createReponse(response, Order.class);
    }

    Gson createGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Type.class, new TypeDeserializer());
        return gsonBuilder.create();
    }

    SubmissionStatus getSubmissionStatus(int orderId) {
        ClientResponse response = webResource.path(getSubmissionStatusUrl(orderId))
                .queryParam("id", "" + orderId)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("X-Pwinty-MerchantId", merchantId)
                .header("X-Pwinty-REST-API-Key", apiKey)
                .get(ClientResponse.class);

        return createReponse(response, SubmissionStatus.class);
    }

    /**
     * Add a photo File object to the order. This method will block until the
     * File is uploaded.
     */
    Photo addPhotoToOrder(int orderId, File photo, Photo.Type type, int copies,
            Sizing sizing) {
        return addPhotoToOrder(orderId, photo, null, type, copies, sizing);
    }

    /**
     * Add a photo to the order using a public URL.
     */
    Photo addPhotoToOrder(int orderId, URL photoUrl, Photo.Type type,
            int copies, Sizing sizing) {
        return addPhotoToOrder(orderId, null, photoUrl, type, copies, sizing);
    }

    /**
     * Either the File or URL must be supplied
     */
    private Photo addPhotoToOrder(int orderId, File photo, URL photoUrl,
            Photo.Type type, int copies, Sizing sizing) {

        @SuppressWarnings("resource")
        FormDataMultiPart form = new FormDataMultiPart()
                .field("type", type.toString())
                .field("sizing", sizing.toString())
                .field("copies", "" + copies).field("orderId", "" + orderId);

        if (photo != null) {
            form.bodyPart(new FileDataBodyPart("file", photo,
                    MediaType.MULTIPART_FORM_DATA_TYPE));
        } else {
            form = form.field("url", photoUrl.toExternalForm());
        }
        String uploadPath = getAddPhotoUrl(orderId);
        ClientResponse response = webResource.path(uploadPath)
                .type(MediaType.MULTIPART_FORM_DATA_TYPE)
                .header("X-Pwinty-MerchantId", merchantId)
                .header("X-Pwinty-REST-API-Key", apiKey)
                .post(ClientResponse.class, form);

        throwIfBad(response);

        return createReponse(response, Photo.class);
    }

    public Photo getPhoto(int orderId,int photoId) {
        ClientResponse response = webResource.path(getPhotoUrl(orderId,photoId))
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("X-Pwinty-MerchantId", merchantId)
                .header("X-Pwinty-REST-API-Key", apiKey)
                .get(ClientResponse.class);

        return createReponse(response, Photo.class);
    }



	void deletePhoto(int orderId,int photoId) {
 
        ClientResponse response =  webResource.path(getPhotoUrl(orderId, photoId))
                .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("X-Pwinty-MerchantId", merchantId)
                .header("X-Pwinty-REST-API-Key", apiKey)
                .delete(ClientResponse.class);
        throwIfBad(response);
    }

    /**
     * Submit the Order for printing and shipping
     * 
     * If an error occurs, a {@link PwintyError} will be thrown
     */
    void submitOrder(int orderId) {
        updateOrder(orderId, Status.Submitted);
    }

    /**
     * If an error occurs, a {@link PwintyError} will be thrown
     */
    void cancelOrder(int orderId) {
        updateOrder(orderId, Status.Cancelled);
    }

    void updateOrder(int orderId, Status status) {
        Form form = new Form();
        form.add("id", orderId);
        form.add("status", status);

        ClientResponse response = webResource.path(getOrderStatusUrl(orderId))
                .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("X-Pwinty-MerchantId", merchantId)
                .header("X-Pwinty-REST-API-Key", apiKey)
                .post(ClientResponse.class, form);

        throwIfBad(response);
    }



    void deleteDocument(int documentId) {
        Form form = new Form();
        form.add("id", documentId);

        ClientResponse response = webResource.path("Documents")
                .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("X-Pwinty-MerchantId", merchantId)
                .header("X-Pwinty-REST-API-Key", apiKey)
                .delete(ClientResponse.class, form);

        throwIfBad(response);
    }

    private Form createOrderForm(Order newOrder) {
        Form form = new Form();
        form.add("recipientName", newOrder.getRecipientName());
        form.add("address1", newOrder.getAddress1());
        form.add("address2", newOrder.getAddress2());
        form.add("addressTownOrCity", newOrder.getAddressTownOrCity());
        form.add("stateOrCounty", newOrder.getStateOrCounty());
        form.add("postalOrZipCode", newOrder.getPostalOrZipCode());
        form.add("countryCode", newOrder.getCountryCode());
        form.add("qualityLevel", newOrder.getQualityLevel());
        return form;
    }

    private <T> T createReponse(ClientResponse response, Class<T> type) {
        throwIfBad(response);
        Gson gson = createGson();
        return gson.fromJson(response.getEntity(String.class), type);
    }

    private void throwIfBad(ClientResponse response) {
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            throw toError(response);
        }
    }

    private PwintyError toError(ClientResponse response) {
    	String jsonResponse = response.getEntity(String.class);
        PwintyError error = createGson().fromJson(
                jsonResponse, PwintyError.class);
        if (error == null) {
            error = new PwintyError();
        }
        error.setCode(response.getStatus());
        return error;
    }

    public enum Environment {
        LIVE("https://api.pwinty.com/v2"), SANDBOX("http://local.pwinty.com/v2");

        private String url;

        private Environment(String url) {
            this.url = url;
        }
    }
}
