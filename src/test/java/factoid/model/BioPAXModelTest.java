package factoid.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

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
import org.junit.Test;

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
		
		assertTrue("Protein is added to the model", innerModel.contains(prot1));
		assertEquals("Protein name is set", prot1.getDisplayName(), protName);
		assertEquals("Protein reference is set", protRef, prot1.getEntityReference());
		assertEquals("Protein modification types are set", modificationTypes.size(), prot1.getFeature().size());
		assertEquals("Protein reference has a new modification", 1, protRef.getEntityFeature().size());
		
		Protein prot2 = model.getOrCreatePhysicalEntity(Protein.class, protName, protRef, modificationTypes, null);
		assertEquals("No duplication in adding the second Protein with same features", prot1, prot2);
		
		Protein prot3 = model.getOrCreatePhysicalEntity(Protein.class, protName, protRef);
		assertNotEquals("A new protein is added with no modification", prot1, prot3);
		
		Protein prot4 = model.getOrCreatePhysicalEntity(Protein.class, protName, protRef, modificationTypes2, null);
		assertNotEquals("A new protein is added with with different modifications", prot1, prot4);
		assertEquals("Protein reference has a new modification", 2, protRef.getEntityFeature().size());
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
		assertTrue("Protein reference is added to the model", innerModel.contains(protRef1));
		assertEquals("Protein reference name is set", commonName, protRef1.getDisplayName());
		
		ProteinReference protRef2 = model.getOrCreateEntityReference(ProteinReference.class, commonName, commonXref);
		assertEquals("No duplication in adding second protein modification with same name", protRef1, protRef2);
		
		SmallMoleculeReference smRef = model.getOrCreateEntityReference(SmallMoleculeReference.class, commonName, commonXref);
		assertNotEquals("A new small molecule reference is added that has an existing protein reference name", protRef2, smRef);
		
		ProteinReference protRef3 = model.getOrCreateEntityReference(ProteinReference.class, uniqueName, commonXref);
		assertNotEquals("A new protein is added with a new name", protRef1, protRef3);
	}
	
//	@Test
//	public void addCellularLocationVocabularyTest() {
//		
//		BioPAXModel model = new BioPAXModel();
//		
//		// Underlying PAXTools model
//		Model innerModel = model.getPaxtoolsModel();
//		
//		String commonLocationName = "location1";
//		String uniqueLocationName = "location2";
//		
//		CellularLocationVocabulary clv1 = model.getOrCreateCellularLocationVocabulary(commonLocationName);
//		assertTrue("Cellular location vocabulary is added to the model", innerModel.contains(clv1));
//		assertEquals("Cellular location vocabulary has the name", 1, clv1.getTerm().size());
//		
//		CellularLocationVocabulary clv2 = model.getOrCreateCellularLocationVocabulary(commonLocationName);
//		assertEquals("No duplication in adding the second cellular location with the same name", clv1, clv2);
//		
//		CellularLocationVocabulary clv3 = model.getOrCreateCellularLocationVocabulary(uniqueLocationName);
//		assertNotEquals("A new cellular location is added with a new name", clv1, clv3);
//	}
	
	@Test
	public void addConversionTest() {
		
		BioPAXModel model = new BioPAXModel();
		
		// Underlying PAXTools model
		Model innerModel = model.getPaxtoolsModel();
		
		ConversionDirectionType dir = ConversionDirectionType.LEFT_TO_RIGHT;
		Protein left = model.addNew(Protein.class);
		Protein right = model.addNew(Protein.class);
		
		Conversion conversion = model.addNewConversion(Conversion.class, left, right, dir);
		assertTrue("Conversion is added to the model", innerModel.contains(conversion));
		assertTrue("Coversions left side is set", conversion.getLeft().contains(left));
		assertTrue("Coversions right side is set", conversion.getRight().contains(right));
		assertEquals("Conversion direction is set", dir, conversion.getConversionDirection());
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
		
		assertTrue("Control is added to the model", innerModel.contains(control));
		assertTrue("Controller is set", control.getController().contains(controller));
		assertTrue("Controlled is set", control.getControlled().contains(controlled));
		assertEquals("Control type is set", controlType, control.getControlType());
	}

}
