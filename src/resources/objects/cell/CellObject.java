package resources.objects.cell;

import resources.objects.SWGObject;

public class CellObject extends SWGObject {
	
	private static final long serialVersionUID = 1L;
	
	private boolean	isPublic	= true;
	private int		number		= 0;
	private String	label		= "";
	
	public CellObject(long objectId) {
		super(objectId);
	}
	
	public boolean isPublic() {
		return isPublic;
	}
	
	public int getNumber() {
		return number;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
	
	public void setNumber(int number) {
		this.number = number;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
}
