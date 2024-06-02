package factoid.web;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import factoid.converter.BiopaxToFactoid;
import factoid.converter.FactoidToBiopax;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.io.sbgn.L3ToSBGNPDConverter;
import org.biopax.paxtools.model.Model;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;


@RestController
@RequestMapping(value = "/v2", method = {RequestMethod.POST})
public class Controller {

  @Operation(summary = "json-to-biopax", description = "Converts a Factoid model to BioPAX.")
  @RequestMapping(path = "/json-to-biopax",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = "application/vnd.biopax.rdf+xml"
  )
  public String jsonToBiopax(
    @Parameter(description = "Factoid document content (JSON string)") @RequestBody String body) {
    // Add templates to converter by the reader
    FactoidToBiopax converter = new FactoidToBiopax();
    try {
      converter.addToModel(body);
    } catch (IllegalStateException | JsonSyntaxException | JsonIOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    } catch (Throwable e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "json-to-biopax failed", e);
    }

    // Convert the model to biopax string
    return converter.convertToBiopax();
  }

  @Operation(summary = "json-to-sbgn", description = "Converts a Factoid model to SBGN-ML (via BioPAX).")
  @RequestMapping(path = "/json-to-sbgn",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = "application/xml"
  )
  public String jsonToSbgn(
    @Parameter(description = "Factoid document (JSON string)") @RequestBody String body) {
    try {
      InputStream is = new ByteArrayInputStream(jsonToBiopax(body).getBytes(StandardCharsets.UTF_8));
      Model model = new SimpleIOHandler().convertFromOWL(is);
      is.close();
      L3ToSBGNPDConverter converter = new L3ToSBGNPDConverter();
      converter.setDoLayout(false); //TODO: apply the default sbgn layout?
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      converter.writeSBGN(model, baos);
      return baos.toString(StandardCharsets.UTF_8.name());
    } catch (Throwable e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "json-to-sbgn failed", e);
    }
  }

  @Operation(summary = "biopax-to-sbgn", description = "Converts a factoid BioPAX model to SBGN-ML (SBGN PD).")
  @RequestMapping(path = "/biopax-to-sbgn",
    consumes = "application/vnd.biopax.rdf+xml",
    produces = "application/xml"
  )
  public String biopaxToSbgn(
    @Parameter(description = "A factoid (small) BioPAX RDF/XML model") @RequestBody String body) {
    try {
      InputStream is = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
      Model model = new SimpleIOHandler().convertFromOWL(is);
      is.close();
      L3ToSBGNPDConverter converter = new L3ToSBGNPDConverter();
      converter.setDoLayout(false);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      converter.writeSBGN(model, baos);
      return baos.toString(StandardCharsets.UTF_8.name());
    } catch (Throwable e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "biopax-to-sbgn failed", e);
    }
  }

  @Operation(summary = "biopax-to-json", description = "Converts a BioPAX model to Factoid JSON.")
  @RequestMapping(path = "/biopax-to-json",
    consumes = "application/vnd.biopax.rdf+xml",
    produces = "application/json"
  )
  public String biopaxToFactoid(
		  @Parameter(description = "A BioPAX RDF/XML model") @RequestBody String body) {
	  BiopaxToFactoid converter = new BiopaxToFactoid();
	  try {
		  InputStream is = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
      Model model = new SimpleIOHandler().convertFromOWL(is);
		  return converter.convert(model).toString();
	  } catch (IllegalStateException | JsonSyntaxException | JsonIOException e) {
		  throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
	  } catch (Throwable e) {
		  throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "biopax-to-json failed", e);
	  }
  }
  
  @Operation(summary = "biopax-url-to-json", description = "Converts a BioPAX model to Factoid JSON.")
  @RequestMapping(path = "/biopax-url-to-json",
    consumes = "text/plain",
    produces = "application/json"
  )
  public String biopaxUrlToFactoid(
		  @Parameter(description = "URL of a BioPAX RDF/XML file") @RequestBody String url) {
	  BiopaxToFactoid converter = new BiopaxToFactoid();
	  try {
		  String body = getContentFromUrl(url);
		  InputStream is = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
      Model model = new SimpleIOHandler().convertFromOWL(is);
		  return converter.convert(model).toString();
	  } catch (IllegalStateException | JsonSyntaxException | JsonIOException e) {
		  throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
	  } catch (Throwable e) {
		  throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "biopax-url-to-json failed", e);
	  }
  }
  
  private String getContentFromUrl(String url) {
		InputStream is = null;
		try {
      try {
			  is = new GZIPInputStream(new URL(url).openStream());
      } catch (IOException e) {
        //e.printStackTrace();
        is = new URL(url).openStream();
      }
		} catch (IOException e) {
			e.printStackTrace();
		}
    Reader reader = new InputStreamReader(is);
		Writer writer = new StringWriter();
		char[] buffer = new char[10240];
	    try {
			for (int length; (length = reader.read(buffer)) > 0;) {
			    writer.write(buffer, 0, length);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	  String body = writer.toString();
	  return body;
	}
}
