package com.github.jenspiegsa.mockitoextension;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * @author Jens Piegsa
 */
public class SampleClient {

	private static final Logger log = Logger.getLogger(SampleClient.class.getSimpleName());

	private final Client client;
	final WebTarget target;

	public SampleClient(final String uri) {
		client = ClientBuilder.newClient();
		target = client.target(UriBuilder.fromUri(uri).build());
		log.info(() -> "Sample client targeting uri: " + uri);
	}

	public boolean isOk() {
		try {
			final Response response = target.request().get();
			final int status = response.getStatus();
			response.close();
			return status == Response.Status.OK.getStatusCode();
		} catch (final ResponseProcessingException e) {
			e.getResponse().close();
		} catch (final ProcessingException e) {
			log.log(Level.WARNING, e, e::getMessage);
		}
		return false;
	}

	public void close() {
		client.close();
	}
}
