package factoid.converter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import factoid.model.XrefModel;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

//TODO: test each distinct json template (cases 1 - 4E from the interaction types doc.)
public class FactoidToBiopaxTest {

  @Test
  public void testToOwl() throws IOException {
    Gson gson = new Gson();
    JsonReader reader = new JsonReader(new FileReader(getClass()
      .getResource("/test2.json").getFile()));
    JsonObject template = gson.fromJson(reader, JsonObject.class);
    FactoidToBiopax converter = new FactoidToBiopax();
    converter.addToModel(template);
    String res = converter.convertToBiopax();

    assertThat(res,notNullValue());
    assertThat(res, endsWith("</rdf:RDF>"));

    Model m = converterResultToModel(res);
    assertThat(m, notNullValue());
    assertThat(m.getObjects().size(), greaterThan(20));

    Set<SmallMoleculeReference> smols = m.getObjects(SmallMoleculeReference.class);
    assertThat(smols.size(), is(4));

    Set<Modulation> mods = m.getObjects(Modulation.class);
    assertThat(mods.size(), is(2));

    List<?> controllers = mods.stream().map(Modulation::getController)
      .flatMap(Set::stream).collect(Collectors.toList());
    assertThat(controllers.stream().filter(SmallMolecule.class::isInstance).count(), is(2L));
    
    Set<Complex> complexes = m.getObjects(Complex.class);
    assertThat(complexes.size(), is(2));
    
    Set<PublicationXref> pubs = m.getObjects(PublicationXref.class);
    assertThat(pubs.size(), is(1));
  }
  
  public FactoidToBiopax getBiopaxConvertor(String intnsContent, String publicationContent) {
	  return getBiopaxConvertor(intnsContent, publicationContent, null);
  }
  
  public FactoidToBiopax getBiopaxConvertor(String intnsContent, String publicationContent, String pathwayName) {
	  return getBiopaxConvertor(intnsContent, publicationContent, pathwayName, null);
  }
  
  public FactoidToBiopax getBiopaxConvertor(String intnsContent, String publicationContent, String pathwayName, String pathwayId) {
	  JsonParser jsonParser = new JsonParser();
	  JsonObject template = new JsonObject();
	  JsonArray intnTemplates = jsonParser.parse(intnsContent).getAsJsonArray();
	  
	  template.add("interactions", intnTemplates);
	  
	  if ( publicationContent != null ) {
		  JsonObject pubTemplate = jsonParser.parse(publicationContent).getAsJsonObject();
		  template.add("publication", pubTemplate);
	  }
	  
	  if ( pathwayName != null ) {
		  template.addProperty("pathwayName", pathwayName);
	  }
	  
	  if ( pathwayId != null ) {
		  template.addProperty("pathwayId", pathwayId);
	  }
	  
	  
	  FactoidToBiopax converter = new FactoidToBiopax();
	  converter.addToModel(template); //processing
	  
	  return converter;
  }
  
  @Test
  public void testPublication() throws IOException {
	  String intnTemplates = "[]";
	  String publicationTemplate = "{\"id\": \"2549346\", \"db\": \"PubMed\"}";
	  
	  FactoidToBiopax converter = getBiopaxConvertor(intnTemplates, publicationTemplate);
	  Model m = converterResultToModel(converter.convertToBiopax());
	  assertThat(m.getObjects(PublicationXref.class).size(), equalTo(1));
  }
  
  @Test
  public void testPathwayName() throws IOException {
	  String intnTemplates = "[]";
	  String publicationTemplate = null;
	  String pathwayName = "MeCP2 facilitates breast cancer growth via promoting ubiquitination-mediated...";
	  
	  FactoidToBiopax converter = getBiopaxConvertor(intnTemplates, publicationTemplate, pathwayName);
	  Model m = converterResultToModel(converter.convertToBiopax());
	  assertEquals(m.getObjects(Pathway.class).iterator().next().getDisplayName(), pathwayName);
  }
  
  @Test
  public void testPathwayId() throws IOException {
	  String intnTemplates = "[]";
	  String publicationTemplate = null;
	  String pathwayId = "01b5959d-8022-4585-8d8c-6f71f8183e0c";
	  
	  FactoidToBiopax converter = getBiopaxConvertor(intnTemplates, publicationTemplate, null, pathwayId);
	  Model m = converterResultToModel(converter.convertToBiopax());
	  Set<Xref> xrefs = m.getObjects(Pathway.class).iterator().next().getXref();
	  Iterator<Xref> xrefIt = xrefs.stream().filter(xref -> xref instanceof UnificationXref).iterator();
	  assertEquals(xrefIt.hasNext(), true);
	  assertEquals(xrefIt.next().getId(), pathwayId);
  }
  
