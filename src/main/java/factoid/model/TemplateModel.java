package factoid.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.biopax.paxtools.model.level3.BiochemicalReaction;
import org.biopax.paxtools.model.level3.Catalysis;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.ControlType;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.Modulation;
import org.biopax.paxtools.model.level3.MolecularInteraction;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.TemplateReaction;
import org.biopax.paxtools.model.level3.TemplateReactionRegulation;

/*
 * A model class that keeps an underlying BioPAX model and enables updating it by adding templates.
 */
public class TemplateModel {
	
	private BioPAXModel model;

	private static final String ACTIVE = "active";

	private enum SideType {
		LEFT,
		RIGHT
	}
	
	public TemplateModel() {
		model = new BioPAXModel();
	}
	
	// Section: public methods
	
	// modifiers

	public void addControlsState(EntityModel controllerModel, EntityModel targetModel, String modification, ControlType controlType) {
		
		Set<String> leftMFTypes = new HashSet<String>();
		Set<String> rightMFTypes = new HashSet<String>();
		Set<String> leftNotMFTypes = new HashSet<String>();
		Set<String> rightNotMFTypes = new HashSet<String>();
		
		if (modification != null) {
			leftNotMFTypes.add(modification);
			rightMFTypes.add(modification);
		}
		
		addActiveNotActiveFeatures(controlType, leftMFTypes, rightMFTypes, leftNotMFTypes, rightNotMFTypes);
		
		PhysicalEntity left = physicalEntityFromModel(targetModel, leftMFTypes, leftNotMFTypes);
		PhysicalEntity right = physicalEntityFromModel(targetModel, rightMFTypes, rightNotMFTypes);
		
		Class<? extends PhysicalEntity> controllerClass = targetModel.getEntityClass();
		Class<? extends EntityReference> controllerRefClass = targetModel.getEntityRefClass();
		
		PhysicalEntity controller = physicalEntityFromModel(controllerModel);
		
		Conversion conversion = model.addNewConversion(Conversion.class, left, right);
		model.addNewControl(Catalysis.class, controller, conversion, ControlType.ACTIVATION);
	}
	
	public void addModulation(EntityModel controllerModel, EntityModel targetModel, ControlType controlType) {
		
		PhysicalEntity target = physicalEntityFromModel(targetModel);
		PhysicalEntity controller = physicalEntityFromModel(controllerModel);
		
		Catalysis catalysis = model.addNewControl(Catalysis.class, target, null, ControlType.ACTIVATION);
		model.addNewControl(Modulation.class, controller, catalysis, controlType);
	}
	
	public void addMolecularInteraction(List<EntityModel> participantModels) {
		addInteractionWithParticipants(MolecularInteraction.class, participantModels);
	}
	
	public void addInteraction(List<EntityModel> participantModels) {
		addInteractionWithParticipants(Interaction.class, participantModels);
	}
	
	public void addExpressionRegulation(EntityModel controllerModel, EntityModel targetModel, ControlType controlType) {
		PhysicalEntity controller = physicalEntityFromModel(controllerModel);
		PhysicalEntity product = physicalEntityFromModel(targetModel);
		
		TemplateReaction reaction = model.addNew(TemplateReaction.class);
		reaction.addProduct(product);
		
		model.addNewControl(TemplateReactionRegulation.class, controller, reaction, controlType);
	}
	
	public void addControlSequence(EntityModel entityModel1, EntityModel entityModel2, ControlType controlType) {
		PhysicalEntity entity1 = physicalEntityFromModel(entityModel1);
		PhysicalEntity entity2 = physicalEntityFromModel(entityModel2);
		
		// Second entity is controller in somewhere where controlled is unknown
		// First entity controls the interaction above
		Control controlled = model.addNewControl(Control.class, entity2, null, controlType);
		model.addNewControl(Control.class, entity1, controlled, controlType);
	}
	
