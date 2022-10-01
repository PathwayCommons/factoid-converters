package factoid.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.ControlType;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.ConversionDirectionType;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.junit.jupiter.api.Test;

public class BioPAXModelTest {
	
	@Test
	public void addPhysicalEntityTest() throws NoSuchFieldException, SecurityException {
		
		BioPAXModel model = new BioPAXModel();
		
		// Underlying PAXTools model
		Model innerModel = model.getPaxtoolsModel();
		
		String protName = "TP53";
		XrefModel protXref = new XrefModel("xrefid1", "uniprot");
		
		Set<String> modificationTypes = new HashSet<String>();
		modificationTypes.add("active");
		
		Set<String> modificationTypes2 = new HashSet<String>();
		modificationTypes2.add("inactive");
		
		ProteinReference protRef = model.getOrCreateEntityReference(ProteinReference.class, protName, protXref);
		
		Protein prot1 = model.getOrCreatePhysicalEntity(Protein.class, protName, protRef, modificationTypes, null);
		
		assertTrue(innerModel.contains(prot1), "Protein is added to the model");
		assertEquals(prot1.getDisplayName(), protName, "Protein name is set");
		assertEquals(protRef, prot1.getEntityReference(), "Protein reference is set");
		assertEquals(modificationTypes.size(), prot1.getFeature().size(), "Protein modification types are set");
		assertEquals(1, protRef.getEntityFeature().size(), "Protein reference has a new modification");
		
		Protein prot2 = model.getOrCreatePhysicalEntity(Protein.class, protName, protRef, modificationTypes, null);
		assertEquals(prot1, prot2, "No duplication in adding the second Protein with same features");
		
		Protein prot3 = model.getOrCreatePhysicalEntity(Protein.class, protName, protRef);
		assertNotEquals(prot1, prot3, "A new protein is added with no modification");
		
		Protein prot4 = model.getOrCreatePhysicalEntity(Protein.class, protName, protRef, modificationTypes2, null);
		assertNotEquals(prot1, prot4, "A new protein is added with with different modifications");
		assertEquals(2, protRef.getEntityFeature().size(), "Protein reference has a new modification");
	}
	
	@Test
	public void addEntityReferenceTest() {
		
		BioPAXModel model = new BioPAXModel();
		
		// Underlying PAXTools model
		Model innerModel = model.getPaxtoolsModel();
		
		String commonName = "Protein1";
		String uniqueName = "Protein2";
		
		// TODO: add tests for same name but different xref as well
		XrefModel commonXref = new XrefModel("common-xref", "uniprot");
		
		ProteinReference protRef1 = model.getOrCreateEntityReference(ProteinReference.class, commonName, commonXref);
		assertTrue(innerModel.contains(protRef1), "Protein reference is added to the model");
		assertEquals(commonName, protRef1.getDisplayName(), "Protein reference name is set");
		
		ProteinReference protRef2 = model.getOrCreateEntityReference(ProteinReference.class, commonName, commonXref);
		assertEquals(protRef1, protRef2, "No duplication in adding second protein modification with same name");
		
		SmallMoleculeReference smRef = model.getOrCreateEntityReference(SmallMoleculeReference.class, commonName, commonXref);
		assertNotEquals(protRef2, smRef, "A new small molecule reference is added that has an existing protein reference name");
		
		ProteinReference protRef3 = model.getOrCreateEntityReference(ProteinReference.class, uniqueName, commonXref);
		assertNotEquals(protRef1, protRef3, "A new protein is added with a new name");
	}
	
	@Test
	public void addConversionTest() {
		
		BioPAXModel model = new BioPAXModel();
		
		// Underlying PAXTools model
		Model innerModel = model.getPaxtoolsModel();
		
		ConversionDirectionType dir = ConversionDirectionType.LEFT_TO_RIGHT;
		Protein left = model.addNew(Protein.class);
		Protein right = model.addNew(Protein.class);
		
		Conversion conversion = model.addNewConversion(Conversion.class, left, right, dir);
		assertTrue(innerModel.contains(conversion), "Conversion is added to the model");
		assertTrue(conversion.getLeft().contains(left), "Coversions left side is set");
		assertTrue(conversion.getRight().contains(right), "Coversions right side is set");
		assertEquals(dir, conversion.getConversionDirection(), "Conversion direction is set");
	}
	
	@Test
	public void addControlTest() {
		
		BioPAXModel model = new BioPAXModel();
		
		// Underlying PAXTools model
		Model innerModel = model.getPaxtoolsModel();
		
		Conversion controlled = model.addNewConversion(Conversion.class);
		String protName = "prot1";
		ProteinReference controllerref = model.getOrCreateEntityReference(ProteinReference.class, protName, null);
		Protein controller = model.getOrCreatePhysicalEntity(Protein.class, protName, controllerref);
		ControlType controlType = ControlType.ACTIVATION;
		
		Control control = model.addNewControl(Control.class, controller, controlled, controlType);
		
		assertTrue(innerModel.contains(control), "Control is added to the model");
		assertTrue(control.getController().contains(controller), "Controller is set");
		assertTrue(control.getControlled().contains(controlled), "Controlled is set");
		assertEquals(controlType, control.getControlType(), "Control type is set");
	}

}