  @Test
  public void testMolecularInteraction() throws IOException {
	  String templates = "[{\n" +
		  "    \"type\": \"Molecular Interaction\",\n" +
		  "    \"participants\": [\n" +
		  "      {\n" +
		  "        \"type\": \"protein\",\n" +
		  "        \"name\": \"IGF1\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": P05019,\n" +
		  "          \"db\": \"uniprot\"\n" +
		  "        }\n" +
		  "      },\n" +
		  "      {\n" +
		  "        \"type\": \"protein\",\n" +
		  "        \"name\": \"FSHB\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": \"P01225\",\n" +
		  "          \"db\": \"uniprot\"\n" +
		  "        }\n" +
		  "      }\n" +
		  "    ]\n" +
		  "  }]";

	  FactoidToBiopax converter = getBiopaxConvertor(templates, null);

	  Model m = converterResultToModel(converter.convertToBiopax());
	  assertThat(m.getObjects().size(), equalTo(8));

	  Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
	  assertThat(xrefs, notNullValue());
	  assertThat(xrefs.size(), equalTo(2));
	  xrefs.stream().forEach(x -> {
		  assertThat(x.getDb(), equalTo("uniprot"));
		  assertThat(x.getId(), isOneOf("P05019","P01225"));
	  });
	  
	  Set<MolecularInteraction> intns = m.getObjects(MolecularInteraction.class);
	  assertThat(intns, not(empty()));
	  MolecularInteraction intn = intns.iterator().next();
	  Set<Entity> participants = intn.getParticipant();

	  assertThat(participants.size(), equalTo(2));
	  participants.stream().forEach(p -> {
		  assertThat(p, instanceOf(Protein.class));
		  assertThat(((Protein)p).getEntityReference(), notNullValue());
		  assertThat(((Protein)p).getEntityReference().getXref(), not(empty()));
	  });
  }
  
  @Test
  public void testExpressionRegulation() throws IOException {
	  String templates = "[{\n" +
			  "    \"type\": \"Expression Regulation\",\n" +
			  "    \"controlType\": \"inhibition\",\n" +
			  "    \"controller\": " +
			  "      {\n" +
			  "        \"type\": \"protein\",\n" +
			  "        \"name\": \"JUN\",\n" +
			  "        \"xref\": {\n" +
			  "          \"id\": P05412,\n" +
			  "          \"db\": \"uniprot\"\n" +
			  "        }\n" +
			  "      },\n" +
			  "    \"target\": " +
			  "      {\n" +
			  "        \"type\": \"protein\",\n" +
			  "        \"name\": \"BMP2\",\n" +
			  "        \"xref\": {\n" +
			  "          \"id\": \"P12643\",\n" +
			  "          \"db\": \"uniprot\"\n" +
			  "        }\n" +
			  "      }\n" +
			  "  }]";

	  	  FactoidToBiopax converter = getBiopaxConvertor(templates, null);

		  Model m = converterResultToModel(converter.convertToBiopax());
		  assertThat(m.getObjects().size(), equalTo(9));

		  Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
		  assertThat(xrefs, notNullValue());
		  assertThat(xrefs.size(), equalTo(2));
		  xrefs.stream().forEach(x -> {
			  assertThat(x.getDb(), equalTo("uniprot"));
			  assertThat(x.getId(), isOneOf("P05412","P12643"));
		  });  
		  
		  Set<TemplateReactionRegulation> controls = m.getObjects(TemplateReactionRegulation.class);
		  assertThat(controls, not(empty()));
		  TemplateReactionRegulation control = controls.iterator().next();
		  
		  assertThat(control.getControlType(), equalTo(ControlType.INHIBITION));
		  assertThat(control.getController(), not(empty()));
		  assertThat(control.getControlled(), not(empty()));
		  
		  TemplateReaction reaction = (TemplateReaction) control.getControlled().iterator().next();
		  PhysicalEntity controller = (PhysicalEntity) control.getController().iterator().next();
		  
		  assertThat(reaction.getParticipant(), not(empty()));
		  
		  PhysicalEntity product = (PhysicalEntity) reaction.getParticipant().iterator().next();
		  
		  assertThat(product, instanceOf(Protein.class));
		  assertThat(controller, instanceOf(Protein.class));
		  
		  assertThat(((Protein) product).getEntityReference(), notNullValue());
		  assertThat(((Protein) controller).getEntityReference(), notNullValue());
		  
		  assertThat(((Protein) product).getEntityReference().getXref(), not(empty()));
		  assertThat(((Protein) controller).getEntityReference().getXref(), not(empty()));
  }
  
