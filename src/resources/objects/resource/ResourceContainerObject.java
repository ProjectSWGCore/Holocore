package resources.objects.resource;

import resources.objects.tangible.TangibleObject;

public class ResourceContainerObject extends TangibleObject {
	
	private static final long serialVersionUID = 1L;
	
	private long	resourceType	= 0;
	private String	resourceName	= "";
	private int		quantity		= 0;
	private int		maxQuantity		= 0;
	private String	parentName		= "";
	private String	displayName		= "";
	
	public ResourceContainerObject(long objectId) {
		super(objectId);
	}
	
	public long getResourceType() {
		return resourceType;
	}
	
	public String getResourceName() {
		return resourceName;
	}
	
	public int getQuantity() {
		return quantity;
	}
	
	public int getMaxQuantity() {
		return maxQuantity;
	}
	
	public String getParentName() {
		return parentName;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public void setResourceType(long resourceType) {
		this.resourceType = resourceType;
	}
	
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}
	
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	
	public void setMaxQuantity(int maxQuantity) {
		this.maxQuantity = maxQuantity;
	}
	
	public void setParentName(String parentName) {
		this.parentName = parentName;
	}
	
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
}
