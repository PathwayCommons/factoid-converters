package factoid.converter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Catalysis;
import org.biopax.paxtools.model.level3.Complex;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.ControlType;
import org.biopax.paxtools.model.level3.Controller;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.Dna;
import org.biopax.paxtools.model.level3.DnaRegion;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.EntityFeature;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Evidence;
import org.biopax.paxtools.model.level3.EvidenceCodeVocabulary;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.ModificationFeature;
import org.biopax.paxtools.model.level3.MolecularInteraction;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.Rna;
import org.biopax.paxtools.model.level3.SequenceEntity;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.SmallMolecule;
import org.biopax.paxtools.model.level3.TemplateReaction;
import org.biopax.paxtools.model.level3.TemplateReactionRegulation;
import org.biopax.paxtools.model.level3.Xref;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;



public class BiopaxToFactoid {
	
	public BiopaxToFactoid() {
		
	}
	
	public JsonObject convert(Model model) {
		JsonObject o = new JsonObject();
		Set<Interaction> intns = model.getObjects(Interaction.class);
		intns = intns.stream()
				.filter(intn -> intn.getControlledOf().size() == 0)
				.filter(intn -> keepIntn(intn))
				.collect(Collectors.toSet());

		for (Interaction intn : intns) {
			Set<String> markedPmids = new HashSet<String>();
			Process controlled = getControlled(intn);
			for ( Evidence evidence : controlled.getEvidence() ) {
				for (Xref xref : evidence.getXref()) {
					String db = xref.getDb();
					if ( db != null && db.equalsIgnoreCase("pubmed") ) {
						String pmid = xref.getId();
						if ( pmid != null && !markedPmids.contains(pmid) ) {
							if (o.get(pmid) == null) {
								o.add(pmid, new JsonArray());
							}
							JsonArray arr = (JsonArray) o.get(pmid);
							handleIntn(arr, intn);
							markedPmids.add(pmid);
						}
					}
				}
			}
		}
		return o;
	}
	
	private <T extends Object>  T getOptional(Optional<T> o) {
		if ( o.isPresent() ) {
			return o.get();
		}
		
		return null;
	}
	
	private Process getControlled(Interaction intn) {
		Set<Process> s = null;
		if ( intn instanceof Control ) {
			s = ((Control) intn).getControlled();
		}
		if ( intn instanceof TemplateReactionRegulation ) {
			s = ((TemplateReactionRegulation) intn).getControlled();
		}
		if ( intn instanceof Catalysis ) {
			s = ((Catalysis) intn).getControlled();
		}
		if ( s == null ) {
			return null;
		}
		Optional<Process> first = s.stream().findFirst();
		if ( first.isPresent() ) {
			return first.get();
		}
		
		return null;
	}
	