  @Test
  public void testProteinControlsState() throws IOException {
	  String templates = "[{\n" +
		  "    \"type\": \"Protein Controls State\",\n" +
		  "    \"controlType\": \"activation\",\n" +
		  "    \"modification\": \"phosphorylated\",\n" +
		  "    \"controller\": " +
		  "      {\n" +
		  "        \"type\": \"protein\",\n" +
		  "        \"name\": \"TP53\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": P04637,\n" +
		  "          \"db\": \"uniprot\"\n" +
		  "        }\n" +
		  "      },\n" +
		  "    \"target\": " +
		  "      {\n" +
		  "        \"type\": \"protein\",\n" +
		  "        \"name\": \"MDM2\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": \"Q00987\",\n" +
		  "          \"db\": \"uniprot\"\n" +
		  "        }\n" +
		  "      }\n" +
		  "  }]";

	  FactoidToBiopax converter = getBiopaxConvertor(templates, null);

	  Model m = converterResultToModel(converter.convertToBiopax());
	  assertThat(m.getObjects().size(), equalTo(14));

	  Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
	  assertThat(xrefs, notNullValue());
	  assertThat(xrefs.size(), equalTo(2));
	  xrefs.stream().forEach(x -> {
		  assertThat(x.getDb(), equalTo("uniprot"));
		  assertThat(x.getId(), isOneOf("P04637","Q00987"));
	  });  
	  
	  Set<Catalysis> controls = m.getObjects(Catalysis.class);
	  assertThat(controls, not(empty()));
	  Catalysis control = controls.iterator().next();
	  
	  assertThat(control.getControlled(), not(empty()));
	  assertThat(control.getController(), not(empty()));
	  assertThat(control.getControlType(), equalTo(ControlType.ACTIVATION));
	  
	  Conversion conversion = (Conversion) control.getControlled().iterator().next();
	  assertThat(conversion.getLeft(), not(empty()));
	  assertThat(conversion.getRight(), not(empty()));
	  
	  PhysicalEntity controller = (PhysicalEntity) control.getController().iterator().next();
	  PhysicalEntity left = (PhysicalEntity) conversion.getLeft().iterator().next();
	  PhysicalEntity right = (PhysicalEntity) conversion.getRight().iterator().next();
	  
	  assertThat(controller, instanceOf(Protein.class));
	  assertThat(left, instanceOf(Protein.class));
	  assertThat(right, instanceOf(Protein.class));
	  
	  assertThat(((Protein) left).getEntityReference(), notNullValue());
	  assertThat(((Protein) controller).getEntityReference(), notNullValue());
	  assertThat(((Protein) right).getEntityReference(), equalTo(((Protein) left).getEntityReference()));
	  
	  assertThat(((Protein) left).getEntityReference().getXref(), not(empty()));
	  assertThat(((Protein) controller).getEntityReference().getXref(), not(empty()));
  }
  
  @Test
  public void testConversion() throws IOException {
	  String templates = "[{\n" +
		  "    \"type\": \"Other Interaction\",\n" +
		  "    \"controlType\": \"activation\",\n" +
		  "    \"participants\": [\n" +
		  "      {\n" +
		  "        \"type\": \"chemical\",\n" +
		  "        \"name\": \"Iodine\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": 807,\n" +
		  "          \"db\": \"pubchem\"\n" +
		  "        }\n" +
		  "      },\n" +
		  "      {\n" +
		  "        \"type\": \"chemical\",\n" +
		  "        \"name\": \"Nikel\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": 101944429,\n" +
		  "          \"db\": \"pubchem\"\n" +
		  "        }\n" +
		  "      },\n" +
		  "    ]\n" +
		  "  }]";
	  
	  FactoidToBiopax converter = getBiopaxConvertor(templates, null);

	  Model m = converterResultToModel(converter.convertToBiopax());
	  assertThat(m.getObjects().size(), equalTo(8));

	  Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
	  assertThat(xrefs, notNullValue());
	  assertThat(xrefs.size(), equalTo(2));
	  xrefs.stream().forEach(x -> {
		  assertThat(x.getDb(), equalTo("pubchem"));
		  assertThat(x.getId(), isOneOf("807","101944429"));
	  });
	  
	  Set<Conversion> conversions = m.getObjects(Conversion.class);
	  assertThat(conversions, not(empty()));
	  Conversion conversion = conversions.iterator().next();
	  
	  assertThat(conversion.getLeft(), not(empty()));
	  assertThat(conversion.getRight(), not(empty()));
	  
	  PhysicalEntity left = (PhysicalEntity) conversion.getLeft().iterator().next();
	  PhysicalEntity right = (PhysicalEntity) conversion.getRight().iterator().next();
	  
	  assertThat(left, instanceOf(SmallMolecule.class));
	  assertThat(right, instanceOf(SmallMolecule.class));
	  
	  assertThat(left.getDisplayName(), equalTo("Iodine"));
	  assertThat(right.getDisplayName(), equalTo("Nikel"));
	  
	  assertThat(((SmallMolecule) left).getEntityReference(), notNullValue());
	  assertThat(((SmallMolecule) right).getEntityReference(), notNullValue());
	  
	  assertThat(((SmallMolecule) left).getEntityReference().getXref(), not(empty()));
	  assertThat(((SmallMolecule) right).getEntityReference().getXref(), not(empty()));
  }
  
