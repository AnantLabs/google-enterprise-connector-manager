package com.google.enterprise.connector.sp2c_migration;

import java.util.List;

/**
 * Holder for attributes of both folders and documents.
 */
abstract class DirEntry {
	private final String name;
  private final String id;
  private final String parentId;
	private List<Ace> acl;
	private String owner;

  DirEntry(String name, String id, String parentId, List<Ace> acl, String owner) {
		this.name = name;
		this.id = id;
		this.parentId = parentId;
		this.acl = acl;
		this.owner = owner;
  }
  public String getName() {
		return name;
  }

  public String getId() {
		return id;
  }

  public String getParentId() {
		return parentId;
  }

  public List<Ace> getAcl() {
		return acl;
  }

  public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

    public void setAcl(List<Ace> acl) {
		this.acl = acl;
	}

    @Override
	public String toString() {
      return "Document: {name = " + name + "; owner = " + owner + "; parentId = " + parentId + "}";
	}
}
