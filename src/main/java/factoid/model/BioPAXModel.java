/*
 * A model class that keeps an underlying PaxTools model and enables updating it by wrapper functions.
 * This model is designed to avoid duplications of BioPAX elements in certain conditions.
 */

package factoid.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level2.pathwayComponent;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Complex;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.ControlType;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.Controller;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.ConversionDirectionType;
import org.biopax.paxtools.model.level3.EntityFeature;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.ModificationFeature;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.RnaReference;
import org.biopax.paxtools.model.level3.SequenceEntityReference;
import org.biopax.paxtools.model.level3.SequenceModificationVocabulary;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;

public class BioPAXModel {
	
	// Underlying paxtools model
	private Model model;
	private Pathway pathway;
	// Map of term to cellular location
//	private Map<String, CellularLocationVocabulary> cellularLocationMap;
	// Map of xref id to xref itself
	private Map<String, RelationshipXref> xrefMap;
	private Map<String, BioSource> organismMap;
	// Multiple key map of entity reference class and name to entity reference itself
	private MultiKeyMap<Object, EntityReference> entityReferenceMap;
	// Multiple key map of entity name class and name to entity reference itself
	private MultiKeyMap<Object, Set<PhysicalEntity>> noRefPhysicalEntityMap;
	
	// Section: constructors
	
	public BioPAXModel() {
		BioPAXFactory factory = BioPAXLevel.L3.getDefaultFactory();
		model = factory.createModel();
		
		pathway = addNew(Pathway.class);
		
//		cellularLocationMap = new HashMap<String, CellularLocationVocabulary>();
		xrefMap = new HashMap<String, RelationshipXref>();
		organismMap = new HashMap<String, BioSource>();
		entityReferenceMap = new MultiKeyMap<Object, EntityReference>();
		noRefPhysicalEntityMap = new MultiKeyMap<Object, Set<PhysicalEntity>>();
	}

	//for tests
	protected Model getPaxtoolsModel() {
		return model;
	}

	// Section: public methods
	
	// add a new element to model with given id
	public <T extends BioPAXElement> T addNew(Class<T> c, String id, boolean omitPathwayComponent) {
		T el = model.addNew(c, id);
		
		if( !omitPathwayComponent && isInteractionOrSubclass(c) ) {
			pathway.addPathwayComponent((Interaction) el);
		}
		
		return el;
	}
	
	// add a new element to model by generating element id
	public <T extends BioPAXElement> T addNew(Class<T> c) {
		return addNew(c, false);
	}
	
	// add a new element to model by generating element id
	// if omitPathwayComponent parameter is not set to false then create
	// a pathway component for the element if it is an instance of
	// interaction or a subclass of interaction
	public <T extends BioPAXElement> T addNew(Class<T> c, boolean omitPathwayComponent) {
		return addNew(c, generateUUID(), omitPathwayComponent);
	}
	