  @Test
  public void testControlsConsumtionOrProduction() throws IOException {
	  String templates = "[{\n" +
		  "    \"type\": \"Other Interaction\",\n" +
		  "    \"controlType\": \"inhibition\",\n" +
		  "    \"participants\": [\n" +
		  "      {\n" +
		  "        \"type\": \"protein\",\n" +
		  "        \"name\": \"BMP2\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": P12643,\n" +
		  "          \"db\": \"uniprot\"\n" +
		  "        }\n" +
		  "      },\n" +
		  "      {\n" +
		  "        \"type\": \"chemical\",\n" +
		  "        \"name\": \"Progesterone\",\n" +
		  "        \"xref\": {\n" +
		  "          \"id\": 5994,\n" +
		  "          \"db\": \"pubchem\"\n" +
		  "        }\n" +
		  "      },\n" +
		  "    ]\n" +
		  "  }]";

	  FactoidToBiopax converter = getBiopaxConvertor(templates, null);

	  Model m = converterResultToModel(converter.convertToBiopax());
	  assertThat(m.getObjects().size(), equalTo(9));

	  Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
	  assertThat(xrefs, notNullValue());
	  assertThat(xrefs.size(), equalTo(2));
	  xrefs.stream().forEach(x -> {
		  assertThat(x.getDb(), isOneOf("uniprot", "pubchem"));
		  assertThat(x.getId(), isOneOf("5994","P12643"));
	  });
	  
	  Set<Catalysis> controls = m.getObjects(Catalysis.class);
	  assertThat(controls, not(empty()));
	  Catalysis control = controls.iterator().next();
	  assertThat(control.getControlType(), nullValue());
	  
	  assertThat(control.getController(), not(empty()));
	  assertThat(control.getControlled(), not(empty()));
	  
	  PhysicalEntity controller = (PhysicalEntity) control.getController().iterator().next();
	  BiochemicalReaction controlled = (BiochemicalReaction) control.getControlled().iterator().next();
	  
	  assertThat(controlled.getLeft(), not(empty()));
	  assertThat(controlled.getRight(), empty());
	  
	  PhysicalEntity target = (PhysicalEntity) controlled.getLeft().iterator().next();
	  
	  assertThat(controller, instanceOf(Protein.class));
	  assertThat(target, instanceOf(SmallMolecule.class));
	  
	  assertThat(((Protein)controller).getEntityReference(), notNullValue());
	  assertThat(((SmallMolecule)target).getEntityReference(), notNullValue());

	  assertThat(((Protein)controller).getEntityReference().getXref(), not(empty()));
	  assertThat(((SmallMolecule)target).getEntityReference().getXref(), not(empty()));
  }
  
  @Test
  public void testControlSequence() throws IOException {
	  String templates = "[{\n" +
	      "    \"type\": \"Other Interaction\",\n" +
	      "    \"controlType\": \"inhibition\",\n" +
	      "    \"participants\": [\n" +
	      "      {\n" +
	      "        \"type\": \"protein\",\n" +
	      "        \"name\": \"BMP2\",\n" +
	      "        \"xref\": {\n" +
	      "          \"id\": P12643,\n" +
	      "          \"db\": \"uniprot\"\n" +
	      "        }\n" +
	      "      },\n" +
	      "      {\n" +
	      "        \"type\": \"protein\",\n" +
	      "        \"name\": \"IGF1\",\n" +
	      "        \"xref\": {\n" +
	      "          \"id\": \"P05019\",\n" +
	      "          \"db\": \"uniprot\"\n" +
	      "        }\n" +
	      "      }\n" +
	      "    ]\n" +
	      "  }]";
	
	FactoidToBiopax converter = getBiopaxConvertor(templates, null);

    Model m = converterResultToModel(converter.convertToBiopax());
    assertThat(m.getObjects().size(), equalTo(9));

    Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
    assertThat(xrefs, notNullValue());
    assertThat(xrefs.size(), equalTo(2));
    xrefs.stream().forEach(x -> {
      assertThat(x.getDb(), equalTo("uniprot"));
      assertThat(x.getId(), isOneOf("P05019","P12643"));
    });  
    
    Set<Control> controls = m.getObjects(Control.class);
    controls.stream().forEach(c -> {
    		assertThat(c.getControlType(), equalTo(ControlType.INHIBITION));
    		assertThat(c.getController().size(), equalTo(1));
    		assertThat(c.getController().iterator().next(), instanceOf(Protein.class));
    		
    		assertThat(c.getControlled().size(), isOneOf(0, 1));
    		if (c.getControlled().iterator().hasNext()) {
    			assertThat(c.getControlled().iterator().next(), instanceOf(Control.class));
    		}
    });
    
    Set<Protein> entities = m.getObjects(Protein.class);
    entities.stream().forEach(e -> {
		assertThat(e.getEntityReference(), notNullValue());
	    assertThat(e.getEntityReference().getXref(), not(empty()));
    });
  }
  
  // TODO: update model
  @Test
  public void testGeneralInteraction() throws IOException {
	  String templates = "[{\n" +
	      "    \"type\": \"Other Interaction\",\n" +
	      "    \"participants\": [\n" +
	      "      {\n" +
	      "        \"type\": \"protein\",\n" +
	      "        \"name\": \"Saccharopepsin\",\n" +
	      "        \"xref\": {\n" +
	      "          \"id\": P07267,\n" +
	      "          \"db\": \"uniprot\"\n" +
	      "        }\n" +
	      "      },\n" +
	      "      {\n" +
	      "        \"type\": \"protein\",\n" +
	      "        \"name\": \"Kalirin\",\n" +
	      "        \"xref\": {\n" +
	      "          \"id\": \"O60229\",\n" +
	      "          \"db\": \"uniprot\"\n" +
	      "        }\n" +
	      "      }\n" +
	      "    ]\n" +
	      "  }]";
	
	FactoidToBiopax converter = getBiopaxConvertor(templates, null);

    Model m = converterResultToModel(converter.convertToBiopax());
    assertThat(m.getObjects().size(), equalTo(8));

    Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
    assertThat(xrefs, notNullValue());
    assertThat(xrefs.size(), equalTo(2));
    xrefs.stream().forEach(x -> {
      assertThat(x.getDb(), equalTo("uniprot"));
      assertThat(x.getId(), isOneOf("P07267","O60229"));
    });  
    
    Set<Interaction> intns = m.getObjects(Interaction.class);
    assertThat(intns, not(empty()));
    Interaction intn = intns.iterator().next();
    Set<Entity> participants = intn.getParticipant();
    
    assertThat(participants.size(), equalTo(2));
    participants.stream().forEach(p -> {
    		assertThat(p, instanceOf(Protein.class));
    		assertThat(((Protein)p).getEntityReference(), notNullValue());
    	    assertThat(((Protein)p).getEntityReference().getXref(), not(empty()));
    });
  }
  
