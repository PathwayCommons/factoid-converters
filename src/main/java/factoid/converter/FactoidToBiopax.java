package factoid.converter;

import java.io.Reader;

/*
 * A converter class that gets a JSON object that includes sequence of BioPAX templates and enables
 * conversion to BioPAX by adding these templates to underlying Templates Model instance.
 */
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.Complex;
import org.biopax.paxtools.model.level3.ControlType;
import org.biopax.paxtools.model.level3.DnaRegion;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.Rna;
import org.biopax.paxtools.model.level3.SmallMolecule;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import factoid.model.*;

public class FactoidToBiopax {

	//todo: it's a work-in-progress; currently cannot do a round-trip data conversion, e.g. biopax->(biofactoid)json->biopax...

  private static final Map<String, ControlType> CONTROL_TYPE_MAP = createControlTypeMap();
	
	private TemplateModel model;
	private JsonParser jsonParser;
	private Gson gson;
	
	public FactoidToBiopax() {
		model = new TemplateModel();
		jsonParser = new JsonParser();
		gson = new Gson();
	}
	
	public void addToModel(String templateContent) {
		JsonObject template = jsonParser.parse(templateContent).getAsJsonObject();
		addToModel(template);
	}
	
	public void addToModel(Reader contentReader) {
		JsonObject template = jsonParser.parse(contentReader).getAsJsonObject();
		addToModel(template);
	}

  /**
   * Processes the input JSON templates,
   * creates BioPAX objects and adds to the BioPAX model.
   * @param docTemplate
   */
	public void addToModel(JsonObject docTemplate) {
		// quick checks if we can handle this json data
		if(!docTemplate.has("interactions") || !docTemplate.get("interactions").isJsonArray()) {
			throw new AssertionError("Unsupported json shcema (expected 'interactions' array)");
		}

		JsonArray intnTemplates = docTemplate.get("interactions").getAsJsonArray();

		Iterator<JsonElement> it = intnTemplates.iterator();
		while (it.hasNext()) {
			JsonObject template = (JsonObject) it.next();
			String typeStr = template.get("type").getAsString();
			
			if (matchesTemplateType(typeStr, TemplateType.PROTEIN_CONTROLS_STATE)) {
				JsonObject controllerJson = template.get("controller").getAsJsonObject();
				
				String controlTypeStr = null;
				if (template.has("controlType")) {
					controlTypeStr = template.get("controlType").getAsString();
				}
				
				String modification = null;
				if (template.has("modification")) {
					modification = template.get("modification").getAsString();
				}
				
				JsonObject targetJson = template.get("target").getAsJsonObject();
				
				addProteinControlsState(controllerJson, targetJson, modification, controlTypeStr);
			}
			else if (matchesTemplateType(typeStr, TemplateType.EXPRESSION_REGULATION)) {
				JsonObject controllerJson = template.get("controller").getAsJsonObject();
				JsonObject targetJson = template.get("target").getAsJsonObject();
				String controlTypeStr = template.get("controlType").getAsString();
				
				addExpressionRegulation(controllerJson, targetJson, controlTypeStr);
			}
			else if(matchesTemplateType(typeStr, TemplateType.MOLECULAR_INTERACTION)) {
				JsonArray participantsJSON = template.get("participants").getAsJsonArray();
				
				addMolecularInteraction(participantsJSON);
			}
			else if(matchesTemplateType(typeStr, TemplateType.OTHER_INTERACTION)) {
				JsonArray participantsJSON = template.get("participants").getAsJsonArray();
				String controlTypeStr = null;
				
				if (template.has("controlType")) {
					controlTypeStr = template.get("controlType").getAsString();
				}
				
				addOtherInteraction(participantsJSON, controlTypeStr);
			}
		}
		
		if ( docTemplate.has("publication") ) {
			JsonObject pubJson = docTemplate.get("publication").getAsJsonObject();
			setPublication(pubJson);
		}
		
		if ( docTemplate.has("pathwayId") ) {
			String id = docTemplate.get("pathwayId").getAsString();
			setPathwayId(id);
		}
		
		if ( docTemplate.has("pathwayName") ) {
			String name = docTemplate.get("pathwayName").getAsString();
			setPathwayName(name);
		}
	}
	
	private void setPathwayName(String name) {
		model.setPatwayName(name);
	}
	
