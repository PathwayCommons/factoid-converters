package factoid.converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BiopaxToFactoidTest {
	
	@Test
	public void testToJson() throws IOException {
		InputStream f = new FileInputStream(new File(getClass().getResource("/pc_sm.owl").getFile()));
		BioPAXIOHandler handler = new SimpleIOHandler();
		Model model = handler.convertFromOWL(f);
		BiopaxToFactoid b2f = new BiopaxToFactoid();
		JsonObject js = b2f.convert(model);
		Set<Map.Entry<String, JsonElement>> entries = js.entrySet();
		
		for ( Map.Entry<String, JsonElement> entry : entries ) {
			Set<String> entityIds1 = new HashSet<String>();
			Set<String> entityIds2 = new HashSet<String>();
			JsonArray arr = entry.getValue().getAsJsonArray();
			for ( JsonElement el : arr ) {
				JsonObject obj = el.getAsJsonObject();
				if ( obj.has("entries") ) {
					for ( JsonElement entityEl : obj.get("entries").getAsJsonArray() ) {
						entityIds1.add(entityEl.getAsJsonObject().get("id").getAsString());
					}
				}
				else {
					entityIds2.add(el.getAsJsonObject().get("id").getAsString());
				}
			}
			
			assertEquals(entityIds1, entityIds2);
		}
		
		
	}
}
