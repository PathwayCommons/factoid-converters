package factoid.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.CellularLocationVocabulary;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.ControlType;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.ConversionDirectionType;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.RelationshipXref;
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
		
		EntityModel protModel = new EntityModel(protName, protXref, "protein");
		Protein prot1 = model.physicalEntityFromModel(protModel, modificationTypes, null);
		
		assertTrue("Protein is added to the model", innerModel.contains(prot1));
		assertEquals("Protein name is set", prot1.getDisplayName(), protName);
		assertNotNull("Protein reference is set", prot1.getEntityReference());
		assertEquals("Protein modification types are set", modificationTypes.size(), prot1.getFeature().size());
		assertEquals("Protein reference has a new modification", 1, prot1.getEntityReference().getEntityFeature().size());
		
		Protein prot2 = model.physicalEntityFromModel(protModel, modificationTypes, null);
		assertEquals("No duplication in adding the second Protein with same features", prot1, prot2);
		
		Protein prot3 = model.physicalEntityFromModel(protModel);
		assertNotEquals("A new protein is added with no modification", prot1, prot3);
		
		Protein prot4 = model.physicalEntityFromModel(protModel, modificationTypes2, null);
		assertNotEquals("A new protein is added with with different modifications", prot1, prot4);
		assertEquals("Protein reference has a new modification", 2, prot1.getEntityReference().getEntityFeature().size());
	}
	
	@Test
	public void addEntityReferenceTest() {
		
		BioPAXModel model = new BioPAXModel();
		
		// Underlying PAXTools model
		Model innerModel = model.getPaxtoolsModel();
		
		// TODO: add tests for same name but different xref as well
		XrefModel commonXref = new XrefModel("common-xref", "uniprot");
		commonXref.setXrefClass(RelationshipXref.class);
		
		XrefModel uniqXref = new XrefModel("uniq-xref", "uniprot");
		uniqXref.setXrefClass(RelationshipXref.class);
		
		ProteinReference protRef1 = model.getOrCreateEntityReference(ProteinReference.class, commonXref);
		assertTrue("Protein reference is added to the model", innerModel.contains(protRef1));
		assertNotNull("Protein reference xref is set", protRef1.getXref());
		
		ProteinReference protRef2 = model.getOrCreateEntityReference(ProteinReference.class, commonXref);
		assertEquals("No duplication in adding second protein modification with same xref", protRef1, protRef2);
		
		ProteinReference protRef3 = model.getOrCreateEntityReference(ProteinReference.class, uniqXref);
		assertNotEquals("A new protein is added with a new xref", protRef1, protRef3);
	}
	
//	@Test
	public void addCellularLocationVocabularyTest() {
		
		BioPAXModel model = new BioPAXModel();
		
		// Underlying PAXTools model
		Model innerModel = model.getPaxtoolsModel();
		
		String commonLocationName = "location1";
		String uniqueLocationName = "location2";
		
		XrefModel xrefModel = new XrefModel("testId", "testDb");
		
		CellularLocationVocabulary clv1 = model.getOrCreateCellularLocationVocabulary(commonLocationName, xrefModel);
		assertTrue("Cellular location vocabulary is added to the model", innerModel.contains(clv1));
		assertEquals("Cellular location vocabulary has the name", 1, clv1.getTerm().size());
		
		CellularLocationVocabulary clv2 = model.getOrCreateCellularLocationVocabulary(commonLocationName, xrefModel);
		assertEquals("No duplication in adding the second cellular location with the same name", clv1, clv2);
		
		CellularLocationVocabulary clv3 = model.getOrCreateCellularLocationVocabulary(uniqueLocationName, xrefModel);
		assertNotEquals("A new cellular location is added with a new name", clv1, clv3);
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
		XrefModel xrefModel = new XrefModel("test_id", "test_db");
		EntityModel entityModel = new EntityModel(protName, xrefModel, "protein");
		Protein controller = model.physicalEntityFromModel(entityModel);
		ControlType controlType = ControlType.ACTIVATION;
		
		Control control = model.addNewControl(Control.class, controller, controlled, controlType);
		
		assertTrue("Control is added to the model", innerModel.contains(control));
		assertTrue("Controller is set", control.getController().contains(controller));
		assertTrue("Controlled is set", control.getControlled().contains(controlled));
		assertEquals("Control type is set", controlType, control.getControlType());
	}

}