	private void setPathwayId(String id) {
		model.setPatwayId(id);
	}

	private void setPublication(JsonObject pubJson) {
		XrefModel pubXrefModel = gson.fromJson(pubJson, XrefModel.class);
		
		model.setPublication(pubXrefModel);
	}

	public String convertToBiopax() {
		return model.convertToOwl();
	}
	
	private static String getTemplateName(TemplateType templateType) {
		return templateType.getName();
	}
	
	private static boolean matchesTemplateType(String typeStr, TemplateType templateType) {
		return typeStr.equalsIgnoreCase(getTemplateName(templateType));
	}
	
	private ControlType getControlType(String controlTypeStr) {
		return CONTROL_TYPE_MAP.get(controlTypeStr.toUpperCase());
	}
	
	private  boolean isMacromolecule(Class<? extends BioPAXElement> c) {
		return c == Protein.class || c == DnaRegion.class || c == Rna.class;
	}
	
	private boolean isComplexOrMacromolecule(Class<? extends BioPAXElement> c) {
		return c == Complex.class || isMacromolecule(c);
	}
	
	private void addOtherInteraction(JsonArray participantsJSON, String controlTypeStr) {		
		List<EntityModel> participantModels = gson.fromJson(participantsJSON, new TypeToken<List<EntityModel>>(){}.getType());
		if (controlTypeStr == null) {
			model.addInteraction(participantModels);
		}
		else {
			EntityModel srcModel = participantModels.get(0);
			EntityModel tgtModel = participantModels.get(1);
			Class<? extends PhysicalEntity> controllerClass = srcModel.getEntityClass();
			Class<? extends PhysicalEntity> targetClass = tgtModel.getEntityClass();
			ControlType controlType = getControlType(controlTypeStr);
			
			if (isComplexOrMacromolecule(controllerClass) && isComplexOrMacromolecule(targetClass)) {
				model.addControlSequence(srcModel, tgtModel, controlType);
			}
			else if(controllerClass == Protein.class && targetClass == SmallMolecule.class) {
				model.addControlsConsumptionOrProduction(srcModel, tgtModel, controlType);
			}
			else if(controllerClass == SmallMolecule.class && isMacromolecule(targetClass)) {
				// TODO: convert to modification?				
				model.addModulation(srcModel, tgtModel, controlType);
			}
			else if(controllerClass == SmallMolecule.class && targetClass == SmallMolecule.class) {
				model.addConversion(srcModel, tgtModel, controlType);
			}
		}
	}

	private void addMolecularInteraction(JsonArray participantsJSON) {
		List<EntityModel> participantModels = gson.fromJson(participantsJSON, new TypeToken<List<EntityModel>>(){}.getType());
		model.addMolecularInteraction(participantModels);
	}
	
	private void addProteinControlsState(JsonObject controllerProteinJson, JsonObject targetProteinJson, String modification, String controlTypeStr) {
		ControlType controlType = getControlType(controlTypeStr);
		EntityModel controllerProteinModel = gson.fromJson(controllerProteinJson, EntityModel.class);
		EntityModel targetProteinModel = gson.fromJson(targetProteinJson, EntityModel.class);
		
		model.addControlsState(controllerProteinModel, targetProteinModel, modification, controlType);
	}
	
	private void addExpressionRegulation(JsonObject controllerJson, JsonObject targetJson, String controlTypeStr) {
		ControlType controlType = getControlType(controlTypeStr);
		EntityModel controllerModel = gson.fromJson(controllerJson, EntityModel.class);
		EntityModel targetModel = gson.fromJson(targetJson, EntityModel.class);
		
		model.addExpressionRegulation(controllerModel, targetModel, controlType);
	}
	
	private static Map<String, ControlType> createControlTypeMap() {
		Map<String, ControlType> map = new HashMap<String, ControlType>();
		map.put("ACTIVATION", ControlType.ACTIVATION);
		map.put("INHIBITION", ControlType.INHIBITION);
		
		return map;
	}
	
	private enum TemplateType {
		EXPRESSION_REGULATION("Expression Regulation"),
		MOLECULAR_INTERACTION("Molecular Interaction"),
		PROTEIN_CONTROLS_STATE("Protein Controls State"),
		OTHER_INTERACTION("Other Interaction");
			
		private String name;
		
		TemplateType(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public String toString() {
			return getName();
		}
	}
}
