package factoid.converter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
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
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.Rna;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.SmallMolecule;
import org.biopax.paxtools.model.level3.TemplateReaction;
import org.biopax.paxtools.model.level3.TemplateReactionRegulation;
import org.biopax.paxtools.model.level3.Xref;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;



public class BiopaxToFactoid {
	
	Logger logger;
	
	public BiopaxToFactoid() {
		logger = Logger.getLogger(BiopaxToFactoid.class.getName()); 
	}
	
	public JsonObject convert(Model model) {
		JsonObject o = new JsonObject();
		Set<Interaction> intns = model.getObjects(Interaction.class);
		intns = intns.stream()
				.filter(intn -> intn.getControlledOf().size() == 0)
				.filter(intn -> keepIntn(intn))
				.collect(Collectors.toSet());

		for (Interaction intn : intns) {
			Set<String> markedPmids = new HashSet<>();
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
		
		Set<String> pmids = o.keySet();
		Set<String> idsToRemove = new HashSet<>();
		
		// remove the documents including ungrounded entities
		for ( String pmid : pmids ) {
			if ( o.get(pmid).getAsJsonArray().size() == 0 ) {
				idsToRemove.add(pmid);
			}
		}
		
		for ( String pmid : idsToRemove ) {
			o.remove(pmid);
		}
		
		return o;
	}
	
	private <T extends Object>  T getOptional(Optional<T> o) {
		if ( o.isPresent() ) {
			return o.get();
		}
		
		return null;
	}
	
	private void addToJsonArr(JsonArray arr, JsonObject o) {
		if ( o != null ) {
			arr.add(o);
		}
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
		BioSource org = null;
		
		if ( entity instanceof SimplePhysicalEntity ) {
			EntityReference er = ((SimplePhysicalEntity) entity).getEntityReference();
			if ( er instanceof ProteinReference ) {
				org = ((ProteinReference) er).getOrganism();
			}
			if ( er != null && er.getXref().size() > 0 ) {
				xref = getOptional(er.getXref().stream().findFirst());
			}
		}
		
		// if the entity has no grounding return null
		// which will signal that the related interaction must be skipped as well
		if ( xref == null || org == null ) {
			return null;
		}
		
		JsonObject obj = new JsonObject();
		obj.addProperty("type", type);
		obj.addProperty("name", name);
		obj.addProperty("id", generateUUID());
		
		String originalDb = xref.getDb();
		String id = xref.getId();
		String db = null;
		
		if ( originalDb.contains("uniprot") ) {
			db = "uniprot";
		} else if (  originalDb.equalsIgnoreCase("hgnc.symbol")
				|| originalDb.equalsIgnoreCase("hgnc symbol")
				|| originalDb.equalsIgnoreCase("refseq") ) {
			db = originalDb;
		} else {
			logger.warning("Unhandled xref database: " + originalDb);
			// if the entity db is not handled return null
			// which will signal that the related interaction must be skipped as well
			return null;
		}
		
		if ( db != null ) {
			JsonObject jsXref = new JsonObject();
			jsXref.addProperty("db", db);
			jsXref.addProperty("id", id);
			
			Xref orgXref = getOptional(org.getXref().stream().findFirst());
			if ( orgXref == null ) {
				return null;
			}
			
			jsXref.addProperty("org", orgXref.getId());
			obj.add("_xref", jsXref);
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
			
			if ( srcObj == null || tgtObj == null ) {
				// if the interaction has entities skipped because of the grounding issues
				// skip the interaction as well
				return null;
			}
			
			addToJsonArr(arr, srcObj);
			addToJsonArr(arr, tgtObj);
			
			ids.add(srcObj.get("id").getAsString());
			ids.add(tgtObj.get("id").getAsString());
		}
		if ( ppts != null ) {
			Set<JsonObject> pptObjs = new HashSet<JsonObject>();
			for (Entity ppt : ppts) {
				JsonObject pptObj = makeEntityJson(ppt);
				
				if ( pptObj == null ) {
					// if the interaction has entities skipped because of the grounding issues
					// skip the interaction as well
					return null;
				}
				
				pptObjs.add(pptObj);
				
				ids.add(pptObj.get("id").getAsString());
			}
			
			for ( JsonObject pptObj : pptObjs ) {
				addToJsonArr(arr, pptObj);
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
						if ( ids == null ) {
							return;
						}
						JsonObject intnObj = makeIntnJson("transcription-translation", ctrlType, null, ids.get(0), ids.get(1));
						addToJsonArr(arr, intnObj);
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
					if ( ids == null ) {
						return;
					}
					JsonObject intnObj = makeIntnJson(type, ctrlType, null, ids.get(0), ids.get(1));
					addToJsonArr(arr, intnObj);
					return;
				}
			}
		}
		
		Set<Entity> ppts = intn.getParticipant();
		List<Entity> pePPts = ppts.stream().filter(p -> p instanceof PhysicalEntity).collect(Collectors.toList());
		List<Entity> intnPPts = ppts.stream().filter(p -> p instanceof Interaction).collect(Collectors.toList());
		if ( pePPts.size() == ppts.size() ) {
			List<String> ids = handleEntities(arr, ppts, null, null);
			if ( ids == null ) {
				return;
			}
			JsonObject intnObj = makeIntnJson("interaction", null, ids, null, null);
			addToJsonArr(arr, intnObj);
			return;
		}
		if ( pePPts.size() == 1 && intnPPts.size() == 1 ) {
			PhysicalEntity src = (PhysicalEntity) pePPts.get(0);
			PhysicalEntity tgt = getLeafEntity(intnPPts.get(0));
			
			if ( tgt != null ) {
				List<String> ids = handleEntities(arr, null, src, tgt);
				if ( ids == null ) {
					return;
				}
				JsonObject intnObj = makeIntnJson("interaction", null, null, ids.get(0), ids.get(1));
				addToJsonArr(arr, intnObj);
				return;
			}
		}
		Set<Entity> leafs = ppts.stream()
				.map(p -> getLeafEntity(p))
				.filter(l -> l != null)
				.collect(Collectors.toSet());
		
		List<String> ids = handleEntities(arr, leafs, null, null);
		if ( ids == null ) {
			return;
		}
		JsonObject intnObj = makeIntnJson("interaction", null, ids, null, null);
		addToJsonArr(arr, intnObj);
	}
}