  @Test
  //Progesterone chemical-affects LEP (case 4D) is about activation of LEP protein (catalyst) by Progesterone.
  public void testXrefTypes() throws IOException {
	// in this example I modified the example that is used for "testChemicalAffects()"
	// the type of the second participant was "protein" but I updated it as "ggp" for this test case
	// without updating any other field
    String templates = "[{\n" +
      "    \"type\": \"Other Interaction\",\n" +
      "    \"controlType\": \"activation\",\n" +
      "    \"participants\": [\n" +
      "      {\n" +
      "        \"type\": \"chemical\",\n" +
      "        \"name\": \"Progesterone\",\n" +
      "        \"xref\": {\n" +
      "          \"id\": 5994,\n" +
      "          \"db\": \"pubchem\"\n" +
      "        }\n" +
      "      },\n" +
      "      {\n" +
      "        \"type\": \"ggp\",\n" +
      "        \"name\": \"LEP\",\n" +
      "        \"xref\": {\n" +
      "          \"id\": \"P41159\",\n" +
      "          \"db\": \"uniprot\"\n" +
      "        }\n" +
      "      }\n" +
      "    ]\n" +
      "  }]";

    FactoidToBiopax converter = getBiopaxConvertor(templates, null);

    Model m = converterResultToModel(converter.convertToBiopax());
    assertThat(m.getObjects().size(), equalTo(9));

    Set<RelationshipXref> relXrefs = m.getObjects(RelationshipXref.class);
    Set<UnificationXref> uniXrefs = m.getObjects(UnificationXref.class);
    assertThat(relXrefs, notNullValue());
    assertThat(relXrefs.size(), equalTo(1));
    assertThat(uniXrefs, notNullValue());
    assertThat(uniXrefs.size(), equalTo(1));
  }

  @Test
  //Progesterone chemical-affects LEP (case 4D) is about activation of LEP protein (catalyst) by Progesterone.
  public void testChemicalAffects() throws IOException {
    String templates = "[{\n" +
      "    \"type\": \"Other Interaction\",\n" +
      "    \"controlType\": \"activation\",\n" +
      "    \"participants\": [\n" +
      "      {\n" +
      "        \"type\": \"chemical\",\n" +
      "        \"name\": \"Progesterone\",\n" +
      "        \"xref\": {\n" +
      "          \"id\": 5994,\n" +
      "          \"db\": \"pubchem\"\n" +
      "        }\n" +
      "      },\n" +
      "      {\n" +
      "        \"type\": \"protein\",\n" +
      "        \"name\": \"LEP\",\n" +
      "        \"xref\": {\n" +
      "          \"id\": \"P41159\",\n" +
      "          \"db\": \"uniprot\"\n" +
      "        }\n" +
      "      }\n" +
      "    ]\n" +
      "  }]";

    FactoidToBiopax converter = getBiopaxConvertor(templates, null);

    Model m = converterResultToModel(converter.convertToBiopax());
    assertThat(m.getObjects().size(), equalTo(9));

    Set<RelationshipXref> xrefs = m.getObjects(RelationshipXref.class);
    assertThat(xrefs, notNullValue());
    assertThat(xrefs.size(), equalTo(2));
    xrefs.stream().forEach(x -> {
      assertThat(x.getDb(), isOneOf("pubchem","uniprot"));
      assertThat(x.getId(), isOneOf("P41159","5994"));
    });

    Set<Modulation> mos = m.getObjects(Modulation.class);
    assertThat(mos, not(empty()));
    Modulation mod = mos.iterator().next();
    assertThat(mod.getControlType(), is(ControlType.ACTIVATION));
    assertThat(mod.getDisplayName(), nullValue()); //would be nice to generate "Progesterone activates production of LEP"
    assertThat(mod.getController().size(), equalTo(1));
    Controller con = mod.getController().iterator().next();
    assertThat(con, instanceOf(SmallMolecule.class));
    assertThat(con.getXref(), empty());
    assertThat(((SmallMolecule)con).getEntityReference(), notNullValue());
    assertThat(((SmallMolecule)con).getEntityReference().getXref(), not(empty()));
    assertThat(mod.getControlled().size(), equalTo(1));
    Process proc = mod.getControlled().iterator().next();
    assertThat(proc, instanceOf(Catalysis.class));
    Catalysis cat = (Catalysis) proc;
    assertThat(cat.getController().size(), equalTo(1));
    con = cat.getController().iterator().next();
    assertThat(con, instanceOf(Protein.class));
    assertThat(con.getXref(), empty());
    assertThat(((Protein)con).getEntityReference(), notNullValue());
    assertThat(((Protein)con).getEntityReference().getXref(), not(empty()));
    assertThat(cat.getControlType(), is(ControlType.ACTIVATION));
    //controlled reaction is unknown
    assertThat(cat.getControlled(), empty());
  }
  
