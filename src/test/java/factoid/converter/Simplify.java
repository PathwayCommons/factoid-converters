package factoid.converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.biopax.paxtools.controller.Cloner;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Catalysis;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Evidence;
import org.biopax.paxtools.model.level3.EvidenceCodeVocabulary;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.TemplateReactionRegulation;

public class Simplify {
	public static void main(String[] args) throws FileNotFoundException {
		new Simplify().simplify();
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

	
	private void simplify() throws FileNotFoundException {
		InputStream f = new FileInputStream(new File(getClass().getResource("/pc2.owl").getFile()));
		BioPAXIOHandler handler = new SimpleIOHandler();
		Model model = handler.convertFromOWL(f);
		
		BioPAXFactory factory = BioPAXLevel.L3.getDefaultFactory();
//		Model newModel = factory.createModel();
		
		List<Interaction> intns = model.getObjects(Interaction.class)
				.stream()
				.filter(intn -> intn instanceof Catalysis)
				.filter(intn -> intn.getControlledOf().size() == 0)
				.collect(Collectors.toList());
//		int size = intns.size();
//		int numOfRemaining = 10;
		Set<BioPAXElement> els = new HashSet<BioPAXElement>();
		
		for ( int i = 0; i < 10; i++ ) {
			Catalysis intn = (Catalysis) intns.get(i);
//			newModel.add(intn);
			els.add(intn);
			els.addAll(intn.getXref());
			Set<Entity> ppts = intn.getParticipant();
			els.addAll(ppts);
			
			for ( Entity ppt : ppts ) {
				els.addAll(ppt.getXref());
				if ( ppt instanceof SimplePhysicalEntity ) {
					EntityReference er = ((SimplePhysicalEntity) ppt).getEntityReference();
					els.add(er);
					els.addAll(er.getXref());
					if ( er instanceof ProteinReference ) {
						BioSource org = ((ProteinReference) er).getOrganism();
						els.add(org);
						els.addAll(org.getXref());
					}
					
				}
			}
//			ppts.stream().map(p -> els.addAll(p.getXref()));
			Set<Process> controlledSet = intn.getControlled();
			if (controlledSet.size() > 0) {
				Interaction controlled = (Interaction) controlledSet.iterator().next();
				els.add(controlled);
//				els.addAll(controlled.getParticipant());
//				controlled.getParticipant().stream().map(p -> els.addAll(p.getXref()));
				for (Entity ppt : controlled.getParticipant()) {
					els.add(ppt);
					els.addAll(ppt.getXref());
				}
				Set<Evidence> evidences = controlled.getEvidence();
				els.addAll(evidences);
//				Set<Entity> controlledPpts = controlled.getP 
				for (Evidence evidence : evidences) {
					els.add(evidence);
					els.addAll(evidence.getXref());
					Set<EvidenceCodeVocabulary> evidenceCodes = evidence.getEvidenceCode();
					els.addAll(evidenceCodes);
//					evidenceCodes.stream().map(ec -> els.addAll(ec.getXref()));
					for(EvidenceCodeVocabulary ec : evidenceCodes) {
						els.addAll(ec.getXref());
					}
				}
			}
		}
		
		Cloner cln = new Cloner(SimpleEditorMap.L3, BioPAXLevel.L3.getDefaultFactory());
		Model excisedModel = cln.clone(els);
		System.out.println(excisedModel.getObjects().size());
		String owl = SimpleIOHandler.convertToOwl(excisedModel);
		
		FileWriter writer;
		try {
//			/Users/siperm/Documents/Workspace/factoid-converters/bin/test/pc2.owl
			writer = new FileWriter(new File("src/test/resources/pc_sm4.owl"));
			writer.write(owl);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
