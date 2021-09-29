package factoid.model;

import org.biopax.paxtools.model.level3.Xref;

public class XrefModel {
	
	private String id;
	private String db;
	private Class<? extends Xref> c;
	
	public XrefModel(String id, String db) {
		this.id = id;
		this.db = db;
		this.c = null;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setDb(String db) {
		this.db = db;
	}
	
	public String getId() {
		return id;
	}
	
	public String getDb() {
		return db;
	}
	
	public Class<? extends Xref> getXrefClass() {
		return c;
	}
	
	public void setXrefClass(Class<? extends Xref> c) {
		this.c = c;
	}
}