  @Test
  public void testCustomIntns() throws IOException {
	  String templates = "[" +
	  		  "{\n" +
		      "    \"type\": \"Custom Interaction\",\n" +
		      "    \"scriptRelPath\": \"src/test/resources/GroovyIntn.groovy\",\n" +
		      "    \"participants\": [\n" +
		      "      {\n" +
		      "        \"type\": \"complex\",\n" +
		      "        \"name\": \"complex\",\n" +
		      "        \"xref\": null,\n" +
		      "        \"components\": [\n" +
		      "      					{\n" +
		      "        						\"type\": \"protein\",\n" +
			  "							     \"cellularLocation\": {\n" +
			  "										\"term\": \"inner kinetochore of condensed chromosome\",\n" +
			  "										\"xref\": {\n" +
			  "											\"id\": \"GO:0000939\",\n" +
			  "											\"db\": \"gene ontology\"\n" +
			  "										}\n" +
			  "									},\n" +
		      "        						\"name\": \"LEP\",\n" +
		      "        						\"xref\": {\n" +
		      "          						\"id\": \"P41159\",\n" +
		      "      							\"db\": \"uniprot\"\n" +
		      "        						}\n" +
		      "                          \n}," +	
		      "      					{\n" +
		      "        						\"type\": \"protein\",\n" +
		      "        						\"name\": \"Saccharopepsin\",\n" +
		      "        						\"xref\": {\n" +
		      "          						\"id\": \"P07267\",\n" +
		      "      							\"db\": \"uniprot\"\n" +
		      "        						}\n" +
		      "                          \n}" +			
		      "                        ]\n" +
		      "      },\n" +
		      "    ]\n" +
		      "  }," +
		      "  {\n" +
		      "    \"type\": \"Custom Interaction\",\n" +
		      "    \"scriptRelPath\": \"src/test/resources/GroovyIntn.groovy\",\n" +
		      "    \"participants\": [\n" +
		      "      {\n" +
		      "        \"type\": \"complex\",\n" +
		      "        \"name\": \"complex\",\n" +
		      "        \"xref\": null,\n" +
		      "        \"components\": [\n" +
		      "      					{\n" +
		      "        						\"type\": \"protein\",\n" +
		      "        						\"name\": \"LEP\",\n" +
		      "        						\"xref\": {\n" +
		      "          						\"id\": \"P41159\",\n" +
		      "      							\"db\": \"uniprot\"\n" +
		      "        						}\n" +
		      "                          \n}," +	
		      "      					{\n" +
		      "        						\"type\": \"protein\",\n" +
		      "        						\"name\": \"Saccharopepsin\",\n" +
		      "        						\"xref\": {\n" +
		      "          						\"id\": \"P07267\",\n" +
		      "      							\"db\": \"uniprot\"\n" +
		      "        						}\n" +
		      "                          \n}" +			
		      "                        ]\n" +
		      "      },\n" +
		      "    ]\n" +
		      "  }," +
		      "  {\n" +
		      "    \"type\": \"Custom Interaction\",\n" +
		      "    \"scriptRelPath\": \"src/test/resources/GroovyIntn.groovy\",\n" +
		      "    \"participants\": [\n" +
		      "      {\n" +
		      "        \"type\": \"complex\",\n" +
		      "        \"name\": \"complex\",\n" +
		      "        \"xref\": null,\n" +
		      "        \"components\": [\n" +
		      "      					{\n" +
		      "        						\"type\": \"protein\",\n" +
		      "        						\"name\": \"LEP\",\n" +
		      "        						\"xref\": {\n" +
		      "          						\"id\": \"P41159\",\n" +
		      "      							\"db\": \"uniprot\"\n" +
		      "        						}\n" +
		      "                          \n}," +	
		      "      					{\n" +
		      "        						\"type\": \"protein\",\n" +
		      "        						\"name\": \"Saccharopepsin\",\n" +
		      "        						\"xref\": {\n" +
		      "          						\"id\": \"P07267\",\n" +
		      "      							\"db\": \"uniprot\"\n" +
		      "        						}\n" +
		      "                          \n}" +			
		      "                        ]\n" +
		      "      },\n" +
		      "    ]\n" +
		      "  }" +
		      "]";
	    FactoidToBiopax converter = getBiopaxConvertor(templates, null);
	
	    Model m = converterResultToModel(converter.convertToBiopax());
	    assertThat(m.getObjects(Complex.class).size(), equalTo(2));
	    assertThat(m.getObjects(CellularLocationVocabulary.class).size(), equalTo(1));
  }
  
