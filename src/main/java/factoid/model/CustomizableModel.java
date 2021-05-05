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
}
