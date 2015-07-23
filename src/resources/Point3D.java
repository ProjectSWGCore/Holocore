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
		double k0 = oW * oW - 0.5;
		double k1 = getX() * oX + getY() * oY + getZ() * oZ;
		double nX = x + 2 * (getX() * k0 + oX * k1 + oW * (oY * getZ() - oZ * getY()));
		double nY = y + 2 * (getY() * k0 + oY * k1 + oW * (getZ() * getX() - oX * getZ()));
		double nZ = z + 2 * (getZ() * k0 + oZ * k1 + oW * (oX * getY() - oY * getX()));
		set(nX, nY, nZ);
	}
	
	public String toString() {
		return String.format("Point3D[%.2f, %.2f, %.2f]", x, y, z);
	}
	
}
