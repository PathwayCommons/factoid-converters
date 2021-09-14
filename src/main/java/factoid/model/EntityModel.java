package factoid.model;

import java.util.List;

import org.biopax.paxtools.model.level3.Complex;
import org.biopax.paxtools.model.level3.DnaRegion;
import org.biopax.paxtools.model.level3.DnaRegionReference;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.Rna;
import org.biopax.paxtools.model.level3.RnaReference;
import org.biopax.paxtools.model.level3.SmallMolecule;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;

// TODO: decide if cellular location and other modification, modification not features must be added to here
public class EntityModel {
	
	private String name;
	private XrefModel xref;
	private String type;
	private List<EntityModel> components;
	private XrefModel organism;
	private CellularLocationModel cellularLocation;
	
	public EntityModel(String name, XrefModel xref, String type, XrefModel organism, List<EntityModel> components, CellularLocationModel cellularLocation) {
		this.name = name;
		this.xref = xref;
		this.type = type;
		this.components = components;
		this.organism = organism;
		this.cellularLocation = cellularLocation;
	}
	
	public EntityModel(String name, XrefModel xref, String type, XrefModel organism, List<EntityModel> components) {
		this(name, xref, type, organism, components, null);
	}
	
	public EntityModel(String name, XrefModel xref, String type) {
		this(name, xref, type, null, null);
	}
	
	public EntityModel(String name, XrefModel xref, String type, XrefModel organism) {
		this(name, xref, type, organism, null);
	}
	
	public EntityModel(String name, XrefModel xref, String type, List<EntityModel> components) {
		this(name, xref, type, null, components);
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setXref(XrefModel xref) {
		this.xref = xref;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void setCellularLocation(CellularLocationModel cellularLocation) {
		this.cellularLocation = cellularLocation;
	}
	
	public String getName() {
		return name;
	}
	
	public XrefModel getXref() {
		return xref;
	}
	
	public String getType() {
		return type;
	}
	
	public CellularLocationModel getCellularLocation() {
		return cellularLocation;
	}
	
	public XrefModel getOrganism() {
		return organism;
	}
	
	public List<EntityModel> getComponentModels() {
		return components;
	}
	
	public Class<? extends Xref> getEntityXrefClass() {
		if ("ggp".equalsIgnoreCase(type)) {
			return UnificationXref.class;
		}
		return RelationshipXref.class;
	}
	
	public Class<? extends PhysicalEntity> getEntityClass() {
		if("entity".equalsIgnoreCase(type)) {
			return PhysicalEntity.class;
		}		
		else if ("complex".equalsIgnoreCase(type)) {
			return Complex.class;
		}
		// TODO: what to do about ggp is not certain yet
		// consider it as a protein for now
		else if ("protein".equalsIgnoreCase(type) || "ggp".equalsIgnoreCase(type)) {
			return Protein.class;
		}
		else if("chemical".equalsIgnoreCase(type)) {
			return SmallMolecule.class;
		}
		else if("dna".equalsIgnoreCase(type)) {
			return DnaRegion.class;
		}
		else if("rna".equalsIgnoreCase(type)) {
			return Rna.class;
		}
		else {
			throw new IllegalArgumentException("Not a valid entity type: " + type);
		}
	}
	
	public Class<? extends EntityReference> getEntityRefClass() {
		Class<? extends PhysicalEntity> entityClass = getEntityClass();
		if ( entityClass == Protein.class ) {
			return ProteinReference.class;
		}
		else if ( entityClass == SmallMolecule.class ) {
			return SmallMoleculeReference.class;
		}
		else if( entityClass == DnaRegion.class ) {
			return DnaRegionReference.class;
		}
		else if( entityClass == Rna.class ) {
			return RnaReference.class;
		}
		else if( entityClass == PhysicalEntity.class || entityClass == Complex.class ){
			return null;
		}
		else {
			throw new IllegalArgumentException("Not a valid entity class");
		}
	}
}
