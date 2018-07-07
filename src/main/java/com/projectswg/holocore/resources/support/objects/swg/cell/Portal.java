package com.projectswg.holocore.resources.support.objects.swg.cell;

import com.projectswg.common.data.location.Point3D;

public class Portal {
	
	private final CellObject cell1;
	private final CellObject cell2;
	private final Point3D frame1;
	private final Point3D frame2;
	private final double height;
	
	public Portal(CellObject cell1, CellObject cell2, Point3D frame1, Point3D frame2, double height) {
		this.cell1 = cell1;
		this.cell2 = cell2;
		this.frame1 = frame1;
		this.frame2 = frame2;
		this.height = height;
	}
	
	public CellObject getOtherCell(CellObject cell) {
		return cell1 == cell ? cell2 : cell1;
	}
	
	public CellObject getCell1() {
		return cell1;
	}
	
	public CellObject getCell2() {
		return cell2;
	}
	
	public Point3D getFrame1() {
		return frame1;
	}
	
	public Point3D getFrame2() {
		return frame2;
	}
	
	public double getHeight() {
		return height;
	}
	
}