	// Just get a physical entity, create it if not available yet.
	// Do not create duplicate entities if entity references, cellular locations and modifications set matches.
	public <T extends PhysicalEntity> T getOrCreatePhysicalEntity(Class<T> c, String name, EntityReference entityRef, Set<String> modificationTypes, Set<String> modificationNotTypes, boolean inComplex, List<EntityModel> componentModels) {
		
		T entity = null;
		
		if (c == Complex.class) {
			assert entityRef == null : "A complex cannot have an EntityReference";
			entity = (T) findMatchingComplex(name, componentModels, modificationTypes, modificationNotTypes);
		}
		else if (isSimplePhysicalEntityOrSubclass(c)) {
			assert entityRef != null : "Entity reference must be specified to obtain a SimplePhysicalEntity";
			
			Set<T> entities = (Set<T>) entityRef.getEntityReferenceOf();
			entity = findMatchingEntity(entities, modificationTypes, modificationNotTypes, inComplex);
		}
		
		if (entity == null) {
			entity = addNewPhysicalEntity(c, name, entityRef, modificationTypes, modificationNotTypes, componentModels);
		}
		
		return entity;
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
	
	public RelationshipXref getOrCreateEntityXref(XrefModel xrefModel) {
		
		if (xrefModel == null) {
			return null;
		}
		
		String xrefId = xrefModel.getId();
		RelationshipXref xref = xrefMap.get(xrefId);
		
		if (xref == null) {
			xref = addNew(RelationshipXref.class);
			xref.setId(xrefId);
			xref.setDb(xrefModel.getDb());
			xrefMap.put(xrefId, xref);
		}
		
		return xref;
	}
	
	// Get cellular location matching the given term, create one if not available
//	public CellularLocationVocabulary getOrCreateCellularLocationVocabulary(String term) {
//		
//		CellularLocationVocabulary clv = cellularLocationMap.get(term);
//		
//		// if a clv does not exists for the term create one here and put it to the map
//		if(clv == null) {
//			clv = addNewControlledVocabulary(CellularLocationVocabulary.class, term);
//			cellularLocationMap.put(term, clv);
//		}
//		
//		return clv;
//	}
	
	// Get modification feature that has the given modification type. Create one if not available.
	public ModificationFeature getOrCreateModificationFeature(String modificationType, EntityReference entityRef) {
		
		Set<EntityFeature> referenceModifications = entityRef.getEntityFeature();
		ModificationFeature modificationFeature = getFeatureByModificationType((Set)referenceModifications, modificationType);
		
		// if a modification feature does not exists for the modification type create one here and put it to the map
		if (modificationFeature == null) {
			modificationFeature = addNewModificationFeature(modificationType);
			entityRef.addEntityFeature(modificationFeature);
		}
		
		return modificationFeature;
	}
	
	// Get entity reference that has given name and class, create a new one is not available yet.
	public <T extends EntityReference> T getOrCreateEntityReference(Class<T> c, String name, XrefModel xrefModel, XrefModel organismModel) {
		
		if ( c == null ) {
			return null;
		}
		
		T entityRef = null;
		RelationshipXref xref = getOrCreateEntityXref(xrefModel);
		BioSource organism = null;
		
		if ( organismModel != null ) {
			organism = getOrCreateOrganism(organismModel);
		}
		
		// if a name is specified try to get an existing entity reference with the
		// same name and entity class first
		if (name != null) {
			entityRef = (T) entityReferenceMap.get(c, name, xref, organism);
		}
		
		if (entityRef == null) {
			entityRef = addNewEntityReference(c, name, xref, organism);
			entityReferenceMap.put(c, name, xref, organism, entityRef);
		}
		
		return entityRef;
	}
	
	public <T extends EntityReference> T getOrCreateEntityReference(Class<T> c, String name, XrefModel xrefModel) {
		return getOrCreateEntityReference(c, name, xrefModel, null);
	}
	
	private BioSource getOrCreateOrganism(XrefModel organismModel) {
		if (organismModel == null) {
			return null;
		}
		
		String orgId = organismModel.getId();
		BioSource org = organismMap.get(orgId);
		
		if (org == null) {
			org = addNew(BioSource.class);
			UnificationXref xref = addNew(UnificationXref.class);
			xref.setId(orgId);
			xref.setDb(organismModel.getDb());
			org.addXref(xref);
		}
		
		organismMap.put(orgId, org);
		
		return org;
	}
	
	public <T extends Interaction> T addNewInteraction(Class<T> c) {
		return addNew(c);
	}

	// Create a new conversion by given properties
	public <T extends Conversion> T addNewConversion(Class<T> c, PhysicalEntity left, PhysicalEntity right, ConversionDirectionType dir) {
		T conversion = addNew(c);
		
		if(left != null) {
			conversion.addLeft(left);
		}
		
		if(right != null) {
			conversion.addRight(right);
		}
		
		if(dir != null) {
			conversion.setConversionDirection(dir);
		}
		
		return conversion;
	}
	
	public <T extends Conversion> T addNewConversion(Class<T> c) {
		return addNewConversion(c, null, null);
	}
	
	public <T extends Conversion> T addNewConversion(Class<T> c, PhysicalEntity left, PhysicalEntity right) {
		return addNewConversion(c, left, right, ConversionDirectionType.LEFT_TO_RIGHT);
	}
	
	// Create a new control instance by given properties
	public <T extends Control> T addNewControl(Class<T> c, Controller controller, Process controlled, ControlType controlType) {
		
		T control = addNew(c);
		
		if(controller != null) {
			control.addController(controller);
		}
		
		if(controlled != null) {
			control.addControlled(controlled);
		}
		
		if(controlType != null) {
			control.setControlType(controlType);
		}
		
		return control;
	}
	
	public String convertToOwl() {
		return SimpleIOHandler.convertToOwl(model);
	}
	
	// Section: private helper methods
	
	// Generate unique id for new elements
	private static String generateUUID() {
		return UUID.randomUUID().toString();
	}
	
	// Find the physical entity that has the expected cellular location and modification types
	private static <T extends PhysicalEntity> T findMatchingEntity(Set<T> entities, Set<String> modificationTypes, Set<String> modificationNotTypes, boolean inComplex){		
		
		Optional<T> match = entities.stream().filter(t -> {
			boolean tInComplex = !t.getComponentOf().isEmpty();
			return tInComplex == inComplex
					&& isAbstractionOf(getModificationFeatureOfEntity(t, false), modificationTypes)
					&& isAbstractionOf(getModificationFeatureOfEntity(t, true), modificationNotTypes);
		} ).findFirst();
		
		if (match.isPresent()) {
			return match.get();
		}
		
		return null;
	}
	
	private static boolean compareComplexComponents(List<EntityModel> componentModels, Set<PhysicalEntity> components) {
		// It is expected the input does not include any interaction whose one side is a component of any complex.
		// Such interactions must be moved to the complex itself in the input.
		// Therefore, there is no need to handle modifications etc here, just name and xref is enough.
		String seperator = "\t";
		Set<String> modelSummary = componentModels.stream().map(m -> {
			String xrefId = null;
			String xrefDb = null;
			XrefModel xref = m.getXref();
			if ( xref != null ) {
				xrefId = xref.getId();
				xrefDb = xref.getDb();
			}
			return m.getName() + seperator + xrefId + seperator + xrefDb;
		}).collect(Collectors.toSet());
		
		Set<String> entitiesSummary = components.stream().map(c -> {
			String xrefId = null;
			String xrefDb = null;
			
			if ( isSimplePhysicalEntityOrSubclass(c.getClass()) ) {
				Iterator<Xref> it = ((SimplePhysicalEntity)c).getEntityReference().getXref().iterator();
				
				if ( it.hasNext() ) {
					Xref xref = it.next();
					xrefId = xref.getId();
					xrefDb = xref.getDb();
				}
			}
			
			return c.getName().iterator().next() + seperator + xrefId + seperator + xrefDb;
		}).collect(Collectors.toSet());
		
		return modelSummary.equals(entitiesSummary);
	}
	
	private  Complex findMatchingComplex(String name, List<EntityModel> componentModels, Set<String> modificationTypes, Set<String> modificationNotTypes) {
		Set<PhysicalEntity> candidates = noRefPhysicalEntityMap.get(Complex.class, name);
		
		if ( candidates == null ) {
			return null;
		}
		
		Optional<PhysicalEntity> match = candidates.stream().filter(t -> {
			Set<PhysicalEntity> components = ((Complex) t).getComponent();
			return isAbstractionOf(getModificationFeatureOfEntity(t, false), modificationTypes)
					&& isAbstractionOf(getModificationFeatureOfEntity(t, true), modificationNotTypes)
					&& compareComplexComponents(componentModels, components);
					
		} ).findFirst();
		
		if (match.isPresent()) {
			return (Complex) match.get();
		}
		
		return null;
	}
	
	// compare 2 object while staying away from null pointer exception
	private static boolean nullSafeEquals(Object obj1, Object obj2) {
		// if one is null but other is not return false
		if ( ( obj1 == null ) != ( obj2 == null ) ) {
			return false;
		}
		
		// now it is known that obj2 is null given that obj1 is null
		return obj1 == null || obj1.equals(obj2);
	}
	
	private static Set<ModificationFeature> getModificationFeatureOfEntity(PhysicalEntity entity, boolean useNotFeature){
		
		Set<EntityFeature> entityFeatures = useNotFeature ? entity.getNotFeature() : entity.getFeature();
		
		// Assert that any entity feature is a ModificationFeature since other kind of entity features are 
		// not supposed to be created
		assert allAreInstanceOfModificationFeature(entityFeatures) : 
			"All members of feature set is expected to have modification feature type";
		
		// Do not filter modification features by relying on the assumption that all features are 
		// modification feature for better performance. If that is not the case the assertion above is
		// suppose to fail.
		return (Set) entityFeatures;
	}
	
	private static <T extends PhysicalEntity> boolean isSimplePhysicalEntityOrSubclass(Class<T> c) {
		return SimplePhysicalEntity.class.isAssignableFrom(c);
	}
	
	private static <T extends Object> boolean isInteractionOrSubclass(Class<T> c) {
		return Interaction.class.isAssignableFrom(c);
	}
	
	private static <T extends PhysicalEntity> void assertSimplePhysicalEntityOrSubclass(Class<T> c, String messageOpt) {
		
		String message = null;
		
		if (messageOpt == null) {
			message = "Entity reference field is valid only for instances of SimplePhysicalEntity and its subclasses";
		}
		else {
			message = messageOpt;
		}
		
		assert isSimplePhysicalEntityOrSubclass(c) : message;
	}
	
	private static <T extends PhysicalEntity> void assertSimplePhysicalEntityOrSubclass(Class<T> c) {
		assertSimplePhysicalEntityOrSubclass(c, null);
	}
	
	// TODO try converting this to allAreInstanceOf(Class? c, Collection collection)
	private static <T extends EntityFeature> boolean allAreInstanceOfModificationFeature(Collection<T> collection) {
		Iterator<T> it = collection.iterator();
		while(it.hasNext()) {
			Object o = it.next();
			if (!(o instanceof ModificationFeature)) {
				return false;
			}
		}
		return true;
	}
	
	// check if a collection is either empty or null
	private static <T extends Object> boolean isEmptyOrNull(Collection<T> collection) {
		return collection == null || collection.isEmpty();
	}
	
	// Check if modificationTypes set is an abstraction of modifications set
	private static boolean isAbstractionOf(Set<ModificationFeature> modifications, Set<String> modificationTypes) {
		
		// return false if only one side is empty or null 
		if ( isEmptyOrNull(modifications) == !isEmptyOrNull(modificationTypes)  ) {
			return false;
		}
		
		// return true if both sides are empty or null
		// note that we made sure that either both or none is empty or null
		if (isEmptyOrNull(modifications)) {
			return true;
		}
		
		if (modifications.isEmpty() && modificationTypes.isEmpty()) {
			return true;
		}
		
		if (modifications.size() != modificationTypes.size()) {
			return false;
		}
		
		return modifications.stream().map(t -> getOnlyElement(t.getModificationType().getTerm())).collect(Collectors.toSet()).equals(modificationTypes);
	}
	
	// get only element of collection
	// TODO this method would be moved to a utility file
	private static <T extends Object> T getOnlyElement(Collection<T> collection) {
		return collection.iterator().next();
	}
	
	// Create a new physical entity with given properties
	private <T extends PhysicalEntity> T addNewPhysicalEntity(Class<T> c, String name, EntityReference entityRef, 
			Set<String> modificationTypes, Set<String> modificationNotTypes, List<EntityModel> componentModels) {
		
		T entity = addNew(c);
		
		if (name != null) {
			entity.setDisplayName(name);
		}
		
		if (entityRef != null) {
			assertSimplePhysicalEntityOrSubclass(c);
			
			((SimplePhysicalEntity) entity).setEntityReference(entityRef);
		}
		else {
			if ( !noRefPhysicalEntityMap.containsKey(c, name) ) {
				noRefPhysicalEntityMap.put(c, name, new HashSet<PhysicalEntity>());
			}
			noRefPhysicalEntityMap.get(c, name).add(entity);
		}
		
		if (modificationTypes != null) {
			for(String modificationType : modificationTypes) {
				ModificationFeature modificationFeature = getOrCreateModificationFeature(modificationType, entityRef);
				entity.addFeature(modificationFeature);
			}
		}
		
		if (modificationNotTypes != null) {
			for(String modificationNotType : modificationNotTypes) {
				ModificationFeature modificationFeature = getOrCreateModificationFeature(modificationNotType, entityRef);
				entity.addNotFeature(modificationFeature);
			}
		}
		
		if (componentModels != null) {
			for(EntityModel model : componentModels) {
				String cName = model.getName();
				boolean inComplex = true;
				EntityReference cRef = getOrCreateEntityReference(model.getEntityRefClass(), cName, model.getXref());
				PhysicalEntity component = getOrCreatePhysicalEntity(model.getEntityClass(), cName, cRef, null, null, inComplex, null);
				((Complex) entity).addComponent(component);
			}
		}
		
		return entity;
	}
	
	// create a new controlled vocabulary initialized with the given term
	private <T extends ControlledVocabulary> T addNewControlledVocabulary(Class<T> c, String term) {
		T vocab = addNew(c);
		
		if (term != null) {
			vocab.addTerm(term);
		}
		
		return vocab;
	}
	
	// Create a new modification feature that has the given modification type
	private ModificationFeature addNewModificationFeature(String modificationType) {
		SequenceModificationVocabulary seqModVocab = addNewControlledVocabulary(SequenceModificationVocabulary.class, modificationType);
		
		ModificationFeature modificationFeature = addNew(ModificationFeature.class);
		modificationFeature.setModificationType(seqModVocab);
		
		return modificationFeature;
	}
	
	// Get modification feature with the given modification type from a set of modification features
	private ModificationFeature getFeatureByModificationType(Set<ModificationFeature> modificationFeatures,
			String modificationType) {
		
		Iterator<ModificationFeature> it = modificationFeatures.iterator();
		
		while (it.hasNext()) {
			ModificationFeature modificationFeature = it.next();
			Set<String> terms = modificationFeature.getModificationType().getTerm();
			if ( getOnlyElement(terms).equals(modificationType) ) {
				return modificationFeature;
			}
		}
		
		return null;
	}
	
	// Create a new entity reference by given properties
	private <T extends EntityReference> T addNewEntityReference(Class<T> c, String name, RelationshipXref xref, BioSource organism) {
		
		T entityRef = addNew(c);
		
		if(name != null) {
			entityRef.setDisplayName(name);
		}
		
		if(xref != null) {
			entityRef.addXref(xref);
		}
		
		if(organism != null) {
			assert SequenceEntityReference.class.isAssignableFrom(c) 
				: "An entity reference must be subclass of SequenceEntityReference class to be able to have an organism";
			
			try {
				((SequenceEntityReference) entityRef).setOrganism(organism);
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		
		return entityRef;
	}

	public void createPublicaitonXref(XrefModel model) {
		PublicationXref xref = addNew(PublicationXref.class);
		xref.setId(model.getId());
		xref.setDb(model.getDb());
		
		pathway.addXref(xref);
	}

	public void setPatwayName(String name) {
		pathway.setDisplayName(name);
	}

	public void setPatwayId(String id) {
		UnificationXref xref = addNew(UnificationXref.class);
		xref.setId(id);
		xref.setDb("BioFactoid");
		
		pathway.addXref(xref);
	}
}