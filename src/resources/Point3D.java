package resources;

import java.io.Serializable;

public class Point3D implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private double x;
	private double y;
	private double z;
	
	public Point3D() {
		this(0, 0, 0);
	}

	public Point3D(Point3D p) {
		this(p.getX(), p.getY(), p.getZ());
	}
	
	public Point3D(double x, double y, double z) {
		set(x, y, z);
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public double getZ() {
		return z;
	}
	
	public void setX(double x) {
		this.x = x;
	}
	
	public void setY(double y) {
		this.y = y;
	}
	
	public void setZ(double z) {
		this.z = z;
	}
	
	public void set(double x, double y, double z) {
		setX(x);
		setY(y);
		setZ(z);
	}
	
	public void rotateAround(double x, double y, double z, Quaternion rot) {
		double oX = rot.getX();
		double oY = rot.getY();
		double oZ = rot.getZ();
		double oW = rot.getW();
		double nX = x + oW*oW*getX() + 2*oY*oW*getZ() - 2*oZ*oW*getY() + oX*oX*getX() + 2*oY*oX*getY() + 2*oZ*oX*getZ() - oZ*oZ*getX() - oY*oY*getX();
		double nY = y + 2*oX*oY*getX() + oY*oY*getY() + 2*oZ*oY*getZ() + 2*oW*oZ*getX() - oZ*oZ*getY() + oW*oW*getY() - 2*oX*oW*getZ() - oX*oX*getY();
		double nZ = z + 2*oX*oZ*getX() + 2*oY*oZ*getY() + oZ*oZ*getZ() - 2*oW*oY*getX() - oY*oY*getZ() + 2*oW*oX*getY() - oX*oX*getZ() + oW*oW*getZ();
		set(nX, nY, nZ);
	}
	
	public String toString() {
		return String.format("Point3D[%.2f, %.2f, %.2f]", x, y, z);
	}
	
}
