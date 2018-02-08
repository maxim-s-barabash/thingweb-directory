package de.thingweb.directory.servlet;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.github.jsonldjava.utils.JsonUtils;

import de.thingweb.directory.BaseTest;
import de.thingweb.directory.servlet.utils.MockHttpServletRequest;
import de.thingweb.directory.servlet.utils.MockHttpServletResponse;

public class TDLookUpSemServletTest extends BaseTest {

	@Test
	@Ignore
	public void testDoGetWithQuery() throws Exception {
		TDServlet servlet = new TDServlet();
		TDLookUpSemServlet lookUpServlet = new TDLookUpSemServlet(servlet);
		
		byte[] b = loadResource("samples/fanTD.jsonld");
		MockHttpServletRequest req = new MockHttpServletRequest("/", b, "application/ld+json");
		MockHttpServletResponse resp = new MockHttpServletResponse();
		
		String id = servlet.doAdd(req, resp);
		
		b = loadResource("samples/temperatureSensorTD.jsonld");
		req = new MockHttpServletRequest("/", b, "application/ld+json");
		resp = new MockHttpServletResponse();
		
		servlet.doAdd(req, resp);

		String q = "?thing a <http://uri.etsi.org/m2m/saref#Sensor> .\n"
				+ "FILTER NOT EXISTS {"
				+ "  ?thing <http://iot.linkeddata.es/def/wot#providesInteractionPattern> ?i .\n"
				+ "  ?i a <http://uri.etsi.org/m2m/saref#ToggleCommand> .\n"
				+ "}";
		HashMap<String, String> params = new HashMap<>();
		params.put("query", q);
		req = new MockHttpServletRequest("/", new byte [0], "text/plain", new HashMap<>(), params);
		resp = new MockHttpServletResponse();
		
		lookUpServlet.doGet(req, resp);
		
		Object o = JsonUtils.fromString(new String(resp.getBytes()));
		assertTrue("Lookup result is not formatted as expected", o instanceof Map);
		assertEquals("SPARQL filter was not applied", 1, ((Map) o).keySet().size());
		assertEquals("SPARQL filter was not applied correctly", id, ((Map) o).keySet().iterator().next());
	}

}