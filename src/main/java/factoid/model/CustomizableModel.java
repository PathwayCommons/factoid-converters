package factoid.model;

import java.util.List;
import java.util.Set;

import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.PhysicalEntity;

public class CustomizableModel {
	private BioPAXModel model;
	
	public CustomizableModel(BioPAXModel model) {
		this.model = model;
	}
	
	public <T extends PhysicalEntity> T getOrCreatePhysicalEntity(Class<T> c, String name, EntityReference entityRef, Set<String> modificationTypes, Set<String> modificationNotTypes, boolean inComplex, List<EntityModel> componentModels) {
		return model.getOrCreatePhysicalEntity(c, name, entityRef, modificationTypes, modificationNotTypes, inComplex, componentModels);
	}
	
	public <T extends EntityReference> T getOrCreateEntityReference(Class<T> c, String name, XrefModel xrefModel, XrefModel organismModel) {
		return model.getOrCreateEntityReference(c, name, xrefModel, organismModel);
	}
	
	public <T extends Interaction> T addNewInteraction(Class<T> c) {
		return model.addNewInteraction(c);
	}
	
	public <T extends PhysicalEntity> T getOrCreatePhysicalEntity(Class<T> c, String name, EntityReference entityRef, boolean inComplex, List<EntityModel> componentModels) {
		Set<String> modificationTypes = null;
		Set<String> modificationNotTypes = null;
		return getOrCreatePhysicalEntity(c, name, entityRef, modificationTypes, modificationNotTypes, inComplex, componentModels);
	}
	
	public <T extends PhysicalEntity> T getOrCreatePhysicalEntity(Class<T> c, String name, EntityReference entityRef, Set<String> modificationTypes, Set<String> modificationNotTypes) {
		boolean inComplex = false;
		return getOrCreatePhysicalEntity(c, name, entityRef, modificationTypes, modificationNotTypes, inComplex, null);
		
	}
	
	public <T extends PhysicalEntity> T getOrCreatePhysicalEntity(Class<T> c, String name, EntityReference entityRef) {
		return getOrCreatePhysicalEntity(c, name, entityRef, null, null);
	}
	
	public <T extends PhysicalEntity> T getOrCreatePhysicalEntity(Class<T> c, String name) {
		return getOrCreatePhysicalEntity(c, name, null);
	}
	
	public <T extends PhysicalEntity> T getOrCreatePhysicalEntity(Class<T> c) {
		return getOrCreatePhysicalEntity(c, null);
	}
	
	public String validate() {
		return model.validate();
	}
	
	public <T extends PhysicalEntity> T physicalEntityFromModel(EntityModel entityModel) {
		return model.physicalEntityFromModel(entityModel);
	}
	
	public <T extends PhysicalEntity> T physicalEntityFromModel(EntityModel entityModel, Set<String> modificationTypes, Set<String> modificationNotTypes) {
		return model.physicalEntityFromModel(entityModel, modificationTypes, modificationNotTypes);
	}
}