	public void addControlsConsumptionOrProduction(EntityModel controllerModel, EntityModel targetModel, ControlType controlType) {
		SideType targetSide = controlType.equals(ControlType.ACTIVATION) ? SideType.RIGHT : SideType.LEFT;
		BiochemicalReaction reaction = model.addNewConversion(BiochemicalReaction.class);
		addNewEntityToConversion(reaction, targetModel, targetSide);
		PhysicalEntity catalyzer = physicalEntityFromModel(controllerModel);
		
		model.addNewControl(Catalysis.class, catalyzer, reaction, null);
	}
	
	public void addConversion(EntityModel srcModel, EntityModel tgtModel, ControlType controlType) {
		Conversion conversion = model.addNewConversion(Conversion.class);
		SideType srcSide = SideType.LEFT;
		SideType tgtSide = SideType.RIGHT;
		
		// if controlType is inhibition switch the sides
		if (controlType == ControlType.INHIBITION) {
			srcSide = SideType.RIGHT;
			tgtSide = SideType.LEFT;
		}
		
		addNewEntityToConversion(conversion, srcModel, srcSide);
		addNewEntityToConversion(conversion, tgtModel, tgtSide);
	}
	
	// accessors
	
	public String convertToOwl() {
		return model.convertToOwl();
	}
	
	// Section: private helper methods
	
	private <T extends PhysicalEntity> T physicalEntityFromModel(EntityModel entityModel) {
		return physicalEntityFromModel(entityModel, null, null);
	}
	
	private <T extends PhysicalEntity> T physicalEntityFromModel(EntityModel entityModel, Set<String> modificationTypes, Set<String> modificationNotTypes) {
		String name = entityModel.getName();
		XrefModel xref = entityModel.getXref();
		XrefModel org = entityModel.getOrganism();
		
		
		List<EntityModel> componentModels = entityModel.getComponentModels();
		boolean inComplex = false;
		
		Class<? extends EntityReference> entityRefClass = entityModel.getEntityRefClass();
		Class<? extends PhysicalEntity> entityClass = entityModel.getEntityClass();
		EntityReference entityRef = model.getOrCreateEntityReference(entityRefClass, name, xref, org);
		
		T entity = (T) model.getOrCreatePhysicalEntity(entityClass, name, entityRef, modificationTypes, modificationNotTypes, inComplex, componentModels);
		
		return entity;
	}
	
	private <T extends Interaction> void addInteractionWithParticipants(Class<T> c, List<EntityModel> participantModels) {
		T interaction = model.addNew(c);
		
		for(EntityModel participantModel : participantModels) {
			PhysicalEntity participant = physicalEntityFromModel(participantModel);
			interaction.addParticipant(participant);
		}
	}
	
	// Add a new entity to the given side of conversion
	private void addNewEntityToConversion(Conversion conversion, EntityModel entityModel, SideType sideType) {
		PhysicalEntity entity = physicalEntityFromModel(entityModel);
		addSideToConversion(conversion, entity, sideType);
	}
	
	// Add a physical entity to given side of conversation.
	private static void addSideToConversion(Conversion conversion, PhysicalEntity entity, SideType side) {
		if (side == SideType.LEFT) {
			conversion.addLeft(entity);
		}
		else if (side == SideType.RIGHT) {
			conversion.addRight(entity);
		}
	}
	
	// Update types set by adding ACTIVE type or notType to the sides according to control type
	private static void addActiveNotActiveFeatures(ControlType controlType, Set<String> leftTypes, Set<String> rightTypes, Set<String> leftNotTypes, Set<String> rightNotTypes){
		if ( controlType == ControlType.ACTIVATION ) {
			rightTypes.add(ACTIVE);
			leftNotTypes.add(ACTIVE);
		}
		else if (controlType == ControlType.INHIBITION) {
			leftTypes.add(ACTIVE);
			rightNotTypes.add(ACTIVE);
		}
	}

	public void setPublication(XrefModel pubXref) {
		model.createPublicaitonXref(pubXref);
	}

	public void setPatwayName(String name) {
		model.setPatwayName(name);
	}

	public void setPatwayId(String id) {
		model.setPatwayId(id);
	}
	
	public BioPAXModel getBiopaxModel() {
		return model;
	}
}
