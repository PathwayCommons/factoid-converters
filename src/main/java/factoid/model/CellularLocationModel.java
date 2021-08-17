package factoid.model;

public class CellularLocationModel {
	private String term;
	private XrefModel xref;
	
	public CellularLocationModel(String term, XrefModel xref) {
		this.term = term;
		this.xref = xref;
	}
	
	public void setTerm(String term) {
		this.term = term;
	}
	
	public void setXref(XrefModel xref) {
		this.xref = xref;
	}
	
	public String getTerm() {
		return term;
	}
	
	public XrefModel getXref() {
		return xref;
	}
}
