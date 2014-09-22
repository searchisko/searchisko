package org.searchisko.api.filter;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.http.HttpInfo;
import org.searchisko.api.rest.security.HttpBasicChallengeInterceptor;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.ElasticsearchClientService;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.service.StatsClientService;
import org.searchisko.api.util.SearchUtils;

/**
 * This is a filter that allows to call internal ES instances for authenticated and authorized users. You can use
 * <code>useStatsClient</code> init param with any nonempty value to handle stats ES client, search client is used
 * otherwise.
 * <p>
 * Inspired by <a href="https://github.com/mitre/HTTP-Proxy-Servlet">Smiley's HTTP Proxy Servlet</a>, thanks.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ESProxyFilter implements Filter {

	protected static final String CFG_USE_STATS_CLIENT = "useStatsClient";

	@Inject
	protected Logger log;

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	protected StatsClientService statsClientService;

	protected CloseableHttpClient proxyClient;

	protected boolean useStatsClient = false;

	/**
	 * @return ElasticsearchClientService to get client from.
	 */
	protected ElasticsearchClientService getElasticsearchClientService() {
		if (useStatsClient)
			return statsClientService;
		else
			return searchClientService;
	};

	protected URI getEsNodeURI() throws ServletException {
		Client client = getElasticsearchClientService().getClient();

		if (client == null)
			throw new ServletException("Elasticsearch client for " + (useStatsClient ? "stats" : "search")
					+ " cluster is not available");

		NodesInfoResponse nir = client.admin().cluster().nodesInfo(new NodesInfoRequest().http(true)).actionGet();

		NodeInfo[] nis = nir.getNodes();
		if (nis == null || nis.length == 0) {
			throw new ServletException("Elasticsearch node is not available");
		}

		NodeInfo ni = nir.getAt(0);

		HttpInfo hi = ni.getHttp();
		if (hi == null) {
			throw new ServletException("HTTP Connector is not available for Elasticsearch node " + ni.getNode().getName());
		}

		TransportAddress ta = hi.getAddress().publishAddress();
		if (ta == null || !(ta instanceof InetSocketTransportAddress)) {
			throw new ServletException("HTTP Connector is not available for Elasticsearch node " + ni.getNode().getName());
		}

		InetSocketTransportAddress a = (InetSocketTransportAddress) ta;

		String url = "http://" + a.address().getHostString() + ":" + a.address().getPort();

		try {
			return new URI(url);
		} catch (URISyntaxException e) {
			throw new ServletException("Generated ES URL is invalid: " + url);
		}

	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		useStatsClient = !SearchUtils.isBlank(filterConfig.getInitParameter(CFG_USE_STATS_CLIENT));
		proxyClient = HttpClients.custom().build();
	}

	@Override
	public void destroy() {
		if (proxyClient != null) {
			try {
				proxyClient.close();
			} catch (IOException e) {
				// ignore
			}
			proxyClient = null;
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {

		final HttpServletRequest httpRequest = (HttpServletRequest) request;
		final HttpServletResponse httpResponse = (HttpServletResponse) response;

		if (httpRequest.isUserInRole(Role.ADMIN)) {
			doProxyCall(httpRequest, httpResponse);
		} else {
			if (httpRequest.getUserPrincipal() == null) {
				httpResponse.addHeader("WWW-Authenticate", "Basic realm=\"" + HttpBasicChallengeInterceptor.CHALLENGE_TEXT
						+ "\"");
				httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			} else {
				httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
			}
		}

	}

	@SuppressWarnings("deprecation")
	protected void doProxyCall(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {

		URI targetUri = getEsNodeURI();

		String method = servletRequest.getMethod();

		String proxyRequestUri = rewriteRequestUrl(servletRequest, targetUri);
		HttpRequest proxyRequest;
		if (servletRequest.getHeader(HttpHeaders.CONTENT_LENGTH) != null
				|| servletRequest.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
			proxyRequest = copyRequestData(servletRequest, new BasicHttpEntityEnclosingRequest(method, proxyRequestUri));
		} else
			proxyRequest = new BasicHttpRequest(method, proxyRequestUri);

		copyRequestHeaders(servletRequest, proxyRequest);

		HttpResponse proxyResponse = null;
		try {
			log.fine("proxy " + method + " uri: " + servletRequest.getRequestURI() + " -- "
					+ proxyRequest.getRequestLine().getUri());

			proxyResponse = proxyClient.execute(URIUtils.extractHost(targetUri), proxyRequest);

			int sc = proxyResponse.getStatusLine().getStatusCode();

			if (handleNotModifiedResponse(servletResponse, sc)) {
				return;
			}

			servletResponse.setStatus(sc, proxyResponse.getStatusLine().getReasonPhrase());

			copyResponseHeaders(proxyResponse, servletResponse);
			copyResponseData(proxyResponse, servletResponse);

		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			if (e instanceof ServletException)
				throw (ServletException) e;
			if (e instanceof IOException)
				throw (IOException) e;
			throw new RuntimeException(e);
		} finally {
			// make sure the entire entity was consumed, so the connection is released
			consumeQuietly(proxyResponse.getEntity());
			closeQuietly(servletResponse.getOutputStream());
		}
	}

	/**
	 * Gets the request URL from {@code servletRequest} and rewrites it to be used for proxied server.
	 */
	protected String rewriteRequestUrl(HttpServletRequest servletRequest, URI targetUri) {
		StringBuilder uri = new StringBuilder(500);
		uri.append(targetUri);
		// Handle the path
		String path = servletRequest.getRequestURI();
		if (!SearchUtils.isBlank(path)) {
			// remove first 5 path elements as they are from Searchisko REST API URL. Only rest is for ES
			String[] pe = path.split("/");
			int matchcount = 5;
			for (int i = 0; i < pe.length; i++) {
				if ("sys".equals(pe[i])) {
					matchcount = i + 2;
				}
				if (i > matchcount) {
					uri.append("/");
					uri.append(pe[i]);
				}
			}
		}
		// Handle the query string
		String queryString = servletRequest.getQueryString();
		if (!SearchUtils.isBlank(queryString)) {
			uri.append('?');
			uri.append(queryString);
		}
		return uri.toString();
	}

	/**
	 * Copy request data from the caller's request to proxy request
	 */
	protected HttpEntityEnclosingRequest copyRequestData(HttpServletRequest servletRequest,
			HttpEntityEnclosingRequest proxyRequest) throws IOException {
		proxyRequest.setEntity(new InputStreamEntity(servletRequest.getInputStream(), servletRequest.getContentLength()));
		return proxyRequest;
	}

	/**
	 * List of headers that are not copied between caller and proxy request and responses.
	 */
	protected static final Set<String> omittedHeaders;
	static {
		omittedHeaders = new HashSet<>();
		for (String header : new String[] { "Connection", "Keep-Alive", "Authorization", "Proxy-Authenticate",
				"Proxy-Authorization", "TE", "Trailers", "Transfer-Encoding", "Upgrade", "Origin",
				"Access-Control-Allow-Origin" }) {
			omittedHeaders.add(header.toLowerCase());
		}
	}

	/**
	 * Copy request headers from the caller's request to the proxy request with necessary filtering.
	 */
	protected void copyRequestHeaders(HttpServletRequest servletRequest, HttpRequest proxyRequest) {
		Enumeration<String> headerNames = servletRequest.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = (String) headerNames.nextElement();
			if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
				continue;
			} else if (omittedHeaders.contains(headerName.toLowerCase())) {
				continue;
			}

			Enumeration<String> headersValues = servletRequest.getHeaders(headerName);
			while (headersValues.hasMoreElements()) {
				String headerValue = (String) headersValues.nextElement();
				proxyRequest.addHeader(headerName, headerValue);
			}
		}
	}

	protected boolean handleNotModifiedResponse(HttpServletResponse servletResponse, int statusCode)
			throws ServletException, IOException {
		if (statusCode == HttpServletResponse.SC_NOT_MODIFIED) {
			servletResponse.setIntHeader(HttpHeaders.CONTENT_LENGTH, 0);
			servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return true;
		}
		return false;
	}

	/**
	 * Copy proxied response headers back to the caller's response.
	 */
	protected void copyResponseHeaders(HttpResponse proxyResponse, HttpServletResponse servletResponse) {
		for (Header header : proxyResponse.getAllHeaders()) {
			if (omittedHeaders.contains(header.getName().toLowerCase()))
				continue;
			servletResponse.addHeader(header.getName(), header.getValue());
		}
	}

	/**
	 * Copy data (the entity) from the proxy response to the caller's response.
	 */
	protected void copyResponseData(HttpResponse proxyResponse, HttpServletResponse servletResponse) throws IOException {
		HttpEntity entity = proxyResponse.getEntity();
		if (entity != null) {
			OutputStream servletOutputStream = servletResponse.getOutputStream();
			entity.writeTo(servletOutputStream);
		}
	}

	protected void closeQuietly(Closeable closeable) {
		try {
			closeable.close();
		} catch (IOException e) {
			log.warning(e.getMessage());
		}
	}

	/**
	 * HttpClient v4.1 doesn't have the
	 * {@link org.apache.http.util.EntityUtils#consumeQuietly(org.apache.http.HttpEntity)} method.
	 */
	protected void consumeQuietly(HttpEntity entity) {
		try {
			EntityUtils.consume(entity);
		} catch (IOException e) {
			log.warning(e.getMessage());
		}
	}

}