	private boolean keepIntn(Interaction intn) {
		Process controlled = getControlled(intn);
		if ( controlled == null ) {
			return false;
		}
		for( Evidence evidence : controlled.getEvidence() ) {
			for(EvidenceCodeVocabulary v : evidence.getEvidenceCode()) {
				for(Xref xref : v.getXref()) {
					String id = xref.getId();
					if ( id.equalsIgnoreCase("MI:0074") || id.equalsIgnoreCase("MI:0421")
							|| id.equalsIgnoreCase("MI:0113")) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	private JsonObject makeIntnJson(String type, ControlType ctrlType, List<String> participantIds, String srcId, String tgtId) {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", type);
		obj.addProperty("association", type);
		
		if ( ctrlType != null ) {
			obj.addProperty("controlType", ctrlType.toString());
		}
		
		if (participantIds == null && srcId != null && tgtId != null) {
			participantIds = new ArrayList<String>();
			participantIds.add(srcId);
			participantIds.add(tgtId);
		}
		JsonArray participantsArr = new JsonArray();
		for ( String pptId : participantIds ) {
			JsonObject pptObj = new JsonObject();
			pptObj.addProperty("id", pptId);
			if ( pptId == tgtId ) {
				pptObj.addProperty("group", "positive");
			}
			participantsArr.add(pptObj);
		}
		obj.add("entries", participantsArr);
		obj.addProperty("id", generateUUID());

		return obj;
	}
	
	// Generate unique id for new elements
	private static String generateUUID() {
		return UUID.randomUUID().toString();
	}
	
	private JsonObject makeEntityJson(Entity entity) {
		String type = getEntityType(entity);
		String name = entity.getDisplayName();
		
		Xref xref = null;
		
		if ( entity instanceof SimplePhysicalEntity ) {
			EntityReference er = ((SimplePhysicalEntity) entity).getEntityReference();
			if ( er != null && er.getXref().size() > 0 ) {
				xref = getOptional(er.getXref().stream().findFirst());
			}
		}
		
		JsonObject obj = new JsonObject();
		obj.addProperty("type", type);
		obj.addProperty("name", name);
		obj.addProperty("id", generateUUID());
		
		if ( xref != null ) {
			String db = xref.getDb();
			String id = xref.getId();
			
			if ( db.equals("uniprot knowledgebase") ) {
				obj.addProperty("uniprotId", id);
			}
		}
		
		return obj;
	}
	
	private JsonObject xrefToJson(Xref xref) {
		JsonObject xrefJson = null;
		
		if ( xref != null ) {
			xrefJson = new JsonObject();
			xrefJson.addProperty("id", xref.getId());
			xrefJson.addProperty("db", xref.getDb());
		}
		
		return xrefJson;
	}
	
	private String getEntityType(Entity e) {
		if ( e instanceof Protein ) {
			return "protein";
		}
		else if ( e instanceof SmallMolecule ) {
			return "chemical";
		}
		else if ( e instanceof Complex ) {
			return "complex";
		}
		else if ( e instanceof DnaRegion ) {
			return "dna";
		}
		else if ( e instanceof Rna ) {
			return "rna";
		}
		else {
			try {
				throw new Exception("Unexpected enitity class" + e.getClass().toString());
			} catch (Exception exc) {
				exc.printStackTrace();
				return null;
			}
		}
	}
	
	private boolean isMacromolecule(Class c) {
		return isDna(c) || isRna(c) || isProtein(c);
	}
	
	private boolean isDna(Class c) {
		return c == Dna.class;
	}
	
	private boolean isRna(Class c) {
		return c == Rna.class;
	}
	
	private boolean isProtein(Class c) {
		return c == Protein.class;
	}
	
	private boolean isComplex(Class c) {
		return c == Complex.class;
	}
	
	private boolean isChemical(Class c) {
		return c == SmallMolecule.class;
	}
	
	private PhysicalEntity getLeafEntity(Entity entity) {
		if (entity instanceof PhysicalEntity) {
			return (PhysicalEntity) entity;
		}
		if (entity instanceof Interaction) {
			Set<Entity> ppts = ((Interaction) entity).getParticipant();
			for ( Entity ppt : ppts ) {
				PhysicalEntity leaf = getLeafEntity(ppt);
				if ( leaf != null ) {
					return leaf;
				}
			}
		}
		return null;
	}
	
	private List<String> handleEntities(JsonArray arr, Set<Entity> ppts, Entity src, Entity tgt) {
		List<String> ids = new ArrayList<String>();
		
		if ( src != null && tgt != null ) {
			JsonObject srcObj = makeEntityJson(src);
			JsonObject tgtObj = makeEntityJson(tgt);
			
			arr.add(srcObj);
			arr.add(tgtObj);
			
			ids.add(srcObj.get("id").getAsString());
			ids.add(tgtObj.get("id").getAsString());
		}
		if ( ppts != null ) {
			for (Entity ppt : ppts) {
				JsonObject pptObj = makeEntityJson(ppt);
				arr.add(pptObj);
				ids.add(pptObj.get("id").getAsString());
			}
		}
		
		return ids;
	}
	
	private void handleIntn(JsonArray arr, Interaction intn) {
		Class c = intn.getClass();
		
//		if ( c == MolecularInteraction.class ) {
//			JsonObject obj = makeIntnJson("Molecular Interaction", null, intn.getParticipant(), null, null);
//			return obj;
//		}
		if ( c == TemplateReactionRegulation.class ) {
			TemplateReactionRegulation regulation = (TemplateReactionRegulation) intn;
			Controller src = getOptional(regulation.getController().stream().findFirst());
			Class srcClass = src.getClass();
			if ( isMacromolecule(srcClass) || isComplex(srcClass) ) {
				Process reaction = getOptional(regulation.getControlled().stream().findFirst());
				if (reaction.getClass() == Process.class) {
					PhysicalEntity tgt = getOptional(((TemplateReaction) reaction).getProduct().stream().findFirst());
					Class tgtClass = tgt.getClass();
					if ( isProtein(tgtClass) || isRna(tgtClass) ) {
						ControlType ctrlType = regulation.getControlType();
						List<String> ids = handleEntities(arr, null, src, tgt);
						JsonObject intnObj = makeIntnJson("transcription-translation", ctrlType, null, ids.get(0), ids.get(1));
						arr.add(intnObj);
						return;
					}
				}
			}
		}
		if ( c == Catalysis.class ) {
			Catalysis catalysis = (Catalysis) intn;
			Controller src = getOptional(catalysis.getController().stream().findFirst());
			Class srcClass = src.getClass();
			if ( isMacromolecule(srcClass) || isComplex(srcClass) ) {
				Conversion conversion = (Conversion) getOptional(catalysis.getControlled().stream().findFirst());
				// getLeft() would also work instead of getRight()				
				PhysicalEntity tgt = getOptional(((Conversion) conversion).getRight().stream().findFirst());
				Class tgtClass = tgt.getClass();
				if ( isMacromolecule(tgtClass) ) {
					ControlType ctrlType = catalysis.getControlType();
					String type = "modification";
					EntityFeature mf = getOptional(((PhysicalEntity) tgt).getFeature().stream().filter(f -> f instanceof ModificationFeature).findFirst());
					
					if ( mf != null ) {
						type = mf.toString().replace("ion", "ed");
					}
					List<String> ids = handleEntities(arr, null, src, tgt);
					JsonObject intnObj = makeIntnJson(type, ctrlType, null, ids.get(0), ids.get(1));
					arr.add(intnObj);
					return;
				}
			}
		}
		
		Set<Entity> ppts = intn.getParticipant();
		List<Entity> pePPts = ppts.stream().filter(p -> p instanceof PhysicalEntity).collect(Collectors.toList());
		List<Entity> intnPPts = ppts.stream().filter(p -> p instanceof Interaction).collect(Collectors.toList());
		if ( pePPts.size() == ppts.size() ) {
			List<String> ids = handleEntities(arr, ppts, null, null);
			JsonObject intnObj = makeIntnJson("interaction", null, ids, null, null);
			arr.add(intnObj);
			return;
		}
		if ( pePPts.size() == 1 && intnPPts.size() == 1 ) {
			PhysicalEntity src = (PhysicalEntity) pePPts.get(0);
			PhysicalEntity tgt = getLeafEntity(intnPPts.get(0));
			
			if ( tgt != null ) {
				List<String> ids = handleEntities(arr, null, src, tgt);
				JsonObject intnObj = makeIntnJson("interaction", null, null, ids.get(0), ids.get(1));
				arr.add(intnObj);
				return;
			}
		}
		Set<Entity> leafs = ppts.stream()
				.map(p -> getLeafEntity(p))
				.filter(l -> l != null)
				.collect(Collectors.toSet());
		
		List<String> ids = handleEntities(arr, leafs, null, null);
		JsonObject intnObj = makeIntnJson("interaction", null, ids, null, null);
		arr.add(intnObj);
	}
}