  @Test
  public void testCustomIntnsWithValidation() throws IOException {
	  String templates = "[{\n" +
		      "    \"type\": \"Custom Interaction\",\n" +
		      "    \"scriptRelPath\": \"src/test/resources/GroovyIntn2.groovy\",\n" +
		      "    \"participants\": [\n" +
		      "      {\n" +
		      "        \"type\": \"protein\",\n" +
		      "        \"name\": \"Aurora B\",\n" +
		      "        \"xref\": {\n" +
		      "          \"id\": \"Q96GD4\",\n" +
		      "          \"db\": \"UniProtKB\"\n" +
		      "        },\n" +
		      "        \"cellularLocation\": {\n" +
		      "          \"term\": \"inner kinetochore of condensed chromosome\",\n" +
		      "          \"xref\": {\n" +
		      "          	\"id\": \"GO:0000939\",\n" +
		      "          	\"db\": \"gene ontology\"\n" +
		      "    		}\n" +
		      "        },\n" +
		      "        \"organism\": {\n" +
		      "          \"id\": 9606,\n" +
		      "          \"db\": \"taxonomy\"\n" +
		      "        }\n" +
		      "      },\n" +
		      "      {\n" +
		      "        \"type\": \"protein\",\n" +
		      "        \"name\": \"Aurora B\",\n" +
		      "        \"xref\": {\n" +
		      "          \"id\": \"Q96GD4\",\n" +
		      "          \"db\": \"UniProtKB\"\n" +
		      "        },\n" +
		      "        \"organism\": {\n" +
		      "          \"id\": 9606,\n" +
		      "          \"db\": \"taxonomy\"\n" +
		      "        }\n" +
		      "      }\n" +
		      "    ]\n" +
		      "  }]";
	    FactoidToBiopax converter = getBiopaxConvertor(templates, null);
	
	    Model m = converterResultToModel(converter.convertToBiopax());
	    assertThat(m.getObjects(Transport.class).size(), equalTo(1));
	    assertThat(m.getObjects(CellularLocationVocabulary.class).size(), equalTo(1));
	    
	    // we expect one error (where type="ERROR") caused by usage of relationship xref rather then unification xref
	    // see if there is any unexpected one
	    boolean foundUnexpectedError = false;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream inputStream = new ByteArrayInputStream(converter.validate().getBytes());
			Document document = builder.parse(inputStream);
			NodeList errorNodes = document.getElementsByTagName("error");
			
			for ( int i = 0; i < errorNodes.getLength(); i++ ) {
				Node errorNode = errorNodes.item(i);
				String type = errorNode.getAttributes().getNamedItem("type").getNodeValue();
				String code = errorNode.getAttributes().getNamedItem("code").getNodeValue();
				
				if ( type.equals("ERROR") && !code.equals("no.unification.xref") ) {
					foundUnexpectedError = true;
				}
			}
		} catch (ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
		
		assertFalse(foundUnexpectedError);
  }
  
  @Test
  public void testComplexes() throws IOException {
	  String templates = "[" +
	  		  "{\n" +
		      "    \"type\": \"Other Interaction\",\n" +
		      "    \"participants\": [\n" +
		      "      {\n" +
		      "        \"type\": \"complex\",\n" +
		      "        \"name\": \"complex\",\n" +
		      "        \"xref\": null,\n" +
		      "        \"components\": [\n" +
		      "      					{\n" +
		      "        						\"type\": \"protein\",\n" +
		      "        						\"name\": \"LEP\",\n" +
		      "        						\"xref\": {\n" +
		      "          						\"id\": \"P41159\",\n" +
		      "      							\"db\": \"uniprot\"\n" +
		      "        						}\n" +
		      "                          \n}," +	
		      "      					{\n" +
		      "        						\"type\": \"protein\",\n" +
		      "        						\"name\": \"Saccharopepsin\",\n" +
		      "        						\"xref\": {\n" +
		      "          						\"id\": \"P07267\",\n" +
		      "      							\"db\": \"uniprot\"\n" +
		      "        						}\n" +
		      "                          \n}" +			
		      "                        ]\n" +
		      "      },\n" +
		      "      {\n" +
		      "        \"type\": \"protein\",\n" +
		      "        \"name\": \"LEP\",\n" +
		      "        \"xref\": {\n" +
		      "          \"id\": \"P41159\",\n" +
		      "          \"db\": \"uniprot\"\n" +
		      "        }\n" +
		      "      }\n" +
		      "    ]\n" +
		      "  }," +
		      "  {\n" +
		      "    \"type\": \"Other Interaction\",\n" +
		      "    \"participants\": [\n" +
		      "      {\n" +
		      "        \"type\": \"complex\",\n" +
		      "        \"name\": \"complex\",\n" +
		      "        \"xref\": null,\n" +
		      "        \"components\": [\n" +
		      "      					{\n" +
		      "        						\"type\": \"protein\",\n" +
		      "        						\"name\": \"LEP\",\n" +
		      "        						\"xref\": {\n" +
		      "          						\"id\": \"P41159\",\n" +
		      "      							\"db\": \"uniprot\"\n" +
		      "        						}\n" +
		      "                          \n}," +	
		      "      					{\n" +
		      "        						\"type\": \"protein\",\n" +
		      "        						\"name\": \"Saccharopepsin\",\n" +
		      "        						\"xref\": {\n" +
		      "          						\"id\": \"P07267\",\n" +
		      "      							\"db\": \"uniprot\"\n" +
		      "        						}\n" +
		      "                          \n}" +			
		      "                        ]\n" +
		      "      },\n" +
		      "      {\n" +
		      "        \"type\": \"protein\",\n" +
		      "        \"name\": \"EGFR\",\n" +
		      "        \"xref\": {\n" +
		      "          \"id\": \"1956\",\n" +
		      "          \"db\": \"ncbi\"\n" +
		      "        }\n" +
		      "      }\n" +
		      "    ]\n" +
		      "  }" +
		      "]";
	    FactoidToBiopax converter = getBiopaxConvertor(templates, null);
	
	    Model m = converterResultToModel(converter.convertToBiopax());
	    assertThat(m.getObjects(Complex.class).size(), equalTo(1));
	    assertThat(m.getObjects(Protein.class).size(), equalTo(4));
  }
  
 @Test
 public void testOrganism() throws IOException {
	  String templates = "[{\n" +
	      "    \"type\": \"Other Interaction\",\n" +
	      "    \"participants\": [\n" +
	      "      {\n" +
	      "        \"type\": \"protein\",\n" +
	      "        \"name\": \"Saccharopepsin\",\n" +
	      "        \"xref\": {\n" +
	      "          \"id\": P07267,\n" +
	      "          \"db\": \"uniprot\"\n" +
	      "        },\n" +
	      "        \"organism\": {\n" +
	      "          \"id\": 9606,\n" +
	      "          \"db\": \"taxonomy\"\n" +
	      "        }\n" +
	      "      },\n" +
	      "      {\n" +
	      "        \"type\": \"protein\",\n" +
	      "        \"name\": \"Kalirin\",\n" +
	      "        \"xref\": {\n" +
	      "          \"id\": \"O60229\",\n" +
	      "          \"db\": \"uniprot\"\n" +
	      "        },\n" +
	      "        \"organism\": {\n" +
	      "          \"id\": 9606,\n" +
	      "          \"db\": \"taxonomy\"\n" +
	      "        }\n" +
	      "      }\n" +
	      "    ]\n" +
	      "  }]";
	
	FactoidToBiopax converter = getBiopaxConvertor(templates, null);

   Model m = converterResultToModel(converter.convertToBiopax());
   assertThat(m.getObjects(BioSource.class).size(), equalTo(1));
 }
 
 @Test
 public void testBiopaxIds() throws IOException {
	 String templates = "[{\n" +
		      "    \"type\": \"Other Interaction\",\n" +
		      "    \"participants\": [\n" +
		      "      {\n" +
		      "        \"type\": \"protein\",\n" +
		      "        \"name\": \"Saccharopepsin\",\n" +
		      "        \"xref\": {\n" +
		      "          \"id\": P07267,\n" +
		      "          \"db\": \"uniprot\"\n" +
		      "        }\n" +
		      "      },\n" +
		      "      {\n" +
		      "        \"type\": \"chemical\",\n" +
		      "        \"name\": \"ChemName\",\n" +
		      "        \"xref\": {\n" +
		      "          \"id\": 000000,\n" +
		      "          \"db\": \"unsupported\"\n" +
		      "        }\n" +
		      "      }\n" +
		      "    ]\n" +
		      "  }]";
	
	FactoidToBiopax converter = getBiopaxConvertor(templates, null);

   Model m = converterResultToModel(converter.convertToBiopax());
   
   assertThat(m.getObjects(Protein.class).size(), equalTo(1));
   assertThat(m.getObjects(SmallMolecule.class).size(), equalTo(1));
   
   Protein prot = m.getObjects(Protein.class).iterator().next();
   SmallMolecule chem = m.getObjects(SmallMolecule.class).iterator().next();
   EntityReference protRef = prot.getEntityReference();
   EntityReference chemRef = chem.getEntityReference();
   
   assertThat(protRef.getUri(), equalTo("http_//identifiers.org/uniprot/" + "P07267"));
   assertThat(chemRef.getUri(), containsStringIgnoringCase("SmallMoleculeReference"));
   assertThat(protRef.getXref().size(), equalTo(1));
   assertThat(chemRef.getXref().size(), equalTo(1));
   
   Xref protXref = protRef.getXref().iterator().next();
   Xref chemXref = chemRef.getXref().iterator().next();
   assertThat(protXref.getUri(), endsWithIgnoringCase("_" + protXref.getDb() + "_" + protXref.getId()));
   assertThat(chemXref.getUri(), endsWithIgnoringCase("_" + chemXref.getDb() + "_" + chemXref.getId()));
 }

  //local utils

  private Model converterResultToModel(String result)
    throws UnsupportedEncodingException
  {
    return (new SimpleIOHandler()).convertFromOWL(new ByteArrayInputStream(
      result.getBytes("UTF-8")));
  }

}
